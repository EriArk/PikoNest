import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.io.*;

/** Complete bounded background workflow through game uses, without a Lua cursor. */
public final class BackgroundUsesTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    static WorkshopCartridge cart(String code){return GameUsesTest.cart(code);}
    static String text(WorkshopCartridge c){return GameUsesTest.text(c);}
    static void resources(WorkshopCartridge a,WorkshopCartridge b){
        check(Arrays.equals(Arrays.copyOfRange(a.bytes(),textEnd(a),a.bytes().length),Arrays.copyOfRange(b.bytes(),textEnd(b),b.bytes().length)),"gfx/map/audio/unknown sections unchanged");
    }
    static int textEnd(WorkshopCartridge c){return new String(c.bytes(),java.nio.charset.StandardCharsets.UTF_8).indexOf("__gfx__");}
    static WorkshopCartridge apply(GameUses g){g.review();return g.proposal().candidate(g.base);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;byte[] launched;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");writes++;}
        public void launch(byte[] b){launches++;launched=b;}
    }
    static void act(WorkshopSession s,Action... a){for(Action key:a)s.act(key);}
    static void menu(WorkshopSession s,int n){s.act(Action.MENU);for(int i=0;i<n;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);}
    public static void main(String[] args)throws Exception {
        for(String nl:new String[]{"\n","\r\n","\r"}){
            WorkshopCartridge blank=cart("-- blank without a hero"+nl+"score=0"+nl);GameUses g=new GameUses(blank,0);g.addBackground(new SpriteRegion(0,0,4,1));
            g.background.form.set(4,"0");g.background.form.set(5,"0");g.background.form.set(6,"0");WorkshopCartridge first=apply(g);
            check(!first.hasHero()&&text(first).startsWith(text(blank)),"blank callback added without a hero or rewriting prefix");resources(blank,first);
            GameUses reopened=new GameUses(first,0);check(reopened.entries.size()==1&&reopened.current().isBackground(),"saved strip appears in common list");reopened.edit();check(reopened.screen==GameUses.Screen.BACKGROUND,"reopen from list");
            check(Arrays.equals(apply(reopened).bytes(),first.bytes()),"no-op preserves exact bytes");
            check(new GameSketch(first,0).pixels[0]==1&&new GameSketch(first,0).pixels[3]==4,"strip preview reads original sheet");
            GameUses copy=new GameUses(first,0);copy.duplicate();copy.background.form.set(4,"20");WorkshopCartridge doubled=apply(GameUses.restore(copy.encode(),first));
            GameUses list=new GameUses(doubled,0);check(list.entries.size()==2&&list.entries.get(0).y()==0&&list.entries.get(1).y()==20,"copy adjacent, independent parameters");resources(first,doubled);
            list.index=1;list.reorderLayer(-1);GameUses recovered=GameUses.restore(list.encode(),doubled);check(recovered.resultIndex()==0,"selection follows layer move");WorkshopCartridge swapped=recovered.proposal().candidate(doubled);
            GameUses moved=new GameUses(swapped,0);check(moved.entries.get(0).y()==20&&moved.entries.get(1).y()==0,"swap in source draw order");moved.reorderLayer(1);check(Arrays.equals(moved.proposal().candidate(swapped).bytes(),doubled.bytes()),"reverse move restores exact bytes");
            list=new GameUses(doubled,0);list.index=1;list.delete();check(Arrays.equals(list.proposal().candidate(doubled).bytes(),first.bytes()),"delete copied block preserves resource and original source");
            String cameraSource="function _draw()"+nl+" cls(1)"+nl+" camera(1,9)"+nl+" spr(0,40,40) -- keep"+nl+" camera()"+nl+" print('hud',1,1,7)"+nl+"end"+nl;
            WorkshopCartridge world=cart(cameraSource);g=new GameUses(world,0);g.index=1;g.addBackground(new SpriteRegion(0,0,4,1));g.background.form.set(4,"0");g.background.form.set(5,"0");g.background.form.set(6,"1");WorkshopCartridge withLayer=apply(g);GameUses scene=new GameUses(withLayer,0);
            check(scene.entries.get(1).isBackground()&&scene.entries.get(2).kind().equals("spr"),"new layer inserted before selected use");
            check(CameraUse.offset(scene.entries.get(2).view)[0]==1&&CameraUse.offset(scene.entries.get(2).view)[1]==9,"background restores outer world camera for following use");
            check(text(withLayer).contains(" spr(0,40,40) -- keep"+nl+" camera()"+nl+" print('hud',1,1,7)"),"HUD order, camera boundary and comment preserved");
            check(new GameSketch(withLayer,0).pixels[0]==2,"scene sketch applies incoming camera to parallax");scene.edit();scene.camera.preview(false);check(!scene.camera.dynamicPreview,"camera preview accepts background blocks");
            scene=new GameUses(withLayer,0);scene.index=1;final GameUses bg=scene;refused(bg::animateSelected);scene.duplicate();WorkshopCartridge pair=apply(scene);GameUses paired=new GameUses(pair,0);check(paired.entries.get(2).isBackground()&&CameraUse.offset(paired.entries.get(2).view)[0]==1,"duplicate retains its camera domain");
            CartridgeExport out=new CartridgeExport("blank-0001","Layer scene",pair.bytes());ByteArrayOutputStream bytes=new ByteArrayOutputStream();out.writeTo(bytes);out.verify(new ByteArrayInputStream(bytes.toByteArray()));
            check(Arrays.equals(bytes.toByteArray(),pair.bytes())&&new GameUses(new WorkshopCartridge(bytes.toByteArray()),0).entries.size()==5,"export/reopen ordinary .p8 without editor metadata");
        }
        String a=BackgroundLayersTest.layer(0,"\n"),b=BackgroundLayersTest.layer(20,"\n");
        for(String gap:new String[]{"-- keep this boundary\n","camera()\n","spr(1,10,10)\n","print('label',1,1,7)\n"}){
            WorkshopCartridge base=cart("function _draw()\n cls(1)\n"+a+gap+b+"end\n");GameUses g=new GameUses(base,0);check(g.entries.size()>=2,"separated layers remain discoverable");refused(()->g.reorderLayer(1));check(Arrays.equals(g.base.bytes(),base.bytes()),"blocked reorder does not mutate");
        }
        for(String src:new String[]{"local sspr=fn\n","#include part.p8\n","_ENV.x=1\n","local cls=fn\n"}){GameUses g=new GameUses(cart(src),0);refused(()->g.addBackground(new SpriteRegion(0,0,8,8)));}
        GameUses unknown=new GameUses(cart("function _draw()\n"+a.replace("127,_bg_w","126,_bg_w")+"end\n"),0);check(!unknown.blocked.isEmpty(),"manually changed formula is preserved, not rewritten");refused(()->unknown.addBackground(new SpriteRegion(0,0,8,8)));
        WorkshopCartridge layered=cart("function _draw()\n cls(1)\n"+a+b+"end\n");GameUses g=new GameUses(layered,0);g.edit();g.background.field=0;g.background.chooseRegion();g.background.picker.next();
        g=GameUses.restore(g.encode(),layered);check(g.background.picker.phase==1,"resource picker restores");g.background.cancelPick();g.background.cancelPick();g.background.field=2;g.beginField();g.background.change(-3);
        g=GameUses.restore(g.encode(),layered);g.cancelField();check(g.background.form.value(5).equals("4"),"field revert across process loss");
        g.background.toggle();g.background.advance(750);GameUses restored=GameUses.restore(g.encode(),layered);check(restored.background.previewMillis==750&&!restored.background.playing,"preview restores paused without game writes");
        final GameUses stale=g;refused(()->GameUses.restore(stale.encode(),cart("-- changed\n")));refused(()->stale.proposal().candidate(cart("-- changed\n")));
        byte[] bad=Arrays.copyOf(g.encode(),g.encode().length+1);refused(()->GameUses.restore(bad,layered));g.layerMove=2;final GameUses invalid=g;refused(()->GameUses.restore(invalid.encode(),layered));
        GameUses old=new GameUses(layered,0);old.addAnimation(new SpriteRegion(0,0,8,8));old.animation.field=2;old.beginField();old.animation.change(1);byte[] v4=Arrays.copyOf(old.encode(),old.encode().length-5);v4[3]=4;
        GameUses previous=GameUses.restore(v4,layered);previous.cancelField();check(previous.animation.frame(0).millis==250,"v4 field draft remains recoverable");
        // Two uses refer to the same sheet data, not copies in an external library.
        GameUses shared=new GameUses(cart("-- shared\n"),0);shared.addBackground(new SpriteRegion(0,64,8,8));WorkshopCartridge lower=apply(shared);resources(shared.base,lower);
        check(new GameUses(lower,0).current().call.form.value(1).equals("64"),"lower shared-gfx source remains a valid resource binding");
        shared=new GameUses(lower,0);shared.duplicate();shared.background.form.set(4,"40");WorkshopCartridge linked=apply(shared);
        WorkshopCartridge painted=linked.withPixel(new SpriteRegion(0,64,8,8),0,0,12);GameSketch sharedSketch=new GameSketch(painted,0);
        check(sharedSketch.pixels[24*128]==12&&sharedSketch.pixels[40*128]==12,"sheet edit reaches both uses without copying pixels");
        check(painted.map().tile(0,32)==12,"background's lower sheet follows standard shared map memory");
        WorkshopCartridge overlap=cart("function _draw()\n cls(1)\n"+a+a.replace("sspr(0,0,16,16","sspr(1,0,16,16")+"end\n");
        GameUses stack=new GameUses(overlap,0);check(stack.entries.size()==2,"overlapping layers recognized");
        check(new GameSketch(overlap,0).pixels[0]==2,"later nontransparent layer covers earlier layer");
        stack.index=1;stack.edit();stack.background.form.set(7,"false");check(new GameSketch(apply(stack),0).pixels[0]==1,"hidden layer reveals previous resource");
        Port p=new Port();WorkshopSession s=new WorkshopSession(cart("-- controller blank\n"),p);s.openUses(false);menu(s,10);
        check(s.uses.background!=null&&s.codeDraft==null,"controller entry from blank without Lua");byte[] original=s.cart().bytes();s.act(Action.CONFIRM);s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&p.launches==0,"unfinished picker explains Test prerequisite");act(s,Action.CONFIRM,Action.CONFIRM,Action.CONFIRM);
        act(s,Action.DOWN,Action.RIGHT);check(s.uses.background.form.value(4).equals("24"),"navigation never changes field");act(s,Action.CONFIRM,Action.RIGHT,Action.CONFIRM);
        byte[] draft=s.uses.proposal().candidate(s.cart()).bytes();s.act(Action.TEST);check(Arrays.equals(p.launched,draft)&&Arrays.equals(s.cart().bytes(),original)&&p.writes==0,"Test launches draft without saving");
        s.act(Action.MENU);p.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.uses.background!=null&&s.undoCount()==0,"failed Apply keeps draft and history");p.fail=false;act(s,Action.CONFIRM,Action.CONFIRM);
        check(p.writes==1&&s.undoCount()==1&&s.uses.current().isBackground(),"Apply is one operation and selects saved layer");byte[] saved=s.cart().bytes();s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),original),"one Undo removes layer and callback");s.act(Action.REDO);check(Arrays.equals(saved,s.cart().bytes()),"Redo exact bytes");
        s.act(Action.CONFIRM);s.uses.background.field=4;act(s,Action.CONFIRM,Action.RIGHT,Action.CONFIRM,Action.MENU,Action.CONFIRM);check(s.uses.current().call.form.value(7).equals("false"),"visibility persisted");
        menu(s,2);s.act(Action.MENU);s.act(Action.CONFIRM);check(s.uses.index==1&&s.uses.entries.size()==2,"duplicate selects adjacent new layer");
        menu(s,11);s.act(Action.CONFIRM);check(s.uses.index==0,"controller reorder selects moved layer");menu(s,3);s.act(Action.CANCEL);check(s.uses.entries.size()==2,"delete cancel preserves layer");
        menu(s,3);s.act(Action.CONFIRM);check(s.uses.entries.size()==1,"remove only selected use");
        System.out.println("BackgroundUsesTest: "+checks+" checks passed");
    }
}
