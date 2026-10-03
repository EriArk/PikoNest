import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LuaCallTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable action,String why){try{action.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge cart(String code){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code+"__future__\nkeep\n").getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] saved;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("full");saved=bytes;saves++;}
        public void launch(byte[] bytes){check(Arrays.equals(saved,bytes),"saved source launches");launches++;}
    }
    public static void main(String[] args){
        for(String call:new String[]{" cls ( 0x01 ) ; -- keep","\tprint( 'a,b)', 1, 2, 7 ) --keep", "circfill(64,64,score,14)",
            "rectfill(f(1,2),t[{1,2}],30,40,col)","spr(0, 10,20)"}){
            for(String eol:new String[]{"\n","\r\n","\r"}){
                String source=call+eol+"-- unrelated"+eol;LuaDraft d=new LuaDraft(cart(source),0);d.point(0,3);int cursor=d.cursor();
                d.beginParameters();check(d.text().equals(source),"open does not rewrite "+call);
                check(d.callEdit.preview(d.insertion).equals(call),"exact preview when unchanged");
                LuaDraft restored=LuaDraft.restore(d.encode());check(restored.callEdit.preview(restored.insertion).equals(call),"form recovery");
                d.applyInsert();check(d.text().equals(source)&&d.cursor()==cursor&&!d.canUndo(),"no-op has no history or caret movement");
            }
        }
        LuaDraft d=new LuaDraft(cart("\tcircfill ( 64 , 64,  score , 0x0e ) ; --keep\r\nother=1\r\n"),0);
        d.beginParameters();d.insertion.set(2,"score*2");d.applyInsert();
        check(d.text().equals("\tcircfill ( 64 , 64,  score*2 , 0x0e ) ; --keep\r\nother=1\r\n"),"only changed argument replaced, whitespace/hex/comment untouched");
        d.history(false);check(d.text().contains("  score ,"),"one-step undo");d.history(true);check(d.text().contains("score*2"),"redo");
        d=new LuaDraft(cart("print('hello', 1, 2, 14)\n"),0);d.beginParameters();check(d.insertion.kind(0)==LuaInsert.Kind.STRING,"simple literal gets text editor");
        d.insertion.set(0,"it's \\\"ok\"");d.applyInsert();check(d.text().startsWith("print(\"it's \\\\\\\"ok\\\"\", 1, 2, 14)"),"changed text quoted/escaped only in its span");
        for(String expr:new String[]{"'a\\nb'","[[a,b)]]","'a'..'b'"}){
            d=new LuaDraft(cart("print("+expr+",1,2,7)\n"),0);d.beginParameters();check(d.insertion.kind(0)==LuaInsert.Kind.EXPR,"complex literal preserved as expression");d.insertion.set(1,"3");d.applyInsert();check(d.text().equals("print("+expr+",3,2,7)\n"),"complex literal untouched");
        }
        for(String code:new String[]{"cls()","print(1)","spr(0,1,2,2,2)","if a then cls(1) end","t.cls(1)",
            "cls(1) cls(2)","-- cls(1)","circfill(1,2,3,4","circfill(1,2,3,4]","circfill(1,,3,4)","circfill(1,2,3,4) + x"}){
            final LuaDraft rejected=new LuaDraft(cart(code+"\n"),0);refused(rejected::beginParameters,"unsafe/unsupported line accepted: "+code);check(rejected.text().equals(code+"\n"),"refusal preserves source");
        }
        for(String prefix:new String[]{"--[[\n","s=[=[\n","function cls(x) end\n","local cls=fn\n","function test(cls) end\n"}){
            final LuaDraft rejected=new LuaDraft(cart(prefix+"cls(1)\n"),1);refused(rejected::beginParameters,"comment/string/override accepted");
        }
        for(String expression:new String[]{"1,2","1) evil(2","1 -- swallowed","f("}){
            final LuaDraft escaped=new LuaDraft(cart("circfill(1,2,3,4)\n"),0);escaped.beginParameters();escaped.insertion.set(2,expression);
            refused(escaped::applyInsert,"argument escaped call");check(escaped.text().equals("circfill(1,2,3,4)\n")&&escaped.proposal(),"failed apply retains original and form");
        }
        LuaDraft color=new LuaDraft(cart("circfill(1,2,3,col)\n"),0);color.beginParameters();color.insertion.field=3;
        check(color.insertion.canBrowse(),"expression color uses existing name/API chooser");color.insertion.set(3,"14");color.insertion.step(-1);check(color.insertion.value(3).equals("13"),"literal color arrows use palette");
        color.insertion.beginText();color.insertion.type("col");color.insertion.acceptText();color.applyInsert();check(!color.dirty(),"restore original color expression is no-op");
        LuaInsert invalid=new LuaInsert();invalid.choose(12);refused(()->invalid.set(0,"\ud800"),"invalid Unicode silently encoded");
        LuaDraft stale=new LuaDraft(cart("cls(1)\n"),0);stale.beginParameters();stale.replace("-- ");refused(stale::applyInsert,"stale source overwritten");
        Port port=new Port();WorkshopSession s=new WorkshopSession(cart("circfill(64,64,3,14)\n"),port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.PREVIOUS);
        check(s.codeDraft.panel==LuaDraft.Panel.PARAMETERS,"controller L opens current line");s.act(Action.TEST);s.codeCommand(1);check(port.saves==0,"modal start/save trapped");
        s.act(Action.RIGHT);byte[] encoded=s.codeDraft.encode();s.restoreCode(encoded);check(s.codeDraft.insertion.value(0).equals("65"),"unfinished changed field recovery");
        s.act(Action.CANCEL);check(!s.codeDraft.dirty(),"cancel returns directly to unchanged code");
        s.codeCommand(19);for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(!s.codeDraft.canUndo(),"menu route no-op");
        s.act(Action.PREVIOUS);s.act(Action.RIGHT);for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        port.fail=true;s.act(Action.TEST);check(port.saves==0&&s.codeDraft!=null,"failed save retains edit");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);
        check(port.launches==1&&new String(port.saved,StandardCharsets.UTF_8).endsWith("__future__\nkeep\n"),"targeted save and launch preserve unrelated section");
        LuaDraft old=new LuaDraft(cart("cls(1)\n"),0);old.beginInsert();byte[] v3=old.encode();v3[3]=3;check(LuaDraft.restore(v3).panel==LuaDraft.Panel.INSERT,"format 3 insertion migration");
        System.out.println("Lua call parameters: "+checks+" checks passed");
    }
}
