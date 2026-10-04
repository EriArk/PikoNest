import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class LibraryWorkflowTest {
    static int checks;
    static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    static final class Port implements LibrarySession.Port {
        final Map<String,byte[]> files=new HashMap<>();
        String opened;WorkshopCartridge cart;boolean failCreate,failOpen;int resumes,creates;
        public List<String> ids(){return new ArrayList<>(files.keySet());}
        public byte[] read(String id)throws Exception{if(!files.containsKey(id))throw new Exception("missing");return files.get(id).clone();}
        public void create(String id,byte[] bytes)throws Exception{
            if(failCreate)throw new Exception("disk full");
            if(files.containsKey(id))throw new Exception("exists");files.put(id,bytes.clone());creates++;
        }
        public void open(String id,WorkshopCartridge cart)throws Exception{if(failOpen)throw new Exception("open failed");this.opened=id;this.cart=cart;}
        public void resume(){resumes++;}
    }
    public static void main(String[] args)throws Exception{
        byte[] template=Files.readAllBytes(Paths.get(args[0]));
        byte[] original=new WorkshopCartridge(template).withValue(0,4).withPixel(0,1,1,10).bytes();
        Port port=new Port();port.files.put("moon-garden",original.clone());
        LibrarySession s=new LibrarySession(port,template);s.refresh("moon-garden");
        check(s.current().id.equals("moon-garden"),"existing original selected");
        s.command(1);check(s.mode==LibrarySession.Mode.CREATE,"new game has explanatory confirmation");
        s.act(Action.TEST);check(port.creates==0,"Start cannot bypass creation dialog");
        s.act(Action.CANCEL);check(port.files.size()==1,"cancel creates nothing");
        s.command(1);s.act(Action.CONFIRM);
        check("garden-0001".equals(port.opened)&&Arrays.equals(port.cart.bytes(),template),"new game uses clean template, not current edits");
        check(Arrays.equals(port.files.get("moon-garden"),original),"creating cannot replace original");
        s.refresh("moon-garden");int beforeCopy=port.creates;
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.COPY&&port.creates==beforeCopy,"copy menu only previews the operation");
        s.act(Action.TEST);check(port.creates==beforeCopy,"Start cannot bypass copy confirmation");
        s.act(Action.CANCEL);check(s.mode==LibrarySession.Mode.SHELF&&port.creates==beforeCopy,"cancel copy keeps source and file count");
        s.refresh("moon-garden");s.command(2);s.act(Action.CONFIRM);
        check("remix-0001".equals(port.opened)&&Arrays.equals(port.cart.bytes(),original),"copy preserves all source bytes");
        byte[] edited=port.cart.withValue(1,1).bytes();port.files.put(port.opened,edited);
        check(Arrays.equals(port.files.get("moon-garden"),original),"copy edit isolated from source");
        s.refresh("moon-garden");s.command(2);s.act(Action.CONFIRM);
        check("remix-0002".equals(port.opened),"controller button copy uses unique destination");
        check(Arrays.equals(port.files.get("remix-0001"),edited),"second copy cannot overwrite earlier work");
        s.refresh("garden-0001");s.act(Action.CONFIRM);
        check("garden-0001".equals(port.opened),"reopen selected project");
        s.act(Action.CANCEL);check(port.resumes==1,"back requests the parent shelf");
        port.failCreate=true;s.command(2);s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.ERROR&&port.files.size()==4&&port.opened.equals("garden-0001"),"failed write leaves library and active project unchanged");
        s.act(Action.CANCEL);port.failCreate=false;port.failOpen=true;s.command(2);s.act(Action.CONFIRM);
        check(s.mode==LibrarySession.Mode.ERROR&&port.files.containsKey("remix-0003"),"open failure retains completely created project");
        port.failOpen=false;s.act(Action.CANCEL);s.act(Action.CONFIRM);
        check(port.opened.equals("remix-0003"),"retry opens retained copy, not another duplicate");
        port.files.put("garden-0002",new byte[]{1,2,3});port.files.put("../outside",template);
        s.refresh("garden-0002");check(s.current().cart==null,"bad cart does not prevent shelf loading");
        check(s.entries().size()==6,"unsafe id is excluded");
        s.act(Action.CONFIRM);check(s.mode==LibrarySession.Mode.ERROR&&port.files.get("garden-0002").length==3,"unsupported cart preserved on open");
        s.act(Action.CANCEL);s.command(2);s.act(Action.CONFIRM);check(s.mode==LibrarySession.Mode.ERROR&&port.files.size()==7,"invalid copy cannot create a substitute template");
        check(!LibrarySession.validId(null)&&!LibrarySession.validId("../../game")&&!LibrarySession.validId("garden-0001/extra"),"storage IDs reject traversal");
        check(LibrarySession.title("remix-0012").equals("Copy 12"),"titles do not require keyboard");
        s.refresh("moon-garden");
        byte[] unusual=(new String(original,java.nio.charset.StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        port.files.put("moon-garden",unusual); // Newer on-disk version than the shelf preview.
        s.command(2);s.act(Action.CONFIRM);
        check(Arrays.equals(port.cart.bytes(),unusual),"copy reads latest saved bytes, preserving CRLF and unknown sections");
        s.refresh("moon-garden");for(int i=0;i<20;i++)s.act(Action.RIGHT);
        check(s.selected==s.entries().size()-1,"navigation reaches later pages and stops at end");
        int chosen=s.selected;s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CANCEL);
        check(s.focus==0&&s.selected==chosen,"menu cancellation retains selection");
        Port empty=new Port();LibrarySession blank=new LibrarySession(empty,template);blank.refresh(null);
        check(blank.focus==1&&blank.current()==null,"empty library directs to creation");
        blank.act(Action.CONFIRM);blank.act(Action.CONFIRM);check(empty.files.size()==1,"empty-library creation works");
        final int[] visits={0};WorkshopSession workshop=new WorkshopSession(new WorkshopCartridge(template),new WorkshopSession.Port(){
            public void save(byte[] b){}public void launch(byte[] b){}public void library(){visits[0]++;}
        });
        workshop.act(Action.CONFIRM);workshop.act(Action.RIGHT);workshop.act(Action.MENU);
        check(workshop.mode==WorkshopSession.Mode.VALUE&&visits[0]==0,"shelf cannot silently discard pending parameter draft");
        workshop.act(Action.CANCEL);workshop.act(Action.MENU);
        for(int i=0;i<4;i++)workshop.act(Action.DOWN);workshop.act(Action.CONFIRM);
        check(visits[0]==1&&workshop.mode==WorkshopSession.Mode.NAVIGATE,"shelf menu controller path restores editing context");
        System.out.println("LibraryWorkflowTest: "+checks+" checks passed");
    }
}
