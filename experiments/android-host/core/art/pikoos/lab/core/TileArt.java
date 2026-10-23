package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Nested 8x8 tile draft. Pixels are ordinary gfx; no object or Lua is created. */
public final class TileArt {
    public enum Mode { NEW, COPY, ALL }
    public enum Page { DRAW, MENU, PALETTE, REVIEW }
    public enum Result { NONE, KEEP, DISCARD, TEST }
    public final WorkshopCartridge context;
    public final Mode mode;
    public final int source,target,mapX,mapY,flags;
    public final SpriteRegion region;
    public Page page=Page.DRAW;
    public int x,y,color=11,focus;
    public boolean eraser;
    private int[] pixels=new int[64];
    private final ArrayDeque<int[]> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private SpriteStroke stroke;
    private int[] strokeBefore;
    public TileArt(WorkshopCartridge context,Mode mode,int source,int target,int mapX,int mapY){
        if(target<1||target>127||mapX<0||mapX>127||mapY<0||mapY>31||mode==Mode.NEW&&source!=-1||mode!=Mode.NEW&&(source<1||source>255)||mode==Mode.ALL&&source!=target)throw new IllegalArgumentException("Invalid tile edit");
        this.context=context;this.mode=mode;this.source=source;this.target=target;this.mapX=mapX;this.mapY=mapY;
        region=new SpriteRegion(target%16*8,target/16*8,8,8);flags=source<0?0:context.flags(source);
        if(source>=0)for(int y=0;y<8;y++)for(int x=0;x<8;x++)pixels[y*8+x]=context.sheetPixel(source%16*8+x,source/16*8+y);
    }
    public String title(){if(page==Page.REVIEW)return "Review tile";return mode==Mode.NEW?"Draw a new tile":mode==Mode.COPY?"Edit tile copy":"Edit tile "+target;}
    public String[] rows(){return page==Page.MENU?new String[]{"Review tile","Choose color",eraser?"Use brush":"Use eraser","Keep drawing","Discard tile"}:page==Page.REVIEW?new String[]{"Keep tile","Keep drawing","Discard tile"}:new String[0];}
    public String summary(){return mode==Mode.ALL?"Changes every use of tile "+target+". Keep adds this edit to the world draft.":"Keep puts this tile in the selected map cell. Other tiles and objects stay as they are.";}
    public int pixel(int x,int y){return pixels[y*8+x];}
    public int beforePixel(int x,int y){return source<0?0:context.sheetPixel(source%16*8+x,source/16*8+y);}
    public int[] pixels(){return pixels.clone();}
    public boolean strokeActive(){return stroke!=null;}
    public void point(int px,int py){x=Math.max(0,Math.min(7,px));y=Math.max(0,Math.min(7,py));}
    private WorkshopCartridge painted(int[] data){int[] values=new int[16384];Arrays.fill(values,-1);for(int i=0;i<64;i++)values[(region.y+i/8)*128+region.x+i%8]=data[i];return context.withSheetPixels(values);}
    public WorkshopCartridge candidate(){WorkshopCartridge result=painted(pixels);return flags==context.flags(target)?result:result.withFlags(target,flags);}
    public void requireVisible(){if(mode!=Mode.ALL){for(int p:pixels)if(p!=0)return;throw new IllegalArgumentException("Paint at least one visible pixel before keeping or testing the new tile. Color 0 is transparent.");}}
    public void beginStroke(int px,int py){endStroke();point(px,py);strokeBefore=pixels.clone();stroke=new SpriteStroke(painted(strokeBefore),region,eraser?0:color,x,y);readStroke();}
    public void strokePoint(int px,int py){if(stroke==null){beginStroke(px,py);return;}point(px,py);stroke.point(x,y);readStroke();}
    private void readStroke(){WorkshopCartridge result=stroke.preview(painted(strokeBefore));for(int n=0;n<64;n++)pixels[n]=result.sheetPixel(region.x+n%8,region.y+n/8);}
    public void endStroke(){if(stroke==null)return;record(strokeBefore);stroke=null;strokeBefore=null;}
    public void paint(int px,int py){beginStroke(px,py);endStroke();}
    private void record(int[] previous){if(Arrays.equals(previous,pixels))return;undo.push(previous.clone());while(undo.size()>32)undo.removeLast();redo.clear();}
    public Result act(Action a){
        endStroke();if(a==Action.TEST){requireVisible();return Result.TEST;}
        if(a==Action.UNDO||a==Action.REDO){ArrayDeque<int[]> from=a==Action.UNDO?undo:redo,to=a==Action.UNDO?redo:undo;if(!from.isEmpty()){to.push(pixels.clone());pixels=from.pop();}return Result.NONE;}
        if(page==Page.DRAW){
            if(a==Action.LEFT)point(x-1,y);if(a==Action.RIGHT)point(x+1,y);if(a==Action.UP)point(x,y-1);if(a==Action.DOWN)point(x,y+1);if(a==Action.CONFIRM)paint(x,y);
            if(a==Action.CONTEXT){page=Page.PALETTE;focus=color;}if(a==Action.CANCEL||a==Action.MENU){page=Page.MENU;focus=0;}return Result.NONE;
        }
        if(page==Page.PALETTE){
            if(a==Action.LEFT)focus=(focus+15)%16;if(a==Action.RIGHT)focus=(focus+1)%16;if(a==Action.UP||a==Action.DOWN)focus=(focus+8)%16;
            if(a==Action.CONFIRM){color=focus;eraser=false;page=Page.DRAW;focus=0;}if(a==Action.CANCEL){page=Page.DRAW;focus=0;}return Result.NONE;
        }
        if(a==Action.CANCEL){page=Page.DRAW;focus=0;return Result.NONE;}
        if(a==Action.UP)focus=Math.max(0,focus-1);if(a==Action.DOWN)focus=Math.min(rows().length-1,focus+1);
        if(a==Action.CONFIRM){
            if(page==Page.MENU){if(focus==0){page=Page.REVIEW;focus=0;}else if(focus==1){page=Page.PALETTE;focus=color;}else if(focus==2){eraser=!eraser;page=Page.DRAW;focus=0;}else if(focus==3){page=Page.DRAW;focus=0;}else return Result.DISCARD;}
            else{if(focus==0){requireVisible();return Result.KEEP;}if(focus==2)return Result.DISCARD;page=Page.DRAW;focus=0;}
        }return Result.NONE;
    }
    private static void writePixels(DataOutputStream out,int[] values)throws IOException{for(int v:values)out.writeByte(v);}
    private static int[] readPixels(DataInputStream in)throws IOException{int[] values=new int[64];for(int n=0;n<64;n++){values[n]=in.readUnsignedByte();if(values[n]>15)throw new IOException();}return values;}
    void encode(DataOutputStream out)throws IOException{
        out.writeUTF(mode.name());for(int n:new int[]{source,target,mapX,mapY,x,y,color,focus})out.writeInt(n);out.writeUTF(page.name());out.writeBoolean(eraser);writePixels(out,pixels);
        for(ArrayDeque<int[]> stack:Arrays.asList(undo,redo)){out.writeInt(stack.size());for(int[] v:stack)writePixels(out,v);}
        out.writeBoolean(stroke!=null);if(stroke!=null){writePixels(out,strokeBefore);byte[] data=stroke.encode();out.writeInt(data.length);out.write(data);}
    }
    static TileArt decode(DataInputStream in,WorkshopCartridge context)throws IOException{
        TileArt t=new TileArt(context,Mode.valueOf(in.readUTF()),in.readInt(),in.readInt(),in.readInt(),in.readInt());t.x=in.readInt();t.y=in.readInt();t.color=in.readInt();t.focus=in.readInt();t.page=Page.valueOf(in.readUTF());t.eraser=in.readBoolean();t.pixels=readPixels(in);
        for(ArrayDeque<int[]> stack:Arrays.asList(t.undo,t.redo)){int count=in.readInt();if(count<0||count>32)throw new IOException();for(int n=0;n<count;n++)stack.add(readPixels(in));}
        if(t.x<0||t.x>7||t.y<0||t.y>7||t.color<0||t.color>15||t.focus<0||t.focus>=(t.page==Page.PALETTE?16:Math.max(1,t.rows().length)))throw new IOException();
        if(in.readBoolean()){
            if(t.page!=Page.DRAW)throw new IOException();t.strokeBefore=readPixels(in);int length=in.readInt();if(length<0||length>1000||length>in.available())throw new IOException();byte[] data=new byte[length];in.readFully(data);t.stroke=SpriteStroke.restore(data,t.painted(t.strokeBefore));
            if(t.stroke.scope.x!=t.region.x||t.stroke.scope.y!=t.region.y||t.stroke.scope.width!=8||t.stroke.scope.height!=8||t.stroke.x!=t.x||t.stroke.y!=t.y||t.stroke.color!=(t.eraser?0:t.color))throw new IOException();
            int[] expected=t.pixels.clone();t.readStroke();if(!Arrays.equals(expected,t.pixels))throw new IOException();
        }return t;
    }
}
