package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Standard 256 sprite flag bytes in __gff__; independent of shared gfx/map memory. */
public final class P8Flags {
    private final P8Document document;
    private final int section,count;
    private final byte[] body,newline;
    private final int[] rows={-1,-1};
    public P8Flags(P8Document document){
        this.document=document;int found=-1;
        for(P8Document.Section s:document.sections())if(s.name.equals("gff"))found=document.uniqueSection("gff");
        section=found;body=found<0?new byte[0]:document.body(found);int at=0,n=0;byte[] ending=null;
        while(at<body.length){
            if(n==2||body.length-at<256)throw new IllegalArgumentException("Формат флагов пока не поддержан. Исходник сохранён.");
            rows[n++]=at;
            for(int i=0;i<256;i++)if(Character.digit((char)body[at+i],16)<0)throw new IllegalArgumentException("Некорректные данные флагов. Исходник сохранён.");
            at+=256;if(at==body.length)break;
            boolean cr=body[at]=='\r';if(cr)at++;
            if(at>=body.length||body[at++]!='\n')throw new IllegalArgumentException("Неподдержанный конец строки флагов");
            if(ending==null)ending=cr?new byte[]{'\r','\n'}:new byte[]{'\n'};
        }
        count=n;
        if(ending==null){byte[] source=document.bytes();boolean cr=false;for(int i=1;i<source.length;i++)if(source[i]=='\n'){cr=source[i-1]=='\r';break;}ending=cr?new byte[]{'\r','\n'}:new byte[]{'\n'};}
        newline=ending;
    }
    public int get(int tile){
        if(tile<0||tile>255)throw new IllegalArgumentException("Тайл: от 0 до 255");
        int row=rows[tile/128];if(row<0)return 0;int offset=row+tile%128*2;
        return Character.digit((char)body[offset],16)*16+Character.digit((char)body[offset+1],16);
    }
    public P8Document withFlags(int tile,int value){
        int old=get(tile);if(value<0||value>255)throw new IllegalArgumentException("Флаги: от 0 до 255");
        if(old==value)return document;
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(body,0,body.length);int offset=rows[tile/128];
        if(offset<0){
            if(body.length>0&&body[body.length-1]!='\n')out.write(newline,0,newline.length);
            byte[] blank=new byte[256];Arrays.fill(blank,(byte)'0');
            for(int row=count;row<=tile/128;row++){if(row==tile/128)offset=out.size();out.write(blank,0,blank.length);out.write(newline,0,newline.length);}
        }
        byte[] changed=out.toByteArray();offset+=tile%128*2;String hex="0123456789abcdef";
        // Do not normalize a digit that is already correct (including uppercase).
        if((old>>4)!=(value>>4))changed[offset]=(byte)hex.charAt(value>>4);
        if((old&15)!=(value&15))changed[offset+1]=(byte)hex.charAt(value&15);
        if(section>=0)return document.edit(section,0,body.length,changed);
        byte[] source=document.bytes();out.reset();out.write(source,0,source.length);
        if(source.length>0&&source[source.length-1]!='\n'&&source[source.length-1]!='\r')out.write(newline,0,newline.length);
        byte[] marker="__gff__".getBytes(StandardCharsets.US_ASCII);out.write(marker,0,marker.length);out.write(newline,0,newline.length);out.write(changed,0,changed.length);
        return P8Document.parse(out.toByteArray());
    }
}
