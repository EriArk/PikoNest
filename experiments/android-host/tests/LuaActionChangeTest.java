import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.Arrays;

public final class LuaActionChangeTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable action){try{action.run();throw new AssertionError("unsafe proposal accepted");}catch(IllegalArgumentException expected){checks++;}}
    static LuaDraft draft(String text,int line){
        WorkshopCartridge cart=(text.endsWith("\n")||text.endsWith("\r"))?LuaNavigationTest.cart(text):new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n"+text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LuaDraft d=new LuaDraft(cart,0);d.point(line,0);d.beginBranches();d.beginActions();return d;
    }
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String text=String.join(nl,"-- outside","if ready then"," -- keep comment"," score+=1 -- first"," "," print(score,16,24,12) -- second"," cls(1)","else"," custom()","end","");
            LuaActionChange move=new LuaActionChange(text,1,5,0);
            String swapped=text.replace(" score+=1 -- first"+nl+" "+nl+" print(score,16,24,12) -- second", " print(score,16,24,12) -- second"+nl+" "+nl+" score+=1 -- first");
            check(move.result.equals(swapped)&&move.selectedLine==3,"swap exact statements including inline comments; keep blank gap");
            check(new LuaActionChange(swapped,1,3,1).result.equals(text),"move down exactly reverses up");
            for(int line:new int[]{3,5,6}){
                LuaActionChange del=new LuaActionChange(text,1,line,2);
                check(del.result.equals(text.replace(text.split("\\r\\n|\\r|\\n",-1)[line]+nl,"")),"delete first/middle/last keeps sibling and comments");
                check(!del.result.contains("__piko"),"no runtime dependency");
            }
            LuaDraft d=draft(text,5);d.actionOptions();d.reviewAction();
            check(d.text().equals(text)&&!d.dirty()&&!d.canUndo(),"proposal is read-only");
            d.actionBefore=true;d.actionColumn=8;
            d=LuaDraft.restore(d.encode());check(d.actionChange.result.equals(swapped)&&d.actionBefore&&d.actionColumn==8,"proposal recovery recalculates same operation");
            d.applyAction();check(d.text().equals(swapped)&&d.actions.current().line==3&&d.actionMenu==-1,"apply returns to moved action");
            d.openAction(true);d.history(false);check(d.text().equals(text),"single undo");d.history(true);check(d.text().equals(swapped),"redo");
            d=draft(text,6);d.actionOptions();d.actionMenu=2;d=LuaDraft.restore(d.encode());check(d.actionMenu==2&&d.actionChange==null,"menu recovery");
            d.reviewAction();d.applyAction();check(d.actions.index==1&&d.actions.current().line==5,"last deletion selects preceding action");
            LuaDraft one=draft("if ok then"+nl+" cls(1)"+nl+"end",1);one.actionOptions();one.actionMenu=2;one.reviewAction();one.applyAction();
            check(one.actions.entries.isEmpty()&&one.text().equals("if ok then"+nl+"end"),"last action leaves legal empty branch; final newline unchanged");
            one.actionsToBranches();one.cancelBranches();one.history(false);check(one.text().contains("cls(1)"),"empty branch deletion undo");
        }
        String simple="if ready then\n cls(1)\n cls(2)\nend\n";
        refused(()->new LuaActionChange(simple,0,1,0));refused(()->new LuaActionChange(simple,0,2,1));
        refused(()->new LuaActionChange(simple,0,0,2));refused(()->new LuaActionChange(simple,0,1,3));
        for(String code:new String[]{"custom()","local score=1","if inner then\n  cls(2)\n end","return 1","print(\n 1,2,3,4)","x=1\n +2","(fn)()"}){
            String text="if ok then\n cls(1)\n "+code+"\nend\n";
            refused(()->new LuaActionChange(text,0,1,2));refused(()->new LuaActionChange(text,0,1,1));
        }
        String comments="if ok then\n cls(1)\n -- belongs where?\n cls(2)\nend\n";
        refused(()->new LuaActionChange(comments,0,1,1));
        check(new LuaActionChange(comments,0,1,2).result.contains(" -- belongs where?\n cls(2)"),"delete retains standalone comment");
        String same="if ok then\n cls(1)\n cls(1)\nend\n";LuaDraft noop=draft(same,2);noop.actionOptions();noop.reviewAction();noop.applyAction();check(!noop.canUndo()&&!noop.dirty(),"identical swap no history");
        LuaDraft stale=draft(simple,2);stale.actionOptions();stale.reviewAction();stale.replace(" ");String changed=stale.text();refused(stale::applyAction);check(stale.text().equals(changed),"stale proposal cannot overwrite new source");
        LuaDraft journal=draft(simple,2);journal.actionOptions();journal.reviewAction();byte[] good=journal.encode();
        refused(()->LuaDraft.restore(Arrays.copyOf(good,good.length-1)));refused(()->LuaDraft.restore(Arrays.copyOf(good,good.length+1)));
        byte[] bad=good.clone();bad[bad.length-11]=9;refused(()->LuaDraft.restore(bad));
        LuaNavigationTest.Port port=new LuaNavigationTest.Port();WorkshopSession s=new WorkshopSession(LuaNavigationTest.cart(simple),port);
        s.switchTool(1);s.act(Action.CONFIRM);s.codeCommand(23);s.act(Action.CONFIRM);s.act(Action.DOWN);s.act(Action.MENU);s.act(Action.CONFIRM);
        s.act(Action.TEST);s.act(Action.UNDO);s.codeCommand(1);s.codeText("damage");check(port.writes==0&&port.launches==0&&!s.codeDraft.dirty(),"review traps background commands");
        s.act(Action.CONTEXT);check(s.codeDraft.actionBefore,"before view");s.act(Action.CANCEL);check(s.codeDraft.actionChange==null&&s.codeDraft.actionMenu==0&&!s.codeDraft.dirty(),"cancel review retains menu");
        s.act(Action.CANCEL);check(s.codeDraft.actionMenu==-1&&s.codeDraft.actions.index==1,"cancel menu restores selected action");
        s.act(Action.MENU);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.codeDraft.text().equals(simple.replace(" cls(1)\n cls(2)"," cls(2)\n cls(1)")),"controller apply");
        System.out.println("LuaActionChangeTest: "+checks+" checks passed");
    }
}
