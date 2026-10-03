import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LuaNavigationTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static WorkshopCartridge cart(String lua){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n"+lua+"__future__\nkeep\n").getBytes(StandardCharsets.UTF_8));}
    static void line(LuaDraft d,int number){d.beginNavigation();d.navigation.tab=1;d.navigation.target=number;d.jump();}
    static class Port implements WorkshopSession.Port {
        int writes,launches;
        public void save(byte[] b){writes++;}public void launch(byte[] b){launches++;}
    }
    public static void main(String[] args){
        for(String eol:new String[]{"\n","\r\n","\r"}){
            String source=String.join(eol,"-- function fake()", "s=[=[function hidden()]=]", "local function work(x,y)",
                " local function work(z) end", "end", "function actor:draw(x) end", "actor.update = function(dt) end",
                "local fn=function(x) end", "s='function nope()'", "--[=[function bad()]=]", "function unfinished(")+eol;
            LuaNavigation n=new LuaNavigation(source,5);
            check(n.entries.size()==6,"comments/strings hidden, duplicates and unfinished signature retained");
            String[] names={"work","work","actor:draw","actor.update","fn","unfinished"};int[] lines={2,3,5,6,7,10};
            for(int i=0;i<names.length;i++)check(n.entries.get(i).name.equals(names[i])&&n.entries.get(i).line==lines[i],"source-order outline "+names[i]);
            LuaDraft d=new LuaDraft(cart(source),0);d.point(4,2);d.select();d.point(5,4);int cursor=d.cursor(),anchor=d.anchor();
            d.beginNavigation();d.navigation.index=0;d.cancelNavigation();check(d.cursor()==cursor&&d.anchor()==anchor,"cancel retains selection");
            d.beginNavigation();d.navigation.index=0;d.jump();check(d.line()==2&&d.anchor()==-1&&!d.dirty()&&!d.canUndo(),"jump no edit/history");
            d=LuaDraft.restore(d.encode());d.goBack();check(d.cursor()==cursor&&d.anchor()==anchor,"recovered return restores selection");
            check(Arrays.equals(d.edit().candidate(cart(source)).bytes(),cart(source).bytes()),"navigation preserves cartridge bytes");
            d.beginNavigation();d.navigation.tab=1;d.navigation.target=8;d.navigation.digit=1;
            d=LuaDraft.restore(d.encode());check(d.navigation.target==8&&d.navigation.digit==1&&d.cursor()==cursor,"pending line selection recovery");
            d.navigation.move(0,1);check(d.navigation.target==d.lineCount(),"line clamp high");
            d.navigation.move(0,-1);d.navigation.move(0,-1);check(d.navigation.target==1,"line clamp low");
            d.navigation.move(1,0);check(d.navigation.digit==0,"digit direction");
        }
        LuaDraft d=new LuaDraft(cart("one\ntwo\nthree\n"),2);d.point(2,2);line(d,1);d.replace("new\n");d.goBack();
        check(d.line()==3&&d.column()==2,"return tracks insertion before origin");
        line(d,1);d.erase(true);d.history(false);d.goBack();check(d.line()==3&&d.column()==2,"return tracks edit undo");
        d=new LuaDraft(cart("one\n😀xx\nthree\n"),1);d.point(1,1);line(d,1);d.selectAll();d.replace("🙂\n");d.goBack();
        check(d.cursor()==0,"deleted target lands at replacement boundary");
        d=new LuaDraft(cart("one\ntwo\n"),0);line(d,1);check(!d.canGoBack(),"no-op jump no return entry");
        for(int i=0;i<40;i++)line(d,i%2==0?2:1);int returns=0;while(d.canGoBack()){d.goBack();returns++;}check(returns==32,"bounded return stack");
        LuaNavigation empty=new LuaNavigation("-- function no()\n",0);check(empty.tab==1&&empty.entries.isEmpty(),"empty outline opens line picker");
        LuaNavigation barrier=new LuaNavigation("x='value'\nfunction yes() end\na==function()end\nf(function()end)",0);
        check(barrier.entries.size()==1&&barrier.entries.get(0).name.equals("yes"),"anonymous functions not attributed to preceding value");
        StringBuilder huge=new StringBuilder();for(int i=0;i<100001;i++)huge.append("a ");
        check(new LuaNavigation(huge.toString(),0).truncated,"bounded scan declares incomplete outline");
        Port p=new Port();WorkshopSession s=new WorkshopSession(cart("function a()\nend\nfunction b(x)end\n"),p);s.switchTool(1);s.act(Action.CONFIRM);
        s.act(Action.NEXT);check(s.codeDraft.panel==LuaDraft.Panel.NAVIGATION,"R opens navigation");
        String before=s.codeDraft.text();s.act(Action.TEST);s.act(Action.UNDO);s.codeCommand(0);s.codeText("bad");
        check(p.writes==0&&p.launches==0&&s.codeDraft.text().equals(before),"modal save/launch/text/undo trapped");
        s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.line()==2,"controller function jump");
        s.act(Action.NEXT);s.act(Action.CONTEXT);check(s.codeDraft.line()==0,"controller return");
        s.codeCommand(20);s.act(Action.NEXT);s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.line()==1,"menu and line path");
        s.codeCommand(21);check(s.codeDraft.line()==0,"menu return");
        s.codeDraft.replace("-- draft\n");s.act(Action.NEXT);check(s.codeDraft.navigation.entries.get(0).line==1,"outline uses unsaved source");
        byte[] encoded=s.codeDraft.encode();check(encoded[3]==5,"navigation recovery version");
        encoded[encoded.length-1]=127;boolean refused=false;try{LuaDraft.restore(encoded);}catch(IllegalArgumentException ex){refused=true;}check(refused,"invalid return count rejected");
        System.out.println("Lua navigation: "+checks+" checks passed");
    }
}
