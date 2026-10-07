package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Controller-first import of saves, carts and runtime configuration, independent of projects. */
public final class RuntimeDataActivity extends Activity {
    private static final int PICK=72;RuntimeMigrationJob job;RuntimeDataView view;ControllerInput input;
    boolean importedWins;int conflictIndex;String notice="";private final RuntimeMigrationJob.Listener listener=()->{if(view!=null)view.invalidate();};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(saved!=null){importedWins=saved.getBoolean("importedWins");conflictIndex=saved.getInt("conflictIndex");}
        view=new RuntimeDataView(this);input=new ControllerInput(this::action,()->getSharedPreferences("library-ui",0).getBoolean("swapAB",false));
        setContentView(view);view.requestFocus();if(RuntimeMigrationJob.current!=null)attach(RuntimeMigrationJob.current);immersive();
    }
    void action(Action action){
        if(action==Action.CANCEL){if(job!=null&&RuntimeMigrationJob.pending()){job.cancel();if(job.busy)return;}finish();return;}
        if(job!=null&&job.busy)return;
        if(job!=null&&job.ready&&!job.cancelled){
            if(action==Action.LEFT||action==Action.RIGHT)importedWins=!importedWins;
            else if((action==Action.UP||action==Action.DOWN)&&!job.plan.conflicts.isEmpty())conflictIndex=Math.floorMod(conflictIndex+(action==Action.DOWN?1:-1),job.plan.conflicts.size());
            else if(action==Action.CONFIRM){job.apply(importedWins);}view.invalidate();return;
        }
        if(action==Action.CONFIRM){if(job!=null&&job.applied){finish();return;}pick();}
    }
    private void pick(){
        try{new ExternalPicoBackend(this).checkAvailableForLaunch();
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),PICK);
        }catch(Exception e){notice=e.getMessage();view.invalidate();}
    }
    private void attach(RuntimeMigrationJob next){if(job!=null)job.detach(listener);job=next;job.attach(listener);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=PICK)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){notice="Cancelled / current data kept";view.invalidate();return;}
        try{new ExternalPicoBackend(this).checkAvailableForLaunch();getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);notice="";attach(RuntimeMigrationJob.start(this,data.getData()));}
        catch(Exception e){notice=e.getMessage();view.invalidate();}
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(input!=null)input.reset();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("importedWins",importedWins);b.putInt("conflictIndex",conflictIndex);}
    @Override protected void onDestroy(){if(job!=null)job.detach(listener);super.onDestroy();}
    @Override protected void onPause(){if(input!=null)input.reset();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){action(Action.CANCEL);}
}
