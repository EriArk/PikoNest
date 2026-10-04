import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BackgroundLayerTest {
    static int checks;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe background accepted");}catch(IllegalArgumentException expected){checks++;}}
    static LuaInsert form(){LuaInsert f=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("background"))f.choose(n);f.screen=LuaInsert.Screen.FIELDS;return f;}
    static LuaDraft draft(String source){LuaDraft d=new LuaDraft(LuaNavigationTest.cart(source),0);d.beginInsert();d.insertion=form();return d;}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String src="function _draw()"+nl+" cls(1)"+nl+" print('hud',1,1,7) -- keep"+nl+"end"+nl;
            LuaDraft d=draft(src);d.point(2,0);d.beginBackgroundPick();d.backgroundPicker.point(24,8);d.acceptBackgroundPick();d.backgroundPicker.point(47,23);
            LuaDraft restored=LuaDraft.restore(d.encode());check(restored.backgroundPicker.phase==1&&restored.backgroundPicker.source().width==24,"picker recovery");
            restored.acceptBackgroundPick();check(restored.insertion.value(0).equals("24")&&restored.insertion.value(2).equals("24"),"region accepted");
            restored.insertion.beginPreview();d=LuaDraft.restore(restored.encode());check(!d.dirty(),"preview read-only");d.applyInsert();String inserted=d.text();
            check(inserted.endsWith(" print('hud',1,1,7) -- keep"+nl+"end"+nl),"following source preserved");check(inserted.contains("sspr(24,8,24,16"),"selected rectangle emitted");
            d.history(false);check(d.text().equals(src),"one undo for block");d.history(true);check(d.text().equals(inserted),"redo");
            d.point(6,0);d.beginParameters();check(d.insertion.backgroundRecipe(),"reopen inside block");d.insertion.set(5,"-3");d.insertion.set(6,"0.25");d.insertion.beginPreview();
            restored=LuaDraft.restore(d.encode());restored.applyInsert();d.applyInsert();check(d.text().equals(restored.text()),"parameter preview recovery");
            check(d.text().equals(inserted.replace("*4)","*-3)").replace("_bg_cx*0.5","_bg_cx*0.25")),"only chosen argument spans change");
            d.point(3,0);d.beginParameters();d.applyInsert();d.history(false);check(d.text().equals(inserted),"no-op adds no undo");
            check(new String(d.edit().candidate(LuaNavigationTest.cart(src)).bytes(),StandardCharsets.UTF_8).endsWith("__future__\nkeep\n"),"unknown section preserved");
        }
        for(String api:new String[]{"camera","sspr","time","flr"}){LuaDraft d=draft("local "+api+"=fn\n");refused(d::applyInsert);check(!d.dirty(),"shadow refusal");}
        for(String src:new String[]{"#include other.p8\n","_ENV.x=1\n"}){LuaDraft d=draft(src);refused(d::applyInsert);}
        for(int n:new int[]{0,1,2,3,4,5}){final int field=n;refused(()->form().set(field,"1);evil()"));}
        for(String v:new String[]{"-1","1.25","0.1"})refused(()->form().set(6,v));
        LuaInsert bad=form();bad.set(0,"127");refused(()->BackgroundLayer.code(bad));
        String code=form().code();check(BackgroundLayer.find(code.replace("127,_bg_w","126,_bg_w"),0)==null,"manual code detaches binding");
        check(BackgroundLayer.find("--[[\n"+code+"]]\n",5)==null,"comment not a binding");
        String two=code+code;LuaCall second=BackgroundLayer.find(two,code.length()+5);check(second!=null&&second.start==code.length(),"independent second layer");second.form.set(7,"false");check(second.replacement(two,second.form).startsWith(code),"first layer byte stable");
        LuaInsert preset=form();ParameterPreset p=ParameterPreset.capture(preset,"Clouds","Test");for(int n=0;n<4;n++)check(p.value(n)==null,"resource not captured in settings");
        LuaInsert other=form();BackgroundLayer.setRegion(other,new SpriteRegion(32,16,24,8));p.apply(other);check(other.value(0).equals("32")&&other.value(2).equals("24"),"settings retain target resource");
        LuaNavigationTest.Port port=new LuaNavigationTest.Port();WorkshopSession s=new WorkshopSession(LuaNavigationTest.cart("\n"),port);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.CONTEXT);s.codeDraft.insertion.choose(form().selected);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.TEST);s.codeCommand(1);
        check(port.writes==0&&port.launches==0&&!s.codeDraft.dirty(),"picker traps Test/save");s.act(Action.CANCEL);check(s.codeDraft.backgroundPicker==null,"picker cancel");s.act(Action.DOWN);check(s.codeDraft.insertion.field==4,"controller skips raw rectangle coordinates");s.act(Action.DOWN);s.act(Action.LEFT);check(s.codeDraft.insertion.value(5).equals("3"),"speed steps");s.act(Action.CONTEXT);check(s.codeDraft.insertion.screen==LuaInsert.Screen.PREVIEW,"Lua review");s.act(Action.CANCEL);check(!s.codeDraft.dirty(),"review cancel");
        if(args.length>0)Files.write(Paths.get(args[0]),oracle().getBytes(StandardCharsets.UTF_8));
        System.out.println("BackgroundLayerTest: "+checks+" checks passed");
    }
    static String oracle(){
        StringBuilder lua=new StringBuilder("checks=0\nfor x=0,127 do sset(x,0,x%14+1) end\n");
        int[][] cases={{3,0,0,0,2},{3,1,4,64,2},{3,5,-3,-31,1},{16,1,32,32767,4},{16,1,-32,-32768,4},{128,32760,32,32767,3},{7,-32768,-32,-32768,1},{1,0,0,0,0}};
        for(int[] a:cases){
            LuaInsert f=form();BackgroundLayer.setRegion(f,new SpriteRegion(0,0,a[0],1));f.set(4,"24");f.set(5,""+a[2]);f.set(6,BackgroundLayer.FACTORS[a[4]]);
            double pos=a[1]*(double)a[2]-a[3]*Double.parseDouble(f.value(6));int shift=Math.floorMod((int)Math.floor(pos),a[0]);StringBuilder expected=new StringBuilder();
            for(int x=0;x<128;x++)expected.append("0123456789abcdef".charAt(Math.floorMod(x-shift,a[0])%14+1));
            lua.append("camera() cls(0) camera(").append(a[3]).append(",17)\n").append(f.code().replace("time()","("+a[1]+")"));
            lua.append("local cx,cy=camera()\nassert(cx==").append(a[3]).append(" and cy==17,\"camera restore\")\nlocal expected=\"").append(expected).append("\"\nfor x=0,127 do assert(pget(x,24)==tonum(\"0x\"..sub(expected,x+1,x+1)),\"stripe \"..checks..\" x \"..x) end\nchecks+=1\n");
        }
        LuaInsert hidden=form();hidden.set(7,"false");lua.append("camera() cls(2)\n").append(hidden.code()).append("assert(pget(0,24)==2,\"hidden\") checks+=1\n");
        LuaInsert real=form();real.set(5,"0");real.set(6,"0");BackgroundLayer.setRegion(real,new SpriteRegion(0,0,16,1));
        lua.append("camera() cls(0) palt(1,true) pal(2,8) clip(10,24,10,1)\n").append(real.code()).append("assert(pget(0,24)==0 and pget(16,24)==0 and pget(17,24)==8 and pget(20,24)==0,\"draw state\")\nrectfill(0,0,127,127,11)\nassert(pget(10,24)==11 and pget(20,24)==0,\"clip unchanged\")\nchecks+=1\nclip() pal() palt()\nfunction _draw()\n cls(1) print(\"background: \"..checks..\"/10\",8,48,11)\n print(\"repeat / camera / state\",8,64,7)\nend\n");
        return "pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+lua;
    }
}
