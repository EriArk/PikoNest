package art.pikoos.runtimelab;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import art.pikoos.lab.core.*;
import java.io.*;
import java.util.*;

/** Whole-workspace backup/recovery. Staged restore needs a separate explicit confirmation. */
public final class BackupActivity extends Activity {
    static final int SAVE=94,OPEN=95;private BackupView view;private ControllerInput input;
    private static Job current;private final Handler handler=new Handler();private int selected;private boolean recovery;
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,300);}};
    static final class Job {volatile boolean busy,cancelled,applying;volatile String message="";volatile File stage;volatile BackupArchive.Report report;}
    static boolean inProgress(){return current!=null&&current.busy;}
    @Override public void onCreate(Bundle state){super.onCreate(state);recovery=getIntent().getBooleanExtra("recovery",false)||!BackupRecovery.problem.isEmpty()||BackupRecovery.pending(this)||BackupRecovery.needsRestart;view=new BackupView(this,this::action);input=new ControllerInput(view::act,()->!recovery&&getSharedPreferences("library-ui",0).getBoolean("swapAB",false));setContentView(view);view.requestFocus();render();if(recovery&&current==null&&BackupRecovery.pending(this))resumeRecovery();}
    private void resumeRecovery(){Job job=new Job();current=job;job.busy=true;job.applying=true;job.message="Restoring verified data. Interrupted restores can resume.";Context c=getApplicationContext();new Thread(()->{BackupRecovery.recover(c);job.message=BackupRecovery.problem;job.busy=false;handler.post(()->{if(!isFinishing()&&!isDestroyed())render();});},"pikonest-restore").start();}
    private void idle()throws Exception{
        ExternalPicoBackend b=new ExternalPicoBackend(this);if(b.hasSession()&&!RuntimeSession.ended(b.sessionPhase()))throw new IOException("Finish the current game before backing up or restoring.");
        if(RuntimeMigrationJob.pending()||RuntimeArchiveJob.current!=null&&!RuntimeArchiveJob.current.done||ProjectStorage.activating||LibraryStorage.activating)throw new IOException("Finish setup or storage work first.");
    }
    void action(WorkshopSession.Action a){
        if(current!=null&&current.busy){if(a==WorkshopSession.Action.CANCEL&&!current.applying)current.cancelled=true;return;}
        if(recovery&&BackupRecovery.needsRestart&&(a==WorkshopSession.Action.CONFIRM||a==WorkshopSession.Action.CANCEL)){restart();return;}
        if(a==WorkshopSession.Action.CANCEL){if(current!=null&&current.stage!=null){current=null;render();}else finish();return;}
        if(a==WorkshopSession.Action.UP)selected=Math.max(0,selected-1);if(a==WorkshopSession.Action.DOWN)selected=Math.min(view.options.length-1,selected+1);
        if(a==WorkshopSession.Action.CONFIRM){try{
            if(recovery){if(selected==2){finish();return;}String[] request=new String(BackupRecovery.pointer(this).readFully(),"US-ASCII").split("\n");BackupRecovery.schedule(this,new File(BackupRecovery.root(this),request[0]),selected==1);resumeRecovery();return;}
            if(current!=null&&current.stage!=null){if(selected==0){idle();scheduleRestore();}else if(selected==1){current=null;selected=0;}else finish();}
            else if(selected==2)finish();else{idle();Intent i=new Intent(selected==0?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(selected==0?"application/zip":"*/*");if(selected==0)i.putExtra(Intent.EXTRA_TITLE,"PikoNest-backup-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".zip");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(i,selected==0?SAVE:OPEN);}
        }catch(Exception e){current=new Job();current.message=e.getMessage();}}render();
    }
    private void scheduleRestore(){Job job=current;job.busy=true;job.message="Checking current work before restore...";Context c=getApplicationContext();new Thread(()->{try{BackupSwap.validatePrepared(new File(c.getApplicationInfo().dataDir),job.stage);RuntimeArchive.check(()->job.cancelled);BackupRecovery.schedule(c,job.stage,false);handler.post(()->restart());}catch(Exception e){if(BackupRecovery.pending(c)){handler.post(()->restart());return;}job.message=e.getMessage();job.stage=null;job.busy=false;}},"pikonest-restore-confirm").start();}
    private void restart(){finishAffinity();android.os.Process.killProcess(android.os.Process.myPid());}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=SAVE&&request!=OPEN)return;if(result!=RESULT_OK||data==null||data.getData()==null){if(current==null)current=new Job();current.message="Cancelled. Existing work kept.";render();return;}
        try{idle();}catch(Exception e){current=new Job();current.message=e.getMessage();render();return;}
        Job job=new Job();current=job;job.busy=true;job.message=request==SAVE?"Creating and verifying backup...":"Checking backup before restore...";Uri uri=data.getData();Context c=getApplicationContext();
        new Thread(()->{File stage=null;try{
            BackupRecovery.root(c).mkdirs();stage=new File(BackupRecovery.root(c),UUID.randomUUID().toString());if(!stage.mkdir())throw new IOException("Cannot create backup staging folder");File zip=new File(stage,"backup.zip");
            if(request==SAVE){
                BackupArchive.Report report=BackupArchive.write(BackupData.source(c),zip,()->job.cancelled);copy(new FileInputStream(zip),c.getContentResolver().openOutputStream(uri,"wt"),job);
                File reread=new File(stage,"readback.zip");copy(c.getContentResolver().openInputStream(uri),new FileOutputStream(reread),job);BackupArchive.verify(reread,null,()->job.cancelled);
                if(!RuntimeArchive.hash(zip,()->job.cancelled).equals(RuntimeArchive.hash(reread,()->job.cancelled)))throw new IOException("Saved ZIP differs from the checked copy");
                job.message="Backup verified: "+report.files+" files. Keep the ZIP outside the app before uninstalling.";
            }else{
                copy(c.getContentResolver().openInputStream(uri),new FileOutputStream(zip),job);File extracted=new File(stage,"extracted");job.report=BackupArchive.verify(zip,extracted,()->job.cancelled);
                RuntimeArchive.check(()->job.cancelled);BackupData.prepareRestore(c,extracted,stage);RuntimeArchive.check(()->job.cancelled);job.stage=stage;
                job.message="Verified "+job.report.files+" files. Restore replaces active work with an internal copy. Current private data is retained for recovery; external folders stay unchanged. PikoNest closes for restoration; reopen it, then use Finish and open it once more to load restored settings. Reconnect Games and import PICO-8 if needed.";
            }
        }catch(java.util.concurrent.CancellationException e){job.message="Cancelled. Current data kept; incomplete backup candidates retained.";}catch(Exception e){job.message="Not completed: "+e.getMessage()+". Current work kept; candidate retained.";}finally{job.busy=false;}},"pikonest-backup").start();selected=0;render();
    }
    private static void copy(InputStream input,OutputStream output,Job job)throws Exception{
        try(InputStream in=input;OutputStream out=output){if(in==null||out==null)throw new IOException("Cannot open backup document");long n=0;byte[] b=new byte[65536];for(int read;(read=in.read(b))!=-1;){RuntimeArchive.check(()->job.cancelled);n+=read;if(n>BackupArchive.MAX_BYTES+8L*1024*1024)throw new IOException("Backup exceeds 520 MiB ZIP limit");out.write(b,0,read);}out.flush();if(out instanceof FileOutputStream)((FileOutputStream)out).getFD().sync();}
    }
    private void render(){if(view==null)return;view.selected=selected;view.busy=current!=null&&current.busy;
        view.message=BackupRecovery.needsRestart?"Restore complete. Close PikoNest and open it again to load the restored settings. Reconnect Games; your projects and library now use the restored internal copy.":current!=null&&current.busy?current.message:recovery?BackupRecovery.problem:current==null?"Back up projects, reusable assets, unfinished edits, workshop settings, saved games and downloaded carts. The purchased runtime and Android folder permissions are not included.":current.message;
        view.options=BackupRecovery.needsRestart?new String[]{"Finish and close"}:recovery?new String[]{"Retry restore","Restore previous data","Close"}:current!=null&&current.stage!=null?new String[]{"Restore and close","Keep current work","Back"}:new String[]{"Create backup ZIP","Restore from backup ZIP","Back"};selected=Math.min(selected,view.options.length-1);view.selected=selected;view.cancellable=current==null||!current.applying;view.invalidate();}
    @Override protected void onResume(){super.onResume();handler.post(refresh);getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);if(input!=null)input.reset();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){action(WorkshopSession.Action.CANCEL);}
}
