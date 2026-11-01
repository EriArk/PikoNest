import art.pikoos.lab.core.VerifiedTransfer;
import java.io.*;
import java.util.*;

public class VerifiedTransferTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt action,String why)throws Exception{try{action.run();}catch(Exception e){checks++;return;}throw new AssertionError(why);}
    static class Memory implements VerifiedTransfer.Source,VerifiedTransfer.Target {
        Map<String,byte[]> data=new TreeMap<>();int creates;boolean partial,deny;
        public List<String> files(){return new ArrayList<>(data.keySet());}
        public InputStream open(String p)throws Exception{return read(p);}
        public InputStream read(String p)throws Exception{if(deny)throw new IOException("revoked");if(!data.containsKey(p))throw new IOException("missing");return new ByteArrayInputStream(data.get(p));}
        public boolean exists(String p){return data.containsKey(p);}
        public OutputStream create(String p)throws Exception{
            if(data.containsKey(p))throw new IOException("overwrite");creates++;
            return new ByteArrayOutputStream(){public void close()throws IOException{byte[] bytes=toByteArray();data.put(p,partial?Arrays.copyOf(bytes,Math.max(0,bytes.length-1)):bytes);}};
        }
    }
    static Memory source(){Memory s=new Memory();s.data.put("projects/a/game.p8","ordinary lua\n".getBytes());s.data.put("projects/a/.pikoos/notes/idea.txt","hello".getBytes());s.data.put("projects/b/game.p8",new byte[]{0,-1,13,10});return s;}
    static void copy(Memory s,Memory d)throws Exception{VerifiedTransfer.copy(s,d,()->false,(a,b,p)->{});}
    public static void main(String[] args)throws Exception{
        Memory s=source(),d=new Memory();copy(s,d);
        check(d.exists(VerifiedTransfer.COMPLETE),"receipt only after verification");
        for(String p:s.files())check(Arrays.equals(s.data.get(p),d.data.get(p)),"exact bytes and metadata");
        int writes=d.creates;copy(s,d);check(writes==d.creates,"resume verified copy without writes");
        d.data.remove("projects/b/game.p8");rejects(()->copy(s,d),"completed damaged copy cannot gain files");check(d.creates==writes,"no mutation of damaged completed copy");
        Memory partial=new Memory();partial.partial=true;rejects(()->copy(s,partial),"provider truncation");check(!partial.exists(VerifiedTransfer.COMPLETE),"no receipt for partial copy");
        partial.partial=false;int before=partial.creates;rejects(()->copy(s,partial),"resume never overwrites partial file");check(partial.creates==before,"partial preserved");
        Memory interrupted=new Memory();boolean[] cancel={false};rejects(()->VerifiedTransfer.copy(s,interrupted,()->cancel[0],(n,total,p)->cancel[0]=true),"cancel between files");
        check(!interrupted.exists(VerifiedTransfer.COMPLETE),"cancel not complete");copy(s,interrupted);check(interrupted.exists(VerifiedTransfer.COMPLETE),"resume between files");
        Memory changed=source(),dest=new Memory();rejects(()->VerifiedTransfer.copy(changed,dest,()->false,(n,total,p)->{if(n==total)changed.data.put("projects/a/game.p8",new byte[]{5});}),"source mutation after first verification");check(!dest.exists(VerifiedTransfer.COMPLETE),"final source recheck prevents false completion");
        Memory added=source(),to=new Memory();rejects(()->VerifiedTransfer.copy(added,to,()->false,(n,total,p)->added.data.put("new.p8",new byte[]{4})),"new source files");
        check(!to.exists(VerifiedTransfer.COMPLETE),"new file invalidates receipt");
        Memory revoked=new Memory();revoked.deny=true;rejects(()->copy(s,revoked),"revoked destination");check(!revoked.exists(VerifiedTransfer.COMPLETE),"access error never complete");
        for(String path:new String[]{"../a","/a","a/../b","a//b","a\\b","C:/a","a\nb","a/",""})rejects(()->VerifiedTransfer.path(path),"unsafe path");
        Memory ambiguous=source();ambiguous.data.put("PROJECTS/a/game.p8",new byte[]{0});Memory unused=new Memory();rejects(()->copy(ambiguous,unused),"case ambiguity");check(unused.creates==0,"validate before writes");
        Memory empty=new Memory(),emptyDest=new Memory();copy(empty,emptyDest);check(emptyDest.exists(VerifiedTransfer.COMPLETE),"empty library gets explicit receipt");
        Memory reserved=source();reserved.data.put(VerifiedTransfer.COMPLETE.toUpperCase(java.util.Locale.ROOT),new byte[]{0});rejects(()->copy(reserved,new Memory()),"receipt name reserved across case-sensitive and insensitive providers");
        Memory failReceipt=new Memory(){public OutputStream create(String p)throws Exception{if(p.equals(VerifiedTransfer.COMPLETE))throw new IOException("full");return super.create(p);}};
        rejects(()->copy(s,failReceipt),"receipt failure");check(!failReceipt.exists(VerifiedTransfer.COMPLETE),"receipt failure not success");
        check(s.data.size()==3&&Arrays.equals(s.data.get("projects/b/game.p8"),new byte[]{0,-1,13,10}),"all failures preserve source");
        System.out.println("VerifiedTransferTest: "+checks+" checks passed");
    }
}
