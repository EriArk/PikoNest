import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

public final class AnimationUsesTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    static WorkshopCartridge cart(String s){return GameUsesTest.cart(s);}
    static String text(WorkshopCartridge c){return GameUsesTest.text(c);}
    static void resources(WorkshopCartridge a,WorkshopCartridge b){
        String x=new String(a.bytes(),java.nio.charset.StandardCharsets.UTF_8),y=new String(b.bytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(x.substring(x.indexOf("__gfx__")).equals(y.substring(y.indexOf("__gfx__"))),"all resource bytes preserved");
    }
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] saved;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saves++;saved=bytes;}
        public void launch(byte[] bytes){launches++;}
    }
    static void menu(WorkshopSession s,int n){s.act(Action.MENU);for(int i=0;i<n;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String source=("-- keep header\nfunction _draw()\n cls(1)\n sspr(8,16,16,8,24,40) -- keep placement note\n map(0,0,0,80,16,2)\n spr(3,64,40)\nend\n-- keep tail\n").replace("\n",nl);
            WorkshopCartridge base=cart(source);GameUses g=new GameUses(base,2);g.animateSelected();
            check(g.animation.x==24&&g.animation.y==40&&g.animation.frame(0).region.x==8&&g.animation.frame(0).region.height==8,"conversion keeps placement and region");
            g.back();check(GameUses.restore(g.encode(),base).screen==GameUses.Screen.LIST,"cancel conversion restores ordinary list");g.animateSelected();
            g.animation.duplicate();g.animation.durationStep(3);g.animation.beginPick(false);g.animation.picker.point(32,16);g.animation.acceptPick();g.animation.acceptPick();g.review();
            GameUses recovered=GameUses.restore(g.encode(),base);check(recovered.converting&&recovered.animation.count()==2,"conversion recovery");
            WorkshopCartridge animated=recovered.proposal().candidate(base);resources(base,animated);
            String changed=text(animated);check(changed.contains("-- keep placement note")&&changed.contains("-- keep tail"),"conversion preserves comments");
            GameUses list=new GameUses(animated,2);check(list.blocked.isEmpty()&&list.entries.size()==3,"animation participates in common list "+list.blocked);
            check(list.entries.get(0).animation!=null&&list.entries.get(1).kind().equals("map")&&list.entries.get(2).kind().equals("spr"),"draw order preserved");
            list.edit();check(Arrays.equals(list.proposal().candidate(animated).bytes(),animated.bytes()),"opening and saving animation is byte-exact no-op");
            list.animation.durationStep(1);list.review();WorkshopCartridge edited=list.proposal().candidate(animated);
            check(text(edited).contains(" map(0,0,0,80,16,2)")&&text(edited).contains(" spr(3,64,40)"),"neighbour calls unchanged");resources(animated,edited);
            GameUses copy=new GameUses(edited,2);copy.duplicate();copy.animation.x=80;copy.review();WorkshopCartridge duplicated=copy.proposal().candidate(edited);
            GameUses all=new GameUses(duplicated,2);check(all.entries.size()==4&&all.entries.get(0).x()==24&&all.entries.get(3).x()==80,"independent duplicate appended after other uses");
            all.index=3;all.delete();check(Arrays.equals(all.proposal().candidate(duplicated).bytes(),edited.bytes()),"delete duplicate restores original source exactly");
            all=new GameUses(edited,2);all.index=2;all.edit();all.field=1;all.adjust(1);check(text(all.proposal().candidate(edited)).contains("spr(3,65,40)"),"static placements remain editable next to animation");
            GameUses stale=copy;refused(()->stale.proposal().candidate(base));refused(()->GameUses.restore(stale.encode(),base));
        }
        WorkshopCartridge blank=cart("-- own blank\n");GameUses added=new GameUses(blank,0);added.addAnimation(new SpriteRegion(16,0,8,8));added.review();
        WorkshopCartridge created=added.proposal().candidate(blank);GameUses list=new GameUses(created,0);
        check(list.blocked.isEmpty()&&list.entries.size()==1&&text(created).contains("function _draw()\n cls(1)\n do"),"blank automatically gains ordinary draw callback");
        list.addMap(0,0);list.review();WorkshopCartridge mapped=list.proposal().candidate(created);check(new GameUses(mapped,0).entries.size()==2,"map addition survives animated draw");
        list=new GameUses(mapped,0);list.delete();check(text(list.proposal().candidate(mapped)).contains("map(")&&!text(list.proposal().candidate(mapped)).contains("local frames"),"delete only the selected animation");
        String block=new SpriteAnimation(new SpriteRegion(0,0,8,8)).code();
        for(String source:new String[]{"function _draw()\n if true then\n"+block+" end\nend\n","function _draw()\n"+block.replace("time()","time()+1")+"end\n","function _draw()\n"+block+"cls(1)\nend\n"}){
            GameUses blocked=new GameUses(cart(source),0);check(!blocked.blocked.isEmpty(),"unknown/nested/reordered draw remains blocked");refused(()->blocked.addAnimation(new SpriteRegion(0,0,8,8)));
        }
        for(String api:new String[]{"sspr","time","cls"}){
            GameUses shadow=new GameUses(cart(api+"=function() end\n"),0);refused(()->shadow.addAnimation(new SpriteRegion(0,0,8,8)));
        }
        GameUses map=new GameUses(mapped,0);map.index=1;refused(map::animateSelected);
        GameUses far=new GameUses(cart("function _draw()\n spr(1,200,0)\nend\n"),0);refused(far::animateSelected);
        GameUses g=new GameUses(created,2);g.edit();g.animation.duplicate();g.animation.beginPick(false);g.animation.picker.next();
        GameUses restored=GameUses.restore(g.encode(),created);check(restored.animation.picker.phase==1&&restored.animation.count()==2,"frame picker recovery");
        restored.animation.cancelPick();restored.animation.cancelPick();restored.animation.beginPick(true);
        check(GameUses.restore(restored.encode(),created).animation.picker.phase==2,"position picker recovery");
        g=new GameUses(created,2);g.edit();g.animation.toggle();g.animation.advance(120);restored=GameUses.restore(g.encode(),created);
        check(!restored.animation.playing&&restored.animation.elapsed()==120,"restore pauses preview at saved time");
        g.back();check(GameUses.restore(g.encode(),created).screen==GameUses.Screen.LIST,"cancel discards form but list journal remains valid");
        GameUses old=new GameUses(blank,0);old.addSprite(new SpriteRegion(0,0,8,8));byte[] v1=Arrays.copyOf(old.encode(),old.encode().length-8);v1[3]=1;
        check(GameUses.restore(v1,blank).screen==GameUses.Screen.FORM,"version 1 placement journals still restore");
        final GameUses valid=restored;refused(()->GameUses.restore(Arrays.copyOf(valid.encode(),valid.encode().length-1),created));
        restored.animation.review=true;final GameUses invalid=restored;refused(()->GameUses.restore(invalid.encode(),created));

        Port port=new Port();WorkshopSession s=new WorkshopSession(blank,port);s.openUses(false);menu(s,6);
        check(s.mode==WorkshopSession.Mode.USES&&s.uses.screen==GameUses.Screen.ANIMATION,"controller entry from workshop placement menu");
        s.act(Action.TEST);check(port.launches==1&&port.saves==0,"Test draft without save");
        s.uses.animation.field=3;s.act(Action.CONFIRM);s.uses.animation.field=8;s.act(Action.CONFIRM);
        check(s.uses.screen==GameUses.Screen.REVIEW&&s.codeDraft==null,"review never opens Lua");
        port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.uses.animation.count()==2&&s.undoCount()==0&&Arrays.equals(s.cart().bytes(),blank.bytes()),"failed save preserves form, original and history");
        port.fail=false;s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(port.saves==1&&s.undoCount()==1&&s.uses.current().animation!=null,"one atomic save/history step");
        byte[] saved=s.cart().bytes();s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),blank.bytes()),"one Undo removes entire generated block and callback");s.act(Action.REDO);check(Arrays.equals(saved,s.cart().bytes()),"Redo restores exact cart");
        s.act(Action.CONFIRM);s.act(Action.CANCEL);check(port.saves==3&&s.uses.screen==GameUses.Screen.LIST,"cancel edit writes nothing");
        menu(s,2);check(s.uses.creating&&s.uses.animation.count()==2,"controller duplicate");s.uses.animation.field=8;s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.uses.index==1&&s.uses.entries.size()==2,"duplicate selected in common list");menu(s,3);s.act(Action.CANCEL);check(s.uses.entries.size()==2,"delete cancellation");
        menu(s,3);s.act(Action.CONFIRM);check(s.uses.entries.size()==1,"delete selected whole animation");s.act(Action.UNDO);check(s.uses.entries.size()==2,"delete Undo");
        s.act(Action.TEST);check(port.launches==2,"Test from saved list");menu(s,4);check(s.mode==WorkshopSession.Mode.CODE,"optional Lua navigation works for animation entries");
        System.out.println("AnimationUsesTest: "+checks+" checks passed");
    }
}
