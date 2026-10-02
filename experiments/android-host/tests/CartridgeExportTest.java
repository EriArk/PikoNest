import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class CartridgeExportTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt a,String why)throws Exception{try{a.run();}catch(Exception expected){checks++;return;}throw new AssertionError(why);}
    static final class Port implements LibrarySession.Port {
        Map<String,byte[]> files=new HashMap<>();CartridgeExport saved,picked;
        boolean failSave,failPick,failClear;int picks,creates,opens;
        public List<String> ids(){return new ArrayList<>(files.keySet());}
        public byte[] read(String id){return files.get(id).clone();}
        public void create(String id,byte[] bytes){creates++;files.put(id,bytes.clone());}
        public void open(String id,WorkshopCartridge cart){opens++;}
        public void resume(){}
        public void saveExport(CartridgeExport draft)throws Exception{if(failSave)throw new IOException("journal full");saved=CartridgeExport.decode(draft.encode());}
        public void pickExport(CartridgeExport draft)throws Exception{if(failPick)throw new IOException("picker unavailable");picked=draft;picks++;}
        public void clearExport()throws Exception{if(failClear)throw new IOException("journal");saved=null;}
    }
    public static void main(String[] args)throws Exception{
        byte[] template=Files.readAllBytes(Paths.get(args[0]));
        byte[] original=(new String(template,StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081preserve\r\n").getBytes(StandardCharsets.ISO_8859_1);
        CartridgeExport draft=new CartridgeExport("puzzle-0001","Пазл",original);
        ByteArrayOutputStream output=new ByteArrayOutputStream();draft.writeTo(output);
        check(Arrays.equals(original,output.toByteArray()),"export preserves CRLF, unknown sections and bytes");
        draft.verify(new ByteArrayInputStream(output.toByteArray()));checks++;
        check(draft.filename.equals("Пазл.p8"),"ordinary P8 suggested name");
        CartridgeExport unsafe=new CartridgeExport("puzzle-0001","../folder\\a:b?*\n.",original);
        check(!unsafe.filename.contains("/")&&!unsafe.filename.contains("\\")&&!unsafe.filename.contains(":"),"source title cannot become destination path");
        check(new CartridgeExport("puzzle-0001","...",original).filename.equals("game.p8"),"empty name fallback");
        byte[] changed=output.toByteArray();changed[changed.length-1]^=1;
        rejects(()->draft.verify(new ByteArrayInputStream(changed)),"corrupt output is not success");
        rejects(()->draft.verify(new ByteArrayInputStream(Arrays.copyOf(original,original.length-1))),"truncated output is not success");
        rejects(()->draft.verify(new ByteArrayInputStream(Arrays.copyOf(original,original.length+1))),"trailing bytes are not success");
        rejects(()->draft.verify(null),"unreadable output is not success");
        rejects(()->draft.writeTo(null),"missing output rejected");
        rejects(()->draft.writeTo(new OutputStream(){int n;public void write(int b)throws IOException{if(++n>31)throw new IOException("disk full");}}),"partial write failure propagates");
        rejects(()->draft.verify(new InputStream(){public int read()throws IOException{throw new IOException("provider offline");}}),"provider read failure propagates");
        for(CartridgeExport.State state:CartridgeExport.State.values()){
            CartridgeExport recovered=CartridgeExport.decode(draft.withState(state,"Пазл (1).p8").encode());
            check(recovered.state==state&&recovered.sourceId.equals(draft.sourceId)&&recovered.resultName.equals("Пазл (1).p8")&&Arrays.equals(recovered.cart.bytes(),original),"journal roundtrip "+state);
        }
        byte[] encoded=draft.encode();rejects(()->CartridgeExport.decode(Arrays.copyOf(encoded,encoded.length-1)),"truncated journal");
        rejects(()->CartridgeExport.decode(Arrays.copyOf(encoded,encoded.length+1)),"extra journal bytes");
        rejects(()->new CartridgeExport("../unsafe","x",original),"untrusted source address");
        rejects(()->new CartridgeExport("puzzle-0001","x",new byte[CartridgeImport.MAX_BYTES+1]),"lab budget");
        Port port=new Port();port.files.put("moon-garden",template.clone());port.files.put("puzzle-0001",template.clone());
        LibrarySession s=new LibrarySession(port,template);s.refresh("puzzle-0001");
        port.files.put("puzzle-0001",original.clone()); // Newer saved bytes than thumbnail.
        s.act(Action.UNDO);check(s.mode==LibrarySession.Mode.EXPORT&&port.saved!=null&&port.picks==0,"Y opens preview, not file creation");
        check(Arrays.equals(s.exporting.cart.bytes(),original),"selected project's latest canonical bytes, not active cart or thumbnail");
        s.act(Action.TEST);s.act(Action.CONTEXT);s.act(Action.NEXT);
        check(port.creates==0&&port.opens==0&&port.picks==0,"preview does not run, switch or copy a project");
        s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.SHELF&&s.exporting==null&&port.saved==null,"cancel before picker writes no external file");
        s.command(4);s.act(Action.CONFIRM);check(s.mode==LibrarySession.Mode.EXPORT_PICKER&&port.picks==1,"confirm chooses destination once");
        s.act(Action.CONFIRM);check(port.picks==1,"repeated confirm cannot launch multiple pickers");
        s.exportPickerCancelled();check(s.mode==LibrarySession.Mode.EXPORT&&s.exporting!=null,"picker cancel keeps preview");
        CartridgeExport snapshot=s.exporting;port.files.put("puzzle-0001",template.clone());
        s.act(Action.CONFIRM);check(Arrays.equals(port.picked.cart.bytes(),original),"preview snapshot survives later source change");
        s.stageExport(snapshot.withState(CartridgeExport.State.WRITING,snapshot.filename));
        s.act(Action.CONFIRM);s.act(Action.CANCEL);s.act(Action.UNDO);check(s.mode==LibrarySession.Mode.EXPORT_WRITING&&port.picks==2,"writing cannot start conflicting operation");
        s.stageExport(snapshot.withState(CartridgeExport.State.UNCERTAIN,snapshot.filename));s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.EXPORT_PICKER&&port.picks==3,"uncertain result requires explicit new target selection");
        s.stageExport(snapshot.withState(CartridgeExport.State.SAVED,"result.p8"));s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.SHELF&&s.exporting==null&&port.picks==3,"acknowledging success does not write again");
        port.failSave=true;s.command(4);check(s.mode==LibrarySession.Mode.ERROR&&s.exporting==null&&port.picks==3,"failed preview persistence prevents picker");
        s.act(Action.CANCEL);port.failSave=false;s.command(4);port.failPick=true;s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.ERROR&&s.exporting!=null,"picker failure preserves snapshot");
        s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.EXPORT,"picker failure returns to preview");
        port.failPick=false;port.failClear=true;s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.ERROR&&s.exporting!=null,"failed cleanup retains draft");
        s.act(Action.CANCEL);port.failClear=false;s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.SHELF,"cleanup retry safe");
        check(Arrays.equals(port.files.get("moon-garden"),template)&&Arrays.equals(port.files.get("puzzle-0001"),template),"export never mutates source projects");
        s.refresh("puzzle-0001");s.act(Action.DOWN);s.act(Action.RIGHT);s.act(Action.DOWN);
        check(s.focus==4,"spatial D-pad route reaches export");
        s.act(Action.LEFT);check(s.focus==3,"same row reaches import");
        s.act(Action.RIGHT);s.act(Action.UP);check(s.focus==2,"up preserves column");
        port.files.put("puzzle-0001",new byte[]{1,2});s.act(Action.UNDO);
        check(s.mode==LibrarySession.Mode.ERROR&&port.picks==3,"unreadable cart not silently exported as a template");
        System.out.println("CartridgeExportTest: "+checks+" checks passed");
    }
}
