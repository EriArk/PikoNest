package art.pikoos.lab.core;

import java.io.*;
import java.util.Map;

/** Optional authored world; default PICO-8 map memory, ordinary camera/map calls. */
public final class PlayWorld {
    public static final String[] CAMERAS={"Fixed view","Follow an element","Change rooms"};
    public int width=16,height=16,camera,target=-1,x,y;
    public boolean tiles=true;
    public PlayWorld copy(){PlayWorld w=new PlayWorld();w.width=width;w.height=height;w.camera=camera;w.target=target;w.x=x;w.y=y;w.tiles=tiles;return w;}
    public void validate(int elements){
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
    static PlayWorld read(Map<String,Integer> v){
        if(!v.containsKey("world_width"))return null;
        PlayWorld w=new PlayWorld();w.width=v.get("world_width");w.height=v.get("world_height");w.camera=v.get("world_camera");w.target=v.get("world_target");w.x=v.get("world_x");w.y=v.get("world_y");int tiles=v.get("world_tiles");if(tiles<0||tiles>1)throw new IllegalArgumentException();w.tiles=tiles==1;return w;
    }
    void encode(DataOutputStream out)throws IOException{for(int v:new int[]{width,height,camera,target,x,y})out.writeInt(v);out.writeBoolean(tiles);}
    static PlayWorld decode(DataInputStream in)throws IOException{PlayWorld w=new PlayWorld();w.width=in.readInt();w.height=in.readInt();w.camera=in.readInt();w.target=in.readInt();w.x=in.readInt();w.y=in.readInt();w.tiles=in.readBoolean();return w;}
}
