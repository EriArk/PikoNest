package art.pikoos.runtimelab;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.SharedPreferences;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
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
import art.pikoos.lab.core.SpriteAsset;
import art.pikoos.lab.core.NameEditor;
import android.util.Base64;
import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.CartridgeImport;
import art.pikoos.lab.core.CartridgeExport;
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
    private SpriteAssetStore assetStore;
    private LibrarySession library;
    private LibraryView shelf;
    private String activeId="moon-garden";
    private String activeTitle="Лунный сад";
    private static final int PICK_CART=41;
    private AtomicFile importDraft;
    private static final int SAVE_CART=42;
    private AtomicFile exportDraft;
    private ExportJob exportJob;
    private final ExportJob.Listener exportListener=result->{
        if(library!=null){library.stageExport(result);if(shelf!=null)shelf.invalidate();persistUi();}
    };
    private int importGeneration;
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
            assetStore=new SpriteAssetStore(getFilesDir());
            importDraft=new AtomicFile(new File(getFilesDir(),"pending-import.bin"));
            exportDraft=new AtomicFile(new File(getFilesDir(),"pending-export.bin"));
            byte[] template;
            try(InputStream in=getAssets().open("moon-garden.p8")){template=readAll(in);}
            if(!store.directory("moon-garden").exists())store.create("moon-garden",template);
            library=new LibrarySession(new LibrarySession.Port(){
                public java.util.List<String> ids()throws Exception{return store.ids();}
                public byte[] read(String id)throws Exception{return store.read(id);}
                public void create(String id,byte[] bytes)throws Exception{store.create(id,bytes);}
                public void open(String id,WorkshopCartridge cart)throws Exception{openProject(id,cart);}
                public void resume(){if(session!=null)showWorkshop();}
                public String title(String id)throws Exception{return store.title(id);}
                public void pickImport(){
                    importGeneration++;
                    Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                    pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(pick,PICK_CART);
                }
                public void importProject(CartridgeImport draft)throws Exception{store.importProject(draft);}
                public void clearImport()throws Exception{saveCart(importDraft,new byte[0]);}
                public void saveExport(CartridgeExport draft)throws Exception{saveCart(exportDraft,draft.encode());}
                public void clearExport()throws Exception{saveCart(exportDraft,new byte[0]);if(exportJob!=null)exportJob.detach(exportListener);exportJob=null;}
                public void pickExport(CartridgeExport draft){
                    Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream");
                    save.putExtra(Intent.EXTRA_TITLE,draft.filename);
                    save.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(save,SAVE_CART);
                }
            },template,asset("blank.p8"),asset("lights.p8"));
            input=new ControllerInput(action->{if(showingLibrary)shelf.action(action);else if(surface!=null)surface.action(action);},
                ()->session!=null?session.swapAB:libraryPrefs.getBoolean("swapAB",false));
            try{openProject(activeId,new WorkshopCartridge(store.read(activeId)));}
            catch(Exception e){Log.e(TAG,"Last project unavailable; retained",e);showLibrary();library.fail(e);shelf.invalidate();}
            if(!awaitingReturn&&startOnShelf)showLibrary(shelfSelection,shelfFocus);
            if(!awaitingReturn&&importDraft.getBaseFile().exists()){
                try{
                    byte[] pending=importDraft.readFully();
                    if(pending.length>0){showLibrary(shelfSelection,3);library.stageImport(CartridgeImport.decode(pending));shelf.invalidate();}
                }catch(Exception e){showLibrary(shelfSelection,3);library.fail(e);shelf.invalidate();}
            }
            exportJob=(ExportJob)getLastNonConfigurationInstance();
            if(!awaitingReturn&&exportJob!=null){
                // Retained worker owns the journal until completion; do not read its AtomicFile concurrently.
                showLibrary(exportJob.draft.sourceId,4);library.stageExport(exportJob.draft);shelf.invalidate();
            }else if(!awaitingReturn&&exportDraft.getBaseFile().exists()){
                try{
                    byte[] pending=exportDraft.readFully();
                    if(pending.length>0){
                        CartridgeExport draft=CartridgeExport.decode(pending);
                        // Process loss does not prove whether the provider finished its write.
                        if(draft.state==CartridgeExport.State.WRITING&&exportJob==null)draft=draft.withState(CartridgeExport.State.UNCERTAIN,draft.filename);
                        showLibrary(draft.sourceId,4);library.stageExport(draft);shelf.invalidate();
                    }
                }catch(Exception e){showLibrary(shelfSelection,4);library.fail(e);shelf.invalidate();}
            }
            if(exportJob!=null)exportJob.attach(exportListener);
        }catch(Exception e){
            TextView error=new TextView(this);error.setText("Не удалось открыть проекты. Исходные файлы сохранены.\n"+e.getMessage());
            error.setTextColor(WorkshopView.COLORS[7]);error.setBackgroundColor(WorkshopView.COLORS[1]);error.setPadding(32,32,32,32);
            setContentView(error);Log.e(TAG,"Project load failed; original retained",e);
        }
    }
    private void openProject(final String id,WorkshopCartridge cart)throws Exception {
        persistUi();
        store.cart(id); // Validate the target before switching editor state.
        final String projectTitle=store.title(id);activeTitle=projectTitle;
        WorkshopSession next=sessions.get(id);
        if(next==null||!java.util.Arrays.equals(next.cart().bytes(),cart.bytes())) {
            next=new WorkshopSession(cart,new WorkshopSession.Port(){
                public void save(byte[] bytes)throws Exception{saveCart(store.cart(id),bytes);Log.i(TAG,"project_saved id="+id+" bytes="+bytes.length);}
                public void launch(byte[] bytes)throws Exception{launchCart(bytes);}
                public void library()throws Exception{showLibrary();}
                public java.util.List<SpriteAsset> assets()throws Exception{return assetStore.list();}
                public void storeAsset(SpriteAsset asset)throws Exception{assetStore.create(asset);}
                public void renameAsset(SpriteAsset expected,String title)throws Exception{assetStore.rename(expected,title);}
                public String projectOrigin(){return projectTitle;}
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
        surface=new WorkshopView(this,session,activeTitle,()->persistUi());
        setContentView(surface);surface.requestFocus();immersive();persistUi();
    }
    private void showLibrary()throws Exception{
        showLibrary(activeId,0);
    }
    private void showLibrary(String preferred,int focus)throws Exception{
        persistUi();library.refresh(preferred);
        if(!library.entries().isEmpty())library.focus=Math.max(0,Math.min(4,focus));
        showingLibrary=true;
        shelf=new LibraryView(this,library,activeId,session!=null&&session.swapAB,()->persistUi());
        setContentView(shelf);shelf.requestFocus();immersive();persistUi();
    }
    private static byte[] readAll(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();
    }
    private byte[] asset(String name)throws Exception{try(InputStream in=getAssets().open(name)){return readAll(in);}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==SAVE_CART){exportResult(result,data);return;}
        if(request!=PICK_CART||library==null)return;
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        final Uri uri=data.getData();final int generation=++importGeneration;
        library.mode=LibrarySession.Mode.READING;shelf.invalidate();
        // Document providers may be remote; never block controller/UI dispatch while reading.
        new Thread(()->{
            CartridgeImport candidate=null;Exception problem=null;
            try{
                String name="selected.p8";
                try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);
                }
                try(InputStream in=getContentResolver().openInputStream(uri)){candidate=new CartridgeImport(name,CartridgeImport.readBounded(in));}
            }catch(Exception e){problem=e;}
            final CartridgeImport ready=candidate;final Exception failure=problem;
            runOnUiThread(()->{
                // An interrupted read has no project side effects; select again after recreation.
                if(isDestroyed()||isFinishing()||generation!=importGeneration||library.mode!=LibrarySession.Mode.READING)return;
                try{
                    if(failure!=null)throw failure;
                    saveCart(importDraft,ready.encode());library.stageImport(ready);
                }catch(Exception e){library.fail(e);}
                shelf.invalidate();persistUi();
            });
        },"pikoos-cart-import").start();
    }
    private void exportResult(int result,Intent data){
        if(library==null||library.exporting==null)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){library.exportPickerCancelled();shelf.invalidate();return;}
        CartridgeExport draft=library.exporting.withState(CartridgeExport.State.WRITING,library.exporting.filename);
        try{
            saveCart(exportDraft,draft.encode());library.stageExport(draft);
            if(exportJob!=null)exportJob.detach(exportListener);
            exportJob=new ExportJob(getApplicationContext().getContentResolver(),data.getData(),exportDraft,draft);
            exportJob.attach(exportListener);exportJob.start();
        }catch(Exception e){library.stageExport(draft.withState(CartridgeExport.State.UNCERTAIN,draft.filename));Log.e(TAG,"Export could not start",e);}
        shelf.invalidate();
    }
    @Override public Object onRetainNonConfigurationInstance(){return exportJob;}
    @Override protected void onDestroy(){if(exportJob!=null)exportJob.detach(exportListener);super.onDestroy();}
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
            Log.i(TAG,"launch_requested project="+activeId);
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
            .putBoolean("copying",session.copying()).putInt("copyX",session.copyX).putInt("copyY",session.copyY)
            .putString("copyReturn",session.copyReturnMode())
            .putBoolean("transforming",session.transforming())
            .putString("transformOperation",session.transforming()?session.transformOperation().name():"")
            .putString("transformReturn",session.transformReturnMode())
            .putBoolean("recoloring",session.recoloring())
            .putInt("recolorFrom",session.recolorFrom()).putInt("recolorTo",session.recolorTo())
            .putInt("recolorField",session.recolorField()).putString("recolorReturn",session.recolorReturnMode())
            .putBoolean("assets",session.mode==Mode.ASSETS||session.assetDraft!=null||session.copyAsset!=null||session.nameEditor!=null)
            .putInt("assetIndex",session.assetIndex).putString("assetsReturn",session.assetsReturnMode())
            .putString("assetId",session.currentAsset()==null?"":session.currentAsset().id)
            .putString("assetDraft",session.assetDraft==null?"":Base64.encodeToString(session.assetDraft.encode(),Base64.NO_WRAP))
            .putString("copyAsset",session.copyAsset==null?"":Base64.encodeToString(session.copyAsset.encode(),Base64.NO_WRAP))
            .putString("nameTarget",session.nameTarget==null?"":Base64.encodeToString(session.nameTarget.encode(),Base64.NO_WRAP))
            .putString("nameText",session.nameEditor==null?"":session.nameEditor.text())
            .putBoolean("nameNew",session.namingNewAsset)
            .putInt("nameKey",session.nameEditor==null?0:session.nameEditor.key)
            .putBoolean("nameLatin",session.nameEditor!=null&&session.nameEditor.latin)
            .putBoolean("nameUpper",session.nameEditor!=null&&session.nameEditor.uppercase)
            .putBoolean("nameAll",session.nameEditor!=null&&session.nameEditor.replaceAll)
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
        if(session.cart().hasHero()&&session.tool!=2&&prefs.getString("mode","").equals("VALUE")){
            session.mode=Mode.VALUE;session.field=bounded("field",0,1);session.draft=Math.max(1,bounded("draft",2,4));
        }
        if(session.tool==2&&prefs.getString("mode","").equals("HERO"))session.previewHero();
        if(prefs.getBoolean("copying",false))session.restoreCopy(prefs.getInt("copyX",-1),prefs.getInt("copyY",-1),prefs.getString("copyReturn",""));
        if(prefs.getBoolean("transforming",false))session.restoreTransform(prefs.getString("transformOperation",""),prefs.getString("transformReturn",""));
        if(prefs.getBoolean("recoloring",false))session.restoreRecolor(prefs.getInt("recolorFrom",-1),prefs.getInt("recolorTo",-1),prefs.getInt("recolorField",-1),prefs.getString("recolorReturn",""));
        if(prefs.getBoolean("assets",false)){
            try{
                session.restoreAssets(prefs.getString("assetsReturn","NAVIGATE"),prefs.getInt("assetIndex",0));
                session.selectAssetId(prefs.getString("assetId",""));
                String saved=prefs.getString("assetDraft",""),copy=prefs.getString("copyAsset","");
                if(!saved.isEmpty()&&session.mode==Mode.ASSETS){session.assetDraft=SpriteAsset.decode(Base64.decode(saved,Base64.NO_WRAP));session.mode=Mode.ASSET_SAVE;}
                if(!copy.isEmpty()&&session.mode==Mode.ASSETS)session.restoreInsertion(SpriteAsset.decode(Base64.decode(copy,Base64.NO_WRAP)),prefs.getInt("copyX",-1),prefs.getInt("copyY",-1));
                String target=prefs.getString("nameTarget","");
                if(!target.isEmpty()){
                    NameEditor editor=new NameEditor(prefs.getString("nameText",""));
                    editor.latin=prefs.getBoolean("nameLatin",false);editor.uppercase=prefs.getBoolean("nameUpper",true);editor.replaceAll=prefs.getBoolean("nameAll",true);
                    editor.key=bounded("nameKey",0,editor.count()-1);
                    session.restoreName(SpriteAsset.decode(Base64.decode(target,Base64.NO_WRAP)),prefs.getBoolean("nameNew",false),editor);
                }
            }catch(Exception e){session.fail(e);}
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
