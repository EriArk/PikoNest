import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TileMotionTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe movement accepted");}catch(IllegalArgumentException expected){checks++;}}
    static int item(String id){for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals(id))return n;throw new AssertionError();}
    static LuaDraft proposal(String source,String id){LuaDraft d=TileProbeTest.proposal(source);d.insertion.choose(item(id));d.insertion.screen=LuaInsert.Screen.FIELDS;return d;}
    public static void main(String[] args)throws Exception{
        String source="function _update()\r\n -- keep me\r\nend\r\n";
        LuaDraft d=proposal(source,"move_box");d.point(1,1);d.insertion.set(1,"7");d.insertion.set(2,"16");d.insertion.set(3,"16");d.insertion.beginPreview();d.insertion.previewLine=48;
        LuaDraft recovered=LuaDraft.restore(d.encode());check(recovered.insertion.previewLine==48,"review recovery");d.applyInsert();recovered.applyInsert();check(d.text().equals(recovered.text()),"same recovered bundle");check(d.text().endsWith(source)&&d.text().startsWith("local function move_box_solid"),"isolated helpers and unchanged source");
        String applied=d.text();d.history(false);check(d.text().equals(source)&&d.line()==1,"one undo restores entire bundle/cursor");d.history(true);check(d.text().equals(applied),"redo entire bundle");
        for(String s:new String[]{"move_box=1\r\n","local move_box_solid\r\n","print(move_box_axis)\r\n","local abs\r\n","function a(sgn) end\r\n","assert=print\r\n","#include other.p8\r\n"}){LuaDraft bad=proposal(s,"move_box");refused(bad::applyInsert);check(bad.text().equals(s),"conflict preserves source");}
        LuaDraft missing=proposal("\r\n","move_call");refused(missing::applyInsert);
        LuaDraft call=proposal(applied,"move_call");call.insertion.set(1,"pos_x");call.insertion.set(2,"pos_y");call.insertion.set(5,"btn(1) and 12 or 0");call.insertion.beginPreview();
        LuaDraft callRecovered=LuaDraft.restore(call.encode());call.applyInsert();callRecovered.applyInsert();check(call.text().equals(callRecovered.text()),"call review recovery");check(call.text().contains("pos_x,pos_y=move_box(\r\n pos_x,pos_y,"),"two result variables");call.history(false);check(call.text().equals(applied),"undo call only");
        call=proposal(applied,"move_call");call.insertion.set(1,"y");check(call.insertion.code().contains("y,y="),"intermediate fields remain renderable");final LuaDraft same=call;refused(same::applyInsert);
        call=proposal(applied,"move_call");call.insertion.set(1,"move_box");final LuaDraft overwritten=call;refused(overwritten::applyInsert);
        WorkshopCartridge c=TileProbeTest.cart(source);TileProbeTest.Port port=new TileProbeTest.Port();WorkshopSession s=new WorkshopSession(c,port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.CONTEXT);for(int n=0;n<item("move_box");n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.CONTEXT);check(s.codeDraft.insertion.screen==LuaInsert.Screen.PREVIEW,"controller review");s.act(Action.RIGHT);s.act(Action.TEST);check(port.saves==0&&port.launches==0,"review traps Start");s.act(Action.CANCEL);check(s.codeDraft.insertion.screen==LuaInsert.Screen.FIELDS,"back to fields");
        for(int n=0;n<5;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.codeDraft.text().contains("function move_box("),"controller inserts bundle");port.fail=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&s.codeDraft!=null,"save failure preserves draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry save/Test");s.act(Action.UNDO);check(Arrays.equals(c.bytes(),s.cart().bytes()),"project undo exact");
        if(args.length>0){Path p=Paths.get(args[0]);Files.createDirectories(p.getParent());Files.write(p,oracle().getBytes(StandardCharsets.UTF_8));}
        System.out.println("TileMotionTest: "+checks+" checks passed");
    }
    static String oracle(){
        String code=TileMotion.code("move_box","7","16","16","true")+TileMotion.code("free_move","7","16","16","false")
            +"checks=0\nfunction check(f,x,y,w,h,dx,dy,ex,ey,ehx,ehy)\n local a,b,c,d=f(x,y,w,h,dx,dy)\n assert(a==ex and b==ey and c==ehx and d==ehy,\"move case \"..checks..\": \"..a..\",\"..b)\n checks+=1\nend\n"
            +"for y=0,31 do for x=0,127 do mset(x,y,0) end end\nfor i=0,255 do fset(i,0) end\nfset(17,128)\nfor y=2,12 do mset(4,y,17) end\nfor x=0,3 do mset(x,10,17) end\n";
        String[] cases={
            "8,24,8,8,100,0,24,24,true,false","48,24,8,8,-40,0,40,24,true,false",
            "16,64,8,8,0,40,16,72,false,true","16,96,8,8,0,-40,16,88,false,true",
            "24,24,8,8,20,16,24,40,true,false","16,64,8,8,20,20,24,72,true,true",
            "0,0,8,8,2.25,3.5,2.25,3.5,false,false","23.75,24,8,8,.5,0,24,24,true,false",
            "23.75,24,8,8,.25,0,24,24,false,false","24,24,8,8,0x0.0001,0,24,24,true,false",
            "31.9999847412109375,24,0x0.0001,8,100,0,31.9999847412109375,24,true,false",
            "32,24,8,8,1,0,32,24,true,true","8,24,0,8,100,0,8,24,false,false",
            "0,0,8,8,-3,0,0,0,true,false","120,0,8,8,16,0,120,0,true,false",
            "8,0,8,8,0,0,8,0,false,false","8,0,8,8,0,-4,8,0,false,true",
            "8,0,8,8,16,0,24,0,false,false"};
        for(String c:cases)code+="check(move_box,"+c+")\n";
        code+="check(free_move,-100,24,8,8,200,0,24,24,true,false)\ncheck(free_move,120,0,8,8,16,0,136,0,false,false)\n"
            +"camera(40,50)\ncheck(move_box,8,24,8,8,100,0,24,24,true,false)\ncamera()\n"
            +"fset(17,64)\ncheck(move_box,8,24,8,8,100,0,108,24,false,false)\nfset(17,128)\n"
            +"function _draw()\n cls(1)\n print(\"movement: \"..checks..\"/22\",8,40,11)\n print(\"sweep / slide / fractions\",8,56,7)\nend\n";
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code;
    }
}
