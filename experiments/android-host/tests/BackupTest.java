import art.pikoos.lab.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class BackupTest {
    static int checks;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Work{void run()throws Exception;}static void reject(Work work,String why)throws Exception{try{work.run();throw new AssertionError(why);}catch(IOException expected){checks++;}}
    static void put(Path root,String path,String text)throws Exception{Path f=root.resolve(path);Files.createDirectories(f.getParent());Files.write(f,text.getBytes("UTF-8"));}
    static String get(Path root,String path)throws Exception{return new String(Files.readAllBytes(root.resolve(path)),"UTF-8");}
    static void remove(Path root)throws Exception{try(java.util.stream.Stream<Path> files=Files.walk(root)){for(Path p:(Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
    static void rewrite(File original,File next,String extra,boolean damage)throws Exception{
        try(ZipFile in=new ZipFile(original);ZipOutputStream out=new ZipOutputStream(new FileOutputStream(next))){for(Enumeration<? extends ZipEntry> it=in.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();out.putNextEntry(new ZipEntry(e.getName()));byte[] b;try(InputStream stream=in.getInputStream(e);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] block=new byte[4096];for(int n;(n=stream.read(block))!=-1;)bytes.write(block,0,n);b=bytes.toByteArray();}if(damage&&e.getName().endsWith("game.p8"))b[0]^=1;out.write(b);out.closeEntry();}if(extra!=null){out.putNextEntry(new ZipEntry(extra));out.write(1);out.closeEntry();}}
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("pikonest-backup-");try{
            Path source=Files.createDirectory(temp.resolve("source"));put(source,"projects/blank/game.p8","pico-8 cartridge\nversion 42\n__lua__\nprint('hello')\n");put(source,"projects/blank/.pikoos/note","keep me");put(source,"library/asset-library/custom.bin","unfamiliar library metadata");put(source,"runtime/data/cdata/save.p8d","save bytes");
            WorkshopCartridge authored=StarterGames.create(false);WorkshopSession session=new WorkshopSession(authored,new WorkshopSession.Port(){public void save(byte[] b){}public void launch(byte[] b){}});session.openGame();session.game.openWorld();session.game.world.point(3,3);session.game.world.brush.tile=100;session.game.world.paint();byte[] draft=session.game.encode(),candidate=session.game.candidate(authored).bytes();Files.write(source.resolve("projects/blank/game.p8"),authored.bytes());
            Map<String,Object> preferences=new TreeMap<>();preferences.put("name","猫 & < >");preferences.put("flag",true);preferences.put("n",7);preferences.put("long",99999999999L);preferences.put("float",1.5f);preferences.put("favorites",new HashSet<>(Arrays.asList("a","b")));preferences.put("gameCanvas",Base64.getEncoder().encodeToString(draft));byte[] encoded=BackupPreferences.encode(preferences);check(BackupPreferences.decode(encoded).equals(preferences),"all preference types round-trip");
            reject(()->BackupPreferences.decode(Arrays.copyOf(encoded,encoded.length-1)),"truncated preferences refused");reject(()->BackupPreferences.decode(Arrays.copyOf(encoded,encoded.length+1)),"trailing preferences refused");
            Files.createDirectories(source.resolve("preferences"));Files.write(source.resolve("preferences/blank-ui.pkpf"),encoded);
            File zip=temp.resolve("backup.zip").toFile();BackupArchive.Report report=BackupArchive.write(BackupArchive.directory(source.toFile()),zip,()->false);check(report.files==5,"all current data sections included");
            File extraction=temp.resolve("restore").toFile();check(BackupArchive.verify(zip,extraction,()->false).bytes==report.bytes,"restore checked against manifest");check(RuntimeHomeMigration.scan(source.toFile(),()->false).equals(RuntimeHomeMigration.scan(extraction,()->false)),"exact carts metadata settings library saves");
            WorkshopCartridge restoredCart=new WorkshopCartridge(Files.readAllBytes(new File(extraction,"projects/blank/game.p8").toPath()));Map<String,Object> restoredPrefs=BackupPreferences.decode(Files.readAllBytes(new File(extraction,"preferences/blank-ui.pkpf").toPath()));GameCanvas restoredDraft=GameCanvas.restore(Base64.getDecoder().decode((String)restoredPrefs.get("gameCanvas")),restoredCart);check(Arrays.equals(candidate,restoredDraft.candidate(restoredCart).bytes()),"real unfinished world draft restores against its exact archived cart");check(Arrays.equals(authored.bytes(),restoredCart.bytes()),"backup does not commit the unfinished draft");
            reject(()->BackupArchive.verify(zip,extraction,()->false),"cannot overwrite existing restore folder");
            for(String extra:new String[]{"../escape","projects/../escape","/absolute","projects/blank/GAME.P8","projects/blank","unknown/file","projects/unlisted.p8"}){File bad=temp.resolve("bad-"+checks+".zip").toFile();rewrite(zip,bad,extra,false);reject(()->BackupArchive.verify(bad,null,()->false),"invalid paths or unexpected extra data refused");}
            File damaged=temp.resolve("damaged.zip").toFile();rewrite(zip,damaged,null,true);reject(()->BackupArchive.verify(damaged,null,()->false),"changed payload refused even with valid ZIP CRC");
            try{BackupArchive.verify(zip,null,()->true);throw new AssertionError("cancellation ignored");}catch(java.util.concurrent.CancellationException ok){checks++;}
            VerifiedTransfer.Source changing=new VerifiedTransfer.Source(){int opens;public List<String> files(){return Arrays.asList("projects/blank/game.p8");}public InputStream open(String n){return new ByteArrayInputStream(new byte[]{(byte)++opens});}};
            reject(()->BackupArchive.write(changing,temp.resolve("changed.zip").toFile(),()->false),"changing source cannot become a successful backup");
            // Crash after each rename, then either finish or roll back. Includes absent -> file,
            // file -> absent, directory replacement and different preference generations.
            for(boolean rollback:new boolean[]{false,true})for(int fail=0;fail<=10;fail++){
                Path data=Files.createDirectory(temp.resolve("data-"+rollback+fail)),stage=Files.createDirectory(temp.resolve("stage-"+rollback+fail));
                put(data,"files/projects/blank/game.p8","old-cart");put(data,"shared_prefs/blank-ui.xml","old-draft");put(data,"files/runtime-home.txt","old-pointer");
                put(data,"files/project-location.json.bak","stale-external-location");put(data,"files/runtime-home.txt.new","interrupted-pointer");
                put(stage,"next/files/projects/blank/game.p8","new-cart");put(stage,"next/shared_prefs/blank-ui.xml","new-draft");put(stage,"next/files/sprite-buffer.pksp","new-buffer");
                BackupSwap.prepare(data.toFile(),stage.toFile());BackupSwap.validatePrepared(data.toFile(),stage.toFile());final int stop=fail;
                try{if(fail>0)BackupSwap.apply(data.toFile(),stage.toFile(),false,n->{if(n==stop)throw new IOException("simulated process loss");});}catch(IOException expected){}
                BackupSwap.apply(data.toFile(),stage.toFile(),rollback,n->{});BackupSwap.apply(data.toFile(),stage.toFile(),rollback,n->{});
                check(get(data,"files/projects/blank/game.p8").equals(rollback?"old-cart":"new-cart"),"cart recovered as a whole version");check(get(data,"shared_prefs/blank-ui.xml").equals(rollback?"old-draft":"new-draft"),"matching draft generation restored");check(Files.exists(data.resolve("files/runtime-home.txt"))==rollback,"removed pointer reverses correctly");check(Files.exists(data.resolve("files/sprite-buffer.pksp"))!=rollback,"new-only resource reverses correctly");
                check(Files.exists(data.resolve("files/project-location.json.bak"))==rollback,"stale AtomicFile backup cannot reactivate external storage");check(Files.exists(data.resolve("files/runtime-home.txt.new"))==rollback,"interrupted AtomicFile candidate retained outside active data");
            }
            Path data=Files.createDirectory(temp.resolve("conflict-data")),stage=Files.createDirectory(temp.resolve("conflict-stage"));put(data,"files/projects/blank/game.p8","old");put(stage,"next/files/projects/blank/game.p8","new");BackupSwap.prepare(data.toFile(),stage.toFile());put(data,"files/projects/blank/game.p8","external-change");
            reject(()->BackupSwap.validatePrepared(data.toFile(),stage.toFile()),"stale review refused before scheduling");reject(()->BackupSwap.apply(data.toFile(),stage.toFile(),false,n->{}),"unrecognized current data never overwritten");check(get(data,"files/projects/blank/game.p8").equals("external-change"),"conflict preserves user edit");
        }finally{remove(temp);}System.out.println("BackupTest: "+checks+" checks passed");
    }
}
