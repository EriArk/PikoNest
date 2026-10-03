import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;

public final class LuaRuleTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable run,String why){try{run.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){checks++;}}
    static LuaDraft draft(String code){return new LuaDraft(LuaCallTest.cart(code),0);}
    public static void main(String[] args){
        for(String line:new String[]{" if score >= 5 then -- goal", "elseif score!=0 then", "if btnp(4) then", "if ready and score<10 then",
            "if (score>=5) then", "if (a or b) == true then", "if s == 'then -- \\\" end' then", "if f(1,2)>t[i+1] then",
            "score=0", "\tlocal score = -2 ; -- keep", " score += 1 -- increment", "x=print(1,2,3,4)", "s='cls(1)'",
            "x=0x1.8+0b10.1", "x=-(2^3)", "x=a..'b'", "x=f(g(1,2),t[i])", "x=false", "x=nil"}){
            for(String eol:new String[]{"\n","\r\n","\r"}){
                String source=line+eol+"-- untouched"+eol;LuaDraft d=draft(source);d.point(0,2);int cursor=d.cursor();
                d.beginParameters();check(d.callEdit.preview(d.insertion).equals(line),"no-op preview: "+line);
                d=LuaDraft.restore(d.encode());d.applyInsert();
                check(d.text().equals(source)&&d.cursor()==cursor&&!d.canUndo(),"no-op/recovery preserves bytes and history");
            }
        }
        String source="\tif  score  >=  5 then -- keep\r\n print('goal')\r\nend\r\n";
        LuaDraft d=draft(source);d.beginParameters();check(d.insertion.item().id.equals("compare"),"comparison split into fields");
        d.insertion.field=2;d.insertion.step(1);check(d.callEdit.preview(d.insertion).equals("\tif  score  >=  6 then -- keep"),"only threshold preview");
        d=LuaDraft.restore(d.encode());d.applyInsert();String changed=source.replace(">=  5",">=  6");
        check(d.text().equals(changed),"body, spacing and comment preserved");d.history(false);check(d.text().equals(source),"single undo");d.history(true);check(d.text().equals(changed),"redo");
        d=draft(" local hp = 0x03 ; -- local stays\nx=hp\n");d.beginParameters();d.insertion.set(1,"4");d.applyInsert();
        check(d.text().equals(" local hp = 4 ; -- local stays\nx=hp\n"),"local and unrelated use preserved");
        d=draft("score+=1 --keep\n");d.beginParameters();d.insertion.set(0,"points");d.insertion.set(1,"2");d.applyInsert();check(d.text().equals("points+=2 --keep\n"),"targeted name/value change");
        for(String op:LuaInsert.COMPARISONS){d=draft("if score>=5 then\nend\n");d.beginParameters();d.insertion.set(1,op);d.applyInsert();check(d.text().startsWith("if score"+op+"5 then"),"operator "+op);}
        for(String line:new String[]{"if a then x=1 end", "if(a) x=1", "if x then;", "if a=2 then", "if x -- then", "if a + then",
            "local x+=1", "a,b=1,2", "t.x=3", "t[1]=3", "score=1 score=2", "score=1;score=2", "score=1,2", "score=()",
            "score=f(", "score='unclosed", "score=[[long]]", "score=function() end", "score={a=1}", "score=1 --[[ hidden ]] x=2", "score=(1]"}){
            final LuaDraft bad=draft(line+"\n");refused(bad::beginParameters,"unsupported accepted: "+line);check(bad.text().equals(line+"\n"),"refusal preserved source");
        }
        for(String prefix:new String[]{"--[[\n","s=[[\n","t={\n","x=(\n"}){
            LuaDraft nested=draft(prefix+"score=1\n");nested.point(1,0);refused(nested::beginParameters,"nested source accepted");
        }
        for(String value:new String[]{"1; evil()", "1 -- hide", "1,2", "1 then evil() end if true", "1 or evil()", "1>=2", "1 --[[x]]", "f("}){
            final LuaDraft bad=draft("if score>=5 then --keep\n work()\nend\n");bad.beginParameters();bad.insertion.set(2,value);
            refused(bad::applyInsert,"comparison field escaped: "+value);check(!bad.dirty()&&bad.proposal(),"failed apply keeps proposal/source");
        }
        for(String value:new String[]{"1; evil()", "1 -- hide", "1,2", "1 other=2", "function() end"}){
            final LuaDraft bad=draft("x=1 --keep\n");bad.beginParameters();bad.insertion.set(1,value);refused(bad::applyInsert,"assignment field escaped");
        }
        LuaDraft stale=draft("score=0\n");stale.beginParameters();stale.replace("-- ");refused(stale::applyInsert,"stale line overwritten");
        LuaDraft insert=draft("\n");insert.beginInsert();insert.insertion.choose(17);insert.insertion.screen=LuaInsert.Screen.FIELDS;
        insert.insertion.set(0,"a or b");refused(insert::applyInsert,"ambiguous comparison insertion");insert.insertion.set(0,"(a or b)");insert.applyInsert();check(insert.text().startsWith("if (a or b)>=5 then"),"explicit grouping accepted");
        LuaCallTest.Port port=new LuaCallTest.Port();WorkshopSession s=new WorkshopSession(LuaCallTest.cart("if score>=5 then\nend\n"),port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.PREVIOUS);
        s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.RIGHT);s.act(Action.TEST);check(port.saves==0,"Start cannot commit a form");
        s.restoreCode(s.codeDraft.encode());check(s.codeDraft.insertion.value(2).equals("6"),"controller proposal recovers");
        s.act(Action.CANCEL);check(!s.codeDraft.dirty(),"B cancels unchanged");s.act(Action.PREVIOUS);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.RIGHT);s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.UNDO);check(s.codeDraft.text().contains(">=5"),"controller undo");s.act(Action.REDO);s.act(Action.TEST);check(port.launches==1,"save/Test path");
        System.out.println("Lua rule fields: "+checks+" checks passed");
    }
}
