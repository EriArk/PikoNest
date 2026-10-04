import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class GameUsesTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe mutation");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    static WorkshopCartridge cart(String source){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n"+source+"__gfx__\n1234"+new String(new char[124]).replace('\0','0')+"\n__sfx__\nunchanged-audio\n").getBytes(StandardCharsets.UTF_8));}
    static String text(WorkshopCartridge cart){return new LuaDraft(cart,0).text();}
    static void resources(WorkshopCartridge before,WorkshopCartridge after){
        String a=new String(before.bytes(),StandardCharsets.UTF_8),b=new String(after.bytes(),StandardCharsets.UTF_8);check(a.substring(a.indexOf("__gfx__")).equals(b.substring(b.indexOf("__gfx__"))),"resource bytes preserved");
    }
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String source="-- outside: _draw() untouched"+nl+"function _update()"+nl+" score+=1"+nl+"end"+nl+"function _draw()"+nl+" cls(1)"+nl+" sspr(0,0,8,8,12,20) -- keep comment"+nl+" spr(2,45,40)"+nl+"end"+nl+"-- tail"+nl;
            WorkshopCartridge original=cart(source);GameUses g=new GameUses(original,2);check(g.blocked.isEmpty(),"simple draw "+g.blocked);check(g.entries.size()==2,"two uses");
            g.index=1;g.edit();g.field=1;g.adjust(7);g.review();GameUses recovered=GameUses.restore(g.encode(),original);check(recovered.values[1]==52&&recovered.screen==GameUses.Screen.REVIEW,"review recovery");
            WorkshopCartridge edited=recovered.proposal().candidate(original);check(text(edited).equals(source.replace("spr(2,45,40)","spr(2,52,40)")),"only selected args");resources(original,edited);
            g=new GameUses(edited,2);g.index=1;g.duplicate();g.field=2;g.adjust(8);g.review();WorkshopCartridge copied=g.proposal().candidate(edited);
            check(new GameUses(copied,2).entries.size()==3,"duplicate");check(text(copied).contains("spr(2,52,48)"),"independent position");resources(edited,copied);
            g=new GameUses(copied,2);g.index=2;g.delete();check(Arrays.equals(g.proposal().candidate(copied).bytes(),edited.bytes()),"delete duplicate exact undo outcome");
            g=new GameUses(original,2);g.addSprite(new SpriteRegion(8,16,24,16));g.pick();g.picker.next();GameUses pick=GameUses.restore(g.encode(),original);check(pick.picker.phase==1,"picker recovery");pick.picked();check(pick.region().width==24,"region retained");g.back();g.back();check(Arrays.equals(original.bytes(),g.base.bytes()),"cancel no writes");
            g=new GameUses(original,3);g.addMap(4,5);g.field=4;g.adjust(2);g.review();WorkshopCartridge mapped=g.proposal().candidate(original);check(text(mapped).contains("map(4,5,0,0,18,16)"),"map dimensions and position");resources(original,mapped);
            final GameUses stale=g;refused(()->stale.proposal().candidate(edited));refused(()->GameUses.restore(stale.encode(),edited));
        }
        WorkshopCartridge noDraw=cart("score=0\n");GameUses g=new GameUses(noDraw,2);g.addSprite(new SpriteRegion(0,0,8,8));g.review();check(text(g.proposal().candidate(noDraw)).contains("function _draw()\n cls(1)\n sspr"),"new callback only when absent");
        String[] bad={
            "function _draw()\n if true then\n spr(1,0,0)\n end\nend\n",
            "function outer()\nfunction _draw()\ncls(1)\nend\nend\n",
            "function _draw()\n cls(1)\n spr(1,x,0)\nend\n",
            "function _draw()\n camera(10,10)\n spr(1,0,0)\nend\n",
            "function _draw()\n spr(1,0,0)\n cls(1)\nend\n",
            "function _draw() cls(1) end\n",
            "_draw=function()\n cls(1)\nend\n",
            "function _draw()\n cls(1)\nend\n_draw=other\n",
            "function _draw()\n cls(1)\n for i=1,2 do spr(i,0,0) end\nend\n",
            "function _draw()\n cls(1)\n --[[ open\n",
            "local spr=function(a,b,c) end\nfunction _draw()\n spr(1,0,0)\nend\n"
        };
        for(String source:bad){GameUses blocked=new GameUses(cart(source),2);check(!blocked.blocked.isEmpty(),"unsafe source blocked: "+source);refused(()->blocked.addMap(0,0));}
        GameUses shadow=new GameUses(cart("sspr=function() end\n"),2);refused(()->shadow.addSprite(new SpriteRegion(0,0,8,8)));
        GameUses limits=new GameUses(noDraw,3);limits.addMap(127,63);limits.field=4;limits.adjust(1);check(limits.values[4]==1,"map bounds");limits.field=0;limits.adjust(1);check(limits.values[0]==127,"map origin bounds");
        final GameUses recovery=limits;byte[] truncated=Arrays.copyOf(limits.encode(),10);refused(()->GameUses.restore(truncated,noDraw));byte[] extra=Arrays.copyOf(limits.encode(),limits.encode().length+1);refused(()->GameUses.restore(extra,noDraw));
        class Port implements WorkshopSession.Port {boolean fail;byte[] saved,launched;public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;}public void launch(byte[] b){launched=b;}}
        Port port=new Port();WorkshopSession s=new WorkshopSession(cart("function _draw()\n cls(1)\nend\n"),port);s.switchTool(2);s.openUses(true);
        check(s.mode==WorkshopSession.Mode.USES&&s.codeDraft==null,"no Lua UI");s.act(Action.CONFIRM);check(port.saved==null,"review not saved");byte[] journal=s.uses.encode();
        port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&!s.canUndo(),"write failure keeps history");s.act(Action.CANCEL);check(s.mode==WorkshopSession.Mode.USES&&s.uses.screen==GameUses.Screen.REVIEW,"retry returns to review");
        port.fail=false;s.act(Action.CONFIRM);check(s.uses.entries.size()==1&&s.canUndo(),"commit saves one");check(s.codeDraft==null,"commit never opens code");resources(noDraw,s.cart());
        s.act(Action.TEST);check(Arrays.equals(port.launched,s.cart().bytes())&&s.mode==WorkshopSession.Mode.USES,"test saved bytes and list remains");
        s.act(Action.UNDO);check(s.uses.entries.isEmpty()&&s.canRedo(),"undo from list");s.act(Action.REDO);check(s.uses.entries.size()==1,"redo from list");
        port.fail=true;s.act(Action.UNDO);check(s.mode==WorkshopSession.Mode.ERROR&&s.uses.entries.size()==1&&s.canUndo(),"failed undo preserves state");s.act(Action.CANCEL);check(s.mode==WorkshopSession.Mode.USES,"failed undo returns list");port.fail=false;
        WorkshopSession reboot=new WorkshopSession(s.cart(),port);reboot.restoreUses(s.uses.encode());check(reboot.mode==WorkshopSession.Mode.USES&&reboot.codeDraft==null,"list recovery without Lua");
        reboot.act(Action.NEXT);reboot.act(Action.CONFIRM);reboot.act(Action.CANCEL);reboot.act(Action.CANCEL);check(reboot.uses.entries.size()==1,"cancel new no duplicate");
        s.act(Action.MENU);s.uses.menu=3;s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.uses.entries.isEmpty(),"controller deletion");s.act(Action.UNDO);check(s.uses.entries.size()==1,"delete undo");
        System.out.println("GameUsesTest: "+checks+" checks passed");
    }
}
