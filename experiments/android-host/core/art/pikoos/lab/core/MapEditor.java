package art.pikoos.lab.core;

/** Cursor movement never paints. Picker changes remain a proposal until confirmed. */
public final class MapEditor {
    public int x,y,tile=1,choice=1;
    public boolean picking;
    public int left(){return Math.min(112,Math.max(0,x-7));}
    public int top(){return Math.min(48,Math.max(0,y-7));}
    public void move(int dx,int dy){
        if(picking)choice=clamp(choice%16+dx,15)+16*clamp(choice/16+dy,15);
        else{x=clamp(x+dx,127);y=clamp(y+dy,63);}
    }
    public void begin(){choice=tile;picking=true;}
    public void accept(){tile=choice;picking=false;}
    public void cancel(){picking=false;choice=tile;}
    private static int clamp(int n,int max){return Math.max(0,Math.min(max,n));}
}
