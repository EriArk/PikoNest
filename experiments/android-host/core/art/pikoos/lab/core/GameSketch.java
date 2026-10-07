package art.pikoos.lab.core;

import java.util.Arrays;

/** Resource-only illustration in draw order. Never evaluates arbitrary Lua. */
public final class GameSketch {
    public final int[] pixels=new int[16384];
    public boolean dynamic;
    public GameSketch(WorkshopCartridge cart,double seconds){
        Arrays.fill(pixels,1);GameUses uses=new GameUses(cart,0);P8Map map=cart.map();
        if(!uses.blocked.isEmpty())throw new IllegalArgumentException(uses.blocked);
        for(GameUses.Entry e:uses.entries){
            if(e.isCamera())continue;int[] camera=CameraUse.offset(e.view);
            if(e.isBackground()){
                if(camera==null&&!e.call.form.value(6).equals("0")){dynamic=true;continue;}
                for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                    int color=BackgroundLayer.pixel(cart,e.call.form,x,y,seconds,camera==null?0:camera[0]);
                    if(color!=0)pixels[y*128+x]=color;
                }continue;
            }
            if(camera==null){dynamic=true;continue;}
            int dx=e.x()-camera[0],dy=e.y()-camera[1];
            SpriteRegion r=e.animation!=null?e.animation.initial.frame(e.animation.initial.frameAt((int)(seconds*1000))).region:e.kind().equals("spr")?new SpriteRegion(e.values[0]%16*8,e.values[0]/16*8,8,8):e.kind().equals("sspr")?new SpriteRegion(e.values[0],e.values[1],e.values[2],e.values[3]):null;
            int width=r!=null?r.width:e.values[4]*8,height=r!=null?r.height:e.values[5]*8;
            for(int yy=Math.max(0,dy);yy<Math.min(128,dy+height);yy++)for(int xx=Math.max(0,dx);xx<Math.min(128,dx+width);xx++){
                int x=xx-dx,y=yy-dy,color;
                if(r!=null)color=cart.sheetPixel(r.x+x,r.y+y);
                else{int tile=map.tile(e.values[0]+x/8,e.values[1]+y/8);color=tile==0?0:cart.sheetPixel(tile%16*8+x%8,tile/16*8+y%8);}
                if(color!=0)pixels[yy*128+xx]=color;
            }
        }
    }
}
