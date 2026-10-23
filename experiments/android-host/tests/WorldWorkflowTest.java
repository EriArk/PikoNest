import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.file.*;

/** Controller journey, world coordinates, resource preservation and atomic recovery. */
public final class WorldWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action: "+s.error);}
    static WorldEditor e(WorkshopSession s){return s.game.world;}
    static void open(WorkshopSession s){String[] rows=s.game.rows();for(int n=0;n<rows.length;n++)if(rows[n].equals("World & camera")){row(s,n);return;}throw new AssertionError("world entry");}
    static void save(WorkshopSession s){s.act(Action.MENU);row(s,0);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl).withPixel(new SpriteRegion(8,0,8,8),0,0,11).withPixel(new SpriteRegion(0,64,8,8),2,2,9).withTile(70,0,6);
            PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(blank,port);open(s);
            row(s,0);s.act(Action.RIGHT);check(e(s).world.width==32,"world width in screens");s.restoreGame(s.game.encode());check(e(s).editing,"field recovery");s.act(Action.CANCEL);check(e(s).world.width==16,"revert field");row(s,0);s.act(Action.RIGHT);s.act(Action.CONFIRM);row(s,1);s.act(Action.RIGHT);s.act(Action.CONFIRM);
            row(s,2);e(s).point(18,17);e(s).brush.tile=1;s.act(Action.CONFIRM);check(e(s).tile(18,17)==1,"paint a tile beyond first screen");s.act(Action.CONTEXT);s.act(Action.RIGHT);s.restoreGame(s.game.encode());check(e(s).brush.picking&&e(s).brush.choice==2,"picker recovery");s.act(Action.CANCEL);check(e(s).brush.tile==1,"picker cancel retains brush");
            s.act(Action.UNDO);check(e(s).tile(18,17)==0,"draft Undo tile");s.act(Action.REDO);check(e(s).tile(18,17)==1,"draft Redo tile");s.act(Action.TEST);WorkshopCartridge preview=new WorkshopCartridge(port.launched);same(blank.bytes(),s.cart().bytes(),"isolated draft Test");check(PlayModel.read(preview).world.width==32&&preview.map().tile(18,17)==1,"Test contains map and wiring");
            byte[] journal=s.game.encode();s.restoreGame(journal);same(preview.bytes(),s.game.candidate(blank).bytes(),"exact recovered world");s.act(Action.MENU); // Paint options
            row(s,3);port.fail=true;s.game.focus=0;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).draft(),"failed save retains all draft data");port.fail=false;s.act(Action.CANCEL);row(s,0);WorkshopCartridge saved=s.cart();check(s.game.world!=null&&!e(s).draft(),"save returns to world settings");same(preview.bytes(),saved.bytes(),"one atomic save");
            s.act(Action.UNDO);same(blank.bytes(),s.cart().bytes(),"one Undo restores Lua and map");s.act(Action.REDO);same(saved.bytes(),s.cart().bytes(),"Redo exact");
            for(int y=64;y<128;y++)for(int x=0;x<128;x++)check(blank.sheetPixel(x,y)==saved.sheetPixel(x,y),"shared gfx/map memory preserved");check(saved.map().tile(70,0)==6,"outside map cells preserved");
            s.act(Action.CANCEL);PlayModelTest.sprite(s,180,140);check(s.game.play.elements.get(0).x==180&&s.game.play.elements.get(0).y==140,"place beyond initial screen");PlayModelTest.behavior(s,0);PlayModelTest.field(s,0,1);row(s,s.game.rows().length-2);
            open(s);row(s,4);row(s,1);row(s,0);check(e(s).world.camera==1&&e(s).world.target==0,"follow explicit element");check(e(s).previewX()==120&&e(s).previewY()==80,"centered camera preview");save(s);WorkshopCartridge follow=s.cart();PlayModel model=PlayModel.read(follow);check(follow.code().contains("sprite1_x+sprite1_speed,248")&&follow.code().contains("sprite1_y+sprite1_speed,248"),"world movement bounds");check(follow.code().contains("camera(mid(0,sprite1_x+4-64,world_width*8-128)"),"ordinary follow camera");check(follow.code().indexOf(" map(")<follow.code().indexOf("  sspr(")&&follow.code().indexOf(" camera()")<follow.code().indexOf("  rectfill(14"),"map before sprites, HUD after reset");
            GameCanvas game=new GameCanvas(follow);game.selected=0;game.frameSelection();check(game.viewX>0&&game.viewY>0,"off-screen selection pans");int x=180-game.viewX,y=140-game.viewY;check(game.pickElementAt(x,y),"touch pick accounts for viewport");game.act(Action.CONFIRM);game.focus=1;game.act(Action.CONFIRM);game.point(220,180);check(game.x==220&&game.viewX>=100,"placement pans at viewport edge");GameCanvas recovered=GameCanvas.restore(game.encode(),follow);same(game.candidate(follow).bytes(),recovered.candidate(follow).bytes(),"placement and viewport recover");
            PlayModel pair=model.copy();PlayModel.Element second=pair.elements.get(0).copy();second.x=0;second.y=0;second.movement=0;second.rule=1;second.target=0;pair.elements.add(second);WorkshopCartridge pairCart=pair.write(follow);
            GameCanvas relation=new GameCanvas(pairCart);relation.selected=1;relation.configure(true);relation.frameSelection();relation.focus=2;relation.act(Action.CONFIRM);check(relation.screen==GameCanvas.Screen.RELATION&&relation.viewX>0,"picker opens on its off-screen target");relation.act(Action.CANCEL);check(relation.viewX==0&&relation.viewY==0,"cancel target selection returns viewport to edited element");
            // Editing other capabilities never loses world data, including animation's nested paint checkpoint.
            game=new GameCanvas(follow);game.configure(false);game=GameCanvas.restore(game.encode(),follow);check(game.settings.world.camera==1,"rules recovery retains world");game=new GameCanvas(follow);game.animate();game.focus=3;game.act(Action.CONFIRM);game=GameCanvas.restore(game.encode(),follow);game.act(Action.CANCEL);game.focus=4;game.act(Action.CONFIRM);check(game.animation.model.world.camera==1,"nested frame cancel retains world");
            PlayModel rich=model.copy();rich.musicStart=0;rich.startSound=8;rich.winSound=9;rich.elements.get(0).animation=new PlayAnimation(new SpriteRegion(rich.elements.get(0).sx,rich.elements.get(0).sy,8,8));WorkshopCartridge audio=rich.write(follow);
            WorldEditor world=new WorldEditor(audio);world.world.camera=2;WorkshopCartridge rooms=world.candidate();PlayModel rr=PlayModel.read(rooms);check(rr.musicStart==0&&rr.startSound==8&&rr.elements.get(0).animation!=null,"world editing retains animation and audio");check(rr.world.offset(rr,true)==128&&rr.world.offset(rr,false)==128,"room preview switches screens");same(rooms.bytes(),rr.write(rooms).bytes(),"ordinary cart reopens exactly");
            GameCanvas removal=new GameCanvas(rooms);removal.screen=GameCanvas.Screen.DELETE;removal.focus=1;PlayModelTest.refused(()->removal.candidate(rooms));
            GameCanvas original=new GameCanvas(rooms);original.openWorld();original.world.world.camera=0;WorkshopCartridge fixed=original.candidate(rooms);GameCanvas deletion=new GameCanvas(fixed);deletion.screen=GameCanvas.Screen.DELETE;WorkshopCartridge noElement=deletion.candidate(fixed);check(PlayModel.read(noElement).world.target==-1,"fixed camera permits deleting old target");
            WorldEditor shrink=new WorldEditor(saved);shrink.world.width=16;shrink.world.height=16;WorkshopCartridge smaller=shrink.candidate();check(smaller.map().tile(18,17)==1,"shrinking never clears hidden cells");
            WorkshopCartridge unknown=PlayModel.code(follow,follow.code().replace("map(0,0,0,0,world_width,world_height)","map(0,0,0,0,world_width,world_height,1)"));check(!new GameCanvas(unknown).blocked.isEmpty(),"modified Lua not silently regenerated");PlayModelTest.refused(()->new WorldEditor(unknown));PlayModelTest.refused(()->GameCanvas.restore(journal,saved));
            WorkshopSession empty=PlayModelTest.session(blank,new PlayModelTest.Port());open(empty);row(empty,4);empty.game.focus=1;empty.act(Action.CONFIRM);check(empty.mode==Mode.ERROR,"following needs a selected element, not an invented hero");empty.act(Action.CANCEL);row(empty,0);save(empty);check(PlayModel.read(empty.cart()).elements.isEmpty(),"hero-free map saves");
            if(args.length>0&&nl.equals("\n")){Path dir=Paths.get(args[0]);Files.createDirectories(dir);Files.write(dir.resolve("world-follow.p8"),follow.bytes());Files.write(dir.resolve("world-rooms.p8"),rooms.bytes());Files.write(dir.resolve("world-fixed.p8"),saved.bytes());}
        }
        System.out.println("WorldWorkflowTest: "+checks+" checks passed");
    }
}
