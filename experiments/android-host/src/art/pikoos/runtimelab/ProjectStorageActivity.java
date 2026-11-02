package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import art.pikoos.lab.core.*;
import org.json.JSONObject;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Explicit project-only activation/reconnection; internal originals remain a separate older workspace. */
public final class ProjectStorageActivity extends Activity {
    private static Job current;private TextView status;private Button[] buttons;private ControllerInput input;private int selected;private boolean library;
    private final Handler handler=new Handler();private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,500);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);library=getIntent().getBooleanExtra("library",false);if(current!=null&&current.done&&current.library!=library)current=null;LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(24,20,24,20);body.setBackgroundColor(WorkshopView.COLORS[1]);
        Typeface font=Typeface.createFromAsset(getAssets(),"Monocraft.ttf");
        TextView title=text(library?"PikoNest / Library storage":"PikoNest / Project storage",font,23,7);body.addView(title);
        body.addView(text(library?"Use selected folder copies sprites, presets and the shared sprite buffer to a checked external library. Internal originals stay separate. Projects, settings, drafts and saves keep their current locations.\n\nKeep this app installed: recovery records are private. Reconnect chooses the SAME folder. Returning to internal originals does not merge newer external changes.":"Use selected folder copies your current internal projects to a new workspace, checks them, then switches Projects there. Originals stay inside the app.\n\nLibrary, settings, unfinished drafts and saves keep their current locations. Keep this app installed: save-recovery records are private.\n\nReconnect restores access to the SAME external folder. Internal originals are older copies; switching back does not merge later external edits.",font,17,6));
        status=text("",font,17,10);status.setPadding(0,18,0,18);body.addView(status);
        String[] names={library?"Use selected folder for library":"Use selected folder for projects","Reconnect active folder","Use older internal originals","Back"};buttons=new Button[names.length];
        for(int i=0;i<names.length;i++){final int index=i;Button b=new Button(this);b.setText(names[i]);b.setAllCaps(false);b.setTextSize(18);b.setTypeface(font);b.setTextColor(WorkshopView.COLORS[10]);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(WorkshopView.COLORS[2]));b.setOnClickListener(v->choose(index));body.addView(b,new LinearLayout.LayoutParams(-1,-2));buttons[i]=b;}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(WorkshopView.COLORS[1]);scroll.addView(body);setContentView(scroll);buttons[0].requestFocus();
        boolean swap=getSharedPreferences("library-ui",0).getBoolean("swapAB",false);input=new ControllerInput(this::action,()->swap);
    }
    private String identity(){return library?LibraryStorage.identity(this):ProjectStorage.identity(this);}
    private void select(String value)throws Exception{if(library)LibraryStorage.select(this,value);else ProjectStorage.select(this,value);}
    private TextView text(String text,Typeface font,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTypeface(font);t.setTextSize(size);t.setTextColor(WorkshopView.COLORS[color]);return t;}
    private void choose(int index){
        if(current!=null&&!current.done){if(index==3)synchronized(current){if(!current.done)current.cancelled=true;}return;}
        if(index==3){finish();return;}
        try{
            if(index==0){ExternalPicoBackend backend=new ExternalPicoBackend(this);if(backend.hasSession()&&!RuntimeSession.ended(backend.sessionPhase()))throw new Exception("Finish the active game before changing storage.");synchronized(ProjectStore.class){synchronized(LocalLibraryStorage.class){if(ProjectStorage.activating||LibraryStorage.activating)throw new Exception("Another storage activation is running");if(library)LibraryStorage.activating=true;else ProjectStorage.activating=true;current=new Job(getApplicationContext(),library);}}new Thread(current,"pikonest-project-activation").start();}
            if(index==1){if(identity().isEmpty())throw new Exception("No external workspace is active.");startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),90);}
            if(index==2){select("");current=null;status.setText("Using internal originals. External workspace kept separately.");}
        }catch(Exception e){status.setText(e.getMessage());}render();
    }
    private void action(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){choose(3);return;}
        if(current!=null&&!current.done)return;
        if(a==WorkshopSession.Action.UP)selected=Math.max(0,selected-1);if(a==WorkshopSession.Action.DOWN)selected=Math.min(3,selected+1);
        buttons[selected].requestFocus();if(a==WorkshopSession.Action.CONFIRM)choose(selected);
    }
    private void render(){if(buttons==null)return;boolean busy=current!=null&&!current.done;for(int i=0;i<3;i++)buttons[i].setEnabled(!busy);buttons[3].setText(busy?"Cancel activation":"Back");if(current!=null)status.setText(current.message);else if(status.getText().length()==0)status.setText(library?LibraryStorage.description(this):ProjectStorage.description(this));}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=90)return;
        try{
            if(result!=RESULT_OK||data==null||data.getData()==null)throw new Exception("Reconnection cancelled. Location kept.");
            String active=new JSONObject(identity()).getString("tree");Uri uri=data.getData();
            if(!uri.toString().equals(active))throw new Exception("Choose the original active folder. A different folder cannot replace its resources.");
            getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            current=null;status.setText("Access reconnected. Back reopens the workshop; unavailable or conflicting files still need review.");
        }catch(Exception e){current=null;status.setText(e.getMessage());}
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);if(input!=null)input.reset();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){choose(3);}
    private static final class Job implements Runnable {
        final Context context;final boolean library;volatile boolean done,cancelled;volatile String message="Checking selected folder...";
        Job(Context c,boolean library){context=c;this.library=library;}
        public void run(){
            boolean selecting=false;
            try{
                if(!(library?LibraryStorage.identity(context):ProjectStorage.identity(context)).isEmpty())throw new Exception("An external workspace is already selected. Reconnect it, or explicitly return to internal originals first.");
                String uri=context.getSharedPreferences("folder-setup",0).getString(library?"DATA.uri":"PROJECTS.uri","");if(uri.isEmpty())throw new Exception("Choose the destination in Folders first.");
                Uri tree=Uri.parse(uri);FolderAccess.verify(context.getContentResolver(),tree,true);
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
                String label=(library?"PikoNest-library-":"PikoNest-workspace-")+UUID.randomUUID();Uri candidate=DocumentsContract.createDocument(context.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,label);
                if(candidate==null)throw new Exception("Cannot create workspace");String root=DocumentsContract.getDocumentId(candidate);
                StorageCopy.Tree target=new StorageCopy.Tree(context.getContentResolver(),tree,root);
                VerifiedTransfer.Source source=StorageCopy.source(context.getFilesDir(),!library);
                VerifiedTransfer.copy(source,target,()->cancelled,(n,total,path)->message="Checking projects "+n+" / "+total+"\n"+path);
                if(library){
                    SafLibraryStorage adapter=new SafLibraryStorage(context,uri,root);
                    LibraryRepository local=new LibraryRepository(new LocalLibraryStorage(context.getFilesDir()));
                    LibraryRepository external=new LibraryRepository(adapter);
                    Map<String,byte[]> expected=local.snapshot(),actual=external.snapshot();
                    if(!expected.keySet().equals(actual.keySet()))throw new Exception("Library contents changed. Retry activation; originals kept.");
                    for(String path:expected.keySet()){
                        if(cancelled)throw new java.util.concurrent.CancellationException();
                        byte[] bytes=expected.get(path);if(!Arrays.equals(bytes,actual.get(path)))throw new Exception("Library changed during activation");
                        adapter.replace(path,bytes,bytes);
                    }
                }else{
                SafProjectStore adapter=new SafProjectStore(context,uri,root);
                ProjectRepository repository=new ProjectRepository(adapter);ProjectRepository local=new ProjectRepository(new ProjectStore(context.getFilesDir()));
                List<String> ids=local.ids();if(!new HashSet<>(ids).equals(new HashSet<>(repository.ids())))throw new Exception("Project list changed. Originals and candidate kept; retry activation.");
                for(String id:ids){
                    if(cancelled)throw new java.util.concurrent.CancellationException();byte[] original=local.read(id);
                    if(!Arrays.equals(original,repository.read(id))||!local.title(id).equals(repository.title(id)))throw new Exception("Project changed before activation. Originals and candidate kept.");
                    target.renameSupported(target.resolve("projects/"+id+"/game.p8",false));
                    // Prove the complete save/recovery protocol on this candidate, not on the originals.
                    adapter.replace(id,original,original);
                }
                }
                // Exercise the actual provider's rename without changing a project or deleting evidence.
                String probe=".pikonest-rename-"+UUID.randomUUID();try(java.io.OutputStream out=target.create(probe)){out.write("PikoNest rename check".getBytes(StandardCharsets.UTF_8));}
                target.rename(probe,probe+"-checked");
                try(java.io.InputStream in=target.read(probe+"-checked")){if(!Arrays.equals("PikoNest rename check".getBytes(StandardCharsets.UTF_8),CartridgeImport.readBounded(in)))throw new Exception("Provider rename check failed");}
                target.directory(probe+"-directory");target.rename(probe+"-directory",probe+"-directory-checked");
                VerifiedTransfer.copy(source,target,()->cancelled,(n,total,path)->message="Final check "+n+" / "+total+"\n"+path);
                synchronized(this){if(cancelled)throw new java.util.concurrent.CancellationException();selecting=true;String location=new JSONObject().put("tree",uri).put("root",root).put("label",label).toString();if(library)LibraryStorage.select(context,location);else ProjectStorage.select(context,location);message=(library?"External library selected: ":"External Projects selected: ")+label+". Back reopens the workshop. Internal originals kept.";done=true;}
            }catch(java.util.concurrent.CancellationException e){message="Activation cancelled. Original location and all candidate files kept.";}
            catch(Exception e){message=(e.getMessage()==null?"Activation failed. Reconnect storage and retry.":e.getMessage())+(selecting?"\nReopen storage controls to check the selection. Both workspaces were kept.":"\nPrevious location kept.");}
            finally{if(library)LibraryStorage.activating=false;else ProjectStorage.activating=false;done=true;}
        }
    }
}
