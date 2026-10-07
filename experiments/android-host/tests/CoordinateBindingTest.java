import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

/** Saved ordinary Lua, field cancellation, copy independence and unsafe-source refusal. */
public final class CoordinateBindingTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("Unsafe binding accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    static WorkshopCartridge cart(String s){return GameUsesTest.cart(s);}
    static String text(WorkshopCartridge c){return new LuaDraft(c,0).text();}
    static class Port implements WorkshopSession.Port {
        byte[] launched;int writes;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");writes++;}
        public void launch(byte[] b){launched=b;}
    }
    static void act(WorkshopSession s,Action... keys){for(Action a:keys)s.act(a);}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String init="function _init()"+nl+" x=12 -- start"+nl+" y=20"+nl+"end"+nl;
            String draws="function _draw()"+nl+" cls(1)"+nl+" sspr(0,0,8,8,x,y) -- keep"+nl+" spr(2,x,44)"+nl+" map(0,0,x,y,1,1)"+nl+"end"+nl;
            WorkshopCartridge original=cart(init+draws);GameUses g=new GameUses(original,2);
            check(g.blocked.isEmpty()&&g.entries.size()==3,"all ordinary coordinate uses reopen");
            check(g.entries.get(0).coordinate(0).equals("x")&&g.entries.get(0).x()==12,"coordinate name and starting position separate");
            check(new GameRules(original).blocked.isEmpty(),"rules stay available with linked draw calls");
            for(int n=0;n<3;n++){g=new GameUses(original,2);g.index=n;g.edit();g.review();check(Arrays.equals(original.bytes(),g.proposal().candidate(original).bytes()),"bound no-op preserves bytes/comments");}
            g=new GameUses(original,2);g.edit();g.field=g.values.length;g.beginField();g.changePlacement(-1);
            check(g.links[0].isEmpty()&&g.x()==12,"unlink explicitly uses known starting position");
            GameUses restored=GameUses.restore(g.encode(),original);restored.cancelField();check(restored.links[0].equals("x"),"recovered field cancel restores link");
            g.finishField();g.review();WorkshopCartridge unlinked=g.proposal().candidate(original);
            check(text(unlinked).equals((init+draws).replace("sspr(0,0,8,8,x,y)","sspr(0,0,8,8,12,y)")),"unlink changes only one argument");GameUsesTest.resources(original,unlinked);
            g=new GameUses(original,2);g.index=1;g.duplicate();g.links[0]="y";g.review();WorkshopCartridge duplicate=g.proposal().candidate(original);
            check(text(duplicate).contains("spr(2,y,44)")&&text(duplicate).contains("spr(2,x,44)"),"independent duplicate binding");
            g=new GameUses(duplicate,2);g.index=3;g.delete();check(Arrays.equals(g.proposal().candidate(duplicate).bytes(),original.bytes()),"remove duplicate exact");
            final GameUses bound=new GameUses(original,2);bound.edit();bound.field=4;refused(()->bound.adjust(1));refused(bound::animateSelected);
            GameRules values=new GameRules(original);refused(values::remove);check(Arrays.equals(original.bytes(),values.base.bytes()),"linked starting value deletion preserves source");
            GameSketch sketch=new GameSketch(original,0);check(sketch.dynamic&&sketch.pixels[20*128+12]==1,"sketch marks live coordinates and shows their initial location");
            // Test the sprite alone so a later map draw cannot conceal it.
            GameSketch sprite=new GameSketch(cart(init+"function _draw()"+nl+" cls(1)"+nl+" sspr(0,0,8,8,x,y)"+nl+"end"+nl),0);
            check(sprite.dynamic&&sprite.pixels[20*128+12]==1&&sprite.pixels[20*128+13]==2,"starting sketch samples actual sprite data");
            g=new GameUses(original,3);g.addMap(0,0);g.links=new String[]{"x","y"};g.review();WorkshopCartridge added=g.proposal().candidate(original);check(text(added).contains("map(0,0,x,y,16,16)"),"map binding is genre neutral");
            GameUsesTest.resources(original,added);
        }
        for(String prefix:new String[]{"", "local a,x=0,0\n", "function _init()\n x=rnd(10)\nend\n", "function _init()\n x=0.5\nend\n", "function _init()\n x=0\n x=1\nend\n", "function _init()\n x=0\nend\nfunction other(x)\nend\n", "#include state.lua\n"}){
            GameUses g=new GameUses(cart(prefix+"function _draw()\n spr(1,x,0)\nend\n"),0);
            check(!g.blocked.isEmpty(),"unrecognized, local, included or fractional source is preserved");refused(()->g.addSprite(new SpriteRegion(0,0,8,8)));
        }
        String init="function _init()\n x=24\n y=64\nend\n";
        GameUses expression=new GameUses(cart(init+"function _draw()\n spr(1,x+1,0)\nend\n"),0);check(!expression.blocked.isEmpty(),"arbitrary expression never simplified");
        GameUses wrongArgument=new GameUses(cart(init+"function _draw()\n sspr(x,0,8,8,0,0)\nend\n"),0);check(!wrongArgument.blocked.isEmpty(),"only destination coordinates bind");
        WorkshopCartridge base=cart(init+"-- blank graphics\n");Port port=new Port();WorkshopSession s=new WorkshopSession(base,port);s.openUses(true);
        s.uses.field=4;act(s,Action.RIGHT,Action.NEXT);check(s.uses.values[4]==60,"navigation cannot edit coordinates");
        act(s,Action.CONFIRM,Action.RIGHT,Action.DOWN);check(s.uses.values[4]==61&&s.uses.field==4,"explicit edit locks field selection");
        s.restoreUses(s.uses.encode());s.act(Action.CANCEL);check(s.uses.values[4]==60,"field rollback survives restart");
        s.uses.field=6;act(s,Action.CONFIRM,Action.RIGHT,Action.CONFIRM);s.uses.field=7;act(s,Action.CONFIRM,Action.RIGHT,Action.RIGHT,Action.CONFIRM);
        check(s.uses.links[0].equals("x")&&s.uses.links[1].equals("y"),"controller connects both axes");
        byte[] saved=s.cart().bytes(),journal=s.uses.encode();s.act(Action.TEST);
        check(port.writes==0&&Arrays.equals(saved,s.cart().bytes())&&text(new WorkshopCartridge(port.launched)).contains("sspr(0,0,16,16,x,y)"),"draft Test launches links without saving");
        check(Arrays.equals(journal,s.uses.encode()),"Test preserves exact draft context");
        act(s,Action.MENU);GameUses recoveredReview=GameUses.restore(s.uses.encode(),s.cart());check(Arrays.equals(recoveredReview.proposal().candidate(s.cart()).bytes(),port.launched),"review recovery retains binding-field focus and exact candidate");port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.undoCount()==0,"write failure retains draft/history");port.fail=false;act(s,Action.CONFIRM,Action.CONFIRM);
        check(port.writes==1&&s.undoCount()==1&&s.uses.entries.get(0).linked(),"Apply makes one linked edit");byte[] committed=s.cart().bytes();
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),base.bytes()),"Undo returns exact blank");s.act(Action.REDO);check(Arrays.equals(s.cart().bytes(),committed),"Redo restores bindings");
        GameUses reopen=new GameUses(new WorkshopCartridge(committed),0);reopen.edit();check(reopen.links[0].equals("x")&&reopen.links[1].equals("y"),"reopen needs no metadata");
        reopen.links[0]="missing";refused(reopen::proposal);refused(()->GameUses.restore(reopen.encode(),reopen.base));
        final byte[] valid=s.uses.encode();refused(()->GameUses.restore(valid,cart("-- changed externally\n")));
        System.out.println("CoordinateBindingTest: "+checks+" checks passed");
    }
}
