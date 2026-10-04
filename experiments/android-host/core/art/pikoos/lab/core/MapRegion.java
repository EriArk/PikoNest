package art.pikoos.lab.core;

import java.util.Arrays;

/** Snapshot-based rectangular map editing. Drafts never mutate the cartridge. */
public final class MapRegion {
    public enum Operation { COPY, MOVE, CLEAR }
    public final Operation operation;
    private final WorkshopCartridge base;
    private final String digest;
    public final int anchorX,anchorY;
    public int x,y,dx,dy,peekX,peekY;
    /** 0 source corner; 1 destination; 2 review; 3 shared graphics review. */
    public int phase;
    private String cachedKey="";
    private Proposal cached;
    public MapRegion(WorkshopCartridge cart,Operation operation,int x,int y){
        check(x,y);cart.map();base=cart;digest=MapEditor.hash(cart.bytes());this.operation=operation;
        anchorX=this.x=dx=peekX=x;anchorY=this.y=dy=peekY=y;
    }
    public int left(){return Math.min(anchorX,x);}
    public int top(){return Math.min(anchorY,y);}
    public int width(){return Math.abs(anchorX-x)+1;}
    public int height(){return Math.abs(anchorY-y)+1;}
    public int viewLeft(){return clamp((phase==0?x:phase==1?dx:peekX)-7,112);}
    public int viewTop(){return clamp((phase==0?y:phase==1?dy:peekY)-7,48);}
    public void point(int px,int py){
        if(phase==0){x=clamp(px,127);y=clamp(py,63);}
        else if(phase==1){dx=clamp(px,128-width());dy=clamp(py,64-height());}
        else{peekX=clamp(px,127);peekY=clamp(py,63);}
    }
    public void move(int mx,int my){point((phase==0?x:phase==1?dx:peekX)+mx,(phase==0?y:phase==1?dy:peekY)+my);}
    /** True only when this deliberate confirmation may be committed. */
    public boolean confirm(){
        if(phase==0){dx=left();dy=top();peekX=dx;peekY=dy;phase=operation==Operation.CLEAR?2:1;return false;}
        if(phase==1){peekX=dx;peekY=dy;phase=2;return false;}
        if(phase==2&&proposal().sharedCells>0){phase=3;return false;}
        return true;
    }
    /** True means leave the operation without edits. */
    public boolean back(){if(phase==0)return true;if(phase==3)phase=2;else if(phase==2)phase=operation==Operation.CLEAR?0:1;else phase=0;return false;}
    public WorkshopCartridge candidate(WorkshopCartridge cart){
        if(!digest.equals(MapEditor.hash(cart.bytes())))throw new IllegalArgumentException("Картридж изменился. Выбери область заново.");
        Proposal p=proposal();
        if(phase<2||p.sharedCells>0&&phase!=3)throw new IllegalArgumentException("Сначала проверь изменения спрайт-листа.");
        return p.cart;
    }
    public boolean sourceContains(int px,int py){return inside(px,py,left(),top(),width(),height());}
    public boolean targetContains(int px,int py){return operation!=Operation.CLEAR&&inside(px,py,dx,dy,width(),height());}
    private static boolean inside(int x,int y,int l,int t,int w,int h){return x>=l&&y>=t&&x<l+w&&y<t+h;}
    public WorkshopCartridge original(){return base;}
    public Proposal proposal(){
        String key=x+":"+y+":"+dx+":"+dy;
        if(key.equals(cachedKey))return cached;
        P8Map map=base.map();int[] cells=new int[8192];Arrays.fill(cells,-1);
        if(operation!=Operation.COPY)for(int yy=top();yy<top()+height();yy++)for(int xx=left();xx<left()+width();xx++)cells[yy*128+xx]=0;
        int overwritten=0;
        if(operation!=Operation.CLEAR)for(int yy=0;yy<height();yy++)for(int xx=0;xx<width();xx++){
            int value=map.tile(left()+xx,top()+yy),tx=dx+xx,ty=dy+yy;
            // Selection phase has no destination yet; callers render the original until confirm.
            if(tx>=128||ty>=64)continue;
            cells[ty*128+tx]=value;
            if(map.tile(tx,ty)!=0&&map.tile(tx,ty)!=value)overwritten++;
        }
        int count=0,shared=0,pixels=0;
        for(int i=0;i<cells.length;i++)if(cells[i]>=0){
            int old=map.tile(i%128,i/128);
            if(cells[i]==old){cells[i]=-1;continue;}
            count++;
            if(i>=4096){shared++;if((old&15)!=(cells[i]&15))pixels++;if((old>>4)!=(cells[i]>>4))pixels++;}
        }
        cached=new Proposal(base.withMapCells(cells),cells,count,shared,pixels,overwritten);cachedKey=key;return cached;
    }
    public static final class Proposal {
        public final WorkshopCartridge cart;
        private final int[] cells;
        public final int count,sharedCells,pixels,overwritten;
        private Proposal(WorkshopCartridge cart,int[] cells,int count,int shared,int pixels,int overwritten){this.cart=cart;this.cells=cells;this.count=count;sharedCells=shared;this.pixels=pixels;this.overwritten=overwritten;}
        public boolean changes(int x,int y){return x>=0&&x<128&&y>=0&&y<64&&cells[y*128+x]>=0;}
    }
    public String encode(){return "2;"+operation+";"+phase+";"+anchorX+";"+anchorY+";"+x+";"+y+";"+dx+";"+dy+";"+peekX+";"+peekY+";"+digest;}
    public static MapRegion restore(String encoded,WorkshopCartridge cart){
        try{
            String[] f=encoded.split(";",-1);if(f.length!=12||!f[0].equals("2")||!f[11].equals(MapEditor.hash(cart.bytes())))throw new IllegalArgumentException();
            MapRegion r=new MapRegion(cart,Operation.valueOf(f[1]),Integer.parseInt(f[3]),Integer.parseInt(f[4]));
            r.phase=Integer.parseInt(f[2]);r.x=Integer.parseInt(f[5]);r.y=Integer.parseInt(f[6]);r.dx=Integer.parseInt(f[7]);r.dy=Integer.parseInt(f[8]);r.peekX=Integer.parseInt(f[9]);r.peekY=Integer.parseInt(f[10]);
            check(r.x,r.y);check(r.dx,r.dy);check(r.peekX,r.peekY);
            if(r.phase<0||r.phase>3||r.operation==Operation.CLEAR&&r.phase==1||r.phase>0&&(r.dx+r.width()>128||r.dy+r.height()>64))throw new IllegalArgumentException();
            if(r.phase==3&&r.proposal().sharedCells==0)throw new IllegalArgumentException();
            return r;
        }catch(Exception e){throw new IllegalArgumentException("Не удалось восстановить область. Картридж сохранён; выдели её заново.");}
    }
    private static int clamp(int n,int max){return Math.max(0,Math.min(n,max));}
    private static void check(int x,int y){if(x<0||x>127||y<0||y>63)throw new IllegalArgumentException("Клетка вне карты 128×64");}
}
