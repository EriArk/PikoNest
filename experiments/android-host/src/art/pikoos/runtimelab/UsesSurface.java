package art.pikoos.runtimelab;

import android.graphics.*;
import android.os.SystemClock;
import android.view.MotionEvent;
import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.ArrayList;

/** Shared controller forms for ordinary game uses. The session owns every edit. */
final class UsesSurface {
    private final WorkshopView view;
    private final WorkshopSession s;
    private final String project;
    private final Paint paint=new Paint();
    private final Bitmap bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
    private final ArrayList<Hit> hits=new ArrayList<>();
    private Canvas c; private float w,h,bottom,scale,downX,downY;
    private long clock;
    private int errorScroll;
    private GameSketch layerReview;
    private GameUses layerReviewOwner;
    private RectF pickerArea;
    private SpritePlacement picker;
    private static final int[] COLORS=PicoPalette.COLORS;
    private static final class Hit {
        final RectF area;final Runnable run;
        Hit(float x,float y,float w,float h,Runnable r){area=new RectF(x,y,x+w,y+h);run=r;}
    }
    UsesSurface(WorkshopView view,WorkshopSession s,String project){
        this.view=view;this.s=s;this.project=project;PixelText.configure(view.getContext(),paint);
        paint.setAntiAlias(false);paint.setFilterBitmap(false);
    }
    private void act(Action a){view.action(a);}
    boolean handle(Action a){
        if(s.mode!=WorkshopSession.Mode.ERROR)return false;
        if(a==Action.UP||a==Action.DOWN){errorScroll=Math.max(0,errorScroll+(a==Action.UP?-1:1));view.invalidate();return true;}
        errorScroll=0;return false;
    }
    private String yes(){return s.swapAB?"B":"A";}
    private String back(){return s.swapAB?"A":"B";}
    private void box(float x,float y,float width,float height,int color){paint.setColor(COLORS[color]);c.drawRect(x,y,x+width,y+height,paint);}
    private void text(String t,float x,float y,int size,int color){paint.setTextSize(PixelText.size(size));paint.setColor(COLORS[color]);c.drawText(t,x,y,paint);}
    private void fit(String t,float x,float y,int size,int color,float max){
        paint.setTextSize(PixelText.size(size));while(t.length()>1&&paint.measureText(t)>max)t=t.substring(0,t.length()-2)+"…";text(t,x,y,size,color);
    }
    private void hit(float x,float y,float width,float height,Runnable r){hits.add(new Hit(x,y,width,height,r));}
    private void row(String label,float y,boolean selected,Runnable r){
        if(selected){box(12,y,w-24,44,2);box(12,y,4,44,10);}
        fit(label,24,y+29,20,selected?10:7,w-48);hit(12,y,w-24,44,r);
    }
    private void scroll(int selected,int total,int count,float top){
        if(total<=count)return;float height=count*44;
        box(w-7,top,2,height,13);box(w-8,top+(height-12)*selected/(total-1),4,12,14);
    }
    private void hint(String label,int slot,Action action){
        float x=(slot%2)*w/2,y=bottom+(slot/2)*44;
        paint.setTextSize(PixelText.size(18));int split=label.indexOf(' ');
        if(split>0&&paint.measureText(label)>w/2-24){
            text(label.substring(0,split),x+16,y+19,18,14);
            fit(label.substring(split+1),x+16,y+40,18,6,w/2-24);
        }else text(label,x+16,y+28,18,slot==0?14:6);
        hit(x,y,w/2,44,()->act(action));
    }
    private void footer(String primary,String secondary,String extra,Action extraAction,boolean test){
        box(0,bottom,w,1,13);hint(yes()+" "+primary,0,Action.CONFIRM);hint(back()+" "+secondary,1,Action.CANCEL);
        if(extra!=null)hint(extra,2,extraAction);if(test)hint("Start Test",3,Action.TEST);
    }
    private void editingFooter(){
        footer("Done","Revert",null,null,false);hint("← Decrease",2,Action.LEFT);hint("→ Increase",3,Action.RIGHT);
    }
    void draw(Canvas canvas,float scale,float width,float height){
        c=canvas;this.scale=scale;w=width;h=height;bottom=h-88;hits.clear();pickerArea=null;picker=null;
        box(0,0,w,h,1);GameUses g=s.uses;
        fit(project,16,25,18,6,w-136);fit(g.screen==GameUses.Screen.LIST||g.screen==GameUses.Screen.MENU?"Saved":"Draft",w-112,25,18,10,96);
        String title=g.background!=null?"Background":g.camera!=null?"Camera":g.animation!=null?"Animation":g.screen==GameUses.Screen.MENU?"Game actions":"In the game";
        text(title,16,60,26,7);box(16,72,32,3,14);
        if(g.screen!=GameUses.Screen.REVIEW){layerReview=null;layerReviewOwner=null;}
        if(s.mode==WorkshopSession.Mode.ERROR){error();return;}
        if(g.screen==GameUses.Screen.PICK){resource(g.picker,"Sprite region");return;}
        if(g.animation!=null&&g.animation.picker!=null){resource(g.animation.picker,g.animation.picker.phase==2?"Position in game":"Frame "+(g.animation.selected+1)+" region");return;}
        if(g.background!=null&&g.background.picker!=null){resource(g.background.picker,"Background strip");return;}
        if(g.screen==GameUses.Screen.REVIEW){review();return;}
        if(g.background!=null){background();return;}
        if(g.camera!=null){camera();return;}
        if(g.animation!=null){animation();return;}
        if(g.screen==GameUses.Screen.FORM){placement();return;}
        if(g.screen==GameUses.Screen.MENU){
            String[] items={"Add sprite","Add map","Duplicate selection","Remove use","Open Lua (optional)","Back to list","Add animation","Animate selected sprite","Camera for selected uses","Screen space from here","Add background before selection","Move layer backward","Move layer forward"};
            fit("Choose an action",16,101,18,6,w-32);int count=Math.max(1,(int)((bottom-120)/44)),first=Math.max(0,g.menu-count+1);
            for(int n=0;n<count&&first+n<items.length;n++){final int at=first+n;row(items[at],116+n*44,g.menu==at,()->{g.menu=at;act(Action.CONFIRM);});}
            footer("Choose","Back","↑↓ Browse",Action.DOWN,false);return;
        }
        fit(g.entries.size()+" uses · draw order",16,101,18,6,w-32);
        if(!g.blocked.isEmpty()){wrap("This list cannot safely edit this draw code yet. Your source is preserved. Open Lua from Actions.",16,148,w-32,20,7,5);}
        else if(g.entries.isEmpty()){wrap("Add a sprite, map, animation or background from Actions, then try it in Test.",16,156,w-32,20,7,5);}
        int count=Math.max(1,(int)((bottom-122)/60)),first=Math.max(0,g.index-count+1);
        for(int n=0;n<count&&first+n<g.entries.size();n++){
            final int at=first+n;GameUses.Entry e=g.entries.get(at);float y=116+n*60;
            if(at==g.index){box(12,y,w-24,56,2);box(12,y,4,56,10);}
            fit((at+1)+"  "+entryTitle(e),24,y+24,20,at==g.index?10:7,w-48);
            String detail=e.isBackground()?"Y "+e.call.form.value(4)+" · "+e.call.form.value(5)+" px/s · "+(e.call.form.value(7).equals("true")?"visible":"hidden"):e.isCamera()?"Applies to following uses":"X "+e.x()+"   Y "+e.y()+"   · "+(e.view==null||e.view.form.item().id.equals("camera_reset")?"screen":"world");
            fit(detail,24,y+47,16,6,w-48);hit(12,y,w-24,56,()->{g.index=at;act(Action.CONFIRM);});
        }
        footer(g.entries.isEmpty()?"Add sprite":"Edit","Workshop","Select Actions",Action.MENU,false);hint("Start Test",3,Action.TEST);
    }
    private static String cameraTitle(CameraUse camera){return cameraTitle(camera.form);}
    private static String cameraTitle(LuaInsert form){String id=form.item().id;return id.equals("camera_reset")?"Screen space":id.equals("camera_follow")?"Follow a point":id.equals("camera_rooms")?"Room by room":"Fixed offset";}
    private static String entryTitle(GameUses.Entry e){return e.isBackground()?"Background · "+e.call.form.value(2)+" × "+e.call.form.value(3):e.isCamera()?"Camera · "+cameraTitle(e.call.form):e.animation!=null?"Animation · "+e.animation.initial.count()+" frames":e.kind().equals("map")?"Map":"Sprite";}
    private void background(){
        GameUses g=s.uses;BackgroundUse b=g.background;boolean editing=g.editingField();long now=SystemClock.uptimeMillis();
        if(b.playing&&view.getWindowVisibility()==android.view.View.VISIBLE){if(clock!=0)b.advance((int)Math.min(100,now-clock));clock=now;view.postInvalidateDelayed(33);}else clock=0;
        fit(g.creating?(g.copyAfter?"Copy after selected layer":g.current()==null?"First use in the game":"Before use "+(g.index+1)+" · behind later uses"):"Repeats horizontally · keeps the camera",16,101,18,6,w-32);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){int color=b.pixel(s.cart(),x,y);bitmap.setPixel(x,y,COLORS[color==0?1:color]);}
        c.drawBitmap(bitmap,null,new RectF(16,116,112,212),paint);box(16,212,96,1,13);
        fit(b.playing?"Playing sketch":"Paused sketch",128,139,18,14,w-144);
        fit("Camera X: "+b.previewCameraX,128,166,18,6,w-144);
        fit("0 = screen · 1 = world",128,193,18,6,w-144);
        SpriteRegion r=b.region();String[] rows={"Region: "+r.width+" × "+r.height+" at "+r.x+", "+r.y,"Screen Y: "+b.form.value(4),"Speed: "+b.form.value(5)+" px/s","Parallax: "+b.form.value(6),"Visible: "+(b.form.value(7).equals("true")?"Yes":"No"),"Sketch camera X: "+b.previewCameraX,"Preview changes"};
        int count=Math.max(1,(int)((bottom-258)/44)),first=Math.max(0,b.field-count+1);
        for(int n=0;n<count&&first+n<rows.length;n++){final int at=first+n;row((editing&&at==b.field?"< ":"")+rows[at]+(editing&&at==b.field?" >":""),224+n*44,b.field==at,()->{if(!editing||b.field==at){b.field=at;act(Action.CONFIRM);}});}
        scroll(b.field,rows.length,count,224);
        fit(b.field==5?"Sketch camera is not saved to the game":r.sharesMap()?"This strip shares memory with the map":"Region refers to pixels in this cartridge",16,bottom-14,16,6,w-32);
        if(editing)editingFooter();else footer(b.field==6?"Preview":b.field==0?"Choose":"Edit","Cancel",b.playing?"X Pause":"X Play sketch",Action.CONTEXT,true);
    }
    private void camera(){
        GameUses g=s.uses;CameraUse a=g.camera;boolean editing=g.editingField();
        fit(editing?"Editing field · ←→ adjust":"Choose a field to edit",16,101,18,6,w-32);
        int count=Math.max(1,(int)((bottom-172)/44)),first=Math.max(0,a.field-count+1);
        String id=a.form.item().id;
        for(int n=0;n<count&&first+n<a.rows();n++){
            final int at=first+n;String label;
            if(at==0)label="Mode: "+cameraTitle(a);
            else if(at<=a.form.item().fields.length){String key=at==1?"X":at==2?"Y":(at==3?"Width":"Height")+(id.equals("camera_rooms")?" (rooms)":" (tiles)");label=key+": "+a.form.value(at-1);}
            else if(a.ranged()&&at==a.rows()-2)label="Through use: "+(a.rangeEnd+1);
            else label="Preview changes";
            if(editing&&at==a.field)label="< "+label+" >";
            row(label,116+n*44,a.field==at,()->{if(!editing||a.field==at){a.field=at;act(Action.CONFIRM);}});
        }
        scroll(a.field,a.rows(),count,116);
        if(editing&&(a.field==1||a.field==2)){fit("X Choose project value",16,bottom-27,18,14,w-32);hit(12,bottom-52,w-24,48,()->act(Action.CONTEXT));}
        else fit(a.ranged()?"Uses "+(g.index+1)+"–"+(a.rangeEnd+1)+"; then restore camera":"Until the next camera setting",16,bottom-27,18,6,w-32);
        if(editing)editingFooter();else footer(a.field==a.rows()-1?"Preview":"Edit","Cancel","Select Preview",Action.MENU,true);
    }
    private void animation(){
        GameUses g=s.uses;SpriteAnimation a=g.animation;boolean editing=g.editingField();long now=SystemClock.uptimeMillis();
        if(a.playing&&view.getWindowVisibility()==android.view.View.VISIBLE){if(clock!=0)a.advance((int)Math.min(100,now-clock));clock=now;view.postInvalidateDelayed(16);}else clock=0;
        fit("Frame "+(a.selected+1)+" / "+a.count()+" · "+a.duration()+" ms total",16,101,18,6,w-32);
        frame(a.frame(a.visibleFrame()).region,16,116,88,a.previewWidth(),a.previewHeight());
        fit(a.playing?"Playing":"Paused",120,139,18,14,w-136);
        int first=Math.max(0,a.selected-1),thumbs=Math.max(1,Math.min(5,(int)((w-136)/52)));
        for(int n=0;n<thumbs&&first+n<a.count();n++){final int at=first+n;float x=120+n*52;frame(a.frame(at).region,x,152,44);if(at==a.selected)box(x,198,44,3,10);hit(x,152,44,48,()->{if(!editing){a.select(at);act(Action.CHECK);}});}
        SpriteRegion r=a.frame(a.selected).region;
        String[] rows={"Frame: "+(a.selected+1),"Region: "+r.width+" × "+r.height+" at "+r.x+", "+r.y,"Duration: "+a.frame(a.selected).millis+" ms","Duplicate frame","Order: "+(a.selected+1)+" / "+a.count(),"Remove frame","Playback: "+(a.loop?"Loop":"Once"),"Position: "+a.x+", "+a.y,"Preview changes"};
        int count=Math.max(1,(int)((bottom-224)/44)),start=Math.max(0,a.field-count+1);
        for(int n=0;n<count&&start+n<rows.length;n++){final int at=start+n;row((editing&&at==a.field?"< ":"")+rows[at]+(editing&&at==a.field?" >":""),216+n*44,a.field==at,()->{if(!editing||a.field==at){a.field=at;act(Action.CONFIRM);}});}
        scroll(a.field,9,count,216);
        if(editing)editingFooter();else footer(a.field==8?"Preview":"Choose","Cancel",a.playing?"X Pause":"X Play preview",Action.CONTEXT,true);
    }
    private void frame(SpriteRegion r,float x,float y,float size){frame(r,x,y,size,r.width,r.height);}
    private void frame(SpriteRegion r,float x,float y,float size,int canvasWidth,int canvasHeight){
        for(int yy=0;yy<r.height;yy++)for(int xx=0;xx<r.width;xx++)bitmap.setPixel(xx,yy,COLORS[s.cart().pixel(r,xx,yy)]);
        box(x,y,size,size,0);float zoom=size/Math.max(canvasWidth,canvasHeight),ww=r.width*zoom,hh=r.height*zoom;
        c.drawBitmap(bitmap,new Rect(0,0,r.width,r.height),new RectF(x+(size-canvasWidth*zoom)/2,y+(size-canvasHeight*zoom)/2,x+(size-canvasWidth*zoom)/2+ww,y+(size-canvasHeight*zoom)/2+hh),paint);
    }
    private void resource(SpritePlacement v,String title){
        clock=0;picker=v;fit(title+" · "+(v.phase==0?"first corner":v.phase==1?"second corner":"position"),16,101,18,6,w-32);
        float size=Math.min(256,Math.min(w-32,bottom-184)),left=(w-size)/2,top=116;SpriteRegion r=v.source();
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)bitmap.setPixel(x,y,COLORS[v.phase<2?s.cart().sheetPixel(x,y):v.pixel(s.cart(),x,y)]);
        pickerArea=new RectF(left,top,left+size,top+size);c.drawBitmap(bitmap,null,pickerArea,paint);
        c.save();c.clipRect(pickerArea);float unit=size/128,x=left+(v.phase<2?r.x:v.x)*unit,y=top+(v.phase<2?r.y:v.y)*unit;
        box(x,y,r.width*unit,2,10);box(x,y+r.height*unit-2,r.width*unit,2,10);box(x,y,2,r.height*unit,10);box(x+r.width*unit-2,y,2,r.height*unit,10);c.restore();
        fit(v.phase<2?r.width+" × "+r.height+" px at "+r.x+", "+r.y:"X "+v.x+" · Y "+v.y,16,bottom-47,18,10,w-32);
        fit(v.phase<2&&r.sharesMap()?"Shared with lower map rows":"Choose with D-pad or touch",16,bottom-21,16,6,w-32);
        footer(v.phase==0?"Next corner":"Use selection","Back","X Step "+v.step+" px",Action.CONTEXT,false);
    }
    private void placement(){
        GameUses g=s.uses;fit((g.kind.equals("map")?"Map":"Sprite")+" · region and position",16,101,18,6,w-32);
        String[] labels=g.kind.equals("map")?new String[]{"Map X (tiles)","Map Y (tiles)","Game X","Game Y","Width (tiles)","Height (tiles)"}:g.kind.equals("spr")?new String[]{"Tile","Game X","Game Y"}:new String[]{"Sheet X","Sheet Y","Width (px)","Height (px)","Game X","Game Y"};
        int count=Math.max(1,(int)((bottom-160)/44)),first=Math.max(0,g.field-count+1);
        for(int n=0;n<count&&first+n<labels.length;n++){final int at=first+n;row(labels[at]+": "+g.values[at],116+n*44,g.field==at,()->{g.field=at;act(Action.CHECK);});}
        fit("←→ Adjust · L/R ±8",16,bottom-22,18,6,w-32);
        footer("Preview","Cancel",g.kind.equals("map")?null:"X Choose region",Action.CONTEXT,false);
    }
    private void review(){
        clock=0;GameUses g=s.uses;fit(g.deleting?"Remove this use?":g.layerMove<0?"Move layer backward?":g.layerMove>0?"Move layer forward?":"Preview changes",16,101,20,10,w-32);
        float size=Math.min(256,Math.min(w-32,bottom-208)),left=(w-size)/2,top=116;
        boolean dynamic=false;
        if(g.background!=null){
            if(layerReview==null||layerReviewOwner!=g){layerReview=new GameSketch(g.proposal().candidate(s.cart()),0);layerReviewOwner=g;}
            dynamic=layerReview.dynamic;for(int y=0;y<128;y++)for(int x=0;x<128;x++)bitmap.setPixel(x,y,COLORS[layerReview.pixels[y*128+x]]);
        }else if(g.camera!=null){int[] pixels=g.camera.preview(g.deleting);dynamic=g.camera.dynamicPreview;for(int y=0;y<128;y++)for(int x=0;x<128;x++)bitmap.setPixel(x,y,COLORS[pixels[y*128+x]]);}
        else for(int y=0;y<128;y++)for(int x=0;x<128;x++)bitmap.setPixel(x,y,COLORS[g.deleting?1:g.pixel(s.cart(),x-g.x(),y-g.y())]);
        c.drawBitmap(bitmap,null,new RectF(left,top,left+size,top+size),paint);
        box(left-1,top-1,size+2,1,13);box(left-1,top+size,size+2,1,13);
        box(left-1,top,1,size,13);box(left+size,top,1,size,13);
        String caption=dynamic?"Dynamic positions need Test":"Sketch only · Test for gameplay";
        fit(caption,16,bottom-69,16,6,w-32);
        fit(g.deleting?"Sprite-sheet and map data stay intact":g.background!=null?"Scene at start · camera restored":g.camera!=null?cameraTitle(g.camera):g.animation!=null?g.animation.count()+" frames · "+(g.animation.loop?"loop":"once")+" · from game start":"Position: "+g.x()+", "+g.y(),16,bottom-44,18,7,w-32);
        fit("Apply saves one undo step",16,bottom-20,16,6,w-32);
        footer(g.deleting?"Remove":"Apply",g.deleting||g.layerMove!=0?"Cancel":"Edit",null,null,true);
    }
    private void error(){
        fit("Could not complete this action",16,101,18,10,w-32);
        ArrayList<String> lines=lines(s.error,w-32,18);int count=Math.max(1,(int)((bottom-172)/25));errorScroll=Math.min(errorScroll,Math.max(0,lines.size()-count));
        for(int n=0;n<count&&errorScroll+n<lines.size();n++)text(lines.get(errorScroll+n),16,142+n*25,18,7);
        fit("Your saved project is unchanged.",16,bottom-24,18,6,w-32);
        footer("Return","Back",lines.size()>count?"↑↓ Read more":null,Action.DOWN,false);
    }
    private ArrayList<String> lines(String value,float max,int size){
        ArrayList<String> out=new ArrayList<>();paint.setTextSize(PixelText.size(size));
        for(String line:value.split("\n",-1)){while(!line.isEmpty()){int n=line.length();while(n>1&&paint.measureText(line.substring(0,n))>max)n--;if(n<line.length()){int space=line.lastIndexOf(' ',n);if(space>0)n=space;}out.add(line.substring(0,n));line=line.substring(n).trim();}}
        return out;
    }
    private void wrap(String value,float x,float y,float max,int size,int color,int count){ArrayList<String> rows=lines(value,max,size);for(int i=0;i<Math.min(count,rows.size());i++)text(rows.get(i),x,y+i*28,size,color);}
    boolean touch(MotionEvent event){
        float x=event.getX()/scale,y=event.getY()/scale;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){
            if(Math.abs(y-downY)>24){int count=Math.max(1,Math.round(Math.abs(y-downY)/44));for(int n=0;n<count;n++)act(y<downY?Action.DOWN:Action.UP);return true;}
            if(Math.abs(x-downX)>24)return true;
            if(pickerArea!=null&&pickerArea.contains(x,y)){picker.point((int)((x-pickerArea.left)*128/pickerArea.width()),(int)((y-pickerArea.top)*128/pickerArea.height()));act(Action.CHECK);return true;}
            for(int n=hits.size()-1;n>=0;n--)if(hits.get(n).area.contains(x,y)){hits.get(n).run.run();break;}
            view.performClick();
        }return true;
    }
}
