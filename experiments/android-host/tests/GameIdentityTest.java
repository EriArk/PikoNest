import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** Names follow supported edits; explicit choices are cancellable complete operations. */
public final class GameIdentityTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);}
    static void name(WorkshopSession s,int index,String value){row(s,index+1);row(s,6);s.game.nameDraft.restoreText(value);s.game.focus=1;s.act(Action.CONFIRM);check(s.mode==Mode.GAME&&!s.game.draft(),"name saved");}
    static void choose(WorkshopSession s,int field,int value){int n=0;while(s.game.formFields()[n]!=field)n++;row(s,n);check(s.game.screen==GameCanvas.Screen.CHOICE,"explicit choice list");s.game.focus=value;s.act(Action.CONFIRM);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge original=GameRelationshipsTest.fixture(nl);PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(original,port);
            row(s,1);row(s,6);check(s.game.nameDraft.latin&&s.game.nameDraft.limit==24,"English bounded keyboard");
            for(int n=0;n<7;n++)s.act(Action.RIGHT);s.act(Action.CONFIRM);check(s.game.nameDraft.text().equals("H"),"controller types letter");
            s.act(Action.UNDO);s.game.nameDraft.restoreText("Heart");s.game.nameDraft.replaceAll=false;
            byte[] journal=s.game.encode();s.act(Action.TEST);WorkshopCartridge preview=new WorkshopCartridge(port.launched);
            check(new GameCanvas(preview).label(0).equals("Heart"),"Test includes draft name");same(original.bytes(),s.cart().bytes(),"name Test leaves source exact");
            s.restoreGame(journal);check(s.game.nameDraft.text().equals("Heart")&&!s.game.nameDraft.uppercase&&!s.game.nameDraft.replaceAll,"keyboard state recovered");
            s.act(Action.CANCEL);same(original.bytes(),s.cart().bytes(),"name cancellation exact");
            row(s,1);row(s,6);s.game.nameDraft.restoreText("Heart");s.game.nameDraft.key=40;s.act(Action.DOWN);check(s.game.focus==1,"save reachable below keyboard");s.act(Action.CONFIRM);
            WorkshopCartridge named=s.cart();check(s.game.label(0).equals("Heart")&&s.game.play==null,"naming does not convert static source to a behavior recipe");
            check(named.code().substring(named.code().indexOf(nl,2*nl.length())+nl.length()).equals(original.code()),"comments leave executable static Lua unchanged");
            same(original.bytes(),PlayModel.code(named,original.code()).bytes(),"non-code sections exact");
            s.act(Action.UNDO);same(original.bytes(),s.cart().bytes(),"rename Undo exact");s.act(Action.REDO);same(named.bytes(),s.cart().bytes(),"rename Redo exact");
            name(s,1,"Switch");name(s,2,"Switch");check(s.game.label(1).equals("Switch A")&&s.game.label(2).equals("Switch B"),"duplicate names disambiguated");
            name(s,3,"Switch A");check(new HashSet<>(Arrays.asList(s.game.label(0),s.game.label(1),s.game.label(2),s.game.label(3))).size()==4,"explicit name cannot collide with generated suffix");
            row(s,1);row(s,1);s.game.point(30,30);s.act(Action.CONFIRM);check(s.game.label(0).equals("Heart"),"move follows name");
            row(s,1);row(s,2);s.game.point(80,80);s.act(Action.CONFIRM);check(s.game.label(4).equals("Heart B"),"duplicate inherits independent identity label");name(s,4,"Echo");check(s.game.label(0).equals("Heart"),"renaming duplicate leaves original name");
            row(s,2);row(s,3);row(s,1);check(s.game.names.values.get(3).equals("Echo")&&s.game.names.values.get(1).equals("Switch"),"delete reindexes names with placements");
            PlayModelTest.behavior(s,0);choose(s,0,1);check(s.game.settings.elements.get(0).movement==1,"movement selected explicitly");row(s,s.game.rows().length-2);
            check(s.game.play!=null&&s.game.label(0).equals("Heart"),"behavior conversion keeps names");WorkshopCartridge withBehavior=s.cart();
            PlayModelTest.behavior(s,1);choose(s,2,1);check(s.game.screen==GameCanvas.Screen.RELATION,"contact choice asks who touches it");
            byte[] pending=s.game.encode();s.restoreGame(pending);s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.CHOICE&&s.game.settings.elements.get(1).rule==0,"relationship cancel restores prior rule");
            s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.BEHAVIOR,"choice cancel returns to form");same(withBehavior.bytes(),s.cart().bytes(),"guided cancel leaves source exact");
            choose(s,2,1);s.act(Action.CONFIRM);check(s.game.screen==GameCanvas.Screen.BEHAVIOR&&s.game.settings.elements.get(1).target==0,"named target confirmed");row(s,s.game.rows().length-2);
            check(s.game.play.elements.get(1).rule==1&&s.game.label(0).equals("Heart"),"guided contact saved");
            GameCanvas exported=new GameCanvas(new WorkshopCartridge(s.cart().bytes()));check(exported.label(0).equals("Heart")&&exported.names.values.get(3).equals("Echo"),"names reopen from ordinary cart alone");
            GameAnimationTest.animate(s,0);row(s,4);row(s,5);row(s,4);check(s.mode==Mode.GAME&&s.game.animation.sequence().frames.size()==3,"named sequence can allocate again after first-frame change");s.restoreGame(s.game.encode());s.act(Action.TEST);check(new GameCanvas(new WorkshopCartridge(port.launched)).label(0).equals("Heart"),"named reordered animation recovers and tests");row(s,8);check(s.game.label(0).equals("Heart"),"first-frame allocation/reorder retains identity");
            row(s,1);row(s,6);s.game.nameDraft.restoreText("Changed");port.fail=true;s.game.focus=1;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.game.nameDraft.text().equals("Changed"),"save failure keeps name draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.CANCEL);check(s.game.label(0).equals("Heart"),"cancel failed rename retains saved label");
            name(s,0,"");check(!s.game.label(0).equals("Heart"),"empty name returns to automatic description");
            GameCanvas stale=new GameCanvas(named);stale.rename();byte[] encoded=stale.encode();refused(()->GameCanvas.restore(encoded,original));
            WorkshopCartridge external=PlayModel.code(named,named.code().replace(",10,20)",",11,20)"));GameCanvas protectedNames=new GameCanvas(external);check(!protectedNames.blocked.isEmpty(),"external layout mismatch cannot silently misattach a name");same(external.bytes(),protectedNames.base.bytes(),"external source preserved");
            int annotation=named.code().indexOf("-- pikonest:name:");String line=named.code().substring(annotation,named.code().indexOf(nl,annotation)+nl.length());check(!new GameCanvas(PlayModel.code(named,line+named.code())).blocked.isEmpty(),"duplicate annotations refused");
            GameCanvas old=new GameCanvas(original);old.screen=GameCanvas.Screen.ACTIONS;old.focus=6;byte[] v4=old.encode();v4=Arrays.copyOf(v4,v4.length-27);java.nio.ByteBuffer.wrap(v4).putInt(4);old=GameCanvas.restore(v4,original);old.act(Action.CONFIRM);check(old.screen==GameCanvas.Screen.HOME,"v4 Back remains Back");
        }
        WorkshopCartridge source=GameRelationshipsTest.fixture("\n");GameElementNames unicode=GameElementNames.read(source,new GameUses(source,0));unicode.values.set(0,"\u0417\u0432\u0435\u0437\u0434\u0430 --[]");WorkshopCartridge safe=unicode.apply(source);check(new GameCanvas(safe).label(0).equals(unicode.values.get(0)),"Unicode/punctuation round trips through ASCII comment bytes");
        check(safe.code().chars().allMatch(c->c<128),"name cannot inject code or non-P8 source encoding");
        refused(()->GameElementNames.normalize("bad\ncode"));refused(()->GameElementNames.normalize("\u202ehidden"));refused(()->GameElementNames.normalize("\ud800"));refused(()->GameElementNames.normalize("1234567890123456789012345"));
        WorkshopCartridge titled=PlayModel.code(source,"-- My game\n-- By the author\n"+source.code());GameElementNames titleNames=GameElementNames.read(titled,new GameUses(titled,0));titleNames.values.set(0,"Heart");WorkshopCartridge withTitle=titleNames.apply(titled);check(withTitle.code().startsWith("-- My game\n-- By the author\n"),"title and author stay in first two lines");same(withTitle.bytes(),titleNames.apply(withTitle).bytes(),"reapplying names is byte-exact");
        NameEditor bounded=new NameEditor("123456789012345678901234",24,true);bounded.replaceAll=false;bounded.type(0);check(bounded.text().length()==24&&bounded.warning.startsWith("Limit"),"English limit feedback");
        System.out.println("GameIdentityTest: "+checks+" checks passed");
    }
}
