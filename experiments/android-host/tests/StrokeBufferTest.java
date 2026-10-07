import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** User transactions: one stroke, isolated Test, recovery, and independent cross-project pixels. */
public final class StrokeBufferTest {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static void same(byte[] a,byte[] b,String message){check(Arrays.equals(a,b),message);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("Unsafe recovery accepted");}catch(IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port {
        byte[] buffer,launched;int saves;boolean fail,bufferFail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;}
        public void launch(byte[] b){launched=b.clone();}
        public void writeSpriteBuffer(SpriteAsset asset)throws Exception{if(bufferFail)throw new Exception("buffer write failed");buffer=asset.encode();}
        public SpriteAsset readSpriteBuffer()throws Exception{return buffer==null?null:SpriteAsset.decode(buffer);}
    }
    static WorkshopSession editor(WorkshopCartridge cart,SpriteRegion r,Port p){
        WorkshopSession s=new WorkshopSession(cart,p);s.tool=2;s.browsingSprites=false;s.region=r;s.mode=Mode.CANVAS;s.cursorX=s.cursorY=0;return s;
    }
    static void operation(WorkshopSession s,int item){s.act(Action.DRAW_TOOLS);s.chooseDrawTool(item);}
    static void history(WorkshopSession s,WorkshopCartridge before,WorkshopCartridge after){
        check(s.undoCount()==1&&s.stroke==null&&s.sharedEdit==null,"one history item after complete stroke");same(after.bytes(),s.cart().bytes(),"Apply saves the entire candidate");
        s.act(Action.UNDO);same(before.bytes(),s.cart().bytes(),"one Undo restores every pixel and map cell");
        s.act(Action.REDO);same(after.bytes(),s.cart().bytes(),"Redo exactly restores the stroke");
    }
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge b=SharedEditingTest.blank(nl);
            for(SpriteRegion r:new SpriteRegion[]{new SpriteRegion(0,0,32,24),new SpriteRegion(0,56,32,24),new SpriteRegion(112,120,16,8)}){
                Port p=new Port();WorkshopSession s=editor(b,r,p);s.color=12;
                operation(s,WorkshopSession.strokeMenuIndex());check(s.mode==Mode.STROKE,"controller menu starts a stroke");
                int[] end={r.width-1,r.height-1};s.strokePoint(end[0],end[1]);s.strokePoint(0,end[1]);
                WorkshopCartridge expected=b.withLine(r,0,0,end[0],end[1],12).withLine(r,end[0],end[1],0,end[1],12);
                same(expected.bytes(),s.canvasPreview().bytes(),"sparse touch samples connect without gaps");same(b.bytes(),s.cart().bytes(),"drawing never publishes early");
                s.act(Action.TEST);same(expected.bytes(),p.launched,"stroke Test uses the isolated preview");check(p.saves==0&&s.undoCount()==0,"Test preserves history and storage");
                s.act(Action.MENU);s.act(Action.NEXT);s.act(Action.CONTEXT);s.act(Action.ASSETS);s.openSprite(2);s.openUses(true);s.switchTool(0);
                check(s.mode==Mode.STROKE&&s.tool==2&&s.uses==null,"modal stroke cannot escape or publish through other actions");
                byte[] draft=s.stroke.encode();Port q=new Port();WorkshopSession recovered=editor(b,r,q);recovered.restoreFreehand(draft);
                same(expected.bytes(),recovered.canvasPreview().bytes(),"stroke recovers full path and scope");
                WorkshopSession stale=editor(b.withPixel(r,1,1,7),r,new Port());refused(()->stale.restoreFreehand(draft));
                byte[] corrupt=Arrays.copyOf(draft,draft.length+1);refused(()->SpriteStroke.restore(corrupt,b));
                s.act(Action.CANCEL);same(b.bytes(),s.cart().bytes(),"Cancel discards the whole stroke");check(s.stroke==null&&p.saves==0,"Cancel creates no history");
                if(r.sharesMap()){
                    recovered.act(Action.CONFIRM);check(recovered.mode==Mode.SHARED,"lower pixels receive shared review once after stroke");
                    byte[] shared=recovered.sharedEdit.encode();recovered.act(Action.CANCEL);check(recovered.mode==Mode.STROKE&&recovered.stroke!=null,"review Back retains the complete path");
                    same(expected.bytes(),recovered.canvasPreview().bytes(),"Back does not lose the stroke");
                    WorkshopSession missing=editor(b,r,new Port());refused(()->missing.restoreShared(shared));
                    recovered.restoreShared(shared);q.fail=true;recovered.act(Action.CONFIRM);check(recovered.mode==Mode.ERROR&&recovered.sharedEdit!=null&&recovered.stroke!=null,"failed shared write retains proposal and path");
                    q.fail=false;recovered.act(Action.CANCEL);recovered.act(Action.CONFIRM);
                }else{
                    q.fail=true;recovered.act(Action.CONFIRM);check(recovered.mode==Mode.ERROR&&recovered.stroke!=null&&q.saves==0,"failed upper stroke retains draft");
                    q.fail=false;recovered.act(Action.CANCEL);check(recovered.mode==Mode.STROKE,"error dismissal returns to stroke");recovered.act(Action.CONFIRM);
                }
                history(recovered,b,expected);
                Port eraserPort=new Port();WorkshopSession eraser=editor(expected,r,eraserPort);eraser.drawTool=DrawTool.ERASER;
                check(eraser.beginStrokeAt(0,0),"touch eraser starts a stroke");eraser.strokePoint(end[0],end[1]);check(eraser.canvasPreview().pixel(r,0,0)==0&&eraser.canvasPreview().pixel(r,end[0],end[1])==0,"eraser path writes palette zero");eraser.act(Action.UNDO);same(expected.bytes(),eraser.cart().bytes(),"Y cancels the unsaved eraser");
            }
            SpriteRegion full=new SpriteRegion(0,0,128,128);SpriteStroke all=new SpriteStroke(b,full,15,0,0);
            for(int y=0;y<128;y++)all.point(y%2==0?127:0,y);
            same(all.preview(b).bytes(),SpriteStroke.restore(all.encode(),b).preview(b).bytes(),"maximum-size path round trips");
            Port p=new Port();WorkshopSession s=editor(b,new SpriteRegion(0,64,8,8),p);s.color=0;s.beginStrokeAt(0,0);s.strokePoint(7,7);s.act(Action.CONFIRM);
            check(s.mode==Mode.CANVAS&&s.stroke==null&&s.undoCount()==0&&p.saves==0,"no-op stroke adds no review, sections, writes or history");same(b.bytes(),s.cart().bytes(),"no-op source stays exact");
            SpriteRegion source=new SpriteRegion(0,64,32,24);WorkshopCartridge authored=b.withLine(source,0,0,31,23,14);p=new Port();s=editor(authored,source,p);
            operation(s,WorkshopSession.bufferCopyIndex());SpriteAsset captured=p.readSpriteBuffer();byte[] buffer=p.buffer.clone();
            check(captured.width==32&&captured.height==24&&captured.pixel(31,23)==14,"buffer captures dimensions and independent indexed pixels");check(s.undoCount()==0&&p.saves==0,"buffer copy does not edit the source cart");
            p.bufferFail=true;operation(s,WorkshopSession.bufferCopyIndex());check(s.mode==Mode.ERROR,"buffer failure shown");same(buffer,p.buffer,"failed copy preserves existing buffer");s.act(Action.CANCEL);s.act(Action.CANCEL);check(s.mode==Mode.CANVAS,"buffer failure can return from tools without trapping navigation");
            Port destPort=new Port();destPort.buffer=buffer.clone();WorkshopSession dest=editor(b,new SpriteRegion(0,0,8,8),destPort);
            operation(dest,WorkshopSession.bufferPasteIndex());dest.pointCopy(12,12);dest.act(Action.CONFIRM);check(dest.mode==Mode.COPY_CONFIRM&&dest.bufferInsertion(),"another project previews buffer replacement");
            WorkshopCartridge expected=b.insert(captured,new SpriteRegion(96,96,32,24));dest.act(Action.TEST);same(expected.bytes(),destPort.launched,"buffer form Test materializes ordinary pixels");same(b.bytes(),dest.cart().bytes(),"buffer Test leaves saved cart unchanged");
            dest.act(Action.CONFIRM);check(dest.mode==Mode.SHARED,"buffer lower destination requests shared review");byte[] shared=dest.sharedEdit.encode();
            // A pending paste restores its own captured bytes, independent of a subsequent global copy.
            destPort.buffer=SpriteAsset.capture(authored,source,"New buffer","Other project").encode();
            WorkshopSession restored=editor(b,new SpriteRegion(0,0,8,8),destPort);restored.restoreBufferInsertion(SpriteAsset.decode(buffer),96,96);restored.restoreShared(shared);
            same(expected.bytes(),restored.sharedEdit.after.bytes(),"restored paste uses captured buffer, not current global contents");restored.act(Action.CONFIRM);history(restored,b,expected);
            same(buffer,captured.encode(),"editing destination never changes captured pixels");
            Port empty=new Port();WorkshopSession noBuffer=editor(b,source,empty);operation(noBuffer,WorkshopSession.bufferPasteIndex());check(noBuffer.mode==Mode.DRAW_TOOLS&&!noBuffer.copying()&&empty.saves==0,"empty buffer explains setup without inventing pixels");
        }
        System.out.println("Stroke and buffer checks passed: "+checks);
    }
}
