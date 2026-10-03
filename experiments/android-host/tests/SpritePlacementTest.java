import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class SpritePlacementTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void refused(Runnable action){try{action.run();throw new AssertionError("accepted unsafe state");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge cart(){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\nfunction _draw()\r\n cls(0)\r\n -- chosen line\r\nend\r\n__future__\r\nopaque\r\n").getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        int saves,launches;byte[] saved;
        public void save(byte[] bytes){saves++;saved=bytes;}
        public void launch(byte[] bytes){launches++;check(Arrays.equals(saved,bytes),"launch committed bytes");}
    }
    public static void main(String[] args){
        WorkshopCartridge original=cart().withPixel(new SpriteRegion(8,16,24,32),2,3,14);
        byte[] bytes=original.bytes();LuaDraft d=new LuaDraft(original,2);String initial=d.text();
        d.beginSprite(new SpriteRegion(8,16,24,32));SpritePlacement p=d.placement;
        for(int phase=0;phase<4;phase++){
            check(!d.dirty()&&d.text().equals(initial),"proposal never edits");
            LuaDraft recovered=LuaDraft.restore(d.encode());
            check(recovered.panel==LuaDraft.Panel.SPRITE&&recovered.placement.phase==phase,"recover each stage");
            check(recovered.placement.code().equals(p.code()),"recover region and position");p.next();
        }
        check(p.code().equals("sspr(8,16,24,32,52,48)"),"pixel dimensions, not tile counts");
        check(p.pixel(original,54,51)==14&&p.pixel(original,52,48)==0&&p.pixel(original,0,0)==0,"default pixel preview");
        d.applySprite();
        check(d.text().equals(initial.replace(" -- chosen line"," sspr(8,16,24,32,52,48)\r\n -- chosen line")),"one indented CRLF insertion, other source exact");
        WorkshopCartridge candidate=d.edit().candidate(original);
        check(candidate.sheetPixel(10,19)==14&&!candidate.hasHero(),"hero-free use retains graphics");
        String all=new String(candidate.bytes(),StandardCharsets.UTF_8),old=new String(bytes,StandardCharsets.UTF_8);
        check(all.substring(all.indexOf("__future__")).equals(old.substring(old.indexOf("__future__"))),"resources and unknown sections byte-exact");
        d.history(false);check(d.text().equals(initial),"one undo removes whole use");d.history(true);check(d.text().contains("sspr("),"redo");
        d.point(2,0);d.beginParameters();check(d.insertion.selected==18,"existing sspr fields");
        d.insertion.set(4,"x+1");d.applyInsert();check(d.text().contains(",x+1,48)"),"existing use editable as ordinary Lua expression");
        d.beginSprite(new SpriteRegion(0,64,128,64));d.cancelSprite();check(d.panel==LuaDraft.Panel.CURSOR,"cancel returns to source");
        check(Arrays.equals(bytes,original.bytes()),"source cart untouched");
        SpritePlacement edge=new SpritePlacement(new SpriteRegion(0,0,8,8));edge.phase=1;edge.move(100,100);
        check(edge.source().width==128&&edge.source().height==128&&edge.source().sharesMap(),"full sheet including shared lower half");
        edge.phase=2;edge.move(-100,-100);check(edge.x==-127&&edge.y==-127,"bounded offscreen positioning");
        edge.toggleStep();edge.move(1,1);check(edge.x==-126&&edge.y==-126,"fine step");
        check(edge.pixel(original,127,127)==0,"offscreen clipping preview");
        edge.point(24,80);check(edge.x==24&&edge.y==80,"touch placement");
        edge.phase=3;edge.point(0,0);check(edge.x==24&&edge.y==80,"review locks touch placement");
        SpritePlacement corners=new SpritePlacement(new SpriteRegion(0,0,16,16));corners.point(30,40);corners.next();corners.point(10,20);
        check(corners.source().x==10&&corners.source().y==20&&corners.source().width==21&&corners.source().height==21,"reverse corners and non-tile region");
        check(!corners.back()&&corners.back(),"back steps then cancels");
        LuaDraft selected=new LuaDraft(original,2);selected.selectAll();refused(()->selected.beginSprite(new SpriteRegion(0,0,8,8)));
        LuaDraft bad=new LuaDraft(new WorkshopCartridge("pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n--[[\ntext\n]]\n".getBytes(StandardCharsets.UTF_8)),1);
        refused(()->bad.beginSprite(new SpriteRegion(0,0,8,8)));
        LuaDraft corrupt=new LuaDraft(original,2);corrupt.beginSprite(new SpriteRegion(0,0,8,8));byte[] journal=corrupt.encode();journal[journal.length-1]=0;refused(()->LuaDraft.restore(journal));
        Port port=new Port();WorkshopSession session=new WorkshopSession(original,port);session.switchTool(1);session.codeLine=2;session.act(Action.CONFIRM);
        session.act(Action.CONTEXT);session.codeDraft.insertion.choose(18);session.act(Action.CONFIRM);
        check(session.codeDraft.placement!=null,"catalog opens visual placement");
        session.act(Action.TEST);session.act(Action.UNDO);session.codeCommand(1);
        check(port.saves==0&&port.launches==0&&!session.codeDraft.dirty(),"shortcuts cannot bypass review");
        session.act(Action.CONFIRM);session.act(Action.CONFIRM);session.act(Action.RIGHT);session.act(Action.CONFIRM);session.act(Action.CONFIRM);
        check(session.codeDraft.dirty()&&port.saves==0,"explicit insertion is draft-only");
        session.act(Action.UNDO);check(!session.codeDraft.dirty(),"controller undo");session.act(Action.REDO);session.act(Action.TEST);
        check(port.saves==1&&port.launches==1,"save then Test");
        System.out.println("SpritePlacementTest: "+checks+" checks passed");
    }
}
