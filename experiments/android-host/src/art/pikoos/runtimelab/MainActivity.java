package art.pikoos.runtimelab;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.View;
import android.widget.TextView;
import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.PicoRuntimeBackend;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final String TAG="PikoRuntimeLab";
    private WorkshopSession session;
    private WorkshopView surface;
    private ControllerInput input;
    private PicoRuntimeBackend backend;
    private SharedPreferences prefs;
    private AtomicFile project;
    private boolean awaitingReturn,leftForRuntime;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs=getSharedPreferences("moon-garden-ui",MODE_PRIVATE);
        awaitingReturn=prefs.getBoolean("awaitingReturn",false);leftForRuntime=awaitingReturn;
        backend=new ExternalPicoBackend(this);
        try {
            File directory=new File(getFilesDir(),"projects/moon-garden");
            if(!directory.isDirectory()&&!directory.mkdirs())throw new IllegalStateException("Не удалось создать папку проекта");
            project=new AtomicFile(new File(directory,"game.p8"));
            WorkshopCartridge cart;
            if(project.getBaseFile().exists()||new File(directory,"game.p8.bak").exists())cart=new WorkshopCartridge(project.readFully());
            else {
                try(InputStream in=getAssets().open("moon-garden.p8")){cart=new WorkshopCartridge(readAll(in));}
                saveCart(cart.bytes());
            }
            session=new WorkshopSession(cart,new WorkshopSession.Port(){
                public void save(byte[] bytes)throws Exception{saveCart(bytes);Log.i(TAG,"project_saved bytes="+bytes.length);}
                public void launch(byte[] bytes)throws Exception{launchCart(bytes);}
            });
            restoreUi();
            surface=new WorkshopView(this,session,()->persistUi());
            input=new ControllerInput(action->{surface.action(action);},()->session.swapAB);
            setContentView(surface);surface.requestFocus();immersive();
        }catch(Exception e){
            TextView error=new TextView(this);error.setText("Не удалось открыть проект. Исходный файл сохранён.\n"+e.getMessage());
            error.setTextColor(WorkshopView.COLORS[7]);error.setBackgroundColor(WorkshopView.COLORS[1]);error.setPadding(32,32,32,32);
            setContentView(error);Log.e(TAG,"Project load failed; original retained",e);
        }
    }
    private static byte[] readAll(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();
    }
    private void saveCart(byte[] bytes)throws Exception{
        FileOutputStream out=null;
        try{out=project.startWrite();out.write(bytes);project.finishWrite(out);}
        catch(Exception e){project.failWrite(out);throw e;}
    }
    private void launchCart(byte[] bytes)throws Exception{
        if(awaitingReturn)return;
        awaitingReturn=true;leftForRuntime=false;
        try{
            persistUi();
            if(!prefs.edit().putBoolean("awaitingReturn",true).commit())throw new IllegalStateException("Не удалось сохранить состояние запуска");
            backend.launch(bytes);
            Log.i(TAG,"launch_requested speed="+session.cart().value(0)+" jump="+session.cart().value(1));
        }catch(Exception e){awaitingReturn=false;prefs.edit().putBoolean("awaitingReturn",false).commit();throw e;}
    }
    private void persistUi(){
        if(session==null)return;
        prefs.edit().putInt("tool",session.tool).putInt("focus",session.focus)
            .putInt("line",session.codeLine).putInt("x",session.cursorX).putInt("y",session.cursorY)
            .putInt("color",session.color).putBoolean("eraser",session.eraser).putBoolean("swapAB",session.swapAB)
            .putInt("field",session.field).putInt("draft",session.draft)
            .putInt("spriteSlot",session.spriteSlot).putInt("sheetFocus",session.sheetFocus)
            .putBoolean("browsingSprites",session.browsingSprites)
            .putString("mode",session.mode==Mode.CANVAS?"CANVAS":session.mode==Mode.VALUE?"VALUE":"NAVIGATE").apply();
    }
    private int bounded(String key,int fallback,int max){return Math.max(0,Math.min(max,prefs.getInt(key,fallback)));}
    private void restoreUi(){
        session.tool=bounded("tool",0,2);session.focus=bounded("focus",0,4);
        session.codeLine=bounded("line",session.cart().line(0),session.cart().code().split("\n",-1).length-1);
        session.cursorX=bounded("x",7,15);session.cursorY=bounded("y",7,15);session.color=bounded("color",14,15);
        session.eraser=prefs.getBoolean("eraser",false);session.swapAB=prefs.getBoolean("swapAB",false);
        session.spriteSlot=bounded("spriteSlot",session.cart().heroSlot(),7);
        session.sheetFocus=bounded("sheetFocus",session.spriteSlot,10);
        session.browsingSprites=prefs.getBoolean("browsingSprites",true);
        if(session.tool==2){
            if(session.browsingSprites)session.mode=Mode.SHEET;
            else if(prefs.getString("mode","").equals("CANVAS"))session.mode=Mode.CANVAS;
        }
        if(session.tool!=2&&prefs.getString("mode","").equals("VALUE")){
            session.mode=Mode.VALUE;session.field=bounded("field",0,1);session.draft=Math.max(1,bounded("draft",2,4));
        }
    }
    private void immersive(){
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        }else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(input!=null)input.reset();}
    @Override protected void onPause(){if(input!=null)input.reset();persistUi();super.onPause();}
    @Override protected void onStop(){super.onStop();if(awaitingReturn)leftForRuntime=true;}
    @Override protected void onResume(){
        super.onResume();immersive();
        if(session!=null&&awaitingReturn&&leftForRuntime){
            awaitingReturn=false;leftForRuntime=false;prefs.edit().putBoolean("awaitingReturn",false).apply();
            session.notice="Сохранено";surface.invalidate();
            Log.i(TAG,"host_resumed tool="+session.tool+" focus="+session.focus+" result=unknown");
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){return input!=null&&input.key(event)||super.dispatchKeyEvent(event);}
    @Override public boolean onGenericMotionEvent(MotionEvent event){return input!=null&&input.motion(event)||super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){if(surface!=null)surface.action(Action.CANCEL);else super.onBackPressed();}
}
