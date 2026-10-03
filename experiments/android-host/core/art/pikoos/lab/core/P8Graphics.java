package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** Ordinary textual gfx rows. Missing trailing rows read as zero; unrelated bytes stay intact.
 * This decoder supports complete 128-pixel rows, with LF/CRLF and an optional final newline.
 * Lower-half edits affect memory shared with the map; callers must make that choice explicit.
 */
public final class P8Graphics {
    private final P8Document document;
    private final int section;
    private final byte[] body;
    private final int[] rows=new int[128];
    private final int count;
    private final byte[] newline;
    public P8Graphics(P8Document document){
        this.document=document;int found=-1;
        for(P8Document.Section s:document.sections())if(s.name.equals("gfx"))found=document.uniqueSection("gfx");
        section=found;body=section<0?new byte[0]:document.body(section);
        Arrays.fill(rows,-1);int at=0,n=0;byte[] ending=null;
        while(at<body.length){
            if(n==128||body.length-at<128)throw new IllegalArgumentException("Unsupported gfx row layout; source retained");
            rows[n++]=at;
            for(int x=0;x<128;x++)if(Character.digit((char)body[at+x],16)<0)
                throw new IllegalArgumentException("Invalid gfx pixel; source retained");
            at+=128;
            if(at==body.length)break;
            boolean cr=body[at]=='\r';if(cr)at++;
            if(at>=body.length||body[at++]!='\n')throw new IllegalArgumentException("Invalid gfx row ending");
            if(ending==null)ending=cr?new byte[]{'\r','\n'}:new byte[]{'\n'};
        }
        count=n;
        if(ending==null){
            byte[] source=document.bytes();boolean cr=false;
            for(int i=1;i<source.length;i++)if(source[i]=='\n'){cr=source[i-1]=='\r';break;}
            ending=cr?new byte[]{'\r','\n'}:new byte[]{'\n'};
        }
        newline=ending;
    }
    public int pixel(int x,int y){
        if(x<0||y<0||x>=128||y>=128)throw new IllegalArgumentException("Pixel outside sheet");
        return rows[y]<0?0:Character.digit((char)body[rows[y]+x],16);
    }
    public int pixel(SpriteRegion r,int x,int y){r.checkPixel(x,y);return pixel(r.x+x,r.y+y);}
    private int[] read(SpriteRegion r){
        int[] result=new int[r.width*r.height];
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)result[y*r.width+x]=pixel(r,x,y);
        return result;
    }
    private static void color(int value){if(value<0||value>15)throw new IllegalArgumentException("Color out of range");}
    private P8Document write(SpriteRegion r,int[] values){
        int last=count-1;boolean differs=false;
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
            int value=values[y*r.width+x];
            if(value!=pixel(r,x,y)){differs=true;last=Math.max(last,r.y+y);}
        }
        if(!differs)return document;
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(body,0,body.length);
        int[] offsets=rows.clone();
        if(last>=count){
            if(body.length>0&&body[body.length-1]!='\n')out.write(newline,0,newline.length);
            byte[] blank=new byte[128];Arrays.fill(blank,(byte)'0');
            for(int y=count;y<=last;y++){
                offsets[y]=out.size();out.write(blank,0,128);out.write(newline,0,newline.length);
            }
        }
        byte[] changed=out.toByteArray();
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
            int value=values[y*r.width+x];
            if(value!=pixel(r,x,y))changed[offsets[r.y+y]+r.x+x]=(byte)"0123456789abcdef".charAt(value);
        }
        if(section>=0)return document.edit(section,0,body.length,changed);
        byte[] source=document.bytes();ByteArrayOutputStream appended=new ByteArrayOutputStream();
        appended.write(source,0,source.length);
        if(source.length>0&&source[source.length-1]!='\n'&&source[source.length-1]!='\r')appended.write(newline,0,newline.length);
        byte[] marker="__gfx__".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        appended.write(marker,0,marker.length);appended.write(newline,0,newline.length);appended.write(changed,0,changed.length);
        return P8Document.parse(appended.toByteArray());
    }
    public P8Document transform(SpriteRegion r,SpriteTransform operation){
        if(!operation.supports(r))throw new IllegalArgumentException("Quarter turn requires a square selection");
        int[] source=read(r),result=new int[source.length];
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
            int sx=x,sy=y;
            switch(operation){
                case FLIP_HORIZONTAL:sx=r.width-1-x;break;
                case FLIP_VERTICAL:sy=r.height-1-y;break;
                case ROTATE_CLOCKWISE:sx=y;sy=r.height-1-x;break;
                case ROTATE_HALF:sx=r.width-1-x;sy=r.height-1-y;break;
            }
            result[y*r.width+x]=source[sy*r.width+sx];
        }
        return write(r,result);
    }
    public P8Document replaceColor(SpriteRegion r,int from,int to){
        color(from);color(to);
        if(from==to)return document;
        int[] values=read(r);
        for(int i=0;i<values.length;i++)if(values[i]==from)values[i]=to;
        return write(r,values);
    }
    public P8Document withPixel(SpriteRegion r,int x,int y,int value){
        r.checkPixel(x,y);color(value);int[] values=read(r);values[y*r.width+x]=value;return write(r,values);
    }
    public P8Document copy(SpriteRegion source,SpriteRegion destination){
        if(source.width!=destination.width||source.height!=destination.height)throw new IllegalArgumentException("Copy dimensions differ");
        return write(destination,read(source));
    }
    public P8Document insert(SpriteAsset asset,SpriteRegion destination){
        if(asset.width!=destination.width||asset.height!=destination.height)throw new IllegalArgumentException("Asset dimensions differ");
        return write(destination,asset.colors());
    }
    public P8Document withFill(SpriteRegion r,int x,int y,int value){
        r.checkPixel(x,y);color(value);int[] values=read(r);int old=values[y*r.width+x];
        if(old==value)return document;
        int[] queue=new int[values.length];int head=0,tail=0;queue[tail++]=y*r.width+x;values[queue[0]]=value;
        while(head<tail){
            int at=queue[head++],px=at%r.width,py=at/r.width;
            int[] neighbors={px>0?at-1:-1,px<r.width-1?at+1:-1,py>0?at-r.width:-1,py<r.height-1?at+r.width:-1};
            for(int next:neighbors)if(next>=0&&values[next]==old){values[next]=value;queue[tail++]=next;}
        }
        return write(r,values);
    }
    public P8Document withLine(SpriteRegion r,int x0,int y0,int x1,int y1,int value){
        r.checkPixel(x0,y0);r.checkPixel(x1,y1);color(value);int[] values=read(r);
        if(x0>x1||(x0==x1&&y0>y1)){int t=x0;x0=x1;x1=t;t=y0;y0=y1;y1=t;}
        int dx=Math.abs(x1-x0),dy=-Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,error=dx+dy;
        while(true){
            values[y0*r.width+x0]=value;if(x0==x1&&y0==y1)break;
            int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}
        }
        return write(r,values);
    }
}
