package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.function.Consumer;

/** Scrollable explanation plus three fixed controller/touch actions. */
final class BackupView extends View {
    int selected;boolean busy,cancellable=true;String message="";String[] options={"Create backup ZIP","Restore from backup ZIP","Back"};
    private final Consumer<Action> action;private final Paint p=new Paint();private Canvas c;private float scale,w,h,left,width,top,scroll,max,downX,downY;
    BackupView(Context context,Consumer<Action> action){super(context);this.action=action;PixelText.configure(context,p);p.setAntiAlias(false);setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Backup and restore. Up/down selects. Confirm opens. Back cancels. Left/right scrolls the explanation.");}
    void act(Action a){if(a==Action.LEFT||a==Action.RIGHT){scroll=Math.max(0,Math.min(max,scroll+(a==Action.LEFT?-64:64)));invalidate();}else action.accept(a);}
    private void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    private void text(String s,float x,float y,int size,int color){p.setTextSize(PixelText.size(size));p.setColor(WorkshopView.COLORS[color]);c.drawText(s,x,y,p);}
    private void fit(String s,float x,float y,int size,int color,float width){p.setTextSize(PixelText.size(size));while(s.length()>1&&p.measureText(s)>width)s=s.substring(0,s.length()-2)+"…";text(s,x,y,size,color);}
    @Override protected void onDraw(Canvas canvas){c=canvas;scale=Math.min(Math.max(1,Math.round(getResources().getDisplayMetrics().density)),Math.min(getWidth()/360f,getHeight()/540f));w=getWidth()/scale;h=getHeight()/scale;left=Math.max(20,(w-620)/2);width=Math.min(w-40,620);top=h-210;
        c.save();c.scale(scale,scale);rect(0,0,w,h,1);rect(0,0,w,42,2);text("PikoNest",left,30,26,7);text("Backup & restore",left,86,26,7);
        c.save();c.clipRect(left,110,left+width,top-18);c.translate(0,-scroll);float y=134;String line="";p.setTextSize(PixelText.size(18));for(String word:message.split(" ")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&p.measureText(next)>width){text(line,left,y,18,6);y+=25;line=word;}else line=next;}if(!line.isEmpty()){text(line,left,y,18,6);y+=25;}max=Math.max(0,y-(top-18));c.restore();if(scroll>max){scroll=max;invalidate();}
        for(int i=0;i<options.length;i++){rect(left,top+i*52,width,44,busy?5:i==selected?2:0);rect(left,top+i*52,4,44,i==selected?10:13);fit(busy&&i==0?"Working...":options[i],left+14,top+i*52+29,20,i==selected?10:7,width-28);}
        fit(busy?(cancellable?"Back: cancel / arrows: read":"Restoring / arrows: read"):max>0?"Left / right: read more":"Up / down: choose",left,h-30,16,14,width);c.restore();}
    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/scale,y=e.getY()/scale;if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){if(downY<top&&Math.abs(y-downY)>20){scroll=Math.max(0,Math.min(max,scroll+downY-y));invalidate();}else if(!busy&&Math.abs(x-downX)<20&&Math.abs(y-downY)<20&&y>=top&&y<top+options.length*52){int target=(int)((y-top)/52);while(selected<target)action.accept(Action.DOWN);while(selected>target)action.accept(Action.UP);action.accept(Action.CONFIRM);}performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
