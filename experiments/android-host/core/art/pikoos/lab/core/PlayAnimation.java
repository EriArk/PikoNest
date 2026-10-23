package art.pikoos.lab.core;

import java.io.*;
import java.util.*;

/** A bounded Game-view sequence; all frames are ordinary sprite-sheet rectangles. */
public final class PlayAnimation {
    public static final String[] MODES={"Loop","When moving","When on"};
    public static final int MAX_FRAMES=8; // Editor scope, not a console limit.
    public static final class Frame {
        public SpriteRegion region;
        public int ticks;
        public Frame(SpriteRegion r,int ticks){region=r;this.ticks=ticks;}
    }
    public int mode;
    public final List<Frame> frames=new ArrayList<>();
    public PlayAnimation(SpriteRegion r){frames.add(new Frame(r,6));}
    public PlayAnimation copy(){PlayAnimation a=new PlayAnimation(frames.get(0).region);a.frames.clear();a.mode=mode;for(Frame f:frames)a.frames.add(new Frame(f.region,f.ticks));return a;}
    public int duration(){int sum=0;for(Frame f:frames)sum+=f.ticks;return sum;}
    public int frameAt(int tick){int t=Math.floorMod(tick,duration());for(int n=0;n<frames.size();n++){t-=frames.get(n).ticks;if(t<0)return n;}return 0;}
    public void validate(){
        if(mode<0||mode>=MODES.length||frames.isEmpty()||frames.size()>MAX_FRAMES)throw new IllegalArgumentException("Invalid animation sequence");
        for(Frame f:frames)if(f.region==null||f.ticks<1||f.ticks>150)throw new IllegalArgumentException("Frame duration must be 1 to 150 game updates (up to 5 seconds).");
    }
    void init(StringBuilder b,String id){
        b.append(' ').append(id).append("_animation=").append(mode).append('\n');
        b.append(' ').append(id).append("_frames=").append(frames.size()).append('\n');
        for(int n=0;n<frames.size();n++){
            Frame f=frames.get(n);SpriteRegion r=f.region;String key=id+"_f"+n;
            String[] names={"sx","sy","w","h","ticks"};int[] values={r.x,r.y,r.width,r.height,f.ticks};
            for(int k=0;k<names.length;k++)b.append(' ').append(key).append('_').append(names[k]).append('=').append(values[k]).append('\n');
        }
        b.append(' ').append(id).append("_clock=0\n ").append(id).append("_active=0\n");
    }
    void beforeMovement(StringBuilder b,String id){if(mode==1)b.append(" local ").append(id).append("_oldx=").append(id).append("_x\n local ").append(id).append("_oldy=").append(id).append("_y\n");}
    void update(StringBuilder b,String id){
        String condition=mode==0?"true":mode==1?id+"_x~="+id+"_oldx or "+id+"_y~="+id+"_oldy":id+"_on==1";
        b.append(" if ").append(condition).append(" then\n  if ").append(id).append("_active==1 then ").append(id).append("_clock=(").append(id).append("_clock+1)%").append(duration()).append(" end\n  ").append(id).append("_active=1\n else\n  ").append(id).append("_clock=0\n  ").append(id).append("_active=0\n end\n");
    }
    void draw(StringBuilder b,String id){
        int end=0;
        for(int n=0;n<frames.size();n++){
            Frame f=frames.get(n);end+=f.ticks;SpriteRegion r=f.region;
            if(n==0)b.append("  if ").append(id).append("_clock<").append(end).append(" then\n");
            else if(n==frames.size()-1)b.append("  else\n");
            else b.append("  elseif ").append(id).append("_clock<").append(end).append(" then\n");
            b.append("   sspr(").append(r.x).append(',').append(r.y).append(',').append(r.width).append(',').append(r.height).append(',').append(id).append("_x,").append(id).append("_y)\n");
        }
        b.append("  end\n");
    }
    static PlayAnimation read(Map<String,Integer> values,String id){
        if(!values.containsKey(id+"_animation"))return null;
        int count=values.get(id+"_frames");if(count<1||count>MAX_FRAMES)throw new IllegalArgumentException("Invalid frame count");
        PlayAnimation a=new PlayAnimation(new SpriteRegion(0,0,8,8));a.frames.clear();a.mode=values.get(id+"_animation");
        for(int n=0;n<count;n++){String k=id+"_f"+n;a.frames.add(new Frame(new SpriteRegion(values.get(k+"_sx"),values.get(k+"_sy"),values.get(k+"_w"),values.get(k+"_h")),values.get(k+"_ticks")));}
        a.validate();return a;
    }
    void encode(DataOutputStream o)throws IOException{o.writeInt(mode);o.writeInt(frames.size());for(Frame f:frames){SpriteRegion r=f.region;for(int v:new int[]{r.x,r.y,r.width,r.height,f.ticks})o.writeInt(v);}}
    static PlayAnimation decode(DataInputStream i)throws IOException{
        PlayAnimation a=new PlayAnimation(new SpriteRegion(0,0,8,8));a.frames.clear();a.mode=i.readInt();int count=i.readInt();if(count<1||count>MAX_FRAMES)throw new IOException();
        for(int n=0;n<count;n++)a.frames.add(new Frame(new SpriteRegion(i.readInt(),i.readInt(),i.readInt(),i.readInt()),i.readInt()));a.validate();return a;
    }
}
