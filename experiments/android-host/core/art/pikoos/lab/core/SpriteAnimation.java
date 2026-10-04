package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/** A proposal made of ordinary sheet regions and elapsed-time draw code. No runtime metadata. */
public final class SpriteAnimation {
    public static final int MAX_FRAMES=32; // Initial workshop limit, not a PICO-8 limit.
    public static final class Frame {
        public final SpriteRegion region; public final int millis;
        Frame(SpriteRegion region,int millis){this.region=region;this.millis=millis;}
    }
    private final ArrayList<Frame> frames=new ArrayList<>();
    public int selected,field,x=56,y=56,previewLine;
    public boolean loop=true,playing,review;
    private int elapsed;
    public SpritePlacement picker;
    public SpriteAnimation(SpriteRegion region){frames.add(new Frame(region,250));x=(128-region.width)/2;y=(128-region.height)/2;}
    public int count(){return frames.size();}
    public Frame frame(int index){return frames.get(index);}
    public int previewWidth(){int size=1;for(Frame f:frames)size=Math.max(size,f.region.width);return size;}
    public int previewHeight(){int size=1;for(Frame f:frames)size=Math.max(size,f.region.height);return size;}
    public int duration(){int total=0;for(Frame f:frames)total+=f.millis;return total;}
    public int elapsed(){return elapsed;}
    public int frameAt(int millis){
        int t=Math.max(0,millis);if(loop)t%=duration();
        for(int n=0;n<count();n++){t-=frame(n).millis;if(t<0)return n;}return count()-1;
    }
    public int visibleFrame(){return frameAt(elapsed);}
    public void select(int index){selected=Math.max(0,Math.min(count()-1,index));seekSelected();}
    private void seekSelected(){playing=false;elapsed=0;for(int n=0;n<selected;n++)elapsed+=frame(n).millis;}
    public void toggle(){if(!playing&&!loop&&elapsed>=duration())elapsed=0;playing=!playing;}
    public void stop(){playing=false;elapsed=0;}
    public void advance(int millis){
        if(!playing||millis<=0||review||picker!=null)return;
        long next=(long)elapsed+millis;
        if(loop)elapsed=(int)(next%duration());
        else{elapsed=(int)Math.min(next,duration());if(elapsed==duration())playing=false;}
    }
    public void duplicate(){
        if(count()==MAX_FRAMES)throw new IllegalArgumentException("В этой версии до 32 кадров. Исходный код сохранён.");
        frames.add(selected+1,frame(selected));select(selected+1);
    }
    public void remove(){if(count()==1)throw new IllegalArgumentException("Оставь хотя бы один кадр");frames.remove(selected);select(selected);}
    public void reorder(int delta){int to=Math.max(0,Math.min(count()-1,selected+delta));Collections.swap(frames,selected,to);select(to);}
    public void durationStep(int delta){Frame f=frame(selected);frames.set(selected,new Frame(f.region,Math.max(50,Math.min(5000,f.millis+delta*50))));seekSelected();}
    public void beginPick(boolean position){
        playing=false;picker=new SpritePlacement(frame(selected).region);picker.x=x;picker.y=y;if(position)picker.phase=2;
    }
    public void acceptPick(){
        if(picker.phase==0){picker.next();return;}
        if(picker.phase==1){Frame f=frame(selected);frames.set(selected,new Frame(picker.source(),f.millis));}
        else{x=picker.x;y=picker.y;}
        picker=null;seekSelected();
    }
    public void cancelPick(){if(picker.phase==1)picker.phase=0;else picker=null;}
    public void change(int delta){
        if(field==0)select(selected+delta);if(field==2)durationStep(delta);
        if(field==4)reorder(delta);if(field==6){loop=!loop;seekSelected();}
    }
    public void choose(){
        if(field==0||field==1)beginPick(false);if(field==2)durationStep(1);
        if(field==3)duplicate();if(field==5)remove();if(field==6){loop=!loop;seekSelected();}
        if(field==7)beginPick(true);if(field==8){playing=false;review=true;previewLine=0;}
    }
    public String label(int row){
        Frame f=frame(selected);
        switch(row){
            case 0:return "Кадр "+(selected+1)+" / "+count();
            case 1:return "Область "+f.region.width+"×"+f.region.height;
            case 2:return "Длительность: "+f.millis+" мс";
            case 3:return "+ Копия кадра";
            case 4:return "Порядок: ← раньше / позже →";
            case 5:return "Убрать этот кадр";
            case 6:return loop?"Повтор: по кругу":"Повтор: один раз";
            case 7:return "Место: "+x+", "+y;
            default:return "Просмотреть Lua";
        }
    }
    public static void validateSource(String source){
        LuaContext context=new LuaContext(source);String mask=context.masked();
        if(Pattern.compile("(?m)^\\s*#include\\b|\\b(?:_G|_ENV)\\b").matcher(mask).find())
            throw new IllegalArgumentException("Для includes/изменённого окружения вставка пока недоступна. Исходник сохранён.");
        LuaSymbols symbols=new LuaSymbols(source);
        for(String api:new String[]{"time","sspr"})if(symbols.shadows(api)||context.defines(api))
            throw new IllegalArgumentException("API "+api+" переопределён. Анимация не вставлена.");
    }
    // Hex literals express exact 16.16 boundaries; no time*fps multiplication overflow.
    private static String seconds(int millis){int fixed=(int)((long)millis*65536/1000);return String.format(Locale.ROOT,"0x%x.%04x",fixed>>16,fixed&65535);}
    public String code(){
        StringBuilder b=new StringBuilder("do\n -- animation: sx,sy,w,h,end_seconds\n local frames={\n");int total=0;
        for(Frame f:frames){SpriteRegion r=f.region;total+=f.millis;b.append("  {").append(r.x).append(',').append(r.y).append(',').append(r.width).append(',').append(r.height).append(',').append(seconds(total)).append("},\n");}
        b.append(" }\n local elapsed=time()\n");if(loop)b.append(" elapsed%=").append(seconds(total)).append('\n');
        b.append(" for i=1,#frames do\n  local f=frames[i]\n  if elapsed<f[5] or i==#frames then\n   sspr(f[1],f[2],f[3],f[4],\n    ").append(x).append(',').append(y).append(")\n   break\n  end\n end\nend\n");
        return b.toString();
    }
    public void write(DataOutputStream out)throws IOException{
        out.writeInt(count());for(Frame f:frames){SpriteRegion r=f.region;for(int n:new int[]{r.x,r.y,r.width,r.height,f.millis})out.writeInt(n);}
        for(int n:new int[]{selected,field,x,y,previewLine,elapsed})out.writeInt(n);
        out.writeBoolean(loop);out.writeBoolean(review);out.writeBoolean(picker!=null);if(picker!=null)picker.write(out);
    }
    public static SpriteAnimation read(DataInputStream in)throws IOException{
        int count=in.readInt();if(count<1||count>MAX_FRAMES)throw new IOException("animation count");
        SpriteAnimation a=new SpriteAnimation(new SpriteRegion(0,0,8,8));a.frames.clear();
        for(int n=0;n<count;n++){
            SpriteRegion r=new SpriteRegion(in.readInt(),in.readInt(),in.readInt(),in.readInt());int ms=in.readInt();
            if(ms<50||ms>5000||ms%50!=0)throw new IOException("animation duration");a.frames.add(new Frame(r,ms));
        }
        a.selected=in.readInt();a.field=in.readInt();a.x=in.readInt();a.y=in.readInt();a.previewLine=in.readInt();a.elapsed=in.readInt();
        a.loop=in.readBoolean();a.review=in.readBoolean();if(in.readBoolean())a.picker=SpritePlacement.read(in);
        if(a.selected<0||a.selected>=count||a.field<0||a.field>8||a.x<-127||a.x>127||a.y<-127||a.y>127||a.previewLine<0||a.previewLine>a.code().length()||a.elapsed<0||a.elapsed>a.duration()||(a.picker!=null&&(a.review||a.picker.phase>2)))throw new IOException("animation state");
        return a; // Restore paused at the same elapsed time; no background playback.
    }
}
