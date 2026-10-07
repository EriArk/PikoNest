import art.pikoos.lab.core.RuntimeHomeMigration;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Preservation, conflict choices, interruption and changed-source rejection. */
public final class RuntimeHomeMigrationTest {
    static int checks;static final java.util.function.BooleanSupplier running=()->false;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("migration "+checks);}
    interface Work{void run()throws Exception;}
    static void refuses(Work work)throws Exception{try{work.run();throw new AssertionError("accepted unsafe operation");}catch(IOException|IllegalArgumentException|java.util.concurrent.CancellationException expected){checks++;}}
    static void put(File root,String name,String data)throws Exception{File f=new File(root,name);f.getParentFile().mkdirs();Files.write(f.toPath(),data.getBytes("UTF-8"));}
    static String read(File root,String name)throws Exception{return new String(Files.readAllBytes(new File(root,name).toPath()),"UTF-8");}
    public static void main(String[] args)throws Exception{
        File tmp=Files.createTempDirectory("pikonest-runtime-data-test-").toFile();
        try{
            File old=new File(tmp,"old"),source=new File(tmp,"source");
            put(old,"data/config.txt","current");put(old,"data/cdata/score.p8d","new score");put(old,"data/carts/shared.p8","same");put(old,"themes/custom.cfg","theme");
            put(source,"config.txt","imported");put(source,"cdata/score.p8d","old score");put(source,"carts/shared.p8","same");put(source,"bbs/cache/42.p8.png","downloaded");
            Map<String,RuntimeHomeMigration.Entry> baseline=RuntimeHomeMigration.scan(old,running),original=RuntimeHomeMigration.scan(source,running);
            RuntimeHomeMigration.Plan plan=new RuntimeHomeMigration.Plan(baseline,original);
            check(plan.added.size()==1);check(plan.same.size()==1);check(plan.conflicts.size()==2);
            File keep=new File(tmp,"keep");RuntimeHomeMigration.prepare(old,source,keep,plan,false,running);
            check(read(keep,"data/config.txt").equals("current"));check(read(keep,"data/cdata/score.p8d").equals("new score"));check(read(keep,"data/bbs/cache/42.p8.png").equals("downloaded"));check(read(keep,"themes/custom.cfg").equals("theme"));
            File use=new File(tmp,"use");RuntimeHomeMigration.prepare(old,source,use,plan,true,running);
            check(read(use,"data/config.txt").equals("imported"));check(read(use,"data/cdata/score.p8d").equals("old score"));
            check(RuntimeHomeMigration.scan(old,running).equals(baseline));check(RuntimeHomeMigration.scan(source,running).equals(original));
            refuses(()->RuntimeHomeMigration.prepare(old,source,keep,plan,true,running));
            refuses(()->RuntimeHomeMigration.prepare(old,source,new File(tmp,"cancel"),plan,true,()->true));check(RuntimeHomeMigration.scan(old,running).equals(baseline));
            put(source,"config.txt","changed during review");refuses(()->RuntimeHomeMigration.prepare(old,source,new File(tmp,"changed-source"),plan,true,running));check(!new File(tmp,"changed-source").exists());
            put(source,"config.txt","imported");put(old,"data/config.txt","changed current");refuses(()->RuntimeHomeMigration.prepare(old,source,new File(tmp,"changed-current"),plan,true,running));
            for(String bad:new String[]{"../config.txt","/tmp/data","cdata/../score","cdata\\score","a//b","a:/b","a/","a\n"})refuses(()->RuntimeHomeMigration.path(bad));
            Map<String,RuntimeHomeMigration.Entry> clash=new TreeMap<>(original);clash.put("cdata",original.get("config.txt"));refuses(()->new RuntimeHomeMigration.Plan(baseline,clash));
            clash.remove("cdata");clash.put("CONFIG.TXT",original.get("config.txt"));refuses(()->new RuntimeHomeMigration.Plan(baseline,clash));
            System.out.println("Runtime home migration: "+checks+" checks passed");
        }finally{try(java.util.stream.Stream<Path> files=Files.walk(tmp.toPath())){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new UncheckedIOException(e);}});}}
    }
}
