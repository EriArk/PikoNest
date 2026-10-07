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
import art.pikoos.lab.core.FolderSetup;
import art.pikoos.lab.core.PlaySession;
import art.pikoos.lab.core.PlayCartridge;
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
    private final art.pikoos.lab.core.ToolCatalogue toolCatalogue=new art.pikoos.lab.core.ToolCatalogue();
    private ProjectStore store;
    private SpriteAssetStore assetStore;
    private SpriteBufferStore spriteBuffer;
    private ParameterPresetStore presetStore;
    private LibrarySession library;
    private LibraryView shelf;
    private SharedPreferences folderPrefs;
    private FolderSetup folders;
    private FolderView folderView;
    private boolean showingFolders;
    private boolean showingPlay,folderReturnPlay;
    private SharedPreferences playPrefs;
    private PlaySession play;
    private PlayView playView;
    private volatile int playGeneration;
    private static final int PICK_FOLDER=43;
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
    private LaunchView runtimeGate;
    private void runtimeAction(Action action){
        if(action==Action.CANCEL){moveTaskToBack(true);return;}
        if(action==Action.CONTEXT&&runtimeGate.recovery){
            runtimeGate.busy=true;runtimeGate.message="Checking the previous session...";runtimeGate.invalidate();
            new Thread(()->{
                boolean recovered=false;try{recovered=backend.recoverSession();}catch(Exception ignored){}
                final boolean ended=recovered;
                runOnUiThread(()->{if(isFinishing()||isDestroyed()||runtimeGate==null)return;
                    runtimeGate.busy=false;
                    if(ended)onRuntimeResume();
                    else{runtimeGate.message="Recovery could not confirm an idle runtime. Your draft and session record are kept. Return to the game, or update the old runtime adapter if this is a lab migration.";runtimeGate.invalidate();}
                });
            },"pikonest-session-recovery").start();return;
        }
        if(action==Action.CONFIRM||action==Action.TEST){
            if(backend.sessionEnded()){onRuntimeResume();return;}
            try{backend.resume();}catch(Exception e){runtimeGate.message="Не удалось вернуться к игре. Попробуй ещё раз.";runtimeGate.invalidate();}
        }
    }
    private final java.util.HashSet<String> codeRecoveryFailed=new java.util.HashSet<>();
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs=getSharedPreferences("moon-garden-ui",MODE_PRIVATE);
        libraryPrefs=getSharedPreferences("library-ui",MODE_PRIVATE);
        toolCatalogue.restore(libraryPrefs.getString("favoriteTools",""),libraryPrefs.getInt("toolCategory",0));
        folderPrefs=getSharedPreferences("folder-setup",MODE_PRIVATE);
        playPrefs=getSharedPreferences("play-library",MODE_PRIVATE);
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
            spriteBuffer=new SpriteBufferStore(getFilesDir());
            presetStore=new ParameterPresetStore(getFilesDir());
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
                public void resume(){showPlay();}
                public void folders(){showFolders(false);}
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
            input=new ControllerInput(action->{if(runtimeGate!=null)runtimeAction(action);else if(showingFolders)folderView.action(action);else if(showingPlay)playView.action(action);else if(showingLibrary)shelf.action(action);else if(surface!=null)surface.action(action);},
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
            if((!awaitingReturn||playPrefs.getBoolean("runtime",false))&&(library.mode==LibrarySession.Mode.SHELF||library.mode==LibrarySession.Mode.ERROR))showPlay();
            if(!awaitingReturn&&folderPrefs.getBoolean("visible",false))showFolders(folderPrefs.getBoolean("fromPlay",true));
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
                public void save(byte[] expected,byte[] bytes)throws Exception{
                    if(!java.util.Arrays.equals(expected,store.read(id)))throw new Exception("Файл проекта изменился вне редактора. Исходник не заменён; вернись к правке или открой проект заново.");
                    save(bytes);
                }
                public void launch(byte[] bytes)throws Exception{launchCart(bytes);}
                public void diagnose(byte[] bytes)throws Exception{
                    persistUi();
                    if(!libraryPrefs.edit().putString("diagnosticProject",id).commit())throw new Exception("Не удалось сохранить место проверки");
                    backend.diagnose(bytes,session.swapAB);
                }
                public void library()throws Exception{showLibrary();}
                public java.util.List<SpriteAsset> assets()throws Exception{return assetStore.list();}
                public void storeAsset(SpriteAsset asset)throws Exception{assetStore.create(asset);}
                public void writeSpriteBuffer(SpriteAsset asset)throws Exception{spriteBuffer.write(asset);}
                public SpriteAsset readSpriteBuffer()throws Exception{return spriteBuffer.read();}
                public void renameAsset(SpriteAsset expected,String title)throws Exception{assetStore.rename(expected,title);}
                public void categorizeAsset(SpriteAsset expected,SpriteAsset.Category category)throws Exception{assetStore.categorize(expected,category);}
                public void favoriteAsset(SpriteAsset expected,boolean favorite)throws Exception{assetStore.favorite(expected,favorite);}
                public String projectOrigin(){return projectTitle;}
                public java.util.List<art.pikoos.lab.core.ParameterPreset> presets()throws Exception{return presetStore.list();}
                public void storePreset(art.pikoos.lab.core.ParameterPreset p)throws Exception{presetStore.create(p);}
                public void updatePreset(art.pikoos.lab.core.ParameterPreset a,art.pikoos.lab.core.ParameterPreset b)throws Exception{presetStore.update(a,b);}
            });
            activeId=id;session=next;
            prefs=getSharedPreferences(id+"-ui",MODE_PRIVATE);
            restoreUi();
            restoreCodeDraft();
            restoreUses();
            try{session.restorePresets(prefs.getString("presetPanel",""));}catch(Exception e){session.fail(new Exception("Набор не восстановлен. Форма и исходник сохранены: "+e.getMessage()));}
            sessions.put(id,next);
        }else{
            activeId=id;session=next;prefs=getSharedPreferences(id+"-ui",MODE_PRIVATE);
        }
        session.swapAB=libraryPrefs.getBoolean("swapAB",getSharedPreferences("moon-garden-ui",MODE_PRIVATE).getBoolean("swapAB",false));
        session.toolCatalogue=toolCatalogue;
        libraryPrefs.edit().putString("active",id).apply();
        showWorkshop();
    }
    private void showWorkshop(){
        showingPlay=false;playGeneration++;
        showingFolders=false;
        showingLibrary=false;
        surface=new WorkshopView(this,session,activeTitle,()->persistUi());
        setContentView(surface);surface.requestFocus();immersive();persistUi();
    }
    private void showLibrary()throws Exception{
        showLibrary(activeId,0);
    }
    private void showLibrary(String preferred,int focus)throws Exception{
        showingPlay=false;showingFolders=false;playGeneration++;
        persistUi();library.refresh(preferred);
        if(!library.entries().isEmpty())library.focus=0;
        showingLibrary=true;
        shelf=new LibraryView(this,library,activeId,session!=null&&session.swapAB,()->persistUi(),()->showFolders(),()->showPlay());
        setContentView(shelf);shelf.requestFocus();immersive();persistUi();
    }
    private void showPlay(){
        persistUi();showingPlay=true;showingFolders=false;showingLibrary=false;
        play=new PlaySession(new PlaySession.Port(){
            public void launch(PlaySession.Game game){launchGame(game);}
            public void exit(){moveTaskToBack(true);}
            public void refresh(){scanGames();}
            public void workshop(){try{showLibrary();}catch(Exception e){play.fail("Could not open Workshop.");}}
            public void folders(){showFolders();}
            public void favorite(PlaySession.Game game,boolean value)throws Exception{
                if(!playPrefs.edit().putBoolean("favorite:"+game.id,value).commit())throw new Exception("Cannot save favorite");
            }
        });
        play.filter=Math.max(0,Math.min(2,playPrefs.getInt("filter",0)));
        playView=new PlayView(this,play,session!=null&&session.swapAB,()->persistPlay());
        setContentView(playView);playView.requestFocus();immersive();scanGames();
    }
    private void persistPlay(){
        if(play==null)return;
        SharedPreferences.Editor edit=playPrefs.edit().putInt("filter",play.filter);
        if(play.current()!=null)edit.putString("selected",play.current().id);edit.apply();
    }
    private void scanGames(){
        final int generation=++playGeneration;final PlaySession target=play;
        final String location=folderPrefs.getString("GAMES.uri","");
        if(location.isEmpty()){target.replace(java.util.Collections.emptyList(),"");target.notice="Select: choose your games folder";playView.invalidate();return;}
        target.busy=true;target.error="";playView.invalidate();
        final android.content.ContentResolver resolver=getApplicationContext().getContentResolver();
        new Thread(()->{
            GameFolder.Listing result=null;String failure=null;
            try{result=GameFolder.list(resolver,location,playPrefs,()->generation!=playGeneration);}
            catch(java.util.concurrent.CancellationException e){return;}
            catch(Exception e){failure="Folder unavailable. Select: reconnect";Log.w(TAG,"Game scan failed",e);}
            final GameFolder.Listing ready=result;final String error=failure;
            runOnUiThread(()->{
                if(isDestroyed()||!showingPlay||generation!=playGeneration||target!=play)return;
                if(error!=null){target.replace(java.util.Collections.emptyList(),"");target.fail(error);}
                else{
                    target.replace(ready.games,playPrefs.getString("selected",""));
                    target.notice=ready.limited?"Some games are hidden. Choose a smaller folder.":ready.unreadable>0?"Unreadable folders: "+ready.unreadable+" · Menu: retry":"Your games, ready to play";
                }
                playView.invalidate();
            });
        },"pikoos-game-scan").start();
    }
    private void launchGame(PlaySession.Game game){
        final int generation=++playGeneration;final PlaySession target=play;
        final android.content.ContentResolver resolver=getApplicationContext().getContentResolver();
        final String location=folderPrefs.getString("GAMES.uri","");
        new Thread(()->{
            byte[] bytes=null;art.pikoos.lab.core.RuntimeFileSet files=null;String failure=null;
            try{
                bytes=GameFolder.read(resolver,game.id);
                IncludeSource.Prepared ready=IncludeSource.launch(resolver,location,Uri.parse(game.id),game.title+game.format.extension,bytes,()->generation!=playGeneration);
                bytes=ready.bytes;files=ready.files;
            }catch(java.util.concurrent.CancellationException e){return;}
            catch(IllegalArgumentException e){failure=e.getMessage();}
            catch(Exception e){failure="Could not read game. Menu: refresh";}
            final byte[] snapshot=bytes;final String error=failure;
            final art.pikoos.lab.core.RuntimeFileSet fileSet=files;
            runOnUiThread(()->{
                if(isDestroyed()||!showingPlay||generation!=playGeneration||target!=play)return;
                if(error!=null){target.fail(error);playView.invalidate();return;}
                try{
                    if(!backend.detect().launcherPresent)throw new Exception("Connect PICO-8 first. Runtime wrapper not found.");
                    persistPlay();
                    if(!playPrefs.edit().putBoolean("runtime",true).putString("selected",game.id).commit())throw new Exception("Could not save library selection.");
                    awaitingReturn=true;leftForRuntime=false;
                    if(!libraryPrefs.edit().putBoolean("awaitingReturn",true).commit())throw new Exception("Could not save launch state.");
                    if(fileSet!=null)backend.launch(fileSet);else backend.launch(snapshot,game.format);
                    game.recent=System.currentTimeMillis();playPrefs.edit().putLong("recent:"+game.id,game.recent).apply();
                }catch(Exception e){awaitingReturn=false;playPrefs.edit().putBoolean("runtime",false).apply();libraryPrefs.edit().putBoolean("awaitingReturn",false).apply();target.fail(e.getMessage()==null?"Could not launch PICO-8.":e.getMessage());}
                playView.invalidate();
            });
        },"pikoos-game-launch").start();
    }
    private void showFolders(){showFolders(showingPlay);}
    private void showFolders(boolean fromPlay){
        playGeneration++;folderReturnPlay=fromPlay;
        folderPrefs.edit().putBoolean("fromPlay",fromPlay).apply();
        persistUi();
        String[] locations=new String[4],names=new String[4];
        for(FolderSetup.Role role:FolderSetup.Role.values()){
            locations[role.ordinal()]=folderPrefs.getString(role.name()+".uri","");
            names[role.ordinal()]=folderPrefs.getString(role.name()+".name","");
        }
        folders=new FolderSetup(new FolderSetup.Port(){
            public void pick(FolderSetup.Role role){
                try{
                    if(!folderPrefs.edit().putString("pending",role.name()).commit())throw new Exception("Не удалось сохранить выбор");
                    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                    if(role.writable())intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    String current=folders.entry(role.ordinal()).location;
                    if(!current.isEmpty())intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,Uri.parse(current));
                    startActivityForResult(intent,PICK_FOLDER);
                }catch(Exception e){folders.notice="Не удалось открыть выбор папки";folderView.invalidate();}
            }
            public void check(FolderSetup.Role role,String location){checkFolder(role,location);}
            public void save(FolderSetup.Role role,String location,String name)throws Exception{
                String key=role.name(),oldUri=folderPrefs.getString(key+".uri",""),oldName=folderPrefs.getString(key+".name","");
                if(!folderPrefs.edit().putString(key+".uri",location).putString(key+".name",name).commit()){
                    // SharedPreferences publishes in memory even when durable commit fails.
                    folderPrefs.edit().putString(key+".uri",oldUri).putString(key+".name",oldName).commit();
                    throw new Exception("Не удалось сохранить папку");
                }
            }
            public void leave(){
                showingFolders=false;folderPrefs.edit().putBoolean("visible",false).apply();
                if(folderReturnPlay)showPlay();else{
                    try{showLibrary(library.current()==null?activeId:library.current().id,library.focus);}
                    catch(Exception e){Log.w(TAG,"Could not return to project shelf",e);showWorkshop();}
                }
            }
        },locations,names);
        folders.select(folderPrefs.getInt("selected",0));showingFolders=true;
        folderPrefs.edit().putBoolean("visible",true).apply();
        folderView=new FolderView(this,folders,session!=null&&session.swapAB,()->folderPrefs.edit().putInt("selected",folders.selected).apply(),()->startActivity(new Intent(this,RuntimeSetupActivity.class)));
        setContentView(folderView);folderView.requestFocus();immersive();
    }
    private void checkFolder(FolderSetup.Role role,String location){
        final FolderSetup target=folders;
        final android.content.ContentResolver resolver=getApplicationContext().getContentResolver();
        new Thread(()->{
            String name=null,problem=null;
            try{name=FolderAccess.verify(resolver,Uri.parse(location),role.writable());}
            catch(Exception e){Log.w(TAG,"Folder access check failed",e);problem=e instanceof SecurityException?"Нет доступа. Выбери папку снова.":e.getMessage();if(problem==null)problem="Папка недоступна. Попробуй снова.";}
            final String result=name,error=problem;
            runOnUiThread(()->{
                // A stale/recreated Activity cannot publish a new location. Retry is safe.
                if(isDestroyed()||isFinishing()||!showingFolders||folders!=target)return;
                target.complete(result,error);folderView.invalidate();
            });
        },"pikoos-folder-check").start();
    }
    private void folderResult(int result,Intent data){
        String pending=folderPrefs.getString("pending","");folderPrefs.edit().remove("pending").apply();
        if(!showingFolders)showFolders();
        if(result!=RESULT_OK||data==null||data.getData()==null){folders.notice="Выбор отменён · прежняя папка сохранена";folderView.invalidate();return;}
        try{
            FolderSetup.Role role=FolderSetup.Role.valueOf(pending);Uri tree=data.getData();
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(!role.writable())flags&=Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(tree,flags);
            folders.begin(role,tree.toString());
        }catch(Exception e){folders.notice="Доступ не сохранён. Попробуй выбрать снова.";Log.w(TAG,"Folder selection failed",e);}
        folderView.invalidate();
    }
    private static byte[] readAll(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();
    }
    private byte[] asset(String name)throws Exception{try(InputStream in=getAssets().open(name)){return readAll(in);}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==ExternalPicoBackend.DIAGNOSTIC_REQUEST){
            try{
                if(data!=null&&!libraryPrefs.getString("diagnosticToken","").equals(data.getStringExtra("token")))return;
                String id=libraryPrefs.getString("diagnosticProject","");libraryPrefs.edit().remove("diagnosticProject").remove("diagnosticToken").apply();
                if(session==null||!activeId.equals(id)||session.codeDraft==null)return;
                byte[] checked=new AtomicFile(new File(getFilesDir(),"diagnostic.p8")).readFully();
                boolean cancelled=result!=RESULT_OK||data==null||data.getBooleanExtra("cancelled",false);
                String log="";boolean completed=false,ended=false;
                if(!cancelled){
                    StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(checked))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
                    if(!hash.toString().equals(data.getStringExtra("hash")))log="Результат относится к другой копии картриджа";
                    else{log=data.getStringExtra("log");completed=data.getBooleanExtra("completed",false);ended=data.getBooleanExtra("windowEnded",false);}
                }
                session.diagnosticResult(checked,log,cancelled,completed,ended);showWorkshop();persistUi();surface.invalidate();
            }catch(Exception e){Log.e(TAG,"Diagnostic result unavailable",e);if(session!=null){session.diagnosticPending=false;session.mode=Mode.CODE;session.notice="Результат проверки недоступен";surface.invalidate();}}
            return;
        }
        if(request==PICK_FOLDER){folderResult(result,data);return;}
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
    @Override protected void onDestroy(){playGeneration++;if(exportJob!=null)exportJob.detach(exportListener);super.onDestroy();}
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
            if(!libraryPrefs.edit().putBoolean("awaitingReturn",true).putBoolean("shelf",false).commit())throw new IllegalStateException("Could not save launch state.");
            backend.launch(bytes);
            Log.i(TAG,"launch_requested project="+activeId);
        }catch(Exception e){awaitingReturn=false;libraryPrefs.edit().putBoolean("awaitingReturn",false).commit();throw e;}
    }
    private void persistUi(){
        if(libraryPrefs!=null)libraryPrefs.edit().putBoolean("shelf",showingLibrary).apply();
        if(showingLibrary&&library!=null&&library.current()!=null)
            libraryPrefs.edit().putString("selected",library.current().id).putInt("focus",library.focus).apply();
        if(session==null)return;
        libraryPrefs.edit().putString("favoriteTools",toolCatalogue.encode()).putInt("toolCategory",toolCatalogue.category).apply();
        if(session.codeDraft!=null)codeRecoveryFailed.remove(activeId);
        if(session.uses!=null)usesRecoveryFailed.remove(activeId);
        if(session.sharedEdit!=null)sharedRecoveryFailed.remove(activeId);
        if(session.stroke!=null)strokeRecoveryFailed.remove(activeId);
        libraryPrefs.edit().putString("active",activeId).putBoolean("swapAB",session.swapAB).apply();
        prefs.edit().putInt("tool",session.tool).putInt("focus",session.focus)
            .putInt("mapX",session.mapEditor.x).putInt("mapY",session.mapEditor.y).putInt("mapTile",session.mapEditor.tile)
            .putString("mapTool",session.mapEditor.tool.name()).putString("mapDraft",session.mapEditor.encode())
            .putString("freehand",session.stroke==null?(strokeRecoveryFailed.contains(activeId)?prefs.getString("freehand",""):""):Base64.encodeToString(session.stroke.encode(),Base64.NO_WRAP))
            .putString("sharedEdit",session.sharedEdit==null?(sharedRecoveryFailed.contains(activeId)?prefs.getString("sharedEdit",""):""):Base64.encodeToString(session.sharedEdit.encode(),Base64.NO_WRAP))
            .putString("flagDraft",session.flagDraft==null?"":session.flagDraft.encode())
            .putString("gameUses",session.uses==null?(usesRecoveryFailed.contains(activeId)?prefs.getString("gameUses",""):""):Base64.encodeToString(session.uses.encode(),Base64.NO_WRAP))
            .putString("luaDraft",session.codeDraft==null?(codeRecoveryFailed.contains(activeId)?prefs.getString("luaDraft",""):""):Base64.encodeToString(session.codeDraft.encode(),Base64.NO_WRAP))
            .putInt("line",session.codeLine).putInt("x",session.cursorX).putInt("y",session.cursorY)
            .putInt("codeColumn",session.codeColumn)
            .putString("presetPanel",session.presets==null?"":session.presets.encode())
            .putInt("color",session.color).putString("drawTool",session.drawTool.name()).putBoolean("swapAB",session.swapAB)
            .putString("pickerReturn",session.pickerReturn.name())
            .putInt("lineX",session.pendingStroke()?session.lineX:-1).putInt("lineY",session.pendingStroke()?session.lineY:-1)
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
            .putString("moveDraft",session.move==null?"":session.move.encode()).putString("moveReturn",session.moveReturnMode())
            .putInt("recolorFrom",session.recolorFrom()).putInt("recolorTo",session.recolorTo())
            .putInt("recolorField",session.recolorField()).putString("recolorReturn",session.recolorReturnMode())
            .putBoolean("assets",session.mode==Mode.ASSETS||session.mode==Mode.ASSET_CATEGORY||session.assetDraft!=null||session.copyAsset!=null&&!session.bufferInsertion()||session.nameEditor!=null)
            .putInt("assetFilter",session.assetFilter).putInt("assetCategoryChoice",session.mode==Mode.ASSET_CATEGORY?session.assetCategoryChoice:-1)
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
            .putString("mode",session.heroDraft!=null?"HERO":session.mode==Mode.CANVAS||session.pendingStroke()?"CANVAS":session.mode==Mode.VALUE?"VALUE":"NAVIGATE").apply();
    }
    private int bounded(String key,int fallback,int max){return Math.max(0,Math.min(max,prefs.getInt(key,fallback)));}
    private void restoreUi(){
        session.tool=bounded("tool",0,3);
        session.mapEditor.x=bounded("mapX",0,127);session.mapEditor.y=bounded("mapY",0,63);session.mapEditor.tile=bounded("mapTile",1,255);
        try{session.mapEditor.tool=art.pikoos.lab.core.MapEditor.Tool.valueOf(prefs.getString("mapTool","BRUSH"));}catch(IllegalArgumentException ignored){}
        session.codeLine=bounded("line",session.cart().line(0),session.cart().code().split("\n",-1).length-1);
        session.codeColumn=bounded("codeColumn",0,2*1024*1024);
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
                if(r.x%8==0&&r.y%8==0&&r.width%8==0&&r.height%8==0)session.region=r;
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
        session.restoreStroke(lineX,lineY);
        if(session.cart().hasHero()&&session.tool<2&&prefs.getString("mode","").equals("VALUE")){
            session.mode=Mode.VALUE;session.field=bounded("field",0,1);session.draft=Math.max(1,bounded("draft",2,4));
        }
        if(session.tool==2&&prefs.getString("mode","").equals("HERO"))session.previewHero();
        if(session.tool==3)try{session.mapEditor.restore(prefs.getString("mapDraft",""),session.cart());}catch(IllegalArgumentException e){session.fail(e);}
        if(session.tool==3&&!session.mapEditor.pending()&&!prefs.getString("flagDraft","").isEmpty())try{
            session.flagDraft=art.pikoos.lab.core.FlagDraft.restore(prefs.getString("flagDraft",""),session.cart());
        }catch(IllegalArgumentException e){session.fail(e);}
        if(prefs.getBoolean("copying",false))session.restoreCopy(prefs.getInt("copyX",-1),prefs.getInt("copyY",-1),prefs.getString("copyReturn",""));
        if(prefs.getBoolean("transforming",false))session.restoreTransform(prefs.getString("transformOperation",""),prefs.getString("transformReturn",""));
        if(prefs.getBoolean("recoloring",false))session.restoreRecolor(prefs.getInt("recolorFrom",-1),prefs.getInt("recolorTo",-1),prefs.getInt("recolorField",-1),prefs.getString("recolorReturn",""));
        if(!prefs.getString("moveDraft","").isEmpty())session.restoreMove(prefs.getString("moveDraft",""),prefs.getString("moveReturn",""));
        if(prefs.getBoolean("assets",false)){
            try{
                session.assetFilter=Math.max(0,Math.min(WorkshopSession.favoriteAssetFilter(),prefs.getInt("assetFilter",0)));
                session.restoreAssets(prefs.getString("assetsReturn","NAVIGATE"),prefs.getInt("assetIndex",0));
                session.selectAssetId(prefs.getString("assetId",""));
                String saved=prefs.getString("assetDraft",""),copy=prefs.getString("copyAsset","");
                if(!saved.isEmpty()&&session.mode==Mode.ASSETS){session.assetDraft=SpriteAsset.decode(Base64.decode(saved,Base64.NO_WRAP));session.mode=Mode.ASSET_SAVE;}
                session.restoreCategory(prefs.getInt("assetCategoryChoice",-1));
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
        if(!prefs.getBoolean("assets",false)&&!prefs.getString("copyAsset","").isEmpty())try{
            session.restoreBufferInsertion(SpriteAsset.decode(Base64.decode(prefs.getString("copyAsset",""),Base64.NO_WRAP)),prefs.getInt("copyX",-1),prefs.getInt("copyY",-1));
        }catch(Exception e){session.fail(e);}
        strokeRecoveryFailed.remove(activeId);
        String freehand=prefs.getString("freehand","");
        if(!freehand.isEmpty())try{session.restoreFreehand(Base64.decode(freehand,Base64.NO_WRAP));}
        catch(Exception e){strokeRecoveryFailed.add(activeId);session.fail(e);}
        sharedRecoveryFailed.remove(activeId);
        String shared=prefs.getString("sharedEdit","");
        if(!shared.isEmpty())try{session.restoreShared(Base64.decode(shared,Base64.NO_WRAP));}
        catch(Exception e){sharedRecoveryFailed.add(activeId);session.fail(e);}
    }
    private final java.util.Set<String> strokeRecoveryFailed=new java.util.HashSet<>();
    private final java.util.Set<String> sharedRecoveryFailed=new java.util.HashSet<>();
    private final java.util.Set<String> usesRecoveryFailed=new java.util.HashSet<>();
    private void restoreUses(){
        usesRecoveryFailed.remove(activeId);String encoded=prefs.getString("gameUses","");
        if(!encoded.isEmpty())try{if(session.codeDraft!=null)throw new IllegalArgumentException("Сначала заверши восстановленный черновик кода");session.restoreUses(Base64.decode(encoded,Base64.NO_WRAP));}
        catch(Exception e){usesRecoveryFailed.add(activeId);session.fail(e);}
    }
    private void restoreCodeDraft(){
        codeRecoveryFailed.remove(activeId);
        String encoded=prefs.getString("luaDraft","");
        if(!encoded.isEmpty())try{session.restoreCode(Base64.decode(encoded,Base64.NO_WRAP));}
        catch(Exception e){codeRecoveryFailed.add(activeId);Log.e(TAG,"Code draft retained in preferences",e);session.fail(e);}
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
        onRuntimeResume();
    }
    private void onRuntimeResume(){
        if(backend==null||session==null)return;
        art.pikoos.lab.core.RuntimeSession.Phase observed=backend.hasSession()?backend.sessionPhase():art.pikoos.lab.core.RuntimeSession.Phase.EXITED;
        if(backend.hasSession()&&!art.pikoos.lab.core.RuntimeSession.ended(observed)){
            runtimeGate=new LaunchView(this,this::runtimeAction,libraryPrefs.getBoolean("swapAB",false));
            runtimeGate.busy=false;runtimeGate.active=true;
            runtimeGate.message="A game is still open. Return to continue or finish it.";
            if(observed==art.pikoos.lab.core.RuntimeSession.Phase.PREPARING)runtimeGate.message="PICO-8 is starting. Return to check the game.";
            if(observed==art.pikoos.lab.core.RuntimeSession.Phase.UNKNOWN){runtimeGate.recovery=true;runtimeGate.message="The previous session could not be identified. X checks whether its runtime is idle and preserves the record as interrupted. It never closes a running game.";}
            setContentView(runtimeGate);runtimeGate.requestFocus();return;
        }
        if(runtimeGate!=null){runtimeGate=null;setContentView(showingFolders?folderView:showingPlay?playView:showingLibrary?shelf:surface);}
        SharedPreferences setup=getSharedPreferences("runtime-setup",0);
        if(setup.getBoolean("dispatched",false)){
            setup.edit().putBoolean("dispatched",false).commit();
            play.notice=observed==art.pikoos.lab.core.RuntimeSession.Phase.INTERRUPTED?"The PICO-8 test was interrupted. You can try again in runtime setup.":"The PICO-8 test has ended.";
            playView.invalidate();
        }
        if(session!=null&&awaitingReturn&&leftForRuntime){
            awaitingReturn=false;leftForRuntime=false;libraryPrefs.edit().putBoolean("awaitingReturn",false).apply();
            getSharedPreferences("moon-garden-ui",MODE_PRIVATE).edit().putBoolean("awaitingReturn",false).apply();
            if(playPrefs.getBoolean("runtime",false)){
                playPrefs.edit().putBoolean("runtime",false).apply();
                if(!showingPlay)showPlay();play.busy=false;play.notice=observed==art.pikoos.lab.core.RuntimeSession.Phase.INTERRUPTED?"The game was interrupted. Your library is ready.":"Back in Play. Choose your next game.";playView.invalidate();
            }else{session.notice="Сохранено";surface.invalidate();}
            Log.i(TAG,"host_resumed tool="+session.tool+" focus="+session.focus+" result=unknown");
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(runtimeGate==null&&!showingPlay&&!showingLibrary&&!showingFolders&&surface!=null&&surface.codeKey(event))return true;
        return input!=null&&input.key(event)||super.dispatchKeyEvent(event);
    }
    @Override public boolean onGenericMotionEvent(MotionEvent event){return input!=null&&input.motion(event)||super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){if(runtimeGate!=null)runtimeAction(Action.CANCEL);else if(showingFolders)folderView.action(Action.CANCEL);else if(showingPlay)playView.action(Action.CANCEL);else if(showingLibrary&&shelf!=null)shelf.action(Action.CANCEL);else if(surface!=null)surface.action(Action.CANCEL);else super.onBackPressed();}
}
