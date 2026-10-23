import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** Visual selection over real recipes, including draft Test/recovery and ambiguous placements. */
public final class GameRelationshipsTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);}
    static void picker(WorkshopSession s,int field){int n=0;while(s.game.formFields()[n]!=field)n++;row(s,n);check(s.game.screen==GameCanvas.Screen.RELATION,"relationship picker opened");}
    static WorkshopCartridge fixture(String nl){
        WorkshopCartridge cart=GameCanvasTest.cart("function _draw()\n cls(1)\n sspr(8,0,8,8,10,20)\n sspr(16,0,8,8,50,20)\n sspr(24,0,8,8,50,20)\n sspr(32,0,8,8,180,20)\nend\n",nl);
        return cart.withPixel(new SpriteRegion(8,0,8,8),0,0,14).withPixel(new SpriteRegion(16,0,8,8),0,0,12).withPixel(new SpriteRegion(24,0,8,8),0,0,12);
    }
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge cart=fixture(nl);PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(cart,port);
            check(s.game.label(0).equals("Pink sprite"),"visual label");check(s.game.label(1).equals("Blue sprite A")&&s.game.label(2).equals("Blue sprite B"),"duplicates distinguishable");
            check(s.game.label(3).equals("Empty sprite"),"transparent element named honestly");
            check(s.game.rowElement(0)==-1&&s.game.rowElement(4)==3,"list includes invisible off-field element");
            check(!s.game.pickElementAt(127,127)&&s.game.pickElementAt(50,20)&&s.game.selected==2,"canvas selects top overlapping element");
            s.act(Action.CONFIRM);check(s.game.screen==GameCanvas.Screen.ACTIONS,"canvas selection opens contextual actions");s.act(Action.CANCEL);
            PlayModelTest.behavior(s,2);PlayModelTest.field(s,2,1);picker(s,3);
            check(Arrays.equals(s.game.relationshipChoices(),new int[]{0,1,3}),"contact candidates exclude self, include empty/off-field");
            check(s.game.settings.elements.get(2).target==0,"initial contact unchanged");
            check(s.game.pickElementAt(50,20)&&s.game.relatedElement()==1,"hit testing skips source and reaches overlapping candidate");
            check(s.game.summary().contains("Blue sprite A touches Blue sprite B"),"relationship sentence identifies both ends");
            byte[] draft=s.game.encode();s.act(Action.TEST);
            check(PlayModel.read(new WorkshopCartridge(port.launched)).elements.get(2).target==1,"Test previews highlighted target");
            check(s.game.settings.elements.get(2).target==0&&s.game.screen==GameCanvas.Screen.RELATION,"Test retains picker and original binding");same(cart.bytes(),s.cart().bytes(),"Test leaves saved source exact");
            s.restoreGame(draft);check(s.game.relatedElement()==1,"recovery restores candidate focus");same(port.launched,s.game.candidate(cart).bytes(),"recovered preview identical");
            s.act(Action.CANCEL);check(s.game.screen==GameCanvas.Screen.BEHAVIOR&&s.game.settings.elements.get(2).target==0&&s.game.formFields()[s.game.focus]==3,"cancel restores binding and originating field");
            picker(s,3);s.game.focus=2;check(s.game.relatedElement()==3,"controller can reach off-field transparent target");s.act(Action.CONFIRM);
            row(s,s.game.rows().length-2);WorkshopCartridge saved=s.cart();check(s.game.play.elements.get(2).target==3,"confirmed relationship saved");s.act(Action.UNDO);same(cart.bytes(),s.cart().bytes(),"one Undo restores original cart");s.act(Action.REDO);same(saved.bytes(),s.cart().bytes(),"Redo restores exact cart");
            GameCanvas reopened=new GameCanvas(new WorkshopCartridge(saved.bytes()));check(reopened.play.elements.get(2).target==3,"reopen without metadata");
            PlayModelTest.behavior(s,2);picker(s,3);s.game.focus=1;s.act(Action.CONFIRM);port.fail=true;row(s,s.game.rows().length-2);
            check(s.mode==Mode.ERROR&&s.game.settings.elements.get(2).target==1,"storage failure retains confirmed draft");same(saved.bytes(),s.cart().bytes(),"failed save preserves bytes");port.fail=false;s.act(Action.CANCEL);s.act(Action.CANCEL);
            PlayModel puzzle=PlayModel.fromStatic(cart);puzzle.elements.get(0).rule=4;puzzle.elements.get(1).rule=4;puzzle.elements.get(3).rule=4;puzzle.goal=2;
            WorkshopCartridge switches=puzzle.write(cart);s=PlayModelTest.session(switches,port);PlayModelTest.behavior(s,0);picker(s,5);
            check(Arrays.equals(s.game.relationshipChoices(),new int[]{-1,1,3}),"linked switch list excludes ordinary sprites and self");s.act(Action.DOWN);check(s.game.relatedElement()==1,"D-pad chooses next eligible switch");
            s.act(Action.TEST);check(PlayModel.read(new WorkshopCartridge(port.launched)).elements.get(0).neighbor==1,"linked switch draft Test");
            s.act(Action.CONFIRM);row(s,s.game.rows().length-2);check(s.game.play.elements.get(0).neighbor==1,"linked switch saves");
            PlayModelTest.behavior(s,0);picker(s,5);s.game.focus=0;s.act(Action.CONFIRM);check(s.game.settings.elements.get(0).neighbor==-1,"explicit no-link choice");s.act(Action.CANCEL);
            PlayModelTest.behavior(s,0);picker(s,5);s.game.focus=s.game.rows().length-1;s.act(Action.CONFIRM);check(s.game.settings.elements.get(0).neighbor==1,"cancel row preserves link");
            byte[] stale=s.game.encode();refused(()->GameCanvas.restore(stale,cart));
            GameCanvas original=new GameCanvas(cart);byte[] v3=original.encode();v3=Arrays.copyOf(v3,v3.length-18);java.nio.ByteBuffer.wrap(v3).putInt(3);same(cart.bytes(),GameCanvas.restore(v3,cart).candidate(cart).bytes(),"v3 recovery remains byte-exact");
            GameCanvas bad=new GameCanvas(PlayModel.code(cart,cart.code()+"poke(0,1)\n"));check(!bad.blocked.isEmpty()&&!bad.pickElementAt(10,20),"unknown Lua cannot acquire visual-edit bindings");
        }
        WorkshopCartridge one=GameCanvasTest.cart("function _draw()\n cls(1)\n spr(1,10,10)\nend\n","\n");WorkshopSession s=PlayModelTest.session(one,new PlayModelTest.Port());PlayModelTest.behavior(s,0);PlayModelTest.field(s,2,1);
        check(s.game.relationshipChoices().length==0&&s.game.rows()[0].contains("Add another"),"missing prerequisite explained");byte[] empty=s.game.encode();GameCanvas recovered=GameCanvas.restore(empty,one);check(recovered.screen==GameCanvas.Screen.RELATION,"incomplete relationship can recover");refused(()->recovered.candidate(one));recovered.act(Action.CANCEL);check(recovered.screen==GameCanvas.Screen.CHOICE,"empty picker returns to interaction choices");
        List<SpriteRegion> many=new ArrayList<>();for(int n=0;n<28;n++)many.add(new SpriteRegion(0,0,8,8));GameElementLabels labels=new GameElementLabels(one,many);check(labels.get(26).equals("Empty sprite AA")&&labels.get(27).equals("Empty sprite AB"),"static list has no invented 26-element limit");
        System.out.println("GameRelationshipsTest: "+checks+" checks passed");
    }
}
