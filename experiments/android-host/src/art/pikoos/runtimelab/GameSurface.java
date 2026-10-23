package art.pikoos.runtimelab;

import android.graphics.*;
import android.view.MotionEvent;
import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.ArrayList;

/** Native game canvas and contextual actions. Portable GameCanvas owns the draft. */
final class GameSurface {
    private final WorkshopView view;
    private final WorkshopSession session;
    private final String project;
    private final Paint paint=new Paint();
    private final Bitmap bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
    private final Bitmap thumbnail=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
    private final ArrayList<Hit> hits=new ArrayList<>();
    private Canvas canvas;
    private float width,height,scale;
    private RectF imageArea;
    private boolean painting;
    private int lastX,lastY,pointer;
    private static final class Hit {final RectF area;final Runnable run;Hit(float x,float y,float w,float h,Runnable r){area=new RectF(x,y,x+w,y+h);run=r;}}
    GameSurface(WorkshopView v,WorkshopSession s,String title){view=v;session=s;project=title;PixelText.configure(v.getContext(),paint);paint.setAntiAlias(false);paint.setFilterBitmap(false);}
    private GameCanvas game(){return session.game;}
    private void act(Action a){view.action(a);}
    private void changed(){view.gameChanged();}
    private void rect(float x,float y,float w,float h,int col){paint.setColor(PicoPalette.COLORS[col]);canvas.drawRect(x,y,x+w,y+h,paint);}
    private void text(String str,float x,float y,int size,int col,float max){paint.setTextSize(PixelText.size(size));while(str.length()>1&&paint.measureText(str)>max)str=str.substring(0,str.length()-2)+"…";paint.setColor(PicoPalette.COLORS[col]);canvas.drawText(str,x,y,paint);}
    private void button(String str,float x,float y,float w,float h,boolean focus,Runnable run){rect(x,y,w,h,focus?2:1);if(focus)rect(x,y,3,h,10);int split=str.indexOf('\n');
        if(split<0)text(str,x+12,y+h/2+7,20,focus?10:7,w-24);
        else{text(str.substring(0,split),x+12,y+17,14,6,w-24);text(str.substring(split+1),x+12,y+39,20,focus?10:7,w-24);}hits.add(new Hit(x,y,w,h,run));}
    private void outline(float x,float y,float w,float h,int col){rect(x,y,w,1,col);rect(x,y+h-1,w,1,col);rect(x,y,1,h,col);rect(x+w-1,y,1,h,col);}
    private void footer(String primary,Action action,String secondary,Action back){
        float y=height-88;rect(0,y,width,88,1);rect(16,y,width-32,1,13);
        button(primary,8,y+4,width/2-12,40,true,()->act(action));button(secondary,width/2+4,y+4,width/2-12,40,false,()->act(back));
        button(game().editingField?"Left  Decrease":"Start  Test",8,y+44,width/2-12,40,false,()->act(game().editingField?Action.LEFT:Action.TEST));
        String extra=game().editingField?"Right  Increase":game().screen==GameCanvas.Screen.NAME?"X  Erase":game().screen==GameCanvas.Screen.DRAW?"X  Color":game().screen==GameCanvas.Screen.PLACE?"D-pad  Move":"D-pad  List";
        button(extra,width/2+4,y+44,width/2-12,40,false,()->{if(game().editingField)act(Action.RIGHT);else if(game().screen==GameCanvas.Screen.DRAW||game().screen==GameCanvas.Screen.NAME)act(Action.CONTEXT);});
    }
    void draw(Canvas c,float s,float w,float h){
        canvas=c;scale=s;width=w;height=h;hits.clear();imageArea=null;GameCanvas g=game();
        rect(0,0,w,h,1);text("PikoNest",16,30,24,14,w-144);text(g.draft()?"Draft":"Saved",w-108,28,18,g.draft()?10:6,94);
        text(project,160,28,16,6,w-280);
        text(g.title(),16,64,24,7,w-32);
        String yes=session.swapAB?"B":"A",back=session.swapAB?"A":"B";
        if(session.mode==WorkshopSession.Mode.ERROR){
            text("Could not complete this action",16,112,22,9,w-32);
            wrap(session.error,16,150,w-32,Math.max(3,(int)((h-250)/26)));
            footer(yes+"  Return",Action.CONFIRM,back+"  Return",Action.CANCEL);return;
        }
        if(g.screen==GameCanvas.Screen.NAME){nameEditor(yes,back);return;}
        boolean draw=g.screen==GameCanvas.Screen.DRAW||g.screen==GameCanvas.Screen.DRAW_MENU||g.screen==GameCanvas.Screen.PALETTE;
        boolean wide=w>=540;float top=104,bottom=h-104,side;
        boolean contextual=g.screen==GameCanvas.Screen.BEHAVIOR||g.screen==GameCanvas.Screen.RELATION||g.screen==GameCanvas.Screen.ACTIONS||g.screen==GameCanvas.Screen.SETTINGS||g.screen==GameCanvas.Screen.CHOICE||g.screen==GameCanvas.Screen.GOAL||g.screen==GameCanvas.Screen.OVERVIEW;
        if(!draw)text("Starting layout",16,87,14,6,w-32);
        String[] rows=g.rows();boolean menu=rows.length>0;
        float listX=16,listY=top,listW=w-32;
        if(wide){side=Math.min(bottom-top,w*.55f-24);listX=side+40;listW=w-listX-16;}
        else{side=Math.min(w-32,menu?Math.max(104,(bottom-top)*.49f):bottom-top-24);if(contextual)side=Math.min(side,(w-64)*.5f);listY=top+side+12;}
        if(!wide&&(g.screen==GameCanvas.Screen.OVERVIEW||g.screen==GameCanvas.Screen.GOAL)){side=Math.min(side,112);listY=top+156;}
        if(wide&&(g.screen==GameCanvas.Screen.ANIMATION||contextual))side=Math.max(100,Math.min(side,bottom-top-(g.screen==GameCanvas.Screen.OVERVIEW||g.screen==GameCanvas.Screen.GOAL?156:100)));
        if(wide&&(g.screen==GameCanvas.Screen.OVERVIEW||g.screen==GameCanvas.Screen.GOAL)){listX=Math.max(side+40,w*.42f);listW=w-listX-16;}
        if(g.screen==GameCanvas.Screen.HOME){if(!wide)listY+=20;text("Elements",listX,wide?87:listY-8,14,14,listW);}
        float left=wide||contextual?16:(w-side)/2;
        SpriteRegion r=g.region();
        float imageW=draw?side*r.width/Math.max(r.width,r.height):side,imageH=draw?side*r.height/Math.max(r.width,r.height):side;
        imageArea=new RectF(left,top,left+imageW,top+imageH);
        int[] indices=draw?null:g.image(),colors=new int[16384];
        for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){
            int p=draw?g.pixel(Math.min(r.width-1,xx*r.width/128),Math.min(r.height-1,yy*r.height/128)):indices[yy*128+xx];
            colors[yy*128+xx]=PicoPalette.COLORS[p];
        }
        bitmap.setPixels(colors,0,128,0,0,128,128);canvas.drawBitmap(bitmap,null,imageArea,paint);
        outline(left-1,top-1,imageW+2,imageH+2,13);
        if(draw){float pw=imageW/r.width,ph=imageH/r.height;outline(left+g.cursorX*pw,top+g.cursorY*ph,pw,ph,7);
            if(g.screen==GameCanvas.Screen.DRAW){
                if(wide){text("Your sprite",listX,top+24,22,7,listW);text(r.width+" x "+r.height+" pixels",listX,top+58,18,6,listW);
                    rect(listX,top+82,28,28,g.eraser?0:g.color);text(g.eraser?"Eraser":"Brush",listX+40,top+104,20,7,listW-40);
                    wrap("A paints a pixel. B opens Done and drawing options.",listX,top+146,listW,5);
                }else text(g.eraser?"Eraser":"Brush  /  color "+g.color,left,bottom+12,18,6,w-32);
            }
        }else if(g.blocked.isEmpty()&&!g.uses.entries.isEmpty()&&!g.globalRuleSelection()||g.screen==GameCanvas.Screen.PLACE){
            int sx,sy,sw,sh;
            if(g.screen==GameCanvas.Screen.PLACE){sx=g.x;sy=g.y;sw=r.width;sh=r.height;}
            else{GameUses.Entry e=g.uses.entries.get(g.selected);SpriteRegion region=g.region(g.selected);sx=e.x();sy=e.y();sw=region.width;sh=region.height;}
            canvas.save();canvas.clipRect(imageArea);outline(left+sx*side/128,top+sy*side/128,sw*side/128,sh*side/128,10);
            if(g.screen==GameCanvas.Screen.RELATION)for(int candidate:g.relationshipChoices())if(candidate>=0)elementOutline(candidate,side,13);
            int other=g.relatedElement();
            if(other>=0){GameUses.Entry t=g.uses.entries.get(other);SpriteRegion tr=g.region(other);paint.setColor(PicoPalette.COLORS[12]);paint.setStrokeWidth(1);
                canvas.drawLine(left+(sx+sw/2f)*side/128,top+(sy+sh/2f)*side/128,left+(t.x()+tr.width/2f)*side/128,top+(t.y()+tr.height/2f)*side/128,paint);
                elementOutline(other,side,12);outline(left+sx*side/128,top+sy*side/128,sw*side/128,sh*side/128,10);
            }
            canvas.restore();
        }
        if(!draw){canvas.save();canvas.clipRect(imageArea);for(int element:g.goalElements())elementOutline(element,side,12);canvas.restore();}
        if(wide&&g.screen==GameCanvas.Screen.ANIMATION){
            GameAnimation animation=g.animation;float y=top+side+12,cell=Math.min(34,(listX-40)/8);
            for(int n=0;n<animation.sequence().frames.size();n++){
                final int frame=n;SpriteRegion source=animation.sequence().frames.get(n).region;float x=left+n*cell;
                for(int py=0;py<source.height;py++)for(int px=0;px<source.width;px++)rect(x+px*(cell-4)/source.width,y+py*(cell-4)/source.height,(cell-4)/source.width,(cell-4)/source.height,animation.resources.pixel(source,px,py));
                outline(x-1,y-1,cell-2,cell-2,n==animation.frame?10:13);
                hits.add(new Hit(x,y,cell,cell,()->{if(g.editingField)return;animation.frame=frame;changed();}));
            }
            wrap(g.guidance(),left,y+cell+20,listX-left-20,Math.max(0,(int)((bottom-y-cell-8)/26)));
        }
        if(wide&&contextual)wrap(g.summary(),left,top+side+24,listX-left-20,Math.max(0,(int)((bottom-top-side-8)/26)));
        else if(contextual)wrap(g.summary(),left+side+16,top+18,w-left-side-32,g.screen==GameCanvas.Screen.OVERVIEW||g.screen==GameCanvas.Screen.GOAL?6:Math.max(1,(int)(side/26)));
        if(g.screen==GameCanvas.Screen.PALETTE){
            float px=wide?listX:16,py=wide?top+20:listY,cell=Math.min((wide?listW:w-32)/8,40);
            for(int n=0;n<16;n++){final int color=n;float cx=px+n%8*cell,cy=py+n/8*cell;rect(cx+2,cy+2,cell-4,cell-4,n);if(g.focus==n)outline(cx,cy,cell,cell,7);hits.add(new Hit(cx,cy,cell,cell,()->{g.focus=color;act(Action.CONFIRM);}));}
        }else if(menu){
            int rowHeight=g.screen==GameCanvas.Screen.BEHAVIOR||g.screen==GameCanvas.Screen.SETTINGS||g.screen==GameCanvas.Screen.CHOICE||g.screen==GameCanvas.Screen.GOAL||g.screen==GameCanvas.Screen.OVERVIEW?50:40;
            int visible=Math.max(1,(int)((bottom-listY)/rowHeight)),first=Math.max(0,Math.min(Math.max(0,rows.length-visible),g.focus-visible/2));
            for(int n=first;n<Math.min(rows.length,first+visible);n++){
                final int row=n;float yy=listY+(n-first)*rowHeight;int element=g.rowElement(row);
                button(element>=0?"":rows[n],listX,yy,listW,rowHeight-2,g.focus==n,()->{
                    if(g.editingField){if(row==g.focus)act(Action.RIGHT);return;}
                    g.focus=row;
                    if(g.screen==GameCanvas.Screen.RELATION){changed();return;}
                    act(Action.CONFIRM);
                });
                if(element>=0){drawThumbnail(element,listX+8,yy+3,32);int split=rows[n].indexOf('\n');
                    if(split<0)text(rows[n],listX+48,yy+27,20,g.focus==n?10:7,listW-58);
                    else{text(rows[n].substring(0,split),listX+48,yy+17,18,6,listW-58);text(rows[n].substring(split+1),listX+48,yy+39,20,g.focus==n?10:7,listW-58);}}
            }
            if(rows.length>visible){rect(w-7,listY,2,visible*rowHeight,13);rect(w-8,listY+(visible*rowHeight-10)*g.focus/(rows.length-1),4,10,14);}
            if(wide&&g.screen==GameCanvas.Screen.SHARED)wrap(g.sharedUses()+" placements use these pixels"+(g.mapReference()?"; map tiles also refer to them.":"."),listX,listY+rows.length*rowHeight+20,listW,4);
            if(wide&&g.screen==GameCanvas.Screen.HOME&&g.uses.entries.isEmpty())wrap(g.blocked.isEmpty()?"Start with your own sprite. Draw it, then place it in the game.":g.blocked,listX,listY+rows.length*rowHeight+20,listW,4);
        }else if(g.screen==GameCanvas.Screen.PLACE&&wide){
            text("Choose a position",listX,top+24,22,7,listW);wrap("Move with the D-pad or tap the game. Done saves the whole change.",listX,top+62,listW,5);
        }
        if(g.screen==GameCanvas.Screen.CHOICE||g.screen==GameCanvas.Screen.GOAL)footer(yes+"  Choose",Action.CONFIRM,back+"  Cancel",Action.CANCEL);
        else if(g.screen==GameCanvas.Screen.RELATION)footer(yes+"  Confirm",Action.CONFIRM,back+"  Cancel",Action.CANCEL);
        else if(g.editingField)footer(yes+"  Keep value",Action.CONFIRM,back+"  Revert",Action.CANCEL);
        else if(g.screen==GameCanvas.Screen.DRAW)footer(yes+"  Paint",Action.CONFIRM,back+"  Done / options",Action.CANCEL);
        else if(g.screen==GameCanvas.Screen.PLACE)footer(yes+"  Done",Action.CONFIRM,back+(g.operation==GameCanvas.Operation.ADD?"  Draw again":"  Cancel"),Action.CANCEL);
        else footer(yes+"  Select",Action.CONFIRM,back+(g.screen==GameCanvas.Screen.HOME?"  Projects":"  Back"),Action.CANCEL);
    }
    private void nameEditor(String yes,String back){
        GameCanvas g=game();NameEditor e=g.nameDraft;
        text(g.label(g.selected),16,87,18,6,width-122);text(e.text().length()+" / "+e.limit,width-100,87,18,6,84);
        rect(16,96,width-32,40,e.replaceAll?2:0);outline(16,96,width-32,40,10);
        text(e.text().isEmpty()?"(automatic label)":e.text(),24,124,20,e.text().isEmpty()?6:7,width-48);
        float control=(width-40)/3;
        button("Y Case",16,144,control,34,false,()->act(Action.UNDO));
        button(e.latin?"L/R EN":"L/R RU",20+control,144,control,34,false,()->act(Action.NEXT));
        button(e.replaceAll?"All":"Append",24+control*2,144,control,34,false,()->act(Action.MENU));
        float top=192,rowHeight=Math.min(40,(height-top-88-48)/5),cell=(width-32)/10;
        for(int n=0;n<e.count();n++){
            final int key=n;char ch=e.character(n);
            button(ch==' '?"_":String.valueOf(ch),16+n%10*cell,top+n/10*rowHeight,cell-2,rowHeight-2,g.focus==0&&e.key==n,()->{g.focus=0;e.key=key;act(Action.CONFIRM);});
        }
        float y=top+5*rowHeight+8;
        button("Save name",16,y,(width-40)/2,36,g.focus==1,()->{g.focus=1;act(Action.CONFIRM);});
        button("Cancel",24+(width-40)/2,y,(width-40)/2,36,g.focus==2,()->act(Action.CANCEL));
        if(!e.warning.isEmpty())text(e.warning,16,184,18,9,width-32);
        footer(yes+(g.focus==0?"  Type":"  Select"),Action.CONFIRM,back+"  Cancel",Action.CANCEL);
    }
    private void elementOutline(int index,float side,int color){
        GameUses.Entry e=game().uses.entries.get(index);SpriteRegion r=game().region(index);
        outline(imageArea.left+e.x()*side/128,imageArea.top+e.y()*side/128,r.width*side/128,r.height*side/128,color);
    }
    private void drawThumbnail(int index,float x,float y,float size){
        SpriteRegion r=game().region(index);int[] colors=new int[32*32];int longest=Math.max(r.width,r.height);
        int tw=Math.max(1,32*r.width/longest),th=Math.max(1,32*r.height/longest);
        for(int py=0;py<32;py++)for(int px=0;px<32;px++){
            int col=(px/4+py/4)%2==0?0:5;
            if(px<tw&&py<th){int pixel=game().base.pixel(r,Math.min(r.width-1,px*r.width/tw),Math.min(r.height-1,py*r.height/th));if(pixel!=0)col=pixel;}
            colors[py*32+px]=PicoPalette.COLORS[col];
        }
        thumbnail.setPixels(colors,0,32,0,0,32,32);canvas.drawBitmap(thumbnail,null,new RectF(x,y,x+size,y+size),paint);
    }
    private void wrap(String str,float x,float y,float max,int lines){
        paint.setTextSize(PixelText.size(18));for(int n=0;n<lines&&!str.isEmpty();n++){
            int at=str.length();while(at>1&&paint.measureText(str.substring(0,at))>max)at--;
            if(at<str.length()){int space=str.lastIndexOf(' ',at);if(space>0)at=space;}
            text(str.substring(0,at),x,y+n*26,18,6,max);str=str.substring(at).trim();
        }
    }
    private int px(float x){return Math.max(0,Math.min(game().region().width-1,(int)((x-imageArea.left)/imageArea.width()*game().region().width)));}
    private int py(float y){return Math.max(0,Math.min(game().region().height-1,(int)((y-imageArea.top)/imageArea.height()*game().region().height)));}
    private void paintTo(int x,int y){int dx=Math.abs(x-lastX),sx=lastX<x?1:-1,dy=-Math.abs(y-lastY),sy=lastY<y?1:-1,err=dx+dy;
        while(true){game().paint(lastX,lastY);if(lastX==x&&lastY==y)break;int twice=2*err;if(twice>=dy){err+=dy;lastX+=sx;}if(twice<=dx){err+=dx;lastY+=sy;}}
    }
    boolean touch(MotionEvent e){
        float x=e.getX()/scale,y=e.getY()/scale;GameCanvas g=game();
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){painting=false;return true;}
        if(session.mode!=WorkshopSession.Mode.ERROR&&e.getActionMasked()==MotionEvent.ACTION_DOWN&&g.screen==GameCanvas.Screen.DRAW&&imageArea!=null&&imageArea.contains(x,y)){
            painting=true;pointer=e.getPointerId(0);lastX=px(x);lastY=py(y);g.paint(lastX,lastY);changed();return true;
        }
        if(painting){
            int at=e.findPointerIndex(pointer);if(at<0){painting=false;return true;}
            boolean end=e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointer;
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE||end){
                for(int n=0;n<e.getHistorySize();n++)paintTo(px(e.getHistoricalX(at,n)/scale),py(e.getHistoricalY(at,n)/scale));
                paintTo(px(e.getX(at)/scale),py(e.getY(at)/scale));changed();
            }
            if(end)painting=false;return true;
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            if(session.mode!=WorkshopSession.Mode.ERROR&&imageArea!=null&&imageArea.contains(x,y)&&(g.screen==GameCanvas.Screen.HOME||g.screen==GameCanvas.Screen.PLACE||g.screen==GameCanvas.Screen.RELATION)){
                int px=(int)((x-imageArea.left)*128/imageArea.width()),py=(int)((y-imageArea.top)*128/imageArea.height());
                if(g.screen==GameCanvas.Screen.PLACE)g.point(px,py);
                else if(g.pickElementAt(px,py)&&g.screen==GameCanvas.Screen.HOME){act(Action.CONFIRM);return true;}
                changed();return true;
            }
            for(Hit hit:hits)if(hit.area.contains(x,y)){hit.run.run();break;}view.performClick();
        }return true;
    }
}
