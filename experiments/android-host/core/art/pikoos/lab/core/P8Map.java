package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Default 128x64 PICO-8 map. Only the independent upper 32 rows are writable. */
public final class P8Map {
    private final P8Document document;
    private final int section,count;
    private final byte[] body,newline;
    private final int[] rows=new int[32];
    private final P8Graphics graphics;
    public P8Map(P8Document document){
        this.document=document;graphics=new P8Graphics(document);int found=-1;
        for(P8Document.Section s:document.sections())if(s.name.equals("map"))found=document.uniqueSection("map");
        section=found;body=found<0?new byte[0]:document.body(found);Arrays.fill(rows,-1);
        int at=0,n=0;byte[] ending=null;
        while(at<body.length){
            if(n==32||body.length-at<256)throw new IllegalArgumentException("Формат карты пока не поддержан. Исходник сохранён.");
            rows[n++]=at;
            for(int x=0;x<256;x++)if(Character.digit((char)body[at+x],16)<0)
                throw new IllegalArgumentException("Некорректная строка карты. Исходник сохранён.");
            at+=256;if(at==body.length)break;
            boolean cr=body[at]=='\r';if(cr)at++;
            if(at>=body.length||body[at++]!='\n')throw new IllegalArgumentException("Неподдержанный конец строки карты");
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
    public int tile(int x,int y){
        if(x<0||x>=128||y<0||y>=64)throw new IllegalArgumentException("Клетка вне карты 128×64");
        if(y>=32){int offset=(y-32)*128+x;return graphics.pixel(offset%64*2,64+offset/64)|graphics.pixel(offset%64*2+1,64+offset/64)<<4;}
        if(rows[y]<0)return 0;
        int at=rows[y]+x*2;return Character.digit((char)body[at],16)*16+Character.digit((char)body[at+1],16);
    }
    public P8Document withTile(int x,int y,int value){
        tile(x,y);
        if(y>=32)throw new IllegalArgumentException("Нижняя половина карты делит память со спрайтами. Пока доступен только просмотр.");
        boolean[] mask=new boolean[4096];mask[y*128+x]=true;return withTiles(mask,value);
    }
    /** Atomic bulk edit: serialize once, preserve all digits outside changed cells. */
    public P8Document withTiles(boolean[] mask,int value){
        if(mask==null||mask.length!=4096||value<0||value>255)throw new IllegalArgumentException("Invalid map edit");
        int last=-1;
        for(int i=0;i<4096;i++)if(mask[i]&&tile(i%128,i/128)!=value)last=i/128;
        if(last<0)return document;
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(body,0,body.length);
        int[] offsets=rows.clone();
        if(last>=count){
            if(body.length>0&&body[body.length-1]!='\n')out.write(newline,0,newline.length);
            byte[] blank=new byte[256];Arrays.fill(blank,(byte)'0');
            for(int row=count;row<=last;row++){
                offsets[row]=out.size();out.write(blank,0,blank.length);out.write(newline,0,newline.length);
            }
        }
        byte[] changed=out.toByteArray();String hex="0123456789abcdef";
        for(int i=0;i<4096;i++)if(mask[i]&&tile(i%128,i/128)!=value){
            int offset=offsets[i/128]+i%128*2;
            changed[offset]=(byte)hex.charAt(value>>4);changed[offset+1]=(byte)hex.charAt(value&15);
        }
        if(section>=0)return document.edit(section,0,body.length,changed);
        byte[] source=document.bytes();out.reset();out.write(source,0,source.length);
        if(source.length>0&&source[source.length-1]!='\n'&&source[source.length-1]!='\r')out.write(newline,0,newline.length);
        byte[] marker="__map__".getBytes(StandardCharsets.US_ASCII);out.write(marker,0,marker.length);
        out.write(newline,0,newline.length);out.write(changed,0,changed.length);
        return P8Document.parse(out.toByteArray());
    }
}
