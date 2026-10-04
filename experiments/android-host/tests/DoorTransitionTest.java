import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class DoorTransitionTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe transition accepted");}catch(IllegalArgumentException expected){checks++;}}
    static LuaDraft draft(String src){return WorldCameraTest.proposal(src,"door_pair");}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String src="x=64 y=64"+nl+"function _update()"+nl+" -- after motion"+nl+"end"+nl;
            LuaDraft d=draft(src);d.point(2,0);d.insertion.beginPreview();LuaDraft r=LuaDraft.restore(d.encode());d.applyInsert();r.applyInsert();
            check(d.text().equals(r.text()),"insertion restored");String inserted=d.text();
            check(inserted.replace(" "+DoorTransition.code(draft("").insertion).replace("\n",nl+" ").trim()+nl,"").equals(src),"only insertion");
            d.history(false);check(d.text().equals(src),"one undo");d.history(true);check(d.text().equals(inserted),"redo");
            for(int line=2;line<19;line++){
                d.point(line,0);d.beginParameters();check(d.insertion.doorRecipe()&&d.insertion.screen==LuaInsert.Screen.FIELDS,"reopen fields on every block line "+line);d.cancelInsert();
            }
            d.point(2,0);d.beginParameters();String[] values={"cx","cy","portal_lock","16","24","224","144","24","40"};
            for(int n=0;n<9;n++)d.insertion.set(n,values[n]);d.insertion.beginPreview();r=LuaDraft.restore(d.encode());d.applyInsert();r.applyInsert();
            check(d.text().equals(r.text()),"edit restored");check(d.text().contains("cx,cy=224,144")&&d.text().contains("cx,cy=16,24"),"all repeated uses replaced");
            check(d.text().substring(0,d.text().indexOf("do")).equals(inserted.substring(0,inserted.indexOf("do"))),"prefix preserved");
            d.history(false);check(d.text().equals(inserted),"edit undo exact");d.beginParameters();d.applyInsert();d.history(false);check(d.text().equals(src),"no-op no history");
        }
        for(String bad:new String[]{"gate_busy=false\n","local gate_busy\n","function f(gate_busy) end\n","#include other.p8\n","_ENV={}\n"}){
            LuaDraft d=draft(bad);refused(d::applyInsert);check(d.text().equals(bad),"source preserved");
        }
        for(String bad:new String[]{"camera","_gate_x","x","y"}){LuaDraft d=draft("");d.insertion.set(2,bad);refused(d::applyInsert);}
        LuaDraft overlap=draft("");overlap.insertion.set(5,"100");refused(overlap::applyInsert);
        overlap.insertion.set(5,"112");overlap.applyInsert();check(overlap.text().contains("_gate_x>=112"),"touching edges allowed");
        LuaInsert f=draft("").insertion;for(String bad:new String[]{"-16385","16384","1.5","1);print(1)"})refused(()->f.set(3,bad));
        for(String bad:new String[]{"0","1025","-1"})refused(()->f.set(7,bad));
        f.set(3,"-16384");f.set(5,"16383");f.set(7,"1024");DoorTransition.validate("",f);check(true,"bounded endpoints avoid overflow");
        String original=DoorTransition.code(draft("").insertion);
        check(DoorTransition.find("--[[\n"+original+"]]",5)==null,"long string/comment protected");
        check(DoorTransition.find(original.replace("elseif not gate_busy","elseif gate_busy"),0)==null,"manual behavior not rewritten");
        check(DoorTransition.find(original.replace("x,y=160,48","x,y=161,48"),0)==null,"inconsistent destinations not rebound");
        LuaDraft conflict=draft(original);conflict.cancelInsert();conflict.beginParameters();conflict.insertion.set(2,"other");
        check(conflict.callEdit.replacement(original,conflict.insertion).contains("other=true"),"latch renamed together");
        LuaDraft two=draft(original);two.insertion.set(2,"another");two.applyInsert();check(DoorTransition.find(two.text(),0)!=null,"two independent latches");
        WorkshopCartridge cart=TileProbeTest.cart("x=0 y=0\nfunction _update()\nend\n");TileProbeTest.Port port=new TileProbeTest.Port();WorkshopSession s=new WorkshopSession(cart,port);
        s.switchTool(1);s.act(Action.CONFIRM);s.codeDraft.point(2,0);s.act(Action.CONTEXT);
        for(int n=0;n<29;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.codeDraft.insertion.doorRecipe(),"controller catalog");for(int n=0;n<9;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.TEST);
        check(port.saves==0&&port.launches==0,"Start trapped");s.act(Action.CANCEL);check(s.codeDraft.insertion.screen==LuaInsert.Screen.FIELDS,"back to fields");s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.codeDraft.text().contains("gate_busy=true"),"controller apply");port.fail=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&s.codeDraft!=null,"failed save retains draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry and test");s.act(Action.UNDO);check(Arrays.equals(cart.bytes(),s.cart().bytes()),"opaque sections retained");
        if(args.length>0)Files.write(Paths.get(args[0]),oracle().getBytes(StandardCharsets.UTF_8));
        System.out.println("DoorTransitionTest: "+checks+" checks passed");
    }
    static String oracle(){
        LuaInsert f=draft("").insertion;
        String code="checks=0\nfunction step()\n"+DoorTransition.code(f)+"end\nfunction check(a,b,c)\n assert(x==a and y==b and gate_busy==c,\"gate \"..checks)\n checks+=1\nend\n";
        code+="x=64 y=64 step() check(64,64,false)\nx=96 y=48 step() check(160,48,true)\n";
        code+="for i=1,120 do step() end\ncheck(160,48,true)\nx=175 y=79 step() check(175,79,true)\n";
        code+="x=176 step() check(176,79,false)\nx=160 step() check(96,48,true)\nx=112 step() check(112,48,false)\n";
        code+="x=96 y=80 step() check(96,80,false)\ny=47 step() check(96,47,false)\nx=95 y=48 step() check(95,48,false)\n";
        code+="x=111.999 y=79.999 step() check(160,48,true)\nx=-32768 y=-32768 step() check(-32768,-32768,false)\n";
        code+="x=0x7fff.ffff y=0x7fff.ffff step() check(x,y,false)\ngate_busy=nil x=160 y=48 step() check(96,48,true)\n";
        f.set(2,"other_busy");f.set(3,"-16384");f.set(4,"-16384");f.set(5,"16383");f.set(6,"16383");f.set(7,"1024");f.set(8,"1024");
        code+="x=-16384 y=-16384\n"+DoorTransition.code(f)+"assert(x==16383 and y==16383 and other_busy,\"extreme endpoints\")\nchecks+=1\n";
        code+="assert(gate_busy,\"independent latch\")\nchecks+=1\nx=96 y=48 gate_busy=false step()\n"+WorldCamera.roomsCode("x","y","2","2")+"local cx,cy=camera()\nassert(cx==128 and cy==0,\"arrival camera\")\nchecks+=1\n";
        code+="function _draw()\n cls(1)\n print(\"doors: \"..checks..\"/17\",8,48,11)\n print(\"entry / exit / lock / camera\",8,64,7)\nend\n";
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code;
    }
}
