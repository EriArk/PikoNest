package art.pikoos.lab.core;

import java.io.*;

/** A reversible proposal for one ordinary sspr call. Never mutates the sprite sheet. */
public final class SpritePlacement {
    public int phase,ax,ay,bx,by,x,y,step=8;
    public SpritePlacement(SpriteRegion source){
        ax=source.x;ay=source.y;bx=ax+source.width-1;by=ay+source.height-1;
        x=(128-source.width)/2;y=(128-source.height)/2;
    }
    private static int clamp(int n,int lo,int hi){return Math.max(lo,Math.min(hi,n));}
    public SpriteRegion source(){return new SpriteRegion(Math.min(ax,bx),Math.min(ay,by),Math.abs(bx-ax)+1,Math.abs(by-ay)+1);}
    public void move(int dx,int dy){
        if(phase==0){
            int width=source().width,height=source().height;
            ax=clamp(ax+dx*step,0,127);ay=clamp(ay+dy*step,0,127);
            bx=Math.min(127,ax+width-1);by=Math.min(127,ay+height-1);
        }else if(phase==1){bx=clamp(bx+dx*step,0,127);by=clamp(by+dy*step,0,127);}
        else if(phase==2){x=clamp(x+dx*step,-127,127);y=clamp(y+dy*step,-127,127);}
    }
    public void toggleStep(){step=step==8?1:8;}
    public void point(int px,int py){
        px=clamp(px,0,127);py=clamp(py,0,127);
        if(phase==0){int width=source().width,height=source().height;ax=px;ay=py;bx=Math.min(127,ax+width-1);by=Math.min(127,ay+height-1);}
        else if(phase==1){bx=px;by=py;}
        else if(phase==2){x=px;y=py;}
    }
    public void next(){if(phase<3)phase++;}
    public boolean back(){if(phase==0)return true;phase--;return false;}
    public LuaInsert form(){
        SpriteRegion r=source();LuaInsert i=new LuaInsert();i.choose(18);i.screen=LuaInsert.Screen.FIELDS;
        int[] values={r.x,r.y,r.width,r.height,x,y};
        for(int n=0;n<values.length;n++)i.set(n,Integer.toString(values[n]));
        return i;
    }
    public String code(){return form().code().trim();}
    /** Default draw state: no camera/palette changes, colour 0 transparent. */
    public int pixel(WorkshopCartridge cart,int px,int py){
        SpriteRegion r=source();int sx=px-x,sy=py-y;
        return sx<0||sy<0||sx>=r.width||sy>=r.height?0:cart.pixel(r,sx,sy);
    }
    public void write(DataOutputStream out)throws IOException{
        for(int n:new int[]{phase,ax,ay,bx,by,x,y,step})out.writeInt(n);
    }
    public static SpritePlacement read(DataInputStream in)throws IOException{
        SpritePlacement p=new SpritePlacement(new SpriteRegion(0,0,8,8));
        p.phase=in.readInt();p.ax=in.readInt();p.ay=in.readInt();p.bx=in.readInt();p.by=in.readInt();p.x=in.readInt();p.y=in.readInt();p.step=in.readInt();
        if(p.phase<0||p.phase>3||p.ax<0||p.ay<0||p.bx<0||p.by<0||p.ax>127||p.ay>127||p.bx>127||p.by>127||p.x<-127||p.x>127||p.y<-127||p.y>127||(p.step!=1&&p.step!=8))throw new IOException("sprite proposal");
        return p;
    }
}
