package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Optional authored world; default PICO-8 map memory, ordinary camera/map calls. */
public final class PlayWorld {
    public static final String[] CAMERAS={"Fixed view","Follow an element","Change rooms"};
    public int width=16,height=16,camera,target=-1,x,y;
    public boolean tiles=true;
    public final boolean[] walls=new boolean[256];
    /** A cell binding, independent of every other use of its closed tile. */
    public static final class Passage {
        public int x,y,tile,target,on=1;
        public Passage copy(){Passage p=new Passage();p.x=x;p.y=y;p.tile=tile;p.target=target;p.on=on;return p;}
    }
    public final List<Passage> passages=new ArrayList<>();
    public Passage passage(int x,int y){for(Passage p:passages)if(p.x==x&&p.y==y)return p;return null;}
    public void validatePassages(PlayModel model){
        if(passages.size()>32)throw new IllegalArgumentException("This editor currently supports up to 32 passage cells.");
        Set<Integer> cells=new HashSet<>();
        for(Passage p:passages){
            if(p.x<0||p.x>=width||p.y<0||p.y>=height||!cells.add(p.y*128+p.x))throw new IllegalArgumentException("Keep each passage cell inside the world, or remove its link first.");
            if(p.tile<1||p.tile>255||!walls[p.tile])throw new IllegalArgumentException("A passage needs a wall tile. Remove its switch link before making that tile walkable.");
            if(p.target<0||p.target>=model.elements.size()||!PlayModel.isSwitch(model.elements.get(p.target)))throw new IllegalArgumentException("A passage uses this switch. Remove or change its link in World & camera first.");
            if(p.on<0||p.on>1)throw new IllegalArgumentException("Choose On or Off for the passage.");
        }
    }
    void initPassages(StringBuilder b){
        if(passages.isEmpty())return;
        b.append(" passage_count=").append(passages.size()).append('\n');
        for(int n=0;n<passages.size();n++){Passage p=passages.get(n);String[] names={"x","y","tile","target","on"};int[] v={p.x,p.y,p.tile,p.target,p.on};for(int f=0;f<v.length;f++)b.append(" passage").append(n+1).append('_').append(names[f]).append('=').append(v[f]).append('\n');}
        b.append(" world_passages(true)\n");
    }
    void passageFunctions(StringBuilder b,PlayModel model){
        if(passages.isEmpty())return;
        b.append("function world_occupied(x,y)\n");
        for(int i=0;i<model.elements.size();i++)if(model.elements.get(i).movement!=0){String e="sprite"+(i+1);b.append(" if ").append(e).append("_visible==1 and ").append(e).append("_x<x+8 and x<").append(e).append("_x+").append(e).append("_w and ").append(e).append("_y<y+8 and y<").append(e).append("_y+").append(e).append("_h then return true end\n");}
        b.append(" return false\nend\n\nfunction world_passages(reset)\n");
        for(int n=0;n<passages.size();n++){
            Passage p=passages.get(n);String a="passage"+(n+1);
            b.append(" if sprite").append(p.target+1).append("_on==").append(a).append("_on then\n  mset(").append(a).append("_x,").append(a).append("_y,0)\n elseif reset or not world_occupied(").append(a).append("_x*8,").append(a).append("_y*8) then\n  mset(").append(a).append("_x,").append(a).append("_y,").append(a).append("_tile)\n end\n");
        }
        b.append("end\n\n");
    }
    void encodePassages(DataOutputStream out)throws IOException{out.writeInt(passages.size());for(Passage p:passages)for(int v:new int[]{p.x,p.y,p.tile,p.target,p.on})out.writeInt(v);}
    void decodePassages(DataInputStream in)throws IOException{int count=in.readInt();if(count<0||count>32)throw new IOException();for(int n=0;n<count;n++){Passage p=new Passage();p.x=in.readInt();p.y=in.readInt();p.tile=in.readInt();p.target=in.readInt();p.on=in.readInt();passages.add(p);}}
    public int wallCount(){int count=0;for(boolean wall:walls)if(wall)count++;return count;}
    public PlayWorld copy(){PlayWorld w=new PlayWorld();w.width=width;w.height=height;w.camera=camera;w.target=target;w.x=x;w.y=y;w.tiles=tiles;System.arraycopy(walls,0,w.walls,0,walls.length);for(Passage p:passages)w.passages.add(p.copy());return w;}
    public void validate(int elements){
        if(walls[0])throw new IllegalArgumentException("Tile 0 is empty and cannot be a wall.");
        if(width<16||width>128||height<16||height>32||width%16!=0||height%16!=0)throw new IllegalArgumentException("Game view currently supports 1 to 8 screens across and 1 to 2 down. Shared map rows remain in All tools.");
        if(camera<0||camera>2||target< -1||target>=elements||camera!=0&&target<0)throw new IllegalArgumentException("Choose an element for the camera to follow, or use Fixed view.");
        if(x<0||x>width*8-128||y<0||y>height*8-128)throw new IllegalArgumentException("Keep the fixed view inside the world.");
    }
    public int offset(PlayModel model,boolean horizontal){
        if(camera==0)return horizontal?x:y;
        PlayModel.Element e=model.elements.get(target);
        int center=(horizontal?e.x+e.w/2:e.y+e.h/2),limit=(horizontal?width:height)*8-128;
        return Math.max(0,Math.min(limit,camera==1?center-64:Math.floorDiv(center,128)*128));
    }
    void init(StringBuilder b){
        String[] names={"width","height","camera","target","x","y","tiles"};int[] values={width,height,camera,target,x,y,tiles?1:0};
        for(int n=0;n<names.length;n++)b.append(" world_").append(names[n]).append('=').append(values[n]).append('\n');
    }
    /** Wall types are ordinary Lua data; no sprite flag is claimed or overwritten. */
    void initWalls(StringBuilder b){
        if(wallCount()==0)return;
        b.append(" world_walls={");boolean comma=false;
        for(int t=1;t<256;t++)if(walls[t]){if(comma)b.append(',');b.append('[').append(t).append("]=true");comma=true;}
        b.append("}\n");
    }
    void functions(StringBuilder b){
        if(wallCount()==0)return;
        b.append("function world_blocked(x,y,w,h)\n")
         .append(" for ty=flr(y/8),flr((y+h-1)/8) do\n")
         .append("  for tx=flr(x/8),flr((x+w-1)/8) do\n")
         .append("   if world_walls[mget(tx,ty)] then return true end\n")
         .append("  end\n end\n return false\nend\n\n")
         .append("function world_move(x,y,w,h,dx,dy)\n")
         .append(" local sx=dx==0 and 0 or sgn(dx)\n local sy=dy==0 and 0 or sgn(dy)\n")
         .append(" for step=1,max(abs(dx),abs(dy)) do\n")
         .append("  local nx=mid(0,x+sx,world_width*8-w)\n")
         .append("  local ny=mid(0,y+sy,world_height*8-h)\n")
         .append("  if dx~=0 and world_blocked(nx+(dx>0 and w-1 or 0),y,1,h) then break end\n")
         .append("  if dy~=0 and world_blocked(x,ny+(dy>0 and h-1 or 0),w,1) then break end\n")
         .append("  x=nx y=ny\n end\n return x,y\nend\n\n");
    }
    void validateStarts(PlayModel model,WorkshopCartridge cart){
        validatePassages(model);P8Map map=cart.map();
        for(Passage p:passages)if(map.tile(p.x,p.y)!=p.tile)throw new IllegalArgumentException("A passage cell changed. Remove its switch link before replacing its tile.");
        if(wallCount()==0)return;
        for(int n=0;n<model.elements.size();n++){
            PlayModel.Element e=model.elements.get(n);if(e.movement==0)continue;
            if(e.x<0||e.y<0||e.x>width*8-e.w||e.y>height*8-e.h)
                throw new IllegalArgumentException("Move element "+(n+1)+" inside the world before testing or saving walls.");
            for(int y=e.y/8;y<=(e.y+e.h-1)/8;y++)for(int x=e.x/8;x<=(e.x+e.w-1)/8;x++)if(walls[map.tile(x,y)]&&(passage(x,y)==null||model.startsOn(passage(x,y).target)!=(passage(x,y).on==1)))
                throw new IllegalArgumentException("Element "+(n+1)+" starts inside a wall. Move it in Game view or make that tile walkable.");
        }
    }
    void draw(StringBuilder b,PlayModel model){
        b.append(" camera(").append(axis(model,true)).append(',').append(axis(model,false)).append(")\n");
        if(tiles)b.append(" map(0,0,0,0,world_width,world_height)\n");
    }
    private String axis(PlayModel model,boolean horizontal){
        String axis=horizontal?"x":"y",size=horizontal?"width":"height";
        if(camera==0)return "world_"+axis;
        PlayModel.Element e=model.elements.get(target);String center="sprite"+(target+1)+"_"+axis+"+"+(horizontal?e.w:e.h)/2;
        return "mid(0,"+(camera==1?center+"-64":"flr(("+center+")/128)*128")+",world_"+size+"*8-128)";
    }
    static PlayWorld read(Map<String,Integer> v,String init){
        if(!v.containsKey("world_width"))return null;
        PlayWorld w=new PlayWorld();w.width=v.get("world_width");w.height=v.get("world_height");w.camera=v.get("world_camera");w.target=v.get("world_target");w.x=v.get("world_x");w.y=v.get("world_y");int tiles=v.get("world_tiles");if(tiles<0||tiles>1)throw new IllegalArgumentException();w.tiles=tiles==1;
        Matcher row=Pattern.compile("(?m)^ world_walls=\\{([^}]+)\\}$").matcher(init);
        if(row.find()){
            String[] items=row.group(1).split(",",-1);
            for(String item:items){Matcher t=Pattern.compile("\\[([0-9]+)\\]=true").matcher(item);if(!t.matches())throw new IllegalArgumentException();int tile=Integer.parseInt(t.group(1));if(tile<1||tile>255||w.walls[tile])throw new IllegalArgumentException();w.walls[tile]=true;}
            if(row.find())throw new IllegalArgumentException();
        }
        int count=v.getOrDefault("passage_count",0);if(count<0||count>32)throw new IllegalArgumentException();
        for(int n=1;n<=count;n++){String a="passage"+n+"_";Passage p=new Passage();p.x=v.get(a+"x");p.y=v.get(a+"y");p.tile=v.get(a+"tile");p.target=v.get(a+"target");p.on=v.get(a+"on");w.passages.add(p);}
        return w;
    }
    void encode(DataOutputStream out)throws IOException{for(int v:new int[]{width,height,camera,target,x,y})out.writeInt(v);out.writeBoolean(tiles);}
    static PlayWorld decode(DataInputStream in)throws IOException{PlayWorld w=new PlayWorld();w.width=in.readInt();w.height=in.readInt();w.camera=in.readInt();w.target=in.readInt();w.x=in.readInt();w.y=in.readInt();w.tiles=in.readBoolean();return w;}
}
