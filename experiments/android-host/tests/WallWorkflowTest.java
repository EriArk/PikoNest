import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.nio.file.*;
import java.util.*;

/** Blank -> art/movement/contacts -> wall map -> recover/Test/save/export. */
public final class WallWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static WorldEditor e(WorkshopSession s){return s.game.world;}
    static void row(WorkshopSession s,int row){s.game.focus=row;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action: "+s.error);}
    static void recover(WorkshopSession s){byte[] bytes=s.game.encode();s.restoreGame(bytes);check(s.mode==Mode.GAME,"draft recovery: "+s.error);}
    static void wallHere(WorkshopSession s){s.act(Action.MENU);row(s,8);check(e(s).page==WorldEditor.Page.WALL,"wall action from map");}
    static void backToPaint(WorkshopSession s){e(s).page=WorldEditor.Page.PAINT;s.game.focus=0;}
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args.length>0?args[0]:".");
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl).withFlags(100,165).withTile(100,31,200);
            PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(blank,port);
            PlayModelTest.sprite(s,8,8);PlayModelTest.sprite(s,72,8);PlayModelTest.sprite(s,72,56);
            PlayModelTest.behavior(s,0);PlayModelTest.field(s,0,1);PlayModelTest.field(s,1,3);row(s,s.game.rows().length-2);
            PlayModelTest.behavior(s,1);PlayModelTest.field(s,2,1);row(s,s.game.rows().length-2);
            PlayModelTest.behavior(s,2);PlayModelTest.field(s,2,3);row(s,s.game.rows().length-2);
            s.game.configure(false);PlayModelTest.field(s,0,1);row(s,2);
            WorkshopCartridge before=s.cart();WorldWorkflowTest.open(s);row(s,2);e(s).point(4,0);
            TileWorkflowTest.create(s);for(int y=0;y<8;y++)for(int x=0;x<8;x++)e(s).art.paint(x,y);int wall=e(s).art.target;TileWorkflowTest.keep(s);
            for(int y=1;y<5;y++){e(s).point(4,y);s.act(Action.CONFIRM);}
            wallHere(s);check(e(s).summary().contains("5"),"all map occurrences reviewed");recover(s);s.act(Action.CANCEL);check(!e(s).world.walls[wall],"cancel doesn't change rule");
            wallHere(s);row(s,0);check(e(s).world.walls[wall]&&e(s).wallAt(4,0)&&e(s).wallAt(4,4),"all uses become walls");
            s.act(Action.UNDO);check(!e(s).world.walls[wall],"draft Undo wall rule");s.act(Action.REDO);check(e(s).world.walls[wall],"draft Redo wall rule");recover(s);
            s.act(Action.TEST);check(port.launched!=null,"draft launched");WorkshopCartridge candidate=new WorkshopCartridge(port.launched);same(before.bytes(),s.cart().bytes(),"Test keeps saved source exact");
            PlayModel p=PlayModel.read(candidate);check(p!=null&&p.world.walls[wall]&&p.world.wallCount()==1,"ordinary Lua reopens");same(candidate.bytes(),p.write(candidate).bytes(),"exact save/reopen");
            s.act(Action.MENU);row(s,3);port.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).world.walls[wall],"failed save retains walls");port.fail=false;s.act(Action.CANCEL);row(s,0);same(candidate.bytes(),s.cart().bytes(),"wall and map one save");s.act(Action.UNDO);same(before.bytes(),s.cart().bytes(),"one session Undo");s.act(Action.REDO);same(candidate.bytes(),s.cart().bytes(),"one session Redo");
            for(int t=0;t<256;t++)check(candidate.flags(t)==before.flags(t),"no flag ownership imposed");check(candidate.map().tile(100,31)==200,"hidden map preserved");
            for(int y=64;y<128;y++)for(int x=0;x<128;x++)check(candidate.sheetPixel(x,y)==before.sheetPixel(x,y),"shared memory preserved");
            WorldEditor hidden=new WorldEditor(candidate);hidden.world.tiles=false;WorkshopCartridge invisible=hidden.candidate();check(PlayModel.read(invisible).world.walls[wall]&&!invisible.code().contains(" map("),"hidden map keeps explicit collision");
            WorldEditor bad=new WorldEditor(candidate);bad.point(1,1);bad.brush.tile=wall;bad.paint();PlayModelTest.refused(()->bad.candidate());same(candidate.bytes(),bad.base.bytes(),"wall under start refuses without source write");
            PlayModel outside=p.copy();outside.elements.get(0).x=-1;PlayModelTest.refused(()->outside.write(candidate));
            for(String code:new String[]{candidate.code().replace("world_walls[mget(tx,ty)]","world_walls[mget(tx,ty)] or true"),candidate.code().replace("["+wall+"]=true","["+wall+"]=true,["+wall+"]=true"),candidate.code().replace("["+wall+"]=true","[0]=true")}){
                WorkshopCartridge changed=PlayModel.code(candidate,code);check(PlayModel.read(changed)==null&&!new GameCanvas(changed).blocked.isEmpty(),"modified collision Lua preserved/refused");
            }
            // Removing the last wall returns exactly to the previous no-wall recipe.
            WorldEditor clear=new WorldEditor(candidate);clear.world.walls[wall]=false;WorkshopCartridge cleared=clear.candidate();check(!cleared.code().contains("world_walls")&&!cleared.code().contains("world_move"),"last wall removes generated collision only");
            same(cleared.bytes(),PlayModel.read(cleared).write(cleared).bytes(),"no-wall round trip");
            // The ordinary world survives settings, audio, appearance and nested animation recovery.
            GameCanvas game=new GameCanvas(candidate);game.configure(false);game=GameCanvas.restore(game.encode(),candidate);check(game.settings.world.walls[wall],"rules recovery retains walls");
            game=new GameCanvas(candidate);game.selected=0;game.animate();game=GameCanvas.restore(game.encode(),candidate);check(game.animation.model.world.walls[wall],"animation recovery retains walls");
            WorldEditor richer=new WorldEditor(candidate);richer.model.startSound=8;richer.model.winSound=9;richer.model.musicStart=0;PlayAnimation animation=new PlayAnimation(new SpriteRegion(8,0,8,8));animation.mode=1;richer.model.elements.get(0).animation=animation;richer.world.width=32;richer.world.camera=1;richer.world.target=0;
            WorkshopCartridge rich=richer.candidate();check(PlayModel.read(rich).world.walls[wall]&&PlayModel.read(rich).musicStart==0,"camera/audio coexist");
            // Copy a wall tile into one cell; its new resource retains collision, including draft Test.
            s=TileWorkflowTest.open(candidate,new PlayModelTest.Port());e(s).point(4,2);s.act(Action.MENU);row(s,6);row(s,1);int copy=e(s).art.target;
            check(PlayModel.read(s.game.candidate(candidate)).world.walls[copy],"nested Test copy retains wall rule");recover(s);TileWorkflowTest.keep(s);
            WorkshopCartridge copied=s.game.candidate(candidate);check(PlayModel.read(copied).world.walls[copy]&&copied.map().tile(4,2)==copy&&copied.map().tile(4,1)==wall,"independent tile copy retains both rules");
            wallHere(s);row(s,0);check(!e(s).world.walls[copy]&&e(s).world.walls[wall],"copied cell becomes doorway independently");recover(s);
            // A wall type without visible pixels or map uses is still a reserved resource.
            PlayModel noObjects=new PlayModel();noObjects.world=new PlayWorld();noObjects.world.walls[1]=true;noObjects.world.walls[255]=true;
            WorkshopCartridge reserved=noObjects.write(blank);check(new GameCanvas(reserved).allocate(8,8).x==16,"sprite allocation reserves wall types too");s=TileWorkflowTest.open(reserved,new PlayModelTest.Port());TileWorkflowTest.create(s);check(e(s).art.target==2,"transparent wall type reserves its resource");
            check(PlayModel.read(reserved).elements.isEmpty()&&PlayModel.read(reserved).world.walls[255],"hero-free and tile 255 supported");
            WorkshopSession empty=TileWorkflowTest.open(blank,new PlayModelTest.Port());empty.act(Action.MENU);empty.game.focus=8;empty.act(Action.CONFIRM);check(empty.mode==Mode.ERROR&&e(empty).world.wallCount()==0,"tile zero refuses before mutation");
            if(nl.equals("\n")){
                Path out=root.resolve(".local/walls-work/evidence");Files.createDirectories(out);Files.write(out.resolve("maze.p8"),candidate.bytes());Files.write(out.resolve("maze.lua"),candidate.code().getBytes("UTF-8"));Files.write(out.resolve("hidden.p8"),invisible.bytes());Files.write(out.resolve("animated.p8"),rich.bytes());
            }
        }
        // This journal was emitted by unmodified beta-1 classes, before the wall implementation.
        WorkshopCartridge legacyBase=new WorkshopCartridge(Files.readAllBytes(root.resolve("experiments/android-host/assets/blank.p8")));
        GameCanvas legacy=GameCanvas.restore(Files.readAllBytes(root.resolve("experiments/android-host/tests/fixtures/world-v11.journal")),legacyBase);
        check(legacy.world.world.width==32&&legacy.world.tile(20,3)==2&&legacy.world.world.wallCount()==0,"real v11 draft data retained");
        check(legacy.focus==4&&legacy.world.rows()[legacy.focus].equals("Back"),"legacy focus keeps Back action");
        legacy.act(Action.CANCEL);legacy.act(Action.UNDO);check(legacy.world.tile(20,3)==0,"legacy Undo history upgraded");
        System.out.println("WallWorkflowTest: "+checks+" checks passed");
    }
}
