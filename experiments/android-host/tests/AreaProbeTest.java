import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public final class AreaProbeTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe area accepted");}catch(IllegalArgumentException expected){checks++;}}
    static int item(){for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("solid_box"))return n;throw new AssertionError();}
    static LuaDraft proposal(String source){LuaDraft d=TileProbeTest.proposal(source);d.insertion.choose(item());d.insertion.screen=LuaInsert.Screen.FIELDS;return d;}
    public static void main(String[] args)throws Exception{
        String source="function _draw()\r\n cls(1)\r\nend\r\n";
        LuaDraft d=proposal(source);d.point(1,1);d.insertion.set(1,"7");d.insertion.set(2,"16");d.insertion.set(3,"16");d.insertion.set(4,"false");
        d.insertion.beginPreview();d.insertion.previewLine=12;
        LuaDraft recovered=LuaDraft.restore(d.encode());check(recovered.insertion.previewLine==12&&recovered.insertion.screen==LuaInsert.Screen.PREVIEW,"recover review and position");
        check(recovered.text().equals(source),"review has no source mutation");d.applyInsert();recovered.applyInsert();check(recovered.text().equals(d.text()),"same recovered insertion");
        check(d.text().endsWith(source)&&d.text().startsWith("function solid_box(x,y,w,h)\r\n"),"top-level prefix preserves source");
        String applied=d.text();d.history(false);check(d.text().equals(source)&&d.line()==1,"one undo including cursor");d.history(true);check(d.text().equals(applied),"one redo");
        for(String conflict:new String[]{"solid_box=1\r\n","local ceil\r\n","min=function() end\r\n","function a(min) end\r\n","#include game.lua\r\n"}){LuaDraft bad=proposal(conflict);bad.insertion.beginPreview();refused(bad::applyInsert);check(bad.text().equals(conflict)&&bad.insertion!=null,"rejected review retains original");}
        LuaDraft bad=proposal(source);bad.insertion.beginPreview();bad.insertion.previewLine=-1;byte[] corrupt=bad.encode();refused(()->LuaDraft.restore(corrupt));
        bad.insertion.previewLine=100000;byte[] excessive=bad.encode();refused(()->LuaDraft.restore(excessive));
        WorkshopCartridge cart=TileProbeTest.cart(source);TileProbeTest.Port port=new TileProbeTest.Port();WorkshopSession s=new WorkshopSession(cart,port);
        s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.CONTEXT);for(int n=0;n<item();n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.DOWN);s.act(Action.RIGHT);s.act(Action.CONTEXT);check(s.codeDraft.insertion.screen==LuaInsert.Screen.PREVIEW,"X opens full review");
        s.act(Action.RIGHT);check(s.codeDraft.insertion.previewLine==6,"D-pad pages without shoulders");s.act(Action.TEST);check(port.saves==0&&port.launches==0,"Start cannot commit review");
        s.act(Action.CANCEL);check(s.codeDraft.insertion.screen==LuaInsert.Screen.FIELDS&&s.codeDraft.insertion.value(1).equals("1"),"back keeps fields");
        for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.insertion.screen==LuaInsert.Screen.PREVIEW&&s.codeDraft.text().equals(source),"apply button reviews before insert");
        s.act(Action.CONFIRM);check(s.codeDraft.text().startsWith("function solid_box"),"explicit insertion");
        port.fail=true;s.act(Action.TEST);check(port.saves==0&&s.mode==WorkshopSession.Mode.ERROR,"failed storage retains draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry saved launch");s.act(Action.UNDO);check(Arrays.equals(cart.bytes(),s.cart().bytes()),"project undo exact");
        if(args.length>0){Path path=Paths.get(args[0]);Files.createDirectories(path.getParent());Files.write(path,oracle().getBytes(StandardCharsets.UTF_8));}
        System.out.println("AreaProbeTest: "+checks+" checks passed");
    }
    static String oracle(){
        String code="checks=0\nfunction check(ok,why)\n assert(ok,why)\n checks+=1\nend\n"
            +TileProbe.areaCode("free_box","7","16","16","false")
            +TileProbe.areaCode("wall_box","7","16","16","true")
            +TileProbe.areaCode("large_box","7","128","64","false")
            +TileProbe.areaCode("six_box","6","16","16","false")
            +"for y=0,63 do for x=0,127 do mset(x,y,0) end end\nfor i=0,255 do fset(i,0) end\nfset(17,128) mset(4,5,17)\n";
        String[][] cases={
            {"32,40,8,8","true"},{"24,40,8,8","false"},{"24,40,8.0000152587890625,8","true"},
            {"32,32,8,8","false"},{"32,32,8,8.0000152587890625","true"},{"40,40,8,8","false"},
            {"16,24,40,40","true"},{"0,0,0,8","false"},{"32,40,-1,8","false"},{"32,40,8,-1","false"},
            {"32.5,40.5,0x0.0001,0x0.0001","true"},{"-4,40,40,8","true"},{"-40,40,40,8","false"},
            {"128,0,8,8","false"},{"0,128,8,8","false"},{"0,0,32767,32767","true"},
            {"32760,0,16,16","false"},{"-32768,40,32767,8","false"},{"120,40,32767,8","false"},
            {"0,0,16,16","false"},{"127.5,127.5,1,1","false"}};
        int n=0;for(String[] c:cases)code+="check(free_box("+c[0]+")=="+c[1]+",\"free"+(n++)+"\")\n";
        for(String c:new String[]{"-1,0,8,8","128,0,8,8","0,128,8,8","120,40,32767,8","32760,0,16,16","127.5,127.5,1,1"})code+="check(wall_box("+c+"),\"boundary\")\n";
        code+="check(not wall_box(-1,0,0,8),\"empty before boundary\")\ncheck(not wall_box(0,0,128,128)==false,\"internal tile\")\n"
            +"camera(30,50)\ncheck(free_box(32,40,8,8),\"world coordinates\")\ncamera()\n"
            +"fset(17,64)\ncheck(not free_box(32,40,8,8),\"chosen bit only\")\ncheck(six_box(32,40,8,8),\"flag six\")\n"
            +"fset(0,128)\ncheck(free_box(0,0,1,1),\"tile zero\")\ncheck(not free_box(-8,0,8,8),\"empty clip\")\nfset(0,0)\n"
            +"fset(255,128) mset(127,63,255)\ncheck(large_box(1016,504,8,8),\"shared last tile\")\ncheck(not large_box(1024,504,8,8),\"last boundary\")\n"
            +"function _draw()\n cls(1)\n print(\"area checks: \"..checks..\"/36\",8,40,11)\n print(\"edges / overlap / shared map\",8,56,7)\nend\n";
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code;
    }
}
