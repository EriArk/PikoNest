import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SpriteAnimationTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable run){try{run.run();throw new AssertionError("unsafe animation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static SpriteAnimation pair(){
        SpriteAnimation a=new SpriteAnimation(new SpriteRegion(0,0,8,8));a.duplicate();a.durationStep(5);a.beginPick(false);a.picker.point(16,0);a.acceptPick();a.picker.point(39,15);a.acceptPick();a.x=20;a.y=30;a.stop();return a;
    }
    public static void main(String[] args)throws Exception{
        SpriteAnimation a=pair();check(a.count()==2&&a.duration()==750&&a.frame(1).region.width==24,"unequal durations and large region");
        check(a.previewWidth()==24&&a.previewHeight()==16,"shared preview bounds prevent frame-by-frame zoom");
        int[] times={0,249,250,749,750,1000,30000000},expected={0,0,1,1,0,1,0};
        for(int n=0;n<times.length;n++)check(a.frameAt(times[n])==expected[n],"loop boundary "+times[n]);
        a.loop=false;check(a.frameAt(750)==1&&a.frameAt(30000000)==1,"once holds last");a.toggle();a.advance(1000);check(!a.playing&&a.visibleFrame()==1,"once stops");a.toggle();check(a.elapsed()==0&&a.playing,"once restarts");a.advance(250);a.toggle();a.advance(500);check(a.elapsed()==250,"pause holds position");a.stop();check(a.visibleFrame()==0&&!a.playing,"stop resets preview");
        a.select(1);a.reorder(-1);check(a.selected==0&&a.frame(0).region.width==24&&a.frame(0).millis==500,"reorder retains frame timing");a.remove();check(a.count()==1&&a.frame(0).millis==250,"delete chosen frame");refused(a::remove);
        a.durationStep(-100);check(a.frame(0).millis==50,"minimum duration");a.durationStep(200);check(a.frame(0).millis==5000,"maximum duration");
        for(int n=1;n<SpriteAnimation.MAX_FRAMES;n++)a.duplicate();refused(a::duplicate);check(a.duration()==160000&&a.code().contains("0xa0.0000"),"long sequence avoids millisecond literal overflow");
        WorkshopCartridge original=SpritePlacementTest.cart().withPixel(new SpriteRegion(0,64,128,64),3,4,14);
        LuaDraft d=new LuaDraft(original,2);String before=d.text();d.beginAnimation(new SpriteRegion(0,64,128,64));
        check(!d.dirty()&&d.animation.frame(0).region.sharesMap(),"proposal reads full shared sheet");
        d.animation=pair();d.animation.select(1);d.animation.toggle();d.animation.advance(50);
        LuaDraft recovered=LuaDraft.restore(d.encode());check(!recovered.animation.playing&&recovered.animation.elapsed()==300&&recovered.animation.visibleFrame()==1,"restore paused at same time");
        d.animation.beginPick(false);d.animation.picker.phase=1;d.animation.picker.move(-1,1);
        recovered=LuaDraft.restore(d.encode());check(recovered.animation.picker.phase==1&&recovered.animation.code().equals(d.animation.code()),"unconfirmed region restored without mutation");
        d.animation.cancelPick();d.animation.cancelPick();d.animation.beginPick(true);d.animation.picker.move(1,-1);d.animation.acceptPick();check(d.animation.x==28&&d.animation.y==22,"position selected separately");
        d.animation.review=true;d.animation.previewLine=8;recovered=LuaDraft.restore(d.encode());check(recovered.animation.review&&recovered.animation.previewLine==8,"full Lua review restored");
        byte[] corrupt=d.encode();corrupt[3]=99;refused(()->LuaDraft.restore(corrupt));
        d.applyAnimation();recovered.applyAnimation();check(d.text().equals(recovered.text()),"same recovered insertion");
        check(d.text().startsWith(before.substring(0,before.indexOf(" -- chosen")))&&d.text().endsWith(" -- chosen line\r\nend\r\n"),"surrounding code retained");
        check(!d.text().replace("\r\n","").contains("\n"),"CRLF retained");
        WorkshopCartridge result=d.edit().candidate(original);check(result.sheetPixel(3,68)==14,"shared graphics retained");
        String all=new String(result.bytes(),StandardCharsets.UTF_8),old=new String(original.bytes(),StandardCharsets.UTF_8);check(all.substring(all.indexOf("__future__")).equals(old.substring(old.indexOf("__future__"))),"unknown and graphics sections exact");
        d.history(false);check(d.text().equals(before)&&d.line()==2,"one undo");d.history(true);check(d.text().contains("local frames="),"redo block");
        for(String bad:new String[]{"local time=1\n","function sspr() end\n","#include x.lua\n","_ENV={}\n"})refused(()->SpriteAnimation.validateSource(bad));
        SpriteAnimation.validateSource("frames=3\nf=1\ni=2\nelapsed=4\n-- time=0\n");checks++;
        LuaDraft protectedDraft=LuaInsertTest.draft("--[[\ninside\n]]\n");protectedDraft.point(1,0);refused(()->protectedDraft.beginAnimation(new SpriteRegion(0,0,8,8)));
        LuaInsertTest.Port port=new LuaInsertTest.Port();WorkshopSession s=new WorkshopSession(original,port);s.switchTool(1);s.codeLine=2;s.act(Action.CONFIRM);s.act(Action.CONTEXT);
        for(int n=0;n<LuaInsert.ITEMS.length-1;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.panel==LuaDraft.Panel.ANIMATION,"controller entry");
        s.act(Action.TEST);s.codeCommand(1);check(port.saves==0&&port.launches==0,"modal traps save/Test");
        s.act(Action.CONTEXT);check(s.codeDraft.animation.playing,"controller play");s.act(Action.UNDO);check(!s.codeDraft.animation.playing,"controller stop");
        for(int n=0;n<3;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.animation.count()==2,"controller duplicate");
        s.act(Action.DOWN);s.act(Action.LEFT);check(s.codeDraft.animation.selected==0,"controller reorder");
        for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.TEST);check(port.launches==0&&s.codeDraft.animation.review,"review traps Start");
        s.act(Action.CANCEL);check(!s.codeDraft.animation.review,"review back retains proposal");s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.codeDraft.animation==null&&s.codeDraft.dirty()&&port.saves==0,"insert is draft only");
        port.fail=true;s.act(Action.TEST);check(s.codeDraft!=null&&port.saves==0,"save failure keeps draft");port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(port.saves==1&&port.launches==1,"retry saves and launches");s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),original.bytes()),"project undo exact");
        if(args.length>0){Path path=Paths.get(args[0]);Files.createDirectories(path.getParent());Files.write(path,oracle().getBytes(StandardCharsets.UTF_8));}
        System.out.println("SpriteAnimationTest: "+checks+" checks passed");
    }
    static String oracle(){
        SpriteAnimation a=pair();StringBuilder b=new StringBuilder("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\nchecks=0\nfor y=0,127 do for x=0,127 do sset(x,y,0) end end\nfor y=0,7 do for x=0,7 do sset(x,y,8) end end\nfor y=0,15 do for x=16,39 do sset(x,y,11) end end\n");
        for(int mode=0;mode<2;mode++){
            a.loop=mode==0;
            b.append("function check_").append(mode).append("(t,c,w,h)\n cls(1)\n local time=function() return t end\n").append(a.code())
             .append(" assert(pget(20,30)==c and pget(20+w-1,30+h-1)==c,\"frame \"..checks)\n assert(pget(20+w,30)==1 and pget(20,30+h)==1,\"bounds \"..checks)\n checks+=1\nend\n");
        }
        b.append("check_0(0,8,8,8)\ncheck_0(0x0.3fff,8,8,8)\ncheck_0(.25,11,24,16)\ncheck_0(0x0.bfff,11,24,16)\ncheck_0(.75,8,8,8)\ncheck_0(1,11,24,16)\ncheck_0(30000,8,8,8)\ncheck_0(30000.25,11,24,16)\ncheck_1(0,8,8,8)\ncheck_1(.25,11,24,16)\ncheck_1(.75,11,24,16)\ncheck_1(30000,11,24,16)\n");
        // Non-binary durations: exactly one fixed-point unit before/at each generated boundary.
        a=pair();a.select(0);a.durationStep(-4);a.select(1);a.durationStep(-8);
        b.append("function small(t,c)\n cls(1)\n local time=function() return t end\n").append(a.code()).append(" assert(pget(20,30)==c,\"fraction \"..checks) checks+=1\nend\nsmall(0x0.0ccb,8)\nsmall(0x0.0ccc,11)\nsmall(0x0.2665,11)\nsmall(0x0.2666,8)\n");
        b.append("function _draw()\n cls(1)\n print(\"animation: \"..checks..\"/16\",8,40,11)\n print(\"timing / loop / regions\",8,56,7)\nend\n");return b.toString();
    }
}
