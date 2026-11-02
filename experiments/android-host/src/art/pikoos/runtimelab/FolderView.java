package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import art.pikoos.lab.core.FolderSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Same pixel palette and semantic controls as the workshop. */
final class FolderView extends View {
    private final FolderSetup s;
    private final boolean swap;
    private final Runnable changed;
    private final Runnable runtime;
    private final Paint p=new Paint();
    private Canvas c;
    private float scale,w,h,downX,downY;
    private static final String[] TITLES={"Games","Splore","Projects","Asset library"};
    private static final String[] HINTS={"Read games; originals unchanged.","Splore still uses app storage.","Start opens project location / recovery.","Start opens library location / recovery."};
    FolderView(Context context,FolderSetup session,boolean swap,Runnable changed,Runnable runtime){
        super(context);s=session;this.swap=swap;this.changed=changed;this.runtime=runtime;
        PixelText.configure(context,p);p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
        setContentDescription("Folders. Up/down: role. Confirm: choose folder. X: check access. Select: setup guide. Back: return.");
    }
    void action(Action a){
        if(a==Action.UNDO&&!s.busy){((android.app.Activity)getContext()).startActivity(new android.content.Intent(getContext(),BackupActivity.class));return;}
        if(a==Action.TEST&&!s.busy&&s.selected==2){((android.app.Activity)getContext()).startActivity(new android.content.Intent(getContext(),ProjectStorageActivity.class));return;}
        if(a==Action.TEST&&!s.busy&&s.selected==3){((android.app.Activity)getContext()).startActivity(new android.content.Intent(getContext(),ProjectStorageActivity.class).putExtra("library",true));return;}
        if(a==Action.MENU&&!s.busy){((android.app.Activity)getContext()).startActivityForResult(new android.content.Intent(getContext(),FirstRunActivity.class),87);return;}if(a==Action.NEXT&&!s.busy){runtime.run();return;}s.act(a);changed.run();invalidate();}
    private void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    private void text(String value,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(value,x,y,p);}
    private void fit(String value,float x,float y,int size,int color,float max){p.setTextSize(PixelText.size(size));while(value.length()>1&&p.measureText(value)>max)value=value.substring(0,value.length()-2)+"…";text(value,x,y,size,color);}
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        scale=Math.min(scale,Math.min(getWidth()/360f,getHeight()/540f));w=getWidth()/scale;h=getHeight()/scale;
        c.save();c.scale(scale,scale);rect(0,0,w,h,1);rect(0,0,w,42,2);
        text("PikoNest",16,30,24,7);text("R PICO8",w-130,28,18,14);
        text("Folders",16,82,28,7);
        fit("Choose storage or press Y for backups.",16,108,17,6,w-32);
        for(int i=0;i<4;i++){
            FolderSetup.Entry e=s.entry(i);float y=124+i*69;boolean selected=s.selected==i;
            rect(16,y,w-32,62,selected?2:0);rect(16,y,4,62,selected?10:13);
            fit(TITLES[i],30,y+23,21,selected?10:7,w-190);
            String state=e.state==FolderSetup.State.READY?(i==0?"Connected":(i==2&&ProjectStorage.selectedActive(getContext(),e.location)||i==3&&LibraryStorage.selectedActive(getContext(),e.location))?"Active":"Reserved"):e.state==FolderSetup.State.CHECKING?"Checking":e.state==FolderSetup.State.UNAVAILABLE?"Reconnect":e.state==FolderSetup.State.UNKNOWN?"Check":"Choose";
            fit(state,w-145,y+23,16,e.state==FolderSetup.State.READY?11:e.state==FolderSetup.State.UNAVAILABLE?9:6,115);
            fit(e.location.isEmpty()?"No external folder selected":e.name,30,y+48,17,6,w-60);
        }
        FolderSetup.Entry e=s.entry(s.selected);
        fit(e.problem.isEmpty()?HINTS[s.selected]:e.problem,16,423,17,e.problem.isEmpty()?6:9,w-32);
        fit(s.notice.isEmpty()?(swap?"B":"A")+" choose / reconnect · X check":s.notice,16,451,18,10,w-32);
        fit(s.selected==2?"Start: project storage / Select: setup":s.selected==3?"Start: library storage / Select: setup": "Select: setup guide",16,478,16,13,w-32);
        rect(0,h-44,w,44,0);text((swap?"B":"A")+" Choose",12,h-16,18,10);
        text("X Check",w/3+8,h-16,18,14);text((swap?"A":"B")+" Back",w*2/3+8,h-16,18,6);
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(Math.abs(x-downX)>20||Math.abs(y-downY)>20)return true;
            if(y>=90&&y<116)action(Action.UNDO);
            else if(y<42&&x>w-140)action(Action.NEXT);
            else if(y>=460&&y<490)action(s.selected>=2&&x<w/2?Action.TEST:Action.MENU);
            else if(y>=h-44)action(x<w/3?Action.CONFIRM:x<w*2/3?Action.CONTEXT:Action.CANCEL);
            else if(x>=16&&x<=w-16&&y>=124&&y<400){s.select((int)(y-124)/69);changed.run();invalidate();}
            performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
