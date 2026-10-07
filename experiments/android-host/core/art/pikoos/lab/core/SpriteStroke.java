package art.pikoos.lab.core;

import java.io.*;
import java.util.Arrays;

/** One connected pixel stroke; no persistence or device input belongs here. */
public final class SpriteStroke {
    public final SpriteRegion scope;
    public final int color;
    public int x,y;
    private final String sourceHash;
    private final boolean[] painted;
    private WorkshopCartridge cached;
    public SpriteStroke(WorkshopCartridge cart,SpriteRegion scope,int color,int x,int y){
        if(color<0||color>15)throw new IllegalArgumentException("Invalid stroke color");
        scope.checkPixel(x,y);this.scope=scope;this.color=color;this.x=x;this.y=y;
        sourceHash=MapEditor.hash(cart.bytes());painted=new boolean[scope.width*scope.height];point(x,y);
    }
    public void point(int px,int py){
        scope.checkPixel(px,py);int x0=x,y0=y,x1=px,y1=py;
        // Use the same deterministic line rasterization as the pixel editor.
        if(x0>x1||(x0==x1&&y0>y1)){int t=x0;x0=x1;x1=t;t=y0;y0=y1;y1=t;}
        int dx=Math.abs(x1-x0),dy=-Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,error=dx+dy;
        while(true){painted[y0*scope.width+x0]=true;if(x0==x1&&y0==y1)break;
            int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}}
        x=px;y=py;cached=null;
    }
    public WorkshopCartridge preview(WorkshopCartridge cart){
        if(!sourceHash.equals(MapEditor.hash(cart.bytes())))throw new IllegalArgumentException("The project changed. Saved pixels were preserved.");
        if(cached==null){int[] values=new int[16384];Arrays.fill(values,-1);
            for(int i=0;i<painted.length;i++)if(painted[i])values[(scope.y+i/scope.width)*128+scope.x+i%scope.width]=color;
            cached=cart.withSheetPixels(values);}
        return cached;
    }
    public byte[] encode(){try{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
        o.writeInt(1);o.writeUTF(sourceHash);o.writeInt(scope.x);o.writeInt(scope.y);o.writeInt(scope.width);o.writeInt(scope.height);
        o.writeInt(color);o.writeInt(x);o.writeInt(y);for(boolean p:painted)o.writeBoolean(p);return b.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static SpriteStroke restore(byte[] bytes,WorkshopCartridge cart){try{
        if(bytes==null||bytes.length>17000)throw new IOException();DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes));
        if(i.readInt()!=1||!i.readUTF().equals(MapEditor.hash(cart.bytes())))throw new IOException();
        SpriteRegion r=new SpriteRegion(i.readInt(),i.readInt(),i.readInt(),i.readInt());
        SpriteStroke s=new SpriteStroke(cart,r,i.readInt(),i.readInt(),i.readInt());
        for(int n=0;n<s.painted.length;n++){int p=i.readUnsignedByte();if(p>1)throw new IOException();s.painted[n]=p==1;}
        if(i.available()!=0||!s.painted[s.y*r.width+s.x])throw new IOException();return s;
    }catch(Exception e){throw new IllegalArgumentException("Could not restore the stroke. Saved pixels are intact.");}}
}
