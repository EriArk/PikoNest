import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class SharedEditingTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("Unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge blank(String nl){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com"+nl+"version 43"+nl+"__lua__"+nl+"function _draw()"+nl+" cls(1)"+nl+"end"+nl+"__future__"+nl+"opaque"+nl).getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] launched;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;}
        public void launch(byte[] b){launches++;launched=b;}
    }
    static WorkshopSession canvas(WorkshopCartridge c,Port p,SpriteRegion r){WorkshopSession s=new WorkshopSession(c,p);s.tool=2;s.browsingSprites=false;s.region=r;s.cursorX=s.cursorY=0;s.mode=Mode.CANVAS;return s;}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge b=blank(nl);
            for(int y:new int[]{64,65,127})for(int x:new int[]{0,1,126,127}){
                WorkshopCartridge a=b.withPixel(new SpriteRegion(x,y,1,1),0,0,13);
                SharedEdit e=new SharedEdit(b,a,"Pixel","CANVAS");int offset=(y-64)*64+x/2;
                check(a.map().tile(offset%128,32+offset/128)==((x%2==0)?13:208),"official packed nibbles across row/edge boundaries");
                check(e.sharedCells==1&&e.pixels==1,"one byte may contain one changed nibble");
                check(Arrays.equals(SharedEdit.restore(e.encode(),b).after.bytes(),a.bytes()),"sparse journal preserves candidate bytes");
            }
            WorkshopCartridge a=b.withMapChange(MapChange.rectangle(b.map(),126,31,127,32,0xab));
            SharedEdit e=new SharedEdit(b,a,"Map rectangle","NAVIGATE");
            check(e.cells==4&&e.sharedCells==2&&e.pixels==4,"rectangle straddles independent/shared map rows");
            check(a.sheetPixel(124,65)==11&&a.sheetPixel(125,65)==10,"map low nibble first in gfx");
            check(Arrays.equals(SharedEdit.restore(e.encode(),b).after.bytes(),a.bytes()),"map/gfx section append ordering survives recovery");
            refused(()->SharedEdit.restore(e.encode(),b.withPixel(new SpriteRegion(0,0,1,1),0,0,2)));
            byte[] extra=Arrays.copyOf(e.encode(),e.encode().length+1);refused(()->SharedEdit.restore(extra,b));
            refused(()->new SharedEdit(b,new WorkshopCartridge(new String(a.bytes(),StandardCharsets.UTF_8).replace("cls(1)","cls(2)").getBytes(StandardCharsets.UTF_8)),"Other code","CANVAS"));
            WorkshopCartridge filled=b.withMapChange(MapChange.fill(b.map(),0,31,255));
            check(filled.map().tile(127,63)==255&&filled.sheetPixel(127,127)==15,"fill crosses whole connected 128x64 area");
            check(MapChange.fill(b.map(),0,0,255).count==8192,"whole map fill is counted once per cell");
        }
        WorkshopCartridge b=blank("\n");Port p=new Port();WorkshopSession s=canvas(b,p,new SpriteRegion(0,56,16,16));
        s.drawTool=DrawTool.FILLED_RECTANGLE;s.color=12;s.cursorY=7;s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.DOWN);
        check(p.saves==0&&s.pendingStroke(),"navigation changes only the shape draft");
        s.act(Action.CONFIRM);check(s.mode==Mode.SHARED&&s.sharedEdit.pixels==4&&s.sharedEdit.sharedCells==1&&p.saves==0&&s.undoCount()==0,"cross-boundary shape waits for review: "+s.mode+" / "+s.error+" / "+(s.sharedEdit==null?"none":s.sharedEdit.pixels+":"+s.sharedEdit.sharedCells));
        byte[] proposed=s.sharedEdit.after.bytes(),journal=s.sharedEdit.encode();s.act(Action.NEXT);s.act(Action.MENU);s.act(Action.REGION);
        check(s.mode==Mode.SHARED&&s.tool==2&&p.saves==0,"shared review traps unrelated operations");
        s.act(Action.TEST);check(p.launches==1&&Arrays.equals(proposed,p.launched)&&Arrays.equals(b.bytes(),s.cart().bytes()),"Test gets isolated candidate without publishing or history");
        WorkshopSession reopened=canvas(b,new Port(),s.selection());reopened.restoreShared(journal);reopened.act(Action.CONTEXT);
        SharedEdit restored=SharedEdit.restore(reopened.sharedEdit.encode(),b);check(restored.mapView,"exact review page survives recovery");
        reopened.act(Action.CANCEL);check(reopened.mode==Mode.CANVAS&&reopened.sharedEdit==null&&Arrays.equals(reopened.cart().bytes(),b.bytes()),"Cancel changes neither view's data");
        p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.sharedEdit!=null&&s.undoCount()==0&&Arrays.equals(b.bytes(),s.cart().bytes()),"failed save retains candidate and history");
        s.act(Action.CANCEL);check(s.mode==Mode.SHARED,"dismiss error returns to review");p.fail=false;s.act(Action.CONFIRM);
        check(s.mode==Mode.CANVAS&&s.sharedEdit==null&&s.undoCount()==1&&p.saves==1&&Arrays.equals(s.cart().bytes(),proposed),"one Apply writes entire proposed edit");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),b.bytes()),"Undo restores both resources exactly");s.act(Action.REDO);check(Arrays.equals(s.cart().bytes(),proposed),"Redo restores both resources exactly");
        for(DrawTool t:new DrawTool[]{DrawTool.BRUSH,DrawTool.ERASER,DrawTool.FILL,DrawTool.LINE,DrawTool.RECTANGLE,DrawTool.FILLED_RECTANGLE,DrawTool.OVAL,DrawTool.FILLED_OVAL}){
            Port q=new Port();WorkshopCartridge src=t==DrawTool.ERASER?b.withPixel(new SpriteRegion(0,64,8,8),0,0,7):b;
            WorkshopSession v=canvas(src,q,new SpriteRegion(0,64,8,8));v.drawTool=t;v.color=8;v.act(Action.CONFIRM);
            if(v.pendingStroke()){v.act(Action.RIGHT);v.act(Action.DOWN);v.act(Action.CONFIRM);}
            check(v.mode==Mode.SHARED&&q.saves==0,t+" uses same shared review");v.act(Action.UNDO);check(Arrays.equals(src.bytes(),v.cart().bytes()),t+" draft Undo cancels without reverting older history");
        }
        Port q=new Port();WorkshopSession map=new WorkshopSession(b,q);map.switchTool(3);map.mapEditor.y=63;map.mapEditor.x=127;map.mapEditor.tile=0xa5;map.act(Action.CONFIRM);
        check(map.mode==Mode.SHARED&&map.sharedEdit.sharedCells==1,"brush reaches final map cell through controller action");
        map.act(Action.CONFIRM);check(map.cart().sheetPixel(126,127)==5&&map.cart().sheetPixel(127,127)==10,"brush writes exact shared nibbles");map.act(Action.UNDO);check(Arrays.equals(map.cart().bytes(),b.bytes()),"map brush Undo restores omitted sections too");
        map.mapEditor.tool=MapEditor.Tool.RECTANGLE;map.mapEditor.y=31;map.mapEditor.x=0;map.act(Action.CONFIRM);map.act(Action.DOWN);map.act(Action.CONFIRM);
        MapEditor old=new MapEditor();old.restore(map.mapEditor.encode(),b);check(old.y==32&&old.phase==2,"expanded map draft restores its second corner");
        map.act(Action.CONFIRM);check(map.mode==Mode.SHARED&&!map.mapEditor.pending(),"rectangle progresses from geometry to shared review");map.act(Action.CANCEL);check(q.saves==2,"cancel adds no durable write");
        WorkshopSession region=canvas(b,new Port(),new SpriteRegion(0,0,8,8));region.act(Action.REGION);for(int i=0;i<20;i++){region.act(Action.DOWN);region.act(Action.RIGHT);}region.act(Action.CONFIRM);region.act(Action.CONFIRM);
        check(region.selection().x==120&&region.selection().y==120&&region.selection().width==8,"full sprite sheet selection available without touch");
        System.out.println("SharedEditingTest: "+checks+" checks passed");
    }
}
