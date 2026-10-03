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
    private final Paint p=new Paint();
    private Canvas c;
    private float scale,w,h,downX,downY;
    private static final String[] TITLES={"Готовые игры","Загрузки Splore","Свои проекты","Библиотека и данные"};
    private static final String[] HINTS={"Чтение игр. Исходные файлы не меняются.","Место загрузок. Связь со Splore — позже.","Перенос нынешних проектов — отдельный шаг.","Спрайты, музыка и другие ресурсы для повторного использования."};
    FolderView(Context context,FolderSetup session,boolean swap,Runnable changed){
        super(context);s=session;this.swap=swap;this.changed=changed;
        p.setTypeface(Typeface.createFromAsset(context.getAssets(),"Tiny5-Regular.ttf"));p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
        setContentDescription("Папки. Вверх и вниз: назначение. A: выбрать папку. X: проверить доступ. B: на полку.");
    }
    void action(Action a){s.act(a);changed.run();invalidate();}
    private void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    private void text(String value,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(size);c.drawText(value,x,y,p);}
    private void fit(String value,float x,float y,int size,int color,float max){p.setTextSize(size);while(value.length()>1&&p.measureText(value)>max)value=value.substring(0,value.length()-2)+"…";text(value,x,y,size,color);}
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        scale=Math.min(scale,Math.min(getWidth()/360f,getHeight()/540f));w=getWidth()/scale;h=getHeight()/scale;
        c.save();c.scale(scale,scale);rect(0,0,w,h,1);rect(0,0,w,42,2);
        text("PIKOOS",16,30,28,7);text("дом для твоих игр",170,28,16,14);
        text("Папки мастерской",16,82,28,7);
        fit("Выбери места. Проекты пока хранятся в приложении.",16,108,17,6,w-32);
        for(int i=0;i<4;i++){
            FolderSetup.Entry e=s.entry(i);float y=124+i*69;boolean selected=s.selected==i;
            rect(16,y,w-32,62,selected?2:0);rect(16,y,4,62,selected?10:13);
            fit(TITLES[i],30,y+23,21,selected?10:7,w-180);
            String state=e.state==FolderSetup.State.READY?"Проверена":e.state==FolderSetup.State.CHECKING?"Проверка…":e.state==FolderSetup.State.UNAVAILABLE?"Нет доступа":e.state==FolderSetup.State.UNKNOWN?"Проверить":"Выбрать";
            fit(state,w-133,y+23,16,e.state==FolderSetup.State.READY?11:e.state==FolderSetup.State.UNAVAILABLE?9:6,100);
            fit(e.location.isEmpty()?"Папка ещё не выбрана":e.name,30,y+48,17,6,w-60);
        }
        FolderSetup.Entry e=s.entry(s.selected);
        fit(e.problem.isEmpty()?HINTS[s.selected]:e.problem,16,423,17,e.problem.isEmpty()?6:9,w-32);
        fit(s.notice.isEmpty()?(swap?"B":"A")+" выбрать / подключить снова · X проверить":s.notice,16,451,18,10,w-32);
        fit("В выборе папки откроется окно Android.",16,478,16,13,w-32);
        rect(0,h-44,w,44,0);text((swap?"B":"A")+" папка",12,h-16,18,10);
        text("X проверка",w/3+8,h-16,18,14);text((swap?"A":"B")+" назад",w*2/3+8,h-16,18,6);
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(Math.abs(x-downX)>20||Math.abs(y-downY)>20)return true;
            if(y>=h-44)action(x<w/3?Action.CONFIRM:x<w*2/3?Action.CONTEXT:Action.CANCEL);
            else if(x>=16&&x<=w-16&&y>=124&&y<400){s.select((int)(y-124)/69);changed.run();invalidate();}
            performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
