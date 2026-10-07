import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MapToolsTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge blank(){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\nfunction _draw()\r\n cls(1)\r\n map(0,0,0,0,16,16)\r\nend\r\n__gff__\r\n0001\r\n__future__\r\nopaque\r\n").getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        boolean fail;int saves,launches;byte[] saved;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;saved=b;}
        public void launch(byte[] b){launches++;saved=b;}
    }
    static void sameSections(WorkshopCartridge a,WorkshopCartridge b){
        P8Document before=P8Document.parse(a.bytes()),after=P8Document.parse(b.bytes());
        for(P8Document.Section sec:before.sections())if(!sec.name.equals("map"))check(Arrays.equals(before.body(before.uniqueSection(sec.name)),after.body(after.uniqueSection(sec.name))),"unrelated section "+sec.name);
    }
    public static void main(String[] args){
        WorkshopCartridge b=blank().withPixel(new SpriteRegion(0,64,128,64),3,3,14);
        MapChange rect=MapChange.rectangle(b.map(),10,8,2,3,17);check(rect.count==54,"inclusive reversed rectangle");
        WorkshopCartridge r=b.withMapChange(rect);sameSections(b,r);P8Map bm=b.map(),rm=r.map();
        for(int y=0;y<64;y++)for(int x=0;x<128;x++)check(rm.tile(x,y)==(x>=2&&x<=10&&y>=3&&y<=8?17:bm.tile(x,y)),"only rectangle changes");
        check(MapChange.rectangle(r.map(),2,3,10,8,17).count==0,"no-op rectangle");
        check(r.withMapChange(MapChange.rectangle(r.map(),2,3,10,8,17))==r,"no-op bytes and identity");
        MapChange fill=MapChange.fill(r.map(),4,5,1);check(fill.count==54,"fill connected tile IDs");
        WorkshopCartridge filled=r.withMapChange(fill);check(filled.map().tile(10,8)==1&&filled.map().tile(11,8)==0,"fill stays within boundary");sameSections(r,filled);
        check(MapChange.fill(r.map(),4,5,17).count==0,"same-tile fill is no-op");
        WorkshopCartridge diagonal=b.withTile(0,0,1).withTile(1,1,1).withTile(127,0,1).withTile(0,1,2);
        check(MapChange.fill(diagonal.map(),0,0,2).count==1,"no diagonal or row-wrap connectivity");
        MapChange full=MapChange.fill(b.map(),127,31,255);check(full.count==8191,"large fill crosses shared boundary and preserves a different tile");
        WorkshopCartridge all=b.withMapChange(full);check(all.map().tile(127,31)==255,"far edge serialized");P8Map am=all.map();
        for(int y=32;y<64;y++)for(int x=0;x<128;x++)check(am.tile(x,y)==(bm.tile(x,y)==0?255:bm.tile(x,y)),"shared fill follows matching tile values");
        check(all.withMapChange(MapChange.fill(all.map(),0,0,0)).map().tile(127,31)==0,"zero erases all connected tiles");
        refused(()->MapChange.rectangle(b.map(),0,0,2,64,1));refused(()->MapChange.fill(b.map(),0,64,1));
        refused(()->MapChange.fill(b.map(),-1,0,1));refused(()->b.map().withTiles(new boolean[8192],1));
        Port port=new Port();WorkshopSession s=new WorkshopSession(b,port);s.switchTool(3);s.act(Action.CHECK);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mapEditor.tool==MapEditor.Tool.RECTANGLE,"controller tool selection");s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.DOWN);
        check(s.mapEditor.preview(s.cart()).count==4&&port.saves==0,"live proposal, not saved");
        MapEditor restored=new MapEditor();restored.restore(s.mapEditor.encode(),b);check(restored.phase==1&&restored.preview(b).count==4,"recover second corner");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.DOWN);
        check(s.mapEditor.preview(b).count==4&&s.mapEditor.peekX==2&&s.mapEditor.x==1,"review panning cannot change geometry");
        s.mapEditor.point(30,20);check(s.mapEditor.peekX==30&&s.mapEditor.preview(b).count==4,"touch review only pans");
        restored.restore(s.mapEditor.encode(),b);check(restored.phase==2&&restored.peekX==30&&restored.peekY==20,"recover exact review");
        String journal=s.mapEditor.encode();refused(()->new MapEditor().restore(journal,b.withTile(5,5,9)));
        refused(()->new MapEditor().restore(journal+";bad",b));refused(()->restored.candidate(b.withTile(5,5,9)));
        s.act(Action.TEST);s.act(Action.NEXT);s.act(Action.MENU);s.act(Action.CONTEXT);s.act(Action.REDO);
        check(s.tool==3&&port.launches==0&&port.saves==0&&s.mapEditor.phase==2,"modal shortcuts cannot commit or discard proposal");
        port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.mapEditor.phase==2&&s.undoCount()==0,"failed save retains proposal and history");
        s.act(Action.CONFIRM);port.fail=false;s.act(Action.CONFIRM);check(s.undoCount()==1&&port.saves==1&&!s.mapEditor.pending(),"one durable bulk edit");
        s.act(Action.UNDO);check(Arrays.equals(b.bytes(),s.cart().bytes()),"one undo restores exact source");s.act(Action.REDO);check(s.cart().map().tile(1,1)==1,"redo all cells");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CONFIRM);s.act(Action.CANCEL);check(s.mapEditor.phase==1,"review back to corner");
        s.act(Action.CANCEL);check(!s.mapEditor.pending()&&s.undoCount()==1,"cancel no history mutation");
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.mapEditor.choosingTool,"menu route without shoulder buttons");
        s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.mapEditor.phase==2&&s.mapEditor.tool==MapEditor.Tool.FILL,"fill goes to preview");
        int previousSaves=port.saves;MapEditor recoveredFill=new MapEditor();recoveredFill.restore(s.mapEditor.encode(),s.cart());check(recoveredFill.phase==2,"recover fill");
        s.act(Action.UNDO);check(!s.mapEditor.pending()&&port.saves==previousSaves&&s.undoCount()==1,"Y cancels proposal, not saved history");
        s.mapEditor.x=0;s.mapEditor.y=0;s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.undoCount()==1&&port.saves==previousSaves,"no-op preview creates no save or history");
        s.mapEditor.y=32;s.act(Action.CONFIRM);check(s.mode!=WorkshopSession.Mode.ERROR&&s.mapEditor.phase==2,"shared seed prepares a review without writing");
        System.out.println("MapToolsTest: "+checks+" checks passed");
    }
}
