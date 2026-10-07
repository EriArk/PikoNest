import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

public final class CameraUsesTest {
    static int checks;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException|IllegalStateException e){checks++;}}
    static WorkshopCartridge cart(String s){return GameUsesTest.cart(s);}
    static String text(WorkshopCartridge c){return GameUsesTest.text(c);}
    static void resources(WorkshopCartridge a,WorkshopCartridge b){
        String x=new String(a.bytes(),java.nio.charset.StandardCharsets.UTF_8),y=new String(b.bytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(x.substring(x.indexOf("__gfx__")).equals(y.substring(y.indexOf("__gfx__"))),"resource sections unchanged");
    }
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;}
        public void launch(byte[] b){launches++;}
    }
    static void menu(WorkshopSession s,int n){s.act(Action.MENU);for(int i=0;i<n;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);}
    public static void main(String[] args){
        String plain="function _draw()\n cls(1)\n sspr(0,0,8,8,24,40) -- object\n -- between\n map(0,0,0,80,16,2)\n spr(3,64,40)\nend\n-- tail\n";
        for(String nl:new String[]{"\n","\r\n","\r"}){
            WorkshopCartridge base=cart(plain.replace("\n",nl));GameUses g=new GameUses(base,3);g.addCamera(false);
            g.camera.form.set(0,"16");g.camera.form.set(1,"8");g.camera.rangeEnd=1;g.review();
            GameUses restored=GameUses.restore(g.encode(),base);check(restored.camera.rangeEnd==1&&restored.screen==GameUses.Screen.REVIEW,"range review recovery");
            WorkshopCartridge moved=restored.proposal().candidate(base);resources(base,moved);
            String expected=plain.replace(" sspr("," camera(16,8)\n sspr(").replace(" spr(3", " camera()\n spr(3").replace("\n",nl);
            check(text(moved).equals(expected),"only two camera calls inserted, comments/order retained");
            GameUses list=new GameUses(moved,3);check(list.blocked.isEmpty()&&list.entries.size()==5,"camera/reset entries visible");
            check(CameraUse.offset(list.entries.get(1).view)[0]==16&&CameraUse.offset(list.entries.get(4).view)[0]==0,"world and screen scopes");
            list.edit();check(Arrays.equals(list.proposal().candidate(moved).bytes(),moved.bytes()),"no-op camera edit preserves bytes");
            list.camera.field=1;list.camera.change(8);list.review();WorkshopCartridge changed=list.proposal().candidate(moved);
            check(text(changed).equals(text(moved).replace("camera(16,8)","camera(24,8)")),"only selected camera args change");
            GameUses cp=new GameUses(changed,3);cp.index=1;cp.duplicate();cp.field=4;cp.adjust(1);cp.review();WorkshopCartridge copied=cp.proposal().candidate(changed);
            GameUses copies=new GameUses(copied,3);check(copies.entries.get(2).kind().equals("sspr")&&CameraUse.offset(copies.entries.get(2).view)[0]==24,"duplicate stays inside source camera scope");
            copies.index=2;copies.delete();check(Arrays.equals(copies.proposal().candidate(copied).bytes(),changed.bytes()),"delete copied resource exact restoration");
            GameUses nested=new GameUses(moved,3);nested.index=1;nested.addCamera(false);nested.camera.form.set(0,"5");nested.review();WorkshopCartridge scoped=nested.proposal().candidate(moved);
            GameUses scopes=new GameUses(scoped,3);check(CameraUse.offset(scopes.entries.get(2).view)[0]==5&&CameraUse.offset(scopes.entries.get(4).view)[0]==16,"nested override restores previous camera rather than blindly resetting");
            GameUses crossing=new GameUses(moved,3);crossing.index=1;crossing.addCamera(false);crossing.camera.rangeEnd=4;refused(crossing::review);
            GameUses hud=new GameUses(moved,3);hud.index=2;hud.addCamera(true);hud.review();WorkshopCartridge reset=hud.proposal().candidate(moved);
            GameUses resetList=new GameUses(reset,3);check(CameraUse.offset(resetList.entries.get(3).view)[0]==0,"explicit screen boundary before chosen map");
            GameUses stale=g;refused(()->stale.proposal().candidate(changed));refused(()->GameUses.restore(stale.encode(),changed));
        }
        WorkshopCartridge base=cart(plain);GameUses g=new GameUses(base,0);g.addCamera(false);g.camera.change(1);
        check(g.camera.form.item().id.equals("camera_follow"),"controller mode switching");g.camera.form.set(0,"200");g.camera.form.set(1,"300");g.camera.form.set(2,"32");g.camera.form.set(3,"32");
        check(Arrays.equals(CameraUse.offset(g.camera.form),new int[]{128,128}),"follow clamp");g.camera.change(1);
        check(g.camera.form.item().id.equals("camera_rooms")&&g.camera.form.value(2).equals("2"),"room mode preserves world dimensions");
        check(Arrays.equals(CameraUse.offset(g.camera.form),new int[]{128,128}),"room quantization");g.review();WorkshopCartridge rooms=g.proposal().candidate(base);
        GameUses opened=new GameUses(rooms,0);opened.edit();check(opened.camera.form.item().id.equals("camera_rooms"),"existing rooms reopen without Lua cursor");opened.camera.form.set(0,"-32768");check(CameraUse.offset(opened.camera.form)[0]==0,"negative point clamp before arithmetic");
        opened.camera.form.set(0,"x");check(CameraUse.offset(opened.camera.form)==null,"dynamic expression not falsely evaluated in preview");
        WorkshopCartridge symbols=cart("x=20\ny=80\n"+plain);GameUses named=new GameUses(symbols,0);named.addCamera(false);named.camera.field=1;named.camera.symbol();check(named.camera.form.value(0).equals("24"),"target placement coordinate available without typing");named.camera.symbol();check(named.camera.form.value(0).equals("x"),"project variable chosen without keyboard");named.camera.form.set(0,"x+1");refused(()->named.camera.change(1));
        GameUses comment=new GameUses(cart("function _draw()\n cls(1)\n\tcamera( 16, 8 ) -- keep\n spr(1,20,20)\n camera()\nend\n"),0);comment.edit();comment.camera.change(1);comment.review();
        String switched=text(comment.proposal().candidate(comment.base));check(switched.contains("\tcamera(mid(")&&switched.contains(" -- keep"),"mode switch preserves indentation and inline comment");
        GameUses deletion=new GameUses(comment.base,0);deletion.delete();String removed=text(deletion.proposal().candidate(comment.base));check(removed.contains("-- keep")&&!removed.contains("camera( 16"),"delete camera retains inline note");
        GameUses resetEdit=new GameUses(comment.base,0);resetEdit.index=2;resetEdit.edit();resetEdit.camera.change(-1);resetEdit.review();check(new GameUses(resetEdit.proposal().candidate(comment.base),0).entries.get(2).call.form.item().id.equals("camera_rooms"),"existing reset can become configured camera");
        GameUses empty=new GameUses(cart("-- blank\n"),0);refused(()->empty.addCamera(false));
        GameUses unsupported=new GameUses(cart("function _draw()\n if true then\n camera(1,1)\n end\nend\n"),0);refused(()->unsupported.addCamera(false));
        GameUses shadow=new GameUses(cart("camera=function() end\n"+plain),0);refused(()->shadow.addCamera(false));
        GameUses dynamic=new GameUses(cart("function _draw()\n camera(rnd(20),0)\n spr(1,20,20)\nend\n"),0);dynamic.index=1;dynamic.addCamera(false);refused(dynamic::review);
        dynamic.back();dynamic.addCamera(true);dynamic.review();check(dynamic.proposal().candidate(dynamic.base)!=null,"explicit screen reset does not re-evaluate old dynamic camera");
        GameUses known=new GameUses(cart("function _draw()\n camera(10,20)\n cls(1)\n spr(1,0,0)\nend\n"),0);check(known.entries.size()==2&&CameraUse.offset(known.entries.get(1).view)[1]==20,"legacy camera before cls supported");
        GameUses journal=new GameUses(base,0);journal.addCamera(false);journal.camera.field=2;check(GameUses.restore(journal.encode(),base).camera.field==2,"form field recovery");journal.back();check(GameUses.restore(journal.encode(),base).screen==GameUses.Screen.LIST,"cancel journal");
        GameUses old=new GameUses(base,0);old.addSprite(new SpriteRegion(0,0,8,8));byte[] v2=Arrays.copyOf(old.encode(),old.encode().length-11);v2[3]=2;check(GameUses.restore(v2,base).screen==GameUses.Screen.FORM,"v2 journals remain readable");
        GameUses cp=new GameUses(comment.base,0);cp.index=1;cp.duplicate();check(GameUses.restore(cp.encode(),comment.base).copyAfter,"copy retains camera placement through recovery");
        String animation=new SpriteAnimation(new SpriteRegion(0,0,8,8)).code();GameUses animated=new GameUses(cart("function _draw()\n cls(1)\n camera(16,8)\n"+animation+" camera()\nend\n"),0);animated.index=1;animated.duplicate();animated.review();GameUses doubled=new GameUses(animated.proposal().candidate(animated.base),0);
        check(doubled.entries.get(2).animation!=null&&CameraUse.offset(doubled.entries.get(2).view)[0]==16,"animation duplicate also retains camera");

        Port port=new Port();WorkshopSession s=new WorkshopSession(base,port);s.openUses(false);menu(s,8);
        check(s.uses.screen==GameUses.Screen.CAMERA,"controller camera entry");s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.NEXT);s.act(Action.NEXT);s.act(Action.CONFIRM);s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.NEXT);s.act(Action.CONFIRM);
        s.act(Action.TEST);check(port.saves==0&&port.launches==1,"draft Test without save");s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CONFIRM);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.uses.screen==GameUses.Screen.REVIEW&&s.codeDraft==null,"human review without Lua");port.fail=true;s.act(Action.CONFIRM);
        check(s.mode==WorkshopSession.Mode.ERROR&&s.uses.camera!=null&&s.undoCount()==0&&Arrays.equals(s.cart().bytes(),base.bytes()),"save failure retains proposal/history/source");port.fail=false;s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.uses.index==0&&s.uses.current().isCamera()&&s.undoCount()==1,"saved camera selected, one history item");byte[] result=s.cart().bytes();s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),base.bytes()),"one undo removes both camera calls");s.act(Action.REDO);check(Arrays.equals(result,s.cart().bytes()),"redo exact source");
        menu(s,3);s.act(Action.CANCEL);check(port.saves==3,"delete cancel writes nothing");menu(s,3);s.act(Action.CONFIRM);check(!s.uses.current().isCamera(),"delete single camera setting");s.act(Action.UNDO);s.act(Action.TEST);check(port.launches==2,"saved camera scene Test");
        System.out.println("CameraUsesTest: "+checks+" checks passed");
    }
}
