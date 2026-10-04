import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class RoomCameraTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe room edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static LuaDraft proposal(String code){return WorldCameraTest.proposal(code,"camera_rooms");}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String src="x=64 y=64"+nl+"function _draw()"+nl+"  map(0,0,0,0,32,32) -- world"+nl+"end"+nl;
            LuaDraft d=proposal(src);d.point(2,2);d.insertion.set(0,"x+4");d.insertion.set(1,"max(y,0)");d.insertion.set(2,"8");d.insertion.set(3,"4");d.insertion.beginPreview();
            LuaDraft restored=LuaDraft.restore(d.encode());d.applyInsert();restored.applyInsert();check(d.text().equals(restored.text()),"restore insertion preview");
            String inserted=d.text();check(inserted.replace("  "+WorldCamera.roomsCode("x+4","max(y,0)","8","4").replace("\n",nl),"").equals(src),"only one insertion, newline preserved");
            d.history(false);check(d.text().equals(src),"one undo");d.history(true);check(d.text().equals(inserted),"redo");
            d.point(2,0);d.beginParameters();check(d.insertion.item().id.equals("camera_rooms")&&d.insertion.value(1).equals("max(y,0)"),"reopen room parameters");
            d.insertion.set(0,"x+8");d.insertion.set(1,"y+8");d.insertion.set(2,"3");d.insertion.set(3,"2");d.insertion.beginPreview();restored=LuaDraft.restore(d.encode());restored.applyInsert();d.applyInsert();check(d.text().equals(restored.text()),"restore four field replacement");
            check(d.text().contains(WorldCamera.roomsCode("x+8","y+8","3","2").trim()),"all spans match fields");d.history(false);check(d.text().equals(inserted),"undo all fields together");
            d.point(2,0);d.beginParameters();d.applyInsert();d.history(false);check(d.text().equals(src),"no-op has no history");
        }
        String follow=" \tcamera ( "+WorldCamera.code("x","y","32","48").substring(7).trim();follow=follow.substring(0,follow.length()-1)+" ); -- keep camera()";
        LuaDraft d=proposal(follow+"\r\n");d.cancelInsert();d.beginParameters();d.insertion.switchCameraMode();
        check(d.insertion.value(2).equals("2")&&d.insertion.value(3).equals("3"),"mode switch preserves world extent");
        check(d.text().equals(follow+"\r\n"),"switch is only a proposal");d.insertion.beginPreview();LuaDraft r=LuaDraft.restore(d.encode());check(r.insertion.item().id.equals("camera_rooms"),"restore mode different from source");d.applyInsert();r.applyInsert();check(d.text().equals(r.text()),"recovered conversion same");
        String changed=d.text();check(changed.startsWith(" \tcamera(")&&changed.endsWith("; -- keep camera()\r\n"),"prefix/suffix preserved during mode replacement");check(changed.contains(WorldCamera.roomsCode("x","y","2","3").trim()),"converted generated call");
        d.history(false);check(d.text().equals(follow+"\r\n"),"mode undo exact including formatting");d.beginParameters();d.insertion.switchCameraMode();d.insertion.switchCameraMode();d.applyInsert();check(d.text().equals(follow+"\r\n"),"double switch is no-op");
        d=proposal(changed);d.cancelInsert();d.beginParameters();d.insertion.switchCameraMode();d.insertion.beginPreview();r=LuaDraft.restore(d.encode());r.applyInsert();check(r.text().contains(WorldCamera.code("x","y","32","48").trim()),"reverse conversion extent");
        for(String src:new String[]{"camera=1\r\n","local flr\r\n","function a(mid) end\r\n","#include other.p8\r\n","_ENV={}\r\n"}){LuaDraft b=proposal(src);refused(b::applyInsert);check(b.text().equals(src),"conflict preserved");}
        LuaDraft conflict=proposal("flr=print\r\n"+WorldCamera.code("x","y","32","32"));conflict.cancelInsert();conflict.point(1,0);conflict.beginParameters();conflict.insertion.switchCameraMode();String before=conflict.text();refused(conflict::applyInsert);check(conflict.text().equals(before)&&conflict.insertion!=null,"new mode API conflict retains proposal");
        LuaDraft reverse=proposal("max=print\r\n"+WorldCamera.roomsCode("x","y","2","2"));reverse.cancelInsert();reverse.point(1,0);reverse.beginParameters();reverse.insertion.switchCameraMode();refused(reverse::applyInsert);
        LuaInsert i=proposal("\r\n").insertion;i.field=2;for(int n=0;n<20;n++)i.step(1);check(i.value(2).equals("8"),"column upper bound");for(int n=0;n<20;n++)i.step(-1);check(i.value(2).equals("1"),"column lower bound");i.field=3;for(int n=0;n<20;n++)i.step(1);check(i.value(3).equals("4"),"row upper bound");
        for(String bad:new String[]{"0","9","-1","2.5"}){final String value=bad;refused(()->i.set(2,value));}
        LuaInsert f=WorldCameraTest.proposal("\r\n","camera_follow").insertion;f.set(2,"20");String old=f.code();refused(f::switchCameraMode);check(f.code().equals(old),"partial rooms never silently expand field");
        for(String v:new String[]{"x),7);print(1)--","x) --"}){LuaDraft b=proposal("\r\n");b.insertion.set(0,v);refused(b::applyInsert);}
        String custom=WorldCamera.roomsCode("x","y","2","2").replace("/128)","/64)").trim();LuaCall call=LuaCall.parse(custom,0,custom.length());check(call.form.item().id.equals("camera"),"custom formula stays literal camera");check(call.replacement(custom,call.form).equals(custom),"custom code no-op exact");
        WorkshopCartridge cart=TileProbeTest.cart(WorldCamera.code("x","y","32","32"));TileProbeTest.Port port=new TileProbeTest.Port();WorkshopSession s=new WorkshopSession(cart,port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.PREVIOUS);s.act(Action.UNDO);check(s.codeDraft.insertion.item().id.equals("camera_rooms"),"controller Y mode switch");
        for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.TEST);check(port.saves==0&&port.launches==0,"Start trapped in comparison");s.act(Action.CANCEL);check(s.codeDraft.insertion.item().id.equals("camera_rooms"),"back retains chosen mode");s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.codeDraft.text().contains("camera(flr("),"controller applies conversion");
        port.fail=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&s.codeDraft!=null,"failed save retains draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"save and test after retry");s.act(Action.UNDO);check(Arrays.equals(cart.bytes(),s.cart().bytes()),"project undo preserves opaque sections");
        if(args.length>0)Files.write(Paths.get(args[0]),oracle().getBytes(StandardCharsets.UTF_8));
        System.out.println("RoomCameraTest: "+checks+" checks passed");
    }
    static String oracle(){
        String code="checks=0\nfunction check(ex,ey)\n local x,y=camera()\n assert(x==ex and y==ey,\"room \"..checks..\": \"..x..\",\"..y)\n checks+=1\nend\n";
        String[][] cases={{"0","0","2","2","0","0"},{"0x7f.ffff","0x7f.ffff","2","2","0","0"},{"128","127","2","2","128","0"},{"127","128","2","2","0","128"},{"128","128","2","2","128","128"},{"-1","-32768","2","2","0","0"},{"256","256","2","2","128","128"},{"0x7fff.ffff","0x7fff.ffff","8","4","896","384"},{"200","200","1","1","0","0"},{"384","256","3","2","256","128"},{"255.5","383.5","8","4","128","256"},{"256","384","8","4","256","384"},{"127.5","0","2","2","0","0"},{"128.5","0","2","2","128","0"}};
        for(String[] v:cases)code+=WorldCamera.roomsCode(v[0],v[1],v[2],v[3])+"check("+v[4]+","+v[5]+")\n";
        code+="calls=0\nfunction target() calls+=1 return 128 end\n"+WorldCamera.roomsCode("target()","target()","2","2")+"assert(calls==2,\"targets once\")\ncheck(128,128)\n";
        code+="cls(1)\n"+WorldCamera.roomsCode("128","0","2","2")+"rectfill(132,8,135,11,14)\ncamera()\nassert(pget(4,8)==14 and pget(124,8)==1,\"world offset\")\nchecks+=1\nrectfill(2,2,5,5,11)\nassert(pget(2,2)==11,\"hud\")\nchecks+=1\n";
        code+="camera(33,44)\n"+WorldCamera.roomsCode("0","0","2","2")+"check(0,0)\nfunction _draw()\n cls(1)\n print(\"rooms: \"..checks..\"/18\",8,48,11)\n print(\"edges / fractions / hud\",8,64,7)\nend\n";
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code;
    }
}
