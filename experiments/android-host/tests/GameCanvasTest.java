import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Blank -> draw/place -> linked/independent edits, storage failure and recovered Test. */
public final class GameCanvasTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("Unsafe change accepted");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge cart(String lua,String nl){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com"+nl+"version 43"+nl+"__lua__"+nl+lua.replace("\n",nl)).getBytes(StandardCharsets.ISO_8859_1));}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;int saves;boolean fail,stale;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saved=bytes;saves++;}
        public void save(byte[] expected,byte[] bytes)throws Exception{if(stale)throw new Exception("changed outside");if(saved!=null)check(Arrays.equals(saved,expected),"compare-and-save source");save(bytes);}
        public void launch(byte[] bytes){launched=bytes;}
    }
    static void choose(WorkshopSession s,int index){s.game.focus=index;s.act(Action.CONFIRM);}
    static void select(WorkshopSession s,int index,int action){choose(s,index+1);choose(s,action);}
    static void done(WorkshopSession s){s.act(Action.CANCEL);choose(s,0);}
    static WorkshopSession session(WorkshopCartridge c,Port p){WorkshopSession s=new WorkshopSession(c,p);s.openGame();return s;}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=cart("-- ordinary blank\nfunction _draw()\n cls(1)\nend\n",nl);Port p=new Port();WorkshopSession s=session(blank,p);
            check(s.game.blocked.isEmpty(),"blank recognized");choose(s,0);choose(s,1);check(s.game.region().x==8,"tile zero remains reserved");
            s.game.paint(2,3);s.game.paint(3,3);s.game.color=10;s.game.paint(2,4);done(s);
            check(s.game.screen==GameCanvas.Screen.PLACE&&p.saves==0,"draw and place one unpublished transaction");
            s.game.point(24,40);byte[] recovery=s.game.encode();s.act(Action.TEST);
            same(blank.bytes(),s.cart().bytes(),"Test does not save");check(new GameCanvas(new WorkshopCartridge(p.launched)).image()[40*128+24+2+3*128]==14,"Test includes drawn pixels and position");
            WorkshopSession restored=session(blank,p);restored.restoreGame(recovery);same(p.launched,restored.game.candidate(blank).bytes(),"recover identical pending placement");
            s=restored;p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.game.draft()&&s.undoCount()==0,"failed save keeps entire draft");p.fail=false;s.act(Action.CANCEL);s.act(Action.CONFIRM);
            check(s.mode==Mode.GAME&&s.undoCount()==1&&p.saves==1,"single save and Undo for image plus placement");WorkshopCartridge first=s.cart();
            s.act(Action.UNDO);same(blank.bytes(),s.cart().bytes(),"Undo removes pixels AND draw call");s.act(Action.REDO);same(first.bytes(),s.cart().bytes(),"Redo restores both");
            select(s,0,2);s.game.point(60,40);s.act(Action.CONFIRM);check(s.game.uses.entries.size()==2,"duplicate placement");
            select(s,1,0);check(s.game.screen==GameCanvas.Screen.SHARED&&s.game.sharedUses()==2,"shared appearance asks before editing");choose(s,1);
            check(s.game.operation==GameCanvas.Operation.INDEPENDENT,"explicit independent copy");s.game.color=12;s.game.paint(2,3);s.act(Action.TEST);WorkshopCartridge before=s.cart();byte[] copyDraft=s.game.encode();
            WorkshopSession copyRestore=session(before,p);copyRestore.restoreGame(copyDraft);same(p.launched,copyRestore.game.candidate(before).bytes(),"independent pixels and changed reference recover together");
            s=copyRestore;done(s);check(s.game.region(0).x!=s.game.region(1).x,"independent resource used");int[] image=s.game.image();check(image[43*128+26]==14&&image[43*128+62]==12,"copy edits selected instance only");
            select(s,0,2);s.game.point(24,70);s.act(Action.CONFIRM);select(s,0,0);choose(s,0);s.game.color=11;s.game.paint(2,3);done(s);
            image=s.game.image();check(image[43*128+26]==11&&image[73*128+26]==11&&image[43*128+62]==12,"shared edit affects shared instances only");
            WorkshopCartridge shared=s.cart();select(s,0,1);s.game.point(5,5);s.act(Action.CANCEL);same(shared.bytes(),s.cart().bytes(),"cancel move keeps source");
            select(s,0,1);s.game.point(5,5);p.stale=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.game.screen==GameCanvas.Screen.PLACE,"outside write refuses and retains position");p.stale=false;s.act(Action.CANCEL);s.act(Action.CONFIRM);
            check(s.game.uses.entries.get(0).x()==5,"move saves after retry");select(s,2,3);choose(s,0);check(s.game.uses.entries.size()==3,"delete defaults to keep");select(s,2,3);choose(s,1);check(s.game.uses.entries.size()==2,"remove only placement");
            check(!s.cart().empty(new SpriteRegion(8,0,16,16)),"deletion preserves resource");
            byte[] saved=s.cart().bytes();WorkshopSession reopened=session(new WorkshopCartridge(saved),new Port());check(reopened.game.uses.entries.size()==2,"reopens from ordinary p8 without metadata");same(saved,reopened.cart().bytes(),"open is lossless");
            choose(reopened,0);choose(reopened,0);reopened.game.paint(0,0);done(reopened);reopened.act(Action.CANCEL);check(reopened.game.screen==GameCanvas.Screen.DRAW,"back from placement retains painting");reopened.act(Action.CANCEL);choose(reopened,4);same(saved,reopened.cart().bytes(),"discard addition leaves no orphan resources");
            byte[] corrupt=Arrays.copyOf(recovery,recovery.length+1);refused(()->GameCanvas.restore(corrupt,blank));refused(()->GameCanvas.restore(recovery,first));
            for(String code:new String[]{"function _draw()\n cls(1)\nend\npoke(0x5f54,0)\n","function _draw()\n cls(1)\n spr(n,0,0)\nend\n","function _draw()\n cls(1)\nend\nfunction _update()\n sset(10,10,7)\nend\n","sspr=foo\nfunction _draw()\n cls(1)\nend\n","#include other.lua\nfunction _draw()\n cls(1)\nend\n"}){
                GameCanvas unknown=new GameCanvas(cart(code,nl));check(!unknown.blocked.isEmpty(),"unknown code is not treated as safe blank");refused(()->unknown.allocate(8,8));
            }
            WorkshopCartridge refs=cart("function _draw()\n cls(3)\n sspr(8,0,16,16,20,20)\nend\n",nl);
            GameCanvas allocator=new GameCanvas(refs);check(allocator.allocate(8,8).x==24,"transparent referenced region reserved");check(allocator.image()[0]==3,"preview follows clear color");
            for(int size=0;size<4;size++){
                GameCanvas g=new GameCanvas(blank);g.add(size);g.paint(0,0);g.paint(g.region().width-1,g.region().height-1);
                GameCanvas roundtrip=GameCanvas.restore(g.encode(),blank);same(g.candidate(blank).bytes(),roundtrip.candidate(blank).bytes(),"all offered sizes recover exact pixels and dimensions");
                g.screen=GameCanvas.Screen.DRAW_MENU;g.focus=99;byte[] invalid=g.encode();refused(()->GameCanvas.restore(invalid,blank));
            }
            refs=refs.withTile(0,0,3).withFlags(4,1).withPixel(new SpriteRegion(40,0,8,8),0,0,7);allocator=new GameCanvas(refs);
            check(allocator.allocate(8,8).x==48,"map flags and pixels all reserve space");
            int[] filled=new int[16384];Arrays.fill(filled,0,128*64,7);GameCanvas full=new GameCanvas(blank.withSheetPixels(filled));refused(()->full.allocate(8,8));
            WorkshopCartridge lower=cart("function _draw()\n cls(1)\n sspr(0,64,16,16,20,20)\nend\n",nl).withPixel(new SpriteRegion(0,64,16,16),1,1,8);WorkshopSession ls=session(lower,new Port());select(ls,0,0);choose(ls,0);check(ls.mode==Mode.ERROR,"lower memory requires existing consequence editor");ls.act(Action.CANCEL);choose(ls,1);ls.game.color=10;ls.game.paint(1,1);done(ls);
            for(int yy=64;yy<128;yy++)for(int xx=0;xx<128;xx++)check(ls.cart().sheetPixel(xx,yy)==lower.sheetPixel(xx,yy),"independent copy preserves shared gfx/map bytes");
        }
        System.out.println("GameCanvasTest: "+checks+" checks passed");
    }
}
