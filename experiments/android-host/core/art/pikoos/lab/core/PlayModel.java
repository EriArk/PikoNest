package art.pikoos.lab.core;

import java.util.*;
import java.io.*;
import java.util.regex.*;

/** A bounded, fully verified ordinary-Lua recipe. Not a runtime or a cart-wide entity contract. */
public final class PlayModel {
    public static final String[] MOVEMENT={"Still","D-pad","Horizontal","Vertical"};
    public static final String[] RULES={"None","Touch: collect","Touch: win","Touch: lose","O: toggle"};
    public static final String[] GOALS={"Free play","Collect all","All switches on"};
    public static final class Element {
        public int sx,sy,w,h,x,y,movement,speed=1,rule,target,on,neighbor=-1;
        public Element copy(){Element e=new Element();e.sx=sx;e.sy=sy;e.w=w;e.h=h;e.x=x;e.y=y;e.movement=movement;e.speed=speed;e.rule=rule;e.target=target;e.on=on;e.neighbor=neighbor;return e;}
        int[] values(){return new int[]{sx,sy,w,h,x,y,movement,speed,rule,target,on,neighbor};}
    }
    private static final String[] FIELDS={"sx","sy","w","h","x","y","movement","speed","rule","target","on","neighbor"};
    public final List<Element> elements=new ArrayList<>();
    public int background=1,goal;
    public String prefix="",newline="\n";
    public PlayModel copy(){PlayModel p=new PlayModel();p.background=background;p.goal=goal;p.prefix=prefix;p.newline=newline;for(Element e:elements)p.elements.add(e.copy());return p;}
    public static PlayModel fromStatic(WorkshopCartridge cart){
        GameCanvas g=new GameCanvas(cart);if(!g.blocked.isEmpty())throw new IllegalArgumentException(g.blocked);if(g.play!=null)return g.play.copy();
        PlayModel p=new PlayModel();p.newline=cart.code().contains("\r\n")?"\r\n":"\n";
        String original=cart.code();LuaContext context=new LuaContext(original);StringBuilder comments=new StringBuilder();
        if(!context.allowsLine(original.length()))throw new IllegalArgumentException("Close the unfinished comment in All tools before adding behavior.");
        for(int n=0;n<original.length();n++)if(original.startsWith("--",n)&&context.allowsLine(n)){
            int end=n+1;while(end<original.length()&&!context.allowsLine(end))end++;
            comments.append(original.substring(n,end)).append(p.newline);n=end-1;
        }
        p.prefix=comments.toString();
        Matcher clear=Pattern.compile("cls\\(\\s*(\\d+)\\s*\\)").matcher(new LuaContext(cart.code()).masked());if(clear.find())p.background=Integer.parseInt(clear.group(1));
        p.readLayout(g.uses);p.validate();return p;
    }
    private void readLayout(GameUses uses){for(int n=0;n<uses.entries.size();n++){
        GameUses.Entry row=uses.entries.get(n);Element e=new Element();e.x=row.x();e.y=row.y();
        if(row.kind().equals("spr")){e.sx=row.values[0]%16*8;e.sy=row.values[0]/16*8;e.w=e.h=8;}
        else{e.sx=row.values[0];e.sy=row.values[1];e.w=row.values[2];e.h=row.values[3];}
        e.target=n==0?1:0;elements.add(e);
    }}
    public void validate(){
        if(elements.size()>16)throw new IllegalArgumentException("The first behavior editor supports up to 16 elements. All tools still keeps the full cartridge.");
        if(background<0||background>15||goal<0||goal>2)throw new IllegalArgumentException("Invalid game settings");
        for(int n=0;n<elements.size();n++){Element e=elements.get(n);new SpriteRegion(e.sx,e.sy,e.w,e.h);
            if(e.x<-32768||e.x>32767||e.y<-32768||e.y>32767||e.speed<1||e.speed>4||e.movement<0||e.movement>3||e.rule<0||e.rule>4||e.on<0||e.on>1)throw new IllegalArgumentException("Invalid behavior settings");
            if(e.movement!=0&&(e.w>128||e.h>128))throw new IllegalArgumentException("Movement requires an image fitting the game screen.");
            if(e.neighbor<-1||e.neighbor>=elements.size()||e.neighbor==n)throw new IllegalArgumentException("Choose a different switch, or None, for Also toggle.");
            if(e.rule==4&&e.neighbor>=0&&elements.get(e.neighbor).rule!=4)throw new IllegalArgumentException("Set the other sprite to Toggle first, then link it here.");
            if(e.rule>=1&&e.rule<=3&&(e.target<0||e.target>=elements.size()||e.target==n))throw new IllegalArgumentException("Choose a different sprite to touch this element. Add another sprite first if needed.");
        }
    }
    public String readiness(){
        int collect=0,toggles=0,movers=0;for(Element e:elements){if(e.rule==1)collect++;if(e.rule==4)toggles++;if(e.movement!=0)movers++;}
        if(goal==1&&collect==0)return "Add a Touch: collect rule to at least one sprite.";
        if(goal==2&&toggles==0)return "Add an O: toggle rule to at least one sprite.";
        return "X restarts. "+(toggles>0?"Left/right select switches; O toggles. ":"")+(movers>0?"Directions move configured sprites.":"");
    }
    public String source(){
        validate();StringBuilder b=new StringBuilder(prefix.replace("\r\n","\n"));
        b.append("function _init()\n game_bg=").append(background).append("\n game_goal=").append(goal).append("\n game_count=").append(elements.size()).append('\n');
        for(int n=0;n<elements.size();n++){Element e=elements.get(n);int[] v=e.values();for(int f=0;f<FIELDS.length;f++)b.append(' ').append(id(n)).append('_').append(FIELDS[f]).append('=').append(v[f]).append('\n');b.append(' ').append(id(n)).append("_visible=1\n");}
        b.append(" game_score=0\n game_result=0\n game_selected=0\n");
        for(int n=0;n<6;n++)b.append(" game_last").append(n).append("=btn(").append(n).append(")\n");
        b.append("end\n\nfunction _update()\n");
        for(int n=0;n<6;n++)b.append(" local pressed").append(n).append("=btn(").append(n).append(") and not game_last").append(n).append("\n game_last").append(n).append("=btn(").append(n).append(")\n");
        b.append(" if pressed5 then\n  _init()\n  return\n end\n if game_result~=0 then return end\n");
        List<Integer> switches=new ArrayList<>();for(int n=0;n<elements.size();n++)if(elements.get(n).rule==4)switches.add(n);
        if(!switches.isEmpty()){
            b.append(" if pressed0 then game_selected=(game_selected+").append(switches.size()-1).append(")%").append(switches.size()).append(" end\n");
            b.append(" if pressed1 then game_selected=(game_selected+1)%").append(switches.size()).append(" end\n");
        }
        for(int n=0;n<elements.size();n++){
            Element e=elements.get(n);String a=id(n);
            if(e.movement!=0){
                if(e.movement!=3){move(b,a,0,"x","-",128-e.w);move(b,a,1,"x","+",128-e.w);}
                if(e.movement!=2){move(b,a,2,"y","-",128-e.h);move(b,a,3,"y","+",128-e.h);}
            }
            if(e.rule==4){b.append(" if pressed4 and game_selected==").append(switches.indexOf(n)).append(" then ").append(a).append("_on=1-").append(a).append("_on");if(e.neighbor>=0)b.append(" ").append(id(e.neighbor)).append("_on=1-").append(id(e.neighbor)).append("_on");b.append(" end\n");}
        }
        // Move every element before evaluating any contacts; outcomes are checked in visible list order.
        for(int n=0;n<elements.size();n++){
            Element e=elements.get(n);if(e.rule<1||e.rule>3)continue;String a=id(n),t=id(e.target);Element other=elements.get(e.target);
            b.append(" if game_result==0 and ").append(a).append("_visible==1 and ").append(t).append("_visible==1 and ")
                .append(a).append("_x<").append(t).append("_x+").append(other.w).append(" and ").append(t).append("_x<").append(a).append("_x+").append(e.w)
                .append(" and ").append(a).append("_y<").append(t).append("_y+").append(other.h).append(" and ").append(t).append("_y<").append(a).append("_y+").append(e.h).append(" then\n");
            if(e.rule==1)b.append("  ").append(a).append("_visible=0\n  game_score+=1\n");else b.append("  game_result=").append(e.rule==2?1:2).append('\n');b.append(" end\n");
        }
        int total=0;for(Element e:elements)if(e.rule==1)total++;
        if(goal==1&&total>0)b.append(" if game_result==0 and game_score==").append(total).append(" then game_result=1 end\n");
        if(goal==2&&!switches.isEmpty()){b.append(" if game_result==0");for(int n:switches)b.append(" and ").append(id(n)).append("_on==1");b.append(" then game_result=1 end\n");}
        b.append("end\n\nfunction _draw()\n cls(game_bg)\n");
        for(int n=0;n<elements.size();n++){
            Element e=elements.get(n);String a=id(n);b.append(" if ").append(a).append("_visible==1 then\n  sspr(").append(e.sx).append(',').append(e.sy).append(',').append(e.w).append(',').append(e.h).append(',').append(a).append("_x,").append(a).append("_y)\n");
            if(e.rule==4){
                b.append("  rect(").append(a).append("_x,").append(a).append("_y,").append(a).append("_x+").append(e.w-1).append(',').append(a).append("_y+").append(e.h-1).append(',').append(a).append("_on==1 and 11 or 5)\n");
                b.append("  if game_selected==").append(switches.indexOf(n)).append(" then rect(").append(a).append("_x-2,").append(a).append("_y-2,").append(a).append("_x+").append(e.w+1).append(',').append(a).append("_y+").append(e.h+1).append(",10) end\n");
            }b.append(" end\n");
        }
        if(goal==1)b.append(" print(\"collected \"..game_score..\"/").append(total).append("\",2,2,7)\n");
        if(!switches.isEmpty())b.append(" print(\"left/right: select  o: toggle\",2,118,7)\n");
        b.append(" if game_result~=0 then\n  rectfill(14,48,113,76,1)\n  rect(14,48,113,76,7)\n  print(game_result==1 and \"you win!\" or \"try again\",44,54,10)\n  print(\"x: restart\",44,66,7)\n end\nend\n");
        return b.toString().replace("\n",newline);
    }
    private static String id(int n){return "sprite"+(n+1);}
    private static void move(StringBuilder b,String a,int key,String axis,String sign,int max){b.append(" if btn(").append(key).append(") then ").append(a).append('_').append(axis).append("=mid(0,").append(a).append('_').append(axis).append(sign).append(a).append("_speed,").append(max).append(") end\n");}
    public static WorkshopCartridge code(WorkshopCartridge base,String source){LuaDraft d=new LuaDraft(base,0);d.selectAll();d.replace(source);return d.edit().candidate(base);}
    public WorkshopCartridge write(WorkshopCartridge base){return code(base,source());}
    public WorkshopCartridge layout(WorkshopCartridge cart){StringBuilder b=new StringBuilder("function _draw()\n cls("+background+")\n");for(Element e:elements)b.append(" sspr(").append(e.sx).append(',').append(e.sy).append(',').append(e.w).append(',').append(e.h).append(',').append(e.x).append(',').append(e.y).append(")\n");b.append("end\n");return code(cart,b.toString().replace("\n",newline));}
    public PlayModel withLayout(WorkshopCartridge staticCart,GameCanvas.Operation op,int selected,boolean deleting){
        PlayModel p=copy();List<Element> layout=new ArrayList<>();PlayModel tmp=new PlayModel();tmp.readLayout(new GameUses(staticCart,0));layout.addAll(tmp.elements);
        if(deleting){
            for(int n=0;n<p.elements.size();n++){Element e=p.elements.get(n);if(n!=selected&&(e.rule>=1&&e.rule<=3&&e.target==selected||e.rule==4&&e.neighbor==selected))throw new IllegalArgumentException("Another sprite's rule uses this sprite. Change or remove that rule before removing its target.");}
            p.elements.remove(selected);for(Element e:p.elements){if(e.target>selected)e.target--;if(e.neighbor==selected)e.neighbor=-1;else if(e.neighbor>selected)e.neighbor--;}
        }else if(op==GameCanvas.Operation.ADD)p.elements.add(new Element());
        else if(op==GameCanvas.Operation.DUPLICATE)p.elements.add(p.elements.get(selected).copy());
        if(layout.size()!=p.elements.size())throw new IllegalArgumentException("The layout changed outside this edit.");
        for(int n=0;n<layout.size();n++){Element a=p.elements.get(n),l=layout.get(n);a.sx=l.sx;a.sy=l.sy;a.w=l.w;a.h=l.h;a.x=l.x;a.y=l.y;}
        p.validate();return p;
    }
    void encode(DataOutputStream o)throws IOException{o.writeUTF(prefix);o.writeUTF(newline);o.writeInt(background);o.writeInt(goal);o.writeInt(elements.size());for(Element e:elements)for(int v:e.values())o.writeInt(v);}
    static PlayModel decode(DataInputStream i)throws IOException{PlayModel p=new PlayModel();p.prefix=i.readUTF();p.newline=i.readUTF();if(!p.newline.equals("\n")&&!p.newline.equals("\r\n")||!new LuaContext(p.prefix).masked().trim().isEmpty())throw new IOException();p.background=i.readInt();p.goal=i.readInt();int count=i.readInt();if(count<0||count>16)throw new IOException();for(int n=0;n<count;n++){Element e=new Element();e.sx=i.readInt();e.sy=i.readInt();e.w=i.readInt();e.h=i.readInt();e.x=i.readInt();e.y=i.readInt();e.movement=i.readInt();e.speed=i.readInt();e.rule=i.readInt();e.target=i.readInt();e.on=i.readInt();e.neighbor=i.readInt();if(e.rule<0||e.rule>4||e.target<0||e.target>16)throw new IOException();p.elements.add(e);}return p;}
    /** A marker cannot authorize a rewrite: rebuild and compare every byte of the Lua recipe. */
    public static PlayModel read(WorkshopCartridge cart){
        String source=cart.code(),s=source.replace("\r\n","\n");int start=new LuaContext(s).masked().indexOf("function _init()\n");if(start<0)return null;
        try{
            PlayModel p=new PlayModel();p.newline=source.contains("\r\n")?"\r\n":"\n";p.prefix=s.substring(0,start);
            if(!new LuaContext(p.prefix).masked().trim().isEmpty())return null;
            String init=s.substring(start,s.indexOf("\nend",start));Map<String,Integer> v=new HashMap<>();Matcher m=Pattern.compile("(?m)^ ([a-z0-9_]+)=(-?[0-9]+)$").matcher(init);while(m.find()){if(v.put(m.group(1),Integer.parseInt(m.group(2)))!=null)return null;}
            p.background=v.get("game_bg");p.goal=v.get("game_goal");int count=v.get("game_count");if(count<0||count>16)return null;
            for(int n=0;n<count;n++){Element e=new Element();int[] f=new int[FIELDS.length];for(int k=0;k<f.length;k++)f[k]=v.get(id(n)+"_"+FIELDS[k]);e.sx=f[0];e.sy=f[1];e.w=f[2];e.h=f[3];e.x=f[4];e.y=f[5];e.movement=f[6];e.speed=f[7];e.rule=f[8];e.target=f[9];e.on=f[10];e.neighbor=f[11];p.elements.add(e);}
            p.validate();return source.equals(p.source())?p:null;
        }catch(Exception e){return null;}
    }
}
