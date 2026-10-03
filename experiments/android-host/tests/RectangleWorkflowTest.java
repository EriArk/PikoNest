import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class RectangleWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;byte[] saved,launched;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saved=bytes.clone();writes++;}
        public void launch(byte[] bytes){launched=bytes.clone();launches++;}
    }
    static void tool(WorkshopSession s,DrawTool tool){
        s.act(Action.DRAW_TOOLS);int target=WorkshopSession.drawMenuIndex(tool);
        while(s.drawToolCursor<target)s.act(Action.DOWN);while(s.drawToolCursor>target)s.act(Action.UP);
        s.act(Action.CONFIRM);check(s.drawTool==tool&&s.mode==Mode.CANVAS,"controller chooses rectangle brush");
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            String text=new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(text.getBytes(StandardCharsets.ISO_8859_1));
            for(SpriteRegion r:new SpriteRegion[]{new SpriteRegion(8,8,24,24),new SpriteRegion(64,32,32,24),new SpriteRegion(0,0,128,64)}){
                WorkshopCartridge original=base.withPixel(r,4,4,12);
                int[][] corners={{1,2,r.width-2,r.height-3},{r.width-1,r.height-1,0,0},{3,3,3,3},{2,1,2,9},{1,2,9,2}};
                for(boolean filled:new boolean[]{false,true})for(int color:new int[]{0,10})for(int[] e:corners){
                    WorkshopCartridge out=original.withRectangle(r,e[0],e[1],e[2],e[3],color,filled);
                    int left=Math.min(e[0],e[2])+r.x,right=Math.max(e[0],e[2])+r.x,top=Math.min(e[1],e[3])+r.y,bottom=Math.max(e[1],e[3])+r.y;
                    for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                        boolean inside=x>=left&&x<=right&&y>=top&&y<=bottom;
                        boolean painted=inside&&(filled||x==left||x==right||y==top||y==bottom);
                        check(out.sheetPixel(x,y)==(painted?color:original.sheetPixel(x,y)),"inclusive rectangle mask; untouched interior, outside and shared map rows");
                    }
                    same(out.bytes(),original.withRectangle(r,e[2],e[3],e[0],e[1],color,filled).bytes(),"reversed corners identical");
                    P8Document before=P8Document.parse(original.bytes()),after=P8Document.parse(out.bytes());
                    for(int i=0;i<before.sections().size();i++)if(!before.sections().get(i).name.equals("gfx"))same(before.body(i),after.body(i),"non-gfx including unknown sections exact");
                }
            }
            SpriteRegion r=new SpriteRegion(64,24,32,24);
            for(DrawTool brush:new DrawTool[]{DrawTool.RECTANGLE,DrawTool.FILLED_RECTANGLE}){
                Port p=new Port();WorkshopSession s=new WorkshopSession(base,p);s.openSprite(0);s.region=r;s.zoom=true;
                tool(s,brush);s.cursorX=1;s.cursorY=2;s.color=10;s.act(Action.CONFIRM);
                for(int i=0;i<10;i++)s.act(Action.RIGHT);for(int i=0;i<7;i++)s.act(Action.DOWN);
                check(s.pendingStroke()&&!s.pendingLine()&&p.writes==0,"shape draft never saves early");
                byte[] expected=base.withRectangle(r,1,2,11,9,10,brush==DrawTool.FILLED_RECTANGLE).bytes();
                same(expected,s.canvasPreview().bytes(),"preview matches full rectangle");
                for(Action a:new Action[]{Action.MENU,Action.NEXT,Action.CONTEXT,Action.ASSETS,Action.REGION,Action.ZOOM,Action.COPY_SPRITE,Action.DRAW_TOOLS})s.act(a);
                s.switchTool(0);s.openSprite(1);s.openHero();s.restoreRecolor(8,12,1,"CANVAS");s.restoreTransform("FLIP_HORIZONTAL","CANVAS");
                check(s.pendingStroke()&&s.tool==2&&s.region==r&&s.zoom&&s.drawTool==brush&&p.writes==0,"draft keeps tool, color, selection and zoom");
                same(base.bytes(),s.cart().bytes(),"canonical bytes unchanged during draft");
                s.act(Action.CANCEL);check(!s.pendingStroke()&&!s.canUndo(),"cancel adds no history");
                s.restoreStroke(1,2);check(s.pendingStroke()&&p.writes==0,"restoration only previews");same(expected,s.canvasPreview().bytes(),"restored shape matches");
                p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.pendingStroke()&&!s.canUndo(),"failed save keeps shape");
                same(base.bytes(),s.cart().bytes(),"failure never publishes draft");s.act(Action.CANCEL);p.fail=false;s.act(Action.TEST);
                check(s.mode==Mode.CANVAS&&!s.pendingStroke()&&p.writes==1&&p.launches==1,"Start saves shape once and tests it");
                same(expected,p.saved,"saved preview exactly");same(p.saved,p.launched,"runtime gets saved bytes");
                s.act(Action.UNDO);same(base.bytes(),s.cart().bytes(),"one undo restores all bytes including expanded rows");
                check(!s.canUndo(),"one history entry per shape");
                s.restoreStroke(1,2);s.act(Action.UNDO);check(!s.pendingStroke()&&!s.canUndo(),"Y first cancels draft");
                s.restoreStroke(-1,0);s.restoreStroke(r.width,0);check(!s.pendingStroke(),"invalid anchor ignored");
                s.region=new SpriteRegion(0,56,16,16);s.restoreStroke(1,2);check(!s.pendingStroke(),"restore never enters shared map half");s.region=r;
                s.paintAt(8,8);s.paintAt(4,3);check(!s.pendingStroke(),"two touch corners commit");
                same(base.withRectangle(r,8,8,4,3,10,brush==DrawTool.FILLED_RECTANGLE).bytes(),s.cart().bytes(),"touch and controller use identical operation");
                tool(s,DrawTool.PICKER);s.paintAt(4,3);check(s.drawTool==brush&&s.color==10,"picker returns to rectangle brush");
                s.act(Action.DRAW_TOOLS);check(s.drawToolCursor==WorkshopSession.drawMenuIndex(brush),"reopening menu focuses correct shape");s.act(Action.CANCEL);
            }
        }
        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));SpriteRegion r=new SpriteRegion(0,0,16,16);
        for(boolean filled:new boolean[]{false,true})same(blank.bytes(),blank.withRectangle(r,0,0,15,15,0,filled).bytes(),"zero no-op does not materialize gfx");
        for(int[] bad:new int[][]{{-1,0,0,0,1},{0,0,16,0,1},{0,0,0,16,1},{0,0,1,1,16}}){
            boolean rejected=false;try{blank.withRectangle(r,bad[0],bad[1],bad[2],bad[3],bad[4],false);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid bounds/color rejected");
        }
        check(WorkshopSession.drawMenuTool(5)==null&&WorkshopSession.drawMenuTool(6)==null,"existing operations retain menu positions");
        for(DrawTool t:DrawTool.values())check(WorkshopSession.drawMenuTool(WorkshopSession.drawMenuIndex(t))==t,"menu maps every brush without index collision");
        System.out.println("RectangleWorkflowTest: "+checks+" checks passed");
    }
}
