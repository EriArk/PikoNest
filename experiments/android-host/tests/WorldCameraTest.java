import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class WorldCameraTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe camera accepted");}catch(IllegalArgumentException expected){checks++;}}
    static int item(String id){for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals(id))return n;throw new AssertionError(id);}
    static LuaDraft proposal(String source,String id){LuaDraft d=new LuaDraft(TileProbeTest.cart(source),0);d.beginInsert();d.insertion.choose(item(id));d.insertion.screen=LuaInsert.Screen.FIELDS;return d;}
    public static void main(String[] args)throws Exception{
        for(String newline:new String[]{"\n","\r\n","\r"}){
            String src="x=120 y=96"+newline+"function _draw()"+newline+"  map(0,0,0,0,32,32) -- world"+newline+"end"+newline;
            LuaDraft d=proposal(src,"camera_follow");d.point(2,2);d.insertion.set(0,"x+4");d.insertion.set(1,"y+4");d.insertion.set(2,"64");d.insertion.beginPreview();
            LuaDraft recovery=LuaDraft.restore(d.encode());check(recovery.insertion.screen==LuaInsert.Screen.PREVIEW,"restore review");d.applyInsert();recovery.applyInsert();check(d.text().equals(recovery.text()),"same restored insertion");
            String changed=d.text();check(changed.replace("  "+WorldCamera.code("x+4","y+4","64","32").replace("\n",newline),"").equals(src),"only inserted call changed");
            d.history(false);check(d.text().equals(src),"single exact undo");d.history(true);check(d.text().equals(changed),"redo");
            d.point(2,0);d.beginParameters();check(d.insertion.item().id.equals("camera_follow")&&d.insertion.value(2).equals("64"),"reopen friendly fields");
            d.insertion.set(0,"max(x,0)+8");d.insertion.set(1,"y+6");d.insertion.set(2,"48");d.insertion.set(3,"16");d.insertion.beginPreview();
            recovery=LuaDraft.restore(d.encode());d.applyInsert();recovery.applyInsert();check(d.text().equals(recovery.text()),"edit preview recovery");
            check(d.text().contains(WorldCamera.code("max(x,0)+8","y+6","48","16").trim()),"ordered spans map to X Y width height");
            check(d.text().contains("  map(0,0,0,0,32,32) -- world"),"world draw remains unchanged");d.history(false);check(d.text().equals(changed),"one undo for four fields");
            d.point(2,0);d.beginParameters();d.applyInsert();d.history(false);check(d.text().equals(src),"no-op adds no history");
        }
        String call=" \tcamera( "+WorldCamera.code("x","y","32","16").substring(7).trim();
        call=call.substring(0,call.length()-1)+" ); -- keep";
        LuaCall edit=LuaCall.parse(call,0,call.length());edit.form.set(2,"40");check(edit.replacement(call,edit.form).endsWith(" ); -- keep"),"surrounding whitespace and comment preserved");
        final String stale=call+" ";refused(()->edit.replacement("!"+stale,edit.form));
        for(String raw:new String[]{"camera(3,4)","camera()","camera(  ) -- hud","camera(x+2,min(y,100))"}){
            LuaDraft d=proposal(raw+"\r\n","camera");d.cancelInsert();d.beginParameters();check(d.insertion.cameraRecipe(),"camera form opens");d.insertion.beginPreview();LuaDraft restored=LuaDraft.restore(d.encode());restored.applyInsert();check(restored.text().equals(d.text()),"literal no-op preserves call");
        }
        for(String src:new String[]{"camera=1\r\n","local camera\r\n","function a(mid) end\r\n","max=print\r\n","#include other.p8\r\n","_G.camera=nil\r\n"}){
            LuaDraft d=proposal(src,"camera_follow");refused(d::applyInsert);check(d.text().equals(src),"refusal preserves original");
        }
        for(String value:new String[]{"x),7);print(1)--","x) --","x\",0)"}){
            LuaDraft d=proposal("\r\n","camera_follow");d.insertion.set(0,value);refused(d::applyInsert);
        }
        LuaDraft nested=proposal("\r\n","camera_follow");nested.insertion.set(0,"mid(0,x,128)");nested.applyInsert();check(nested.text().contains("(mid(0,x,128))"),"nested expression retained");
        LuaDraft form=proposal("x=4 y=8\r\n","camera_follow");form.insertion.beginSymbols(form.text());check(form.insertion.choices().stream().anyMatch(e->e.value.equals("x")),"existing coordinates selectable");
        form.insertion.screen=LuaInsert.Screen.FIELDS;form.insertion.field=2;for(int n=0;n<200;n++)form.insertion.step(-1);check(form.insertion.value(2).equals("1"),"small fields allowed");for(int n=0;n<200;n++)form.insertion.step(1);check(form.insertion.value(2).equals("128"),"bounded picker width");
        WorkshopCartridge cart=TileProbeTest.cart("x=64 y=64\r\nfunction _draw()\r\n cls(1)\r\nend\r\n");TileProbeTest.Port port=new TileProbeTest.Port();WorkshopSession s=new WorkshopSession(cart,port);s.switchTool(1);s.act(Action.CONFIRM);s.codeDraft.point(3,0);s.act(Action.CONTEXT);for(int n=0;n<item("camera_follow");n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.insertion.screen==LuaInsert.Screen.PREVIEW,"controller explicit review");s.act(Action.TEST);check(port.saves==0&&port.launches==0,"Start does not commit proposal");s.act(Action.CANCEL);check(s.codeDraft.insertion.screen==LuaInsert.Screen.FIELDS,"back to fields");s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.codeDraft.text().contains("camera(mid("),"controller confirms insertion");
        port.fail=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&s.codeDraft!=null,"write failure retains draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry saved cart launch");s.act(Action.UNDO);check(Arrays.equals(cart.bytes(),s.cart().bytes()),"project undo also preserves unknown sections");
        if(args.length>0)Files.write(Paths.get(args[0]),oracle().getBytes(StandardCharsets.UTF_8));
        System.out.println("WorldCameraTest: "+checks+" checks passed");
    }
    static String oracle(){
        String code="checks=0\nfunction check(x,y,w,h,ex,ey)\n local a,b=camera()\n assert(a==ex and b==ey,\"camera case \"..checks..\": \"..a..\",\"..b)\n checks+=1\nend\n";
        String[][] cases={{"0","0","32","32","0","0"},{"128","96","32","32","64","32"},{"256","256","32","32","128","128"},{"-32768","0x7fff.ffff","128","64","0","384"},{"64.75","80.5","32","32","0","16"},{"500","500","1","8","0","0"},{"999","-20","16","16","0","0"},{"960","448","128","64","896","384"},{"192","192","32","32","128","128"}};
        for(String[] v:cases)code+=WorldCamera.code(v[0],v[1],v[2],v[3])+"check("+String.join(",",v)+")\n";
        code+="calls=0\nfunction target() calls+=1 return 120 end\n"+WorldCamera.code("target()","96","32","32")+"assert(calls==1,\"target once\")\nchecks+=1\ncamera()\ncls(1)\ncamera(40,20)\nrectfill(48,30,50,32,14)\ncamera()\nassert(pget(8,10)==14 and pget(48,30)==1,\"world to screen\")\nchecks+=1\nrectfill(2,2,4,4,11)\nassert(pget(2,2)==11,\"fixed hud\")\nchecks+=1\ncamera(-3,7)\nlocal a,b=camera()\nassert(a==-3 and b==7,\"reset previous state\")\nlocal c,d=camera()\nassert(c==0 and d==0,\"reset origin\")\nchecks+=1\nfunction _draw()\n cls(1)\n print(\"camera: \"..checks..\"/13\",8,48,11)\n print(\"bounds / world / hud\",8,64,7)\nend\n";
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code;
    }
}
