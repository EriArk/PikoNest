import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class LuaInsertTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int saves,launches;byte[] saved;boolean fail;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("full");saved=bytes;saves++;}
        public void launch(byte[] bytes){same(saved,bytes,"launch uses saved source");launches++;}
    }
    static LuaDraft draft(String code){return new LuaDraft(new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\n"+code+"__future__\r\nopaque\r\n").getBytes(StandardCharsets.UTF_8)),0);}
    static void choose(LuaDraft d,int item){d.beginInsert();d.insertion.choose(item);d.insertion.screen=LuaInsert.Screen.FIELDS;}
    static void apply(LuaDraft d,int item){choose(d,item);d.applyInsert();}
    static void refused(Runnable run,String why){try{run.run();throw new AssertionError(why);}catch(IllegalArgumentException e){checks++;}}
    public static void main(String[] args)throws Exception{
        for(int item=0;item<LuaInsert.ITEMS.length;item++){
            LuaDraft d=draft("  \r\n");byte[] before=d.encode();String source=d.text();choose(d,item);
            check(d.text().equals(source),"preview not edit "+item);
            LuaDraft recovery=LuaDraft.restore(d.encode());check(recovery.insertion.item().id.equals(d.insertion.item().id),"proposal recovery "+item);
            recovery.applyInsert();d.applyInsert();check(d.text().equals(recovery.text()),"same recovered insertion "+item);
            check(d.text().contains("\r\n")&&!d.text().replace("\r\n","").contains("\n"),"CRLF retained "+item);
            check(d.column()==(LuaInsert.ITEMS[item].id.startsWith("solid")?0:LuaInsert.ITEMS[item].block?4:2),"caret placed at editable next location "+item);
            d.history(false);check(d.text().equals(source)&&d.cursor()==0,"one-step exact undo including cursor "+item);
            d.history(true);check(!d.text().equals(source),"redo complete insertion "+item);
        }
        LuaDraft d=draft("-- function _init()\r\ns=\"function _init()\"\r\n");apply(d,0);
        check(d.text().startsWith("function _init()"),"comments/strings not mistaken for function");
        d.point(0,0);choose(d,0);String before=d.text();refused(d::applyInsert,"duplicate function accepted");check(before.equals(d.text()),"duplicate preserves text");
        LuaDraft assigned=draft("_init = function() end\r\n");choose(assigned,0);refused(assigned::applyInsert,"assigned callback replaced");
        for(String code:new String[]{"--[[\r\ninside\r\n]]\r\n","s=[=[\r\ninside\r\n]=]\r\n","s=\"escaped\\\"\r\ninside\"\r\n"}){
            LuaDraft protectedDraft=draft(code);protectedDraft.point(1,0);refused(protectedDraft::beginInsert,"inside string/comment accepted");
        }
        d=draft("--[[\r\n");d.point(1,0);refused(d::beginInsert,"unclosed long comment at EOF accepted");
        d=draft("s=\"unfinished\r\n");d.point(1,0);refused(d::beginInsert,"unclosed quoted string at EOF accepted");
        String closed="s=\"escaped\\\" quote\"\n";
        check(new LuaContext(closed).allowsLine(closed.length()),"closed quoted string releases next line");
        check(new LuaContext("--[=[done]=]\n").allowsLine(13),"closed long comment releases EOF");
        d=draft("print(1)\r\n");d.selectAll();refused(d::beginInsert,"selection overwritten");
        LuaInsert text=new LuaInsert();text.choose(12);text.set(0,"a\"b\\c");check(text.code().startsWith("print(\"a\\\"b\\\\c\""),"string quoting");
        refused(()->text.set(0,"a\nb"),"multiline field accepted");
        LuaInsert name=new LuaInsert();name.choose(7);refused(()->name.set(0,"end"),"keyword name accepted");refused(()->name.set(0,"a b"),"invalid identifier accepted");
        name.set(0,"_score2");name.field=1;name.step(-1);check(name.value(1).equals("-1"),"numeric step");name.set(1,"x+1");name.step(1);check(name.value(1).equals("x+1"),"expression not overwritten by numeric step");
        name.beginText();name.type("5");name.acceptText();check(name.value(1).equals("5"),"selected initial parameter replaced");
        LuaInsert button=new LuaInsert();button.choose(9);button.step(1);button.step(1);check(button.value(0).equals("5"),"button picker range");button.field=1;button.step(1);check(button.code().contains("btn(5)"),"held vs press");

        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));Port port=new Port();
        WorkshopSession s=new WorkshopSession(blank,port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.CONTEXT);
        check(s.codeDraft.panel==LuaDraft.Panel.INSERT,"controller shortcut");s.act(Action.TEST);check(port.launches==0,"catalog traps Start");
        s.act(Action.CONFIRM);s.act(Action.TEST);check(port.saves==0,"preview traps Start");s.act(Action.CONFIRM);
        check(s.codeDraft.text().startsWith("function _init()\n  \nend\n"),"controller inserts callback");
        s.act(Action.CONTEXT);for(int n=0;n<7;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.DOWN);s.act(Action.RIGHT);check(s.codeDraft.insertion.value(1).equals("1"),"controller parameter change");
        s.act(Action.CONFIRM);s.insertText("5");check(s.codeDraft.insertion.input.equals("5"),"field input replaces selection");
        byte[] encoded=s.codeDraft.encode();WorkshopSession restored=new WorkshopSession(blank,port);restored.restoreCode(encoded);
        check(restored.codeDraft.insertion.screen==LuaInsert.Screen.TEXT&&restored.codeDraft.insertion.input.equals("5"),"unfinished parameter recovery");
        restored.act(Action.CANCEL);check(restored.codeDraft.insertion.value(1).equals("1"),"field cancel preserves previous value");
        restored.act(Action.DOWN);restored.act(Action.CONFIRM);check(restored.codeDraft.text().contains("  score=1\n"),"ordinary Lua assignment inside callback");
        port.fail=true;restored.act(Action.TEST);check(restored.mode==Mode.ERROR&&restored.codeDraft!=null&&port.saves==0,"failed save retains completed insertion");
        port.fail=false;restored.act(Action.CANCEL);restored.act(Action.TEST);check(port.launches==1,"save and launch after retry");
        P8Document a=P8Document.parse(blank.bytes()),b=P8Document.parse(port.saved);
        for(int n=0;n<a.sections().size();n++)if(!a.sections().get(n).name.equals("lua"))same(a.body(n),b.body(n),"unrelated data");
        restored.act(Action.UNDO);same(blank.bytes(),restored.cart().bytes(),"project undo after generated code");
        // v1 recovery records from 0.0.31 did not contain an insertion flag.
        byte[] v2=new LuaDraft(blank,0).encode(),v1=Arrays.copyOf(v2,v2.length-1);v1[3]=1;
        check(LuaDraft.restore(v1).text().equals(new LuaDraft(blank,0).text()),"0.0.31 draft migration");
        for(int length:new int[]{0,8,encoded.length-1}){final byte[] broken=Arrays.copyOf(encoded,length);refused(()->LuaDraft.restore(broken),"truncated proposal accepted");}
        System.out.println("Lua insertion: "+checks+" checks passed");
    }
}
