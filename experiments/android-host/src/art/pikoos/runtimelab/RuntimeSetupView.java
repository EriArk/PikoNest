package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.RuntimeSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Controller-first archive step in the workshop's existing pixel language. */
final class RuntimeSetupView extends View {
    private final RuntimeSetup state;private final boolean swap;private final Paint p=new Paint();
    private Canvas c;private float scale,w,h,downX,downY,buttonY;
    RuntimeSetupView(Context context,RuntimeSetup state,boolean swap){
        super(context);this.state=state;this.swap=swap;
        PixelText.configure(context,p);p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
        setContentDescription("Подключение PICO-8. A: выбрать архив или проверить запуск. X: другой ZIP. Y: перепроверить архив. B: назад или отменить подготовку.");
    }
    void action(Action a){state.act(a);invalidate();}
    private void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    private void text(String value,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(value,x,y,p);}
    private void fit(String value,float x,float y,int size,int color,float width){p.setTextSize(PixelText.size(size));while(value.length()>1&&p.measureText(value)>width)value=value.substring(0,value.length()-2)+"…";text(value,x,y,size,color);}
    private float wrap(String value,float x,float y,int size,int color,float width){
        String line="";p.setTextSize(PixelText.size(size));
        for(String word:value.split(" ")){String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&p.measureText(next)>width){fit(line,x,y,size,color,width);y+=size+4;line=word;}else line=next;}
        if(!line.isEmpty()){fit(line,x,y,size,color,width);y+=size+4;}return y;
    }
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.min(Math.max(1,Math.round(getResources().getDisplayMetrics().density)),Math.min(getWidth()/360f,getHeight()/620f));
        w=getWidth()/scale;h=getHeight()/scale;c.save();c.scale(scale,scale);
        rect(0,0,w,h,1);rect(0,0,w,42,2);text("PIKOOS",16,30,28,7);text("подключение",w-139,29,18,14);
        float left=Math.max(16,(w-590)/2),width=Math.min(w-32,590);
        text("Твой PICO-8",left,85,32,7);
        float step=width/3;
        int stage=state.testing||state.returned?2:state.busy&&state.verified?1:0;
        for(int i=0;i<3;i++){rect(left+i*step,104,step-6,4,i==stage?10:13);fit(new String[]{"1 Архив","2 Подготовка","3 Проба"}[i],left+i*step,130,16,i==stage?10:13,step-12);}
        float y=wrap("Скачай ZIP для Raspberry Pi со своей страницы покупки PICO-8.",left,164,20,7,width);
        y=wrap("Например: pico-8_0.2.7_raspi.zip",left,y+3,17,14,width);
        y=wrap("Распаковывать вручную не нужно.",left,y+2,17,6,width);
        float panel=y+12;rect(left,panel,width,111,0);
        // A small archive cartridge, using exactly the PICO-8 palette.
        rect(left+14,panel+18,47,69,2);rect(left+20,panel+25,35,30,14);
        rect(left+29,panel+31,6,17,7);rect(left+41,panel+31,6,17,7);rect(left+24,panel+67,27,6,5);
        String caption=state.busy?state.phase:state.hasArchive?(state.verified?"Архив проверен":"Копия сохранена"):"Выбери свой архив";
        fit(caption,left+78,panel+31,22,state.busy?10:state.verified?11:7,width-93);
        fit(state.hasArchive?state.filename:"PICO-8 · Raspberry Pi ZIP",left+78,panel+59,17,6,width-93);
        fit(state.hasArchive?String.format(java.util.Locale.ROOT,"%.1f МиБ · %s",state.archiveBytes/1048576.0,state.verified?"ARM64":"нужна проверка"):"Архив из твоей покупки",left+78,panel+84,16,13,width-93);
        y=panel+137;
        String message=state.problem.isEmpty()?(state.busy?"Можно отменить подготовку кнопкой "+(swap?"A":"B")+".":state.testing?"Проба ещё открыта. Заверши её через меню PICO-8 и вернись сюда.":state.returned?"Проба закрыта. Изображение и кнопки проверяются на тестовом экране.":state.verified?"Запустим PICO-8 из этого архива отдельно. Рабочая установка сохранится.":"Проверим целостность ZIP и наличие версии ARM64."):state.problem;
        y=wrap(message,left,y,19,state.problem.isEmpty()?6:9,width);
        buttonY=Math.max(y+10,h-147);
        rect(left,buttonY,width,49,state.busy?5:2);rect(left,buttonY,4,49,state.busy?13:10);
        fit(state.busy?"Готовим…":(swap?"B":"A")+(state.testing?"  Вернуться в пробу":state.verified?"  Проверить запуск":"  Выбрать ZIP"),left+16,buttonY+32,23,state.busy?6:10,width-30);
        fit(state.notice.isEmpty()?(state.adapterPresent?"Оболочка найдена · её запуск проверяется отдельно":"Для запуска ещё понадобится подготовка среды"):state.notice,left,buttonY+75,16,13,width);
        rect(0,h-45,w,45,0);text((swap?"A":"B")+(state.busy?" отменить":" назад"),left,h-16,18,6);
        if(!state.busy&&!state.testing)text("X другой ZIP",left+width-145,h-16,18,14);
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        float x=event.getX()/scale,y=event.getY()/scale;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){
            if(Math.abs(x-downX)<20&&Math.abs(y-downY)<20){
                if(y>=h-45)action(x<w/2?Action.CANCEL:Action.CONTEXT);
                else if(y>=buttonY&&y<=buttonY+49)action(Action.CONFIRM);
            }performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
