import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.nio.file.*;
import java.util.*;

/** Complete animation transactions and legacy/source preservation, independent of Android. */
public final class GameAnimationTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable action){try{action.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static void row(WorkshopSession s,int row){s.game.focus=row;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"action remains in Game view: "+s.error);}
    static void animate(WorkshopSession s,int element){row(s,element+1);row(s,5);}
    static WorkshopSession session(WorkshopCartridge cart,PlayModelTest.Port port){WorkshopSession s=new WorkshopSession(cart,port);s.openGame();return s;}
    public static void main(String[] args)throws Exception {
        String root=args.length==0?".":args[0];
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("function _draw()\n cls(1)\n sspr(8,0,8,8,20,40)\n sspr(8,0,8,8,70,40)\nend\n",nl).withPixel(new SpriteRegion(8,0,8,8),0,0,14);
            PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=session(blank,port);
            animate(s,0);row(s,4);check(s.game.animation.frame==1,"copy selected");
            SpriteRegion second=s.game.animation.current().region;check(second.x!=8||second.y!=0,"independent allocation");
            same(blank.bytes(),s.cart().bytes(),"copy does not save");row(s,3);s.game.color=10;s.game.paint(0,0);
            byte[] journal=s.game.encode();GameCanvas recovered=GameCanvas.restore(journal,blank);check(recovered.screen==GameCanvas.Screen.DRAW&&recovered.pixel(0,0)==10,"painting recovery");
            s.restoreGame(journal);s.act(Action.TEST);same(blank.bytes(),s.cart().bytes(),"Test preserves saved bytes");
            WorkshopCartridge test=new WorkshopCartridge(port.launched);check(PlayModel.read(test).elements.get(0).animation.frames.size()==2,"Test wires frames");check(test.sheetPixel(second.x,second.y)==10,"Test includes active painting");
            s.act(Action.CANCEL);row(s,0);check(s.game.screen==GameCanvas.Screen.ANIMATION,"drawing returns to frames");
            row(s,2);s.act(Action.RIGHT);s.restoreGame(s.game.encode());s.act(Action.CANCEL);check(s.game.animation.current().ticks==6,"duration reverts after recovery");
            row(s,2);s.act(Action.RIGHT);s.act(Action.CONFIRM);check(s.game.animation.current().ticks==7,"duration retained");
            byte[] proposal=s.game.candidate(s.cart()).bytes();row(s,8);same(proposal,s.cart().bytes(),"one atomic sequence save");
            WorkshopCartridge saved=s.cart();check(saved.sheetPixel(8,0)==14,"shared base sprite preserved");
            s.act(Action.UNDO);same(blank.bytes(),s.cart().bytes(),"Undo pixels and Lua together");s.act(Action.REDO);same(saved.bytes(),s.cart().bytes(),"Redo exact");
            check(PlayModel.read(saved)!=null,"reopen without metadata");
            animate(s,0);row(s,6);check(s.game.animation.frame==1,"reorder later");row(s,8);check(s.game.play.elements.get(0).sx==second.x,"new first frame becomes base appearance");
            animate(s,0);row(s,7);row(s,8);check(s.game.play.elements.get(0).animation.frames.size()==1,"remove sequence reference");check(s.cart().sheetPixel(second.x,second.y)==10,"removed frame pixels kept");
            animate(s,0);row(s,3);check(s.game.region().x!=8||s.game.region().y!=0,"shared frame made independent before painting");s.game.color=12;s.game.paint(0,0);s.act(Action.CANCEL);row(s,0);row(s,8);check(s.cart().sheetPixel(8,0)==14,"other placement pixels unchanged");
            animate(s,1);byte[] unchanged=s.game.candidate(s.cart()).bytes();row(s,3);s.game.paint(1,1);s.restoreGame(s.game.encode());s.act(Action.CANCEL);row(s,4);same(unchanged,s.game.candidate(s.cart()).bytes(),"discard frame after recovery restores allocation and pixels");s.act(Action.CANCEL);
            WorkshopCartridge before=s.cart();animate(s,0);row(s,4);s.act(Action.CANCEL);same(before.bytes(),s.cart().bytes(),"cancel discards all frame copies");
            // Invalid state prerequisites survive recovery and are refused only when committing/testing.
            animate(s,0);row(s,0);s.act(Action.RIGHT);byte[] pending=s.game.encode();s.restoreGame(pending);check(s.game.animation.sequence().mode==1,"unfinished mode recovers");
            refused(()->GameCanvas.restore(pending,blank));s.act(Action.CONFIRM);s.game.focus=8;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR,"moving animation needs movement");s.act(Action.CANCEL);s.act(Action.CANCEL);
            // Configure movement, then connect the animation with ordinary source.
            row(s,1);row(s,4);row(s,0);s.act(Action.RIGHT);s.act(Action.CONFIRM);row(s,s.game.rows().length-2);
            animate(s,0);row(s,0);s.act(Action.RIGHT);s.act(Action.CONFIRM);row(s,4);row(s,8);
            check(s.cart().code().contains("sprite1_x~=sprite1_oldx"),"uses real position change, not just button hold");
            check(s.cart().code().contains("sprite1_clock=(sprite1_clock+1)%"),"bounded timer prevents long-session overflow");
            WorkshopCartridge moving=s.cart();s=session(moving,port);row(s,1);row(s,4);s.restoreGame(s.game.encode());s.act(Action.CANCEL);same(moving.bytes(),s.cart().bytes(),"behavior draft retains animations");
            GameCanvas allocator=new GameCanvas(moving);SpriteRegion free=allocator.allocate(8,8);for(PlayAnimation.Frame f:allocator.play.elements.get(0).animation.frames)check(f.region.x!=free.x||f.region.y!=free.y,"allocator reserves frame references");
            for(String altered:new String[]{moving.code()+"poke(0,1)\n",moving.code().replace("_clock+1","_clock+2"),moving.code().replace("_f0_ticks=6","_f0_ticks=7")}){WorkshopCartridge unknown=PlayModel.code(moving,altered);check(PlayModel.read(unknown)==null,"unfamiliar code refused");same(unknown.bytes(),new GameCanvas(unknown).base.bytes(),"unknown data kept");}
            // Legacy games have no implicit migration on open/write.
            WorkshopCartridge legacy=new WorkshopCartridge(Files.readAllBytes(Paths.get(root,"docs/showcase/0.0.77-en/collection.p8")));same(legacy.bytes(),PlayModel.read(legacy).write(legacy).bytes(),"lab77 exact round trip");
            GameCanvas old=new GameCanvas(legacy);old.screen=GameCanvas.Screen.ACTIONS;old.focus=5;byte[] v2=old.encode();v2=Arrays.copyOf(v2,v2.length-19);java.nio.ByteBuffer.wrap(v2).putInt(2);GameCanvas migrated=GameCanvas.restore(v2,legacy);migrated.act(Action.CONFIRM);check(migrated.screen==GameCanvas.Screen.HOME,"old Back focus migrates");
            GameCanvas current=new GameCanvas(legacy);current.screen=GameCanvas.Screen.ACTIONS;current.focus=5;current=GameCanvas.restore(current.encode(),legacy);current.act(Action.CONFIRM);check(current.screen==GameCanvas.Screen.ANIMATION,"new Animation focus stays Animation");
            // Save errors retain the whole draft and can be retried.
            s=session(moving,port);animate(s,0);row(s,4);byte[] retry=s.game.candidate(moving).bytes();port.fail=true;s.game.focus=8;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.game.animation!=null,"save failure retains draft");port.fail=false;s.act(Action.CANCEL);row(s,8);same(retry,s.cart().bytes(),"retry exact");
            // Empty-looking referenced frames stay reserved. No silent reuse of transparent pixels.
            GameAnimation a=new GameAnimation(legacy,0);while(a.sequence().frames.size()<8)a.duplicate();refused(()->a.duplicate());check(a.sequence().frames.size()==8,"frame cap non-destructive");
            PlayAnimation seq=new PlayAnimation(new SpriteRegion(0,0,8,8));seq.frames.get(0).ticks=2;seq.frames.add(new PlayAnimation.Frame(new SpriteRegion(8,0,8,8),3));
            for(int tick=0;tick<40000;tick++)check(seq.frameAt(tick)==(tick%5<2?0:1),"per-frame boundary and wrap");
        }
        WorkshopCartridge noGfx=new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\nfunction _draw()\n cls(1)\n spr(1,30,30)\nend\n").getBytes("UTF-8"));
        WorkshopSession empty=session(noGfx,new PlayModelTest.Port());animate(empty,0);row(empty,3);empty.game.paint(0,0);empty.act(Action.CANCEL);row(empty,0);empty.restoreGame(empty.game.encode());row(empty,8);check(empty.cart().sheetPixel(8,0)==14,"new gfx section recovers without changing Lua framing");
        WorkshopCartridge puzzle=new WorkshopCartridge(Files.readAllBytes(Paths.get(root,"docs/showcase/0.0.77-en/switches.p8")));
        WorkshopSession toggles=session(puzzle,new PlayModelTest.Port());animate(toggles,0);row(toggles,0);toggles.act(Action.RIGHT);toggles.act(Action.RIGHT);toggles.act(Action.CONFIRM);row(toggles,4);row(toggles,8);
        check(toggles.cart().code().contains("if sprite1_on==1 then"),"switch state animation");row(toggles,1);row(toggles,4);toggles.restoreGame(toggles.game.encode());check(toggles.game.settings.elements.get(0).animation.mode==2,"switch behavior draft recovers animation");
        WorkshopSession corrupt=session(puzzle,new PlayModelTest.Port());animate(corrupt,0);row(corrupt,4);corrupt.game.animation.resources=corrupt.game.animation.resources.withPixel(new SpriteRegion(127,63,1,1),0,0,9);byte[] bad=corrupt.game.encode();refused(()->GameCanvas.restore(bad,puzzle));
        System.out.println("GameAnimationTest: "+checks+" checks passed");
    }
}
