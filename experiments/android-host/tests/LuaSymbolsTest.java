import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LuaSymbolsTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static List<String> names(List<LuaSymbols.Entry> entries){List<String> result=new ArrayList<>();for(LuaSymbols.Entry e:entries)result.add(e.name);return result;}
    static WorkshopCartridge cart(String code){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+code+"__future__\nkeep me\n").getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] saved;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("full");saves++;saved=bytes;}
        public void launch(byte[] bytes){check(Arrays.equals(bytes,saved),"test uses saved bytes");launches++;}
    }
    public static void main(String[] args){
        String source="score=0\n-- ghost=1\ns=\"fake=2\"\nlong=[=[\nhidden=3\n]=]\n"+
            "function _init()\n score=0\nend\nfunction calc(arg) local temp=arg return temp end\n"+
            "local private=0\nfor index=1,2 do private+=index end\n"+
            "data={field=2,nested={secret=1}}\ndata.member=1\nfunction data:method(p) self.x=p end\n"+
            "a,b=1,2\nrestart=function() score=0 end\nlocal function helper() end\n";
        LuaSymbols s=new LuaSymbols(source);
        check(names(s.project(false)).equals(Arrays.asList("a","b","data","long","s","score")),"only simple shared variables: "+names(s.project(false)));
        check(names(s.project(true)).equals(Arrays.asList("_init","restart")),"only simple zero-argument functions");
        check(s.project(false).get(5).line==1,"first declaration line retained across writes");
        for(String eol:new String[]{"\n","\r\n","\r"})check(new LuaSymbols("x=1"+eol+"y=2").project(false).get(1).line==2,"line numbers "+eol.length());
        for(String code:new String[]{"--[[ fake=1\n", "s='fake=1\\\' x=2'\n", "s=[==[fake=1]==]\n"})check(!names(new LuaSymbols(code).project(false)).contains("fake"),"mask strings and comments");
        check(names(new LuaSymbols("function f(score) score=2 end\nscore=4\n").project(false)).isEmpty(),"parameter collision hidden conservatively across draft");
        check(names(new LuaSymbols("local a,b\na,b=1,2\n").project(false)).isEmpty(),"local declaration list without initializer");
        check(names(new LuaSymbols("for k,v in pairs(t) do k,v=1,2 end\n").project(false)).isEmpty(),"generic loop names");
        check(!names(new LuaSymbols("self=1 function t:m() end").project(false)).contains("self"),"implicit method argument excluded");
        check(names(new LuaSymbols("a==b a!=b a<=b a>=b a+=1 t.x=0 t:x()\n").project(false)).isEmpty(),"comparisons, augmented uses and member assignment are not declarations");
        check(names(new LuaSymbols("éabc=1\nx=0xff\ny=0b11\n").project(false)).equals(Arrays.asList("x","y")),"unsupported identifier suffix and numeric tokens");
        check(new LuaSymbols("function f(a) end\nf=function() end\n").project(true).isEmpty(),"ambiguous function not offered as zero-argument");
        check(new LuaSymbols("f=1\nfunction f() end\n").project(true).isEmpty(),"reassigned function conservative");
        check(new LuaSymbols("function f() end\nf=1\n").project(true).isEmpty(),"function changed to value");
        check(new LuaSymbols("a,b=function() end,1\n").project(true).isEmpty(),"multiple assignment never misattributes a function to last name");
        check(s.api().size()==8,"bounded documented API catalogue");
        check(!names(new LuaSymbols("time=function() return 1 end\nlocal rnd=1\n").api()).contains("time"),"global API override");
        check(!names(new LuaSymbols("function f(rnd) end\n").api()).contains("rnd"),"parameter API collision");
        check(names(new LuaSymbols("-- time=1\ns='rnd=1'\n").api()).contains("time"),"comment not API override");
        StringBuilder huge=new StringBuilder();for(int n=0;n<100001;n++)huge.append("+ ");
        LuaSymbols bounded=new LuaSymbols(huge.toString());check(!bounded.complete&&bounded.api().isEmpty()&&bounded.project(false).isEmpty(),"index bound does not offer partial unsafe context");
        LuaSymbols unfinished=new LuaSymbols("score=0 function broken(arg");
        check(!unfinished.complete&&unfinished.project(false).isEmpty()&&unfinished.api().isEmpty(),"unfinished declaration gives empty conservative context");

        Port p=new Port();WorkshopSession session=new WorkshopSession(cart(source),p);
        session.switchTool(1);session.act(Action.CONFIRM);session.act(Action.CONTEXT);
        for(int n=0;n<11;n++)session.act(Action.DOWN);session.act(Action.CONFIRM);
        LuaDraft d=session.codeDraft;String before=d.text();session.act(Action.CONTEXT);
        check(d.insertion.screen==LuaInsert.Screen.SYMBOLS,"X opens parameter choices");
        session.act(Action.TEST);session.codeText("lost");session.codeCommand(1);
        check(before.equals(d.text())&&p.saves==0&&p.launches==0,"modal browser blocks unrelated save/input/start");
        session.act(Action.RIGHT);check(d.insertion.symbolIndex==5,"page jump bounded");
        session.act(Action.CONFIRM);check(d.insertion.value(0).equals("score")&&d.text().equals(before),"choosing name edits proposal only");
        session.act(Action.CONTEXT);session.act(Action.MENU);session.act(Action.DOWN);
        check(d.insertion.choices().get(d.insertion.symbolIndex).value.equals("rnd(1)"),"API tab and selection");
        byte[] snapshot=d.encode();LuaDraft restored=LuaDraft.restore(snapshot);
        check(restored.insertion.screen==LuaInsert.Screen.SYMBOLS&&restored.insertion.symbolGroup==1&&restored.insertion.symbolIndex==1,"browser selection recovery");
        check(restored.insertion.value(0).equals("score"),"browser has not committed highlighted value");
        session.restoreCode(snapshot);session.act(Action.CANCEL);check(session.codeDraft.insertion.value(0).equals("score"),"cancel preserves field");
        session.act(Action.CONTEXT);session.act(Action.MENU);session.act(Action.CONFIRM);
        check(session.codeDraft.insertion.value(0).equals("time()"),"API returns ordinary editable expression");
        for(int n=0;n<4;n++)session.act(Action.DOWN);session.act(Action.CONFIRM);
        check(session.codeDraft.text().startsWith("print(time(),16,60,7)\n"),"explicit apply adds ordinary Lua");
        session.act(Action.UNDO);check(session.codeDraft.text().equals(before),"one-step insertion undo");session.act(Action.REDO);
        p.fail=true;session.act(Action.TEST);check(session.codeDraft!=null&&p.saves==0,"failed save retains source");
        p.fail=false;session.act(Action.CANCEL);session.act(Action.TEST);check(p.launches==1&&new String(p.saved,StandardCharsets.UTF_8).endsWith("__future__\nkeep me\n"),"saved unrelated section preserved");
        LuaDraft legacy=new LuaDraft(cart("score=0\n"),0);legacy.beginInsert();legacy.insertion.choose(11);legacy.insertion.screen=LuaInsert.Screen.FIELDS;
        byte[] v3=legacy.encode(),v2=Arrays.copyOf(v3,v3.length-8);v2[3]=2;
        check(LuaDraft.restore(v2).insertion.value(0).equals("score"),"0.0.32 unfinished proposal migration");
        legacy.insertion.beginSymbols(legacy.text());legacy.insertion.symbolGroup=1;
        LuaDraft empty=new LuaDraft(cart(""),0);empty.beginInsert();empty.insertion.choose(16);empty.insertion.screen=LuaInsert.Screen.FIELDS;empty.insertion.beginSymbols(empty.text());empty.insertion.acceptSymbol();
        check(empty.insertion.screen==LuaInsert.Screen.SYMBOLS,"empty list confirm is harmless");
        check(!empty.insertion.canBrowseApi(),"function-name field never gets expression calls");
        LuaDraft fresh=new LuaDraft(cart(""),0);fresh.replace("new_score=3\n");fresh.beginInsert();fresh.insertion.choose(11);
        fresh.insertion.screen=LuaInsert.Screen.FIELDS;fresh.insertion.beginSymbols(fresh.text());
        check(names(fresh.insertion.choices()).equals(Arrays.asList("new_score")),"index includes unsaved declarations");
        for(int item:new int[]{3,5,12}){LuaInsert i=new LuaInsert();i.choose(item);check(!i.canBrowse(),"new declaration/string field not offered existing names "+item);}
        System.out.println("Lua symbols: "+checks+" checks passed");
    }
}
