package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import art.pikoos.lab.core.VerifiedTransfer;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.UUID;

/** Explicit verified copy, never a storage activation or an uninstall-safe full backup. */
public final class StorageCopyActivity extends Activity {
    private static Job current;
    private TextView status;private Button primary,resume,back;private ControllerInput input;
    private String role;private int selected;private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,400);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(BackupRecovery.guard(this))return;role=getIntent().getStringExtra("role");
        if(!"PROJECTS".equals(role)&&!"DATA".equals(role)){finish();return;}
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,20,24,20);layout.setBackgroundColor(WorkshopView.COLORS[1]);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(WorkshopView.COLORS[1]);scroll.addView(layout);Typeface font=Typeface.createFromAsset(getAssets(),"Monocraft.ttf");
        TextView title=new TextView(this);title.setText("PikoNest / Copy "+(role.equals("PROJECTS")?"projects":"library"));title.setTextSize(23);title.setTextColor(WorkshopView.COLORS[7]);title.setTypeface(font);layout.addView(title);
        TextView info=new TextView(this);info.setText("Make a checked copy in the selected folder. Originals stay in PikoNest. This does not change working storage.\n\nProjects include their files and metadata; library includes sprites, presets and the sprite buffer. Settings, saves and unfinished editor drafts are not included.\n\nAn interrupted copy can be checked again with Resume. A damaged partial file needs a new copy; previous files are kept.");info.setTextSize(17);info.setTextColor(WorkshopView.COLORS[6]);info.setTypeface(font);layout.addView(info);
        status=new TextView(this);status.setTextSize(17);status.setTextColor(WorkshopView.COLORS[10]);status.setTypeface(font);status.setPadding(0,20,0,20);layout.addView(status);
        primary=button(layout,"New verified copy",font,()->start(false));resume=button(layout,"Resume last copy",font,()->start(true));back=button(layout,"Back",font,()->action(Action.CANCEL));
        boolean swap=getSharedPreferences("library-ui",0).getBoolean("swapAB",false);input=new ControllerInput(this::action,()->swap);setContentView(scroll);primary.requestFocus();
    }
    private Button button(LinearLayout parent,String title,Typeface font,Runnable run){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(18);b.setTypeface(font);b.setTextColor(WorkshopView.COLORS[10]);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(WorkshopView.COLORS[2]));b.setOnClickListener(v->run.run());parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
    private void action(Action a){
        if(a==Action.CANCEL){if(current!=null&&!current.done)current.cancelled=true;else finish();return;}
        if(current!=null&&!current.done)return;
        if(a==Action.UP)selected=Math.max(0,selected-1);if(a==Action.DOWN)selected=Math.min(2,selected+1);
        Button b=selected==0?primary:selected==1?resume:back;b.requestFocus();if(a==Action.CONFIRM)b.performClick();
    }
    private void start(boolean resumeOld){
        if(current!=null&&!current.done)return;
        current=new Job(getApplicationContext(),role,resumeOld);new Thread(current,"pikonest-storage-copy").start();render();
    }
    private void render(){if(status==null)return;boolean busy=current!=null&&!current.done;primary.setEnabled(!busy);resume.setEnabled(!busy);back.setText(busy?"Cancel copy":"Back");status.setText(current==null?"Choose New copy, or resume the last copy for this destination.":current.message);}
    @Override protected void onResume(){super.onResume();handler.post(refresh);getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);if(input!=null)input.reset();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return input!=null&&input.key(e)||super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){return input!=null&&input.motion(e)||super.onGenericMotionEvent(e);}
    @Override public void onBackPressed(){action(Action.CANCEL);}
    private static final class Job implements Runnable {
        final Context context;final String role;final boolean resume;volatile boolean done,cancelled;volatile String message="Checking destination...";
        Job(Context c,String r,boolean resume){context=c;role=r;this.resume=resume;}
        public void run(){
            try{
                if(role.equals("DATA")&&!LibraryStorage.identity(context).isEmpty())throw new Exception("Library uses an external workspace. Internal copies exclude newer external edits.");
                if(role.equals("PROJECTS")&&!ProjectStorage.identity(context).isEmpty())throw new Exception("Projects use an external workspace. The internal-copy path cannot back up newer external edits. Export the current cart from Projects.");
                SharedPreferences folders=context.getSharedPreferences("folder-setup",0),journal=context.getSharedPreferences("storage-copy-"+role,0);
                String location=folders.getString(role+".uri","");if(location.isEmpty())throw new Exception("Choose the destination folder in Folders first.");
                Uri tree=Uri.parse(location);FolderAccess.verify(context.getContentResolver(),tree,true);
                String id,name;
                if(resume){
                    if(!location.equals(journal.getString("tree","")))throw new Exception("No copy for this destination. Choose New copy.");
                    id=journal.getString("id","");name=journal.getString("name","");if(id.isEmpty())throw new Exception("No saved copy. Choose New copy.");
                }else{
                    if(cancelled)throw new java.util.concurrent.CancellationException();
                    name="PikoNest-"+role.toLowerCase(java.util.Locale.ROOT)+"-"+UUID.randomUUID();
                    Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
                    Uri folder=DocumentsContract.createDocument(context.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,name);
                    if(folder==null)throw new Exception("Cannot create copy folder");id=DocumentsContract.getDocumentId(folder);
                    if(!journal.edit().putString("tree",location).putString("id",id).putString("name",name).commit())throw new Exception("Cannot save copy progress. Originals and candidate folder kept.");
                }
                final String label=name;
                VerifiedTransfer.copy(StorageCopy.source(context.getFilesDir(),role.equals("PROJECTS")),new StorageCopy.Tree(context.getContentResolver(),tree,id),()->cancelled,(count,total,path)->message="Checking "+count+" / "+total+"\n"+path);
                message="Copy verified: "+label+"\nWorking storage is still inside PikoNest.";
            }catch(java.util.concurrent.CancellationException e){message="Copy cancelled. Originals and copied files kept. Resume to check, or start a new copy.";}
            catch(Exception e){message=(e.getMessage()==null?"Copy failed. Reconnect the folder and retry.":e.getMessage())+"\nOriginals were kept.";}
            finally{done=true;}
        }
    }
}
