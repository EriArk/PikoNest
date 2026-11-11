import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.nio.file.*;
import java.util.*;

/** Pressure plate authoring uses an existing object/target, ordinary Lua and world links. */
public final class PlateWorkflowTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
 static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,s.error);}
 static void recover(WorkshopSession s){s.restoreGame(s.game.encode());check(s.mode==Mode.GAME,"recover");}
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args.length>0?args[0]:".");
  for(String nl:new String[]{"\n","\r\n"}){
   WorkshopCartridge old=new WorkshopCartridge(Files.readAllBytes(root.resolve("docs/showcase/0.0.95-en/switch-passage.p8")));
   old=PlayModel.code(old,old.code().replace("\n",nl));
   same(old.bytes(),PlayModel.read(old).write(old).bytes(),"old switch source exact");
   PlayModel initial=PlayModel.read(old);initial.elements.get(1).x=24;initial.elements.get(1).y=8;
   WorkshopCartridge before=initial.write(old);PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(before,port);
   PlayModelTest.behavior(s,1);row(s,1);s.game.focus=5;s.act(Action.TEST);check(s.mode==Mode.ERROR,"plate preview requires target selection");s.act(Action.CANCEL);s.act(Action.CONFIRM);
   check(s.game.screen==GameCanvas.Screen.RELATION,"choose activator");recover(s);s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.CHOICE&&s.game.settings.elements.get(1).rule==4,"relationship cancel reverts rule");s.act(Action.CONFIRM);row(s,0);
   check(s.game.settings.elements.get(1).rule==5&&s.game.settings.elements.get(1).target==0,"plate pressed by first sprite");recover(s);s.act(Action.TEST);WorkshopCartridge candidate=new WorkshopCartridge(port.launched);same(before.bytes(),s.cart().bytes(),"Test not saved");
   port.fail=true;s.game.focus=s.game.rows().length-2;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR,"save failure");port.fail=false;s.act(Action.CANCEL);row(s,s.game.rows().length-2);same(candidate.bytes(),s.cart().bytes(),"save after retry");s.act(Action.UNDO);same(before.bytes(),s.cart().bytes(),"Undo");s.act(Action.REDO);same(candidate.bytes(),s.cart().bytes(),"Redo");
   PlayModel p=PlayModel.read(candidate);check(p!=null&&p.elements.get(1).rule==5,"ordinary source reopens");check(!p.source().contains("left/right: select"),"plate needs no global selection");same(candidate.bytes(),p.write(candidate).bytes(),"exact source");
   GameCanvas game=new GameCanvas(candidate);game.openOverview();check(Arrays.toString(game.rows()).contains("Pressure plate"),"rules overview accepts plate");game.openWorld();game.world.point(4,1);game.world.page=WorldEditor.Page.PAINT;game.act(Action.MENU);game.focus=9;game.act(Action.CONFIRM);game.act(Action.CONFIRM);check(game.rows().length==2,"plate offered to passage picker");game=GameCanvas.restore(game.encode(),candidate);check(game.world.world.passages.size()==2,"world recovery retains plate binding");
   GameCanvas removal=new GameCanvas(candidate);removal.selected=0;removal.screen=GameCanvas.Screen.DELETE;PlayModelTest.refused(()->removal.candidate(candidate));
   PlayModel unsafe=p.copy();unsafe.elements.get(1).target=1;PlayModelTest.refused(()->unsafe.write(candidate));
   PlayModel animated=p.copy();animated.elements.get(1).animation=new PlayAnimation(new SpriteRegion(animated.elements.get(1).sx,animated.elements.get(1).sy,8,8));animated.elements.get(1).animation.mode=2;animated.elements.get(1).sound=8;animated.musicStart=0;WorkshopCartridge audio=animated.write(candidate);
   game=new GameCanvas(audio);game.selected=1;game.animate();game=GameCanvas.restore(game.encode(),audio);check(game.animation.model.elements.get(1).rule==5,"animation state recovery");
   PlayModel start=p.copy();start.elements.get(0).x=32;start.elements.get(1).x=32;WorkshopCartridge opened=start.write(candidate);check(start.startsOn(1),"initial overlap opens before start validation");start.elements.get(1).x=80;PlayModelTest.refused(()->start.write(candidate));
   PlayModel goal=p.copy();goal.goal=2;check(goal.source().contains("and sprite2_on==1 then game_result=1"),"plate participates in all-on goal");
   WorkshopSession empty=PlayModelTest.session(GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl),new PlayModelTest.Port());PlayModelTest.sprite(empty,8,8);PlayModelTest.behavior(empty,0);row(empty,1);empty.game.focus=5;empty.act(Action.CONFIRM);recover(empty);PlayModelTest.refused(()->empty.game.candidate(empty.cart()));check(empty.game.settings.elements.size()==1,"no automatic hero or activator");
   check(PlayModel.read(PlayModel.code(candidate,candidate.code().replace("_on=(sprite2_visible","_on=(true or sprite2_visible")))==null,"unknown plate Lua refused");
   for(int y=0;y<64;y++)for(int x=0;x<128;x++)check(before.map().tile(x,y)==candidate.map().tile(x,y),"map source preserved");
   if(nl.equals("\n")){Path out=root.resolve(".local/beta2-work/evidence");Files.createDirectories(out);Files.write(out.resolve("plate.p8"),candidate.bytes());Files.write(out.resolve("plate-audio.p8"),audio.bytes());Files.write(out.resolve("plate-starts-open.p8"),opened.bytes());Files.write(out.resolve("plate-goal.p8"),goal.write(candidate).bytes());}
  }
  WorkshopCartridge legacyBase=new WorkshopCartridge(Files.readAllBytes(root.resolve("docs/showcase/0.0.95-en/switch-passage.p8")));
  GameCanvas legacy=GameCanvas.restore(Files.readAllBytes(root.resolve("experiments/android-host/tests/fixtures/choice-v13.journal")),legacyBase);check(legacy.rows()[legacy.focus].equals("Cancel"),"v13 Cancel remains Cancel, not pressure plate");legacy.act(Action.CONFIRM);check(legacy.settings.elements.get(1).rule==4,"legacy Cancel preserves toggle");
  System.out.println("PlateWorkflowTest: "+checks+" checks passed");
 }
}
