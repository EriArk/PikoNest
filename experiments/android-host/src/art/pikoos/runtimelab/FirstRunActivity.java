package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.*;
import art.pikoos.lab.core.FirstRunSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Optional entry guide. Only a successfully verified games tree becomes active here. */
public final class FirstRunActivity extends Activity {
    private static final int PICK=87;
    private FirstRunSetup state;private FirstRunView view;private ControllerInput input;
    private SharedPreferences progress,folders;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);progress=getSharedPreferences("first-run",0);folders=getSharedPreferences("folder-setup",0);
        state=new FirstRunSetup(new FirstRunSetup.Port(){
            public void save(int step,boolean dismissed)throws Exception{
                int oldStep=progress.getInt("step",0);boolean oldDismissed=progress.getBoolean("dismissed",false);
                if(!progress.edit().putInt("step",step).putBoolean("dismissed",dismissed).commit()){
                    progress.edit().putInt("step",oldStep).putBoolean("dismissed",oldDismissed).commit();
                    throw new Exception("Cannot save setup progress");
                }
            }
            public void games(){
                try{
                    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                    String previous=folders.getString("GAMES.uri","");
                    if(!previous.isEmpty())intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,Uri.parse(previous));
                    startActivityForResult(intent,PICK);
                }catch(Exception e){state.problem="Cannot open the Android folder picker. Try again.";}
            }
            public void runtime(){startActivity(new Intent(FirstRunActivity.this,RuntimeSetupActivity.class));}
            public void leave(){finish();}
        },saved==null?progress.getInt("step",0):saved.getInt("step",0));
        boolean swap=getSharedPreferences("library-ui",0).getBoolean("swapAB",false);
        view=new FirstRunView(this,state,swap);input=new ControllerInput(view::action,()->swap);
        setContentView(view);view.requestFocus();
        String location=folders.getString("GAMES.uri","");state.gamesName=folders.getString("GAMES.name","");
        if(!location.isEmpty())check(Uri.parse(location),false);
    }
    private void check(Uri uri,boolean replace){
        long request=state.beginCheck();view.invalidate();
        android.content.ContentResolver resolver=getApplicationContext().getContentResolver();
        new Thread(()->{
            String name="",problem=null;
            try{name=FolderAccess.verify(resolver,uri,false);}
            catch(Exception error){problem=error.getMessage()==null?"Cannot read this folder. Reconnect it or choose another.":error.getMessage();}
            final String checkedName=name,checkedProblem=problem;
            runOnUiThread(()->{
                if(isDestroyed()||isFinishing()||!state.accepts(request))return;
                String failure=checkedProblem;
                if(failure==null&&replace){
                    String oldUri=folders.getString("GAMES.uri",""),oldName=folders.getString("GAMES.name","");
                    if(!folders.edit().putString("GAMES.uri",uri.toString()).putString("GAMES.name",checkedName).commit()){
                        folders.edit().putString("GAMES.uri",oldUri).putString("GAMES.name",oldName).commit();
                        failure="Cannot save the folder. Previous choice kept. Try again.";
                    }
                }
                state.checked(request,checkedName,failure);view.invalidate();
            });
        },"pikonest-setup-folder").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=PICK)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){state.problem="Selection cancelled. Previous folder kept.";view.invalidate();return;}
        try{
            Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);
            check(uri,true);
        }catch(Exception error){state.problem="Access could not be saved. Choose the folder again.";view.invalidate();}
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("step",state.step);super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();state.runtimePrepared=RuntimeInstallation.matchesSaved(this);view.invalidate();immersive();}
    private void immersive(){
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
        else getWindow().getDecorView().setSystemUiVisibility(5894);
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(input!=null)input.reset();}
    @Override protected void onPause(){if(input!=null)input.reset();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){view.action(Action.CANCEL);}
}
