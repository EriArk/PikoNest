import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.file.*;

public final class SoundWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action succeeds: "+s.error);}
    static SoundEditor e(WorkshopSession s){return s.game.sound;}
    static void open(WorkshopSession s){row(s,s.game.blocked.isEmpty()?s.game.uses.entries.size()+2:1);check(e(s)!=null,"sound tool available");}
    static void addNote(WorkshopSession s){row(s,0);row(s,0);s.act(Action.CANCEL);}
    static void save(WorkshopSession s){row(s,35);check(e(s).page==SoundEditor.Page.REVIEW,"explicit shared-resource review");row(s,0);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=SfxDataTest.cart(nl,"");PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(blank,port);open(s);row(s,0);
            check(e(s).slot==8,"new sound avoids custom instrument bank");same(blank.bytes(),s.cart().bytes(),"allocation is a draft");
            String empty=e(s).data.line();s.act(Action.RIGHT);s.act(Action.LEFT);check(e(s).data.line().equals(empty),"cursor motion never paints");addNote(s);check(e(s).data.notes[0][0]==24&&e(s).data.notes[0][2]==5,"explicit add starts at C2");
            row(s,0);row(s,1);s.act(Action.RIGHT);s.restoreGame(s.game.encode());check(e(s).editing&&e(s).data.notes[0][0]==25,"field draft recovers");s.act(Action.CANCEL);check(e(s).data.notes[0][0]==24,"field cancel restores previous pitch");
            row(s,5);check(e(s).note==1&&e(s).data.notes[1][2]==5,"copy note advances");s.act(Action.UNDO);check(e(s).data.notes[1][2]==0,"draft Undo");s.act(Action.REDO);check(e(s).data.notes[1][2]==5,"draft Redo");s.act(Action.CANCEL);
            row(s,32);row(s,0);s.act(Action.LEFT);s.act(Action.CONFIRM);check(e(s).data.speed==15,"speed is editable");row(s,1);s.act(Action.RIGHT);s.act(Action.CONFIRM);row(s,2);s.act(Action.RIGHT);s.act(Action.RIGHT);s.act(Action.CONFIRM);check(e(s).data.loopStart==1&&e(s).data.loopEnd==2,"loop settings");s.act(Action.CANCEL);
            byte[] journal=s.game.encode();s.act(Action.CONTEXT);WorkshopCartridge listen=new WorkshopCartridge(port.launched);check(listen.code().contains("sfx(8,3)")&&listen.code().contains("sfx(-1,3)"),"isolated official listening cart supports play and stop");same(blank.bytes(),s.cart().bytes(),"Listen leaves saved source exact");s.restoreGame(journal);check(e(s).data.loopEnd==2,"listen return journal retains draft");
            s.act(Action.TEST);check(new WorkshopCartridge(port.launched).code().equals(blank.code()),"Test keeps original game code when editing only data");save(s);
            WorkshopCartridge first=s.cart();check(first.sfx().get(8).notes[1][2]==5&&first.code().equals(blank.code()),"SFX save changes resources only");check(e(s).page==SoundEditor.Page.BANK&&s.game.focus==10,"save returns to slot");s.act(Action.UNDO);same(blank.bytes(),s.cart().bytes(),"whole-resource Undo exact");s.act(Action.REDO);same(first.bytes(),s.cart().bytes(),"whole-resource Redo exact");
            row(s,1);row(s,0);row(s,1);addNote(s);int startSlot=e(s).slot;check(startSlot==9,"existing data reserved during event creation");save(s);
            check(s.game.play.startSound==9&&s.game.play.elements.isEmpty(),"blank game start event needs no hero");same(s.cart().bytes(),s.game.play.write(s.cart()).bytes(),"sound recipe canonical reopen");
            row(s,1);row(s,10);row(s,0);check(s.game.play.winSound==8,"existing SFX bound to win");
            row(s,0);row(s,0);row(s,0);check(s.game.play.startSound==-1&&s.cart().sfx().get(9).audible()==1,"unbinding keeps resource");
            s.act(Action.CANCEL);row(s,10);row(s,0);row(s,2);s.act(Action.RIGHT);s.act(Action.CONFIRM);s.act(Action.CANCEL);row(s,35);check(e(s).summary().contains("Events: 1"),"shared use disclosed before Save");
            byte[] protectedBytes=s.cart().bytes();port.fail=true;s.game.focus=0;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&e(s).data!=null,"failed save keeps draft");port.fail=false;s.act(Action.CANCEL);s.restoreGame(s.game.encode());row(s,1);row(s,34);int copy=e(s).slot;check(copy==10,"independent copy chooses unreferenced raw-empty slot");save(s);check(s.cart().sfx().get(8).notes[0][2]==5&&s.cart().sfx().get(copy).notes[0][2]==6,"copy leaves shared original intact");
            check(s.game.play.winSound==8,"copy never silently rebinds other uses");
            WorkshopCartridge current=s.cart();row(s,copy+2);addNote(s);byte[] stale=s.game.encode();SfxDataTest.refused(()->GameCanvas.restore(stale,blank));s.act(Action.CANCEL);same(current.bytes(),s.cart().bytes(),"discard leaves source exact");
            // Unknown Lua can edit an explicit sound but cannot authorize allocation or event rewriting.
            WorkshopCartridge unknown=PlayModel.code(current,"-- arbitrary imported code\nfunction _update() sfx(flr(rnd(64))) end\n");WorkshopSession u=PlayModelTest.session(unknown,new PlayModelTest.Port());open(u);u.game.focus=0;u.act(Action.CONFIRM);check(u.mode==Mode.ERROR,"unknown Lua allocation refused");u.act(Action.CANCEL);row(u,10);check(e(u).model==null,"generic explicit editor is independent of Game view recipe");row(u,0);row(u,0);u.act(Action.CANCEL);save(u);check(u.cart().code().equals(unknown.code()),"unknown Lua stays exact through audio edit");
            // Music reserves slots even when channels are muted; silent but authored notes reserve data.
            P8Sfx.Sound authored=new P8Sfx.Sound();authored.notes[0][0]=1;WorkshopCartridge reserved=SfxDataTest.cart(nl,"__music__"+nl+"00 48494a4b"+nl).withSound(12,authored);SoundEditor safe=new SoundEditor(reserved,false);safe.act(Action.CONFIRM);check(safe.slot==13,"music and nonzero silent data remain reserved");
            P8Sfx.Sound custom=new P8Sfx.Sound();custom.notes[0]=new int[]{24,8,5,0};WorkshopCartridge instruments=reserved.withSound(20,custom);check(instruments.sfx().instrumentUses(0)==1,"custom instrument dependencies retained");
            WorkshopCartridge collection=new WorkshopCartridge(Files.readAllBytes(Paths.get("docs/showcase/0.0.77-en/collection.p8")));PlayModel model=PlayModel.read(collection);model.startSound=8;model.winSound=9;model.loseSound=10;model.elements.get(1).sound=11;WorkshopCartridge bound=model.write(collection);
            check(PlayModel.read(bound)!=null,"interaction event source recognizes exactly");GameCanvas behavior=new GameCanvas(bound);behavior.configure(true);behavior=GameCanvas.restore(behavior.encode(),bound);check(behavior.settings.winSound==9&&behavior.settings.elements.get(1).sound==11,"behavior recovery retains all sound bindings");
            GameCanvas animation=new GameCanvas(bound);animation.animate();animation=GameCanvas.restore(animation.encode(),bound);check(animation.animation.model.winSound==9,"animation recovery retains sound bindings");
            GameCanvas move=new GameCanvas(bound);move.selected=1;move.screen=GameCanvas.Screen.ACTIONS;move.focus=1;move.act(Action.CONFIRM);move.point(40,40);check(PlayModel.read(move.candidate(bound)).elements.get(1).sound==11,"placement edits retain event sound");
            GameCanvas duplicate=new GameCanvas(bound);duplicate.selected=1;duplicate.screen=GameCanvas.Screen.ACTIONS;duplicate.focus=2;duplicate.act(Action.CONFIRM);check(PlayModel.read(duplicate.candidate(bound)).elements.get(3).sound==11,"duplicate intentionally shares SFX binding");
            WorkshopCartridge changedCode=PlayModel.code(bound,bound.code().replace("sfx(sprite2_sfx,3)","sfx(sprite2_sfx,2)"));check(PlayModel.read(changedCode)==null,"different channel Lua not silently normalized");
            WorkshopSession contextual=PlayModelTest.session(bound,new PlayModelTest.Port());row(contextual,2);row(contextual,7);check(e(contextual).returnElement==1,"selected element opens its own sound binding");contextual.restoreGame(contextual.game.encode());contextual.act(Action.CANCEL);check(contextual.game.screen==GameCanvas.Screen.ACTIONS&&contextual.game.focus==7,"binding Cancel returns to selected element");row(contextual,7);row(contextual,0);row(contextual,0);check(contextual.game.screen==GameCanvas.Screen.ACTIONS&&contextual.game.selected==1&&contextual.game.play.elements.get(1).sound==-1,"unbinding save returns to selected element");
            WorkshopSession tools=PlayModelTest.session(unknown,new PlayModelTest.Port());tools.game=null;tools.mode=Mode.NAVIGATE;tools.act(Action.MENU);tools.menuItem=8;tools.act(Action.CONFIRM);check(tools.mode==Mode.GAME&&e(tools).fromTools,"All tools opens generic SFX editor");tools.restoreGame(tools.game.encode());tools.act(Action.CANCEL);check(tools.game==null&&tools.mode==Mode.NAVIGATE,"All tools origin recovers and returns");
            if(args.length>0&&nl.equals("\n")){Path dir=Paths.get(args[0]);Files.createDirectories(dir);Files.write(dir.resolve("collection-sounds.p8"),bound.bytes());PlayModel switches=PlayModel.read(new WorkshopCartridge(Files.readAllBytes(Paths.get("docs/showcase/0.0.77-en/switches.p8"))));switches.elements.get(0).sound=12;switches.winSound=13;Files.write(dir.resolve("switch-sounds.p8"),switches.write(collection).bytes());Files.write(dir.resolve("listen.p8"),listen.bytes());}
        }
        System.out.println("SoundWorkflowTest: "+checks+" checks passed");
    }
}
