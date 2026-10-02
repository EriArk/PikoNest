package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.HeroBinding;
import art.pikoos.lab.core.SpriteRegion;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.ArrayList;

/** Cartridge shelf using the workshop's palette/type and semantic input. */
final class LibraryView extends View {
    private final LibrarySession s;
    private final Runnable changed;
    private final String active;
    private final boolean swap;
    private final Paint p=new Paint();
    private final ArrayList<Hit> hits=new ArrayList<>();
    private Canvas c;
    private float scale,w,h,downX,downY;
    private static final class Hit {
        final RectF rect;final Runnable run;
        Hit(float x,float y,float w,float h,Runnable run){rect=new RectF(x,y,x+w,y+h);this.run=run;}
    }
    LibraryView(Context context,LibrarySession session,String active,boolean swap,Runnable changed){
        super(context);s=session;this.active=active;this.swap=swap;this.changed=changed;
        p.setTypeface(Typeface.createFromAsset(context.getAssets(),"Tiny5-Regular.ttf"));p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
        setContentDescription("Мои игры. Влево и вправо: картридж, вниз: новая игра или копия, подтвердить: открыть.");
    }
    void action(Action a){s.act(a);changed.run();invalidate();}
    private void rect(float x,float y,float w,float h,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+w,y+h,p);}
    private void outline(float x,float y,float w,float h,int color){rect(x,y,w,3,color);rect(x,y+h-3,w,3,color);rect(x,y,3,h,color);rect(x+w-3,y,3,h,color);}
    private void text(String value,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(size);c.drawText(value,x,y,p);}
    private void fit(String value,float x,float y,int size,int color,float max){p.setTextSize(size);while(value.length()>1&&p.measureText(value)>max)value=value.substring(0,value.length()-2)+"…";text(value,x,y,size,color);}
    private void button(String label,float x,float y,float bw,boolean selected,Runnable run){rect(x,y,bw,44,selected?10:2);fit(label,x+12,y+29,20,selected?1:7,bw-24);hits.add(new Hit(x,y,bw,44,run));}
    private String ok(){return swap?"B":"A";}
    private String back(){return swap?"A":"B";}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);c=canvas;scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        if(getWidth()/scale<360)scale=getWidth()/360f;if(getHeight()/scale<480)scale=getHeight()/480f;
        w=getWidth()/scale;h=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();
        rect(0,0,w,h,1);rect(0,0,w,42,2);text("PIKOOS",16,30,28,7);text("твои маленькие миры",170,28,16,14);
        text("Мои игры",16,85,30,7);
        text(s.entries().isEmpty()?"Пока ни одной":(s.selected+1)+" / "+s.entries().size(),w-88,83,18,6);
        int columns=w>=550?3:2,start=s.selected/columns*columns;
        float gap=14,cw=(w-32-gap*(columns-1))/columns,top=112,bottom=h-154,ch=bottom-top;
        for(int col=0;col<columns;col++){
            final int index=start+col;if(index>=s.entries().size())break;
            LibrarySession.Entry entry=s.entries().get(index);float x=16+col*(cw+gap);
            boolean chosen=s.focus==0&&index==s.selected;
            rect(x,top,cw,ch,0);outline(x,top,cw,ch,chosen?10:index==s.selected?14:13);
            float art=Math.min(cw-20,ch-93);art=Math.max(32,(float)Math.floor(art*scale/64)*64/scale);
            artwork(entry.cart,x+(cw-art)/2,top+12,art);
            if(index==s.selected&&s.focus!=0)rect(x+cw-16,top+8,8,8,14);
            fit(entry.title,x+10,bottom-56,22,chosen?10:7,cw-20);
            fit(entry.cart==null?"Не удалось открыть":entry.id.equals(active)?"Продолжить работу":"Мой проект",x+10,bottom-31,16,entry.cart==null?9:6,cw-20);
            for(int pin=0;pin<8;pin++)rect(x+12+pin*(cw-24)/8,bottom-17,5,7,5);
            hits.add(new Hit(x,top,cw,ch,()->{s.choose(index);changed.run();invalidate();}));
        }
        if(s.entries().isEmpty()){text("Здесь будут твои картриджи",24,185,22,14);text("Начни с маленькой игры ниже",24,219,18,6);}
        fit("← → выбрать игру · ↓ новая игра или копия",16,h-133,16,6,w-32);
        button("+ Новая игра",16,h-112,(w-44)/2,s.focus==1,()->{s.command(1);changed.run();invalidate();});
        button("Копия выбранной",28+(w-44)/2,h-112,(w-44)/2,s.focus==2,()->{s.command(2);changed.run();invalidate();});
        rect(0,h-44,w,44,0);
        text(ok()+(s.focus==0?" открыть":s.focus==1?" создать":" копия"),12,h-16,18,10);text(back()+" мастерская",w<500?120:168,h-16,18,6);
        if(w>=500)text("X копия",w-105,h-16,18,14);
        hits.add(new Hit(0,h-44,116,44,()->action(Action.CONFIRM)));
        hits.add(new Hit(w<500?120:168,h-44,155,44,()->action(Action.CANCEL)));
        if(s.mode!=LibrarySession.Mode.SHELF)dialog();
        c.restore();
    }
    private void artwork(WorkshopCartridge cart,float x,float y,float size){
        float unit=size/64;c.save();c.translate(x,y);c.scale(unit,unit);
        rect(0,0,64,64,1);
        for(int i=0;i<13;i++)rect((i*19+3)%64,(i*11+2)%32,1,1,i%3==0?7:13);
        rect(47,6,7,9,15);rect(50,4,6,8,1);rect(0,49,64,15,2);rect(0,49,64,3,3);
        for(int i=0;i<5;i++){rect(3+i*13,54,4,2,4);rect(5+i*13,45,1,4,3);rect(4+i*13,44,3,2,14);}
        if(cart!=null){
            HeroBinding hero=cart.hero();SpriteRegion r=hero.image;
            float cell=Math.min(1,Math.min(48f/r.width,38f/r.height));
            float hx=32-(hero.left+hero.width/2f)*cell,hy=49-(hero.top+hero.height)*cell;
            for(int py=0;py<r.height;py++)for(int px=0;px<r.width;px++){int color=cart.pixel(r,px,py);if(color!=0)rect(hx+px*cell,hy+py*cell,cell,cell,color);}
        }
        else {rect(28,22,7,15,9);rect(28,40,7,4,9);}
        c.restore();
    }
    private void dialog(){
        hits.clear();p.setColor(0xdd000000);c.drawRect(0,0,w,h,p);
        float dw=Math.min(w-32,460),x=(w-dw)/2,y=(h-300)/2;
        rect(x,y,dw,300,1);outline(x,y,dw,300,14);
        boolean create=s.mode==LibrarySession.Mode.CREATE;
        text(create?"Новая маленькая игра":"Не получилось",x+18,y+40,26,create?14:9);
        if(create){
            text("Начнём с лунного сада:",x+18,y+86,20,7);
            text("герой, прыжок и платформы.",x+18,y+115,20,7);
            text("Рисуй и меняй всё, что уже",x+18,y+158,18,6);
            text("умеют инструменты мастерской.",x+18,y+183,18,6);
            text("Это будет отдельный проект.",x+18,y+213,18,11);
        }else{
            text("Исходные проекты сохранены.",x+18,y+82,18,7);
            String message=s.error;
            for(int row=0;row<4&&!message.isEmpty();row++){
                p.setTextSize(18);int count=message.length();while(count>1&&p.measureText(message.substring(0,count))>dw-36)count--;
                text(message.substring(0,count),x+18,y+119+row*25,18,6);message=message.substring(count);
            }
        }
        button(ok()+(create?" Создать":" Назад"),x+16,y+238,create?(dw-44)/2:dw-32,true,()->action(Action.CONFIRM));
        if(create)button(back()+" Отмена",x+28+(dw-44)/2,y+238,(dw-44)/2,false,()->action(Action.CANCEL));
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(s.mode==LibrarySession.Mode.SHELF&&downY>105&&downY<h-154&&Math.abs(x-downX)>40){action(x<downX?Action.RIGHT:Action.LEFT);return true;}
            if(Math.abs(x-downX)>20||Math.abs(y-downY)>20)return true;
            for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).rect.contains(x,y)){hits.get(i).run.run();break;}
            performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
