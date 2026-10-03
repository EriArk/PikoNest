package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Isolated external-play task. Does not instantiate projects, editor or their journals. */
public final class LaunchActivity extends Activity {
    private ExternalPicoBackend backend;private ControllerInput input;private LaunchView view;
    private boolean dispatched,left,busy;private volatile int generation;
    private SharedPreferences state;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);state=getSharedPreferences("external-launch",0);backend=new ExternalPicoBackend(this);
        view=new LaunchView(this,this::action,getSharedPreferences("library-ui",0).getBoolean("swapAB",false));
        input=new ControllerInput(this::action,()->getSharedPreferences("library-ui",0).getBoolean("swapAB",false));
        setContentView(view);immersive();
        if(backend.isReturn(getIntent())){dispatched=true;left=true;return;}
        reconcileEnded();
        if(saved!=null&&saved.getBoolean("dispatched")){dispatched=true;left=true;}
        else if(state.getBoolean("dispatched",false)){
            dispatched=true;left=true;
            view.busy=false;view.active=true;view.message="Предыдущая игра уже передана PICO-8. Можно вернуться к ней.";view.invalidate();
        }else{rememberCaller();read(getIntent());}
    }
    @Override protected void onNewIntent(Intent next){
        super.onNewIntent(next);
        if(backend.isReturn(next)){dispatched=true;left=true;return;}
        reconcileEnded();
        // A second request must never replace the snapshot underneath a live game.
        if(dispatched||state.getBoolean("dispatched",false)){
            generation++;busy=false;view.busy=false;left=false;dispatched=true;
            try{backend.resume();}catch(Exception e){dispatched=false;view.active=true;view.message="Вернуться к PICO-8 не удалось. Попробуй ещё раз.";view.invalidate();}
            return;
        }
        setIntent(next);rememberCaller();read(next);
    }
    private void rememberCaller(){
        android.net.Uri ref=getReferrer();String name=ref!=null&&"android-app".equals(ref.getScheme())?ref.getHost():null;
        if(name==null||name.equals(getPackageName())||name.equals(ExternalPicoBackend.RESTART_TEST_PACKAGE))name="";
        // Navigation hint only, never an authority for file access or authentication.
        state.edit().putString("caller",name).commit();
    }
    private void reconcileEnded(){
        // A completed background session may not have delivered a foreground return.
        if(state.getBoolean("dispatched",false)&&backend.sessionEnded()){
            state.edit().putBoolean("dispatched",false).commit();dispatched=false;left=false;
        }
    }
    private void returnToCaller(){
        String name=state.getString("caller","");
        Intent back=name.isEmpty()?null:getPackageManager().getLaunchIntentForPackage(name);
        if(back!=null){back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);try{startActivity(back);}catch(Exception e){Log.w("PIKOOS-External","Caller unavailable",e);}}
        finish();
    }
    private void read(Intent request){
        busy=true;view.busy=true;view.active=false;view.message="Открываем картридж…";view.invalidate();
        final int attempt=++generation;
        new Thread(()->{
            ExternalCartSource source=null;String error=null;
            try{source=new ExternalCartSource(getApplicationContext(),request,()->attempt!=generation);}
            catch(java.util.concurrent.CancellationException e){return;}
            catch(SecurityException e){error="Нет доступа к игре. Выбери этот файл ещё раз.";}
            catch(IllegalArgumentException e){error=e.getMessage();}
            catch(Exception e){error="Не удалось прочитать игру. Проверь накопитель или выбери файл ещё раз.";}
            final ExternalCartSource cart=source;final String failure=error;
            runOnUiThread(()->{
                if(isDestroyed()||isFinishing()||attempt!=generation)return;
                busy=false;view.busy=false;
                if(failure!=null){view.message=failure;view.invalidate();return;}
                try{
                    backend.checkAvailableForLaunch();
                    if(!backend.detect().launcherPresent)throw new Exception("Сначала установи оболочку PICO-8 и подключи свой runtime. Затем повтори запуск.");
                    // Journal precedes dispatch: process loss can never trigger an automatic second launch.
                    if(!state.edit().putBoolean("dispatched",true).commit())throw new Exception("Не удалось сохранить состояние запуска.");
                    dispatched=true;left=false;view.message=cart.name;view.busy=true;view.invalidate();
                    if(cart.files!=null)backend.launch(cart.files);else backend.launch(cart.bytes,cart.format);
                    Log.i("PIKOOS-External","cart_dispatched format="+cart.format);
                }catch(Exception e){
                    dispatched=false;state.edit().putBoolean("dispatched",false).commit();view.busy=false;
                    view.message=e.getMessage()==null?"Не удалось открыть PICO-8.":e.getMessage();view.invalidate();
                }
            });
        },"pikoos-external-read").start();
    }
    private void action(Action a){
        if(a==Action.CANCEL){generation++;finish();return;}
        if(busy)return;
        if(a==Action.CONFIRM){
            if(view.active){
                try{backend.resume();dispatched=true;left=false;}catch(Exception e){view.message="PICO-8 недоступен. Вернись в лаунчер.";view.invalidate();}
            }else{
                Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(pick,1);
            }
        }else if(a==Action.TEST&&!view.active){read(getIntent());}
        else if(a==Action.CONTEXT&&!view.active){startActivity(new Intent(this,MainActivity.class));finish();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==1&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Intent selected=new Intent(Intent.ACTION_VIEW,data.getData());setIntent(selected);read(selected);
        }
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("dispatched",dispatched);super.onSaveInstanceState(out);}
    @Override protected void onPause(){
        input.reset();
        // Leaving during a provider read must not pull the user back into a game later.
        if(busy){generation++;busy=false;view.busy=false;view.message="Запуск отложен. Нажми Start, когда будешь готов играть.";view.invalidate();}
        super.onPause();
    }
    @Override protected void onStop(){super.onStop();if(dispatched)left=true;}
    @Override protected void onResume(){
        super.onResume();immersive();
        if((dispatched||state.getBoolean("dispatched",false))&&left){
            if(!backend.sessionEnded()){view.busy=false;view.active=true;view.message="Игра ещё открыта. Можно вернуться и продолжить.";view.invalidate();return;}
            state.edit().putBoolean("dispatched",false).commit();dispatched=false;Log.i("PIKOOS-External","returned_to_caller result=unknown");returnToCaller();
        }
    }
    @Override protected void onDestroy(){generation++;if(input!=null)input.reset();super.onDestroy();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){return input!=null&&input.key(event)||super.dispatchKeyEvent(event);}
    @Override public boolean onGenericMotionEvent(MotionEvent event){return input.motion(event)||super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){action(Action.CANCEL);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(input!=null)input.reset();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
}
