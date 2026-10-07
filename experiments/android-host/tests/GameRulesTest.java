import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public final class GameRulesTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable call){try{call.run();throw new AssertionError("unsafe action accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    static WorkshopCartridge cart(String code){return GameUsesTest.cart(code);}
    static String text(WorkshopCartridge c){return new LuaDraft(c,0).text();}
    static WorkshopCartridge apply(GameRules g){g.review();WorkshopCartridge c=g.proposal().candidate(g.base);GameUsesTest.resources(g.base,c);return c;}
    static WorkshopCartridge state(WorkshopCartridge c,String name,int value){GameRules g=new GameRules(c);g.add("state");g.draft.name=name;g.draft.value=value;return apply(g);}
    static WorkshopCartridge rule(WorkshopCartridge c,int event,int button,String guard,int compare,int threshold,int action,String target,int value){GameRules g=new GameRules(c);g.add("rule");GameRules.Entry e=g.draft;e.event=event;e.button=button;e.guard=guard;e.compare=compare;e.threshold=threshold;e.action=action;e.target=target;e.value=value;return apply(g);}
    static class Port implements WorkshopSession.Port {int writes,launches;byte[] launched;boolean fail;public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");writes++;}public void launch(byte[] b){launches++;launched=b;}}
    static void act(WorkshopSession s,Action... keys){for(Action a:keys)s.act(a);}
    static void menu(WorkshopSession s,int n){s.act(Action.MENU);for(int k=0;k<n;k++)s.act(Action.DOWN);s.act(Action.CONFIRM);}
    public static void main(String[] args)throws Exception {
        WorkshopCartridge finalGame=null;
        for(String nl:new String[]{"\n","\r\n","\r"}){
            WorkshopCartridge blank=cart("-- original project"+nl);GameRules empty=new GameRules(blank);
            check(empty.blocked.isEmpty()&&empty.entries.isEmpty(),"blank can start without a hero or code");refused(()->empty.add("rule"));refused(()->empty.add("readout"));
            WorkshopCartridge c=state(blank,"score",0);c=state(c,"phase",0);
            c=rule(c,1,4,"phase",0,0,0,"score",1);
            c=rule(c,0,4,"score",5,5,1,"phase",1);
            c=rule(c,1,5,"",0,0,2,"",0);
            GameRules display=new GameRules(c);display.add("readout");display.draft.name="score";c=apply(display);
            display=new GameRules(c);display.add("readout");display.draft.name="phase";display.draft.y=32;display.draft.color=10;c=apply(display);finalGame=c;
            check(!c.hasHero()&&text(c).startsWith(text(blank)),"ordinary hero-free game preserves prefix");
            check(text(c).contains("if btnp(4) and (phase==0) then"+nl+"  score+=1"),"input guarded by live state");
            check(text(c).contains("if true and (score>=5) then"+nl+"  phase=1"),"goal condition changes another value");
            check(text(c).contains("if btnp(5) then"+nl+"  _init()"),"restart calls ordinary numeric initializer");
            GameRules r=new GameRules(c);check(r.blocked.isEmpty()&&r.entries.size()==7&&r.states.size()==2,"all forms reopen from code alone");
            check(new GameUses(c,0).blocked.isEmpty(),"readouts coexist with game-use editor");
            GameUses sprite=new GameUses(c,0);sprite.addSprite(new SpriteRegion(0,0,8,8));c=sprite.proposal().candidate(c);check(new GameRules(c).entries.size()==7,"resource placement preserves rules and displays");
            for(int n=0;n<7;n++){r=new GameRules(c);r.index=n;r.edit();check(Arrays.equals(apply(r).bytes(),c.bytes()),"no-op round trip "+n);}
            r=new GameRules(c);r.index=2;r.duplicate();WorkshopCartridge copy=apply(r);check(new GameRules(copy).entries.size()==8,"rule duplicate");
            r=new GameRules(copy);r.index=5;r.remove();check(Arrays.equals(r.proposal().candidate(copy).bytes(),c.bytes()),"remove copied rule exactly");
            r=new GameRules(c);r.index=3;r.reorder(-1);WorkshopCartridge swapped=r.proposal().candidate(c);check(text(swapped).indexOf("score>=5")<text(swapped).indexOf("btnp(4)"),"explicit execution order");r=new GameRules(swapped);r.index=2;r.reorder(1);check(Arrays.equals(r.proposal().candidate(swapped).bytes(),c.bytes()),"reverse order exact");
            r=new GameRules(c);final GameRules inUse=r;refused(inUse::remove);check(Arrays.equals(c.bytes(),inUse.base.bytes()),"referenced state cannot be deleted");
            ByteArrayOutputStream out=new ByteArrayOutputStream();CartridgeExport ex=new CartridgeExport("blank-0001","My rules",c.bytes());ex.writeTo(out);ex.verify(new ByteArrayInputStream(out.toByteArray()));check(new GameRules(new WorkshopCartridge(out.toByteArray())).entries.size()==7,"export needs no editor metadata");
        }
        WorkshopCartridge comments=cart("-- untouched\nfunction _init()\n score = 0 -- start\nend\nfunction _update()\n if btnp(4) then -- event\n  score += 1 -- action\n end -- keep\nend\n");
        GameRules r=new GameRules(comments);check(r.entries.size()==2,"commented statements recognized");r.index=1;r.edit();check(Arrays.equals(apply(r).bytes(),comments.bytes()),"commented no-op exact");r.draft.value=2;WorkshopCartridge changed=apply(r);check(text(changed).contains("-- event")&&text(changed).contains("-- action")&&text(changed).contains("end -- keep"),"editing keeps every comment");r=new GameRules(comments);r.index=1;final GameRules noted=r;refused(noted::remove);
        for(String bad:new String[]{"#include rules.lua\n","local _init=function() end\n","_update=other\n","function _update60()\nend\n","function _init()\n local score=0\nend\n","function _init()\n score=rnd(10)\nend\n","function _update()\n if btnp(4) then\n  missing+=1\n end\nend\n","if true then\nfunction _init()\nend\nend\n","function _init()\nend\nfunction _init()\nend\n","local btnp=fn\n"}){GameRules unsupported=new GameRules(cart(bad));check(!unsupported.blocked.isEmpty(),"unsafe callback rejected");refused(()->unsupported.add("state"));}
        r=new GameRules(cart("-- names\nscore=9\n"));r.add("state");r.draft.name="score";final GameRules collision=r;refused(collision::proposal);
        WorkshopCartridge base=state(cart("-- session\n"),"score",0);GameUses g=new GameUses(base,0);g.logic=new GameRules(base);g.screen=GameUses.Screen.LOGIC;g.logic.add("rule");g.logic.field=7;g.logic.beginField();g.logic.change(7);
        GameUses restored=GameUses.restore(g.encode(),base);check(restored.logic.editing()&&restored.logic.draft.value==8,"field draft recovers");restored.logic.revertField();check(restored.logic.draft.value==1,"recovered field reverts");
        final GameUses journal=g;refused(()->GameUses.restore(journal.encode(),cart("-- stale\n")));refused(()->GameUses.restore(Arrays.copyOf(journal.encode(),journal.encode().length+1),base));
        GameUses old=new GameUses(base,0);byte[] v5=Arrays.copyOf(old.encode(),old.encode().length-1);v5[3]=5;check(GameUses.restore(v5,base).logic==null,"v5 journals preserved");
        Port p=new Port();WorkshopSession s=new WorkshopSession(cart("-- blank controller\n"),p);s.openUses(false);menu(s,13);check(s.codeDraft==null&&s.uses.logic!=null,"entry needs no Lua cursor");s.act(Action.CONFIRM);act(s,Action.DOWN,Action.CONFIRM,Action.RIGHT,Action.CANCEL);check(s.uses.logic.draft.value==0,"field cancel");
        byte[] original=s.cart().bytes();s.act(Action.TEST);check(p.launches==1&&p.writes==0&&Arrays.equals(original,s.cart().bytes()),"Test is isolated from saved project");
        act(s,Action.MENU);p.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.undoCount()==0,"failed write retains draft");p.fail=false;act(s,Action.CONFIRM,Action.CONFIRM);byte[] saved=s.cart().bytes();check(s.undoCount()==1&&s.uses.logic.entries.size()==1,"Apply one history step");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),original),"Undo exact");s.act(Action.REDO);check(Arrays.equals(s.cart().bytes(),saved),"Redo exact");
        menu(s,1);act(s,Action.MENU,Action.CONFIRM);check(s.uses.logic.entries.size()==2,"rule saved from controller");menu(s,2);act(s,Action.MENU,Action.CONFIRM);check(s.uses.logic.entries.size()==3,"readout saved from controller");
        if(args.length>0){String bytes=new String(finalGame.bytes(),java.nio.charset.StandardCharsets.UTF_8);bytes=bytes.substring(0,bytes.indexOf("__sfx__"));Files.write(Paths.get(args[0]),bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        System.out.println("GameRulesTest: "+checks+" checks passed");
    }
}
