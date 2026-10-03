package art.pikoos.lab.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

/** Bounded PNG envelope/cover reader, not a Lua decoder or compatibility verdict.
 * Keeps original bytes outside this decoder for exact runtime transport.
 * Current slice accepts 160x205, 8-bit RGBA, non-interlaced official exports.
 */
public final class P8Png {
    public final int[] cover;
    public final int cartVersion;
    private static final byte[] SIGNATURE={(byte)137,80,78,71,13,10,26,10};
    private static IllegalArgumentException bad(){return new IllegalArgumentException("PNG повреждён или имеет неподдержанный формат");}
    private static int number(byte[] b,int p){return (b[p]&255)<<24|(b[p+1]&255)<<16|(b[p+2]&255)<<8|(b[p+3]&255);}
    private static int paeth(int a,int b,int c){int p=a+b-c,x=Math.abs(p-a),y=Math.abs(p-b),z=Math.abs(p-c);return x<=y&&x<=z?a:y<=z?b:c;}
    public P8Png(byte[] file){
        if(file==null||file.length<45||file.length>CartridgeImport.MAX_BYTES||!Arrays.equals(Arrays.copyOf(file,8),SIGNATURE))throw bad();
        boolean header=false,end=false,data=false,afterData=false;int at=8;
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        while(at<file.length){
            if(file.length-at<12)throw bad();int size=number(file,at);int body=at+8;
            if(size<0||size>file.length-at-12)throw bad();
            CRC32 crc=new CRC32();crc.update(file,at+4,size+4);
            if((int)crc.getValue()!=number(file,body+size))throw bad();
            String type=new String(file,at+4,4,StandardCharsets.US_ASCII);
            if(!header&&!type.equals("IHDR"))throw bad();
            if(type.equals("IHDR")){
                if(header||size!=13)throw bad();header=true;
                if(number(file,body)!=160||number(file,body+4)!=205)throw new IllegalArgumentException("Нужен PNG-картридж 160 × 205, не обычная картинка");
                if(file[body+8]!=8||file[body+9]!=6||file[body+10]!=0||file[body+11]!=0||file[body+12]!=0)
                    throw new IllegalArgumentException("Этот вариант PNG пока не поддержан · нужен RGBA без чересстрочности");
            }else if(type.equals("IDAT")){
                if(afterData)throw bad();data=true;compressed.write(file,body,size);
            }else if(type.equals("IEND")){
                if(size!=0||!data||body+size+4!=file.length)throw bad();end=true;
            }else{
                if(data)afterData=true;
                if((file[at+4]&32)==0&&!type.equals("PLTE"))throw bad();
            }
            at=body+size+4;
        }
        if(!end)throw bad();
        byte[] scan=new byte[(640+1)*205];Inflater inflater=new Inflater();
        try{
            inflater.setInput(compressed.toByteArray());int count=0;
            while(count<scan.length){int n=inflater.inflate(scan,count,scan.length-count);if(n==0)throw bad();count+=n;}
            if(inflater.inflate(new byte[1])!=0||!inflater.finished()||inflater.getRemaining()!=0)throw bad();
        }catch(java.util.zip.DataFormatException e){throw bad();}finally{inflater.end();}
        byte[] rgba=new byte[160*205*4];
        for(int y=0;y<205;y++){
            int filter=scan[y*641]&255;if(filter>4)throw bad();
            for(int x=0;x<640;x++){
                int pos=y*640+x,left=x<4?0:rgba[pos-4]&255,up=y==0?0:rgba[pos-640]&255,corner=x<4||y==0?0:rgba[pos-644]&255;
                int add=filter==0?0:filter==1?left:filter==2?up:filter==3?(left+up)/2:paeth(left,up,corner);
                rgba[pos]=(byte)((scan[y*641+1+x]&255)+add);
            }
        }
        int versionAt=0x8000*4;
        cartVersion=(rgba[versionAt+2]&3)|((rgba[versionAt+1]&3)<<2)|((rgba[versionAt]&3)<<4)|((rgba[versionAt+3]&3)<<6);
        cover=new int[128*128];
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int p=((y+24)*160+x+16)*4;
            // Encoded alpha bits are cart data; display the label opaquely, never re-encode.
            cover[y*128+x]=0xff000000|(rgba[p]&255)<<16|(rgba[p+1]&255)<<8|(rgba[p+2]&255);
        }
    }
}
