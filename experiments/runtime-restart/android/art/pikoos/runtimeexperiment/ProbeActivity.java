package art.pikoos.runtimeexperiment;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.system.Os;
import android.widget.Toast;
import art.pikoos.lab.core.RuntimeProbe;
import java.io.*;
import java.util.UUID;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** Manifest signature permission restricts executable import to our signed development host. */
public final class ProbeActivity extends Activity {
    /** Only completed, app-owned probe sessions. Never follows links into other data. */
    private static void trimClosed(File base)throws IOException{
        File[] sessions=base.listFiles();if(sessions==null)throw new IOException("Папка проверки недоступна");
        if(sessions.length<8)return;
        java.util.Arrays.sort(sessions,java.util.Comparator.comparingLong(File::lastModified));
        int count=sessions.length;
        for(File session:sessions){
            if(count<8)break;
            if(!session.getName().matches("[0-9a-f]{32}")||!session.getCanonicalFile().equals(session.getAbsoluteFile()))continue;
            File marker=new File(session,"closed");
            if(!marker.getCanonicalFile().getParentFile().equals(session)||!marker.isFile()||marker.length()!=6)continue;
            if(!new String(Files.readAllBytes(marker.toPath()),"US-ASCII").equals("EXITED"))continue;
            Files.walkFileTree(session.toPath(),new SimpleFileVisitor<Path>(){
                public FileVisitResult visitFile(Path p,BasicFileAttributes attrs)throws IOException{Files.delete(p);return FileVisitResult.CONTINUE;}
                public FileVisitResult postVisitDirectory(Path p,IOException error)throws IOException{if(error!=null)throw error;Files.delete(p);return FileVisitResult.CONTINUE;}
            });count--;
        }
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(saved!=null){finish();return;}
        Uri uri=getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if(uri==null||!uri.toString().equals("content://art.pikoos.runtimelab.carts/runtime-probe.pikorun")){finish();return;}
        try{ProbeStatusProvider.write(this,"PREPARING");}catch(IOException error){finish();return;}
        new Thread(()->{
            File root=null;boolean prepared=false;
            try{
                File base=new File(getFilesDir(),"pikoos-probes");if(!base.isDirectory()&&!base.mkdir())throw new IOException("Не удалось создать папку проверки");
                trimClosed(base);
                String[] sessions=base.list();if(sessions==null||sessions.length>=8)throw new IOException("Накопились прерванные проверки. Нужна очистка тестовых сессий.");
                root=new File(base,UUID.randomUUID().toString().replace("-",""));if(!root.mkdir())throw new IOException("Не удалось подготовить проверку");
                try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Архив недоступен");RuntimeProbe.unpack(in,root,()->isFinishing());}
                for(String name:RuntimeProbe.FILES)Os.chmod(new File(root,name).getPath(),name.equals("pico8_64")?0500:0400);
                if(!new File(root,"probe-home").mkdir())throw new IOException("Не удалось создать данные проверки");
                final String path=new File(root,"probe.p8").getPath();ProbeStatusProvider.write(this,"READY");prepared=true;
                runOnUiThread(()->{
                    try{
                        if(isFinishing())throw new IOException("Проверка прервана");
                        startActivity(new Intent(Intent.ACTION_SEND).setClassName(this,"com.godot.game.GodotAppLauncher").setType("text/plain").putExtra(Intent.EXTRA_TEXT,path));
                    }catch(Exception error){try{ProbeStatusProvider.write(this,"FAILED");}catch(IOException ignored){}}
                    finish();
                });
            }catch(Exception error){try{ProbeStatusProvider.write(this,"FAILED");}catch(IOException ignored){}runOnUiThread(()->{Toast.makeText(this,"Не удалось подготовить PICO-8. Вернись и повтори проверку.",Toast.LENGTH_LONG).show();finish();});}
            finally{
                if(!prepared&&root!=null){for(String name:RuntimeProbe.FILES)new File(root,name).delete();new File(root,"probe-home").delete();root.delete();}
            }
        },"pikoos-probe-import").start();
    }
}
