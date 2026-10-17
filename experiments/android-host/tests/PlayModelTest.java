import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** Connected authoring, persistence and unsafe-source regression checks. */
public final class PlayModelTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;}
        public void launch(byte[] b){launched=b;}
    }
    static void row(WorkshopSession s,int n){s.game.focus=n;s.act(Action.CONFIRM);}
    static void field(WorkshopSession s,int n,int times){int at=0;while(s.game.formFields()[at]!=n)at++;row(s,at);for(int i=0;i<Math.abs(times);i++)s.act(times>0?Action.RIGHT:Action.LEFT);s.act(Action.CONFIRM);}
    static WorkshopSession session(WorkshopCartridge c,Port p){WorkshopSession s=new WorkshopSession(c,p);s.openGame();return s;}
    static void sprite(WorkshopSession s,int x,int y){row(s,0);row(s,0);s.game.paint(0,0);s.act(Action.CANCEL);row(s,0);s.game.point(x,y);s.act(Action.CONFIRM);}
    static void behavior(WorkshopSession s,int n){row(s,n+1);row(s,4);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge blank=GameCanvasTest.cart("-- function is a comment\nfunction _draw()\n cls(1)\nend\n",nl);
            Port p=new Port();WorkshopSession s=session(blank,p);sprite(s,20,50);sprite(s,90,50);
            WorkshopCartridge staticCart=s.cart();
            GameCanvas legacy=new GameCanvas(staticCart);legacy.focus=legacy.uses.entries.size()+1;byte[] v1=legacy.encode();v1=Arrays.copyOf(v1,v1.length-1);java.nio.ByteBuffer.wrap(v1).putInt(1);GameCanvas migrated=GameCanvas.restore(v1,staticCart);check(migrated.act(Action.CONFIRM)==GameCanvas.Command.TOOLS,"v1 All tools focus migrates");
            legacy.screen=GameCanvas.Screen.ACTIONS;legacy.focus=4;v1=legacy.encode();v1=Arrays.copyOf(v1,v1.length-1);java.nio.ByteBuffer.wrap(v1).putInt(1);migrated=GameCanvas.restore(v1,staticCart);migrated.act(Action.CONFIRM);check(migrated.screen==GameCanvas.Screen.HOME,"v1 Back focus migrates");
            behavior(s,0);field(s,0,1);field(s,1,1);
            row(s,1);s.act(Action.RIGHT);byte[] journal=s.game.encode();
            WorkshopSession recovered=session(staticCart,p);recovered.restoreGame(journal);check(recovered.game.editingField,"field editing recovers");recovered.act(Action.CANCEL);
            check(recovered.game.settings.elements.get(0).speed==2,"recovered field revert");s=recovered;
            s.act(Action.TEST);same(staticCart.bytes(),s.cart().bytes(),"draft Test does not save");check(PlayModel.read(new WorkshopCartridge(p.launched)).elements.get(0).movement==1,"Test has behavior");
            p.fail=true;row(s,s.game.rows().length-2);check(s.mode==Mode.ERROR&&s.game.draft(),"save failure retains behavior");p.fail=false;s.act(Action.CANCEL);row(s,s.game.rows().length-2);
            check(s.mode==Mode.GAME&&s.game.play!=null,"behavior saved and recognized");same(staticCart.bytes(),PlayModel.code(s.cart(),staticCart.code()).bytes(),"behavior only changes code");
            check(s.cart().code().startsWith("-- function is a comment"),"leading comment preserved");
            WorkshopCartridge moved=s.cart();s.act(Action.UNDO);same(staticCart.bytes(),s.cart().bytes(),"one Undo");s.act(Action.REDO);same(moved.bytes(),s.cart().bytes(),"one Redo");
            behavior(s,1);field(s,2,1);row(s,s.game.rows().length-2);row(s,s.game.uses.entries.size()+1);field(s,0,1);row(s,2);
            PlayModel collect=PlayModel.read(s.cart());check(collect!=null&&collect.goal==1&&collect.elements.get(1).target==0,"collect goal and target reopen");
            check(collect.source().contains("sprite2_visible=0")&&collect.source().contains("game_score+=1"),"one-shot collect uses ordinary state");
            WorkshopCartridge complete=s.cart();s=session(new WorkshopCartridge(complete.bytes()),p);check(s.game.blocked.isEmpty(),"reopen without metadata");
            row(s,1);row(s,1);s.game.point(30,60);s.act(Action.CONFIRM);check(s.game.play.elements.get(0).x==30&&s.game.play.elements.get(0).movement==1,"move preserves behavior");
            row(s,2);row(s,2);s.game.point(100,30);s.act(Action.CONFIRM);check(s.game.play.elements.size()==3&&s.game.play.elements.get(2).rule==1,"duplicate keeps independent rule state");
            row(s,1);row(s,3);row(s,1);check(s.mode==Mode.ERROR&&s.game.uses.entries.size()==3,"referenced target removal refused");s.act(Action.CANCEL);s.act(Action.CANCEL);
            row(s,3);row(s,0);row(s,1);s.game.color=12;s.game.paint(0,0);s.act(Action.CANCEL);row(s,0);
            check(s.game.play.elements.get(2).rule==1&&s.game.region(2).x!=s.game.region(1).x,"independent appearance preserves rule");
            row(s,3);row(s,3);row(s,1);check(s.game.play.elements.size()==2,"unreferenced item removable");
            sprite(s,40,80);check(s.game.play.elements.size()==3&&s.game.play.elements.get(2).rule==0,"add after behavior preserves existing rules");
            behavior(s,2);field(s,2,4);field(s,4,1);row(s,s.game.rows().length-2);check(s.game.play.elements.get(2).on==1,"switch starts on");
            for(int n=0;n<2;n++){behavior(s,n);field(s,2,4);row(s,s.game.rows().length-2);}
            row(s,4);field(s,0,1);row(s,2);check(s.game.play.goal==2,"switch goal");
            behavior(s,0);field(s,5,2);row(s,s.game.rows().length-2);
            check(s.game.play.elements.get(0).neighbor==1,"linked switch saved");
            byte[] linkSource=s.cart().bytes();behavior(s,1);field(s,2,-4);row(s,s.game.rows().length-2);check(s.mode==Mode.ERROR,"linked switch cannot silently stop being a switch");s.act(Action.CANCEL);s.act(Action.CANCEL);same(linkSource,s.cart().bytes(),"refused switch edit preserves source");
            WorkshopCartridge puzzle=s.cart();same(puzzle.bytes(),s.game.play.write(puzzle).bytes(),"canonical write exact");
            check(puzzle.code().contains("local pressed4=btn(4) and not game_last4")&&!puzzle.code().contains("btnp("),"no held-button auto-repeat");
            check(puzzle.code().indexOf("if pressed5")<puzzle.code().indexOf("if game_result~=0"),"restart available after outcome");
            for(String changed:new String[]{puzzle.code()+"poke(0,1)\n",puzzle.code().replace("local pressed4=", "local other4="),puzzle.code().replace("sprite1_visible=1","sprite1_visible=0")}){
                WorkshopCartridge unfamiliar=PlayModel.code(puzzle,changed);GameCanvas safe=new GameCanvas(unfamiliar);check(!safe.blocked.isEmpty(),"modified Lua not silently rewritten");refused(()->safe.allocate(8,8));same(unfamiliar.bytes(),safe.base.bytes(),"unfamiliar source preserved");
            }
            byte[] stale=s.game.encode();refused(()->GameCanvas.restore(stale,staticCart));
            GameCanvas limit=new GameCanvas(puzzle);PlayModel sixteen=limit.play.copy();while(sixteen.elements.size()<16)sixteen.elements.add(sixteen.elements.get(2).copy());GameCanvas full=new GameCanvas(sixteen.write(puzzle));refused(()->full.add(0));
            WorkshopCartridge comments=GameCanvasTest.cart("-- leading\nfunction _draw()\n cls(1) -- background\n --[[multi\nline]]\nend\n",nl);String kept=PlayModel.fromStatic(comments).source();check(kept.contains("-- background")&&kept.contains("--[[multi"),"conversion keeps inline and block comments");
            WorkshopCartridge fake=GameCanvasTest.cart("--[[function _init()\n game_count=99\nend]]\nfunction _draw()\n cls(1)\nend\n",nl);WorkshopCartridge ordinary=PlayModel.fromStatic(fake).write(fake);check(PlayModel.read(ordinary)!=null,"fake header in comment is not treated as source");
            WorkshopSession one=session(blank,new Port());sprite(one,5,5);behavior(one,0);field(one,2,1);
            GameCanvas invalid=GameCanvas.restore(one.game.encode(),one.cart());check(invalid.settings.elements.get(0).rule==1,"unfinished missing target recovers");refused(()->invalid.candidate(one.cart()));
            one.act(Action.CANCEL);check(one.game.play==null,"discard invalid behavior preserves static source");
            WorkshopSession empty=session(blank,new Port());row(empty,1);field(empty,1,2);byte[] ej=empty.game.encode();empty.restoreGame(ej);row(empty,2);check(empty.game.play.background==3,"empty game settings recover and save");sprite(empty,10,10);check(empty.game.play.elements.size()==1,"add to empty configured game");
            // Removing an earlier unreferenced sprite rebinds target indices, not visual identity.
            PlayModel rebind=PlayModel.fromStatic(staticCart);rebind.elements.add(rebind.elements.get(1).copy());rebind.elements.get(2).rule=1;rebind.elements.get(2).target=1;
            GameCanvas deletion=new GameCanvas(rebind.write(staticCart));deletion.selected=0;deletion.screen=GameCanvas.Screen.DELETE;
            PlayModel after=PlayModel.read(deletion.candidate(deletion.base));check(after.elements.get(1).target==0,"deletion remaps target");
        }
        System.out.println("PlayModelTest: "+checks+" checks passed");
    }
}
