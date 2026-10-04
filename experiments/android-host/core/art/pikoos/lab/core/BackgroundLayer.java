package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Horizontal sheet strip, ordinary draw code with scoped locals and restored camera. */
public final class BackgroundLayer {
    private BackgroundLayer(){}
    public static final int[] FORM_FIELDS={0,4,5,6,7,8};
    public static final String[] FACTORS={"0","0.25","0.5","0.75","1"};
    private static final String TEMPLATE="do\n"
        +" local _bg_cx,_bg_cy=camera()\n"
        +" local _bg_w=@2@\n"
        +" local _bg_phase=flr(\n"
        +"  (((time()%_bg_w)*@5@)%_bg_w\n"
        +"  -(_bg_cx*@6@)%_bg_w)%_bg_w)\n"
        +" if @7@ then\n"
        +"  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do\n"
        +"   sspr(@0@,@1@,@2@,@3@,_bg_dx,@4@)\n"
        +"  end\n end\n camera(_bg_cx,_bg_cy)\nend";
    private static final List<Integer> FIELDS=new ArrayList<>();
    private static final Pattern BLOCK;
    static{
        Matcher m=Pattern.compile("@([0-7])@").matcher(TEMPLATE);StringBuilder p=new StringBuilder("(?m)^[ \\t]*");int at=0;
        while(m.find()){literal(p,TEMPLATE.substring(at,m.start()));int f=Integer.parseInt(m.group(1));FIELDS.add(f);p.append(f==7?"(true|false)":f==6?"(0(?:\\.(?:25|5|75))?|1)":"(-?[0-9]+)");at=m.end();}
        literal(p,TEMPLATE.substring(at));p.append("[ \\t]*(?=\\r|\\n|$)");BLOCK=Pattern.compile(p.toString());
    }
    private static void literal(StringBuilder p,String value){String[] rows=value.split("\n",-1);for(int n=0;n<rows.length;n++){if(n>0)p.append("(?:\\r\\n|\\r|\\n)[ \\t]*");p.append(Pattern.quote(n==0?rows[n]:rows[n].replaceFirst("^ +","")));}}
    public static SpriteRegion region(LuaInsert f){return new SpriteRegion(Integer.parseInt(f.value(0)),Integer.parseInt(f.value(1)),Integer.parseInt(f.value(2)),Integer.parseInt(f.value(3)));}
    public static void setRegion(LuaInsert f,SpriteRegion r){int[] a={r.x,r.y,r.width,r.height};for(int n=0;n<4;n++)f.set(n,""+a[n]);}
    public static String code(LuaInsert f){region(f);String s=TEMPLATE;for(int n=0;n<8;n++)s=s.replace("@"+n+"@",f.value(n));return s+"\n";}
    public static void validate(String source,LuaInsert f){
        region(f);LuaContext context=new LuaContext(source);LuaSymbols symbols=new LuaSymbols(source);
        if(Pattern.compile("(?m)^\\s*#include\\b|\\b(?:_G|_ENV)\\b").matcher(context.masked()).find())throw new IllegalArgumentException("Фон для includes/изменённого окружения пока настраивается в Lua. Код сохранён.");
        for(String api:new String[]{"camera","sspr","time","flr"})if(symbols.shadows(api)||context.defines(api))throw new IllegalArgumentException("API "+api+" переопределён. Фон не изменён.");
    }
    public static LuaCall find(String source,int point){
        Matcher m=BLOCK.matcher(source);LuaContext context=new LuaContext(source);
        while(m.find()){
            if(point<m.start()||point>=m.end()||!context.allowsLine(m.start()))continue;
            LuaInsert f=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("background"))f.choose(n);
            f.editing=true;f.screen=LuaInsert.Screen.FIELDS;String[] values=new String[8];int[] from=new int[FIELDS.size()],to=new int[from.length],fields=new int[from.length];
            for(int n=0;n<from.length;n++){int field=FIELDS.get(n);String v=m.group(n+1);if(values[field]!=null&&!values[field].equals(v))return null;values[field]=v;from[n]=m.start(n+1)-m.start();to[n]=m.end(n+1)-m.start();fields[n]=field;}
            try{for(int n=0;n<8;n++)f.set(n,values[n]);validate(source,f);}catch(IllegalArgumentException e){return null;}
            return new LuaCall(m.start(),m.end(),m.group(),"background",from,to,f,fields);
        }return null;
    }
    /** Illustrative preview at a specified clock/camera; standard palette and transparency. */
    public static int pixel(WorkshopCartridge cart,LuaInsert f,int x,int y,double seconds,double cameraX){
        SpriteRegion r=region(f);int sy=y-Integer.parseInt(f.value(4));if(f.value(7).equals("false")||sy<0||sy>=r.height)return 0;
        double w=r.width,speed=Integer.parseInt(f.value(5)),factor=Double.parseDouble(f.value(6));
        double phase=mod(mod(mod(seconds,w)*speed,w)-mod(cameraX*factor,w),w);
        int sx=Math.floorMod(x-(int)Math.floor(phase),r.width);return cart.pixel(r,sx,sy);
    }
    private static double mod(double x,double y){return x-Math.floor(x/y)*y;}
}
