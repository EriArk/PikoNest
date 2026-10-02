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
import art.pikoos.lab.core.SpriteRegion;
import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.lab.core.PicoRuntimeBackend;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public final class MainActivity extends Activity {
    private static final String TAG="PikoRuntimeLab";
    private WorkshopSession session;
    private WorkshopView surface;
    private ControllerInput input;
    private PicoRuntimeBackend backend;
    private SharedPreferences prefs;
    private SharedPreferences libraryPrefs;
    private ProjectStore store;
    private LibrarySession library;
    private LibraryView shelf;
    private String activeId="moon-garden";
    private boolean showingLibrary;
    private final HashMap<String,WorkshopSession> sessions=new HashMap<>();
    private boolean awaitingReturn,leftForRuntime;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs=getSharedPreferences("moon-garden-ui",MODE_PRIVATE);
        libraryPrefs=getSharedPreferences("library-ui",MODE_PRIVATE);
        activeId=libraryPrefs.getString("active","moon-garden");
        if(!LibrarySession.validId(activeId))activeId="moon-garden";
        awaitingReturn=libraryPrefs.getBoolean("awaitingReturn",prefs.getBoolean("awaitingReturn",false));leftForRuntime=awaitingReturn;
        boolean startOnShelf=libraryPrefs.getBoolean("shelf",true);
        String shelfSelection=libraryPrefs.getString("selected",activeId);
        int shelfFocus=libraryPrefs.getInt("focus",0);
        backend=new ExternalPicoBackend(this);
        try {
            store=new ProjectStore(getFilesDir());
            byte[] template;
            try(InputStream in=getAssets().open("moon-garden.p8")){template=readAll(in);}
            if(!store.directory("moon-garden").exists())store.create("moon-garden",template);
            library=new LibrarySession(new LibrarySession.Port(){
                public java.util.List<String> ids()throws Exception{return store.ids();}
                public byte[] read(String id)throws Exception{return store.read(id);}
                public void create(String id,byte[] bytes)throws Exception{store.create(id,bytes);}
                public void open(String id,WorkshopCartridge cart)throws Exception{openProject(id,cart);}
                public void resume(){if(session!=null)showWorkshop();}
            },template);
            input=new ControllerInput(action->{if(showingLibrary)shelf.action(action);else if(surface!=null)surface.action(action);},
                ()->session!=null?session.swapAB:libraryPrefs.getBoolean("swapAB",false));
            try{openProject(activeId,new WorkshopCartridge(store.read(activeId)));}
            catch(Exception e){Log.e(TAG,"Last project unavailable; retained",e);showLibrary();library.fail(e);shelf.invalidate();}
            if(!awaitingReturn&&startOnShelf)showLibrary(shelfSelection,shelfFocus);
        }catch(Exception e){
            TextView error=new TextView(this);error.setText("Не удалось открыть проекты. Исходные файлы сохранены.\n"+e.getMessage());
            error.setTextColor(WorkshopView.COLORS[7]);error.setBackgroundColor(WorkshopView.COLORS[1]);error.setPadding(32,32,32,32);
            setContentView(error);Log.e(TAG,"Project load failed; original retained",e);
        }
    }
    private void openProject(final String id,WorkshopCartridge cart)throws Exception {
        persistUi();
        store.cart(id); // Validate the target before switching editor state.
        WorkshopSession next=sessions.get(id);
        if(next==null||!java.util.Arrays.equals(next.cart().bytes(),cart.bytes())) {
            next=new WorkshopSession(cart,new WorkshopSession.Port(){
                public void save(byte[] bytes)throws Exception{saveCart(store.cart(id),bytes);Log.i(TAG,"project_saved id="+id+" bytes="+bytes.length);}
                public void launch(byte[] bytes)throws Exception{launchCart(bytes);}
                public void library()throws Exception{showLibrary();}
            });
            activeId=id;session=next;
            prefs=getSharedPreferences(id+"-ui",MODE_PRIVATE);
            restoreUi();
            sessions.put(id,next);
        }else{
            activeId=id;session=next;prefs=getSharedPreferences(id+"-ui",MODE_PRIVATE);
        }
        session.swapAB=libraryPrefs.getBoolean("swapAB",getSharedPreferences("moon-garden-ui",MODE_PRIVATE).getBoolean("swapAB",false));
        libraryPrefs.edit().putString("active",id).apply();
        showWorkshop();
    }
    private void showWorkshop(){
        showingLibrary=false;
        surface=new WorkshopView(this,session,LibrarySession.title(activeId),()->persistUi());
        setContentView(surface);surface.requestFocus();immersive();persistUi();
    }
    private void showLibrary()throws Exception{
        showLibrary(activeId,0);
    }
    private void showLibrary(String preferred,int focus)throws Exception{
        persistUi();library.refresh(preferred);
        if(!library.entries().isEmpty())library.focus=Math.max(0,Math.min(2,focus));
        showingLibrary=true;
        shelf=new LibraryView(this,library,activeId,session!=null&&session.swapAB,()->persistUi());
        setContentView(shelf);shelf.requestFocus();immersive();persistUi();
    }
    private static byte[] readAll(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();
    }
    private void saveCart(AtomicFile target,byte[] bytes)throws Exception{
        FileOutputStream out=null;
        try{out=target.startWrite();out.write(bytes);target.finishWrite(out);}
        catch(Exception e){target.failWrite(out);throw e;}
    }
    private void launchCart(byte[] bytes)throws Exception{
        if(awaitingReturn)return;
        awaitingReturn=true;leftForRuntime=false;
        try{
            persistUi();
            if(!libraryPrefs.edit().putBoolean("awaitingReturn",true).putBoolean("shelf",false).commit())throw new IllegalStateException("Не удалось сохранить состояние запуска");
            backend.launch(bytes);
            Log.i(TAG,"launch_requested speed="+session.cart().value(0)+" jump="+session.cart().value(1));
        }catch(Exception e){awaitingReturn=false;libraryPrefs.edit().putBoolean("awaitingReturn",false).commit();throw e;}
    }
    private void persistUi(){
        if(libraryPrefs!=null)libraryPrefs.edit().putBoolean("shelf",showingLibrary).apply();
        if(showingLibrary&&library!=null&&library.current()!=null)
            libraryPrefs.edit().putString("selected",library.current().id).putInt("focus",library.focus).apply();
        if(session==null)return;
        libraryPrefs.edit().putString("active",activeId).putBoolean("swapAB",session.swapAB).apply();
        prefs.edit().putInt("tool",session.tool).putInt("focus",session.focus)
            .putInt("line",session.codeLine).putInt("x",session.cursorX).putInt("y",session.cursorY)
            .putInt("color",session.color).putString("drawTool",session.drawTool.name()).putBoolean("swapAB",session.swapAB)
            .putString("pickerReturn",session.pickerReturn.name())
            .putInt("lineX",session.pendingLine()?session.lineX:-1).putInt("lineY",session.pendingLine()?session.lineY:-1)
            .putInt("field",session.field).putInt("draft",session.draft)
            .putInt("spriteSlot",session.spriteSlot).putInt("sheetFocus",session.sheetFocus)
            .putBoolean("region",session.region!=null).putBoolean("zoom",session.zoom)
            .putInt("regionX",session.selection().x).putInt("regionY",session.selection().y)
            .putInt("regionWidth",session.selection().width).putInt("regionHeight",session.selection().height)
            .putBoolean("browsingSprites",session.browsingSprites)
            .putString("mode",session.heroDraft!=null?"HERO":session.mode==Mode.CANVAS||session.pendingLine()?"CANVAS":session.mode==Mode.VALUE?"VALUE":"NAVIGATE").apply();
    }
    private int bounded(String key,int fallback,int max){return Math.max(0,Math.min(max,prefs.getInt(key,fallback)));}
    private void restoreUi(){
        session.tool=bounded("tool",0,2);
        session.codeLine=bounded("line",session.cart().line(0),session.cart().code().split("\n",-1).length-1);
        session.color=bounded("color",14,15);
        try{session.drawTool=DrawTool.valueOf(prefs.getString("drawTool",prefs.getBoolean("eraser",false)?"ERASER":"BRUSH"));}
        catch(IllegalArgumentException e){session.drawTool=DrawTool.BRUSH;}
        try{session.pickerReturn=DrawTool.valueOf(prefs.getString("pickerReturn","BRUSH"));}
        catch(IllegalArgumentException e){session.pickerReturn=DrawTool.BRUSH;}
        if(session.pickerReturn==DrawTool.PICKER)session.pickerReturn=DrawTool.BRUSH;
        session.swapAB=prefs.getBoolean("swapAB",false);
        session.spriteSlot=bounded("spriteSlot",session.cart().heroSlot(),7);
        session.sheetFocus=bounded("sheetFocus",session.spriteSlot,11);
        session.browsingSprites=prefs.getBoolean("browsingSprites",true);
        if(prefs.getBoolean("region",false)&&!session.browsingSprites){
            try{
                SpriteRegion r=new SpriteRegion(prefs.getInt("regionX",0),prefs.getInt("regionY",0),prefs.getInt("regionWidth",16),prefs.getInt("regionHeight",16));
                if(!r.sharesMap()&&r.x%8==0&&r.y%8==0&&r.width%8==0&&r.height%8==0)session.region=r;
            }catch(IllegalArgumentException ignored){/* Invalid optional view state cannot damage a cartridge. */}
        }
        session.cursorX=bounded("x",7,session.selection().width-1);session.cursorY=bounded("y",7,session.selection().height-1);
        session.focus=bounded("focus",0,session.maxFocus());
        session.zoom=prefs.getBoolean("zoom",false);
        if(session.tool==2){
            if(session.browsingSprites)session.mode=Mode.SHEET;
            else if(prefs.getString("mode","").equals("CANVAS"))session.mode=Mode.CANVAS;
        }
        int lineX=prefs.getInt("lineX",-1),lineY=prefs.getInt("lineY",-1);
        if(session.mode==Mode.CANVAS&&session.drawTool==DrawTool.LINE&&lineX>=0&&lineX<session.selection().width&&lineY>=0&&lineY<session.selection().height){session.lineX=lineX;session.lineY=lineY;}
        if(session.tool!=2&&prefs.getString("mode","").equals("VALUE")){
            session.mode=Mode.VALUE;session.field=bounded("field",0,1);session.draft=Math.max(1,bounded("draft",2,4));
        }
        if(session.tool==2&&prefs.getString("mode","").equals("HERO"))session.previewHero();
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
            awaitingReturn=false;leftForRuntime=false;libraryPrefs.edit().putBoolean("awaitingReturn",false).apply();
            getSharedPreferences("moon-garden-ui",MODE_PRIVATE).edit().putBoolean("awaitingReturn",false).apply();
            session.notice="Сохранено";surface.invalidate();
            Log.i(TAG,"host_resumed tool="+session.tool+" focus="+session.focus+" result=unknown");
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){return input!=null&&input.key(event)||super.dispatchKeyEvent(event);}
    @Override public boolean onGenericMotionEvent(MotionEvent event){return input!=null&&input.motion(event)||super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){if(showingLibrary&&shelf!=null)shelf.action(Action.CANCEL);else if(surface!=null)surface.action(Action.CANCEL);else super.onBackPressed();}
}
