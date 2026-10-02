import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class SpriteWorkflowTest {
    static int checks;
    static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    interface Throwing {void run();}
    static void rejects(Throwing call){try{call.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;int writes;boolean fail;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saved=bytes;writes++;}
        public void launch(byte[] bytes){launched=bytes;}
    }
    static String ascii(byte[] bytes){return new String(bytes,StandardCharsets.ISO_8859_1);}
    static byte[] bytes(String text){return text.getBytes(StandardCharsets.ISO_8859_1);}
    static byte[] body(byte[] bytes,String name){P8Document d=P8Document.parse(bytes);return d.body(d.uniqueSection(name));}
    public static void main(String[] args)throws Exception{
        byte[] original=Files.readAllBytes(Paths.get(args[0]));WorkshopCartridge cart=new WorkshopCartridge(original);
        check(Arrays.equals(cart.bytes(),original),"legacy project opens unchanged; no migration");
        check(cart.heroSlot()==0&&cart.firstFreeSlot()==1,"legacy hero and allocation");
        for(int slot=0;slot<8;slot++)for(int x:new int[]{0,7,8,15})for(int y:new int[]{0,7,8,15}){
            int color=(cart.pixel(slot,x,y)+1)%16;
            byte[] after=cart.withPixel(slot,x,y,color).bytes();
            int expected=P8Document.parse(original).sections().get(P8Document.parse(original).uniqueSection("gfx")).bodyStart+y*129+slot*16+x;
            int count=0;for(int at=0;at<original.length;at++)if(original[at]!=after[at]){count++;check(at==expected,"actual standard gfx byte address");}
            check(count==1,"one pixel changes one byte in every region");
        }
        rejects(()->cart.withPixel(8,0,0,1));rejects(()->cart.copySprite(0,0));rejects(()->cart.withHero(1));
        String unusual=ascii(original).replace("\n","\r\n")+"__future__\r\n\u0081unknown\r\n";
        WorkshopCartridge source=new WorkshopCartridge(bytes(unusual));
        for(int slot=1;slot<8;slot++){
            WorkshopCartridge copy=source.copySprite(0,slot);
            byte[] before=source.bytes(),after=copy.bytes();
            P8Document d=P8Document.parse(before);int gfxStart=d.sections().get(d.uniqueSection("gfx")).bodyStart;
            check(before.length==after.length,"copy length stable");
            for(int at=0;at<before.length;at++)if(before[at]!=after[at]){
                int relative=at-gfxStart;
                check(relative>=0&&relative/130<16&&relative%130>=slot*16&&relative%130<slot*16+16,"copy touches only destination pixels, retaining CRLF and unknown bytes");
            }
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)check(copy.pixel(slot,x,y)==source.pixel(0,x,y),"copy exact pixels");
            check(copy.heroSlot()==0,"copy does not assign hero");
            final int target=slot;rejects(()->copy.copySprite(0,target));
            WorkshopCartridge assigned=copy.withHero(slot);
            check(assigned.heroSlot()==slot&&assigned.code().contains("spr("+(slot*2)+",x,y,2,2)"),"ordinary PICO-8 sprite number");
            check(Arrays.equals(body(copy.bytes(),"gfx"),body(assigned.bytes(),"gfx")),"assignment cannot change image data");
            check(Arrays.equals(copy.bytes(),assigned.withHero(0).bytes()),"assignment and reversal exact, including two-digit numbers");
        }
        String ambiguous=ascii(original).replace(" spr(0,x,y,2,2)"," spr(0,x,y,2,2)\n spr(0,x,y,2,2)");
        rejects(()->new WorkshopCartridge(bytes(ambiguous)));
        String custom=ascii(original).replace(" spr(0,x,y,2,2)"," spr(other,x,y,2,2)");
        rejects(()->new WorkshopCartridge(bytes(custom)));

        Port port=new Port();WorkshopSession s=new WorkshopSession(cart,port);
        s.switchTool(2);check(s.mode==Mode.SHEET,"first entry is resource sheet");
        s.act(Action.RIGHT);check(s.spriteSlot==1&&s.cart().heroSlot()==0&&port.writes==0,"browsing never assigns or saves");
        s.act(Action.ASSIGN_HERO);check(port.writes==0&&s.mode==Mode.SHEET,"empty assignment stays safely in sheet");
        s.act(Action.LEFT);s.act(Action.CONTEXT);
        check(s.spriteSlot==1&&s.mode==Mode.CANVAS&&s.cart().heroSlot()==0,"copy opens new image, original hero retained");
        check(port.writes==1,"copy is one durable undo step");
        s.act(Action.CONFIRM);s.act(Action.CANCEL);s.act(Action.ASSIGN_HERO);s.act(Action.TEST);
        WorkshopCartridge launched=new WorkshopCartridge(port.launched);
        check(launched.heroSlot()==1&&launched.pixel(1,7,7)==s.color,"launch uses assigned, edited copy");
        check(launched.pixel(0,7,7)==cart.pixel(0,7,7),"original hero pixels unchanged");
        s.act(Action.UNDO);check(s.cart().heroSlot()==0,"undo assignment restores old hero");
        s.act(Action.UNDO);s.act(Action.UNDO);check(Arrays.equals(original,s.cart().bytes()),"copy/edit/assignment fully undo byte-for-byte");
        s.act(Action.SPRITE_SHEET);s.act(Action.NEW_SPRITE);
        check(s.spriteSlot==1&&s.mode==Mode.CANVAS&&s.cart().empty(1),"new enters blank region");
        int writes=port.writes;s.act(Action.CANCEL);s.act(Action.CANCEL);
        check(s.mode==Mode.SHEET&&port.writes==writes,"leaving an untouched new image performs no write");
        s.act(Action.MENU);s.act(Action.CANCEL);check(s.mode==Mode.SHEET,"menu returns to sheet");
        s.act(Action.LEFT);s.act(Action.COPY_SPRITE);s.act(Action.CANCEL);port.fail=true;
        s.act(Action.ASSIGN_HERO);check(s.mode==Mode.ERROR&&s.cart().heroSlot()==0,"failed assignment preserves canonical binding");
        port.fail=false;s.act(Action.CANCEL);s.act(Action.SPRITE_SHEET);s.spriteSlot=0;
        port.fail=true;s.act(Action.COPY_SPRITE);check(s.mode==Mode.ERROR&&s.spriteSlot==0&&s.cart().empty(2),"failed copy preserves selection and empty destination");
        s.act(Action.CANCEL);check(s.mode==Mode.SHEET,"copy error returns to sheet");
        WorkshopCartridge full=cart;for(int slot=1;slot<8;slot++)full=full.withPixel(slot,0,0,7);
        check(full.firstFreeSlot()==-1,"full sheet allocation refuses overwrite");
        Port fullPort=new Port();WorkshopSession filled=new WorkshopSession(full,fullPort);filled.switchTool(2);
        filled.act(Action.NEW_SPRITE);filled.act(Action.COPY_SPRITE);
        check(fullPort.writes==0&&filled.mode==Mode.SHEET,"full sheet gives notice without mutation");
        // Reach New / Copy / Assign and return to the same resource using only D-pad.
        WorkshopSession navigation=new WorkshopSession(cart,new Port());navigation.switchTool(2);
        navigation.act(Action.DOWN);navigation.act(Action.DOWN);check(navigation.sheetFocus==8,"down reaches New");
        navigation.act(Action.RIGHT);check(navigation.sheetFocus==9,"right reaches Copy");
        navigation.act(Action.RIGHT);check(navigation.sheetFocus==10,"right reaches Assign");
        navigation.act(Action.UP);check(navigation.sheetFocus==4,"up returns to prior selected resource");
        System.out.println("Sprite workflows: "+checks+" checks passed (safe copy, assignment, allocation, controller paths)");
    }
}
