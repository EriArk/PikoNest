package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** One recoverable transaction for world wiring and ordinary upper-map cells. */
public final class WorldEditor {
    public enum Page { SETTINGS, CAMERA, TARGET, PAINT, TILES, OPTIONS, REVIEW }
    public final WorkshopCartridge base;
    public final PlayModel model;
    private final P8Map map;
    private final GameCanvas labels;
    private final byte[] initial;
    public final GameElementNames names;
    public final MapEditor brush=new MapEditor();
    public PlayWorld world;
    public Page page=Page.SETTINGS;
    public int focus;
    public boolean editing;
    private int before,pendingCamera;
    private final int[] cells=new int[4096];
    private final ArrayDeque<byte[]> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private byte[] fieldBefore;
    public WorldEditor(WorkshopCartridge base){
        this.base=base;model=PlayModel.fromStatic(base);labels=new GameCanvas(base);names=labels.names;map=base.map();
        world=model.world==null?new PlayWorld():model.world.copy();Arrays.fill(cells,-1);initial=state();
    }
    public boolean draft(){return !Arrays.equals(initial,state());}
    public int changedCells(){int n=0;for(int v:cells)if(v>=0)n++;return n;}
    public int tile(int x,int y){int v=cells[y*128+x];return v<0?map.tile(x,y):v;}
    public String title(){return page==Page.PAINT?"Paint the map":page==Page.TILES?"Choose a tile":page==Page.CAMERA?"Choose a camera":page==Page.TARGET?"Follow which element?":page==Page.REVIEW?"Save world changes?":"World & camera";}
    public String summary(){
        if(page==Page.PAINT||page==Page.TILES||page==Page.OPTIONS)return "Tiles are scenery. Painting does not add walls or collision.";
        if(page==Page.CAMERA)return focus==0?"The view stays in one place.":focus==1?"The view moves with a chosen element.":focus==2?"Switch screens when the element's center crosses an edge.":"Keep the current camera.";
        if(page==Page.TARGET)return "Any element can guide the camera. Choose one from the list.";
        if(page==Page.REVIEW)return changedCells()+" map cells changed. Save keeps the map and camera together.";
        if(focus==0)return "How many screens across?";
        if(focus==1)return "How many screens down?";
        if(focus==2)return "Choose a tile, then paint with A.";
        if(focus==3)return "Hide or show tiles. Map data stays.";
        if(focus==4)return "Fixed, scrolling or room changes.";
        if(focus==5||focus==6)return world.camera==0?"Choose where the fixed view starts.":"Choose the element that guides the view.";
        return "Save or discard this whole change.";
    }
    private String targetLabel(){return world.target<0?"Choose an element":labels.label(world.target);}
    public String[] rows(){
        switch(page){
            case SETTINGS:{ArrayList<String> r=new ArrayList<>(Arrays.asList("Width\n"+world.width/16+" screen"+(world.width>16?"s":""),"Height\n"+world.height/16+" screen"+(world.height>16?"s":""),"Paint map","Show map\n"+(world.tiles?"Yes":"No"),"Camera\n"+PlayWorld.CAMERAS[world.camera]));
                if(world.camera==0){r.add("View X\n"+world.x+" px");r.add("View Y\n"+world.y+" px");}else r.add("Follow\n"+targetLabel());r.add("Review / save");r.add("Back to game");return r.toArray(new String[0]);}
            case CAMERA:return new String[]{"Fixed view","Follow an element","Change rooms","Cancel"};
            case TARGET:{ArrayList<String> r=new ArrayList<>();GameCanvas g=labels;for(int n=0;n<model.elements.size();n++)r.add(g.label(n));r.add("Cancel");return r.toArray(new String[0]);}
            case OPTIONS:return new String[]{"Keep painting","Choose tile","Use eraser","Review / save","World settings"};
            case REVIEW:return new String[]{"Save changes","Keep editing","Discard changes"};
            default:return new String[0];
        }
    }
    public int left(){return Math.max(0,Math.min(world.width-16,brush.x-7));}
    public int top(){return Math.max(0,Math.min(world.height-16,brush.y-7));}
    public void point(int x,int y){brush.x=clamp(x,0,world.width-1);brush.y=clamp(y,0,world.height-1);}
    public void paint(){byte[] previous=state();int at=brush.y*128+brush.x;cells[at]=map.tile(brush.x,brush.y)==brush.tile?-1:brush.tile;record(previous);}
    private int value(){return focus==0?world.width:focus==1?world.height:focus==5?world.x:world.y;}
    private void value(int v){if(focus==0)world.width=clamp(v,16,128);else if(focus==1)world.height=clamp(v,16,32);else if(focus==5)world.x=clamp(v,0,world.width*8-128);else world.y=clamp(v,0,world.height*8-128);world.x=Math.min(world.x,world.width*8-128);world.y=Math.min(world.y,world.height*8-128);point(brush.x,brush.y);}
    private void page(Page p,int at){page=p;focus=at;}
    public GameCanvas.Command act(Action a){
        if(a==Action.TEST)return GameCanvas.Command.TEST;
        if(editing){
            if(a==Action.CANCEL){state(fieldBefore);editing=false;fieldBefore=null;}
            else if(a==Action.CONFIRM){editing=false;record(fieldBefore);fieldBefore=null;}
            else if(a==Action.LEFT||a==Action.RIGHT||a==Action.UP||a==Action.DOWN)value(value()+(a==Action.LEFT||a==Action.DOWN?-1:1)*(focus<2?16:8));
            return GameCanvas.Command.NONE;
        }
        if(a==Action.UNDO||a==Action.REDO){ArrayDeque<byte[]> from=a==Action.UNDO?undo:redo,to=a==Action.UNDO?redo:undo;if(!from.isEmpty()){to.push(state());state(from.pop());point(brush.x,brush.y);if(page!=Page.PAINT&&page!=Page.TILES)page(Page.SETTINGS,0);}else if(!draft())return a==Action.UNDO?GameCanvas.Command.UNDO:GameCanvas.Command.REDO;return GameCanvas.Command.NONE;}
        if(page==Page.PAINT){
            if(a==Action.LEFT)point(brush.x-1,brush.y);if(a==Action.RIGHT)point(brush.x+1,brush.y);if(a==Action.UP)point(brush.x,brush.y-1);if(a==Action.DOWN)point(brush.x,brush.y+1);
            if(a==Action.CONFIRM)paint();if(a==Action.CONTEXT){brush.begin();page(Page.TILES,0);}if(a==Action.CANCEL||a==Action.MENU)page(Page.OPTIONS,0);return GameCanvas.Command.NONE;
        }
        if(page==Page.TILES){
            if(a==Action.CONTEXT)brush.choice=0;
            if(a==Action.LEFT)brush.move(-1,0);if(a==Action.RIGHT)brush.move(1,0);if(a==Action.UP)brush.move(0,-1);if(a==Action.DOWN)brush.move(0,1);
            if(a==Action.CONFIRM){brush.accept();page(Page.PAINT,0);}if(a==Action.CANCEL){brush.cancel();page(Page.PAINT,0);}return GameCanvas.Command.NONE;
        }
        if(a==Action.MENU){page(Page.REVIEW,0);return GameCanvas.Command.NONE;}
        if(a==Action.CANCEL){if(page==Page.SETTINGS){if(!draft())return GameCanvas.Command.CLOSE_WORLD;page(Page.REVIEW,1);}else page(Page.SETTINGS,0);return GameCanvas.Command.NONE;}
        if(a==Action.UP)focus=Math.max(0,focus-1);if(a==Action.DOWN)focus=Math.min(rows().length-1,focus+1);
        if(a!=Action.CONFIRM)return GameCanvas.Command.NONE;
        byte[] previous=state();
        switch(page){
            case SETTINGS:
                if(focus<2||world.camera==0&&(focus==5||focus==6)){before=value();fieldBefore=state();editing=true;}
                else if(focus==2)page(Page.PAINT,0);
                else if(focus==3)world.tiles=!world.tiles;
                else if(focus==4)page(Page.CAMERA,world.camera);
                else if(focus==5){pendingCamera=world.camera;page(Page.TARGET,Math.max(0,world.target));}
                else if(focus==rows().length-2)page(Page.REVIEW,0);
                else{if(!draft())return GameCanvas.Command.CLOSE_WORLD;page(Page.REVIEW,1);}break;
            case CAMERA:
                if(focus==0){world.camera=0;page(Page.SETTINGS,4);}
                else if(focus<3){if(model.elements.isEmpty())throw new IllegalArgumentException("Add an element from Game view first, or choose Fixed view. A map can work without any elements.");pendingCamera=focus;page(Page.TARGET,Math.max(0,world.target));}
                else page(Page.SETTINGS,4);break;
            case TARGET:if(focus<model.elements.size()){world.target=focus;world.camera=pendingCamera;}page(Page.SETTINGS,4);break;
            case OPTIONS:if(focus==0)page(Page.PAINT,0);else if(focus==1){brush.begin();page(Page.TILES,0);}else if(focus==2){brush.tile=0;page(Page.PAINT,0);}else if(focus==3)page(Page.REVIEW,0);else page(Page.SETTINGS,2);break;
            case REVIEW:if(focus==0)return GameCanvas.Command.SAVE;if(focus==2)return GameCanvas.Command.CLOSE_WORLD;page(Page.SETTINGS,0);break;
            default:break;
        }
        if(!editing)record(previous);return GameCanvas.Command.NONE;
    }
    public int previewX(){return page==Page.PAINT||page==Page.OPTIONS?left()*8:world.offset(model,true);}
    public int previewY(){return page==Page.PAINT||page==Page.OPTIONS?top()*8:world.offset(model,false);}
    public int[] image(){
        int[] pixels=new int[16384];Arrays.fill(pixels,model.background);int ox=previewX(),oy=previewY();
        if(page==Page.TILES){for(int y=0;y<128;y++)for(int x=0;x<128;x++)pixels[y*128+x]=base.sheetPixel(x,y);Arrays.fill(pixels,0,8,0);for(int y=0;y<8;y++)for(int x=0;x<8;x++)pixels[y*128+x]=0;return pixels;}
        boolean show=world.tiles||page==Page.PAINT||page==Page.OPTIONS;
        if(show)for(int y=0;y<128;y++)for(int x=0;x<128;x++){int wx=ox+x,wy=oy+y,t=tile(wx/8,wy/8);if(t!=0){int p=base.sheetPixel(t%16*8+wx%8,t/16*8+wy%8);if(p!=0)pixels[y*128+x]=p;}}
        for(PlayModel.Element e:model.elements)for(int y=0;y<e.h;y++)for(int x=0;x<e.w;x++){int dx=e.x+x-ox,dy=e.y+y-oy,p=base.sheetPixel(e.sx+x,e.sy+y);if(p!=0&&dx>=0&&dx<128&&dy>=0&&dy<128)pixels[dy*128+dx]=p;}
        return pixels;
    }
    public WorkshopCartridge candidate(){
        world.validate(model.elements.size());int[] changes=new int[8192];Arrays.fill(changes,-1);System.arraycopy(cells,0,changes,0,4096);
        WorkshopCartridge resources=new WorkshopCartridge(base.map().withCells(changes).bytes());PlayModel result=model.copy();result.world=world.copy();return names.apply(result.write(resources));
    }
    private void record(byte[] previous){if(Arrays.equals(previous,state()))return;undo.push(previous);while(undo.size()>32)undo.removeLast();redo.clear();}
    private byte[] state(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);world.encode(out);for(int v:cells)out.writeShort(v+1);return bytes.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    private void state(byte[] bytes){try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));PlayWorld next=PlayWorld.decode(in);next.validate(model.elements.size());int[] values=new int[4096];for(int n=0;n<4096;n++){values[n]=in.readUnsignedShort()-1;if(values[n]>255)throw new IOException();}if(in.available()!=0)throw new IOException();world=next;System.arraycopy(values,0,cells,0,4096);}catch(IOException e){throw new IllegalArgumentException("Invalid world draft",e);}}
    private static void bytes(DataOutputStream out,byte[] data)throws IOException{out.writeInt(data.length);out.write(data);}
    private static byte[] bytes(DataInputStream in)throws IOException{int length=in.readInt();if(length<0||length>9000||length>in.available())throw new IOException();byte[] data=new byte[length];in.readFully(data);return data;}
    void encode(DataOutputStream out)throws IOException{
        bytes(out,state());out.writeUTF(page.name());for(int v:new int[]{focus,brush.x,brush.y,brush.tile,brush.choice,before,pendingCamera})out.writeInt(v);out.writeBoolean(editing);if(editing)bytes(out,fieldBefore);
        for(ArrayDeque<byte[]> stack:Arrays.asList(undo,redo)){out.writeInt(stack.size());for(byte[] v:stack)bytes(out,v);}
    }
    static WorldEditor decode(DataInputStream in,WorkshopCartridge base)throws IOException{
        WorldEditor e=new WorldEditor(base);e.state(bytes(in));byte[] current=e.state();e.page=Page.valueOf(in.readUTF());e.focus=in.readInt();e.brush.x=in.readInt();e.brush.y=in.readInt();e.brush.tile=in.readInt();e.brush.choice=in.readInt();e.before=in.readInt();e.pendingCamera=in.readInt();e.editing=in.readBoolean();if(e.editing){e.fieldBefore=bytes(in);e.state(e.fieldBefore);e.state(current);}
        for(ArrayDeque<byte[]> stack:Arrays.asList(e.undo,e.redo)){int count=in.readInt();if(count<0||count>32)throw new IOException();for(int n=0;n<count;n++){byte[] v=bytes(in);e.state(v);stack.add(v);}}e.state(current);
        if(e.focus<0||e.focus>=Math.max(1,e.rows().length)||e.brush.x<0||e.brush.x>=e.world.width||e.brush.y<0||e.brush.y>=e.world.height||e.brush.tile<0||e.brush.tile>255||e.brush.choice<0||e.brush.choice>255||e.pendingCamera<0||e.pendingCamera>2)throw new IOException();
        if(e.editing&&(e.page!=Page.SETTINGS||!(e.focus<2||e.world.camera==0&&(e.focus==5||e.focus==6)))||e.page==Page.TARGET&&e.pendingCamera==0)throw new IOException();
        e.brush.picking=e.page==Page.TILES;return e;
    }
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
