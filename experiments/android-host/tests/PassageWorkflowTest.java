import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.nio.file.*;
import java.util.*;

/** A blank-cart switch puzzle, controller authoring, safe dependencies and recovery. */
public final class PassageWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static WorldEditor e(WorkshopSession s){return s.game.world;}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action: "+s.error);}
    static void recover(WorkshopSession s){s.restoreGame(s.game.encode());check(s.mode==Mode.GAME,"recovery: "+s.error);}
    static void passage(WorkshopSession s,int x,int y){e(s).page=WorldEditor.Page.PAINT;s.game.focus=0;e(s).point(x,y);s.act(Action.MENU);row(s,9);}
    static void bind(WorkshopSession s,int x,int y,int target){passage(s,x,y);row(s,0);row(s,target);}
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args.length>0?args[0]:".");
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl).withFlags(100,165).withTile(100,31,200);
            PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(blank,port);
            PlayModelTest.sprite(s,8,8);PlayModelTest.sprite(s,8,80);PlayModelTest.sprite(s,72,8);
            PlayModelTest.behavior(s,0);PlayModelTest.field(s,0,1);PlayModelTest.field(s,1,3);row(s,s.game.rows().length-2);
            PlayModelTest.behavior(s,1);PlayModelTest.field(s,2,4);row(s,s.game.rows().length-2);
            PlayModelTest.behavior(s,2);PlayModelTest.field(s,2,2);row(s,s.game.rows().length-2);
            WorldWorkflowTest.open(s);row(s,2);e(s).point(4,0);TileWorkflowTest.create(s);
            for(int y=0;y<8;y++)for(int x=0;x<8;x++)e(s).art.paint(x,y);int wall=e(s).art.target;TileWorkflowTest.keep(s);
            for(int y=1;y<16;y++){e(s).point(4,y);s.act(Action.CONFIRM);}
            s.act(Action.MENU);row(s,8);row(s,0);s.act(Action.MENU);row(s,3);row(s,0);
            WorkshopCartridge before=s.cart();check(PlayModel.read(before).world.walls[wall],"walls saved");
            passage(s,4,1);row(s,0);check(e(s).rows().length==2,"only real toggle offered");recover(s);s.act(Action.CANCEL);check(e(s).world.passages.isEmpty(),"picker cancel keeps no binding");
            row(s,0);row(s,0);check(e(s).world.passage(4,1).target==1,"bind chosen switch");recover(s);
            s.act(Action.UNDO);check(e(s).world.passages.isEmpty(),"one draft Undo");s.act(Action.REDO);check(e(s).world.passages.size()==1,"one draft Redo");
            bind(s,4,2,0);check(e(s).world.passages.size()==2,"one switch opens multiple cells");
            row(s,1);check(e(s).world.passage(4,2).on==0,"open when Off");recover(s);row(s,1);
            s.act(Action.TEST);check(port.launched!=null,"isolated Test supplied");WorkshopCartridge candidate=new WorkshopCartridge(port.launched);same(before.bytes(),s.cart().bytes(),"Test leaves saved source unchanged");
            PlayModel p=PlayModel.read(candidate);check(p!=null&&p.world.passages.size()==2,"ordinary source recognized");same(candidate.bytes(),p.write(candidate).bytes(),"source round trips byte for byte");
            s.act(Action.MENU);port.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).world.passages.size()==2,"failed save retains passage draft");port.fail=false;s.act(Action.CANCEL);row(s,0);same(candidate.bytes(),s.cart().bytes(),"save after failure");
            s.act(Action.UNDO);same(before.bytes(),s.cart().bytes(),"session Undo exact");s.act(Action.REDO);same(candidate.bytes(),s.cart().bytes(),"session Redo exact");
            WorkshopSession reopened=PlayModelTest.session(new WorkshopCartridge(candidate.bytes()),new PlayModelTest.Port());check(reopened.game.play.world.passages.size()==2,"reopen independent of metadata");
            for(int y=0;y<64;y++)for(int x=0;x<128;x++)check(candidate.map().tile(x,y)==before.map().tile(x,y),"binding preserves every map cell");for(int t=0;t<256;t++)check(candidate.flags(t)==before.flags(t),"flags preserved");
            // Linked cells cannot be painted, erased or replaced by a new/copy tile.
            passage(s,4,1);s.act(Action.CANCEL);row(s,0);e(s).brush.tile=0;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).tile(4,1)==wall,"paint refuses safely");s.act(Action.CANCEL);
            s.act(Action.MENU);s.game.focus=5;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).art==null,"new tile refuses linked cell");s.act(Action.CANCEL);
            row(s,8);s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).world.walls[wall],"wall rule preserves passage");s.act(Action.CANCEL);s.act(Action.CANCEL);
            PlayModel changed=p.copy();changed.elements.get(1).rule=0;PlayModelTest.refused(()->changed.write(candidate));
            GameCanvas remove=new GameCanvas(candidate);remove.selected=1;remove.screen=GameCanvas.Screen.DELETE;PlayModelTest.refused(()->remove.candidate(candidate));
            PlayModelTest.refused(()->p.write(candidate.withTile(4,1,0)));
            WorkshopCartridge external=candidate.withTile(4,1,0);WorkshopSession repair=PlayModelTest.session(external,new PlayModelTest.Port());WorldWorkflowTest.open(repair);passage(repair,4,1);row(repair,2);WorkshopCartridge repaired=repair.game.candidate(external);check(repaired.map().tile(4,1)==0&&PlayModel.read(repaired).world.passage(4,1)==null,"external map change can be repaired by unlinking without rewriting its data");
            // Removing an earlier element remaps the switch identity and contact targets.
            PlayModel indexed=p.copy();indexed.elements.get(2).rule=0;indexed.elements.get(0).movement=0;WorkshopCartridge indexCart=indexed.write(candidate);GameCanvas deletion=new GameCanvas(indexCart);deletion.selected=0;deletion.screen=GameCanvas.Screen.DELETE;
            check(PlayModel.read(deletion.candidate(indexCart)).world.passage(4,1).target==0,"deletion reindexes switch");
            GameCanvas invalid=new GameCanvas(candidate);invalid.selected=1;invalid.configure(true);invalid.settings.elements.get(1).rule=0;GameCanvas invalidRecovered=GameCanvas.restore(invalid.encode(),candidate);PlayModelTest.refused(()->invalidRecovered.candidate(candidate));check(invalidRecovered.settings.elements.get(1).rule==0,"unfinished invalid rule remains recoverable");
            GameCanvas settings=new GameCanvas(candidate);settings.configure(false);settings=GameCanvas.restore(settings.encode(),candidate);check(settings.settings.world.passages.size()==2,"settings recovery preserves passages");
            GameCanvas animation=new GameCanvas(candidate);animation.selected=0;animation.animate();animation=GameCanvas.restore(animation.encode(),candidate);check(animation.animation.model.world.passages.size()==2,"animation recovery preserves passages");
            PlayModel open=p.copy();open.elements.get(1).on=1;open.elements.get(0).x=32;WorkshopCartridge startsOpen=open.write(candidate);check(PlayModel.read(startsOpen)!=null,"start inside initially open passage permitted");open.elements.get(1).on=0;PlayModelTest.refused(()->open.write(candidate));
            PlayModel inverse=p.copy();for(PlayWorld.Passage cell:inverse.world.passages)cell.on=0;WorkshopCartridge inverted=inverse.write(candidate);
            PlayModel hidden=p.copy();hidden.world.tiles=false;WorkshopCartridge invisible=hidden.write(candidate);
            PlayModel noMovers=p.copy();noMovers.elements.get(0).movement=0;check(PlayModel.read(noMovers.write(candidate))!=null,"stationary puzzle supported");
            for(String code:new String[]{candidate.code().replace("world_passages(false)","world_passages(true)"),candidate.code().replace("passage1_target=1","passage1_target=0"),candidate.code().replace("passage_count=2","passage_count=33")})check(PlayModel.read(PlayModel.code(candidate,code))==null,"unfamiliar passage code preserved");
            PlayWorld.Passage extra=new PlayWorld.Passage();extra.x=20;extra.y=1;extra.tile=wall;extra.target=1;
            // Build the source against matching cell data, then refuse shrinking before field mutation.
            PlayModel big=p.copy();big.world.width=32;big.world.passages.add(extra);WorkshopCartridge wide=big.write(candidate.withTile(20,1,wall));WorldEditor shrink=new WorldEditor(wide);shrink.focus=0;shrink.act(Action.CONFIRM);PlayModelTest.refused(()->shrink.act(Action.LEFT));check(shrink.world.width==32,"failed shrink keeps recoverable field");
            passage(s,4,1);row(s,2);passage(s,4,2);row(s,2);same(before.bytes(),s.game.candidate(candidate).bytes(),"removing all links returns exact previous recipe");recover(s);
            PlayModel two=p.copy();PlayModel.Element other=two.elements.get(0).copy();other.x=80;other.y=80;two.elements.add(other);WorkshopCartridge twoMovers=two.write(candidate);
            PlayModel many=p.copy();many.world.passages.clear();many.world.height=32;WorkshopCartridge map=candidate;for(int y=16;y<32;y++)map=map.withTile(4,y,wall);map=map.withTile(5,0,wall);WorkshopCartridge manyBase=many.write(map);
            WorkshopSession full=PlayModelTest.session(manyBase,new PlayModelTest.Port());WorldWorkflowTest.open(full);for(int y=0;y<32;y++)bind(full,4,y,0);recover(full);check(e(full).world.passages.size()==32,"maximum passage draft recovers");same(full.game.candidate(manyBase).bytes(),PlayModel.read(full.game.candidate(manyBase)).write(full.game.candidate(manyBase)).bytes(),"32 cells round trip");
            passage(full,5,0);row(full,0);full.act(Action.CONFIRM);check(full.mode==Mode.ERROR&&e(full).world.passages.size()==32,"limit fails before mutation");full.act(Action.CANCEL);recover(full);for(int n=0;n<32;n++)full.act(Action.UNDO);check(e(full).world.passages.isEmpty(),"32-step Undo survives journal");
            PlayModelTest.refused(()->GameCanvas.restore(s.game.encode(),before));byte[] truncated=s.game.encode();PlayModelTest.refused(()->GameCanvas.restore(Arrays.copyOf(truncated,truncated.length-1),candidate));
            // Existing v12 source stays exact; changing neither map nor rules generates no passage code.
            same(before.bytes(),PlayModel.read(before).write(before).bytes(),"no-passage source unchanged");
            if(nl.equals("\n")){Path out=root.resolve(".local/passages-work/evidence");Files.createDirectories(out);Files.write(out.resolve("switch-passage.p8"),candidate.bytes());Files.write(out.resolve("starts-open.p8"),startsOpen.bytes());Files.write(out.resolve("inverse.p8"),inverted.bytes());Files.write(out.resolve("hidden.p8"),invisible.bytes());Files.write(out.resolve("two-movers.p8"),twoMovers.bytes());}
        }
        WorkshopCartridge old=new WorkshopCartridge(Files.readAllBytes(root.resolve("docs/showcase/0.0.94-en/maze.p8")));
        GameCanvas legacy=GameCanvas.restore(Files.readAllBytes(root.resolve("experiments/android-host/tests/fixtures/world-v12.journal")),old);
        check(legacy.world.page==WorldEditor.Page.WALL&&legacy.world.world.wallCount()==1&&legacy.world.world.passages.isEmpty(),"real v12 wall review recovers");legacy.act(Action.REDO);check(legacy.world.world.wallCount()==0,"v12 history remains usable");legacy.act(Action.UNDO);same(old.bytes(),legacy.candidate(old).bytes(),"v12 wall source returns exact");
        System.out.println("PassageWorkflowTest: "+checks+" checks passed");
    }
}
