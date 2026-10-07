import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public final class RegionWorkflowTest {
    static int checks;
    static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    interface Call{void run();}
    static void rejects(Call c){try{c.run();throw new AssertionError("invalid region accepted");}catch(IllegalArgumentException expected){checks++;}}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.ISO_8859_1);}
    static class Port implements WorkshopSession.Port{
        int writes,launches;boolean fail;byte[] saved,launched;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");writes++;saved=b;}
        public void launch(byte[] b){launches++;launched=b;}
    }
    static void unchangedOtherSections(P8Document a,P8Document b){
        for(int i=0;i<a.sections().size();i++)if(!a.sections().get(i).name.equals("gfx"))
            check(Arrays.equals(a.body(i),b.body(i)),"Lua, map, audio and opaque data unchanged");
    }
    public static void main(String[] args)throws Exception{
        byte[] original=Files.readAllBytes(Paths.get(args[0]));
        String unusual=new String(original,StandardCharsets.ISO_8859_1).replace("\n","\r\n")
            +"__map__\r\n01234567\r\n__sfx__\r\nuntouched\r\n__future__\r\n\u0081opaque\r\n";
        P8Document doc=P8Document.parse(bytes(unusual));P8Graphics gfx=new P8Graphics(doc);
        SpriteRegion r=new SpriteRegion(40,24,32,24);
        check(!r.sharesMap()&&new SpriteRegion(0,63,8,2).sharesMap(),"shared map boundary is pixel row 64");
        check(gfx.pixel(127,127)==0,"omitted trailing rows are zero");
        check(gfx.withPixel(r,31,23,0)==doc,"painting absent zero never expands source");
        P8Document filled=gfx.withFill(r,0,0,12);P8Graphics fg=new P8Graphics(filled);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)check(fg.pixel(x,y)==(x>=40&&x<72&&y>=24&&y<48?12:gfx.pixel(x,y)),"rectangle fill preserves every outside pixel");
        unchangedOtherSections(doc,filled);
        byte[] oldBody=doc.body(doc.uniqueSection("gfx")),newBody=filled.body(filled.uniqueSection("gfx"));
        check(Arrays.equals(oldBody,Arrays.copyOf(newBody,oldBody.length)),"existing gfx row bytes preserved on extension");
        check(newBody.length==48*130,"only needed full rows added using existing CRLF");
        P8Document line=gfx.withLine(r,0,0,31,23,14);
        check(Arrays.equals(line.bytes(),gfx.withLine(r,31,23,0,0,14).bytes()),"non-square line reversal identical");
        P8Graphics lg=new P8Graphics(line);int count=0;
        for(int y=0;y<24;y++)for(int x=0;x<32;x++)if(lg.pixel(r,x,y)==14)count++;
        check(count==32&&lg.pixel(r,31,23)==14,"line crosses old 16-pixel boundaries");
        // The portable model covers the full sheet and non-tile-aligned regions; UI scope is narrower.
        SpriteRegion corner=new SpriteRegion(127,127,1,1);
        P8Document last=gfx.withPixel(corner,0,0,9);
        check(new P8Graphics(last).pixel(127,127)==9,"full 128x128 domain addressable");
        unchangedOtherSections(doc,last);
        P8Document noEnding=P8Document.parse(Arrays.copyOf(original,original.length-1));
        P8Document expanded=new P8Graphics(noEnding).withPixel(r,0,0,7);
        check(new P8Graphics(expanded).pixel(40,24)==7,"unterminated final row expanded with a separator");
        rejects(()->new SpriteRegion(127,0,2,8));rejects(()->new SpriteRegion(0,0,0,8));
        rejects(()->new SpriteRegion(0,Integer.MAX_VALUE,8,8));rejects(()->gfx.withPixel(r,32,0,1));
        rejects(()->gfx.withFill(r,0,24,1));rejects(()->gfx.withLine(r,0,0,31,23,16));
        WorkshopCartridge cart=new WorkshopCartridge(original);Port p=new Port();WorkshopSession s=new WorkshopSession(cart,p);
        s.switchTool(2);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.REGION,"controller reaches region chooser from sheet");
        s.act(Action.TEST);s.act(Action.NEXT);s.act(Action.UNDO);
        check(p.launches==0&&p.writes==0&&s.mode==Mode.REGION,"selection traps unrelated actions without writing");
        s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);
        for(int i=0;i<3;i++)s.act(Action.RIGHT);s.act(Action.DOWN);s.act(Action.DOWN);
        check(s.regionDraft().width==32&&s.regionDraft().height==24,"controller picks rectangular size in native cells");
        s.act(Action.CONFIRM);check(s.selection().y==16&&s.mode==Mode.CANVAS&&p.writes==0,"confirm selects without moving or resizing pixels");
        s.drawTool=DrawTool.FILL;s.color=12;p.fail=true;s.act(Action.CONFIRM);
        check(s.mode==Mode.ERROR&&Arrays.equals(s.cart().bytes(),original)&&!s.canUndo(),"failed expansion leaves source and undo unchanged");
        p.fail=false;s.act(Action.CANCEL);s.act(Action.CONFIRM); // enter canvas after the error
        s.act(Action.CONFIRM);check(p.writes==1&&s.cart().pixel(s.selection(),31,23)==12,"region fill is one save");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),original),"undo also removes added gfx rows byte for byte");
        s.drawTool=DrawTool.LINE;s.paintAt(0,0);s.paintAt(31,23);
        check(s.cart().pixel(s.selection(),31,23)==12&&!s.pendingLine(),"touch endpoints beyond 16 pixels are retained");
        int writes=p.writes;s.act(Action.ASSIGN_HERO);s.act(Action.COPY_SPRITE);
        check(p.writes==writes&&s.cart().heroSlot()==0,"region editing cannot silently rebind hero or copy a different region");
        check(s.mode==Mode.HERO,"assignment offers a preview without changing bytes");s.act(Action.CANCEL);
        s.act(Action.CANCEL);for(int i=0;i<5;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.zoom,"zoom reachable with D-pad and confirm");
        s.act(Action.REGION);s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CANCEL);s.act(Action.CANCEL);
        check(s.selection().width==32&&p.writes==writes,"cancel new selection retains previous region and bytes");
        s.act(Action.TEST);check(Arrays.equals(p.launched,s.cart().bytes()),"runtime gets expanded standard cart bytes");
        s.act(Action.REGION);for(int i=0;i<20;i++){s.act(Action.RIGHT);s.act(Action.DOWN);}
        s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.region.x==120&&s.region.y==120&&s.region.sharesMap(),"full sheet selection reaches the shared corner");
        s.act(Action.SPRITE_SHEET);s.act(Action.ASSIGN_HERO);
        check(s.region==null,"legacy cards resume their explicit 16x16 bindings");
        System.out.println("RegionWorkflowTest: "+checks+" checks passed");
    }
}
