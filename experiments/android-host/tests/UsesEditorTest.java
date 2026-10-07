import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

/** User journeys through draft Test, field cancellation and process recovery. */
public final class UsesEditorTest {
    static int checks;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static class Port implements WorkshopSession.Port {
        byte[] launched;int writes;boolean failLaunch;
        public void save(byte[] bytes){writes++;}
        public void launch(byte[] bytes)throws Exception{if(failLaunch)throw new Exception("Runtime unavailable");launched=bytes;}
    }
    static void act(WorkshopSession s,Action... actions){for(Action a:actions)s.act(a);}
    static void testDraft(WorkshopSession s,Port p){
        byte[] original=s.cart().bytes(),draft=s.uses.proposal().candidate(s.cart()).bytes();int undo=s.undoCount(),writes=p.writes;
        byte[] journal=s.uses.encode();s.act(Action.TEST);
        check(Arrays.equals(p.launched,draft),"Test launches the exact candidate");
        check(Arrays.equals(original,s.cart().bytes())&&p.writes==writes&&s.undoCount()==undo,"Test preserves saved cart and history");
        check(Arrays.equals(journal,s.uses.encode()),"Test preserves edit context");
        WorkshopSession restored=new WorkshopSession(s.cart(),p);restored.restoreUses(journal);
        check(Arrays.equals(restored.uses.proposal().candidate(restored.cart()).bytes(),draft),"process recovery preserves tested candidate");
    }
    public static void main(String[] args){
        WorkshopCartridge blank=GameUsesTest.cart("-- original blank\n");Port p=new Port();WorkshopSession s=new WorkshopSession(blank,p);s.openUses(false);s.uses.addAnimation(new SpriteRegion(0,0,8,8));
        s.uses.animation.field=2;act(s,Action.RIGHT,Action.NEXT);check(s.uses.animation.frame(0).millis==250,"navigation never changes duration");
        act(s,Action.CONFIRM,Action.RIGHT,Action.DOWN);check(s.uses.editingField()&&s.uses.animation.field==2&&s.uses.animation.frame(0).millis==300,"explicit field editing locks navigation");
        testDraft(s,p);byte[] journal=s.uses.encode();s.restoreUses(journal);s.act(Action.CANCEL);
        check(s.uses.animation.frame(0).millis==250&&!s.uses.editingField(),"B restores original field after process recovery");
        act(s,Action.CONFIRM,Action.RIGHT,Action.CONFIRM);check(s.uses.animation.frame(0).millis==300,"A keeps field in draft");
        s.uses.animation.field=3;s.act(Action.CONFIRM);s.uses.animation.field=4;act(s,Action.CONFIRM,Action.LEFT,Action.CANCEL);
        check(s.uses.animation.selected==1&&s.uses.animation.count()==2,"cancel reorder restores frames and selection");
        s.uses.animation.field=1;s.act(Action.CONFIRM);byte[] launched=p.launched;s.act(Action.TEST);check(p.launched==launched&&s.uses.animation.picker!=null&&s.mode==WorkshopSession.Mode.ERROR,"unfinished resource selection explains prerequisite without launching");act(s,Action.CONFIRM,Action.CANCEL);
        s.act(Action.MENU);testDraft(s,p);s.act(Action.CONFIRM);
        check(p.writes==1&&s.undoCount()==1&&s.uses.current().animation!=null,"Apply makes one atomic edit");
        s.act(Action.UNDO);check(Arrays.equals(blank.bytes(),s.cart().bytes()),"one undo returns original blank bytes");s.act(Action.REDO);
        s.uses.addCamera(false);s.uses.camera.field=1;act(s,Action.RIGHT,Action.NEXT);check(s.uses.camera.form.value(0).equals("0"),"camera navigation does not mutate");
        act(s,Action.CONFIRM,Action.NEXT);testDraft(s,p);journal=s.uses.encode();s.restoreUses(journal);s.act(Action.CANCEL);check(s.uses.camera.form.value(0).equals("0"),"camera field rollback survives recovery");
        act(s,Action.CONFIRM,Action.NEXT,Action.CONFIRM);s.act(Action.MENU);testDraft(s,p);act(s,Action.CANCEL);check(s.uses.camera.field==1&&s.uses.camera.form.value(0).equals("8"),"review returns to same field");
        p.failLaunch=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&s.uses.camera.form.value(0).equals("8"),"launch failure keeps draft");s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.USES,"error returns to editor");
        int writes=p.writes;s.act(Action.CANCEL);check(p.writes==writes&&s.uses.screen==GameUses.Screen.LIST,"cancel camera writes nothing");
        // Old journals remain readable: v3 ends immediately before the v4 field memento.
        GameUses old=new GameUses(blank,0);byte[] v3=Arrays.copyOf(old.encode(),old.encode().length-10);v3[3]=3;check(GameUses.restore(v3,blank).screen==GameUses.Screen.LIST,"v3 backward compatibility");
        s.uses.addCamera(false);s.uses.camera.field=0;act(s,Action.CONFIRM,Action.RIGHT);journal=s.uses.encode();s.restoreUses(journal);s.act(Action.CANCEL);check(s.uses.camera.form.item().id.equals("camera"),"mode rollback restores original dimensions and mode");
        System.out.println("UsesEditorTest: "+checks+" checks passed");
    }
}
