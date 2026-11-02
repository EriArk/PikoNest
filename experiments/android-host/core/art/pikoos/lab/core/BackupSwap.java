package art.pikoos.lab.core;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Restart-only restore transaction. Renames retain both old and restored data; no deletion. */
public final class BackupSwap {
    public static final String[] TARGETS=targets();
    private static String[] targets(){
        List<String> paths=new ArrayList<>(Arrays.asList("files/projects","files/asset-library","files/runtime-data","files/project-save-journals","files/library-save-journals","shared_prefs"));
        // AtomicFile may prefer an old .bak over the base on the next read. Move the
        // complete family so an interrupted pre-restore write cannot reactivate it.
        for(String name:new String[]{"sprite-buffer.pksp","runtime-home.txt","project-location.json","library-location.json","pending-import.bin","pending-export.bin"})
            for(String suffix:new String[]{"",".bak",".new"})paths.add("files/"+name+suffix);
        return paths.toArray(new String[0]);
    }
    public interface Fault {void afterRename(int count)throws IOException;}
    private static File safe(File root,String path)throws IOException{VerifiedTransfer.path(path);File f=new File(root.getCanonicalFile(),path);if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Linked restore target");return f;}
    private static String stamp(File f)throws IOException{
        if(!f.exists())return "-";if(Files.isSymbolicLink(f.toPath()))throw new IOException("Linked restore source");
        if(f.isFile()){RuntimeHomeMigration.Entry e=RuntimeHomeMigration.fingerprint(f,()->false);return "F:"+e.bytes+":"+e.hash;}
        if(!f.isDirectory())throw new IOException("Unknown restore file type");
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");
            for(Map.Entry<String,RuntimeHomeMigration.Entry> e:RuntimeHomeMigration.scan(f,()->false).entrySet())digest.update((e.getKey()+"\n"+e.getValue().bytes+" "+e.getValue().hash+"\n").getBytes("UTF-8"));
            StringBuilder s=new StringBuilder("D:");for(byte b:digest.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
        }catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
    public static void prepare(File data,File stage)throws IOException{
        File plan=new File(stage,"plan.bin");if(plan.exists())throw new IOException("Restore plan already exists");
        try(FileOutputStream raw=new FileOutputStream(plan);DataOutputStream out=new DataOutputStream(raw)){
            out.writeUTF("PikoNest restore 1");out.writeInt(TARGETS.length);
            for(String path:TARGETS){out.writeUTF(path);out.writeUTF(stamp(safe(data,path)));out.writeUTF(stamp(safe(new File(stage,"next"),path)));}
            out.flush();raw.getFD().sync();
        }
    }
    /** Can resume after any rename, including failures reported after the rename took effect. */
    public static void validatePrepared(File data,File stage)throws IOException{
        try(DataInputStream in=new DataInputStream(new FileInputStream(new File(stage,"plan.bin")))){
            if(!in.readUTF().equals("PikoNest restore 1")||in.readInt()!=TARGETS.length)throw new IOException("Unknown restore plan");
            for(String path:TARGETS){if(!in.readUTF().equals(path))throw new IOException("Invalid restore target");String old=in.readUTF(),next=in.readUTF();if(!old.equals(stamp(safe(data,path)))||!next.equals(stamp(safe(new File(stage,"next"),path))))throw new IOException("Work changed after restore review. Open the backup again; current work kept.");}
            if(in.read()!=-1)throw new IOException("Trailing restore plan data");
        }
    }
    public static void apply(File data,File stage,boolean rollback,Fault fault)throws IOException{
        List<String[]> rows=new ArrayList<>();try(DataInputStream in=new DataInputStream(new FileInputStream(new File(stage,"plan.bin")))){
            if(!in.readUTF().equals("PikoNest restore 1")||in.readInt()!=TARGETS.length)throw new IOException("Unknown restore plan");
            for(String path:TARGETS){String stored=in.readUTF(),old=in.readUTF(),next=in.readUTF();if(!stored.equals(path)||!valid(old)||!valid(next))throw new IOException("Invalid restore plan");rows.add(new String[]{path,old,next});}
            if(in.read()!=-1)throw new IOException("Trailing restore plan data");
        }
        if(rollback)Collections.reverse(rows);int moved=0;
        for(String[] row:rows){
            File current=safe(data,row[0]),before=safe(new File(stage,rollback?"next":"before"),row[0]),next=safe(new File(stage,rollback?"before":"next"),row[0]);
            String a=row[rollback?2:1],b=row[rollback?1:2],now=stamp(current),saved=stamp(before),candidate=stamp(next);
            if(a.equals(b)&&now.equals(a))continue;
            if(now.equals(b)&&saved.equals(a)&&candidate.equals("-"))continue;
            if(now.equals(a)&&saved.equals("-")&&candidate.equals(b)){
                if(!a.equals("-")){move(current,before);fault.afterRename(++moved);}
                now="-";saved=a;
            }
            if(!now.equals("-")||!saved.equals(a)||!candidate.equals(b))throw new IOException("Restore conflict at "+row[0]+". Both versions kept; do not edit files.");
            if(!b.equals("-")){move(next,current);fault.afterRename(++moved);}
            if(!stamp(current).equals(b))throw new IOException("Restored data did not verify");
        }
    }
    private static boolean valid(String s){return s.equals("-")||s.matches("F:[0-9]+:[0-9a-f]{64}|D:[0-9a-f]{64}");}
    private static void move(File from,File to)throws IOException{
        if(to.exists())throw new IOException("Restore destination exists");if(!to.getParentFile().isDirectory()&&!to.getParentFile().mkdirs())throw new IOException("Cannot create restore directory");
        Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE);
    }
}
