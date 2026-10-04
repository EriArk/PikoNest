package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.ShelfMenu;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.ArrayList;

/** Shared shelf layout and visual roles. No project or navigation state lives here. */
abstract class ShelfSurface extends View {
    protected final Paint p=new Paint();
    protected Canvas c;
    protected float scale,w,h,downX,downY;
    protected final boolean swap;
    protected final ArrayList<Hit> hits=new ArrayList<>();
    private int[] coverPixels;
    private Bitmap coverBitmap;
    protected static final class Hit {
        final RectF rect;final Runnable run;
        Hit(float x,float y,float w,float h,Runnable run){rect=new RectF(x,y,x+w,y+h);this.run=run;}
    }
    ShelfSurface(Context context,boolean swap){
        super(context);this.swap=swap;PixelText.configure(context,p);p.setFilterBitmap(false);
        setFocusable(true);setFocusableInTouchMode(true);
    }
    abstract void action(Action action);
    protected String ok(){return swap?"B":"A";}
    protected String back(){return swap?"A":"B";}
    protected void begin(Canvas canvas){
        c=canvas;scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        // Keep text at its readable size; narrow screens change the layout, not the font.
        w=getWidth()/scale;h=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();rect(0,0,w,h,1);
    }
    protected void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    protected void outline(float x,float y,float width,float height,int color){rect(x,y,width,2,color);rect(x,y+height-2,width,2,color);rect(x,y,2,height,color);rect(x+width-2,y,2,height,color);}
    protected void text(String value,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(value,x,y,p);}
    protected void fit(String value,float x,float y,int size,int color,float width){
        p.setTextSize(PixelText.size(size));String cut=value;
        while(cut.length()>0&&p.measureText(cut)>width)cut=cut.substring(0,cut.length()-1);
        if(!cut.equals(value)&&cut.length()>1)cut=cut.substring(0,cut.length()-1)+"…";
        text(cut,x,y,size,color);
    }
    protected float wrap(String value,float x,float y,float width,int color,int lines){
        p.setTextSize(PixelText.size(18));
        for(int line=0;line<lines&&!value.isEmpty();line++){
            int count=p.breakText(value,true,width,null);if(count==0)break;
            if(count<value.length()){int space=value.lastIndexOf(' ',count);if(space>0)count=space;}
            text(value.substring(0,count),x,y,18,color);value=value.substring(count).trim();y+=24;
        }
        return y;
    }
    protected void button(String label,float x,float y,float width,boolean selected,Runnable run){
        if(selected){rect(x,y,width,44,2);rect(x,y,3,44,10);}
        fit(label,x+12,y+29,18,selected?10:7,width-24);hits.add(new Hit(x,y,width,44,run));
    }
    protected void header(boolean workshop,Runnable play,Runnable projects){
        text("PikoNest",16,31,24,14);
        if(w>=540)text("little worlds, made here",175,30,18,6);
        button("Play",16,48,90,false,play);
        button("Workshop",112,48,145,false,projects);
        rect(workshop?124:28,91,workshop?116:56,2,14);
        // Splore is not yet wired. Do not present a non-working navigation button.
        button("≡",w-60,48,44,false,()->action(Action.MENU));
        rect(16,98,w-32,1,13);
    }
    protected int rows(){return Math.max(2,(int)((h-264)/52));}
    protected float listWidth(){return w>=540?(w-56)*.60f:w-32;}
    protected void row(String title,String subtitle,int index,int selected,int start,float width,Runnable run){
        float y=148+(index-start)*52;
        if(index==selected){rect(16,y,width,48,2);rect(16,y,3,48,10);}
        fit(title,28,y+22,18,index==selected?10:7,width-24);
        fit(subtitle,28,y+43,18,6,width-24);
        hits.add(new Hit(16,y,width,48,run));
    }
    protected void footer(String primary,String secondary,Action second){
        rect(16,h-53,w-32,1,13);
        float quarter=(w-32)/4;
        String[] labels={ok()+" "+primary,back()+" Back",secondary,"≡ Menu"};
        Action[] actions={Action.CONFIRM,Action.CANCEL,second,Action.MENU};
        for(int i=0;i<4;i++){final Action a=actions[i];fit(labels[i],16+i*quarter,h-22,18,i==0?14:6,quarter-6);
            hits.add(new Hit(16+i*quarter,h-50,quarter,48,()->action(a)));}
    }
    protected void menu(ShelfMenu menu){
        if(!menu.open)return;
        hits.clear();p.setColor(0xe6000000);c.drawRect(0,0,w,h,p);
        float width=Math.min(420,w-32),height=menu.commands.length*44+100,x=(w-width)/2,y=(h-height)/2;
        rect(x,y,width,height,1);rect(x,y,width,3,14);text("Actions",x+16,y+34,24,14);
        for(int i=0;i<menu.commands.length;i++){final int chosen=i;
            button(menu.commands[i].label,x+12,y+48+i*44,width-24,i==menu.selected,
                ()->{menu.selected=chosen;action(Action.CONFIRM);});}
        text(ok()+" Choose",x+16,y+height-15,18,6);
        button(back()+" Back",x+width-130,y+height-44,118,false,()->action(Action.CANCEL));
    }
    protected void cover(int[] pixels,float x,float y,float size){
        // Integer device-pixel scaling of real 128x128 cartridge artwork.
        float actual=Math.max(1,(int)(size*scale/128))*128/scale;
        x+=(size-actual)/2;
        if(pixels!=null){if(coverPixels!=pixels){coverPixels=pixels;coverBitmap=Bitmap.createBitmap(pixels,128,128,Bitmap.Config.ARGB_8888);}
            c.drawBitmap(coverBitmap,null,new RectF(x,y,x+actual,y+actual),p);}
        else{
            float u=actual/64;rect(x+8*u,y+4*u,48*u,56*u,2);rect(x+14*u,y+10*u,36*u,28*u,14);
            rect(x+21*u,y+17*u,5*u,12*u,7);rect(x+37*u,y+17*u,5*u,12*u,7);rect(x+20*u,y+48*u,24*u,5*u,5);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        float x=event.getX()/scale,y=event.getY()/scale;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){
            if(Math.abs(x-downX)<20&&Math.abs(y-downY)<20)
                for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).rect.contains(x,y)){hits.get(i).run.run();break;}
            performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
