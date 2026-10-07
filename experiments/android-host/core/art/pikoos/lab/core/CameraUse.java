package art.pikoos.lab.core;

import java.io.*;
import java.util.*;

/** A controller form for ordinary ordered camera calls, never a hidden scene system. */
public final class CameraUse {
    public static final String[] MODES={"camera","camera_follow","camera_rooms","camera_reset"};
    private final GameUses owner;
    private final LuaCall original;
    public LuaInsert form;
    public int field,rangeEnd;
    private int[] preview;
    public boolean dynamicPreview;
    public CameraUse(GameUses owner,boolean reset){this.owner=owner;original=null;rangeEnd=owner.index;form=mode(reset?3:0);}
    public CameraUse(GameUses owner,LuaCall call){this.owner=owner;original=call;rangeEnd=owner.index;form=copy(call.form);}
    private static LuaInsert mode(int n){LuaInsert f=new LuaInsert();for(int i=0;i<LuaInsert.ITEMS.length;i++)if(LuaInsert.ITEMS[i].id.equals(MODES[n]))f.choose(i);return f;}
    private static LuaInsert copy(LuaInsert input){LuaInsert f=new LuaInsert();f.choose(input.selected);for(int n=0;n<input.item().fields.length;n++)f.set(n,input.value(n));return f;}
    public static String title(LuaInsert f){String id=f.item().id;return id.equals("camera_reset")?"Экран · интерфейс":id.equals("camera_follow")?"Камера · слежение":id.equals("camera_rooms")?"Камера · комнаты":"Камера · положение";}
    public boolean ranged(){return original==null&&!form.item().id.equals("camera_reset");}
    public int rows(){return 2+form.item().fields.length+(ranged()?1:0);}
    public String label(int row){
        if(row==0)return title(form);
        if(row<=form.item().fields.length)return form.item().fields[row-1].label+": "+form.value(row-1);
        if(ranged()&&row==rows()-2)return "До: "+(rangeEnd+1)+" · "+owner.entries.get(rangeEnd).title();
        return "Проверить и сохранить";
    }
    public void change(int delta){
        if(field==0){
            int n=Arrays.asList(MODES).indexOf(form.item().id),next=Math.max(0,Math.min(3,n+(delta<0?-1:1)));
            if(next==n)return;
            if((n==1&&next==2)||(n==2&&next==1))form.switchCameraMode();
            else{LuaInsert f=mode(next);if(n!=3&&next!=3){f.set(0,form.value(0));f.set(1,form.value(1));}form=f;}
            field=0;return;
        }
        if(field<=form.item().fields.length){
            int at=field-1;String v=form.value(at);
            if(!v.matches("-?[0-9]+"))throw new IllegalArgumentException("This is an expression. Press X to choose a number or project value. The original value is preserved.");
            long next=Long.parseLong(v)+delta;
            if(next < -32768||next>32767)return;
            try{form.set(at,""+next);}catch(IllegalArgumentException e){return;}
        }else if(ranged()&&field==rows()-2){
            int next=Math.max(owner.index,Math.min(owner.entries.size()-1,rangeEnd+(delta<0?-1:1)));
            for(int n=owner.index;n<=next;n++)if(owner.entries.get(n).isCamera())throw new IllegalArgumentException("The range cannot cross another camera. Edit that camera separately.");
            rangeEnd=next;
        }
    }
    public void symbol(){
        if(field<1||field>2||form.item().fields.length<2)return;
        ArrayList<String> names=new ArrayList<>();names.add("0");
        GameUses.Entry e=owner.current();String point=""+(field==1?e.x():e.y());if(!names.contains(point))names.add(point);
        for(LuaSymbols.Entry symbol:new LuaSymbols(owner.source).project(false))if(!names.contains(symbol.name))names.add(symbol.name);
        int at=names.indexOf(form.value(field-1));form.set(field-1,names.get((at+1)%names.size()));
    }
    public void validate(){
        WorldCamera.validateSource(owner.source,form.item().id);WorldCamera.validateForm(form);
        if(ranged()){
            if(owner.current().view!=null&&offset(owner.current().view)==null)throw new IllegalArgumentException("The previous camera uses runtime expressions. Edit it separately; recalculating its position could change the game.");
            if(rangeEnd<owner.index||rangeEnd>=owner.entries.size())throw new IllegalArgumentException("Choose the last use in the range.");
            for(int n=owner.index;n<=rangeEnd;n++)if(owner.entries.get(n).isCamera())throw new IllegalArgumentException("The range crosses another camera.");
        }
    }
    private static int after(String s,int at){if(at<s.length()&&s.charAt(at)=='\r')at++;if(at<s.length()&&s.charAt(at)=='\n')at++;return at;}
    private String newline(){return owner.source.contains("\r\n")?"\r\n":owner.source.contains("\r")?"\r":"\n";}
    private String indent(int at){int end=at;while(end<owner.source.length()&&(owner.source.charAt(end)==' '||owner.source.charAt(end)=='\t'))end++;return owner.source.substring(at,end);}
    public CartEdit proposal(boolean deleting){
        validate();String source=owner.source,result;String nl=newline();
        if(original!=null){
            if(deleting){
                String mask=new LuaContext(original.original).masked();int a=mask.indexOf("camera"),b=mask.lastIndexOf(')')+1;
                String tail=original.original.substring(b);String replacement=tail.trim().isEmpty()?"":original.original.substring(0,a)+tail+nl;
                result=source.substring(0,original.start)+replacement+source.substring(after(source,original.end));
            }else if(form.item().id.equals(original.form.item().id)||WorldCamera.modeSwitch(original.form.item().id,form))result=original.replacement(source,form);
            else{
                String mask=new LuaContext(original.original).masked();int a=mask.indexOf("camera"),b=mask.lastIndexOf(')')+1;
                String changed=original.original.substring(0,a)+form.code().trim()+original.original.substring(b);
                LuaCall.parse(changed,0,changed.length());result=source.substring(0,original.start)+changed+source.substring(original.end);
            }
        }else{
            GameUses.Entry first=owner.current(),last=owner.entries.get(rangeEnd);int end=last.animation!=null?last.end():after(source,last.end());String lead=indent(first.start());
            String open=lead+form.code().trim()+nl;
            if(ranged()){
                String restore=first.view==null?"camera()":first.view.form.code().trim();
                result=source.substring(0,first.start())+open+source.substring(first.start(),end)+lead+restore+nl+source.substring(end);
            }else result=source.substring(0,first.start())+open+source.substring(first.start());
        }
        LuaDraft d=new LuaDraft(owner.base,0);d.selectAll();d.replace(result);return d.edit();
    }
    public String scope(){
        if(ranged())return "Элементы "+(owner.index+1)+"–"+(rangeEnd+1)+" · возврат камеры";
        int end=owner.index+1;while(end<owner.entries.size()&&!owner.entries.get(end).isCamera())end++;
        return original==null?"До следующей камеры":"Дальше размещений: "+(end-owner.index-1);
    }
    /** Resource-only sketch. Text/shapes and dynamic expressions must be checked in Test. */
    public int[] preview(boolean deleting){
        if(preview!=null)return preview;
        WorkshopCartridge cart=proposal(deleting).candidate(owner.base);GameUses uses=new GameUses(cart,owner.returnTool);
        int[] pixels=new int[16384];Arrays.fill(pixels,1);P8Map map=cart.map();
        for(GameUses.Entry e:uses.entries){
            if(e.isCamera())continue;int[] camera=offset(e.view);if(camera==null){dynamicPreview=true;continue;}
            int dx=e.x()-camera[0],dy=e.y()-camera[1];SpriteRegion r=e.animation!=null?e.animation.initial.frame(0).region:e.kind().equals("spr")?new SpriteRegion(e.values[0]%16*8,e.values[0]/16*8,8,8):e.kind().equals("sspr")?new SpriteRegion(e.values[0],e.values[1],e.values[2],e.values[3]):null;
            int width=r!=null?r.width:e.values[4]*8,height=r!=null?r.height:e.values[5]*8;
            for(int yy=Math.max(0,dy);yy<Math.min(128,dy+height);yy++)for(int xx=Math.max(0,dx);xx<Math.min(128,dx+width);xx++){
                int x=xx-dx,y=yy-dy,color;
                if(r!=null)color=cart.sheetPixel(r.x+x,r.y+y);
                else{int tile=map.tile(e.values[0]+x/8,e.values[1]+y/8);color=tile==0?0:cart.sheetPixel(tile%16*8+x%8,tile/16*8+y%8);}
                if(color!=0)pixels[yy*128+xx]=color;
            }
        }
        preview=pixels;return preview;
    }
    public void invalidatePreview(){preview=null;dynamicPreview=false;}
    /** Null means runtime expressions: never pretend to evaluate arbitrary Lua in an editor preview. */
    public static int[] offset(LuaCall call){return call==null?new int[]{0,0}:offset(call.form);}
    public static int[] offset(LuaInsert f){
        if(f.item().id.equals("camera_reset"))return new int[]{0,0};
        try{
            int x=Integer.parseInt(f.value(0)),y=Integer.parseInt(f.value(1));
            if(x < -32768||x>32767||y < -32768||y>32767)return null;
            if(f.item().id.equals("camera_follow")){x=Math.max(64,Math.min(Math.max(64,Integer.parseInt(f.value(2))*8-64),x))-64;y=Math.max(64,Math.min(Math.max(64,Integer.parseInt(f.value(3))*8-64),y))-64;}
            if(f.item().id.equals("camera_rooms")){x=Math.max(0,Math.min(Integer.parseInt(f.value(2))*128-1,x))/128*128;y=Math.max(0,Math.min(Integer.parseInt(f.value(3))*128-1,y))/128*128;}
            return new int[]{x,y};
        }catch(NumberFormatException e){return null;}
    }
    public void write(DataOutputStream out)throws IOException{
        out.writeInt(field);out.writeInt(rangeEnd);out.writeInt(form.selected);for(int n=0;n<form.item().fields.length;n++)out.writeUTF(form.value(n));
    }
    public static CameraUse read(DataInputStream in,GameUses g)throws IOException{
        int field=in.readInt(),end=in.readInt(),selected=in.readInt();
        if(g.current()==null||(!g.creating&&!g.current().isCamera())||(g.creating&&g.current().isCamera()))throw new IOException("camera selection");
        CameraUse c=g.creating?new CameraUse(g,false):new CameraUse(g,g.current().call);LuaInsert f=new LuaInsert();f.choose(selected);if(!f.cameraRecipe())throw new IOException("camera form");
        for(int n=0;n<f.item().fields.length;n++)f.set(n,in.readUTF());c.form=f;c.field=field;c.rangeEnd=end;
        if(field<0||field>=c.rows()||end<g.index||end>=g.entries.size())throw new IOException("camera state");c.validate();return c;
    }
}
