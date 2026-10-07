import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MoveWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;byte[] saved;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b.clone();writes++;}
        public void launch(byte[] b){launches++;same(saved,b,"test launches saved movement");}
    }
    static void begin(WorkshopSession s){
        s.act(Action.DRAW_TOOLS);while(s.drawToolCursor<WorkshopSession.moveMenuIndex())s.act(Action.DOWN);
        s.act(Action.CONFIRM);check(s.mode==Mode.MOVE&&s.move.phase==0,"controller opens move tool");
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            String text=new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(text.getBytes(StandardCharsets.ISO_8859_1));
            SpriteRegion scope=new SpriteRegion(8,16,32,24),source=new SpriteRegion(11,20,9,7);
            for(int y=0;y<scope.height;y++)for(int x=0;x<scope.width;x++)base=base.withPixel(scope,x,y,(x+3*y)%16);
            for(int[] d:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1},{2,3},{-3,-4},{18,12}}){
                SpriteRegion target=new SpriteRegion(source.x+d[0],source.y+d[1],source.width,source.height);
                WorkshopCartridge result=base.moved(source,target);
                for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                    boolean dest=x>=target.x&&x<target.x+target.width&&y>=target.y&&y<target.y+target.height;
                    boolean src=x>=source.x&&x<source.x+source.width&&y>=source.y&&y<source.y+source.height;
                    int expected=dest?base.sheetPixel(source.x+x-target.x,source.y+y-target.y):src?0:base.sheetPixel(x,y);
                    check(result.sheetPixel(x,y)==expected,"capture, clear, paste precedence including overlap and zero");
                }
                P8Document a=P8Document.parse(base.bytes()),b=P8Document.parse(result.bytes());
                for(int i=0;i<a.sections().size();i++)if(!a.sections().get(i).name.equals("gfx"))same(a.body(i),b.body(i),"other sections byte exact");
                if(d[0]==0&&d[1]==0)same(base.bytes(),result.bytes(),"same-position move is byte exact");
            }
            Port p=new Port();WorkshopSession s=new WorkshopSession(base,p);s.openSprite(0);s.region=scope;s.mode=Mode.CANVAS;s.cursorX=3;s.cursorY=4;
            begin(s);s.act(Action.CONFIRM);for(int i=0;i<8;i++)s.act(Action.RIGHT);for(int i=0;i<6;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);
            s.act(Action.RIGHT);s.act(Action.DOWN);check(s.move.phase==2&&p.writes==0,"selection and placement are previews");
            byte[] expected=base.moved(source,new SpriteRegion(12,21,9,7)).bytes();same(expected,s.move.preview(base).bytes(),"overlapping preview exact");
            for(Action a:new Action[]{Action.NEXT,Action.MENU,Action.CONTEXT,Action.REDO,Action.REGION,Action.ASSETS,Action.COPY_SPRITE})s.act(a);
            s.switchTool(0);s.openSprite(1);s.openHero();check(s.mode==Mode.MOVE&&s.region==scope&&p.writes==0&&p.launches==0,"draft traps escapes and runtime");
            String encoded=s.move.encode(),origin=s.moveReturnMode();s.act(Action.UNDO);same(base.bytes(),s.cart().bytes(),"Y cancels without writing");
            s.restoreMove(encoded,origin);same(expected,s.move.preview(base).bytes(),"restored draft rebuilds from canonical cart");
            p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.move!=null&&!s.canUndo(),"failed save keeps draft");
            same(base.bytes(),s.cart().bytes(),"failed write never publishes");s.act(Action.CANCEL);p.fail=false;s.act(Action.CONFIRM);
            check(s.move==null&&p.writes==1&&s.undoCount()==1,"one durable edit");same(expected,p.saved,"commit matches preview");s.act(Action.TEST);
            s.act(Action.UNDO);same(base.bytes(),s.cart().bytes(),"whole operation undo");
            begin(s);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.canRedo()&&p.writes==2,"same-position no-op retains redo");
            s.act(Action.REDO);same(expected,s.cart().bytes(),"redo retained after no-op");
            begin(s);s.act(Action.CONFIRM);s.move.point(31,23);s.act(Action.CONFIRM);s.move.point(1000,1000);
            check(s.move.destination().x+s.move.source().width<=scope.x+scope.width,"placement clamps without clipping");
            s.act(Action.CANCEL);check(s.move.phase==1,"back adjusts second corner");s.move.point(0,0);
            SpriteMove restored=SpriteMove.restore(scope,s.move.encode());check(restored.phase==1,"selection recovery ignores obsolete destination bounds");
            s.act(Action.CANCEL);s.act(Action.CANCEL);check(s.move==null,"back exits first corner");
            for(String bad:new String[]{"", "2,0,0,32,0,0,0", "2,0,0,8,8,31,23", "3,0,0,0,0,0,0", "2,-1,0,0,0,0,0"}){
                s.restoreMove(bad,"CANVAS");check(s.move==null,"invalid recovery ignored");
            }
            s.region=new SpriteRegion(0,56,16,16);s.restoreMove(encoded,"CANVAS");check(s.move!=null,"shared-map move recovery is read-only");s.act(Action.UNDO);
        }
        System.out.println("MoveWorkflowTest: "+checks+" checks passed");
    }
}
