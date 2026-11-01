package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import art.pikoos.lab.core.FirstRunSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

/** A short, skippable controller journey, using the workshop palette and font. */
final class FirstRunView extends View {
    private final FirstRunSetup state;private final boolean swap;private final Paint p=new Paint();
    private Canvas c;private float scale,w,h,left,width,button,downX,downY,scroll,maxScroll;
    FirstRunView(Context context,FirstRunSetup state,boolean swap){
        super(context);this.state=state;this.swap=swap;PixelText.configure(context,p);p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
        setContentDescription("Welcome to PikoNest. Left and right: setup steps. Confirm: choose. Back: set up later.");
    }
    void action(Action a){
        if(a==Action.UP||a==Action.DOWN)scroll=Math.max(0,Math.min(maxScroll,scroll+(a==Action.UP?-48:48)));
        else{int old=state.step;state.act(a);if(old!=state.step)scroll=0;}
        invalidate();
    }
    private void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    private void text(String s,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(s,x,y,p);}
    private void fit(String s,float x,float y,int size,int color,float max){p.setTextSize(PixelText.size(size));while(s.length()>1&&p.measureText(s)>max)s=s.substring(0,s.length()-2)+"…";text(s,x,y,size,color);}
    private float wrap(String s,float y,int color){
        String line="";p.setTextSize(PixelText.size(18));
        for(String word:s.split(" ")){String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&p.measureText(next)>width){text(line,left,y,18,color);y+=24;line=word;}else line=next;}
        if(!line.isEmpty()){text(line,left,y,18,color);y+=24;}return y;
    }
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.min(Math.max(1,Math.round(getResources().getDisplayMetrics().density)),Math.min(getWidth()/360f,getHeight()/640f));
        w=getWidth()/scale;h=getHeight()/scale;left=Math.max(20,(w-580)/2);width=Math.min(w-40,580);
        c.save();c.scale(scale,scale);rect(0,0,w,h,1);rect(0,0,w,42,2);
        text("PikoNest",left,30,27,7);text((state.step+1)+" / 4",left+width-75,29,18,14);
        fit(state.title(),left,87,27,7,width);
        for(int i=0;i<4;i++)rect(left+i*width/4,104,width/4-6,4,i==state.step?10:13);
        // Tiny cartridges in a nest; original palette art, no proprietary runtime assets.
        float mid=left+width/2;
        for(int i=0;i<3;i++){float x=mid-62+i*43;rect(x,133,37,50,2);rect(x+5,139,27,25,new int[]{12,11,14}[i]);rect(x+9,169,19,5,6);}
        rect(mid-78,185,156,5,4);rect(mid-66,190,132,5,9);rect(mid-49,195,98,5,4);
        button=h-130;c.save();c.clipRect(left,212,left+width,button-12);c.translate(0,-scroll);
        float y=wrap(state.body(),230,6);
        y=wrap(state.status(),y+12,state.busy?10:11);
        if(!state.problem.isEmpty())y=wrap(state.problem,y+4,9);
        maxScroll=Math.max(0,y-(button-12));c.restore();if(scroll>maxScroll){scroll=maxScroll;invalidate();}
        rect(left,button,width,46,state.busy?5:2);rect(left,button,4,46,10);
        fit(state.busy?"Checking...":(swap?"B":"A")+"  "+state.primary(),left+14,button+30,21,10,width-28);
        fit(maxScroll>0?"Up / down: read more":"Left / right: "+(state.step==3?"review steps":"change step"),left,button+75,17,14,width);
        rect(0,h-42,w,42,0);text((swap?"A":"B")+(state.busy?" Cancel check":" Set up later"),left,h-15,18,6);
        text(state.step==3?"< Back":"Next >",left+width-80,h-15,18,14);c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(downY>212&&downY<button&&Math.abs(y-downY)>20){scroll=Math.max(0,Math.min(maxScroll,scroll+downY-y));invalidate();return true;}
            if(Math.abs(x-downX)<20&&Math.abs(y-downY)<20){
                if(y>=h-42)action(x>left+width-100?(state.step==3?Action.PREVIOUS:Action.NEXT):Action.CANCEL);
                else if(y>=button&&y<button+46)action(Action.CONFIRM);
                else if(y<115&&y>95)action(x<left+width/2?Action.PREVIOUS:Action.NEXT);
            }performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
