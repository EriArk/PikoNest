import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** Connected goal/rule editing, semantic return, dependency protection and recovery. */
public final class GameRulesOverviewTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void row(WorkshopSession s,int n){PlayModelTest.row(s,n);}
    static void overview(WorkshopSession s){row(s,s.game.uses.entries.size()+1);check(s.game.screen==GameCanvas.Screen.OVERVIEW,"Game rules opens overview");}
    static int find(WorkshopSession s,int element,String text){String[] rows=s.game.rows();for(int n=0;n<rows.length;n++)if(s.game.rowElement(n)==element&&rows[n].contains(text))return n;throw new AssertionError("missing row "+element+" "+text);}
    static void open(WorkshopSession s,int element,String text){row(s,find(s,element,text));}
    static void choose(WorkshopSession s,int logical,int value){int n=0;while(s.game.formFields()[n]!=logical)n++;row(s,n);s.game.focus=value;s.act(Action.CONFIRM);}
    static void save(WorkshopSession s){row(s,s.game.rows().length-2);check(s.mode==Mode.GAME&&s.game.screen==GameCanvas.Screen.OVERVIEW,"save returns to overview");}
    static WorkshopCartridge fixture(String nl){
        WorkshopCartridge cart=GameRelationshipsTest.fixture(nl);PlayModel m=PlayModel.fromStatic(cart);
        m.goal=1;m.elements.get(0).movement=1;m.elements.get(1).rule=1;m.elements.get(1).target=0;
        m.elements.get(2).rule=4;m.elements.get(3).rule=4;m.elements.get(3).neighbor=2;
        PlayModel.Element mover=m.elements.get(0);mover.animation=new PlayAnimation(new SpriteRegion(mover.sx,mover.sy,mover.w,mover.h));mover.animation.mode=1;
        cart=m.write(cart);GameElementNames names=GameElementNames.read(cart,new GameUses(m.layout(cart),0));
        for(int n=0;n<4;n++)names.values.set(n,new String[]{"Heart","Star","Moon","Sun"}[n]);return names.apply(cart);
    }
    static byte[] legacyGoal(GameCanvas g)throws Exception{
        byte[] bytes=g.encode();java.io.ByteArrayInputStream stream=new java.io.ByteArrayInputStream(bytes);java.io.DataInputStream in=new java.io.DataInputStream(stream);
        in.readInt();int source=in.readInt();in.skipBytes(source);in.readUTF();in.readUTF();in.skipBytes(28);in.readBoolean();if(in.readBoolean()||!in.readBoolean())throw new AssertionError();
        in.readUTF();in.readUTF();in.readInt();in.readInt();int count=in.readInt();in.skipBytes(count*48);in.readBoolean();in.readInt();
        for(int n=0;n<count;n++)if(in.readBoolean()){in.readInt();int frames=in.readInt();in.skipBytes(frames*20);}
        int at=bytes.length-stream.available(),soundSize=20+count*4;byte[] old=new byte[bytes.length-soundSize-17];System.arraycopy(bytes,0,old,0,at);System.arraycopy(bytes,at+soundSize,old,at,old.length-at);java.nio.ByteBuffer.wrap(old).putInt(5);return old;
    }
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge original=fixture(nl);PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(original,port);
            overview(s);check(!s.game.draft(),"overview is not a draft");same(original.bytes(),s.game.candidate(original).bytes(),"overview leaves source exact");
            check(Arrays.equals(s.game.goalElements(),new int[]{1}),"goal highlights only collectible elements");
            check(s.game.summary().contains("collecting 1 item"),"goal describes only collect interactions");
            s.game.focus=find(s,1,"Collect");s.act(Action.RIGHT);s.act(Action.LEFT);check(s.game.selected==1&&s.game.relatedElement()==0,"focused contact highlights both named elements");
            check(s.game.summary().equals("Heart touches Star: collect once."),"named rule sentence");s.restoreGame(s.game.encode());check(s.game.screen==GameCanvas.Screen.OVERVIEW&&s.game.selected==1,"overview focus recovers");
            row(s,0);row(s,0);check(s.game.screen==GameCanvas.Screen.GOAL,"explicit goal chooser");
            s.game.focus=2;check(Arrays.equals(s.game.goalElements(),new int[]{2,3}),"highlighted switch goal identifies participants");s.restoreGame(s.game.encode());s.act(Action.TEST);check(PlayModel.read(new WorkshopCartridge(port.launched)).goal==2,"Test uses highlighted goal");same(original.bytes(),s.cart().bytes(),"goal Test does not save");
            s.act(Action.CANCEL);check(s.game.settings.goal==1&&s.game.screen==GameCanvas.Screen.SETTINGS,"goal cancellation restores form value");
            row(s,0);s.game.focus=0;check(s.game.summary().contains("Contact win / lose"),"free goal does not imply disabling contact outcomes");s.game.focus=2;s.act(Action.CONFIRM);save(s);
            WorkshopCartridge puzzleGoal=s.cart();check(s.game.focus==0&&s.game.play.goal==2,"saved goal returns to matching row");
            s.act(Action.UNDO);same(original.bytes(),s.cart().bytes(),"goal Undo exact");check(s.game.screen==GameCanvas.Screen.OVERVIEW,"Undo retains overview");s.act(Action.REDO);same(puzzleGoal.bytes(),s.cart().bytes(),"goal Redo exact");
            open(s,0,"D-pad");choose(s,0,0);row(s,s.game.rows().length-2);check(s.mode==Mode.ERROR&&s.error.contains("Heart")&&s.error.contains("Animation"),"cannot remove animated movement without named guidance");
            same(puzzleGoal.bytes(),s.cart().bytes(),"blocked removal preserves source");s.act(Action.CANCEL);s.restoreGame(s.game.encode());s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.OVERVIEW&&s.game.rows()[s.game.focus].contains("D-pad"),"discard restored behavior returns to originating rule");
            open(s,0,"Animation");s.restoreGame(s.game.encode());row(s,0);s.act(Action.LEFT);s.act(Action.CONFIRM);row(s,8);check(s.game.screen==GameCanvas.Screen.OVERVIEW&&s.game.rows()[s.game.focus].contains("Animation"),"animation save retains origin");
            check(s.game.play.elements.get(0).animation.mode==0,"animation changed to loop");
            open(s,0,"D-pad");choose(s,0,0);save(s);check(s.game.rowElement(s.game.focus)==0&&s.game.rows()[s.game.focus].contains("Animation"),"removed movement falls back to same element");
            open(s,1,"Collect");choose(s,2,0);s.restoreGame(s.game.encode());s.act(Action.TEST);check(PlayModel.read(new WorkshopCartridge(port.launched)).elements.get(1).rule==0,"Test includes interaction removal");
            byte[] before=s.cart().bytes();port.fail=true;row(s,s.game.rows().length-2);check(s.mode==Mode.ERROR,"failed save retains draft");same(before,s.cart().bytes(),"save failure leaves source exact");port.fail=false;s.act(Action.CANCEL);save(s);
            check(s.game.rowElement(s.game.focus)==1&&s.game.rows()[s.game.focus].contains("Add behavior"),"removed last rule leaves a useful entry");
            s.act(Action.UNDO);check(s.game.screen==GameCanvas.Screen.OVERVIEW&&s.game.rows()[s.game.focus].contains("Collect"),"Undo resolves missing entry to restored rule");
            open(s,2,"Toggle");choose(s,2,0);row(s,s.game.rows().length-2);check(s.mode==Mode.ERROR&&s.error.contains("Moon")&&s.error.contains("Sun"),"linked switch removal names dependency");s.act(Action.CANCEL);s.act(Action.CANCEL);
            open(s,3,"Toggle");choose(s,5,0);save(s);check(s.game.play.elements.get(3).neighbor==-1,"unlink uses existing relationship picker");
            open(s,2,"Toggle");choose(s,2,0);save(s);check(s.game.play.elements.get(2).rule==0,"unlinked switch can be removed safely");
            // A nested interaction selection keeps its overview origin across Test/recovery/cancel.
            open(s,1,"Collect");choose(s,2,2);check(s.game.screen==GameCanvas.Screen.RELATION,"contact picker opens from overview");s.restoreGame(s.game.encode());s.act(Action.CANCEL);s.act(Action.CANCEL);s.act(Action.CANCEL);
            check(s.game.screen==GameCanvas.Screen.OVERVIEW&&s.game.rowElement(s.game.focus)==1,"nested cancellations return to originating element");
            open(s,3,"Toggle");WorkshopCartridge forgedBase=s.cart();byte[] forged=s.game.encode();java.nio.ByteBuffer.wrap(forged,forged.length-15,4).putInt(-2);PlayModelTest.refused(()->GameCanvas.restore(forged,forgedBase));
            byte[] stale=s.game.encode();WorkshopCartridge current=s.cart();PlayModelTest.refused(()->GameCanvas.restore(stale,original));
            s.act(Action.CANCEL);s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.HOME&&s.game.focus==s.game.uses.entries.size()+1,"Back returns to Game rules entrance");
            same(current.bytes(),new GameCanvas(new WorkshopCartridge(current.bytes())).candidate(current).bytes(),"reopen/export remain ordinary exact cart");
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\nend\n",nl);WorkshopSession empty=PlayModelTest.session(blank,new PlayModelTest.Port());overview(empty);row(empty,0);row(empty,0);empty.game.focus=1;
            check(empty.game.summary().contains("No collect rules yet"),"empty goal explains next action");empty.restoreGame(empty.game.encode());empty.act(Action.CONFIRM);save(empty);check(empty.game.play.goal==1&&empty.game.play.elements.isEmpty(),"empty game can save an unfinished goal without inventing a hero");
            row(empty,0);row(empty,0);empty.game.focus=2;check(empty.game.summary().contains("No switches yet"),"switch goal explains missing rules");empty.act(Action.CANCEL);empty.act(Action.CANCEL);check(empty.game.screen==GameCanvas.Screen.OVERVIEW,"empty draft cancellation returns safely");
            row(empty,1);choose(empty,1,0); // Numeric background editor still supports field cancellation.
            empty.act(Action.CANCEL);check(empty.game.screen==GameCanvas.Screen.OVERVIEW,"background cancellation returns to overview");
            GameCanvas legacy=new GameCanvas(original);legacy.configure(false);legacy.focus=0;legacy.editingField=true;byte[] v5=legacyGoal(legacy);
            GameCanvas recovered=GameCanvas.restore(v5,original);recovered.act(Action.RIGHT);recovered.act(Action.CANCEL);check(recovered.settings.goal==0,"legacy numeric goal draft reverts old value");
            WorkshopCartridge unfamiliar=PlayModel.code(original,original.code()+"poke(0,1)\n");GameCanvas protectedCart=new GameCanvas(unfamiliar);PlayModelTest.refused(()->protectedCart.openOverview());same(unfamiliar.bytes(),protectedCart.base.bytes(),"unfamiliar Lua not normalized");
        }
        System.out.println("GameRulesOverviewTest: "+checks+" checks passed");
    }
}
