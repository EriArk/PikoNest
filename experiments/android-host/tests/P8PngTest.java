import art.pikoos.lab.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Independent PNG encoder + JDK decoder oracle; no Android dependencies. */
public class P8PngTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void rejected(byte[] b,String why){
        try{new P8Png(b);throw new AssertionError(why);}catch(IllegalArgumentException expected){checks++;}
        check(!new PlayCartridge("broken.p8.png",b).problem.isEmpty(),"readable launch error: "+why);
    }
    static byte[] chunk(String type,byte[] body)throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
        byte[] name=type.getBytes("US-ASCII");out.writeInt(body.length);out.write(name);out.write(body);
        CRC32 crc=new CRC32();crc.update(name);crc.update(body);out.writeInt((int)crc.getValue());return b.toByteArray();
    }
    static byte[] png(byte[] scan,int width,int depth,int color,int interlace,boolean split)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeLong(0x89504e470d0a1a0aL);
        ByteArrayOutputStream h=new ByteArrayOutputStream();DataOutputStream head=new DataOutputStream(h);
        head.writeInt(width);head.writeInt(205);head.write(new byte[]{(byte)depth,(byte)color,0,0,(byte)interlace});
        out.write(chunk("IHDR",h.toByteArray()));ByteArrayOutputStream z=new ByteArrayOutputStream();
        try(DeflaterOutputStream deflate=new DeflaterOutputStream(z)){deflate.write(scan);}
        byte[] compressed=z.toByteArray();
        if(split){int mid=compressed.length/2;out.write(chunk("IDAT",Arrays.copyOfRange(compressed,0,mid)));out.write(chunk("IDAT",Arrays.copyOfRange(compressed,mid,compressed.length)));}
        else out.write(chunk("IDAT",compressed));
        out.write(chunk("IEND",new byte[0]));return bytes.toByteArray();
    }
    static int predictor(int filter,int a,int b,int c){
        if(filter==0)return 0;if(filter==1)return a;if(filter==2)return b;if(filter==3)return (a+b)/2;
        int p=a+b-c;int[] options={a,b,c};int best=a,distance=Integer.MAX_VALUE;
        for(int option:options)if(Math.abs(option-p)<distance){best=option;distance=Math.abs(option-p);}return best;
    }
    static byte[] scan(byte[] pixels,int filter){
        byte[] rows=new byte[641*205];
        for(int y=0;y<205;y++){rows[y*641]=(byte)filter;for(int x=0;x<640;x++){
            int i=y*640+x;rows[y*641+x+1]=(byte)((pixels[i]&255)-predictor(filter,x<4?0:pixels[i-4]&255,y==0?0:pixels[i-640]&255,x<4||y==0?0:pixels[i-644]&255));
        }}return rows;
    }
    static void compareWithJdk(byte[] bytes)throws Exception{
        byte[] before=bytes.clone();P8Png decoded=new P8Png(bytes);BufferedImage oracle=ImageIO.read(new ByteArrayInputStream(bytes));
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)check(decoded.cover[y*128+x]==(oracle.getRGB(x+16,y+24)|0xff000000),"cover crop/RGB matches JDK");
        check(Arrays.equals(before,bytes),"source bytes untouched");
        check(new PlayCartridge("MiXeD.P8.PNG",bytes).problem.isEmpty(),"PNG launch envelope accepted");
    }
    public static void main(String[] args)throws Exception{
        byte[] pixels=new byte[160*205*4];new Random(20261003).nextBytes(pixels);
        int v=0x8000*4;pixels[v]=2;pixels[v+1]=3;pixels[v+2]=1;pixels[v+3]=2;
        for(int filter=0;filter<=4;filter++){
            byte[] bytes=png(scan(pixels,filter),160,8,6,0,true);compareWithJdk(bytes);
            check(new P8Png(bytes).cartVersion==173,"encoded version channel order");
        }
        byte[] rows=scan(pixels,0),valid=png(rows,160,8,6,0,false);
        for(int length:new int[]{0,8,44,100,valid.length-1})rejected(Arrays.copyOf(valid,length),"truncated PNG");
        byte[] crc=valid.clone();crc[50]^=1;rejected(crc,"CRC corruption");
        byte[] signature=valid.clone();signature[0]=0;rejected(signature,"signature");
        byte[] length=valid.clone();Arrays.fill(length,8,12,(byte)255);rejected(length,"negative chunk length");
        rejected(Arrays.copyOf(valid,valid.length+1),"trailing bytes");
        rejected(png(rows,128,8,6,0,false),"ordinary image dimensions");
        rejected(png(rows,160,16,6,0,false),"unsupported bit depth");
        rejected(png(rows,160,8,2,0,false),"unsupported color type");
        rejected(png(rows,160,8,6,1,false),"unsupported interlace");
        rejected(png(Arrays.copyOf(rows,rows.length+1),160,8,6,0,false),"inflated excess");
        rejected(png(Arrays.copyOf(rows,rows.length-1),160,8,6,0,false),"inflated shortfall");
        rows[0]=5;rejected(png(rows,160,8,6,0,false),"invalid row filter");
        rejected(new byte[CartridgeImport.MAX_BYTES+1],"file size budget");
        byte[] official=Files.readAllBytes(Paths.get(args[0]));compareWithJdk(official);
        check(new P8Png(official).cartVersion==43,"official 0.2.7 format version");
        check(CartridgeFormat.of("MiXeD.P8.PNG")==CartridgeFormat.P8_PNG,"PNG format wins over text");
        check(CartridgeFormat.of("MiXeD.P8")==CartridgeFormat.P8,"text format");
        check(CartridgeFormat.P8_PNG.mime.equals("image/png"),"binary MIME");
        PicoRuntimeBackend legacy=new PicoRuntimeBackend(){
            public Availability detect(){return new Availability(true,"test");}
            public Capabilities capabilities(){return new Capabilities(true,false,false,false);}
            public void launch(byte[] b){check(b==official,"legacy text delegation");}
            public boolean stop(){return false;}
        };
        legacy.launch(official,CartridgeFormat.P8);
        try{legacy.launch(official,CartridgeFormat.P8_PNG);throw new AssertionError("legacy must refuse binary");}catch(UnsupportedOperationException expected){checks++;}
        System.out.println("P8PngTest: "+checks+" checks passed");
    }
}
