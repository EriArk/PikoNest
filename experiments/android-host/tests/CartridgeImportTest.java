import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class CartridgeImportTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt attempt,String why)throws Exception{try{attempt.run();}catch(Exception expected){checks++;return;}throw new AssertionError(why);}
    static final class Port implements LibrarySession.Port {
        Map<String,byte[]> files=new HashMap<>();Map<String,String> names=new HashMap<>();
        int picks,writes,opens,clears;boolean failWrite,failClear,failOpen;
        public List<String> ids(){return new ArrayList<>(files.keySet());}
        public byte[] read(String id){return files.get(id).clone();}
        public String title(String id){return names.containsKey(id)?names.get(id):LibrarySession.title(id);}
        public void create(String id,byte[] bytes){if(files.containsKey(id))throw new IllegalStateException("exists");files.put(id,bytes.clone());writes++;}
        public void open(String id,WorkshopCartridge c)throws Exception{if(failOpen)throw new IOException("open");opens++;}
        public void resume(){}
        public void pickImport(){picks++;}
        public void importProject(CartridgeImport draft)throws Exception{
            if(failWrite)throw new IOException("write");
            if(files.containsKey(draft.id)){
                if(!Arrays.equals(files.get(draft.id),draft.cart.bytes()))throw new IOException("changed");return;
            }
            create(draft.id,draft.cart.bytes());names.put(draft.id,draft.title());
        }
        public void clearImport()throws Exception{if(failClear)throw new IOException("clear");clears++;}
    }
    public static void main(String[] args)throws Exception{
        byte[] original=Files.readAllBytes(Paths.get(args[0]));
        byte[] unusual=(new String(original,StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n").getBytes(StandardCharsets.ISO_8859_1);
        CartridgeImport draft=new CartridgeImport("Пазл.P8",unusual);
        check(Arrays.equals(unusual,draft.cart.bytes()),"no normalization including CRLF and unknown bytes");
        CartridgeImport restored=CartridgeImport.decode(draft.encode());
        check(restored.id.equals(draft.id)&&restored.filename.equals(draft.filename)&&Arrays.equals(restored.cart.bytes(),unusual),"preview restoration retains identity, name, all bytes");
        check(LibrarySession.validId(draft.id)&&!LibrarySession.validId(draft.id+"/../other"),"generated ID only, not a source path");
        check(new CartridgeImport("../folder/game.p8",original).filename.equals(".. folder game.p8"),"filename is display data only");
        check(new CartridgeImport(".p8",original).title().equals("Картридж"),"nonempty fallback title");
        rejects(()->new CartridgeImport("cart.p8.png",original),"PNG support is not guessed");
        rejects(()->new CartridgeImport("cart.zip",original),"archives unsupported");
        rejects(()->new CartridgeImport("cart.p8",new byte[]{1,2}),"wrong framing rejected");
        rejects(()->new CartridgeImport("cart.p8",new byte[CartridgeImport.MAX_BYTES+1]),"bounded import");
        rejects(()->CartridgeImport.readBounded(new ByteArrayInputStream(new byte[CartridgeImport.MAX_BYTES+1])),"bounded untrusted stream");
        check(CartridgeImport.readBounded(new ByteArrayInputStream(new byte[CartridgeImport.MAX_BYTES])).length==CartridgeImport.MAX_BYTES,"exact intake budget accepted");
        byte[] record=draft.encode();rejects(()->CartridgeImport.decode(Arrays.copyOf(record,record.length-1)),"truncated snapshot");
        rejects(()->CartridgeImport.decode(Arrays.copyOf(record,record.length+1)),"trailing data");
        record[0]=0;rejects(()->CartridgeImport.decode(record),"unknown snapshot version");
        Port port=new Port();port.files.put("moon-garden",original.clone());LibrarySession s=new LibrarySession(port,original);s.refresh("moon-garden");
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.focus==3&&port.picks==1,"controller reaches file selection");
        s.stageImport(draft);s.act(Action.TEST);s.act(Action.CONTEXT);
        check(port.writes==0&&port.opens==0,"preview never auto-launches or bypasses confirm");
        s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.SHELF&&s.importing==null&&port.writes==0&&port.clears==1,"cancel clears only snapshot");
        s.stageImport(restored);port.failWrite=true;s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.ERROR&&s.importing==restored&&port.writes==0,"failed publish retains preview");
        s.act(Action.CONFIRM);check(s.mode==LibrarySession.Mode.IMPORT,"error returns to preview");
        port.failWrite=false;port.failClear=true;s.act(Action.CONFIRM);
        check(port.writes==1&&s.mode==LibrarySession.Mode.ERROR,"publish success plus cleanup error does not lose data");
        port.failClear=false;s.act(Action.CANCEL);s.act(Action.CONFIRM);
        check(port.writes==1&&port.opens==1&&s.importing==null,"retry reuses same destination");
        check(Arrays.equals(port.files.get(draft.id),unusual)&&Arrays.equals(port.files.get("moon-garden"),original),"exact import and unrelated source preserved");
        check(s.current().title.equals("Пазл")&&s.current().id.equals(draft.id),"filename appears on selected shelf card");
        s.stageImport(new CartridgeImport("second.p8",original));port.failOpen=true;s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.ERROR&&s.importing==null&&port.writes==2,"open failure preserves published cart, no pending duplicate");
        s.act(Action.CANCEL);port.failOpen=false;s.act(Action.CONFIRM);
        check(port.writes==2&&port.opens==2,"retry opens selected published project");
        s.mode=LibrarySession.Mode.READING;s.act(Action.CONFIRM);check(s.mode==LibrarySession.Mode.READING,"cannot confirm incomplete read");
        s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.SHELF,"slow read cancellable");
        CartridgeImport conflict=new CartridgeImport("conflict.p8",original);port.files.put(conflict.id,new byte[]{42});s.stageImport(conflict);s.act(Action.CONFIRM);
        check(port.files.get(conflict.id)[0]==42&&s.mode==LibrarySession.Mode.ERROR,"retry cannot overwrite changed project");
        // Single-file import preserves external references; it does not fake dependency resolution.
        byte[] include="pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n#include other.lua\n".getBytes(StandardCharsets.US_ASCII);
        check(Arrays.equals(new CartridgeImport("linked.p8",include).cart.bytes(),include),"dependencies remain untouched for explicit later support");
        System.out.println("CartridgeImportTest: "+checks+" checks passed");
    }
}
