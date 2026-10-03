package art.pikoos.lab.core;

/** Pixel selection and placement intent inside one editor region. Never writes by itself. */
public final class SpriteMove {
    public final SpriteRegion scope;
    public int phase,anchorX,anchorY,x,y,destinationX,destinationY;
    public SpriteMove(SpriteRegion scope,int x,int y){this.scope=scope;point(x,y);}
    private int clamp(int n,int max){return Math.max(0,Math.min(max,n));}
    public SpriteRegion source(){return new SpriteRegion(scope.x+Math.min(anchorX,x),scope.y+Math.min(anchorY,y),Math.abs(x-anchorX)+1,Math.abs(y-anchorY)+1);}
    public SpriteRegion destination(){SpriteRegion r=source();return new SpriteRegion(scope.x+destinationX,scope.y+destinationY,r.width,r.height);}
    public void point(int px,int py){
        if(phase==2){SpriteRegion r=source();destinationX=clamp(px,scope.width-r.width);destinationY=clamp(py,scope.height-r.height);}
        else{x=clamp(px,scope.width-1);y=clamp(py,scope.height-1);}
    }
    public void step(int dx,int dy){point((phase==2?destinationX:x)+dx,(phase==2?destinationY:y)+dy);}
    public void next(){
        if(phase==0){anchorX=x;anchorY=y;phase=1;}
        else if(phase==1){SpriteRegion r=source();destinationX=r.x-scope.x;destinationY=r.y-scope.y;phase=2;}
    }
    public boolean back(){if(phase==0)return false;phase--;return true;}
    public WorkshopCartridge preview(WorkshopCartridge cart){return phase==2?cart.moved(source(),destination()):cart;}
    public String encode(){return phase+","+anchorX+","+anchorY+","+x+","+y+","+destinationX+","+destinationY;}
    public static SpriteMove restore(SpriteRegion scope,String encoded){
        String[] values=encoded.split(",",-1);if(values.length!=7)throw new IllegalArgumentException("Invalid move draft");
        int[] n=new int[7];for(int i=0;i<7;i++)n[i]=Integer.parseInt(values[i]);
        if(n[0]<0||n[0]>2)throw new IllegalArgumentException("Invalid move phase");
        scope.checkPixel(n[1],n[2]);scope.checkPixel(n[3],n[4]);
        SpriteMove m=new SpriteMove(scope,n[3],n[4]);m.phase=n[0];m.anchorX=n[1];m.anchorY=n[2];m.destinationX=n[5];m.destinationY=n[6];
        if(m.phase==2&&(n[5]<0||n[6]<0||n[5]+m.source().width>scope.width||n[6]+m.source().height>scope.height))throw new IllegalArgumentException("Move outside region");
        return m;
    }
}
