import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.p8.P8Document;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class DrawingWorkflowTest {
    static int checks;
    static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    interface Call{void run();}
    static void rejects(Call call){try{call.run();throw new AssertionError("invalid drawing accepted");}catch(IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port{
        int writes,launches;byte[] saved,launched;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;writes++;}
        public void launch(byte[] b){launched=b;launches++;}
    }
    static void scoped(byte[] before,byte[] after,int slot,int stride){
        check(before.length==after.length,"drawing preserves length");
        P8Document d=P8Document.parse(before);int start=d.sections().get(d.uniqueSection("gfx")).bodyStart;
        for(int at=0;at<before.length;at++)if(before[at]!=after[at]){
            int relative=at-start;
            check(relative>=0&&relative/stride<16&&relative%stride>=slot*16&&relative%stride<slot*16+16,"only chosen sprite pixels change, not neighbors, line endings or unknown data");
        }
    }
    static void tool(WorkshopSession s,DrawTool tool){
        s.act(Action.DRAW_TOOLS);for(int i=0;i<5;i++)s.act(Action.UP);
        for(int i=0;i<tool.ordinal();i++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.drawTool==tool&&s.mode==Mode.CANVAS,"tool chosen through semantic actions");
    }
    public static void main(String[] args)throws Exception{
        byte[] raw=Files.readAllBytes(Paths.get(args[0]));WorkshopCartridge cart=new WorkshopCartridge(raw);
        byte[] unusual=(new String(raw,StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n").getBytes(StandardCharsets.ISO_8859_1);
        WorkshopCartridge crlf=new WorkshopCartridge(unusual);
        for(int slot=1;slot<8;slot++){
            WorkshopCartridge fill=crlf.withFill(slot,15,15,12);scoped(unusual,fill.bytes(),slot,130);
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)check(fill.pixel(slot,x,y)==12,"full connected blank region fills to edges");
            check(fill.withFill(slot,0,0,12)==fill,"same-color fill is a byte-preserving no-op");
        }
        WorkshopCartridge ring=cart.withLine(1,3,3,12,3,7).withLine(1,12,3,12,12,7).withLine(1,12,12,3,12,7).withLine(1,3,12,3,3,7);
        WorkshopCartridge inside=ring.withFill(1,5,5,10);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            int expected=x>3&&x<12&&y>3&&y<12?10:ring.pixel(1,x,y);
            check(inside.pixel(1,x,y)==expected,"fill respects a closed border");
        }
        WorkshopCartridge separated=cart.withFill(1,0,0,7).withPixel(1,1,1,0).withPixel(1,2,2,0);
        WorkshopCartridge one=separated.withFill(1,1,1,9);
        check(one.pixel(1,1,1)==9&&one.pixel(1,2,2)==0,"diagonally touching areas stay separate");
        int[][] ends={{0,0,15,15},{15,0,0,15},{3,0,3,15},{0,8,15,8},{0,0,2,1},{2,2,5,15},{14,14,12,1},{7,7,7,7}};
        for(int[] e:ends){
            WorkshopCartridge line=crlf.withLine(7,e[0],e[1],e[2],e[3],11);
            scoped(unusual,line.bytes(),7,130);
            check(Arrays.equals(line.bytes(),crlf.withLine(7,e[2],e[3],e[0],e[1],11).bytes()),"line independent of endpoint order");
            check(line.pixel(7,e[0],e[1])==11&&line.pixel(7,e[2],e[3])==11,"inclusive line endpoints");
            int count=0;for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(line.pixel(7,x,y)==11)count++;
            check(count==Math.max(Math.abs(e[2]-e[0]),Math.abs(e[3]-e[1]))+1,"one pixel per major-axis step");
        }
        WorkshopCartridge diagonal=cart.withLine(1,0,0,15,15,8);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)check(diagonal.pixel(1,x,y)==(x==y?8:0),"diagonal exact coordinates");
        rejects(()->cart.withFill(8,0,0,1));rejects(()->cart.withFill(1,-1,0,1));rejects(()->cart.withFill(1,0,0,16));
        rejects(()->cart.withLine(1,0,0,16,0,1));rejects(()->cart.withLine(1,0,0,1,1,-1));
        Port port=new Port();WorkshopSession s=new WorkshopSession(cart,port);s.openSprite(1);
        s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.mode==Mode.DRAW_TOOLS,"controller navigation reaches drawing tools");
        s.act(Action.DOWN);s.act(Action.CANCEL);check(s.drawTool==DrawTool.BRUSH&&port.writes==0,"chooser cancellation never changes tool or data");
        tool(s,DrawTool.FILL);s.color=12;s.act(Action.CONFIRM);
        check(port.writes==1&&s.cart().pixel(1,15,15)==12,"fill is one durable operation");
        s.act(Action.CONFIRM);check(port.writes==1,"same-color fill adds no write or undo");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),raw)&&!s.canUndo(),"one undo restores entire fill");
        tool(s,DrawTool.LINE);int writes=port.writes;s.cursorX=0;s.cursorY=0;s.color=10;s.act(Action.CONFIRM);
        for(int i=0;i<15;i++)s.act(Action.RIGHT);
        check(s.pendingLine()&&port.writes==writes&&Arrays.equals(s.cart().bytes(),raw),"line preview never saves early");
        check(s.canvasPreview().pixel(1,10,0)==10&&s.cart().pixel(1,10,0)==0,"line ghost differs from canonical bytes");
        s.act(Action.MENU);s.act(Action.NEXT);s.act(Action.ASSIGN_HERO);s.act(Action.NEW_SPRITE);s.switchTool(0);s.openSprite(2);
        check(s.tool==2&&s.spriteSlot==1&&s.mode==Mode.CANVAS&&port.writes==writes,"draft cannot escape to another tool, project or resource");
        s.act(Action.CANCEL);check(!s.pendingLine()&&port.writes==writes&&!s.canUndo(),"cancel line leaves file and history unchanged");
        s.cursorX=0;s.cursorY=0;s.act(Action.CONFIRM);for(int i=0;i<15;i++)s.act(Action.DOWN);
        port.fail=true;s.act(Action.CONFIRM);
        check(s.mode==Mode.ERROR&&s.pendingLine()&&Arrays.equals(s.cart().bytes(),raw)&&!s.canUndo(),"failed write retains line draft for retry without publishing");
        port.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);
        check(!s.pendingLine()&&s.mode==Mode.CANVAS&&port.launches==1&&Arrays.equals(port.saved,port.launched),"Test commits pending line and launches exact saved snapshot");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),raw),"whole line undone in one action");
        s.act(Action.CONFIRM);s.act(Action.UNDO);check(!s.pendingLine()&&!s.canUndo(),"undo cancels pending line before touching history");
        s.openSprite(0);tool(s,DrawTool.FILL);tool(s,DrawTool.PICKER);s.cursorX=7;s.cursorY=7;writes=port.writes;s.act(Action.CONFIRM);
        check(s.color==cart.pixel(0,7,7)&&s.drawTool==DrawTool.FILL&&port.writes==writes,"picker reads color without saving and returns to previous fill tool");
        tool(s,DrawTool.ERASER);s.act(Action.CONTEXT);s.chooseColor(3);
        check(s.drawTool==DrawTool.BRUSH&&s.color==3,"choosing color leaves eraser for brush");
        tool(s,DrawTool.ERASER);tool(s,DrawTool.PICKER);s.act(Action.CONFIRM);
        check(s.drawTool==DrawTool.BRUSH,"sampling while erasing returns to a tool that uses the sampled color");
        s.openSprite(1);tool(s,DrawTool.LINE);s.color=8;s.paintAt(1,2);s.paintAt(4,2);
        check(!s.pendingLine()&&s.cart().pixel(1,1,2)==8&&s.cart().pixel(1,4,2)==8,"two touch endpoints use same line transaction");
        System.out.println("DrawingWorkflowTest: "+checks+" checks passed");
    }
}
