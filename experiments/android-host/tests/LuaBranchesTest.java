import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

public final class LuaBranchesTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static String source(String eol){return String.join(eol,"function _update()"," if phase==0 then -- main","  if btnp(0) then","   score+=1","  end"," elseif phase==1 then","  score=9"," else","  -- empty"," end","end","");}
    static LuaDraft draft(String text){return new LuaDraft(LuaNavigationTest.cart(text),0);}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String text=source(nl);LuaBranches b=new LuaBranches(text,3);
            check(b.warning.isEmpty()&&b.entries.size()==4,"nested branches");check(b.index==1,"nearest inner branch");
            check(b.entries.get(0).scope.equals("_update")&&b.entries.get(0).title.endsWith("phase==0"),"function context and clean condition label");
            check(b.entries.get(0).end==5&&b.entries.get(1).end==4&&b.entries.get(2).end==7,"precise boundaries");
            check(b.entries.get(3).first==7,"comment-only branch empty");
            LuaDraft d=draft(text);d.beginBranches();d.branches.index=3;
            LuaDraft recovered=LuaDraft.restore(d.encode());check(recovered.branches.index==3,"recover selected branch");
            recovered.branchAction(true);check(recovered.text().equals(text)&&recovered.line()==9,"proposal leaves source intact");
            recovered.insertion.choose(7);recovered.insertion.screen=LuaInsert.Screen.FIELDS;recovered.insertion.set(0,"phase");
            LuaDraft again=LuaDraft.restore(recovered.encode());again.applyInsert();recovered.applyInsert();
            String expected=text.replace("  -- empty"+nl+" end","  -- empty"+nl+"   phase=0"+nl+" end");
            check(again.text().equals(expected)&&again.text().equals(recovered.text()),"only selected else changed including CRLF");
            check(again.lineText(again.line()).trim().equals("phase=0"),"caret ready for parameter editing");
            recovered.history(false);check(recovered.text().equals(text),"single exact undo");recovered.history(true);check(recovered.text().equals(expected),"redo");
            d.branchAction(true);d.cancelInsert();check(d.panel==LuaDraft.Panel.BRANCHES&&d.branches.index==3&&d.text().equals(text),"cancel retains branch choice");
            d.branchAction(false);check(d.line()==7&&!d.dirty(),"empty branch opens header without adding blank lines");
            d.beginBranches();d.branches.index=1;d.branchAction(false);check(d.line()==3,"open first existing action");d.beginParameters();d.insertion.set(1,"2");d.applyInsert();check(d.text().equals(text.replace("score+=1","score+=2")),"edit action through ordinary form");
        }
        String loops="function f()\n if ready then\n  for i=1,3 do\n   while(x<2) do\n    repeat\n     x+=1\n    until x>0\n   end\n  end\n  do\n   x+=1\n  end\n else\n end\nend\n";
        LuaBranches nested=new LuaBranches(loops,0);check(nested.entries.size()==2&&nested.entries.get(0).end==12,"all block types balanced");
        String protectedSource="s=[=[\nif fake then\nelse\nend\n]=]\n--[[if hidden then\nend]]\nif s==\"end\" then\n print(\"else\") -- if fake then\nelse\nend\n";
        check(new LuaBranches(protectedSource,0).entries.size()==2,"strings and comments masked");
        check(!new LuaBranches("if a then\n if b then x=1 end; return 2\nend\n",0).warning.isEmpty(),"inline block cannot hide unconditional return after it");
        for(String inline:new String[]{"function f() if a then return 1 else return 2 end end", "if a then x=1 elseif b then x=2 else x=3 end","for i=1,3 do while a do x+=1 end end", "repeat x+=1 until x>2"}){
            LuaBranches b=new LuaBranches(inline+"\nif visible then\n "+inline+"\nelse\nend\n",0);
            check(b.entries.size()==2&&b.warning.isEmpty()&&b.entries.get(0).end==3,"inline helper leaves enclosing branches available");
        }
        for(String bad:new String[]{"if a then x=1 end end\n","if (a) x=1\n","if a then\n","else\nend\n","if a then\nelse\nelse\nend\n","repeat\nend\n","#include other.lua\n","s=\"unfinished\n","function f()\nif a then\nend\n"}){
            LuaBranches b=new LuaBranches(bad,0);check(!b.warning.isEmpty()&&b.entries.isEmpty(),"unsupported source safely unavailable");
        }
        for(String exit:new String[]{"return 1","break","x=1; return 2","goto done"}){
            LuaDraft d=draft("if ready then\n "+exit+"\nend\n");d.beginBranches();check(d.branches.current().terminal,"terminal detected");refused(()->d.branchAction(true));check(!d.dirty(),"terminal source intact");
        }
        LuaDraft d=draft("if a then\nelse\nend\n");d.beginBranches();d.branchAction(true);d.insertion.choose(0);d.insertion.screen=LuaInsert.Screen.FIELDS;refused(d::applyInsert);check(!d.dirty(),"callback not nested silently");
        d.insertion.choose(30);d.insertion.screen=LuaInsert.Screen.PREVIEW;d.applyInsert();check(d.text().startsWith("if a then\n  if phase==0 then\n    \n  else\n    \n  end\nelse"),"nested condition inserted into correct branch");
        LuaDraft empty=draft("\n");empty.beginBranches();check(LuaDraft.restore(empty.encode()).branches.entries.isEmpty(),"empty picker recovery");
        byte[] broken=empty.encode();broken[broken.length-1]=2;refused(()->LuaDraft.restore(broken));
        LuaNavigationTest.Port port=new LuaNavigationTest.Port();WorkshopSession s=new WorkshopSession(LuaNavigationTest.cart(source("\n")),port);
        s.switchTool(1);s.act(Action.CONFIRM);s.codeCommand(23);s.act(Action.DOWN);s.act(Action.TEST);s.act(Action.UNDO);s.codeCommand(0);s.codeText("damage");
        check(port.writes==0&&port.launches==0&&!s.codeDraft.dirty(),"picker traps mutation and launch");
        s.act(Action.CONTEXT);check(s.codeDraft.branchInsertion(),"controller add");
        s.codeDraft.insertion.choose(18);s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.codeDraft.branchInsertion(),"visual path cannot lose target");s.act(Action.CANCEL);s.act(Action.CANCEL);
        check(s.codeDraft.panel==LuaDraft.Panel.BRANCHES&&!s.codeDraft.dirty(),"error and cancel preserve selection");s.act(Action.CANCEL);s.act(Action.CONTEXT);s.codeDraft.insertion.choose(31);s.act(Action.CONFIRM);check(s.codeDraft.panel==LuaDraft.Panel.BRANCHES,"catalogue entry");
        System.out.println("LuaBranchesTest: "+checks+" checks passed");
    }
}
