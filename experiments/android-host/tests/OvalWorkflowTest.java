import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class OvalWorkflowTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes;boolean fail;byte[] saved,launched;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");writes++;saved=bytes.clone();}
        public void launch(byte[] bytes){launched=bytes.clone();}
    }
    static void brush(WorkshopSession s,DrawTool tool){
        s.act(Action.DRAW_TOOLS);
        while(s.drawToolCursor<WorkshopSession.drawMenuIndex(tool))s.act(Action.DOWN);
        s.act(Action.CONFIRM);check(s.drawTool==tool&&s.mode==Mode.CANVAS,"controller reaches oval at end of scrolling menu");
    }
    static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b&255));return out.toString();}
    public static void main(String[] args)throws Exception{
        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[1])));
        SpriteRegion sheet=new SpriteRegion(0,0,128,64);MessageDigest sha=MessageDigest.getInstance("SHA-256");int samples=0;
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.US_ASCII)){
            if(line.startsWith("#"))continue;
            String[] fields=line.split(" "),key=fields[0].split(",");
            boolean filled=key[0].equals("1"),reverse=key[3].equals("1");
            int w=Integer.parseInt(key[1]),h=Integer.parseInt(key[2]),left=reverse?128-w:0,top=reverse?64-h:0;
            int x0=reverse?left+w-1:left,y0=reverse?top+h-1:top,x1=reverse?left:left+w-1,y1=reverse?top:top+h-1;
            WorkshopCartridge result=blank.withOval(sheet,x0,y0,x1,y1,10,filled);
            byte[] mask=new byte[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)mask[y*w+x]=(byte)(result.sheetPixel(left+x,top+y)==10?'1':'0');
            check(hex(sha.digest(mask)).equals(fields[1]),"official PICO-8 oval mask "+fields[0]);samples++;
        }
        check(samples==5616,"complete oracle corpus, no truncated capture");
        for(int f=1;f<args.length;f++){
            String text=new String(Files.readAllBytes(Paths.get(args[f])),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(text.getBytes(StandardCharsets.ISO_8859_1));
            SpriteRegion region=new SpriteRegion(8,16,32,24);
            for(DrawTool tool:new DrawTool[]{DrawTool.OVAL,DrawTool.FILLED_OVAL}){
                boolean filled=tool==DrawTool.FILLED_OVAL;Port p=new Port();WorkshopSession s=new WorkshopSession(base,p);
                s.openSprite(0);s.region=region;s.zoom=true;brush(s,tool);s.cursorX=1;s.cursorY=2;s.color=10;s.act(Action.CONFIRM);
                for(int x=0;x<28;x++)s.act(Action.RIGHT);for(int y=0;y<19;y++)s.act(Action.DOWN);
                check(s.pendingStroke()&&p.writes==0,"first corner and movement only preview");byte[] expected=s.canvasPreview().bytes();
                for(Action a:new Action[]{Action.MENU,Action.NEXT,Action.REDO,Action.CONTEXT,Action.REGION,Action.COPY_SPRITE,Action.ASSETS,Action.DRAW_TOOLS})s.act(a);
                s.switchTool(0);s.openSprite(1);check(s.tool==2&&s.region==region&&s.zoom&&s.pendingStroke(),"draft traps navigation and history");
                same(expected,s.canvasPreview().bytes(),"draft preserved after blocked actions");same(base.bytes(),s.cart().bytes(),"canonical bytes remain saved");
                s.act(Action.CANCEL);check(!s.pendingStroke()&&!s.canUndo(),"cancel adds no history");
                s.restoreStroke(1,2);same(expected,s.canvasPreview().bytes(),"restored oval preview exact");
                p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.pendingStroke()&&!s.canUndo(),"failed save keeps oval draft");
                same(base.bytes(),s.cart().bytes(),"failed save never publishes");p.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);
                check(!s.pendingStroke()&&p.writes==1,"Start commits once");same(expected,p.saved,"saved preview exact");same(p.saved,p.launched,"runtime gets saved oval");
                P8Document before=P8Document.parse(base.bytes()),after=P8Document.parse(p.saved);
                for(int i=0;i<before.sections().size();i++)if(!before.sections().get(i).name.equals("gfx"))same(before.body(i),after.body(i),"unrelated and unknown sections unchanged");
                for(int y=0;y<128;y++)for(int x=0;x<128;x++)if(x<9||x>37||y<18||y>37)
                    check(base.sheetPixel(x,y)==s.cart().sheetPixel(x,y),"outside bounds and shared map half intact");
                s.act(Action.UNDO);same(base.bytes(),s.cart().bytes(),"whole oval undo exact");s.act(Action.REDO);same(expected,s.cart().bytes(),"whole oval redo exact");
                s.act(Action.UNDO);s.paintAt(29,21);s.paintAt(1,2);same(expected,s.cart().bytes(),"touch reversed corners match controller");
                s.act(Action.UNDO);s.restoreStroke(1,2);s.act(Action.UNDO);check(!s.pendingStroke()&&s.canRedo(),"Y cancels draft without losing redo");
                s.act(Action.REDO);same(expected,s.cart().bytes(),"redo survives cancelled oval");
                s.act(Action.DRAW_TOOLS);s.chooseDrawTool(4);s.paintAt(15,2);check(s.drawTool==tool,"picker returns to oval");
                s.region=new SpriteRegion(0,56,16,16);s.restoreStroke(1,2);check(!s.pendingStroke(),"shared-map guard applies to oval recovery");
            }
        }
        for(boolean filled:new boolean[]{false,true})same(blank.bytes(),blank.withOval(sheet,0,0,127,63,0,filled).bytes(),"zero no-op preserves absent gfx rows");
        for(int[] invalid:new int[][]{{-1,0,1,1,10},{0,0,128,1,10},{0,0,1,64,10},{0,0,1,1,16}}){
            boolean rejected=false;try{blank.withOval(sheet,invalid[0],invalid[1],invalid[2],invalid[3],invalid[4],false);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid input rejected before write");
        }
        System.out.println("OvalWorkflowTest: "+checks+" checks passed; "+samples+" official runtime masks");
    }
}
