package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.*;
import art.pikoos.lab.core.RuntimeSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Purchased archive validation and isolated runtime probe, not permanent activation. */
public final class RuntimeSetupActivity extends Activity {
    private static final int PICK=71;
    private RuntimeSetup state;private RuntimeSetupView view;private ControllerInput input;private RuntimeArchiveJob job;
    private boolean resumed;private String probeFailure="";private final android.os.Handler handler=new android.os.Handler();
    private final Runnable monitor=new Runnable(){public void run(){observeProbe();if(resumed&&state.testing)handler.postDelayed(this,700);}};
    private final RuntimeArchiveJob.Listener listener=next->{
        if(isDestroyed()||isFinishing()||next!=job)return;
        if(!next.done){state.begin(next.phase);}
        else if(next.cancelled)state.cancelled();
        else if(next.result!=null){state.complete(next.result.name,next.result.bytes,null);if(next.probe&&!next.dispatched&&resumed)launchPrepared();}
        else{if(next.recheck)state.verified=false;state.complete("",0,next.error);}
        if(!probeFailure.isEmpty())state.problem=probeFailure;
        view.invalidate();
    };
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        state=new RuntimeSetup(new RuntimeSetup.Port(){
            public void pick(){
                try{
                    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent,PICK);
                }catch(Exception error){state.problem="Не удалось открыть выбор файла";view.invalidate();}
            }
            public void recheck(){attach(RuntimeArchiveJob.start(RuntimeSetupActivity.this,null,""));}
            public void cancel(){if(job!=null)job.cancel();}
            public void leave(){finish();}
            public void test(){
                try{
                    ExternalPicoBackend backend=new ExternalPicoBackend(RuntimeSetupActivity.this);
                    if(state.testing){backend.resume();return;}
                    backend.checkProbeAvailable();state.returned=false;probeFailure="";
                    attach(RuntimeArchiveJob.prepareProbe(RuntimeSetupActivity.this));
                }catch(Exception error){state.problem=error.getMessage();view.invalidate();}
            }
        });
        state.arm64=java.util.Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a");
        state.adapterPresent=new ExternalPicoBackend(this).detect().launcherPresent;
        boolean swap=getSharedPreferences("library-ui",0).getBoolean("swapAB",false);
        view=new RuntimeSetupView(this,state,swap);input=new ControllerInput(view::action,()->swap);
        setContentView(view);view.requestFocus();immersive();
        try{
            RuntimeArchiveJob.Saved archive=RuntimeArchiveJob.saved(this);
            if(archive!=null){state.hasArchive=true;state.filename=archive.name;state.archiveBytes=archive.bytes;}
            if(RuntimeArchiveJob.current!=null&&!RuntimeArchiveJob.current.done)attach(RuntimeArchiveJob.current);
            else if(archive!=null)attach(RuntimeArchiveJob.start(this,null,""));
        }catch(Exception e){state.problem="Не удалось прочитать запись об архиве. Выбери ZIP снова.";}
    }
    private void attach(RuntimeArchiveJob next){if(job!=null)job.detach(listener);job=next;job.attach(listener);}
    private void launchPrepared(){
        try{
            ExternalPicoBackend backend=new ExternalPicoBackend(this);backend.checkProbeAvailable();
            if(!getSharedPreferences("runtime-setup",0).edit().putBoolean("dispatched",true).commit())throw new Exception("Не удалось сохранить состояние проверки");
            job.dispatched=true;state.testing=true;handler.removeCallbacks(monitor);backend.launchProbe();
        }catch(Exception error){getSharedPreferences("runtime-setup",0).edit().putBoolean("dispatched",false).commit();state.testing=false;state.problem=error.getMessage();}
    }
    private void observeProbe(){
        if(!getSharedPreferences("runtime-setup",0).getBoolean("dispatched",false))return;
        state.testing=true;String phase=new ExternalPicoBackend(this).probePhase();
        if(phase.equals("EXITED")||phase.equals("FAILED")){
            getSharedPreferences("runtime-setup",0).edit().putBoolean("dispatched",false).commit();state.testing=false;state.returned=true;
            state.notice="Рабочая установка сохранена";
            if(phase.equals("FAILED"))state.problem=probeFailure="Пробный запуск прервался. Можно проверить архив и попробовать снова.";
        }view.invalidate();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=PICK)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){state.cancelled();view.invalidate();return;}
        attach(RuntimeArchiveJob.start(this,data.getData(),"PICO-8.zip"));
    }
    private void immersive(){
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
        else getWindow().getDecorView().setSystemUiVisibility(5894);
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(input!=null)input.reset();}
    @Override protected void onResume(){super.onResume();resumed=true;observeProbe();if(job!=null)listener.changed(job);handler.post(monitor);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(monitor);if(input!=null)input.reset();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(monitor);if(job!=null)job.detach(listener);super.onDestroy();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){view.action(Action.CANCEL);}
}
