package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.p8.P8Document;

/** One recoverable transaction for sequence settings, independent pixels and Lua binding. */
public final class GameAnimation {
    public final WorkshopCartridge base;
    public final int element;
    public PlayModel model;
    public WorkshopCartridge resources;
    public int frame,focus,before;
    public boolean editing;
    public String notice="";
    private byte[] paintBefore;
    private final GameElementNames names;
    private final List<SpriteRegion> editedRegions=new ArrayList<>();
    public GameAnimation(WorkshopCartridge base,int element){
        this.base=base;this.element=element;model=PlayModel.fromStatic(base);resources=base;
        names=GameElementNames.read(base,new GameUses(model.layout(base),0));
        PlayModel.Element e=model.elements.get(element);
        if(e.animation==null)e.animation=new PlayAnimation(new SpriteRegion(e.sx,e.sy,e.w,e.h));
    }
    public PlayAnimation sequence(){return model.elements.get(element).animation;}
    public PlayAnimation.Frame current(){return sequence().frames.get(frame);}
    public String[] rows(){return new String[]{"Play: "+PlayAnimation.MODES[sequence().mode],"Frame: "+(frame+1)+" / "+sequence().frames.size(),"Duration: "+Math.round(current().ticks*1000f/30)+" ms","Edit this frame","+ Copy frame","Move earlier","Move later","Remove frame","Save animation","Discard changes"};}
    public int value(){return focus==0?sequence().mode:focus==1?frame:current().ticks;}
    public void value(int v){if(focus==0)sequence().mode=v;else if(focus==1)frame=v;else current().ticks=v;}
    public void step(int delta){int min=focus==2?1:0,max=focus==0?2:focus==1?sequence().frames.size()-1:150;value(Math.max(min,Math.min(max,value()+delta)));}
    private void syncFirst(){PlayModel.Element e=model.elements.get(element);SpriteRegion r=sequence().frames.get(0).region;e.sx=r.x;e.sy=r.y;}
    private SpriteRegion allocate(){PlayModel.Element e=model.elements.get(element);return new GameCanvas(candidate()).allocate(e.w,e.h);}
    private void edited(SpriteRegion r){for(SpriteRegion old:editedRegions)if(old.x==r.x&&old.y==r.y&&old.width==r.width&&old.height==r.height)return;editedRegions.add(r);}
    private WorkshopCartridge copyPixels(SpriteRegion from,SpriteRegion to){int[] pixels=new int[from.width*from.height];for(int y=0;y<from.height;y++)for(int x=0;x<from.width;x++)pixels[y*from.width+x]=resources.pixel(from,x,y);WorkshopCartridge next=pixels(resources,to,pixels);edited(to);return next;}
    public void duplicate(){
        if(sequence().frames.size()>=PlayAnimation.MAX_FRAMES)throw new IllegalArgumentException("This Game view editor supports up to 8 frames per sequence. All tools keeps the full sheet.");
        SpriteRegion target=allocate();WorkshopCartridge copied=copyPixels(current().region,target);
        sequence().frames.add(frame+1,new PlayAnimation.Frame(target,current().ticks));frame++;resources=copied;notice="Independent copy. Edit it to draw the next pose.";
    }
    public void reorder(int delta){int to=Math.max(0,Math.min(sequence().frames.size()-1,frame+delta));Collections.swap(sequence().frames,frame,to);frame=to;syncFirst();}
    public void remove(){if(sequence().frames.size()==1)throw new IllegalArgumentException("Keep one frame. A one-frame sequence is a still sprite.");sequence().frames.remove(frame);frame=Math.min(frame,sequence().frames.size()-1);syncFirst();notice="Frame removed from this sequence; sheet pixels kept.";}
    private static boolean overlap(SpriteRegion a,SpriteRegion b){return a.x<b.x+b.width&&b.x<a.x+a.width&&a.y<b.y+b.height&&b.y<a.y+a.height;}
    /** Shared or map-backed frames get an independent upper-sheet copy before painting. */
    public SpriteRegion beginPaint(){
        byte[] checkpoint;
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();encode(new DataOutputStream(bytes));checkpoint=bytes.toByteArray();}catch(IOException ex){throw new IllegalStateException(ex);}
        SpriteRegion r=current().region;boolean shared=r.sharesMap();
        for(int n=0;n<model.elements.size();n++){PlayModel.Element e=model.elements.get(n);
            if(e.animation==null){if(n!=element&&overlap(r,new SpriteRegion(e.sx,e.sy,e.w,e.h)))shared=true;}
            else for(int f=0;f<e.animation.frames.size();f++)if((n!=element||f!=frame)&&overlap(r,e.animation.frames.get(f).region))shared=true;
        }
        P8Map map=base.map();for(int y=0;y<64;y++)for(int x=0;x<128;x++){int t=map.tile(x,y);if(t!=0&&overlap(r,new SpriteRegion(t%16*8,t/16*8,8,8)))shared=true;}
        if(shared){SpriteRegion target=allocate();WorkshopCartridge copied=copyPixels(r,target);current().region=target;resources=copied;syncFirst();notice="Independent copy: other sprites, frames and tiles stay unchanged.";}
        paintBefore=checkpoint;return current().region;
    }
    static WorkshopCartridge pixels(WorkshopCartridge cart,SpriteRegion r,int[] pixels){
        if(pixels.length!=r.width*r.height)throw new IllegalArgumentException("Invalid frame pixels");int[] sheet=new int[16384];
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)sheet[y*128+x]=cart.sheetPixel(x,y);
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){int p=pixels[y*r.width+x];if(p<0||p>15)throw new IllegalArgumentException("Invalid color");sheet[(r.y+y)*128+r.x+x]=p;}
        return cart.withSheetPixels(sheet);
    }
    public void paint(int[] pixels){resources=pixels(resources,current().region,pixels);edited(current().region);paintBefore=null;}
    public void cancelPaint(){
        if(paintBefore==null)return;
        try{GameAnimation prior=decode(new DataInputStream(new ByteArrayInputStream(paintBefore)),base);model=prior.model;resources=prior.resources;frame=prior.frame;editedRegions.clear();editedRegions.addAll(prior.editedRegions);paintBefore=null;notice="Frame edit discarded.";}catch(IOException ex){throw new IllegalStateException(ex);}
    }
    public WorkshopCartridge candidate(){return names.apply(model.write(resources));}
    public WorkshopCartridge candidate(int[] pixels){return names.apply(model.write(pixels(resources,current().region,pixels)));}
    public String guidance(){if(!notice.isEmpty())return notice;if(sequence().mode==1)return "Plays while position changes. Standing still returns to frame 1.";if(sequence().mode==2)return "Plays while this switch is on. Off returns to frame 1.";return "Copy a frame, draw a new pose, then Test. Save commits the whole sequence.";}
    void encode(DataOutputStream out)throws IOException{
        out.writeInt(element);model.encode(out);model.encodeAnimations(out);model.encodeSounds(out);model.encodeMusic(out);byte[] data=resources.bytes();out.writeInt(data.length);out.write(data);
        out.writeInt(frame);out.writeInt(focus);out.writeInt(before);out.writeBoolean(editing);
        out.writeInt(editedRegions.size());for(SpriteRegion r:editedRegions)for(int v:new int[]{r.x,r.y,r.width,r.height})out.writeInt(v);
        out.writeInt(paintBefore==null?0:paintBefore.length);if(paintBefore!=null)out.write(paintBefore);
    }
    static GameAnimation decode(DataInputStream in,WorkshopCartridge base)throws IOException{return decode(in,base,8);}
    static GameAnimation decode(DataInputStream in,WorkshopCartridge base,int version)throws IOException{
        int element=in.readInt();GameAnimation a=new GameAnimation(base,element);a.model=PlayModel.decode(in);a.model.world=PlayModel.fromStatic(base).world;a.model.decodeAnimations(in);if(version>=7)a.model.decodeSounds(in);if(version>=8)a.model.decodeMusic(in);
        int length=in.readInt();if(length<0||length>2*1024*1024||length>in.available())throw new IOException();byte[] bytes=new byte[length];in.readFully(bytes);a.resources=new WorkshopCartridge(bytes);
        a.frame=in.readInt();a.focus=in.readInt();a.before=in.readInt();a.editing=in.readBoolean();
        int regions=in.readInt();if(regions<0||regions>1024)throw new IOException();for(int n=0;n<regions;n++){SpriteRegion r=new SpriteRegion(in.readInt(),in.readInt(),in.readInt(),in.readInt());if(r.sharesMap())throw new IOException();a.editedRegions.add(r);}
        int checkpoint=in.readInt();if(checkpoint<0||checkpoint>2*1024*1024||checkpoint>in.available())throw new IOException();if(checkpoint>0){a.paintBefore=new byte[checkpoint];in.readFully(a.paintBefore);}
        if(version<8&&a.paintBefore!=null){GameAnimation prior=decode(new DataInputStream(new ByteArrayInputStream(a.paintBefore)),base,version);ByteArrayOutputStream upgraded=new ByteArrayOutputStream();prior.encode(new DataOutputStream(upgraded));a.paintBefore=upgraded.toByteArray();}
        if(a.frame<0||a.frame>=a.sequence().frames.size()||a.focus<0||a.focus>=a.rows().length||a.editing&&(a.focus>2||a.before<(a.focus==2?1:0)||a.before>(a.focus==0?2:a.focus==1?a.sequence().frames.size()-1:150)))throw new IOException();
        PlayModel original=PlayModel.fromStatic(base),checked=a.model.copy();
        if(checked.elements.size()!=original.elements.size())throw new IOException();
        // Only this sequence and its first-frame source may change, never behavior or other elements.
        PlayModel.Element e=checked.elements.get(element),old=original.elements.get(element);e.sx=old.sx;e.sy=old.sy;e.animation=old.animation;
        if(!checked.source().equals(original.source()))throw new IOException();
        P8Document prior=P8Document.parse(base.bytes()),next=P8Document.parse(bytes);
        List<String> beforeNames=new ArrayList<>(),afterNames=new ArrayList<>();
        for(P8Document.Section section:prior.sections())if(!section.name.equals("gfx"))beforeNames.add(section.name);
        for(P8Document.Section section:next.sections())if(!section.name.equals("gfx"))afterNames.add(section.name);
        if(!beforeNames.equals(afterNames)||!prior.version().equals(next.version()))throw new IOException();
        for(String name:beforeNames)if(!Arrays.equals(prior.body(prior.uniqueSection(name)),next.body(next.uniqueSection(name))))throw new IOException();
        boolean[] touched=new boolean[16384];for(SpriteRegion r:a.editedRegions)for(int y=r.y;y<r.y+r.height;y++)for(int x=r.x;x<r.x+r.width;x++)touched[y*128+x]=true;
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)if(!touched[y*128+x]&&base.sheetPixel(x,y)!=a.resources.sheetPixel(x,y))throw new IOException();
        // Invalid state prerequisites may be an unfinished field edit; all frame data must remain valid.
        PlayAnimation seq=a.sequence();seq.validate();for(PlayAnimation.Frame f:seq.frames)if(f.region.width!=old.w||f.region.height!=old.h)throw new IOException();
        SpriteRegion first=seq.frames.get(0).region;if(first.x!=a.model.elements.get(element).sx||first.y!=a.model.elements.get(element).sy)throw new IOException();
        return a;
    }
}
