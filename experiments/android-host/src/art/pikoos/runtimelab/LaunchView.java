package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.PicoPalette;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Small controller/touch recovery surface, using the workshop palette and type. */
final class LaunchView extends View {
    String message="Открываем картридж…";boolean busy=true,active;
    private final ControllerInput.Sink sink;private final boolean swap;
    private final Paint paint=new Paint();private float scale,w,h;private Canvas c;
    LaunchView(Context context,ControllerInput.Sink sink,boolean swap){
        super(context);this.sink=sink;this.swap=swap;
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(),"Tiny5-Regular.ttf"));paint.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);
    }
    private void box(float x,float y,float width,float height,int color){paint.setColor(PicoPalette.COLORS[color]);c.drawRect(x,y,x+width,y+height,paint);}
    private void text(String value,float x,float y,int size,int color){paint.setColor(PicoPalette.COLORS[color]);paint.setTextSize(size);c.drawText(value,x,y,paint);}
    private void wrapped(String value,float y){
        paint.setTextSize(20);String line="";
        for(String word:value.split(" ")){
            if(paint.measureText(line+word)>w-48&&!line.isEmpty()){text(line,24,y,20,7);y+=27;line="";}
            while(paint.measureText(word)>w-48){int n=1;while(n<word.length()&&paint.measureText(word.substring(0,n+1))<w-48)n++;text(word.substring(0,n),24,y,20,7);y+=27;word=word.substring(n);}
            line+=word+" ";
        }text(line,24,y,20,7);
    }
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.min(getWidth()/400f,getHeight()/520f);w=getWidth()/scale;h=getHeight()/scale;
        c.save();c.scale(scale,scale);box(0,0,w,h,1);box(0,0,w,48,2);text("PIKOOS",20,34,30,7);text("ИГРАТЬ",w-110,31,20,14);
        float x=w/2-35;box(x,80,70,82,2);box(x+8,88,54,42,14);box(x+20,100,7,15,7);box(x+43,100,7,15,7);box(x+16,145,38,7,5);
        text(busy?"Сейчас поиграем":active?"Игра ждёт":"Откроем игру?",24,207,28,10);wrapped(message,246);
        if(!busy){box(16,h-154,w-32,44,2);text((swap?"B":"A")+(active?" Вернуться в PICO-8":" Выбрать файл"),28,h-124,22,7);
            if(!active)text("Start Повторить   X Полка PIKOOS",24,h-86,18,6);}
        box(0,h-54,w,54,0);text((swap?"A":"B")+" Назад в лаунчер",24,h-20,22,7);c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_UP){float y=event.getY()/scale;
            if(y>h-54)sink.send(Action.CANCEL);
            else if(!busy&&y>=h-154&&y<=h-110)sink.send(Action.CONFIRM);
            else if(!busy&&!active&&y>h-110)sink.send(event.getX()/scale<w/2?Action.TEST:Action.CONTEXT);
        }return true;
    }
}
