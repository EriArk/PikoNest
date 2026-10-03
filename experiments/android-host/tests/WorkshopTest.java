import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class WorkshopTest {
    static int checks;
    static void check(boolean result,String message){checks++;if(!result)throw new AssertionError(message);}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;int writes,launches;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;writes++;}
        public void launch(byte[] b){launched=b;launches++;}
    }
    static void singleByte(byte[] before,byte[] after){
        check(before.length==after.length,"length preserved");int changes=0;
        for(int i=0;i<before.length;i++)if(before[i]!=after[i])changes++;
        check(changes==1,"only one targeted byte changed");
    }
    public static void main(String[] args)throws Exception{
        byte[] raw=Files.readAllBytes(Paths.get(args[0]));WorkshopCartridge cart=new WorkshopCartridge(raw);
        check(cart.value(0)==2&&cart.value(1)==3,"template values");
        singleByte(raw,cart.withValue(0,4).bytes());singleByte(raw,cart.withValue(1,1).bytes());
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            WorkshopCartridge edited=cart.withPixel(x,y,(cart.pixel(x,y)+1)%16);
            singleByte(raw,edited.bytes());check(edited.pixel(x,y)==(cart.pixel(x,y)+1)%16,"pixel addressed");
        }
        byte[] crlf=(new String(raw,StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n").getBytes(StandardCharsets.ISO_8859_1);
        singleByte(crlf,new WorkshopCartridge(crlf).withPixel(15,15,11).bytes());
        singleByte(crlf,new WorkshopCartridge(crlf).withValue(0,3).bytes());
        byte[] owned=cart.bytes();owned[0]=0;check(cart.bytes()[0]=='p',"defensive bytes");
        try{cart.withValue(0,0);throw new AssertionError("accepted invalid value");}catch(IllegalArgumentException expected){checks++;}
        try{cart.withPixel(16,0,1);throw new AssertionError("accepted invalid pixel");}catch(IllegalArgumentException expected){checks++;}
        Port port=new Port();WorkshopSession s=new WorkshopSession(cart,port);
        s.act(Action.RIGHT);check(s.focus==1&&port.writes==0,"navigation cannot edit");
        s.act(Action.LEFT);s.act(Action.CONFIRM);s.act(Action.RIGHT);
        check(s.draft==3&&s.cart().value(0)==2&&port.writes==0,"draft stays unsaved");
        s.act(Action.NEXT);check(s.tool==0&&s.mode==Mode.VALUE,"tab cannot discard draft");
        s.act(Action.CANCEL);check(s.cart().value(0)==2&&port.writes==0,"cancel preserves original");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.TEST);
        check(s.cart().value(0)==3&&port.writes==1&&port.launches==1,"test commits draft");
        check(Arrays.equals(port.saved,port.launched),"runtime receives saved bytes");
        s.act(Action.UNDO);check(Arrays.equals(raw,s.cart().bytes()),"undo restores exact bytes");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);port.fail=true;s.act(Action.CONFIRM);
        check(s.mode==Mode.ERROR&&s.cart().value(0)==2,"failed write cannot publish edited model");
        check(!s.canUndo(),"failed write cannot add undo entry");
        port.fail=false;s.act(Action.CANCEL);s.act(Action.NEXT);
        s.selectCodeLine(s.cart().line(1));s.codeDraft.end();s.codeDraft.erase(false);s.codeText("2");s.codeCommand(0);
        check(s.cart().value(1)==2,"code parameter changes real Lua");
        s.act(Action.NEXT);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.mode==Mode.CANVAS,"controller enters canvas through sheet");
        int oldX=s.cursorX,oldY=s.cursorY,original=s.cart().pixel(oldX+1,oldY);
        s.act(Action.RIGHT);s.act(Action.CONTEXT);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.CANVAS&&s.color==2,"palette returns to canvas");
        s.act(Action.CONFIRM);check(s.cart().pixel(oldX+1,oldY)==2,"controller paints selected pixel");
        s.act(Action.UNDO);check(s.cart().pixel(oldX+1,oldY)==original,"undo restores painted pixel");
        s.act(Action.CANCEL);s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.swapAB,"mapping reachable by controller");
        int launches=port.launches;s.act(Action.TEST);check(port.launches==launches,"modal traps Start");
        s.act(Action.CANCEL);s.act(Action.TEST);check(port.launches==launches+1,"Start works after modal");
        s.switchTool(0);s.focus=1;s.act(Action.NEXT);s.act(Action.PREVIOUS);
        check(s.focus==1,"tool switch retains focus");
        s.act(Action.NEXT);s.codeLine=s.cart().line(1);s.act(Action.CONTEXT);
        check(s.mode==Mode.HELP&&s.field==1,"code help follows selected variable");
        System.out.println("Workshop: "+checks+" checks passed (byte edits, controller workflows, failures)");
    }
}
