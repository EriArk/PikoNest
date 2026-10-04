import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class AnimationEditTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void refused(Runnable run){try{run.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static String block(SpriteAnimation a,String indent,String nl){StringBuilder b=new StringBuilder();for(String row:a.code().split("\n"))b.append(indent).append(row).append(nl);return b.toString();}
    static WorkshopCartridge cart(String lua){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+lua+"__future__\nopaque\n").getBytes(StandardCharsets.UTF_8));}
    public static void main(String[] args)throws Exception{
        SpriteAnimation a=SpriteAnimationTest.pair();
        for(String nl:new String[]{"\n","\r\n","\r"})for(String indent:new String[]{"","  ","\t"}){
            String generated=block(a,indent,nl),source="-- keep"+nl+generated+"print(9)"+nl;
            int start=source.indexOf(indent+"do"),end=start+generated.length();
            for(int at:new int[]{start,start+generated.indexOf("local frames"),end-nl.length()-1}){
                AnimationEdit e=AnimationEdit.find(source,at);check(e!=null&&e.start==start&&e.end==end,"whole target from any line");
                check(e.initial.code().equals(a.code()),"settings roundtrip");check(e.replacement(source,e.initial.copy()).equals(source),"no-op byte exact");
                SpriteAnimation changed=e.initial.copy();changed.select(1);changed.durationStep(1);changed.reorder(-1);changed.loop=false;
                String after=e.replacement(source,changed);check(after.equals(source.substring(0,start)+block(changed,indent,nl)+source.substring(end)),"only target replaced, indentation/newline retained");
            }
            check(AnimationEdit.find(source,end)==null,"following line outside target");
        }
        String noEof=a.code().trim();AnimationEdit eof=AnimationEdit.find(noEof,0);SpriteAnimation alter=eof.initial.copy();alter.durationStep(1);
        check(!eof.replacement(noEof,alter).endsWith("\n"),"no trailing newline invented");
        String first=a.code(),second=a.copy().code(),two=first+second;
        AnimationEdit e=AnimationEdit.find(two,first.length());SpriteAnimation edited=e.initial.copy();edited.durationStep(2);
        check(e.start==first.length()&&e.replacement(two,edited).startsWith(first),"identical neighbour not replaced");
        for(String wrapped:new String[]{"--[[\n"+first+"]]\n","s=[=[\n"+first+"]=]\n"})check(AnimationEdit.find(wrapped,wrapped.indexOf("local frames"))==null,"string/comment is not animation");
        for(String bad:new String[]{first.replace("break","print(1)\n   break"),first.replace("time()","time()+1"),first.replace("sspr(","spr("),first.replace("0x0.c000","0x0.b000"),first.replace("{0,0,8,8,","{0,0,0,8,"),first.replace("local f=frames[i]","local f=frames[1]"),first.replace(" local frames"," -- extra\n local frames"),first.replace("    20,30)","    x,30)")}){
            refused(()->AnimationEdit.find(bad,0));
        }
        check(AnimationEdit.find(first.replace("-- animation:","-- other:"),0)==null,"unknown marker does not claim a block");
        SpriteAnimation fractional=a.copy();fractional.select(0);fractional.durationStep(-4);fractional.select(1);fractional.durationStep(-8);
        check(AnimationEdit.find(fractional.code(),0).initial.duration()==150,"fixed-point boundary returns original milliseconds");
        SpriteAnimation longOne=new SpriteAnimation(new SpriteRegion(0,0,128,128));longOne.durationStep(100);for(int i=1;i<32;i++)longOne.duplicate();
        check(AnimationEdit.find(longOne.code(),0).initial.duration()==160000,"32 long frames");
        e=AnimationEdit.find(first,0);final AnimationEdit stale=e;refused(()->stale.replacement(first.replace("sspr","spr"),alter));
        String lua="function _draw()\n cls(1)\n"+first+" print(42)\nend\n";WorkshopCartridge original=cart(lua);
        LuaDraft d=new LuaDraft(original,5);int oldCursor=d.cursor();d.beginParameters();check(d.animationEdit!=null&&!d.dirty(),"L opens containing animation");
        d.animation.durationStep(2);d.animation.review=true;d.animationBefore=true;d.animation.previewLine=2;
        LuaDraft recovered=LuaDraft.restore(d.encode());check(recovered.animationBefore&&recovered.animation.duration()==850&&recovered.animationEdit.initial.duration()==750,"edited and original models survive restart");
        d.applyAnimation();recovered.applyAnimation();check(d.text().equals(recovered.text()),"same replacement after recovery");
        check(!d.text().substring(d.text().indexOf("do")+2).contains("\ndo\n"),"replacement does not append duplicate");
        d.history(false);check(d.text().equals(lua)&&d.cursor()==oldCursor,"undo restores text/cursor");d.history(true);check(!d.text().equals(lua),"redo replacement");
        String changed=d.text();d.beginParameters();d.animation.durationStep(1);d.cancelAnimation();check(d.text().equals(changed),"cancel preserves source");
        LuaDraft noop=new LuaDraft(original,2);noop.beginParameters();noop.animation.review=true;noop.applyAnimation();check(!noop.dirty()&&!noop.canUndo(),"no-op adds no undo entry");
        for(String extra:new String[]{"local time=1\n","#include x.lua\n"}){LuaDraft bad=new LuaDraft(cart(extra+lua),3);refused(bad::beginParameters);check(!bad.dirty(),"conflict leaves source");}
        LuaDraft select=new LuaDraft(original,2);select.selectAll();refused(select::beginParameters);
        LuaDraft corrupt=new LuaDraft(original,2);corrupt.beginParameters();byte[] journal=corrupt.encode();journal[journal.length-2]^=1;refused(()->LuaDraft.restore(journal));
        LuaInsertTest.Port port=new LuaInsertTest.Port();WorkshopSession s=new WorkshopSession(original,port);s.switchTool(1);s.codeLine=4;s.act(Action.CONFIRM);s.act(Action.PREVIOUS);
        check(s.codeDraft.animationEdit!=null,"controller opens old block");s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.RIGHT);for(int n=0;n<6;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.CONTEXT);check(s.codeDraft.animationBefore,"compare old");s.act(Action.TEST);s.codeCommand(1);check(port.saves==0&&port.launches==0,"review traps Test");
        s.act(Action.CONFIRM);check(!s.codeDraft.animationBefore&&!s.codeDraft.dirty(),"old view requires viewing new before replacement");s.act(Action.CONFIRM);check(s.codeDraft.animationEdit==null&&s.codeDraft.dirty(),"replace in draft");
        port.fail=true;s.act(Action.TEST);check(s.codeDraft!=null&&port.saves==0,"save error keeps replacement");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry saved Test");s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),original.bytes()),"project undo exact");
        String result=new String(d.edit().candidate(original).bytes(),StandardCharsets.UTF_8);check(result.endsWith("__future__\nopaque\n"),"unknown section preserved");
        if(args.length>0)Files.write(Paths.get(args[0]),d.edit().candidate(original).bytes());
        System.out.println("AnimationEditTest: "+checks+" checks passed");
    }
}
