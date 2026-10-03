package art.pikoos.lab.core;

/** A bounded ordinary tile-data proposal, independent of Android and game roles. */
public final class MapChange {
    private final boolean[] changed=new boolean[4096];
    public final int tile;
    public int count;
    private MapChange(int tile){if(tile<0||tile>255)throw new IllegalArgumentException("Invalid tile");this.tile=tile;}
    private static void point(int x,int y){
        if(x<0||x>127||y<0||y>31)throw new IllegalArgumentException("Инструмент меняет только верх карты: строки 0–31.");
    }
    public boolean changes(int x,int y){return y>=0&&y<32&&x>=0&&x<128&&changed[y*128+x];}
    public static MapChange rectangle(P8Map map,int x0,int y0,int x1,int y1,int tile){
        point(x0,y0);point(x1,y1);MapChange result=new MapChange(tile);
        for(int y=Math.min(y0,y1);y<=Math.max(y0,y1);y++)for(int x=Math.min(x0,x1);x<=Math.max(x0,x1);x++)
            if(map.tile(x,y)!=tile){result.changed[y*128+x]=true;result.count++;}
        return result;
    }
    /** Four-connected, same tile number, bounded by the independent upper map. */
    public static MapChange fill(P8Map map,int x,int y,int tile){
        point(x,y);MapChange result=new MapChange(tile);int old=map.tile(x,y);if(old==tile)return result;
        int[] queue=new int[4096];int head=0,tail=1;queue[0]=y*128+x;result.changed[queue[0]]=true;
        while(head<tail){
            int at=queue[head++],px=at%128,py=at/128;
            int[] neighbors={px>0?at-1:-1,px<127?at+1:-1,py>0?at-128:-1,py<31?at+128:-1};
            for(int next:neighbors)if(next>=0&&!result.changed[next]&&map.tile(next%128,next/128)==old){
                result.changed[next]=true;queue[tail++]=next;
            }
        }
        result.count=tail;return result;
    }
    public art.pikoos.p8.P8Document apply(P8Map map){return map.withTiles(changed,tile);}
}
