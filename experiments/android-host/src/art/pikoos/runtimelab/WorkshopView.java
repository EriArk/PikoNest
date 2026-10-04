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
import art.pikoos.lab.core.MapEditor;
import art.pikoos.lab.core.MapChange;
import art.pikoos.lab.core.SpritePlacement;
import art.pikoos.lab.core.SpriteAnimation;
import art.pikoos.lab.core.SpriteMove;
import art.pikoos.lab.core.SpriteAsset;
import art.pikoos.lab.core.NameEditor;
import art.pikoos.lab.core.LuaDraft;
import art.pikoos.lab.core.LuaInsert;
import art.pikoos.lab.core.LuaCall;
import art.pikoos.lab.core.LuaNavigation;
import art.pikoos.lab.core.RuntimeDiagnostic;
import android.view.KeyEvent;
import android.view.InputDevice;
import art.pikoos.lab.core.HeroBinding;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import java.util.ArrayList;

/** Native pixel surface; all mutations go through the portable session. */
final class WorkshopView extends View {
    static final int[] COLORS = art.pikoos.lab.core.PicoPalette.COLORS;
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
    private RectF placementArea;
    private long animationClock;
    private RectF mapArea;
    private int mapLeft,mapTop;
    private static final String[] MAP_TOOLS={"Кисть","Прямоугольник","Заливка","Флаги тайла"};
    private int viewX,viewY,viewWidth,viewHeight;
    private static final String[] DRAW_NAMES={"Кисть","Ластик","Заливка","Линия","Пипетка","Прямоугольник","Прямоуг. с заливкой","Овал","Овал с заливкой"};
    private static final String[] DRAW_HELP={"Один пиксель выбранным цветом","Убрать пиксель из спрайта","Закрасить связанную область","Выбрать начало и конец линии","Взять цвет из спрайта","Контур: выбери два противоположных угла","Закрашенная фигура по двум углам","Контур овала внутри рамки по двум углам","Закрашенный овал внутри выбранной рамки"};
    private static final String[] TRANSFORM_NAMES={"Зеркало: слева направо","Зеркало: сверху вниз","Поворот на 90° вправо","Разворот на 180°"};
    private static final class Hit {
        final RectF rect; final Runnable action;
        Hit(float x,float y,float w,float h,Runnable action) { rect=new RectF(x,y,x+w,y+h); this.action=action; }
    }
    WorkshopView(Context context, WorkshopSession session, String projectTitle, Runnable changed) {
        super(context); s=session; this.changed=changed;this.projectTitle=projectTitle;
        PixelText.configure(context,p);
        p.setAntiAlias(false); p.setFilterBitmap(false);
        setFocusable(true); setFocusableInTouchMode(true);
        setContentDescription("Мастерская PIKOOS. Крестовина: выбор, A: подтвердить, B: назад, L/R: инструмент, Start: тест.");
    }
    void action(Action a) { s.act(a); changed.run(); invalidate(); }
    boolean codeKey(KeyEvent event){
        if(s.mode!=Mode.CODE||s.codeDraft==null||(event.getSource()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD)return false;
        LuaDraft d=s.codeDraft;
        if(d.panel==LuaDraft.Panel.SPRITE||d.panel==LuaDraft.Panel.ANIMATION)return false;
        if(d.proposal()){
            if(d.insertion.screen!=LuaInsert.Screen.TEXT)return false;
            int k=event.getKeyCode();
            if(k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DEL){
                if(event.getAction()==KeyEvent.ACTION_DOWN)action(k==KeyEvent.KEYCODE_ENTER?Action.TEST:Action.CONTEXT);return true;
            }
            int cp=event.getUnicodeChar();
            if(!event.isCtrlPressed()&&!event.isAltPressed()&&cp>=32&&cp<127){
                if(event.getAction()==KeyEvent.ACTION_DOWN){s.insertText(Character.toString((char)cp));changed.run();invalidate();}return true;
            }
            return false;
        }
        if(d.panel==LuaDraft.Panel.EXIT||d.panel==LuaDraft.Panel.MENU||d.panel==LuaDraft.Panel.NAVIGATION)return false;
        int key=event.getKeyCode(),command=-1;
        if(event.isCtrlPressed()){
            if(key==KeyEvent.KEYCODE_A)command=8;if(key==KeyEvent.KEYCODE_C)command=9;
            if(key==KeyEvent.KEYCODE_X)command=10;if(key==KeyEvent.KEYCODE_V)command=11;
            if(key==KeyEvent.KEYCODE_Z)command=event.isShiftPressed()?13:12;
            if(key==KeyEvent.KEYCODE_Y)command=13;if(key==KeyEvent.KEYCODE_S)command=0;
        }else{
            if(key==KeyEvent.KEYCODE_DEL)command=5;if(key==KeyEvent.KEYCODE_FORWARD_DEL)command=6;
            if(key==KeyEvent.KEYCODE_ENTER)command=2;if(key==KeyEvent.KEYCODE_TAB)command=4;
            if(key==KeyEvent.KEYCODE_MOVE_HOME)command=14;if(key==KeyEvent.KEYCODE_MOVE_END)command=15;
        }
        if(command>=0){if(event.getAction()==KeyEvent.ACTION_DOWN){s.codeCommand(command);changed.run();invalidate();}return true;}
        int codepoint=event.getUnicodeChar();
        if(!event.isCtrlPressed()&&!event.isAltPressed()&&codepoint>=32&&codepoint<127){
            if(event.getAction()==KeyEvent.ACTION_DOWN){s.codeText(Character.toString((char)codepoint));changed.run();invalidate();}return true;
        }
        return false;
    }
    private void hit(float x,float y,float width,float height,Runnable action) { hits.add(new Hit(x,y,width,height,action)); }
    private void rect(float x,float y,float width,float height,int color) { p.setColor(COLORS[color]); p.setStyle(Paint.Style.FILL); c.drawRect(x,y,x+width,y+height,p); }
    private void outline(float x,float y,float width,float height,int color) {
        rect(x,y,width,2,color);rect(x,y+height-2,width,2,color);rect(x,y,2,height,color);rect(x+width-2,y,2,height,color);
    }
    private void text(String value,float x,float y,int size,int color) { p.setColor(COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(value,x,y,p); }
    private float width(String value,int size) { p.setTextSize(PixelText.size(size));return p.measureText(value); }
    private String ok(){return s.swapAB?"B":"A";}
    private String back(){return s.swapAB?"A":"B";}
    private void fitted(String value,float x,float y,int size,int color,float max) {
        p.setTextSize(PixelText.size(size));
        while(value.length()>1 && p.measureText(value)>max) value=value.substring(0,value.length()-2)+"…";
        text(value,x,y,size,color);
    }
    private void button(String label,float x,float y,float bw,float bh,boolean focus,Runnable run) {
        rect(x,y,bw,bh,focus?10:0); fitted(label,x+10,y+bh/2+7,PixelText.actionSize(p,label,bw-20),focus?1:7,bw-20); hit(x,y,bw,bh,run);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); c=canvas;
        scale=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        if(getWidth()/scale<360) scale=getWidth()/360f;
        if(getHeight()/scale<480) scale=getHeight()/480f;
        w=getWidth()/scale;h=getHeight()/scale;bodyBottom=h-44;
        c.save();c.scale(scale,scale);hits.clear();spriteArea=null;placementArea=null;mapArea=null;
        rect(0,0,w,h,1); rect(0,0,w,42,2);rect(0,42,w,4,0);
        text("PIKOOS",16,30,28,7);
        if(w>550) text("маленькая мастерская",width("PIKOOS",28)+28,28,16,14);
        boolean draft=s.flagDraft!=null||s.mapEditor.pending()||s.codeDraft!=null||s.mode==Mode.VALUE||s.mode==Mode.HERO||s.pendingStroke()||s.copying()||s.transforming()||s.recoloring()||s.move!=null||s.assetDraft!=null||s.nameEditor!=null;
        String status=s.mode==Mode.ERROR?"Ошибка":draft?"Правка":"Сохранено";
        fitted(status,Math.max(150,w-158),27,16,draft?10:7,108);
        rect(w-174,19,6,6,draft?10:11);
        if(s.mode!=Mode.NAME){text("≡",w-34,29,26,7);hit(w-48,0,48,44,()->action(Action.MENU));}
        if(s.mode==Mode.NAME){nameEditor();c.restore();return;}
        if(s.mode==Mode.ASSET_CATEGORY){assetCategories();c.restore();return;}
        if(s.mode==Mode.DIAGNOSTIC){diagnostic();c.restore();return;}
        if(s.codeDraft!=null&&(s.mode==Mode.CODE||s.mode==Mode.ERROR)){
            luaEditor();if(s.mode==Mode.ERROR)dialog();c.restore();return;
        }
        fitted(projectTitle,16,82,28,7,w-130);text("game.p8",w-98,80,18,13);
        rect(0,100,w,2,13);
        String[] tabs={"Обзор","Код","Спрайты","Карта"};
        for(int i=0;i<4;i++) { final int tab=i;float x=12+i*(w-24)/4,tw=(w-24)/4;
            rect(x,100,tw-3,40,s.tool==i?14:1);
            String title=w<500&&i==0?"Обзор":tabs[i];
            int ts=w<500?18:20;float tx=x+(tw-width(title,ts))/2;text(title,tx,128,ts,s.tool==i?1:7);
            hit(x,100,tw,40,()->{s.switchTool(tab);changed.run();invalidate();});
        }
        rect(0,140,w,4,0);
        c.save();c.clipRect(0,144,w,bodyBottom);
        if(s.mode==Mode.RECOLOR)recolorChooser();else if(s.mode==Mode.TRANSFORM)transformChooser();else if(s.mode==Mode.MOVE)moveChooser();else if(s.mode==Mode.ASSETS||s.mode==Mode.ASSET_SAVE)assetLibrary();else if(s.copying()&&s.mode!=Mode.ERROR)copyChooser();else if(s.mode==Mode.REGION) regionChooser();else if(s.tool==3)mapEditor();else if(s.tool==0) workshop(); else if(s.tool==1) code(); else if(s.browsingSprites) spriteSheet(); else sprites();
        c.restore();
        footer();
        if(!previousNotice.equals(s.notice)){previousNotice=s.notice;noticeUntil=SystemClock.uptimeMillis()+2200;}
        if(!s.pendingStroke()&&(s.mode==Mode.NAVIGATE||s.mode==Mode.CANVAS||s.mode==Mode.SHEET||s.mode==Mode.ASSETS)){
            long remaining=noticeUntil-SystemClock.uptimeMillis();
            if(remaining>0&&!s.notice.equals("Сохранено")){
                float tw=Math.min(w-32,width(s.notice,18)+28),tx=(w-tw)/2;
                rect(tx,bodyBottom-42,tw,34,0);outline(tx,bodyBottom-42,tw,34,11);
                fitted(s.notice,tx+14,bodyBottom-18,18,7,tw-28);postInvalidateDelayed(remaining);
            }
        }
        if(s.mode==Mode.HELP || s.mode==Mode.MENU || s.mode==Mode.ERROR || s.mode==Mode.DRAW_TOOLS || s.mode==Mode.HERO) dialog();
        c.restore();
    }
    private void footer() {
        rect(0,bodyBottom,w,44,0);
        if(s.tool==3&&s.mode==Mode.NAVIGATE){
            if(s.flagDraft!=null){
                key(ok()+(s.flagDraft.focus==8?" сохранить":" вкл/выкл"),12,bodyBottom,10,()->action(Action.CONFIRM));
                key(back()+" отмена",w-126,bodyBottom,6,()->action(Action.CANCEL));return;
            }
            MapEditor m=s.mapEditor;
            String verb=m.picking||m.choosingTool?" выбор":m.phase==2?" готово":m.phase==1?" обзор":m.tool==MapEditor.Tool.RECTANGLE?" угол":m.tool==MapEditor.Tool.FILL?" обзор":" тайл";
            key(ok()+verb,12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+(m.modal()?" назад":" меню"),w<500?138:190,bodyBottom,6,()->action(Action.CANCEL));
            if(!m.modal())key("START тест",w-126,bodyBottom,14,()->action(Action.TEST));
            else if(m.pending())key("Y отмена",w-106,bodyBottom,14,()->action(Action.UNDO));return;
        }
        if(s.mode==Mode.MOVE){
            key(ok()+(s.move.phase==2?" готово":" угол"),12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" назад",w/3+12,bodyBottom,6,()->action(Action.CANCEL));
            key("Y отмена",w*2/3+12,bodyBottom,14,()->action(Action.UNDO));return;
        }
        if(s.mode==Mode.RECOLOR){
            key(ok()+" применить",12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" отмена",w<500?166:220,bodyBottom,6,()->action(Action.CANCEL));
            key("X из / в",w-110,bodyBottom,14,()->action(Action.CONTEXT));
            return;
        }
        if(s.mode==Mode.TRANSFORM){
            key(ok()+" применить",12,bodyBottom,s.transformAllowed()?10:13,()->action(Action.CONFIRM));
            key(back()+" отмена",w<500?180:228,bodyBottom,6,()->action(Action.CANCEL));
            if(w>=540)text("← → вариант",w-152,bodyBottom+28,18,6);
            return;
        }
        if(s.mode==Mode.ASSETS||s.mode==Mode.ASSET_SAVE){
            if(s.mode==Mode.ASSETS&&s.currentAsset()==null&&s.assetFilter!=0){
                key(back()+" назад",12,bodyBottom,6,()->action(Action.CANCEL));
                if(s.tool==2)key("X сохранить",w-132,bodyBottom,14,()->action(Action.CONTEXT));
                return;
            }
            key(ok()+(s.mode==Mode.ASSET_SAVE||s.currentAsset()==null&&s.tool==2?" сохранить":s.currentAsset()==null?" спрайты":" вставить"),12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" назад",w<500?136:200,bodyBottom,6,()->action(Action.CANCEL));
            if(s.mode==Mode.ASSETS&&s.tool==2)key("X сохранить",w-132,bodyBottom,14,()->action(Action.CONTEXT));
            if(s.mode==Mode.ASSET_SAVE)key("X имя",w-88,bodyBottom,14,()->action(Action.CONTEXT));
            return;
        }
        if(s.copying()&&s.mode!=Mode.ERROR){
            key(ok()+(s.mode==Mode.COPY_CONFIRM?(s.copyAsset!=null?" вставить":" копировать"):" просмотр"),12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" назад",w<500?190:220,bodyBottom,6,()->action(Action.CANCEL));
            if(w>=540&&s.mode==Mode.COPY_PLACE)text("↑ ↓ ← → место",360,bodyBottom+28,18,6);
            return;
        }
        String confirm=s.swapAB?"B":"A",cancel=s.swapAB?"A":"B";
        String corner=s.pendingStroke()?"готово":"угол";
        String[] drawVerbs={"пиксель","стереть","залить",s.pendingStroke()?"линия":"начало","цвет",corner,corner,corner,corner};
        String verb=s.mode==Mode.REGION?(s.choosingEnd?"рисовать":"угол"):s.mode==Mode.VALUE?"готово":s.mode==Mode.CANVAS?drawVerbs[s.drawTool.ordinal()]:s.mode==Mode.PALETTE?"цвет":s.mode==Mode.SHEET&&s.sheetFocus<8?"рисовать":"выбор";
        key(confirm+" "+verb,12,bodyBottom,10,()->action(Action.CONFIRM));
        key(cancel+(s.pendingStroke()?" отмена":" назад"),w<500?142:156,bodyBottom,6,()->action(Action.CANCEL));
        if(w>=540){
            if(s.pendingStroke())text(s.drawTool==DrawTool.LINE?"Конец линии":"Второй угол",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.VALUE)text("← → число",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.PALETTE)text("↑ ↓ ← → цвет",260,bodyBottom+28,18,6);
            else if(s.mode==Mode.REGION)text("↑ ↓ ← → рамка",260,bodyBottom+28,18,6);
            else key("L/R инструм.",260,bodyBottom,6,()->action(Action.NEXT));
        }
        if(s.mode!=Mode.REGION)key("START тест",w-126,bodyBottom,14,()->action(Action.TEST));
    }
    private void key(String label,float x,float y,int color,Runnable run) {
        if(w<500)label=label.replace(" выбрать"," выбор").replace(" сохранить"," сохр.").replace("START готово","START OK");
        text(label,x,y+28,18,color);hit(x,y,width(label,18)+8,44,run);
    }
    private void mapEditor(){
        art.pikoos.lab.core.MapEditor m=s.mapEditor;
        if(s.flagDraft!=null){flagEditor();return;}
        if(m.choosingTool){
            text("ИНСТРУМЕНТ КАРТЫ",16,174,22,14);
            for(int i=0;i<4;i++){final int n=i;button(MAP_TOOLS[i],16,184+i*42,w-32,38,m.toolChoice==i,()->{m.toolChoice=n;action(Action.CONFIRM);});}
            String[] help={"Одна клетка — один тайл.","Выбери два угла.","Соседние одинаковые тайлы.","Для всех клеток с тайлом."};
            fitted(help[m.toolChoice],16,370,18,7,w-32);
            fitted(m.toolChoice==3?"Смысл задаёт Lua игры.":m.toolChoice==0?"Y отмена · R2 вернуть":"Обзор → готово · Y отмена",16,396,18,7,w-32);
            fitted(m.toolChoice==3?"Номера флагов: 0–7.":"Правки — в строках 0–31.",16,422,18,6,w-32);return;
        }
        boolean narrow=w<500;float size=Math.min(narrow?192:320,bodyBottom-230);size=Math.max(128,(int)(size/16)*16);
        float x=16,y=186,right=x+size+16,rw=w-right-16;
        fitted(m.picking?"ВЫБЕРИ ТАЙЛ 8×8":m.pending()?(m.phase==1?"ВТОРОЙ УГОЛ":m.tool==MapEditor.Tool.FILL?"ПРОВЕРЬ ЗАЛИВКУ":"ПРОВЕРЬ ОБЛАСТЬ"):"КАРТА · "+MAP_TOOLS[m.tool.ordinal()],16,169,18,14,w-32);
        art.pikoos.lab.core.P8Map map;
        try{map=s.cart().map();}catch(IllegalArgumentException e){
            fitted("Карта пока не поддержана",16,208,20,9,w-32);
            fitted("Исходник сохранён. Доступен код.",16,242,18,7,w-32);return;
        }
        mapLeft=m.left();mapTop=m.top();
        MapChange proposal=m.preview(s.cart());
        for(int py=0;py<128;py++)for(int px=0;px<128;px++){
            int color;
            if(m.picking){color=s.cart().sheetPixel(px,py);if(px<8&&py<8)color=0;}
            else{int tx=mapLeft+px/8,ty=mapTop+py/8,tile=proposal!=null&&proposal.changes(tx,ty)?proposal.tile:map.tile(tx,ty);color=tile==0?0:s.cart().sheetPixel(tile%16*8+px%8,tile/16*8+py%8);}
            sceneBitmap.setPixel(px,py,COLORS[color==0?1:color]);
        }
        rect(x-4,y-4,size+8,size+8,0);p.setColor(COLORS[7]);c.drawBitmap(sceneBitmap,null,new RectF(x,y,x+size,y+size),p);
        float cell=size/16;
        for(int i=0;i<=16;i++){rect(x+i*cell,y,1,size,0);rect(x,y+i*cell,size,1,0);}
        if(proposal!=null)for(int ty=0;ty<16;ty++)for(int tx=0;tx<16;tx++)if(proposal.changes(mapLeft+tx,mapTop+ty)){
            float bx=x+tx*cell,by=y+ty*cell;
            if(!proposal.changes(mapLeft+tx-1,mapTop+ty))rect(bx,by,2,cell,14);
            if(!proposal.changes(mapLeft+tx+1,mapTop+ty))rect(bx+cell-2,by,2,cell,14);
            if(!proposal.changes(mapLeft+tx,mapTop+ty-1))rect(bx,by,cell,2,14);
            if(!proposal.changes(mapLeft+tx,mapTop+ty+1))rect(bx,by+cell-2,cell,2,14);
        }
        int cx=m.picking?m.choice%16:(m.phase==2?m.peekX:m.x)-mapLeft,cy=m.picking?m.choice/16:(m.phase==2?m.peekY:m.y)-mapTop;
        outline(x+cx*cell,y+cy*cell,cell,cell,10);
        if(!m.picking&&mapTop<=32&&mapTop+16>32)rect(x,y+(32-mapTop)*cell,size,2,9);
        mapArea=new RectF(x,y,x+size,y+size);
        int tile=m.picking?m.choice:m.tile;
        text("Тайл "+tile,right,205,20,10);
        for(int py=0;py<8;py++)for(int px=0;px<8;px++){
            int color=tile==0?0:s.cart().sheetPixel(tile%16*8+px,tile/16*8+py);
            rect(right+px*6,218+py*6,6,6,color==0?0:color);
        }
        text(tile==0?"Пусто":"8×8",right+56,247,18,6);
        text(m.picking?"Из листа":m.pending()?"Клеток":"Клетка",right,291,18,6);
        text(m.picking?"0 = пусто":proposal!=null?""+proposal.count:m.x+", "+m.y,right,315,18,7);
        if(m.pending()){
            button(ok()+(m.phase==1?" обзор":" готово"),right,330,rw,40,true,()->action(Action.CONFIRM));
            button(back()+" назад",right,378,rw,40,false,()->action(Action.CANCEL));
        }else if(!m.picking){
            button("X тайл",right,330,rw,40,false,()->action(Action.CONTEXT));
            button(narrow?"L2 кисти":"L2 инструменты",right,378,rw,40,false,()->action(Action.DRAW_TOOLS));
        }
        String label=m.picking?"Тайл из листа":m.phase==2?"↑↓←→ обзор":m.y>=32?"Только просмотр":"↑↓←→ клетка";
        fitted(label,16,y+size+27,18,m.y>=32&&!m.picking?9:6,size);
        if(!narrow&&bodyBottom>470){
            fitted(m.picking?"0 — пустая клетка":m.pending()?"Верх карты · строки 0–31":"Y отмена · Select меню",right,448,18,6,rw);
            if(!m.picking)text(m.pending()?"Одна правка в истории":"map(0,0,0,0,16,16)",right,474,18,7);
        }
    }
    private void flagEditor(){
        art.pikoos.lab.core.FlagDraft d=s.flagDraft;
        text("ФЛАГИ · ТАЙЛ "+d.tile,16,170,22,14);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)rect(16+x*6,184+y*6,6,6,s.cart().sheetPixel(d.tile%16*8+x,d.tile/16*8+y));
        text("Для всех клеток",80,203,18,7);text("с этим тайлом.",80,227,18,6);
        float cw=(w-32)/4;
        for(int n=0;n<8;n++){
            final int flag=n;float x=16+n%4*cw,y=248+n/4*42;
            button(n+(d.bit(n)?" +":" -"),x,y,cw-4,36,d.focus==n,()->{d.focus=flag;action(Action.CONFIRM);});
            rect(x+2,y+32,cw-8,3,d.bit(n)?11:13);
        }
        String call=d.focus<8?"fget("+d.tile+","+d.focus+") = "+(d.bit(d.focus)?"true":"false"):"fget("+d.tile+") = "+d.value;
        fitted(call,16,350,18,10,w-32);
        fitted("Смысл задаёт Lua игры.",16,374,18,6,w-32);
        button("Сохранить флаги",16,bodyBottom-54,w-32,40,d.focus==8,()->{d.focus=8;action(Action.CONFIRM);});
    }
    private void workshop() {
        if(!s.cart().hasHero()){resources();return;}
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
            SpriteRegion hero=s.cart().hero().image;
            link("Герой · "+hero.width+" × "+hero.height,2,right,infoY-8,rw);
            link("[ ] Посмотреть код",3,right,infoY+40,rw);
            link("? Как это работает",4,right,infoY+88,rw);
        }
    }
    private void resources(){
        text("ТВОЙ КАРТРИДЖ",16,169,18,14);
        fitted("Спрайты для фигур, предметов и окружения",16,204,20,7,w-32);
        float gap=12,size=Math.max(24,Math.min(64,Math.min((w-68)/4,bodyBottom-404))),y=218;
        for(int i=0;i<4;i++){
            float x=16+i*(size+gap);rect(x,y,size,size,0);sprite(i,x,y,size,false);outline(x,y,size,size,13);
        }
        text("Ресурсы проекта · не кадр игры",16,y+size+25,16,6);
        float by=y+size+32;
        link("Спрайты · выбрать и рисовать",0,16,by,w-32);
        link("[ ] Посмотреть код",1,16,by+48,w-32);
        link("? Ресурсы и игра",2,16,by+96,w-32);
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
        HeroBinding hero=s.cart().hero();
        regionSprite(hero.image,hero.spawnX()-hero.left,hero.spawnY()-hero.top,1);c=target;
        c.drawBitmap(sceneBitmap,null,new RectF(x,y,x+size,y+size),p);
    }
    private void regionSprite(SpriteRegion r,float x,float y,float cell){
        for(int py=0;py<r.height;py++)for(int px=0;px<r.width;px++){
            int color=s.cart().pixel(r,px,py);if(color!=0)rect(x+px*cell,y+py*cell,cell,cell,color);
        }
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
        fitted("LUA / game.p8",16,169,18,14,w*.42f-24);
        fitted("↑↓ строка · ←→ страница",w*.42f,169,16,13,w*.58f-16);
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
            fitted(ok()+": редактировать Lua · L/R инструменты",16,bottom+57,16,6,w-32);
        }
    }
    private void luaEditor(){
        LuaDraft d=s.codeDraft;
        if(d.panel==LuaDraft.Panel.ANIMATION){animationEditor();return;}
        if(d.panel==LuaDraft.Panel.SPRITE){spritePlacement();return;}
        if(d.proposal()){luaInsertion();return;}
        if(d.panel==LuaDraft.Panel.NAVIGATION){luaNavigation();return;}
        fitted("[ ] Код / "+projectTitle,16,75,24,7,w-32);
        boolean keys=d.panel==LuaDraft.Panel.KEYS,menu=d.panel==LuaDraft.Panel.MENU,exit=d.panel==LuaDraft.Panel.EXIT;
        String position=(d.line()+1)+":"+(d.column()+1)+(d.anchor()>=0?" · выделение":"");
        text(position,16,99,16,10);
        fitted(keys?"Набор текста":menu?"Правки":exit?"Закрыть черновик?":w<500?"Курсор ↑↓←→":"Крестовина: курсор",w/2,99,16,14,w/2-16);
        float bottom=keys?bodyBottom-240:bodyBottom-106;
        float advance=(float)Math.ceil(width("M",20));
        int rows=Math.max(2,(int)((bottom-112)/24)),columns=Math.max(12,(int)((w-70)/advance));
        int top=Math.max(0,Math.min(d.line()-rows/2,d.lineCount()-rows));
        int left=Math.max(0,d.column()-columns+4);
        rect(12,108,w-24,bottom-108,0);
        c.save();c.clipRect(12,108,w-12,bottom);
        for(int row=0;row<rows&&top+row<d.lineCount();row++){
            final int line=top+row;float y=112+row*24;
            if(line==d.line())rect(12,y,w-24,24,2);
            text(""+(line+1),18,y+18,16,13);
            String source=d.lineText(line);int count=source.codePointCount(0,source.length());
            for(int col=left;col<Math.min(count,columns+left);col++){
                int from=source.offsetByCodePoints(0,col),to=source.offsetByCodePoints(from,1),absolute=d.lineStart(line)+from;
                float x=58+(col-left)*advance;
                if(absolute>=d.selectionStart()&&absolute<d.selectionEnd())rect(x,y,advance,24,4);
                String glyph=source.substring(from,to);if(glyph.equals("\t"))glyph="→";
                text(glyph,x,y+19,20,source.trim().startsWith("--")?13:7);
            }
            if(line==d.line())rect(58+(d.column()-left)*advance,y+2,2,21,10);
            final int viewLeft=left;
            for(int col=0;col<columns;col++){final int column=viewLeft+col;
                hit(58+col*advance,y,advance,24,()->{if(s.mode!=Mode.CODE||d.panel!=LuaDraft.Panel.CURSOR)return;d.point(line,column);changed.run();invalidate();});}
        }
        c.restore();
        if(keys){
            float y=bottom+10,cell=(w-32)/10;
            String[] labels={"abc","ABC","() +"};
            for(int i=0;i<3;i++){final int page=i;
                button(labels[i],16+i*(w-32)/3,y,(w-40)/3,32,d.page==i,()->{d.page=page;d.key=Math.min(d.key,LuaDraft.PAGES[page].length()-1);changed.run();invalidate();});}
            String chars=LuaDraft.PAGES[d.page];
            for(int i=0;i<chars.length();i++){final int index=i;float x=16+i%10*cell,ky=y+40+i/10*36;
                rect(x,ky,cell-3,32,i==d.key?10:0);String label=chars.charAt(i)==' '?"_":chars.substring(i,i+1);
                text(label,x+(cell-width(label,22))/2,ky+24,22,i==d.key?1:7);
                hit(x,ky,cell-3,32,()->{d.key=index;action(Action.CONFIRM);});}
            fitted("L/R: abc / символы · X: все правки",16,bodyBottom-34,16,6,w-32);
            fitted("Y: отменить ввод · START: сохранить и тест",16,bodyBottom-12,16,13,w-32);
        }else{
            button(ok()+" Ввод",16,bottom+12,(w-40)/2,36,false,()->action(Action.CONFIRM));
            button(w<500?"X Вставка":"X Вставить Lua",24+(w-40)/2,bottom+12,(w-40)/2,36,false,()->action(Action.CONTEXT));
            float hint=(w-32)/3;
            fitted("L: поля",16,bodyBottom-36,16,14,hint-4);
            hit(16,bodyBottom-54,hint,26,()->action(Action.PREVIOUS));
            fitted(w<500?"R:найти":"R: перейти",16+hint,bodyBottom-36,16,12,hint-4);
            hit(16+hint,bodyBottom-54,hint,26,()->action(Action.NEXT));
            fitted(w<500?"L2:проба":"L2: проверка",16+hint*2,bodyBottom-36,16,11,hint-4);
            hit(16+hint*2,bodyBottom-54,hint,26,()->action(Action.CHECK));
            fitted(w<500?(d.dirty()?"Черновик":"Сохранено")+" · Select: правки":d.dirty()?"Черновик · Select: правки · START: тест":"Без изменений · Select: правки · START: тест",16,bodyBottom-13,16,6,w-32);
        }
        rect(0,bodyBottom,w,44,0);
        key(ok()+(keys?" символ":" ввод"),12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+(keys?" курсор":" назад"),w/3+8,bodyBottom,6,()->action(Action.CANCEL));
        key(keys?"X правки":"X вставка",w*2/3+8,bodyBottom,14,()->action(Action.CONTEXT));
        if(menu||exit){
            hits.clear(); // Modal controls own touch as well as controller input.
            rect(0,bodyBottom,w,44,0);
            float dw=Math.min(w-32,500),dx=(w-dw)/2,dy=112;
            int visible=Math.max(3,(int)((bodyBottom-dy-52)/40));
            String[] entries=exit?new String[]{"Продолжить правку","Сохранить и закрыть","Отбросить черновик"}:LuaDraft.COMMANDS;
            int first=Math.max(0,Math.min(d.menu-visible/2,entries.length-visible));
            rect(dx,dy,dw,bodyBottom-dy,1);outline(dx,dy,dw,bodyBottom-dy,14);
            text(exit?"Есть несохранённый текст":"Правки  "+(d.menu+1)+" / "+entries.length,dx+12,dy+28,20,14);
            for(int i=0;i<visible&&first+i<entries.length;i++){final int selected=first+i;
                button(entries[selected],dx+8,dy+40+i*40,dw-16,36,d.menu==selected,()->{d.menu=selected;action(Action.CONFIRM);});}
            key(ok()+" выбрать",16,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" к коду",w/2,bodyBottom,6,()->action(Action.CANCEL));
        }
    }
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);animationClock=0;}
    private void animationImage(SpriteRegion r,float left,float top,float size){
        animationImage(r,left,top,size,r.width,r.height);
    }
    private void animationImage(SpriteRegion r,float left,float top,float size,int canvasWidth,int canvasHeight){
        for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){int color=s.cart().sheetPixel(xx,yy);sceneBitmap.setPixel(xx,yy,color==0?0:COLORS[color]);}
        rect(left,top,size,size,0);
        for(int yy=0;yy<4;yy++)for(int xx=0;xx<4;xx++)if((xx+yy)%2==0)rect(left+xx*size/4,top+yy*size/4,size/4,size/4,5);
        float zoom=size/Math.max(canvasWidth,canvasHeight),ox=left+(size-canvasWidth*zoom)/2,oy=top+(size-canvasHeight*zoom)/2;
        c.drawBitmap(sceneBitmap,new android.graphics.Rect(r.x,r.y,r.x+r.width,r.y+r.height),new RectF(ox,oy,ox+r.width*zoom,oy+r.height*zoom),p);
    }
    private void animationEditor(){
        LuaDraft d=s.codeDraft;SpriteAnimation a=d.animation;hits.clear();
        if(a.picker!=null){animationClock=0;animationPicker(a);return;}
        if(a.review){animationClock=0;animationReview(a);return;}
        long now=SystemClock.uptimeMillis();
        if(a.playing&&s.mode==Mode.CODE&&getWindowVisibility()==VISIBLE){if(animationClock!=0)a.advance((int)Math.min(100,now-animationClock));animationClock=now;if(a.playing)postInvalidateDelayed(16);}
        else animationClock=0;
        fitted(d.animationEdit==null?"Анимация · кадры":"Анимация · правка",16,75,24,14,w-32);
        fitted(d.animationEdit!=null?"Блок "+d.animationEdit.firstLine+"–"+d.animationEdit.lastLine:w<500?"Просмотр без кода игры":"Предпросмотр · без кода игры",16,100,16,6,w-32);
        animationImage(a.frame(a.visibleFrame()).region,16,112,96,a.previewWidth(),a.previewHeight());
        fitted((a.playing?"Играет ":"Пауза ")+(a.visibleFrame()+1),128,130,18,10,w-144);
        int firstThumb=Math.max(0,Math.min(a.selected-1,a.count()-3));float thumb=Math.min(48,(w-160)/3);
        for(int n=0;n<3&&firstThumb+n<a.count();n++){
            final int index=firstThumb+n;float left=128+n*(thumb+6);animationImage(a.frame(index).region,left,142,thumb);
            outline(left-1,141,thumb+2,thumb+2,index==a.selected?10:13);
            fitted(""+(index+1),left,154+thumb,14,index==a.selected?10:6,thumb);
            hit(left,142,thumb,thumb,()->{a.select(index);changed.run();invalidate();});
        }
        int rows=Math.max(2,(int)((bodyBottom-290)/34));int first=Math.max(0,Math.min(a.field-rows/2,9-rows));
        for(int n=0;n<rows&&first+n<9;n++){final int row=first+n;
            button(a.label(row),16,228+n*34,w-32,30,a.field==row,()->{a.field=row;action(Action.CONFIRM);});}
        fitted(w<500?"↑↓ поля · ←→ · L/R кадр":"↑↓ поле · ←→ менять · L/R кадр",16,bodyBottom-44,16,13,w-32);
        fitted("Y стоп · "+a.duration()+" мс всего",16,bodyBottom-20,16,6,w-32);
        hit(16,bodyBottom-38,w-32,32,()->action(Action.UNDO));
        rect(0,bodyBottom,w,44,0);
        key(ok()+" выбор",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" отмена",w/3,bodyBottom,6,()->action(Action.CANCEL));
        key(a.playing?"X пауза":"X пуск",w*2/3,bodyBottom,14,()->action(Action.CONTEXT));
    }
    private void animationPicker(SpriteAnimation a){
        SpritePlacement v=a.picker;SpriteRegion r=v.source();
        fitted(v.phase==2?"Место анимации":"Область кадра "+(a.selected+1),16,75,24,14,w-32);
        fitted(v.phase==0?"Первый угол · A дальше":v.phase==1?"Второй угол · A готово":"Внутри _draw после cls()",16,100,18,7,w-32);
        float size=Math.min(256,Math.min(w-32,bodyBottom-256)),left=(w-size)/2,top=112,cell=size/128;
        placementArea=new RectF(left,top,left+size,top+size);
        for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++)sceneBitmap.setPixel(xx,yy,COLORS[v.phase<2?s.cart().sheetPixel(xx,yy):v.pixel(s.cart(),xx,yy)]);
        c.drawBitmap(sceneBitmap,null,placementArea,p);outline(left-2,top-2,size+4,size+4,13);
        c.save();c.clipRect(left-2,top-2,left+size+2,top+size+2);
        outline(left+(v.phase<2?r.x:v.x)*cell,top+(v.phase<2?r.y:v.y)*cell,r.width*cell,r.height*cell,10);
        if(v.phase<2)outline(left+(v.phase==0?v.ax:v.bx)*cell-2,top+(v.phase==0?v.ay:v.by)*cell-2,5,5,14);c.restore();
        fitted(v.phase<2?r.x+", "+r.y+" · "+r.width+" × "+r.height:"X "+v.x+" · Y "+v.y,16,top+size+24,18,10,w-32);
        fitted(v.phase<2&&r.sharesMap()?"Общая память с картой":"Лист и карта не изменяются",16,top+size+46,16,6,w-32);
        button("X Шаг: "+v.step+" пикс.",16,bodyBottom-82,w-32,32,false,()->action(Action.CONTEXT));
        button(ok()+(v.phase==0?" Второй угол":" Готово"),16,bodyBottom-42,w-32,32,true,()->action(Action.CONFIRM));
        rect(0,bodyBottom,w,44,0);key(back()+" назад",12,bodyBottom,6,()->action(Action.CANCEL));
        fitted("↑↓←→ двигать",w/2,bodyBottom+28,16,13,w/2-12);
    }
    private void animationReview(SpriteAnimation a){
        LuaDraft d=s.codeDraft;boolean editing=d.animationEdit!=null;
        fitted(editing?d.animationBefore?"Анимация · было":"Анимация · будет":"Анимация · Lua",16,75,24,14,w-32);
        fitted(editing?"X было / будет":"В _draw · от запуска игры",16,100,16,6,w-32);
        if(editing)hit(16,82,w-32,24,()->action(Action.CONTEXT));
        ArrayList<String> rows=new ArrayList<>();
        for(String line:(editing&&d.animationBefore?d.animationEdit.initial.code():a.code()).split("\n")){
            if(line.isEmpty()){rows.add("");continue;}
            while(!line.isEmpty()){int n=line.length();while(n>1&&width(line.substring(0,n),18)>w-48)n--;rows.add(line.substring(0,n));line=line.substring(n);}
        }
        int visible=Math.max(1,(int)((bodyBottom-162)/22));a.previewLine=Math.min(a.previewLine,Math.max(0,rows.size()-visible));
        rect(16,112,w-32,visible*22+8,0);
        for(int n=0;n<visible&&a.previewLine+n<rows.size();n++)text(rows.get(a.previewLine+n),24,134+n*22,18,7);
        fitted((a.previewLine+1)+"–"+Math.min(rows.size(),a.previewLine+visible)+"/"+rows.size()+(w<500?" · ↑↓ · ←→":" · ↑↓ читать · ←→ листать"),16,bodyBottom-12,16,6,w-32);
        rect(0,bodyBottom,w,44,0);key(ok()+(editing?d.animationBefore?" будет":" заменить":" вставить"),12,bodyBottom,10,()->action(Action.CONFIRM));key(back()+" кадры",w/2,bodyBottom,6,()->action(Action.CANCEL));
        hit(16,112,(w-32)/2,visible*22+8,()->action(Action.LEFT));hit(w/2,112,(w-32)/2,visible*22+8,()->action(Action.RIGHT));
    }
    private void spritePlacement(){
        LuaDraft d=s.codeDraft;SpritePlacement v=d.placement;SpriteRegion r=v.source();hits.clear();
        fitted("Спрайт в игре",16,75,24,14,w-32);
        String[] stages={"1/4 Первый угол области","2/4 Второй угол области","3/4 Место на экране","4/4 Перед строкой "+(d.line()+1)};
        fitted(stages[v.phase],16,100,18,7,w-32);
        float size=Math.min(256,Math.min(w-32,bodyBottom-262)),left=(w-size)/2,top=116,cell=size/128;
        placementArea=new RectF(left,top,left+size,top+size);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)sceneBitmap.setPixel(x,y,COLORS[v.phase<2?s.cart().sheetPixel(x,y):v.pixel(s.cart(),x,y)]);
        c.drawBitmap(sceneBitmap,null,new RectF(left,top,left+size,top+size),p);
        outline(left-2,top-2,size+4,size+4,13);
        c.save();c.clipRect(left-2,top-2,left+size+2,top+size+2);
        outline(left+(v.phase<2?r.x:v.x)*cell,top+(v.phase<2?r.y:v.y)*cell,Math.max(2,r.width*cell),Math.max(2,r.height*cell),10);
        if(v.phase<2)outline(left+(v.phase==0?v.ax:v.bx)*cell-2,top+(v.phase==0?v.ay:v.by)*cell-2,5,5,14);
        c.restore();
        fitted(v.phase<2?r.width+" × "+r.height+" · лист "+r.x+", "+r.y:"X "+v.x+" · Y "+v.y+" · "+r.width+" × "+r.height,16,top+size+22,18,10,w-32);
        if(v.phase==3){
            text("sspr("+r.x+","+r.y+","+r.width+","+r.height+",",16,bodyBottom-108,18,7);
            text("     "+v.x+","+v.y+")",16,bodyBottom-86,18,7);
            fitted("Внутри _draw, после cls()",16,bodyBottom-64,18,6,w-32);
        }else{
            fitted(v.phase<2?(r.sharesMap()?"Нижний лист общий с картой":"Крестовина: выбрать область"):"Пример без кода игры",16,top+size+44,18,6,w-32);
            button("X Шаг: "+v.step+" пикс.",16,bodyBottom-86,w-32,32,false,()->action(Action.CONTEXT));
        }
        String[] actions={"Выбрать первый угол","Область выбрана","Проверить вставку","Вставить в черновик"};
        button(ok()+" "+actions[v.phase],16,bodyBottom-44,w-32,34,true,()->action(Action.CONFIRM));
        rect(0,bodyBottom,w,44,0);
        key(back()+(v.phase==0?" отмена":" назад"),12,bodyBottom,6,()->action(Action.CANCEL));
        fitted(v.phase==3?"Test — потом":"↑↓←→ двигать",w/2,bodyBottom+28,18,13,w/2-12);
    }
    private void diagnostic(){
        hits.clear();RuntimeDiagnostic d=s.diagnostic;
        fitted("Проверка запуска",16,76,26,14,w-32);
        fitted("Официальный PICO-8 · копия черновика",16,102,16,6,w-32);
        if(s.diagnosticDetails&&d!=null){
            String[] rows=d.log.split("\n",-1);int count=Math.max(1,(int)((bodyBottom-177)/22));
            for(int i=0;i<count&&s.diagnosticScroll+i<rows.length;i++)fitted(rows[s.diagnosticScroll+i],16,142+i*22,16,7,w-32);
            fitted("↑↓ журнал · Select: результат",16,bodyBottom-18,16,13,w-32);
        }else{
            String title=s.diagnosticPending?"Проверяю копию…":d==null?"Попробуем запустить?":d.kind==RuntimeDiagnostic.Kind.ERROR?"PICO-8 сообщил об ошибке":d.kind==RuntimeDiagnostic.Kind.OBSERVED?"Пробный запуск завершён":d.message;
            wrapped(title,16,145,24,s.diagnosticPending?10:d!=null&&d.kind==RuntimeDiagnostic.Kind.ERROR?8:10,w-32,2);
            if(d==null){
                wrapped("До 4 секунд, без нажатий и звука. Код выполняется на отдельной копии; её сохранения не попадут в проект.",16,212,20,7,w-32,4);
                wrapped("Дальнейшую игру и управление проверяй через обычный Test.",16,bodyBottom-106,18,6,w-32,2);
            }else if(d.kind==RuntimeDiagnostic.Kind.ERROR){
                fitted(d.line>=0?"Строка "+(d.line+1):"Место в исходнике пока не сопоставлено",16,211,20,12,w-32);
                wrapped(d.message,16,244,20,7,w-32,3);
                if(s.diagnosticStale)wrapped("Черновик изменился. Повтори проверку перед переходом.",16,bodyBottom-111,18,9,w-32,2);
                else if(d.line>=0){rect(12,bodyBottom-117,w-24,52,0);fitted(s.codeDraft.lineText(d.line),20,bodyBottom-86,18,10,w-40);}
            }else{
                wrapped(d.kind==RuntimeDiagnostic.Kind.OBSERVED?"За время проверки сообщений об ошибках не получено. Это ещё не проверка всей игры.":"Черновик на месте. Можно вернуться к коду или повторить проверку.",16,212,20,7,w-32,4);
            }
            if(!s.diagnosticPending){
                if(d==null)button(ok()+" Проверить копию",16,bodyBottom-52,w-32,36,true,()->action(Action.CONFIRM));
                else if(d.line>=0&&!s.diagnosticStale)button(ok()+" К строке "+(d.line+1),16,bodyBottom-52,w-32,36,true,()->action(Action.CONFIRM));
                else button("X Проверить ещё",16,bodyBottom-52,w-32,36,false,()->action(Action.CONTEXT));
            }
        }
        rect(0,bodyBottom,w,44,0);
        key(back()+(s.diagnosticPending?" отменить":" к коду"),12,bodyBottom,6,()->action(Action.CANCEL));
        if(d!=null)key("Select журнал",w/2,bodyBottom,14,()->action(Action.MENU));
    }
    private void luaNavigation(){
        LuaDraft d=s.codeDraft;LuaNavigation n=d.navigation;hits.clear();
        fitted("Перейти / game.p8",16,75,24,14,w-32);
        fitted("L/R: функции / строка · Select: вкладка",16,99,16,6,w-32);
        float bw=(w-44)/2;
        button("Функции · "+n.entries.size(),16,112,bw,36,n.tab==0,()->{n.tab=0;changed.run();invalidate();});
        button("Номер строки",28+bw,112,bw,36,n.tab==1,()->{n.tab=1;changed.run();invalidate();});
        float preview=bodyBottom-174;
        if(n.tab==0){
            int rows=Math.max(1,(int)((preview-188)/34));
            int first=Math.max(0,Math.min(n.index-rows/2,n.entries.size()-rows));
            for(int r=0;r<rows&&first+r<n.entries.size();r++){
                final int item=first+r;LuaNavigation.Entry entry=n.entries.get(item);
                button((entry.line+1)+"  "+entry.name,16,158+r*34,w-32,30,item==n.index,()->{n.index=item;changed.run();invalidate();});
            }
            fitted(n.entries.isEmpty()?"Именованных функций пока нет":n.truncated?"Часть списка · используй номер строки":"↑↓ выбор · ←→ по 6 · "+(n.index+1)+" / "+n.entries.size(),16,preview-12,16,13,w-32);
        }else{
            fitted("Строка 1 — "+n.lines,16,175,18,6,w-32);
            String number=String.format(java.util.Locale.ROOT,"%0"+n.digits()+"d",n.target);
            float cell=Math.min(42,(w-140)/n.digits()),x=(w-n.digits()*cell)/2;
            button("-",16,185,42,42,false,()->action(Action.UP));
            button("+",w-58,185,42,42,false,()->action(Action.DOWN));
            for(int i=0;i<number.length();i++){
                final int digit=number.length()-1-i;boolean selected=digit==n.digit;
                rect(x+i*cell,185,cell-3,42,selected?10:0);
                text(number.substring(i,i+1),x+i*cell+8,214,26,selected?1:7);
                hit(x+i*cell,185,cell-3,42,()->{n.digit=digit;changed.run();invalidate();});
            }
            fitted("←→ разряд · ↑↓ меньше / больше",16,preview-12,16,13,w-32);
        }
        int line=n.selectedLine();
        rect(12,preview,w-24,96,0);
        fitted("СТРОКА "+(line+1)+" · предпросмотр",20,preview+20,16,12,w-40);
        for(int i=0;i<3&&line+i<d.lineCount();i++)fitted(d.lineText(line+i),20,preview+43+i*20,16,i==0?10:6,w-40);
        button(d.canGoBack()?"X Вернуться к прежнему месту":"Переходов ещё не было",16,bodyBottom-66,w-32,34,false,()->action(Action.CONTEXT));
        fitted(ok()+": перейти · "+back()+": остаться на прежнем месте",16,bodyBottom-12,16,13,w-32);
        rect(0,bodyBottom,w,44,0);
        key(ok()+" перейти",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" отмена",w/2,bodyBottom,6,()->action(Action.CANCEL));
    }
    private void luaInsertion(){
        LuaDraft d=s.codeDraft;LuaInsert i=d.insertion;
        hits.clear();
        if(i.screen==LuaInsert.Screen.SYMBOLS){luaSymbols(i);return;}
        if(i.screen==LuaInsert.Screen.PREVIEW){luaRecipePreview(i);return;}
        fitted(i.screen==LuaInsert.Screen.CATALOG?"+ Вставить Lua":d.callEdit!=null&&d.callEdit.name.equals("rule:elseif")?"elseif · иначе если":i.item().title,16,75,24,14,w-32);
        fitted(i.tileRecipe()?"Начало Lua · x/y в пикселях":d.callEdit!=null?(i.item().id.equals("set")||i.item().id.equals("add")?"Имя и значение меняются только в этой строке":"Поля строки "+(d.line()+1)+" · "+back()+" отменить"):"Перед строкой "+(d.line()+1)+" · исходник не заменяется",16,100,16,6,w-32);
        if(i.screen==LuaInsert.Screen.FIELDS&&i.cameraMode()){
            rect(0,80,w,26,1);
            fitted(i.item().id.equals("camera_rooms")?"Y следить · комната 128×128":"Y комнаты · сменить режим",16,100,16,6,w-32);
            hit(16,80,w-32,28,()->action(Action.UNDO));
        }
        if(i.screen==LuaInsert.Screen.FIELDS&&i.doorRecipe()){
            rect(0,80,w,26,1);
            fitted("A ↔ B · после выхода можно снова",16,100,16,6,w-32);
        }
        if(i.screen==LuaInsert.Screen.CATALOG){
            s.toolCatalogue.normalize(i);
            java.util.List<Integer> items=s.toolCatalogue.items();int selected=items.indexOf(i.selected);
            rect(0,80,w,27,1);
            fitted("← "+art.pikoos.lab.core.ToolCatalogue.CATEGORIES[s.toolCatalogue.category]+" →  · "+items.size(),16,100,16,6,w-32);
            hit(16,80,(w-32)/2,28,()->action(Action.LEFT));hit(w/2,80,(w-32)/2,28,()->action(Action.RIGHT));
            int rows=Math.max(3,(int)((bodyBottom-220)/38));
            int first=Math.max(0,Math.min(selected-rows/2,items.size()-rows));
            for(int r=0;r<rows&&first+r<items.size();r++){final int n=items.get(first+r);
                button((s.toolCatalogue.favorite(n)?"* ":"")+LuaInsert.ITEMS[n].title,16,112+r*38,w-32,34,n==i.selected,()->{i.choose(n);action(Action.CONFIRM);});}
            if(items.isEmpty())wrapped("Пока пусто. В другой категории выбери инструмент и нажми Y.",16,158,20,7,w-32,4);
            float y=bodyBottom-92;rect(16,y-18,w-32,2,14);
            wrapped(items.isEmpty()?"Избранное общее для твоих проектов.":i.item().help,16,y+8,18,7,w-32,3);
            rect(0,bodyBottom,w,44,0);
            key(ok()+" выбрать",12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" назад",w/3,bodyBottom,6,()->action(Action.CANCEL));
            if(!items.isEmpty())key(s.toolCatalogue.favorite(i.selected)?(w<500?"Y убрать":"Y убрать *"):"Y в *",w*2/3,bodyBottom,14,()->action(Action.UNDO));
            return;
        }
        if(i.screen==LuaInsert.Screen.TEXT){
            text(i.item().fields[i.field].label,16,133,18,7);
            rect(16,139,w-32,32,i.replaceAll?4:0);
            String shown=i.input;while(shown.length()>1&&width(shown,22)>w-56)shown=shown.substring(1);
            text(shown+"|",24,165,22,10);
            fitted(i.replaceAll?"Новый ввод заменит значение":"Ввод в конец · B отменяет поле",16,188,14,6,w-32);
            fitted("L/R или Select: abc / символы",16,205,14,13,w-32);
            float y=bodyBottom-230,cell=(w-32)/10;
            String[] labels={"abc","ABC","() +"};
            for(int n=0;n<3;n++){final int page=n;
                button(labels[n],16+n*(w-32)/3,y,(w-40)/3,32,i.page==n,()->{i.page=page;i.key=Math.min(i.key,LuaDraft.PAGES[page].length()-1);changed.run();invalidate();});}
            String chars=LuaDraft.PAGES[i.page];
            for(int n=0;n<chars.length();n++){final int index=n;float x=16+n%10*cell,ky=y+40+n/10*36;
                rect(x,ky,cell-3,32,n==i.key?10:0);String label=chars.charAt(n)==' '?"_":chars.substring(n,n+1);
                text(label,x+(cell-width(label,22))/2,ky+24,22,n==i.key?1:7);
                hit(x,ky,cell-3,32,()->{i.key=index;action(Action.CONFIRM);});}
            button("X стереть",16,bodyBottom-42,(w-40)/2,32,false,()->action(Action.CONTEXT));
            button("Y всё / конец",24+(w-40)/2,bodyBottom-42,(w-40)/2,32,false,()->action(Action.UNDO));
            rect(0,bodyBottom,w,44,0);
            key(ok()+" знак",12,bodyBottom,10,()->action(Action.CONFIRM));
            key(back()+" отмена",w/3,bodyBottom,6,()->action(Action.CANCEL));
            key("START готово",w*2/3,bodyBottom,14,()->action(Action.TEST));
            // Select cycles pages too: shoulders are not required.
            hit(16,182,w-32,28,()->action(Action.MENU));
            return;
        }
        boolean longCall=d.callEdit!=null&&((i.item().id.equals("sspr")||i.item().id.equals("map"))&&w<500||i.cameraRecipe()||i.doorRecipe());
        int count=i.item().fields.length,rows=Math.min(count+1,Math.max(2,(int)((bodyBottom-(longCall?298:254))/36)));
        if(i.tileRecipe())rows=2;
        if(i.doorRecipe())rows=Math.min(rows,5);
        int first=Math.max(0,Math.min(i.field-rows/2,count+1-rows));
        for(int r=0;r<rows&&first+r<=count;r++){
            final int index=first+r;float y=112+r*36;boolean focus=index==i.field;
            if(index==count){button(i.fullPreview()?"Просмотреть Lua":d.callEdit!=null?"Применить правку":"+ Вставить в черновик",16,y,w-32,32,focus,()->{i.field=index;action(Action.CONFIRM);});continue;}
            rect(16,y,w-32,32,focus?10:0);
            float labelWidth=Math.min(152,w*.38f);
            fitted(i.item().fields[index].label,24,y+23,18,focus?1:6,labelWidth-12);
            fitted(i.display(index),24+labelWidth,y+23,18,focus?1:7,w-labelWidth-62);
            if(i.colorChoice(index))rect(w-44,y+8,16,16,Integer.parseInt(i.value(index)));
            hit(16,y,w-32,32,()->{i.field=index;action(Action.CONFIRM);});
        }
        float previewY=124+rows*36;
        text(i.doorRecipe()?"Переход · предварительно":i.fullPreview()?"Lua · предварительно":d.callEdit!=null?"Изменение Lua":"Lua после вставки",16,previewY+14,18,14);
        rect(16,previewY+24,w-32,bodyBottom-previewY-62,0);
        c.save();c.clipRect(16,previewY+24,w-16,bodyBottom-34);
        if(i.doorRecipe()){
            fitted("A: ("+i.value(3)+", "+i.value(4)+") ↔ B: ("+i.value(5)+", "+i.value(6)+")",24,previewY+46,18,7,w-48);
            fitted("Области "+i.value(7)+"×"+i.value(8)+" px · точка "+i.value(0)+", "+i.value(1),24,previewY+70,18,7,w-48);
            fitted("Вход → начало другой области",24,previewY+94,16,6,w-48);
            fitted("Полный Lua — в просмотре",24,previewY+118,16,14,w-48);
        }
        else if(longCall){codePreview("Было: "+d.callEdit.original.trim(),24,previewY+46,6,w-48);codePreview("Будет: "+d.callEdit.preview(i).trim(),24,previewY+90,10,w-48);}
        else if(d.callEdit!=null){fitted("Было: "+d.callEdit.original.trim(),24,previewY+46,16,6,w-48);fitted("Будет: "+d.callEdit.preview(i).trim(),24,previewY+70,16,10,w-48);}
        else {int line=0;for(String row:i.code().split("\n")){fitted(row,24,previewY+46+line*22,18,7,w-48);line++;}}
        c.restore();
        fitted(w<500?"↑↓ поле  ←→ менять  "+ok()+" ввод":"↑↓ поле · ←→ значение · "+ok()+" ввод / "+(d.callEdit!=null?"правка":"вставка"),16,bodyBottom-12,16,6,w-32);
        rect(0,bodyBottom,w,44,0);
        key(ok()+" выбрать",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" назад",w/3,bodyBottom,6,()->action(Action.CANCEL));
        if(i.canBrowse())key("X имена",w*2/3,bodyBottom,14,()->action(Action.CONTEXT));
        else if(i.fullPreview())key("X Lua",w*2/3,bodyBottom,14,()->action(Action.CONTEXT));
    }
    private void luaRecipePreview(LuaInsert i){
        fitted(i.cameraRecipe()||i.doorRecipe()||i.conditionalBranches()?i.item().title:i.motionRecipe()?"Движение · код":i.item().id.equals("move_call")?"Движение · вызов":"Область · код функции",16,75,22,14,w-32);
        fitted(i.conditionalBranches()?"Одно сравнение → одна из ветвей":i.doorRecipe()?"В _update · после изменения X/Y":i.cameraRecipe()?(s.codeDraft.callEdit!=null?"Было / будет · только эта строка":i.item().id.equals("camera_reset")?"После мира · перед счётом и меню":"Перед миром · после cls()"):i.motionRecipe()?"Сначала X, затем Y · пиксели":i.item().id.equals("move_call")?"Новые X,Y · на месте курсора":"Карта: x,y,w,h в пикселях",16,100,16,6,w-32);
        java.util.ArrayList<String> rows=new java.util.ArrayList<>();
        LuaCall edit=s.codeDraft.callEdit;
        String preview=edit==null?i.code():"Было:\n"+edit.original+"\n\nБудет:\n"+edit.preview(i);
        if(i.conditionalBranches())preview+="\nПосле вставки курсор под then.\nДобавь туда действия для «да».\nПод else — действия для «нет».\nL на if снова откроет сравнение.\nИзменение переменной внутри\nthen не запускает else тут же.";
        if(i.doorRecipe())preview="Вход A → начало B, B → A.\nПовтор — после выхода из обеих.\nПроверяется точка X/Y, не спрайт.\nРисование задаётся отдельно.\n\n"+preview;
        if(i.cameraRecipe()&&edit==null)preview+=(i.item().id.equals("camera_reset")?"\nДальше рисуй счёт и меню.\nОни останутся на экране.":"\nДальше рисуй карту и объекты.\nПеред счётом и меню добавь\n«Камера · экран».\nЭкран = мир − камера.");
        if(i.item().id.equals("camera_rooms"))preview+="\n\nКомната: 128×128 пикселей.\nПереход на X/Y = 128, 256…\nЗа краем — крайняя комната.\nКоординаты объектов прежние.";
        for(String line:preview.split("\n")){
            if(line.isEmpty()){rows.add("");continue;}
            while(!line.isEmpty()){
                int n=line.length();while(n>1&&width(line.substring(0,n),18)>w-48)n--;
                rows.add(line.substring(0,n));line=line.substring(n);
            }
        }
        int visible=Math.max(1,(int)((bodyBottom-162)/22));
        i.previewLine=Math.min(i.previewLine,Math.max(0,rows.size()-visible));
        rect(16,112,w-32,visible*22+8,0);
        for(int n=0;n<visible&&i.previewLine+n<rows.size();n++)text(rows.get(i.previewLine+n),24,134+n*22,18,7);
        fitted((i.previewLine+1)+"–"+Math.min(rows.size(),i.previewLine+visible)+"/"+rows.size()+(w<500?" · ↑↓ · ←→":" · ↑↓ читать · ←→ листать"),16,bodyBottom-12,16,6,w-32);
        rect(0,bodyBottom,w,44,0);
        key(ok()+(edit==null?" вставить":" заменить"),12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" поля",w/2,bodyBottom,6,()->action(Action.CANCEL));
        hit(16,112,(w-32)/2,visible*22+8,()->action(Action.LEFT));
        hit(w/2,112,(w-32)/2,visible*22+8,()->action(Action.RIGHT));
    }
    private void luaSymbols(LuaInsert i){
        fitted("Выбрать значение",16,75,24,14,w-32);
        fitted(i.item().fields[i.field].label+": "+i.value(i.field),16,100,16,6,w-32);
        button("Из кода",16,112,(w-40)/2,32,i.symbolGroup==0,()->{if(i.symbolGroup!=0){i.symbolTab();changed.run();invalidate();}});
        if(i.canBrowseApi())button("PICO-8",24+(w-40)/2,112,(w-40)/2,32,i.symbolGroup==1,()->{if(i.symbolGroup!=1){i.symbolTab();changed.run();invalidate();}});
        java.util.List<art.pikoos.lab.core.LuaSymbols.Entry> list=i.choices();
        int rows=Math.max(2,(int)((bodyBottom-312)/38));
        int first=Math.max(0,Math.min(i.symbolIndex-rows/2,list.size()-rows));
        for(int row=0;row<rows&&first+row<list.size();row++){
            final int index=first+row;art.pikoos.lab.core.LuaSymbols.Entry e=list.get(index);
            button(e.value+(i.symbolGroup==0?" · строка "+e.line:""),16,156+row*38,w-32,34,index==i.symbolIndex,
                ()->{i.symbolIndex=index;action(Action.CONFIRM);});
        }
        float y=bodyBottom-142;rect(16,y-10,w-32,2,14);
        if(list.isEmpty()){
            wrapped(i.completeSymbols()?(i.symbolGroup==0?"Подходящих общих имён пока нет. Можно ввести значение вручную.":"Эти имена API переопределены в коде. Используй ручной ввод."):
                "Не удалось разобрать имена. Ручной ввод остаётся доступен.",16,178,18,7,w-32,4);
        }else{
            art.pikoos.lab.core.LuaSymbols.Entry e=list.get(i.symbolIndex);
            fitted("Будет: "+e.value,16,y+16,20,10,w-32);
            wrapped(e.help,16,y+42,16,7,w-32,3);
        }
        fitted(ok()+" заменит параметр · "+back()+" оставит прежний",16,bodyBottom-38,14,6,w-32);
        fitted(i.canBrowseApi()?"Select / L/R: источник · ←→ листать":"↑↓ выбрать · ←→ листать",16,bodyBottom-14,14,13,w-32);
        rect(0,bodyBottom,w,44,0);
        key(ok()+" выбрать",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" назад",w/2,bodyBottom,6,()->action(Action.CANCEL));
    }
    private void wrapped(String value,float x,float y,int size,int color,float max,int rows){
        for(int row=0;row<rows&&!value.isEmpty();row++){
            int end=value.length();while(end>1&&width(value.substring(0,end),size)>max)end--;
            if(end<value.length()){int space=value.lastIndexOf(' ',end);if(space>0)end=space;}
            String part=value.substring(0,end);text(part,x,y+row*22,size,color);value=value.substring(end).trim();
        }
    }
    private void codePreview(String value,float x,float y,int color,float max){
        int end=value.length();while(end>1&&width(value.substring(0,end),18)>max)end--;
        if(end<value.length()){int comma=value.lastIndexOf(',',end-1);if(comma>=0)end=comma+1;}
        text(value.substring(0,end),x,y,18,color);fitted(value.substring(end),x,y+22,18,color,max);
    }
    private void syntax(String source,float x,float y) {
        if(source.trim().startsWith("--")) {text(source,x,y,20,13);return;}
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*|[0-9]+(?:\\.[0-9]+)?|[^a-zA-Z_0-9]+").matcher(source);
        while(m.find()) {String token=m.group();int color=7;
            if(token.matches("[0-9].*"))color=9;
            if((" function end if then else local for in do and or true false ").contains(" "+token+" "))color=14;
            if((" btn btnp spr sspr cls rectfill circfill line pset all min mid ").contains(" "+token+" "))color=12;
            text(token,x,y,20,color);x+=width(token,20);
        }
    }
    private void sprites() {
        float size=Math.min(256,Math.min((w-56)*.46f,bodyBottom-232));size=Math.max(128,((int)size/16)*16);
        float x=16,y=180,right=size+32,rw=w-right-16;
        SpriteRegion region=s.selection();
        text((s.cart().hasHero()&&s.cart().hero().sameImage(region)?"ГЕРОЙ":s.region==null?"СПРАЙТ "+(s.spriteSlot+1):"ОБЛАСТЬ")+" / "+region.width+" × "+region.height,16,169,18,14);
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
        if(s.pendingStroke()){
            if(s.lineX>=viewX&&s.lineX<viewX+viewWidth&&s.lineY>=viewY&&s.lineY<viewY+viewHeight)
                outline(x+(s.lineX-viewX)*cell,y+(s.lineY-viewY)*cell,cell,cell,10);
        }
        if(s.mode==Mode.CANVAS) {
            float cx=x+(s.cursorX-viewX)*cell,cy=y+(s.cursorY-viewY)*cell;
            outline(cx-1,cy-1,cell+2,cell+2,0);outline(cx,cy,cell,cell,7);
        }
        String draftLabel=s.drawTool==DrawTool.LINE?"Линия":(Math.abs(s.cursorX-s.lineX)+1)+" × "+(Math.abs(s.cursorY-s.lineY)+1);
        fitted(s.pendingStroke()?draftLabel+": "+ok()+" готово · "+back()+" отмена":s.mode==Mode.CANVAS?DRAW_NAMES[s.drawTool.ordinal()]+" · "+s.cursorX+", "+s.cursorY:ok()+": войти в спрайт",16,180+size+24,16,s.pendingStroke()?10:6,size);
        if(180+size+48<bodyBottom)fitted(s.pendingStroke()?"Начало: "+s.lineX+", "+s.lineY:s.zoom?"Крупно · окно следует за курсором":"X цвет · Y отмена",16,180+size+48,16,6,size);
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
        button("Рамка",right+(rw+8)/2,by,(rw-8)/2,44,s.mode==Mode.NAVIGATE&&s.focus==4,()->action(Action.REGION));
        int count=s.cart().hasHero()&&w>=500?3:2;float bw=(rw-8*(count-1))/count;
        button(s.zoom?"Целиком":"Крупно",right,by+52,bw,40,s.mode==Mode.NAVIGATE&&s.focus==5,()->action(Action.ZOOM));
        button("Копия",right+bw+8,by+52,bw,40,s.mode==Mode.NAVIGATE&&s.focus==6,()->action(Action.COPY_SPRITE));
        if(s.cart().hasHero())button("Герою",w>=500?right+2*(bw+8):16,by+52,w>=500?bw:size,40,s.mode==Mode.NAVIGATE&&s.focus==7,()->action(Action.ASSIGN_HERO));
    }
    private void spriteSheet() {
        text("СПРАЙТЫ ПРОЕКТА",16,169,18,14);
        if(w>=500)text("16 × 16 · X копия",w-186,169,16,6);
        float gap=10,cw=(w-32-gap*3)/4;
        float ch=Math.min(112,(bodyBottom-180-104-gap)/2);
        for(int i=0;i<8;i++) {
            final int slot=i;float x=16+(i%4)*(cw+gap),y=180+(i/4)*(ch+gap);
            boolean chosen=s.sheetFocus==i,used=s.cart().heroSlot()==i,part=s.cart().hasHero()&&s.cart().hero().overlaps(new SpriteRegion(i*16,0,16,16));
            rect(x,y,cw,ch,0);outline(x,y,cw,ch,chosen?10:part?14:13);
            float size=Math.max(32,((int)(ch-38)/16)*16);
            if(s.cart().empty(i))text("+",x+cw/2-8,y+ch/2+1,28,13);
            else sprite(i,x+(cw-size)/2,y+8,size,false);
            if(part){rect(x+cw-14,y+6,8,8,14);}
            String label=used?"Герой":part?"Часть героя":s.cart().empty(i)?"Пусто":"Спрайт "+(i+1);
            fitted(label,x+8,y+ch-10,16,chosen?10:used?14:6,cw-16);
            hit(x,y,cw,ch,()->{s.selectSheet(slot);changed.run();invalidate();});
        }
        float by=180+2*(ch+gap)+2,bw=(w-48)/3;
        String[] labels=s.cart().hasHero()?new String[]{"+ Новый","Копировать","Назначить герою"}:new String[]{"Выбрать область","Копировать","О спрайтах"};
        for(int i=0;i<3;i++){final int choice=8+i;
            button(labels[i],16+i*(bw+8),by,bw,44,s.sheetFocus==choice,()->{s.selectSheet(choice);changed.run();invalidate();});
        }
        button("Область листа · другой размер",16,by+48,w-32,36,s.sheetFocus==11,()->{s.selectSheet(11);changed.run();invalidate();});
    }
    private void assetLibrary(){
        boolean saving=s.mode==Mode.ASSET_SAVE;
        text(saving?"СОХРАНИТЬ СПРАЙТ":"БИБЛИОТЕКА / СПРАЙТЫ",16,169,18,14);
        if(saving){
            SpriteAsset a=s.assetDraft;float size=Math.min(160,bodyBottom-334);
            fitted(a.title+" · "+a.width+" × "+a.height,16,202,24,7,w-32);
            assetPicture(a,(w-size)/2,218,size);
            fitted("Источник: "+a.origin,16,244+size,18,6,w-32);
            fitted("Самостоятельная копия пикселей.",16,272+size,20,7,w-32);
            fitted("Код, флаги и настройки палитры не переносятся.",16,300+size,16,13,w-32);
            fitted("Y категория: "+a.category.title,16,bodyBottom-12,18,10,w-32);
            hit(16,bodyBottom-40,w-32,36,()->action(Action.UNDO));
            return;
        }
        fitted((w<500?"← ":"L ← ")+s.assetFilterTitle()+(w<500?" →":" → R"),16,199,18,6,w-110);
        hit(16,176,(w-32)/2,32,()->action(Action.PREVIOUS));hit(w/2,176,(w-32)/2,32,()->action(Action.NEXT));
        SpriteAsset chosen=s.currentAsset();
        if(chosen==null){
            fitted(s.assetFilter==0?"Здесь будут твои спрайты":"Категория пуста",16,238,22,7,w-32);
            if(s.assetFilter==0){
                fitted("Выбери область во вкладке «Спрайты».",16,266,20,6,w-32);
                fitted("Затем: меню → Ресурсы → X в библиотеку.",16,298,18,6,w-32);
            }else{
                fitted("L/R — другая категория",16,270,18,6,w-32);
                wrapped("Выбери спрайт в «Все» и нажми Select, чтобы назначить ему категорию.",16,310,18,6,w-32,3);
            }
        }else{
            text((s.assetIndex+1)+" / "+s.assets().size(),w-84,199,18,10);
            int perPage=w>=500?3:2,first=(s.assetIndex/perPage)*perPage;
            float gap=12,cw=(w-32-gap*(perPage-1))/perPage,ch=Math.min(176,bodyBottom-304);
            for(int i=0;i<perPage&&first+i<s.assets().size();i++){
                final int index=first+i;SpriteAsset a=s.assets().get(index);float x=16+i*(cw+gap),size=Math.min(cw-16,ch-48);
                rect(x,214,cw,ch,0);assetPicture(a,x+(cw-size)/2,222,size);
                outline(x,214,cw,ch,index==s.assetIndex?10:13);
                fitted(a.title,x+8,214+ch-14,18,index==s.assetIndex?10:7,cw-16);
                hit(x,214,cw,ch,()->{s.selectAsset(index);changed.run();invalidate();});
            }
            float by=214+ch;
            fitted(chosen.width+" × "+chosen.height+" · "+chosen.origin,16,by+28,18,7,w-32);
            fitted("Select: "+chosen.category.title,16,by+52,16,13,w-32);
            hit(16,by+32,w-32,26,()->action(Action.MENU));
            fitted("← → выбрать · Y название",16,by+78,18,6,w-32);
            hit(16,by+56,w-32,34,()->action(Action.UNDO));
        }
    }
    private void assetCategories(){
        hits.clear();fitted("Категория спрайта",16,78,24,14,w-32);
        fitted("Пиксели и использование не меняются",16,102,16,6,w-32);
        SpriteAsset.Category[] categories=SpriteAsset.Category.values();
        for(int n=0;n<categories.length;n++){final int index=n;
            button(categories[n].title,16,120+n*36,w-32,32,n==s.assetCategoryChoice,()->{s.assetCategoryChoice=index;action(Action.CONFIRM);});
        }
        rect(0,bodyBottom,w,44,0);key(ok()+" сохранить",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" отмена",w/2,bodyBottom,6,()->action(Action.CANCEL));
    }
    private void nameEditor(){
        NameEditor e=s.nameEditor;
        text("Название спрайта",16,78,26,14);
        text(e.text().length()+" / 80",w-92,78,18,6);
        rect(16,96,w-32,48,e.replaceAll?10:0);outline(16,96,w-32,48,10);
        String shown=e.text();p.setTextSize(PixelText.size(26));
        while(shown.length()>1&&width(shown+(e.replaceAll?"":"_"),26)>w-58)shown=shown.substring(shown.offsetByCodePoints(0,1));
        text(shown+(e.replaceAll?"":"_"),26,129,26,e.replaceAll?1:7);
        fitted(e.warning.isEmpty()?(e.replaceAll?"Выделено всё · новая буква заменит название":"Ввод в конец · символ «·» вводит пробел"):e.warning,16,165,16,e.warning.isEmpty()?6:8,w-32);
        text("L/R "+(e.latin?"ABC":"АБВ"),16,192,18,12);
        hit(12,170,w-24,30,()->action(Action.NEXT));
        fitted("SELECT "+(e.replaceAll?"в конец":"выделить всё"),150,192,18,6,w-168);
        float step=(bodyBottom-62-204)/5,cw=(w-32)/10;
        for(int i=0;i<e.count();i++){
            final int k=i;float x=16+(i%10)*cw,y=204+(i/10)*step;boolean focused=e.key==i;
            rect(x+1,y+1,cw-3,step-3,focused?10:0);
            char letter=e.character(i);String label=letter==' '?"·":String.valueOf(letter);
            text(label,x+(cw-width(label,24))/2,y+step/2+8,24,focused?1:7);
            hit(x,y,cw,step,()->{s.typeName(k);changed.run();invalidate();});
        }
        float y=bodyBottom-54,bw=(w-48)/3;
        button("X стереть",16,y,bw,42,false,()->action(Action.CONTEXT));
        button("Y Аа",24+bw,y,bw,42,false,()->action(Action.UNDO));
        button(e.replaceAll?"В конец":"Всё",32+bw*2,y,bw,42,false,()->action(Action.MENU));
        rect(0,bodyBottom,w,44,0);
        key(ok()+" буква",12,bodyBottom,10,()->action(Action.CONFIRM));
        key(back()+" отмена",w<500?124:180,bodyBottom,6,()->action(Action.CANCEL));
        key("START готово",w-144,bodyBottom,14,()->action(Action.TEST));
    }
    private void assetPicture(SpriteAsset a,float x,float y,float size){
        picture(new SpriteRegion(0,0,a.width,a.height),a,x,y,size);
    }
    private void moveChooser(){
        SpriteMove m=s.move;SpriteRegion r=m.scope,source=m.source(),target=m.phase==2?m.destination():source;
        text("ПЕРЕНЕСТИ ФРАГМЕНТ",16,169,18,14);
        fitted(m.phase==0?"1 / Выбери первый угол":m.phase==1?"2 / Выбери второй угол":"3 / Выбери новое место",16,198,22,7,w-32);
        float size=Math.min(w-32,bodyBottom-292),left=(w-size)/2,top=214;
        picture(r,null,m.preview(s.cart()),left,top,size);
        float cell=size/Math.max(r.width,r.height);if(cell>=1)cell=(int)cell;
        float sx=left+(size-r.width*cell)/2,sy=top+(size-r.height*cell)/2;
        if(m.phase==2)outline(sx+(source.x-r.x)*cell,sy+(source.y-r.y)*cell,source.width*cell,source.height*cell,14);
        if(m.phase>0)outline(sx+(target.x-r.x)*cell,sy+(target.y-r.y)*cell,target.width*cell,target.height*cell,10);
        else outline(sx+m.x*cell,sy+m.y*cell,cell,cell,10);
        final float fx=sx,fy=sy,fc=cell;
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
            final int px=x,py=y;hit(fx+x*fc,fy+y*fc,fc,fc,()->{m.point(px,py);changed.run();invalidate();});
        }
        float by=top+size;
        fitted(m.phase==2?source.width+" × "+source.height+" · сдвиг "+(target.x-source.x)+", "+(target.y-source.y):"Крестовина: пиксель · касание: позиция",16,by+23,18,10,w-32);
        fitted(m.phase==2?"Источник → цвет 0. Под рамкой — замена, включая 0.":"Выдели часть внутри открытого спрайта или области.",16,by+46,16,6,w-32);
        if(m.phase==2)fitted("Розовая: откуда · жёлтая: куда",16,by+67,16,13,w-32);
    }
    private void copyChooser(){
        SpriteRegion source=s.copySource,target=s.copyDestination();
        boolean confirm=s.mode==Mode.COPY_CONFIRM;
        text((s.copyAsset!=null?"ИЗ БИБЛИОТЕКИ / ":"КОПИЯ / ")+source.width+" × "+source.height,16,169,18,14);
        fitted(confirm?"2 / Проверь замену пикселей":"1 / Выбери место на листе",16,198,24,7,w-32);
        if(confirm){
            float size=Math.min(160,bodyBottom-330),gap=32,total=size*2+gap,left=(w-total)/2;
            text("БЫЛО",left,232,18,6);text("БУДЕТ",left+size+gap,232,18,10);
            copyPicture(target,left,246,size);
            if(s.copyAsset!=null)assetPicture(s.copyAsset,left+size+gap,246,size);else copyPicture(source,left+size+gap,246,size);
            float by=246+size;
            fitted(s.cart().empty(target)?"Нулевые пиксели тоже могут использоваться игрой.":"Пиксели в этом месте будут заменены.",16,by+29,18,10,w-32);
            fitted("Код и ссылки на спрайты остаются прежними.",16,by+54,18,6,w-32);
        }else{
            float cell=Math.max(1,(int)Math.min((w-32)/128,(bodyBottom-306)/64));
            float x=(w-cell*128)/2,y=218;
            for(int py=0;py<64;py++)for(int px=0;px<128;px++){
                int color=s.cart().sheetPixel(px,py);
                rect(x+px*cell,y+py*cell,cell,cell,color==0?((px/8+py/8)%2==0?0:1):color);
            }
            outline(x-2,y-2,128*cell+4,64*cell+4,13);
            if(s.copyAsset==null)outline(x+source.x*cell,y+source.y*cell,source.width*cell,source.height*cell,14);
            outline(x+target.x*cell,y+target.y*cell,target.width*cell,target.height*cell,s.copyOverlaps()?8:10);
            for(int ty=0;ty<8;ty++)for(int tx=0;tx<16;tx++){
                final int px=tx,py=ty;hit(x+tx*8*cell,y+ty*8*cell,8*cell,8*cell,()->{s.pointCopy(px,py);changed.run();invalidate();});
            }
            float by=y+64*cell;
            fitted(s.copyOverlaps()?"Рамки пересекаются — выбери другое место.":s.copyAsset!=null?s.copyAsset.title+" · жёлтая рамка: место вставки":"Розовая: источник · жёлтая: место копии",16,by+29,18,s.copyOverlaps()?8:7,w-32);
            fitted("Место: "+target.x+", "+target.y+" · шаг 8 пикселей",16,by+55,18,6,w-32);
            fitted("Выбор места ничего не меняет в картридже.",16,by+80,16,13,w-32);
        }
    }
    private void recolorChooser(){
        SpriteRegion r=s.selection();
        fitted("ЗАМЕНА ЦВЕТА · "+r.width+" × "+r.height,16,169,18,14,w-32);
        float size=Math.min(Math.min(120,(w-64)/2),bodyBottom-374),gap=32,left=(w-size*2-gap)/2;
        text("БЫЛО",left,197,18,6);text("БУДЕТ",left+size+gap,197,18,10);
        picture(r,null,s.cart(),left,208,size);picture(r,null,s.recolorPreview(),left+size+gap,208,size);
        float by=208+size,bw=(w-44)/2;
        button("Из #"+s.recolorFrom(),16,by+12,bw,40,s.recolorField()==0,()->{s.selectRecolorField(0);changed.run();invalidate();});
        button("В #"+s.recolorTo(),28+bw,by+12,bw,40,s.recolorField()==1,()->{s.selectRecolorField(1);changed.run();invalidate();});
        rect(16+bw-34,by+20,24,24,s.recolorFrom());outline(16+bw-34,by+20,24,24,13);
        rect(w-50,by+20,24,24,s.recolorTo());outline(w-50,by+20,24,24,13);
        int selected=s.recolorField()==0?s.recolorFrom():s.recolorTo();float cw=(w-32)/8;
        for(int i=0;i<16;i++){
            final int value=i;float x=16+i%8*cw,y=by+62+i/8*36;
            rect(x+3,y+3,cw-6,30,i);outline(x+3,y+3,cw-6,30,13);
            if(i==selected){outline(x,y,cw,36,7);outline(x+2,y+2,cw-4,32,0);}
            text(""+i,x+cw/2-width(""+i,16)/2,y+24,16,i==0||i==1||i==2||i==4||i==5||i==8?7:0);
            hit(x,y,cw,36,()->{s.chooseRecolor(value);changed.run();invalidate();});
        }
        String hint="Изменится пикселей: "+s.recolorCount()+" · ← → ↑ ↓ цвет";
        if(s.recolorFrom()==0||s.recolorTo()==0)hint="Пикселей: "+s.recolorCount()+" · 0 обычно прозрачный в игре";
        fitted(hint,16,by+158,16,6,w-32);
    }
    private void transformChooser(){
        SpriteRegion r=s.selection();
        fitted("ОТРАЗИТЬ / ПОВЕРНУТЬ",16,169,18,14,w-188);
        fitted(r.width+" × "+r.height+" · "+(s.transformOperation().ordinal()+1)+" / 4",w-156,169,18,6,140);
        button("<",16,188,44,44,false,()->action(Action.LEFT));
        rect(68,188,w-136,44,0);
        fitted(TRANSFORM_NAMES[s.transformOperation().ordinal()],80,216,22,10,w-160);
        button(">",w-60,188,44,44,false,()->action(Action.RIGHT));
        float size=Math.min(Math.min(160,(w-64)/2),bodyBottom-350),gap=32,left=(w-size*2-gap)/2;
        text("БЫЛО",left,266,18,6);fitted(s.transformAllowed()?"БУДЕТ":"НУЖЕН КВАДРАТ",left+size+gap,266,18,s.transformAllowed()?10:13,w-left-size-gap-16);
        picture(r,null,s.cart(),left,278,size);
        if(s.transformAllowed())picture(r,null,s.transformPreview(),left+size+gap,278,size);
        else{rect(left+size+gap,278,size,size,0);outline(left+size+gap,278,size,size,13);text("?",left+size+gap+size/2-8,278+size/2+12,32,13);}
        float by=278+size;
        fitted(s.transformAllowed()?"Изменится везде, где используется этот спрайт.":"Для 90° пока выбери квадратную область.",16,by+26,18,7,w-32);
        fitted(s.transformAllowed()?"После применения Y отменит эту правку.":"Отражения и 180° доступны для прямоугольника.",16,by+51,18,6,w-32);
    }
    private void copyPicture(SpriteRegion r,float x,float y,float size){
        picture(r,null,x,y,size);
    }
    private void picture(SpriteRegion r,SpriteAsset asset,float x,float y,float size){
        picture(r,asset,s.cart(),x,y,size);
    }
    private void picture(SpriteRegion r,SpriteAsset asset,WorkshopCartridge cart,float x,float y,float size){
        rect(x,y,size,size,0);
        float cell=size/Math.max(r.width,r.height);
        if(cell>=1)cell=(int)cell;
        float sx=x+(size-r.width*cell)/2,sy=y+(size-r.height*cell)/2;
        for(int py=0;py<r.height;py++)for(int px=0;px<r.width;px++){
            int color=asset==null?cart.pixel(r,px,py):asset.pixel(px,py);
            rect(sx+px*cell,sy+py*cell,cell,cell,color==0?((px+py)%2==0?0:1):color);
        }
        outline(x,y,size,size,13);
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
        float dw=Math.min(480,w-32),dx=(w-dw)/2,dh=s.mode==Mode.HERO?400:s.mode==Mode.DRAW_TOOLS?444:s.mode==Mode.MENU?404:280,dy=Math.max(16,(h-dh)/2);
        rect(dx+6,dy+6,dw,dh,0);rect(dx,dy,dw,dh,1);outline(dx,dy,dw,dh,s.mode==Mode.ERROR?8:14);
        if(s.mode==Mode.HERO){
            HeroBinding hero=s.heroDraft;SpriteRegion r=hero.image;
            text("Новый спрайт героя",dx+20,dy+36,26,14);
            text("Спрайт "+r.width+" × "+r.height+" · тело "+hero.width+" × "+hero.height,dx+20,dy+67,18,7);
            rect(dx+16,dy+82,dw-32,146,0);
            float cell=Math.max(1,(int)Math.min((dw-64)/r.width,128f/r.height));
            float sx=dx+(dw-r.width*cell)/2,sy=dy+88+(128-r.height*cell)/2;
            float floor=sy+(hero.top+hero.height)*cell;
            rect(dx+24,floor,dw-48,4,11);
            regionSprite(r,sx,sy,cell);
            outline(sx+hero.left*cell,sy+hero.top*cell,hero.width*cell,hero.height*cell,10);
            fitted("Жёлтая рамка — столкновения.",dx+20,dy+256,18,10,dw-40);
            fitted("Пустые поля не держат героя над землёй.",dx+20,dy+282,18,7,dw-40);
            fitted("Рамка обновится при назначении спрайта.",dx+20,dy+308,16,6,dw-40);
            button(ok()+" Назначить",dx+16,dy+336,(dw-44)/2,48,true,()->action(Action.CONFIRM));
            button(back()+" Отмена",dx+28+(dw-44)/2,dy+336,(dw-44)/2,48,false,()->action(Action.CANCEL));
        }else if(s.mode==Mode.DRAW_TOOLS){
            fitted("Инструменты спрайта",dx+20,dy+36,26,14,dw-104);
            text((s.drawToolCursor+1)+" / "+WorkshopSession.drawMenuCount(),dx+dw-70,dy+34,18,6);
            int first=Math.max(0,s.drawToolCursor-5);
            for(int row=0;row<6;row++){final int item=first+row;
                button(item==WorkshopSession.moveMenuIndex()?"Перенести фрагмент":item==6?"Заменить цвет":item==5?"Отразить / повернуть":DRAW_NAMES[WorkshopSession.drawMenuTool(item).ordinal()],dx+16,dy+54+row*50,dw-32,44,s.drawToolCursor==item,()->{s.chooseDrawTool(item);changed.run();invalidate();});
            }
            fitted(s.drawToolCursor==WorkshopSession.moveMenuIndex()?"Выделить пиксели и выбрать новое место":s.drawToolCursor==6?"Заменить цвет во всём выделении":s.drawToolCursor==5?"Сравнить варианты перед применением":DRAW_HELP[WorkshopSession.drawMenuTool(s.drawToolCursor).ordinal()],dx+20,dy+389,18,7,dw-40);
            text(ok()+" выбрать · "+back()+" назад",dx+20,dy+426,18,6);
            hit(dx+12,dy+398,(dw-32)/2,44,()->action(Action.CONFIRM));
            hit(dx+20+(dw-32)/2,dy+398,(dw-32)/2,44,()->action(Action.CANCEL));
        }else if(s.mode==Mode.MENU) {
            text("Мастерская",dx+20,dy+36,26,14);
            float historyWidth=(dw-40)/2;
            for(int i=0;i<2;i++){
                final boolean returning=i==1;int count=returning?s.redoCount():s.undoCount();
                float x=dx+16+i*(historyWidth+8);boolean selected=s.menuItem==0&&s.menuRedo==returning;
                rect(x,dy+54,historyWidth,44,selected?(count>0?10:5):0);
                fitted((returning?"Вернуть":"Отменить")+" · "+count,x+10,dy+83,20,selected?(count>0?1:7):count>0?7:13,historyWidth-20);
                hit(x,dy+54,historyWidth,44,()->{s.menuItem=0;s.menuRedo=returning;action(Action.CONFIRM);});
            }
            String[] labels={"",s.swapAB?"B выбор / A назад":"A выбор / B назад",s.tool==3?"Инструменты карты":"Как это работает","Вернуться к проекту","Мои игры","Ресурсы · библиотека"};
            for(int i=1;i<6;i++){final int n=i;button(labels[i],dx+16,dy+54+i*48,dw-32,44,s.menuItem==i,()->{s.menuItem=n;action(Action.CONFIRM);});}
            text("Y отменить · R2 вернуть",dx+20,dy+361,16,6);
            text("История — в текущем сеансе",dx+20,dy+386,16,13);
        } else if(s.mode==Mode.HELP) {
            if(s.tool==3){
                text("Карта из тайлов",dx+20,dy+40,26,14);
                fitted("Каждая клетка — спрайт 8×8.",dx+20,dy+80,18,7,dw-40);
                fitted("Тайл 0 стирает клетку.",dx+20,dy+108,18,7,dw-40);
                fitted("Строки 32–63 общие со спрайтами:",dx+20,dy+136,18,6,dw-40);
                fitted("пока их можно только смотреть.",dx+20,dy+162,18,6,dw-40);
                fitted("В игре: map() в _draw после cls().",dx+20,dy+188,18,7,dw-40);
                button(ok()+" К коду",dx+16,dy+210,(dw-44)/2,48,true,()->action(Action.CONFIRM));
                button(back()+" Назад",dx+28+(dw-44)/2,dy+210,(dw-44)/2,48,false,()->action(Action.CANCEL));return;
            }
            if(!s.cart().hasHero()){
                text("Ресурсы и игра",dx+20,dy+40,26,14);
                fitted("Спрайт хранит пиксели.",dx+20,dy+80,20,7,dw-40);
                fitted("Код решает, где и как их показать.",dx+20,dy+108,20,7,dw-40);
                fitted("Один спрайт можно рисовать много раз.",dx+20,dy+136,18,7,dw-40);
                fitted("START: проверь изменения в игре.",dx+20,dy+177,18,6,dw-40);
                button(ok()+(s.tool==2?" К спрайтам":" К коду"),dx+16,dy+210,(dw-44)/2,48,true,()->action(Action.CONFIRM));
                button(back()+" Назад",dx+28+(dw-44)/2,dy+210,(dw-44)/2,48,false,()->action(Action.CANCEL));
                return;
            }
            if(s.tool==2){
                text("Спрайты и герой",dx+20,dy+40,26,14);
                text("Спрайт можно открыть и изменить.",dx+20,dy+80,20,7);
                text("Метка «Герой» показывает, какой",dx+20,dy+108,20,7);
                text("из них сейчас используется в игре.",dx+20,dy+136,20,7);
                text("Копия сохраняет исходный спрайт.",dx+20,dy+177,18,6);
                button(ok()+" К спрайтам",dx+16,dy+210,(dw-44)/2,48,true,()->action(Action.CONFIRM));
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
            if(s.mode==Mode.DRAW_TOOLS&&Math.abs(y-downY)>24){
                int steps=Math.max(1,Math.round(Math.abs(downY-y)/50));
                for(int i=0;i<steps;i++)s.act(downY>y?Action.DOWN:Action.UP);
                changed.run();invalidate();return true;
            }
            if(s.tool==1&&s.mode==Mode.NAVIGATE&&Math.abs(y-downY)>24) {
                int steps=Math.round((downY-y)/24);for(int i=0;i<Math.abs(steps);i++)s.act(steps>0?Action.DOWN:Action.UP);
                changed.run();invalidate();return true;
            }
            if(Math.abs(x-downX)>20||Math.abs(y-downY)>20)return true;
            if(mapArea!=null&&mapArea.contains(x,y)&&s.tool==3&&s.mode==Mode.NAVIGATE){
                int tx=(int)((x-mapArea.left)*16/mapArea.width()),ty=(int)((y-mapArea.top)*16/mapArea.height());
                if(s.mapEditor.picking)s.mapEditor.choice=ty*16+tx;
                else{s.mapEditor.point(mapLeft+tx,mapTop+ty);}
                changed.run();invalidate();performClick();return true;
            }
            if(placementArea!=null&&placementArea.contains(x,y)&&s.mode==Mode.CODE&&s.codeDraft.panel==LuaDraft.Panel.SPRITE){
                s.codeDraft.placement.point((int)((x-placementArea.left)*128/placementArea.width()),(int)((y-placementArea.top)*128/placementArea.height()));
                changed.run();invalidate();performClick();return true;
            }
            if(placementArea!=null&&placementArea.contains(x,y)&&s.mode==Mode.CODE&&s.codeDraft.panel==LuaDraft.Panel.ANIMATION&&s.codeDraft.animation.picker!=null){
                s.codeDraft.animation.picker.point((int)((x-placementArea.left)*128/placementArea.width()),(int)((y-placementArea.top)*128/placementArea.height()));
                changed.run();invalidate();performClick();return true;
            }
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
