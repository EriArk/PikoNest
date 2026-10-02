package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.os.SystemClock;
import art.pikoos.lab.core.WorkshopSession;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.SpriteRegion;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import java.util.ArrayList;

/** Native pixel surface; all mutations go through the portable session. */
final class WorkshopView extends View {
    static final int[] COLORS = {0xff000000,0xff1d2b53,0xff7e2553,0xff008751,
        0xffab5236,0xff5f574f,0xffc2c3c7,0xfffff1e8,0xffff004d,0xffffa300,
        0xffffec27,0xff00e436,0xff29adff,0xff83769c,0xffff77a8,0xffffccaa};
    private final WorkshopSession s;
    private final Runnable changed;
    private final String projectTitle;
    private final Paint p = new Paint();
    private final Bitmap sceneBitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
    private final ArrayList<Hit> hits = new ArrayList<>();
    private Canvas c;
    private float scale, w, h, bodyBottom, downX, downY;
    private int codeTop, codeCount;
    private String previousNotice="Сохранено";
    private long noticeUntil;
    private RectF spriteArea;
    private int viewX,viewY,viewWidth,viewHeight;
    private static final String[] DRAW_NAMES={"Кисть","Ластик","Заливка","Линия","Пипетка"};
    private static final String[] DRAW_HELP={"Один пиксель выбранным цветом","Убрать пиксель из рисунка","Закрасить связанную область","Выбрать начало и конец линии","Взять цвет из рисунка"};
    private static final class Hit {
        final RectF rect; final Runnable action;
        Hit(float x,float y,float w,float h,Runnable action) { rect=new RectF(x,y,x+w,y+h); this.action=action; }
    }
    WorkshopView(Context context, WorkshopSession session, String projectTitle, Runnable changed) {
        super(context); s=session; this.changed=changed;this.projectTitle=projectTitle;
        p.setTypeface(Typeface.createFromAsset(context.getAssets(),"Tiny5-Regular.ttf"));
        p.setAntiAlias(false); p.setFilterBitmap(false);
        setFocusable(true); setFocusableInTouchMode(true);
        setContentDescription("Мастерская PIKOOS. Крестовина: выбор, A: подтвердить, B: назад, L/R: инструмент, Start: тест.");
    }
    void action(Action a) { s.act(a); changed.run(); invalidate(); }
    private void hit(float x,float y,float width,float height,Runnable action) { hits.add(new Hit(x,y,width,height,action)); }
    private void rect(float x,float y,float width,float height,int color) { p.setColor(COLORS[color]); p.setStyle(Paint.Style.FILL); c.drawRect(x,y,x+width,y+height,p); }
    private void outline(float x,float y,float width,float height,int color) {
        rect(x,y,width,2,color);rect(x,y+height-2,width,2,color);rect(x,y,2,height,color);rect(x+width-2,y,2,height,color);
    }
    private void text(String value,float x,float y,int size,int color) { p.setColor(COLORS[color]);p.setTextSize(size);c.drawText(value,x,y,p); }
    private float width(String value,int size) { p.setTextSize(size);return p.measureText(value); }
    private String ok(){return s.swapAB?"B":"A";}
    private String back(){return s.swapAB?"A":"B";}
    private void fitted(String value,float x,float y,int size,int color,float max) {
        p.setTextSize(size);
        while(value.length()>1 && p.measureText(value)>max) value=value.substring(0,value.length()-2)+"…";
        text(value,x,y,size,color);
    }
    private void button(String label,float x,float y,float bw,float bh,boolean focus,Runnable run) {
        rect(x,y,bw,bh,focus?10:0); fitted(label,x+10,y+bh/2+7,20,focus?1:7,bw-20); hit(x,y,bw,bh,run);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); c=canvas;
        scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        if(getWidth()/scale<360) scale=getWidth()/360f;
        if(getHeight()/scale<480) scale=getHeight()/480f;
        w=getWidth()/scale;h=getHeight()/scale;bodyBottom=h-44;
        c.save();c.scale(scale,scale);hits.clear();spriteArea=null;
        rect(0,0,w,h,1); rect(0,0,w,42,2);rect(0,42,w,4,0);
        text("PIKOOS",16,30,28,7);
        if(w>550) text("маленькая мастерская",width("PIKOOS",28)+28,28,16,14);
        boolean draft=s.mode==Mode.VALUE||s.pendingLine();
        String status=s.mode==Mode.ERROR?"Ошибка":draft?"Правка":"Сохранено";
        fitted(status,Math.max(150,w-158),27,16,draft?10:7,108);
        rect(w-174,19,6,6,draft?10:11);
        text("≡",w-34,29,26,7);hit(w-48,0,48,44,()->action(Action.MENU));
        fitted(projectTitle,16,82,28,7,w-130);text("game.p8",w-98,80,18,13);
        rect(0,100,w,2,13);
        String[] tabs={"Мастерская","[ ] Код","Спрайты"};
        for(int i=0;i<3;i++) { final int tab=i;float x=12+i*(w-24)/3,tw=(w-24)/3;
            rect(x,100,tw-3,40,s.tool==i?14:1);
            float tx=x+(tw-width(tabs[i],20))/2;text(tabs[i],tx,128,20,s.tool==i?1:7);
            hit(x,100,tw,40,()->{s.switchTool(tab);changed.run();invalidate();});
        }
        rect(0,140,w,4,0);
        c.save();c.clipRect(0,144,w,bodyBottom);
        if(s.mode==Mode.REGION) regionChooser();else if(s.tool==0) workshop(); else if(s.tool==1) code(); else if(s.browsingSprites) spriteSheet(); else sprites();
        c.restore();
        footer();
        if(!previousNotice.equals(s.notice)){previousNotice=s.notice;noticeUntil=SystemClock.uptimeMillis()+2200;}
        if(s.mode==Mode.NAVIGATE||s.mode==Mode.CANVAS||s.mode==Mode.SHEET){
            long remaining=noticeUntil-SystemClock.uptimeMillis();
            if(remaining>0&&!s.notice.equals("Сохранено")){
                float tw=Math.min(w-32,width(s.notice,18)+28),tx=(w-tw)/2;
                rect(tx,bodyBottom-42,tw,34,0);outline(tx,bodyBottom-42,tw,34,11);
                fitted(s.notice,tx+14,bodyBottom-18,18,7,tw-28);postInvalidateDelayed(remaining);
            }
        }
        if(s.mode==Mode.HELP || s.mode==Mode.MENU || s.mode==Mode.ERROR || s.mode==Mode.DRAW_TOOLS) dialog();
        c.restore();
    }
    private void footer() {
        rect(0,bodyBottom,w,44,0);
        String confirm=s.swapAB?"B":"A",cancel=s.swapAB?"A":"B";
        String[] drawVerbs={"пиксель","стереть","залить",s.pendingLine()?"линия":"начало","цвет"};
        String verb=s.mode==Mode.REGION?(s.choosingEnd?"рисовать":"угол"):s.mode==Mode.VALUE?"готово":s.mode==Mode.CANVAS?drawVerbs[s.drawTool.ordinal()]:s.mode==Mode.PALETTE?"цвет":s.mode==Mode.SHEET&&s.sheetFocus<8?"рисовать":"выбор";
        key(confirm+" "+verb,12,bodyBottom,10,()->action(Action.CONFIRM));
        key(cancel+(s.pendingLine()?" отмена":" назад"),w<500?142:156,bodyBottom,6,()->action(Action.CANCEL));
        if(w>=540){
            if(s.pendingLine())text("Конец линии",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.VALUE)text("← → число",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.PALETTE)text("↑ ↓ ← → цвет",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.REGION)text("↑ ↓ ← → рамка",260,bodyBottom+28,18,6);
            else key("L/R инструм.",260,bodyBottom,6,()->action(Action.NEXT));
        }
        if(s.mode!=Mode.REGION)key("START тест",w-126,bodyBottom,14,()->action(Action.TEST));
    }
    private void key(String label,float x,float y,int color,Runnable run) {
        text(label,x,y+28,18,color);hit(x,y,width(label,18)+8,44,run);
    }
    private void workshop() {
        boolean narrow=w<500;
        float size=narrow?128:Math.min(256,Math.min((w-56)*.46f,bodyBottom-220));
        size=Math.max(96,((int)size/16)*16);
        float x=16,y=180;
        text("ТВОЯ ИГРА",16,169,18,14);
        rect(x-4,y-4,size+8,size+8,0);scene(x,y,size);
        text("Эскиз сцены",16,y+size+24,16,6);
        float right=narrow?160:size+32,rw=w-right-16;
        text("ГЕРОЙ / ДВИЖЕНИЕ",right,169,18,14);
        for(int i=0;i<2;i++) { final int field=i;float fy=180+i*48;
            boolean selected=s.mode==Mode.VALUE?s.field==i:s.focus==i;
            rect(right,fy,rw,44,selected?10:1);rect(right,fy+44,rw,2,selected?10:13);
            text(i==0?"Скорость":"Прыжок",right+10,fy+30,22,selected?1:7);
            text(""+s.displayedValue(i),w-38,fy+30,26,selected?1:7);
            hit(right,fy,rw,46,()->{s.select(field);changed.run();invalidate();});
        }
        float infoY=292;
        if(s.mode==Mode.VALUE) {
            rect(right,infoY,rw,82,0);outline(right,infoY,rw,82,10);
            button("−",right+4,infoY+4,44,44,false,()->action(Action.LEFT));
            text(""+s.draft,right+rw/2-6,infoY+36,30,10);
            button("+",right+rw-48,infoY+4,44,44,false,()->action(Action.RIGHT));
            fitted("← → изменить · "+(s.swapAB?"A":"B")+" отмена",right+8,infoY+68,16,6,rw-16);
        } else {
            link("Рисунок героя  "+(s.cart().heroSlot()+1),2,right,infoY-8,rw);
            link("[ ] Посмотреть код",3,right,infoY+40,rw);
            link("? Как это работает",4,right,infoY+88,rw);
        }
    }
    private void link(String title,int target,float x,float y,float bw) {
        if(s.focus==target) {rect(x,y,bw,40,2);outline(x,y,bw,40,10);}
        fitted(title,x+8,y+27,20,12,bw-16);
        hit(x,y,bw,40,()->{s.select(target);changed.run();invalidate();});
    }
    private void scene(float x,float y,float size) {
        Canvas target=c;c=new Canvas(sceneBitmap);
        rect(0,0,128,128,1);
        for(int i=0;i<30;i++)rect((i*37+13)%128,(i*17+3)%60,1,1,i%3==0?7:13);
        p.setColor(COLORS[15]);c.drawCircle(104,19,8,p);p.setColor(COLORS[1]);c.drawCircle(108,16,8,p);
        for(int i=0;i<7;i++){int mx=i*24-12,my=48+(i%3)*8;rect(mx,my,25,97-my,13);rect(mx+8,my+14,3,78-my,1);}
        rect(0,89,128,20,2);
        for(int i=0;i<8;i++){rect(i*16+3,90,7,4,3);rect(i*16+6,86,5,2,14);}
        int[][] platforms={{0,101,128},{9,73,26},{57,62,26},{96,81,24}};
        for(int[] a:platforms){rect(a[0],a[1],a[2],9,2);rect(a[0],a[1],a[2],3,3);rect(a[0]+2,a[1],a[2]-4,1,11);for(int bx=a[0];bx<a[0]+a[2];bx+=8)rect(bx,a[1]+4,3,2,4);}
        rect(109,85,10,16,4);rect(111,87,6,14,2);rect(112,88,4,2,9);
        rect(68,48,1,7,10);rect(65,51,7,1,10);
        sprite(s.cart().heroSlot(),46,85,16,false);c=target;
        c.drawBitmap(sceneBitmap,null,new RectF(x,y,x+size,y+size),p);
    }
    private void sprite(int slot,float x,float y,float size,boolean grid) {
        float cell=size/16;
        WorkshopCartridge picture=grid?s.canvasPreview():s.cart();
        for(int py=0;py<16;py++)for(int px=0;px<16;px++) {
            int color=picture.pixel(slot,px,py);
            if(grid || color!=0) rect(x+px*cell,y+py*cell,cell,cell,color==0&&grid?((px+py)%2==0?0:1):color);
        }
    }
    private void code() {
        text("LUA / game.p8",16,169,18,14);
        text("↑ ↓ строка   ← → страница",w-272,169,16,13);
        String[] lines=s.cart().code().split("\n",-1);
        float bottom=bodyBottom-76;
        codeCount=Math.max(1,(int)((bottom-180)/24));
        codeTop=Math.max(0,Math.min(s.codeLine-codeCount/2,lines.length-codeCount));
        c.save();c.clipRect(0,178,w,bottom);
        for(int row=0;row<codeCount;row++) {
            final int line=codeTop+row;if(line>=lines.length)break;
            float y=180+row*24;
            if(line==s.codeLine)rect(0,y,w,24,2);
            text(""+(line+1),16,y+19,16,13);
            String source=lines[line].replace("\r","");
            syntax(source,54,y+19);
            hit(0,y,w,24,()->{s.selectCodeLine(line);changed.run();invalidate();});
        }
        c.restore();rect(0,bottom,w,3,14);rect(0,bottom+3,w,73,0);
        int field=s.mode==Mode.VALUE?s.field:(s.codeLine==s.cart().line(1)?1:0);
        if(s.mode==Mode.VALUE) {
            text((field==0?"speed":"jump")+" = "+s.draft,16,bottom+32,24,10);
            text(ok()+" сохранить · "+back()+" отменить",16,bottom+58,16,6);
            button("−",w-120,bottom+14,44,44,false,()->action(Action.LEFT));
            button("+",w-64,bottom+14,44,44,false,()->action(Action.RIGHT));
        } else {
            text("Настоящий Lua из картриджа",16,bottom+30,20,7);
            fitted(ok()+": изменить speed / jump · X: объяснение",16,bottom+57,16,6,w-32);
        }
    }
    private void syntax(String source,float x,float y) {
        if(source.trim().startsWith("--")) {text(source,x,y,20,13);return;}
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*|[0-9]+(?:\\.[0-9]+)?|[^a-zA-Z_0-9]+").matcher(source);
        while(m.find()) {String token=m.group();int color=7;
            if(token.matches("[0-9].*"))color=9;
            if((" function end if then else local for in do and or true false ").contains(" "+token+" "))color=14;
            if((" btn btnp spr cls rectfill circfill line pset all min mid ").contains(" "+token+" "))color=12;
            text(token,x,y,20,color);x+=width(token,20);
        }
    }
    private void sprites() {
        float size=Math.min(256,Math.min((w-56)*.46f,bodyBottom-232));size=Math.max(128,((int)size/16)*16);
        float x=16,y=180,right=size+32,rw=w-right-16;
        SpriteRegion region=s.selection();
        text((s.region==null?"РИСУНОК "+(s.spriteSlot+1):"ОБЛАСТЬ")+" / "+region.width+" × "+region.height,16,169,18,14);
        viewWidth=s.zoom?Math.min(16,region.width):region.width;viewHeight=s.zoom?Math.min(16,region.height):region.height;
        viewX=s.zoom?Math.max(0,Math.min(region.width-viewWidth,s.cursorX-viewWidth/2)):0;
        viewY=s.zoom?Math.max(0,Math.min(region.height-viewHeight,s.cursorY-viewHeight/2)):0;
        float cell=Math.max(1,(int)(size/Math.max(viewWidth,viewHeight)));
        float sw=cell*viewWidth,sh=cell*viewHeight;
        x+=(size-sw)/2;y+=(size-sh)/2;
        outline(x-4,y-4,sw+8,sh+8,s.focus==0||s.mode==Mode.CANVAS?10:0);
        WorkshopCartridge picture=s.canvasPreview();
        for(int py=0;py<viewHeight;py++)for(int px=0;px<viewWidth;px++){
            int color=picture.pixel(region,viewX+px,viewY+py);
            rect(x+px*cell,y+py*cell,cell,cell,color==0?((px+viewX+py+viewY)%2==0?0:1):color);
        }
        spriteArea=new RectF(x,y,x+sw,y+sh);
        if(s.pendingLine()){
            if(s.lineX>=viewX&&s.lineX<viewX+viewWidth&&s.lineY>=viewY&&s.lineY<viewY+viewHeight)
                outline(x+(s.lineX-viewX)*cell,y+(s.lineY-viewY)*cell,cell,cell,10);
        }
        if(s.mode==Mode.CANVAS) {
            float cx=x+(s.cursorX-viewX)*cell,cy=y+(s.cursorY-viewY)*cell;
            outline(cx-1,cy-1,cell+2,cell+2,0);outline(cx,cy,cell,cell,7);
        }
        fitted(s.pendingLine()?"Линия: "+ok()+" готово · "+back()+" отмена":s.mode==Mode.CANVAS?DRAW_NAMES[s.drawTool.ordinal()]+" · "+s.cursorX+", "+s.cursorY:ok()+": войти в рисунок",16,180+size+24,16,s.pendingLine()?10:6,size);
        if(180+size+48<bodyBottom)fitted(s.zoom?"Крупно · окно следует за курсором":s.pendingLine()?"Начало: "+s.lineX+", "+s.lineY:"X цвет · Y отмена",16,180+size+48,16,6,size);
        text("ИНСТРУМЕНТЫ",right,169,18,14);
        button(DRAW_NAMES[s.drawTool.ordinal()]+"  >",right,180,rw,44,s.mode==Mode.NAVIGATE&&s.focus==1,()->action(Action.DRAW_TOOLS));
        boolean paletteActive=s.mode==Mode.PALETTE;
        float step=Math.min(34,(bodyBottom-340)/4);
        if(s.mode==Mode.NAVIGATE&&s.focus==2)outline(right-2,232,rw+4,step*4+6,10);
        float cw=rw/4;
        for(int i=0;i<16;i++) {final int color=i;float cx=right+(i%4)*cw,cy=236+(i/4)*step;
            rect(cx+2,cy+2,cw-4,step-4,i);
            if(i==s.color) {outline(cx+6,cy+6,cw-12,step-12,7);text("+",cx+cw/2-5,cy+step/2+7,20,i==7?0:7);}
            if(paletteActive&&i==s.paletteCursor)outline(cx,cy,cw,step,10);
            hit(cx,cy,cw,step,()->{if(s.mode==Mode.NAVIGATE||s.mode==Mode.CANVAS)s.act(Action.CONTEXT);s.chooseColor(color);changed.run();invalidate();});
        }
        float by=246+step*4;
        button("Лист",right,by,(rw-8)/2,44,s.mode==Mode.NAVIGATE&&s.focus==3,()->action(Action.SPRITE_SHEET));
        button(s.region==null?"Герою":"Рамка",right+(rw+8)/2,by,(rw-8)/2,44,s.mode==Mode.NAVIGATE&&s.focus==4,()->action(s.region==null?Action.ASSIGN_HERO:Action.REGION));
        button(s.zoom?"Весь рисунок":"Приблизить",right,by+52,rw,40,s.mode==Mode.NAVIGATE&&s.focus==5,()->action(Action.ZOOM));
    }
    private void spriteSheet() {
        text("РИСУНКИ ПРОЕКТА",16,169,18,14);
        if(w>=500)text("16 × 16 · X копия",w-186,169,16,6);
        float gap=10,cw=(w-32-gap*3)/4;
        float ch=Math.min(112,(bodyBottom-180-104-gap)/2);
        for(int i=0;i<8;i++) {
            final int slot=i;float x=16+(i%4)*(cw+gap),y=180+(i/4)*(ch+gap);
            boolean chosen=s.sheetFocus==i,used=s.cart().heroSlot()==i;
            rect(x,y,cw,ch,0);outline(x,y,cw,ch,chosen?10:used?14:13);
            float size=Math.max(32,((int)(ch-38)/16)*16);
            if(s.cart().empty(i))text("+",x+cw/2-8,y+ch/2+1,28,13);
            else sprite(i,x+(cw-size)/2,y+8,size,false);
            if(used){rect(x+cw-14,y+6,8,8,14);}
            String label=used?"Герой":s.cart().empty(i)?"Пусто":"Рисунок "+(i+1);
            fitted(label,x+8,y+ch-10,16,chosen?10:used?14:6,cw-16);
            hit(x,y,cw,ch,()->{s.selectSheet(slot);changed.run();invalidate();});
        }
        float by=180+2*(ch+gap)+2,bw=(w-48)/3;
        String[] labels={"+ Новый","Копировать","Назначить герою"};
        for(int i=0;i<3;i++){final int choice=8+i;
            button(labels[i],16+i*(bw+8),by,bw,44,s.sheetFocus==choice,()->{s.selectSheet(choice);changed.run();invalidate();});
        }
        button("Область листа · другой размер",16,by+48,w-32,36,s.sheetFocus==11,()->{s.selectSheet(11);changed.run();invalidate();});
    }
    private void regionChooser(){
        text("ОБЛАСТЬ ЛИСТА",16,169,18,14);
        fitted(s.choosingEnd?"2 / Выбери противоположный угол":"1 / Выбери первый угол",16,196,20,7,w-32);
        float cell=Math.max(1,(int)Math.min((w-32)/128,(bodyBottom-292)/64));
        float x=(w-cell*128)/2,y=214;
        for(int py=0;py<64;py++)for(int px=0;px<128;px++){
            int color=s.cart().sheetPixel(px,py);
            rect(x+px*cell,y+py*cell,cell,cell,color==0?((px/8+py/8)%2==0?0:1):color);
        }
        outline(x-2,y-2,128*cell+4,64*cell+4,13);
        SpriteRegion r=s.regionDraft();
        outline(x+r.x*cell,y+r.y*cell,r.width*cell,r.height*cell,10);
        outline(x+s.regionX*8*cell,y+s.regionY*8*cell,8*cell,8*cell,7);
        if(s.choosingEnd)rect(x+s.anchorX*8*cell+2,y+s.anchorY*8*cell+2,4,4,14);
        for(int ty=0;ty<8;ty++)for(int tx=0;tx<16;tx++){
            final int px=tx,py=ty;hit(x+tx*8*cell,y+ty*8*cell,8*cell,8*cell,()->{s.pointRegion(px,py);changed.run();invalidate();});
        }
        float bottom=y+64*cell;
        text(r.width+" × "+r.height+" пикселей",16,bottom+29,24,10);
        fitted("Рамка выбирает пиксели, не растягивает их.",16,bottom+54,16,6,w-32);
        fitted("Нижние 64 строки связаны с картой — редактор позже.",16,bottom+77,16,13,w-32);
    }
    private void dialog() {
        hits.clear(); // Modal controls trap touch as well as controller focus.
        p.setColor(0xcc000000);c.drawRect(0,0,w,h,p);
        float dw=Math.min(480,w-32),dx=(w-dw)/2,dh=s.mode==Mode.DRAW_TOOLS?392:s.mode==Mode.MENU?380:280,dy=Math.max(16,(h-dh)/2);
        rect(dx+6,dy+6,dw,dh,0);rect(dx,dy,dw,dh,1);outline(dx,dy,dw,dh,s.mode==Mode.ERROR?8:14);
        if(s.mode==Mode.DRAW_TOOLS){
            text("Чем рисуем?",dx+20,dy+36,26,14);
            for(int i=0;i<DRAW_NAMES.length;i++){final int item=i;
                button(DRAW_NAMES[i],dx+16,dy+54+i*50,dw-32,44,s.drawToolCursor==i,()->{s.chooseDrawTool(item);changed.run();invalidate();});
            }
            fitted(DRAW_HELP[s.drawToolCursor],dx+20,dy+337,18,7,dw-40);
            text(ok()+" выбрать · "+back()+" назад",dx+20,dy+374,18,6);
            hit(dx+12,dy+346,(dw-32)/2,44,()->action(Action.CONFIRM));
            hit(dx+20+(dw-32)/2,dy+346,(dw-32)/2,44,()->action(Action.CANCEL));
        }else if(s.mode==Mode.MENU) {
            text("Мастерская",dx+20,dy+36,26,14);
            String[] labels={"Отменить последнюю правку",s.swapAB?"B выбор / A назад":"A выбор / B назад","Как это работает","Вернуться к проекту","Мои игры"};
            for(int i=0;i<5;i++){final int n=i;button(labels[i],dx+16,dy+54+i*54,dw-32,46,s.menuItem==i,()->{s.menuItem=n;action(Action.CONFIRM);});}
            text("SELECT: меню · L/R: инструменты",dx+20,dy+356,16,6);
        } else if(s.mode==Mode.HELP) {
            if(s.tool==2){
                text("Рисунки и герой",dx+20,dy+40,26,14);
                text("Рисунок можно открыть и изменить.",dx+20,dy+80,20,7);
                text("Метка «Герой» показывает, какой",dx+20,dy+108,20,7);
                text("из них сейчас используется в игре.",dx+20,dy+136,20,7);
                text("Копия сохраняет исходный рисунок.",dx+20,dy+177,18,6);
                button(ok()+" К рисункам",dx+16,dy+210,(dw-44)/2,48,true,()->action(Action.CONFIRM));
                button(back()+" Назад",dx+28+(dw-44)/2,dy+210,(dw-44)/2,48,false,()->action(Action.CANCEL));
                return;
            }
            text(s.field==0?"Что делает speed?":"Что делает jump?",dx+20,dy+40,26,14);
            text("Переменная хранит число.",dx+20,dy+80,20,7);
            text(s.field==0?"Больше speed — быстрее шаг.":"Больше jump — выше прыжок.",dx+20,dy+108,20,7);
            text("Меняешь число — меняется игра.",dx+20,dy+136,20,7);
            rect(dx+16,dy+155,dw-32,36,0);text((s.field==0?"speed=":"jump=")+s.cart().value(s.field),dx+28,dy+181,24,10);
            button(ok()+" К коду",dx+16,dy+210,(dw-44)/2,48,true,()->action(Action.CONFIRM));
            button(back()+" Назад",dx+28+(dw-44)/2,dy+210,(dw-44)/2,48,false,()->action(Action.CANCEL));
        } else {
            text("Не получилось",dx+20,dy+40,26,9);
            text("Проект остаётся у тебя.",dx+20,dy+80,20,7);
            String message=s.error;
            for(int row=0;row<3 && !message.isEmpty();row++) {
                int count=message.length();while(count>1&&width(message.substring(0,count),18)>dw-40)count--;
                text(message.substring(0,count),dx+20,dy+116+row*24,18,6);message=message.substring(count);
            }
            button(ok()+" Вернуться",dx+16,dy+210,dw-32,48,true,()->action(Action.CONFIRM));
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x=event.getX()/scale,y=event.getY()/scale;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP) {
            if(s.tool==1&&s.mode==Mode.NAVIGATE&&Math.abs(y-downY)>24) {
                int steps=Math.round((downY-y)/24);for(int i=0;i<Math.abs(steps);i++)s.act(steps>0?Action.DOWN:Action.UP);
                changed.run();invalidate();return true;
            }
            if(Math.abs(x-downX)>20||Math.abs(y-downY)>20)return true;
            if(spriteArea!=null && spriteArea.contains(x,y) && (s.mode==Mode.NAVIGATE||s.mode==Mode.CANVAS)) {
                s.paintAt(viewX+(int)((x-spriteArea.left)*viewWidth/spriteArea.width()),viewY+(int)((y-spriteArea.top)*viewHeight/spriteArea.height()));
                changed.run();invalidate();return true;
            }
            for(int i=hits.size()-1;i>=0;i--) if(hits.get(i).rect.contains(x,y)){hits.get(i).action.run();break;}
            performClick();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
