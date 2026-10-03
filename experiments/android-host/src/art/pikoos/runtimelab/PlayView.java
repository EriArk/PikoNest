package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.PlaySession;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.ArrayList;

/** Controller-first play shelf. Actual cartridge labels or an honest empty cover. */
final class PlayView extends View {
    private final PlaySession s;private final Runnable changed;private final boolean swap;
    private final Paint p=new Paint();private Canvas c;private float scale,w,h,downX,downY;
    private final ArrayList<Hit> hits=new ArrayList<>();
    private static final class Hit {RectF rect;Runnable run;Hit(float x,float y,float w,float h,Runnable r){rect=new RectF(x,y,x+w,y+h);run=r;}}
    PlayView(Context context,PlaySession session,boolean swap,Runnable changed){
        super(context);s=session;this.swap=swap;this.changed=changed;
        p.setTypeface(Typeface.createFromAsset(context.getAssets(),"Tiny5-Regular.ttf"));p.setAntiAlias(false);
        setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Играть. Крестовина: игра. A: запуск. X: избранное. Y: фильтр. R: мастерская. Select: папки. L: обновить.");
    }
    void action(Action a){s.act(a);changed.run();invalidate();}
    private void rect(float x,float y,float bw,float bh,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+bw,y+bh,p);}
    private void text(String t,float x,float y,int size,int color){p.setTextSize(size);p.setColor(WorkshopView.COLORS[color]);c.drawText(t,x,y,p);}
    private void fit(String t,float x,float y,int size,int color,float max){p.setTextSize(size);while(t.length()>1&&p.measureText(t)>max)t=t.substring(0,t.length()-2)+"…";text(t,x,y,size,color);}
    private void button(String t,float x,float y,float bw,int color,Runnable run){rect(x,y,bw,40,color);fit(t,x+10,y+27,20,7,bw-20);hits.add(new Hit(x,y,bw,40,run));}
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.min(Math.max(1,Math.round(getResources().getDisplayMetrics().density)),Math.min(getWidth()/360f,getHeight()/540f));
        w=getWidth()/scale;h=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();rect(0,0,w,h,1);
        rect(0,0,w,44,2);text("PIKOOS",16,31,28,7);if(w>490)text("время поиграть",170,28,16,14);
        button("≡ Папки",w-112,2,108,2,()->action(Action.MENU));
        text("Играть",16,86,32,10);button("R Мастерская",w-188,55,172,2,()->action(Action.NEXT));
        String[] filters={"Все игры","Недавние","Избранное"};
        button("Y "+filters[s.filter],16,106,178,0,()->action(Action.UNDO));
        button("L Обновить",w-162,106,146,0,()->action(Action.PREVIOUS));
        int columns=w>=550?3:2,start=s.selected/columns*columns;
        s.columns=columns;
        float gap=14,cw=(w-32-gap*(columns-1))/columns,top=165,bottom=h-153,ch=bottom-top;
        for(int col=0;col<columns;col++){
            int index=start+col;if(index>=s.games().size())break;final int chosen=index;PlaySession.Game g=s.games().get(index);
            float x=16+col*(cw+gap);boolean selected=index==s.selected;
            rect(x,top,cw,ch,selected?14:13);rect(x+3,top+3,cw-6,ch-6,0);
            float size=Math.min(cw-22,ch-94),cx=x+(cw-size)/2,cy=top+12;
            if(g.cover!=null){float cell=size/128;for(int py=0;py<128;py++)for(int px=0;px<128;px++){p.setColor(g.cover[py*128+px]);c.drawRect(cx+px*cell,cy+py*cell,cx+(px+1)*cell+.01f,cy+(py+1)*cell+.01f,p);}}
            else{
                rect(cx,cy,size,size,1);float u=size/64;
                rect(cx+12*u,cy+8*u,40*u,48*u,2);rect(cx+17*u,cy+13*u,30*u,24*u,14);
                rect(cx+23*u,cy+18*u,5*u,14*u,7);rect(cx+33*u,cy+18*u,5*u,14*u,7);
                rect(cx+20*u,cy+44*u,24*u,6*u,5);
            }
            if(g.favorite){String[] heart={"0110110","1111111","1111111","0111110","0011100","0001000"};for(int hy=0;hy<6;hy++)for(int hx=0;hx<7;hx++)if(heart[hy].charAt(hx)=='1')rect(x+cw-27+hx*3,top+8+hy*3,3,3,10);}
            fit(g.title,x+10,bottom-58,21,selected?10:7,cw-20);
            fit(g.folder.isEmpty()?"Корень папки":g.folder,x+10,bottom-38,15,6,cw-20);
            fit(!g.problem.isEmpty()?"Пока недоступно":g.format.extension+(g.recent>0?" · играли":" · PICO-8"),x+10,bottom-18,15,!g.problem.isEmpty()?9:6,cw-20);
            hits.add(new Hit(x,top,cw,ch,()->{s.select(chosen);changed.run();invalidate();}));
        }
        if(s.games().isEmpty()){
            text(s.busy?"Ищем картриджи…":"Здесь будут твои игры",24,224,25,14);
            fit(s.filter==0?"Выбери папку с играми через Select.":"Y — вернуться к другим играм",24,264,18,6,w-48);
        }
        PlaySession.Game game=s.current();
        String message=!s.error.isEmpty()?s.error:s.busy?"Читаем… можно перейти в мастерскую":game!=null&&!game.problem.isEmpty()?game.problem:s.notice;
        fit(message,16,h-120,18,s.error.isEmpty()?6:9,w-32);
        fit(s.games().isEmpty()?"Готовые игры отдельно от твоих проектов.":(s.selected+1)+" / "+s.games().size()+" · ← → выбрать · Start играть",16,h-90,17,6,w-32);
        rect(0,h-58,w,58,0);
        button((swap?"B":"A")+" Играть",8,h-51,w/2-16,2,()->action(Action.CONFIRM));
        button(game!=null&&game.favorite?"X Из избранного":"X В избранное",w/2+4,h-51,w/2-12,1,()->action(Action.CONTEXT));
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(downY>=165&&downY<h-153&&Math.abs(x-downX)>40){action(x<downX?Action.RIGHT:Action.LEFT);return true;}
            if(Math.abs(x-downX)<20&&Math.abs(y-downY)<20)for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).rect.contains(x,y)){hits.get(i).run.run();break;}
            performClick();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
