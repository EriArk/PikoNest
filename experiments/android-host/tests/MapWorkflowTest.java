import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MapWorkflowTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("accepted unsafe edit");}catch(IllegalArgumentException expected){checks++;}}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static String text(byte[] b){return new String(b,StandardCharsets.UTF_8);}
    static String header="pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\nfunction _draw()\r\n cls(1)\r\nend\r\n";
    static String zeros(){char[] row=new char[256];Arrays.fill(row,'0');return new String(row);}
    static class Port implements WorkshopSession.Port {
        boolean fail;int saves,launches;byte[] saved;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;saved=b;}
        public void launch(byte[] b){launches++;check(Arrays.equals(saved,b),"launch saved bytes");}
    }
    public static void main(String[] args){
        WorkshopCartridge base=new WorkshopCartridge(bytes(header));
        check(base.map().tile(127,31)==0&&base.map().tile(127,63)==0,"missing map and shared gfx are zero");
        check(base.withTile(100,20,0)==base,"missing zero write is byte-exact no-op");
        WorkshopCartridge edge=base.withTile(127,31,255);
        check(edge.map().tile(127,31)==255&&edge.map().tile(0,30)==0,"far edge grows complete rows");
        String source=text(edge.bytes());check(source.startsWith(header)&&source.endsWith("ff\r\n"),"append without rewriting Lua, CRLF inherited");
        check(source.substring(source.indexOf("__map__\r\n")+9).split("\r\n").length==32,"32 serialized rows");
        refused(()->base.withTile(0,32,1));refused(()->base.withTile(128,0,1));refused(()->base.withTile(0,64,1));
        refused(()->base.withTile(0,0,256));refused(()->base.withTile(-1,0,0));
        String row="AB"+zeros().substring(2);
        String framed=header+"__map__\r\n"+row+"\r\n__future__\r\nopaque payload\r\n";
        WorkshopCartridge mixed=new WorkshopCartridge(bytes(framed));
        check(mixed.withTile(0,0,171)==mixed,"uppercase no-op preserved");
        check(text(mixed.withTile(1,0,254).bytes()).equals(framed.replace(row,"ABfe"+row.substring(4))),"only two target digits change");
        WorkshopCartridge noEnding=new WorkshopCartridge(bytes(header+"__map__\r\n"+zeros()));
        check(!text(noEnding.withTile(1,0,1).bytes()).endsWith("\n"),"existing final newline absence retained");
        check(noEnding.withTile(0,1,2).map().tile(0,1)==2,"grow final row without newline");
        WorkshopCartridge lf=new WorkshopCartridge(bytes((header+"__map__\r\n"+zeros()+"\r\n").replace("\r\n","\n")));
        check(!text(lf.withTile(2,1,3).bytes()).contains("\r"),"LF retained");
        String[] malformed={"00\n",zeros()+"x",zeros()+"\n__map__\n"+zeros(),zeros().replace('0','z'),String.join("\n",Collections.nCopies(33,zeros()))};
        for(String bad:malformed){WorkshopCartridge opened=new WorkshopCartridge(bytes(header+"__map__\n"+bad));refused(()->opened.withTile(0,0,1));check(opened.code().contains("_draw"),"unsupported map does not prevent code use");}
        WorkshopCartridge shared=base.withPixel(new SpriteRegion(0,64,128,64),0,0,11).withPixel(new SpriteRegion(0,64,128,64),1,0,10)
            .withPixel(new SpriteRegion(0,64,128,64),126,63,13).withPixel(new SpriteRegion(0,64,128,64),127,63,12);
        check(shared.map().tile(0,32)==171&&shared.map().tile(127,63)==205,"shared map nibble order and endpoints");
        byte[] gfx=P8Document.parse(shared.bytes()).body(P8Document.parse(shared.bytes()).uniqueSection("gfx"));
        P8Document changed=P8Document.parse(shared.withTile(12,3,255).bytes());
        check(Arrays.equals(gfx,changed.body(changed.uniqueSection("gfx"))),"map write never touches shared graphics");
        Port port=new Port();WorkshopSession s=new WorkshopSession(base,port);s.switchTool(3);s.act(Action.RIGHT);s.act(Action.DOWN);
        check(port.saves==0&&s.mapEditor.x==1&&s.mapEditor.y==1,"navigation never paints");
        s.act(Action.CONTEXT);s.act(Action.RIGHT);s.act(Action.TEST);s.act(Action.UNDO);s.act(Action.NEXT);
        check(port.saves==0&&port.launches==0&&s.tool==3&&s.mapEditor.picking,"picker traps external shortcuts");
        s.act(Action.CANCEL);check(s.mapEditor.tile==1,"cancel discards selection");
        s.act(Action.CONTEXT);s.act(Action.RIGHT);s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(port.saves==1&&s.cart().map().tile(1,1)==2&&s.undoCount()==1,"choose then paint one history operation");
        s.act(Action.CONFIRM);check(port.saves==1&&s.undoCount()==1,"same tile no history spam");
        s.act(Action.UNDO);check(s.cart().map().tile(1,1)==0&&Arrays.equals(base.bytes(),s.cart().bytes()),"undo removes newly appended section");
        s.act(Action.REDO);s.act(Action.TEST);check(port.launches==1,"Test exact saved map");
        port.fail=true;s.act(Action.RIGHT);s.act(Action.CONFIRM);
        check(s.mode==WorkshopSession.Mode.ERROR&&s.cart().map().tile(2,1)==0&&s.undoCount()==1,"failed write preserves document and history");
        port.fail=false;s.act(Action.CONFIRM);s.mapEditor.y=32;s.act(Action.CONFIRM);
        check(s.mode==WorkshopSession.Mode.SHARED&&s.undoCount()==1,"shared write waits for explicit review");s.act(Action.CANCEL);
        s.mapEditor.move(1000,1000);check(s.mapEditor.x==127&&s.mapEditor.y==63&&s.mapEditor.left()==112&&s.mapEditor.top()==48,"scroll bounds");
        s.mapEditor.tile=0;s.mapEditor.y=1;s.mapEditor.x=1;s.act(Action.CONFIRM);check(s.cart().map().tile(1,1)==0,"tile zero erases");
        s.switchTool(4);check(s.tool==0,"four-tool wrap");s.switchTool(-1);check(s.tool==3,"reverse four-tool wrap");
        LuaInsert insert=new LuaInsert();insert.choose(19);check(insert.code().equals("map(0,0,0,0,16,16)\n"),"ordinary six-argument map insertion");
        String line="  map(0, 0, 0, 0, 16, 16) -- keep";LuaCall call=LuaCall.parse(line,0,line.length());call.form.set(4,"8");
        check(call.replacement(line,call.form).equals(line.replace(", 16, 16)",", 8, 16)")),"map field edit preserves whitespace and comment");
        refused(()->LuaCall.parse("map()",0,5));
        String shadow="function map() end\nmap(0,0,0,0,16,16)";refused(()->LuaCall.parse(shadow,shadow.indexOf('\n')+1,shadow.length()));
        LuaDraft draft=new LuaDraft(base,3);draft.beginInsert();draft.insertion.choose(19);draft.insertion.screen=LuaInsert.Screen.FIELDS;draft.applyInsert();
        check(draft.text().contains("map(0,0,0,0,16,16)"),"draft insertion");draft.history(false);check(!draft.dirty(),"one undo removes whole map call");
        System.out.println("MapWorkflowTest: "+checks+" checks passed");
    }
}
