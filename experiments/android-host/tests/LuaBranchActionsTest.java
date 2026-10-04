import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;

public final class LuaBranchActionsTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static LuaDraft draft(String text){return new LuaDraft(LuaNavigationTest.cart(text),0);}
    static void refused(byte[] bytes){try{LuaDraft.restore(bytes);throw new AssertionError("invalid journal accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String text=String.join(nl,"if ready then"," -- comment"," print(score,16,24,12) -- keep"," if inner then","  cls(1)"," else","  cls(2)"," end"," custom()"," print(target,32,24,12)","else"," -- empty","end","");
            LuaBranchActions a=new LuaBranchActions(text,0,9);
            check(a.entries.size()==4&&a.index==3,"direct actions only");
            check(a.entries.get(0).fields&&!a.entries.get(1).fields&&!a.entries.get(2).fields&&a.current().fields,"supported forms and opaque code");
            check(a.entries.get(1).line==3&&a.entries.get(1).end==8&&a.entries.get(1).source.contains("else"),"whole nested block retained");
            LuaDraft d=draft(text);d.beginBranches();d.beginActions();d.actions.index=3;
            d=LuaDraft.restore(d.encode());check(d.panel==LuaDraft.Panel.ACTIONS&&d.actions.index==3&&!d.dirty(),"list recovery");
            d.openAction(false);check(d.panel==LuaDraft.Panel.PARAMETERS&&d.insertion.value(0).equals("target"),"chosen call opens fields");
            d.insertion.set(3,"11");d=LuaDraft.restore(d.encode());check(d.insertion.value(3).equals("11"),"pending form recovery");
            d.cancelInsert();check(d.panel==LuaDraft.Panel.ACTIONS&&d.actions.index==3&&d.text().equals(text),"cancel returns to selected action without writes");
            d.openAction(false);d.insertion.set(3,"11");d.applyInsert();
            String expected=text.replace("print(target,32,24,12)","print(target,32,24,11)");
            check(d.text().equals(expected)&&d.panel==LuaDraft.Panel.ACTIONS&&d.actions.index==3,"only selected argument changes; return to list");
            d.openAction(true);check(d.panel==LuaDraft.Panel.CURSOR&&d.line()==9,"explicit code opens same action");
            d.history(false);check(d.text().equals(text),"one undo restores exact bytes");d.history(true);check(d.text().equals(expected),"redo");
            d.beginBranches();d.branches.index=0;d.beginActions();d.actions.index=1;d.openAction(false);check(d.panel==LuaDraft.Panel.CURSOR&&d.line()==3,"nested block opens source");
            d.beginBranches();d.branches.index=3;d.beginActions();check(d.actions.entries.isEmpty(),"empty branch");
            d=LuaDraft.restore(d.encode());d.openAction(false);check(d.panel==LuaDraft.Panel.ACTIONS&&d.actions.current()==null,"empty list recovery and confirm");
        }
        String groups="if ok then\n for i=1,3 do\n  x+=i\n end\n repeat\n  x+=1\n until x>5\n local t={\n  1,2\n }\n print(\n  score,1,2,3)\n s=[=[\n fake()\n ]=]\n cls(1)\nend\n";
        LuaBranchActions a=new LuaBranchActions(groups,0,0);
        check(a.entries.size()==6,"loops, tables, calls, strings grouped");
        for(int n=0;n<5;n++)check(!a.entries.get(n).fields,"multiline code stays code-only");
        check(a.entries.get(5).fields,"next single-line action remains editable");
        LuaDraft journal=draft("if ok then\n cls(1)\nend\n");journal.beginBranches();journal.beginActions();
        byte[] good=journal.encode(),bad=good.clone();bad[bad.length-2]=2;refused(bad);
        bad=good.clone();bad[bad.length-6]=1;refused(bad);
        bad=good.clone();bad[bad.length-1]=1;refused(bad);
        refused(java.util.Arrays.copyOf(good,good.length-1));refused(java.util.Arrays.copyOf(good,good.length+1));
        bad=good.clone();bad[11]=10;refused(bad);
        LuaNavigationTest.Port port=new LuaNavigationTest.Port();WorkshopSession s=new WorkshopSession(LuaNavigationTest.cart("if ready then\n cls(1)\n cls(2)\nend\n"),port);
        s.switchTool(1);s.act(Action.CONFIRM);s.codeCommand(23);s.act(Action.CONFIRM);s.act(Action.DOWN);
        s.act(Action.TEST);s.act(Action.UNDO);s.codeCommand(1);s.codeText("damage");
        check(port.writes==0&&port.launches==0&&!s.codeDraft.dirty()&&s.codeDraft.actions.index==1,"modal controller traps save, launch, text, undo");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CANCEL);check(s.codeDraft.actions.index==1&&!s.codeDraft.dirty(),"controller form cancel");
        s.act(Action.CANCEL);check(s.codeDraft.panel==LuaDraft.Panel.BRANCHES&&s.codeDraft.branches.index==0,"back restores branch");
        System.out.println("LuaBranchActionsTest: "+checks+" checks passed");
    }
}
