package art.pikoos.runtimeexperiment;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Signature-only bounded trial of a private cart copy with the user's official runtime. */
public final class DiagnosticActivity extends Activity {
    private volatile boolean cancelled;
    private boolean delivered;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        if(saved!=null){setResult(RESULT_OK,new Intent().putExtra("cancelled",true).putExtra("token",getIntent().getStringExtra("token")));delivered=true;finish();return;}
        Uri uri=getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if(uri==null||!uri.toString().equals("content://art.pikoos.runtimelab.carts/diagnostic.p8")){finish();return;}
        new Thread(()->runCheck(uri),"pikoos-diagnostic").start();
    }
    @Override public void onBackPressed(){cancelled=true;}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        int back=getIntent().getBooleanExtra("swapAB",false)?KeyEvent.KEYCODE_BUTTON_A:KeyEvent.KEYCODE_BUTTON_B;
        if(e.getKeyCode()==back||e.getKeyCode()==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_DOWN)cancelled=true;return true;}
        return true; // Modal: do not replay trial input into the editor underneath.
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==android.view.MotionEvent.ACTION_UP)cancelled=true;return true;}
    @Override protected void onStop(){super.onStop();if(!delivered)cancelled=true;}
    private void runCheck(Uri uri){
        File root=null;Process process=null;String log="",hash="";boolean completed=false,windowEnded=false,closed=true;
        try{
            File pkg=new File(getFilesDir(),"package");
            if(!new File(pkg,"rootfs/home/pico/pico-8/pico8_64").isFile())throw new IOException("Runtime not prepared");
            File base=new File(getFilesDir(),"pikoos-diagnostics");if(!base.isDirectory()&&!base.mkdir())throw new IOException("No check storage");
            String[] previous=base.list();if(previous==null||previous.length>=8)throw new IOException("Interrupted check storage is full");
            root=new File(base,UUID.randomUUID().toString().replace("-",""));if(!root.mkdir())throw new IOException("Cannot create check");
            File home=new File(root,"home"),tmp=new File(root,"tmp"),work=new File(root,"work");
            if(!home.mkdir()||!tmp.mkdir()||!work.mkdir())throw new IOException("Cannot prepare check");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new IOException("No cartridge");byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException("Cart too large");bytes.write(b,0,n);}
            }
            byte[] cart=bytes.toByteArray();hash=hex(java.security.MessageDigest.getInstance("SHA-256").digest(cart));
            Files.write(new File(work,"game.p8").toPath(),cart);
            if(cancelled)throw new IOException("Cancelled before start");
            // Only fixed paths and our generated hex UUID enter this shell source.
            String script="#!/system/bin/sh\nset -eu\ncd \"$1\"\nexec 9< .\nexec 8< \"$2/tmp\"\n"+
                "LD_LIBRARY_PATH=. PROOT_TMP_DIR=/proc/self/fd/8 exec ./proot --kill-on-exit --cwd=/home/pico/pico-8 "+
                "--bind=/dev --bind=/proc --bind=/sys --bind=$2/home:/check-home --bind=$2/work:/work --rootfs=./rootfs "+
                "/usr/bin/busybox env SDL_VIDEODRIVER=dummy SDL_AUDIODRIVER=dummy ./pico8_64 -x /work/game.p8 -home /check-home -root_path /work\n";
            File shell=new File(root,"check.sh");Files.write(shell.toPath(),script.getBytes("UTF-8"));
            ProcessBuilder builder=new ProcessBuilder("/system/bin/timeout","-k","1","4",new File(pkg,"busybox").getPath(),"ash",shell.getPath(),pkg.getPath(),root.getPath());
            builder.directory(pkg);builder.environment().put("LD_LIBRARY_PATH",pkg.getPath());builder.redirectErrorStream(true);
            long began=android.os.SystemClock.elapsedRealtime();process=builder.start();closed=false;
            final Process running=process;final ByteArrayOutputStream output=new ByteArrayOutputStream();final boolean[] truncated={false};
            Thread reader=new Thread(()->{try(InputStream in=running.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)synchronized(output){int keep=Math.min(n,32768-output.size());if(keep<n)truncated[0]=true;if(keep>0)output.write(b,0,keep);}}catch(IOException ignored){}},"pikoos-diagnostic-output");
            reader.setDaemon(true);reader.start();
            // timeout owns the process group even if Android kills this Activity's process.
            closed=process.waitFor(7,TimeUnit.SECONDS);
            if(!closed){process.destroy();closed=process.waitFor(1,TimeUnit.SECONDS);}
            if(!closed)throw new IOException("Runtime did not stop; files retained");
            reader.join(300);synchronized(output){log=(truncated[0]||reader.isAlive()?"Check unavailable: output incomplete\n":"")+output.toString("UTF-8");}
            completed=process.exitValue()==0;windowEnded=android.os.SystemClock.elapsedRealtime()-began>=3900;
        }catch(Exception e){log+="\nCheck unavailable: "+e.getMessage();}
        finally{
            // Never touch the runtime/rootfs or another session. Retain interrupted sessions.
            if(root!=null&&closed)try{Files.walkFileTree(root.toPath(),new SimpleFileVisitor<Path>(){
                public FileVisitResult visitFile(Path p,BasicFileAttributes a)throws IOException{Files.delete(p);return FileVisitResult.CONTINUE;}
                public FileVisitResult postVisitDirectory(Path p,IOException e)throws IOException{if(e!=null)throw e;Files.delete(p);return FileVisitResult.CONTINUE;}
            });}catch(IOException ignored){}
        }
        Intent result=new Intent().putExtra("token",getIntent().getStringExtra("token")).putExtra("log",log).putExtra("hash",hash).putExtra("completed",completed).putExtra("windowEnded",windowEnded).putExtra("cancelled",cancelled);
        runOnUiThread(()->{delivered=true;setResult(RESULT_OK,result);finish();});
    }
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
