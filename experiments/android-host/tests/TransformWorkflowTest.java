import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TransformWorkflowTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;byte[] saved;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");writes++;saved=bytes.clone();}
        public void launch(byte[] bytes){launches++;same(saved,bytes,"launch uses committed pixels");}
    }
    static WorkshopSession editor(WorkshopCartridge cart,SpriteRegion r,Port p){
        WorkshopSession s=new WorkshopSession(cart,p);s.openSprite(0);s.region=r;s.mode=Mode.CANVAS;return s;
    }
    static void begin(WorkshopSession s){s.act(Action.DRAW_TOOLS);s.chooseDrawTool(5);check(s.mode==Mode.TRANSFORM,"entry through tools");}
    static void verifyPixels(WorkshopCartridge before,WorkshopCartridge after,SpriteRegion r,SpriteTransform op){
        // Map each original pixel to its independent expected destination.
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
            int dx=x,dy=y;
            if(op==SpriteTransform.FLIP_HORIZONTAL||op==SpriteTransform.ROTATE_HALF)dx=r.width-1-x;
            if(op==SpriteTransform.FLIP_VERTICAL||op==SpriteTransform.ROTATE_HALF)dy=r.height-1-y;
            if(op==SpriteTransform.ROTATE_CLOCKWISE){dx=r.height-1-y;dy=x;}
            check(before.pixel(r,x,y)==after.pixel(r,dx,dy),"each pixel reaches its expected destination");
        }
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)if(x<r.x||x>=r.x+r.width||y<r.y||y>=r.y+r.height)
            check(before.sheetPixel(x,y)==after.sheetPixel(x,y),"outside selection including shared map stays untouched");
        P8Document a=P8Document.parse(before.bytes()),b=P8Document.parse(after.bytes());
        for(int i=0;i<a.sections().size();i++)if(!a.sections().get(i).name.equals("gfx"))same(a.body(i),b.body(i),"other sections byte exact");
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            String source=new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(source.getBytes(StandardCharsets.ISO_8859_1));
            for(SpriteRegion r:new SpriteRegion[]{new SpriteRegion(8,8,24,24),new SpriteRegion(64,32,32,24),new SpriteRegion(0,0,128,64)}){
                WorkshopCartridge original=base.withPixel(r,0,0,8).withPixel(r,1,0,12).withPixel(r,r.width-1,r.height-1,10);
                for(SpriteTransform op:SpriteTransform.values()){
                    Port p=new Port();WorkshopSession s=editor(original,r,p);begin(s);s.chooseTransform(op.ordinal());
                    for(Action action:new Action[]{Action.UNDO,Action.NEXT,Action.MENU,Action.REGION,Action.ASSETS,Action.CONTEXT,Action.SPRITE_SHEET})s.act(action);
                    s.switchTool(0);s.openSprite(1);
                    check(s.mode==Mode.TRANSFORM&&s.tool==2&&s.selection()==r&&p.writes==0&&p.launches==0,"draft traps controller and touch escapes");
                    same(original.bytes(),s.cart().bytes(),"preview does not write");
                    if(!op.supports(r)){
                        s.act(Action.CONFIRM);check(s.mode==Mode.TRANSFORM&&p.writes==0,"rectangular quarter turn stays uncommitted");
                        boolean rejected=false;try{original.transformed(r,op);}catch(IllegalArgumentException e){rejected=true;}
                        check(rejected,"core rejects unsafe footprint");s.act(Action.CANCEL);continue;
                    }
                    verifyPixels(original,s.transformPreview(),r,op);
                    byte[] preview=s.transformPreview().bytes();s.act(Action.CANCEL);
                    check(s.mode==Mode.CANVAS&&!s.transforming()&&!s.canUndo(),"cancel preserves editor and history");
                    same(original.bytes(),s.cart().bytes(),"cancel byte exact");
                    begin(s);s.chooseTransform(op.ordinal());p.fail=true;s.act(Action.CONFIRM);
                    check(s.mode==Mode.ERROR&&s.transforming()&&!s.canUndo()&&p.writes==0,"failed save retains preview");
                    same(original.bytes(),s.cart().bytes(),"failed save never publishes pixels");s.act(Action.CANCEL);
                    check(s.mode==Mode.TRANSFORM,"retry returns to the same preview");p.fail=false;s.act(Action.CONFIRM);
                    check(p.writes==1&&!s.transforming()&&s.mode==Mode.CANVAS,"one operation one commit");
                    same(preview,p.saved,"saved exactly what preview showed");s.act(Action.TEST);s.act(Action.UNDO);
                    same(original.bytes(),s.cart().bytes(),"single undo restores all original bytes including missing gfx rows");
                    check(!s.canUndo(),"one history entry");
                }
            }
            SpriteRegion square=new SpriteRegion(0,0,16,16);Port p=new Port();
            WorkshopCartridge original=base.withPixel(square,1,2,14);
            WorkshopSession s=editor(original,square,p);
            s.restoreTransform("ROTATE_CLOCKWISE","CANVAS");check(s.mode==Mode.TRANSFORM&&p.writes==0,"cold restoration requires confirmation");
            verifyPixels(original,s.transformPreview(),square,SpriteTransform.ROTATE_CLOCKWISE);s.act(Action.CANCEL);
            s.restoreTransform("bad","CANVAS");s.restoreTransform("FLIP_HORIZONTAL","ASSETS");check(!s.transforming(),"corrupt metadata ignored");
            s.region=new SpriteRegion(0,56,16,16);s.restoreTransform("FLIP_HORIZONTAL","CANVAS");check(s.transforming(),"shared transform restores a read-only proposal");s.act(Action.CANCEL);
            s.region=square;s.drawTool=WorkshopSession.DrawTool.LINE;s.lineX=0;s.lineY=0;s.act(Action.DRAW_TOOLS);
            check(s.mode==Mode.CANVAS&&s.pendingLine(),"line draft cannot be replaced by transforms");
        }
        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));
        SpriteRegion r=new SpriteRegion(0,0,16,16);Port p=new Port();WorkshopSession s=editor(blank,r,p);begin(s);s.act(Action.CONFIRM);
        check(p.writes==0&&!s.canUndo(),"empty no-op does not materialize gfx or undo");same(blank.bytes(),s.cart().bytes(),"empty byte exact");
        WorkshopCartridge asymmetric=blank.withPixel(r,1,2,8).withPixel(r,4,3,12),rotated=asymmetric;
        for(int i=0;i<4;i++)rotated=rotated.transformed(r,SpriteTransform.ROTATE_CLOCKWISE);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)check(asymmetric.sheetPixel(x,y)==rotated.sheetPixel(x,y),"four turns restore orientation, including implicitly zero rows");
        // Existing upper-case hex survives no-op pixels.
        P8Document doc=P8Document.parse(asymmetric.bytes());int gfx=doc.uniqueSection("gfx");
        byte[] upper=new String(doc.body(gfx),StandardCharsets.US_ASCII).toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        byte[] full=doc.edit(gfx,0,upper.length,upper).bytes();
        WorkshopCartridge unusual=new WorkshopCartridge(full);SpriteRegion other=new SpriteRegion(32,0,16,16);
        same(full,unusual.transformed(other,SpriteTransform.FLIP_HORIZONTAL).bytes(),"blank selection preserves unrelated encoding");
        System.out.println("TransformWorkflowTest: "+checks+" checks passed");
    }
}
