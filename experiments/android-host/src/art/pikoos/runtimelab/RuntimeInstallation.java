package art.pikoos.runtimelab;

import android.content.Context;
import android.content.pm.PackageManager;
import android.system.Os;
import art.pikoos.lab.core.RuntimeArchive;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Android adapter installation. Purchased files never become APK assets. */
final class RuntimeInstallation {
    static boolean integrated(Context c){
        try{return c.getPackageManager().getApplicationInfo(c.getPackageName(),PackageManager.GET_META_DATA).metaData.getBoolean("art.pikoos.integrated",false);}
        catch(Exception absent){return false;}
    }
    static boolean ready(Context c){return new File(c.getFilesDir(),"package/pikonest-ready").isFile();}
    static File prepare(Context c,RuntimeArchiveJob.Saved saved,BooleanSupplier cancelled)throws Exception{
        if(!integrated(c))throw new IOException("Integrated runtime is unavailable");
        File zipFile=new File(RuntimeArchiveJob.directory(c),saved.file);
        RuntimeArchive.Result checked=RuntimeArchive.inspect(zipFile,cancelled);
        if(!saved.hash.equals(checked.sha256))throw new IOException("Saved archive changed. Select the original ZIP again.");
        File marker=new File(c.getFilesDir(),"package/pikonest-ready");
        String identity="pikonest-runtime-1\n"+saved.hash;
        if(marker.isFile()&&new String(Files.readAllBytes(marker.toPath()),"US-ASCII").equals(identity))return null;
        // PulseAudio uses a pathname Unix socket (108-byte sockaddr_un limit).
        // Keep the canonical candidate path short; a long symlink target still fails.
        File installations=new File(c.getFilesDir(),"rt");
        if(!installations.isDirectory()&&!installations.mkdir())throw new IOException("Cannot create runtime storage");
        String[] entries=installations.list();if(entries==null||entries.length>=8)throw new IOException("Runtime installation storage needs review; previous installations were kept.");
        File stage=new File(installations,UUID.randomUUID().toString().replace("-","").substring(0,12));
        if(new File(stage,"package/tmp/pulse/pulse").getPath().getBytes("UTF-8").length>=108)throw new IOException("Runtime storage path is too long for its audio socket");
        if(!stage.mkdir())throw new IOException("Cannot prepare runtime installation");
        // Only our hash-pinned build payload is expanded. User ZIP paths are never extracted.
        File bootstrap=new File(stage,"bootstrap.tar.gz");
        try(InputStream in=c.getAssets().open("package.dat");FileOutputStream out=new FileOutputStream(bootstrap)){
            RuntimeArchive.copy(in,out,cancelled);out.getFD().sync();
        }
        Process unpack=new ProcessBuilder("/system/bin/tar","--no-same-owner","-xzf",bootstrap.getPath(),"-C",stage.getPath())
            .redirectErrorStream(true).redirectOutput(new File(stage,"unpack.log")).start();
        if(!unpack.waitFor(60,TimeUnit.SECONDS)){unpack.destroy();throw new IOException("Runtime preparation timed out; previous installation kept");}
        if(unpack.exitValue()!=0)throw new IOException("Cannot unpack runtime support; previous installation kept");
        RuntimeArchive.check(cancelled);
        File pkg=new File(stage,"package"),destination=new File(pkg,"rootfs/home/pico/pico-8");
        if(!new File(pkg,"busybox").isFile()||!new File(pkg,"proot").isFile()||!destination.getParentFile().isDirectory())throw new IOException("Incomplete runtime support package");
        if(!destination.isDirectory()&&!destination.mkdir())throw new IOException("Cannot create purchased runtime directory");
        try(ZipFile zip=new ZipFile(zipFile)){
            for(String name:new String[]{"pico8_64","pico8.dat"}){
                File target=new File(destination,name);
                if(target.exists())throw new IOException("Runtime support unexpectedly contains purchased data");
                try(InputStream in=zip.getInputStream(zip.getEntry("pico-8/"+name));FileOutputStream out=new FileOutputStream(target)){
                    RuntimeArchive.copy(in,out,cancelled);out.getFD().sync();
                }
                Os.chmod(target.getPath(),name.equals("pico8_64")?0500:0400);
            }
        }
        for(String relative:new String[]{"runtime-data/logs","runtime-data/data/carts","runtime-data/data/screenshots"}){
            File dir=new File(c.getFilesDir(),relative);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot prepare runtime home");
        }
        Files.copy(new File(pkg,"pulse.pa.sles").toPath(),new File(pkg,"pulse.pa").toPath(),StandardCopyOption.REPLACE_EXISTING);
        Files.write(new File(pkg,"pikonest-ready").toPath(),identity.getBytes("US-ASCII"));
        RuntimeArchive.check(cancelled);
        bootstrap.delete();
        return pkg;
    }
    /** Caller holds the archive job's cancel/commit lock. */
    static void activate(Context c,File pkg)throws Exception{
        if(pkg==null)return;
        // Same-filesystem atomic symlink replacement retains the previous package on failure.
        File link=new File(c.getFilesDir(),"package"),next=new File(pkg.getParentFile(),"activate");
        if(link.exists()&&!Files.isSymbolicLink(link.toPath()))throw new IOException("Existing runtime requires migration; no files were replaced");
        Os.symlink(pkg.getPath(),next.getPath());
        Os.rename(next.getPath(),link.getPath());
    }
}
