import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.file.*;

/** Tile art from map context: shared uses, independent resources and atomic recovery. */
public final class TileWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static WorldEditor e(WorkshopSession s){return s.game.world;}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action: "+s.error);}
    static WorkshopSession open(WorkshopCartridge c,PlayModelTest.Port p){WorkshopSession s=PlayModelTest.session(c,p);WorldWorkflowTest.open(s);row(s,2);return s;}
    static void create(WorkshopSession s){s.act(Action.CONTEXT);s.act(Action.CONTEXT);row(s,0);check(e(s).art.mode==TileArt.Mode.NEW,"new tile from picker");}
    static void recover(WorkshopSession s){byte[] data=s.game.encode();s.restoreGame(data);check(s.mode==Mode.GAME&&e(s)!=null,"recover: "+s.error);}
    static void keep(WorkshopSession s){s.act(Action.CANCEL);row(s,0);recover(s);row(s,0);check(e(s).art==null&&e(s).page==WorldEditor.Page.PAINT,"keep returns to map");}
    static void options(WorkshopSession s,int n){s.act(Action.MENU);row(s,n);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl);
            PlayModelTest.Port p=new PlayModelTest.Port();WorkshopSession s=open(blank,p);e(s).point(4,5);create(s);
            int tile=e(s).art.target;check(tile==1,"first nonzero free tile");
            s.act(Action.TEST);check(s.mode==Mode.ERROR&&p.launched==null,"empty tile Test refused");s.act(Action.CANCEL);
            s.act(Action.CONTEXT);s.act(Action.RIGHT);recover(s);check(e(s).art.page==TileArt.Page.PALETTE&&s.game.focus==12,"palette cursor recovered");s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CONFIRM);
            check(e(s).art.pixel(0,0)==12&&e(s).art.pixel(1,0)==12,"controller drawing");
            e(s).art.beginStroke(0,2);e(s).art.strokePoint(7,2);recover(s);check(e(s).art.strokeActive(),"interrupted stroke recovered");s.act(Action.UNDO);for(int x=0;x<8;x++)check(e(s).art.pixel(x,2)==0,"whole stroke Undo");s.act(Action.REDO);for(int x=0;x<8;x++)check(e(s).art.pixel(x,2)==12,"whole stroke Redo");
            s.act(Action.CANCEL);row(s,2);check(e(s).art.eraser,"eraser available");s.act(Action.CONFIRM);check(e(s).art.pixel(7,2)==0,"eraser clears pixel");s.act(Action.UNDO);check(e(s).art.pixel(7,2)==12,"eraser Undo");
            s.act(Action.TEST);WorkshopCartridge tested=new WorkshopCartridge(p.launched);same(blank.bytes(),s.cart().bytes(),"Test isolates source");check(tested.map().tile(4,5)==tile&&tested.sheetPixel(8,2)==12,"Test includes nested tile and cell");check(PlayModel.read(tested).elements.isEmpty(),"no invented object");
            keep(s);same(tested.bytes(),s.game.candidate(blank).bytes(),"Keep preserves tested result");check(e(s).changedTiles()==1,"one changed resource");
            s.act(Action.UNDO);check(e(s).tile(4,5)==0&&e(s).sheetPixel(8,0)==0,"world Undo reverses art and placement");s.act(Action.REDO);recover(s);same(tested.bytes(),s.game.candidate(blank).bytes(),"world Redo and recovery exact");
            e(s).point(5,5);create(s);check(e(s).art.target==2,"pending tiles reserve resources");s.act(Action.CONFIRM);s.act(Action.CANCEL);row(s,4);check(e(s).art==null&&e(s).tile(5,5)==0,"discard leaves no cell");create(s);check(e(s).art.target==2,"discard releases allocation");s.act(Action.CONFIRM);keep(s);
            e(s).point(6,5);s.act(Action.CONFIRM);check(e(s).tile(6,5)==2,"kept tile is brush");
            WorkshopCartridge candidate=s.game.candidate(blank);options(s,3);p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).draft(),"failed save retains tile draft");p.fail=false;s.act(Action.CANCEL);row(s,0);same(candidate.bytes(),s.cart().bytes(),"save art cells camera atomically");check(PlayModel.read(s.cart()).elements.isEmpty(),"reopened map without objects");s.act(Action.UNDO);same(blank.bytes(),s.cart().bytes(),"session Undo exact source");s.act(Action.REDO);same(candidate.bytes(),s.cart().bytes(),"session Redo exact candidate");
            WorkshopCartridge shared=candidate.withFlags(2,165).withTile(70,0,2).withPixel(new SpriteRegion(0,64,8,8),1,1,9);
            PlayModel m=PlayModel.read(shared);m.startSound=8;m.musicStart=0;PlayModel.Element object=new PlayModel.Element();object.sx=16;object.sy=0;object.w=object.h=8;object.x=24;object.y=32;object.animation=new PlayAnimation(new SpriteRegion(16,0,8,8));m.elements.add(object);shared=m.write(shared);
            s=open(shared,new PlayModelTest.Port());e(s).point(5,5);options(s,6);check(e(s).usesSummary().contains("Map cells: 3")&&e(s).usesSummary().contains("Objects: 1")&&e(s).usesSummary().contains("Frames: 1"),"review includes hidden map and overlapping object/frame");recover(s);row(s,1);check(e(s).art.mode==TileArt.Mode.COPY&&e(s).art.flags==165,"independent copy retains flags");int copied=e(s).art.target;e(s).art.color=8;e(s).art.paint(0,0);keep(s);WorkshopCartridge copy=s.game.candidate(shared);check(copy.map().tile(5,5)==copied&&copy.map().tile(6,5)==2&&copy.map().tile(70,0)==2,"only selected cell repointed");check(copy.flags(copied)==165&&copy.flags(2)==165,"copy flags ordinary data");check(copy.sheetPixel(16,0)==shared.sheetPixel(16,0)&&PlayModel.read(copy).elements.get(0).sx==16,"copy leaves sprite and frame uses");
            recover(s);same(copy.bytes(),s.game.candidate(shared).bytes(),"copy flags and pixels recover");check(PlayModel.read(copy).startSound==8&&PlayModel.read(copy).musicStart==0,"audio binding preserved");
            for(int y=64;y<128;y++)for(int x=0;x<128;x++)check(copy.sheetPixel(x,y)==shared.sheetPixel(x,y),"shared lower memory unchanged");
            s=open(shared,new PlayModelTest.Port());e(s).point(5,5);options(s,6);row(s,0);check(e(s).art.mode==TileArt.Mode.ALL,"explicit edit all");e(s).art.color=9;e(s).art.paint(0,0);keep(s);WorkshopCartridge all=s.game.candidate(shared);check(all.sheetPixel(16,0)==9&&all.map().tile(5,5)==2&&all.map().tile(70,0)==2,"shared resource changed without remapping");s.act(Action.UNDO);same(shared.bytes(),s.game.candidate(shared).bytes(),"edit-all Undo exact");
            WorkshopCartridge lower=shared.withTile(0,0,128).withFlags(128,73);s=open(lower,new PlayModelTest.Port());options(s,6);s.game.focus=0;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).art==null,"lower in-place refuses before mutation");s.act(Action.CANCEL);row(s,1);int upper=e(s).art.target;check(e(s).art.pixel(1,1)==9,"lower tile copied exactly");keep(s);WorkshopCartridge lifted=s.game.candidate(lower);check(lifted.map().tile(0,0)==upper&&lifted.flags(upper)==73,"lower copy placed and flags retained");for(int y=64;y<128;y++)for(int x=0;x<128;x++)check(lifted.sheetPixel(x,y)==lower.sheetPixel(x,y),"lower copy preserves shared memory");
            s=open(blank,new PlayModelTest.Port());s.act(Action.MENU);s.game.focus=6;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).art==null,"empty tile cannot edit all");s.act(Action.CANCEL);
            WorkshopCartridge full=blank;for(int n=1;n<128;n++)full=full.withFlags(n,1);s=open(full,new PlayModelTest.Port());s.act(Action.CONTEXT);s.act(Action.CONTEXT);s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).art==null,"full reserved sheet refuses before mutation");same(full.bytes(),s.cart().bytes(),"allocation failure preserves cart");
            s=open(blank.withTile(100,31,1).withFlags(2,1),new PlayModelTest.Port());create(s);check(e(s).art.target==3,"hidden map and flags reserve transparent tiles");s.act(Action.CONFIRM);byte[] journal=s.game.encode();PlayModelTest.refused(()->GameCanvas.restore(journal,blank.withTile(1,1,6)));
            keep(s);for(int n=0;n<40;n++){e(s).point(n%16,n/16);s.act(Action.CONFIRM);}e(s).point(10,10);create(s);for(int n=0;n<40;n++){e(s).art.color=1+n%15;e(s).art.paint(n%8,n/8);}s.act(Action.CANCEL);recover(s);check(e(s).art.page==TileArt.Page.MENU,"tile menu and bounded histories recover");check(s.game.encode().length<3000000,"combined journal within recovery limit");WorkshopCartridge longDraft=s.game.candidate(s.cart());recover(s);same(longDraft.bytes(),s.game.candidate(s.cart()).bytes(),"large draft recovers exactly");
            if(args.length>0&&nl.equals("\n")){Path out=Paths.get(args[0]);Files.createDirectories(out);Files.write(out.resolve("tile-created.p8"),candidate.bytes());Files.write(out.resolve("tile-independent.p8"),copy.bytes());Files.write(out.resolve("tile-shared.p8"),all.bytes());}
        }
        System.out.println("TileWorkflowTest: "+checks+" checks passed");
    }
}
