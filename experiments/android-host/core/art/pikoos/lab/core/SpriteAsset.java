package art.pikoos.lab.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;

/** Independent indexed pixels, not a reference to a project's sprite number. */
public final class SpriteAsset {
    public final String id,title,origin,sourceHash;
    public final int width,height,sourceX,sourceY;
    private final byte[] pixels;
    public SpriteAsset(String id,String title,String origin,String hash,int x,int y,int w,int h,byte[] pixels){
        if(id==null||!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException("Invalid asset ID");
        text(title,80);text(origin,160);
        if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid source hash");
        new SpriteRegion(x,y,w,h);
        if(pixels==null||pixels.length!=w*h)throw new IllegalArgumentException("Invalid pixel count");
        for(byte pixel:pixels)if(pixel<0||pixel>15)throw new IllegalArgumentException("Invalid palette index");
        this.id=id;this.title=title;this.origin=origin;sourceHash=hash;sourceX=x;sourceY=y;width=w;height=h;this.pixels=pixels.clone();
    }
    private static void text(String s,int max){
        if(s==null||s.length()>max||s.isEmpty())throw new IllegalArgumentException("Invalid asset metadata");
        for(int i=0;i<s.length();i++)if(Character.isISOControl(s.charAt(i)))throw new IllegalArgumentException("Invalid metadata character");
    }
    public static SpriteAsset capture(WorkshopCartridge cart,SpriteRegion r,String title,String origin){
        byte[] pixels=new byte[r.width*r.height];
        for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)pixels[y*r.width+x]=(byte)cart.pixel(r,x,y);
        try{
            StringBuilder hash=new StringBuilder();
            for(byte b:MessageDigest.getInstance("SHA-256").digest(cart.bytes()))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            return new SpriteAsset(UUID.randomUUID().toString(),title,origin,hash.toString(),r.x,r.y,r.width,r.height,pixels);
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public int pixel(int x,int y){if(x<0||y<0||x>=width||y>=height)throw new IllegalArgumentException("Pixel outside asset");return pixels[y*width+x];}
    public int[] colors(){int[] result=new int[pixels.length];for(int i=0;i<result.length;i++)result[i]=pixels[i];return result;}
    public byte[] encode(){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(0x504b5350);out.writeInt(1);out.writeUTF(id);out.writeUTF(title);out.writeUTF(origin);out.writeUTF(sourceHash);
            out.writeInt(sourceX);out.writeInt(sourceY);out.writeInt(width);out.writeInt(height);out.write(pixels);out.flush();return bytes.toByteArray();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static SpriteAsset decode(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length>20000)throw new IOException("Invalid sprite record size");
        try{
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
            if(in.readInt()!=0x504b5350||in.readInt()!=1)throw new IOException("Unsupported sprite record version");
            String id=in.readUTF(),title=in.readUTF(),origin=in.readUTF(),hash=in.readUTF();
            int x=in.readInt(),y=in.readInt(),w=in.readInt(),h=in.readInt();new SpriteRegion(x,y,w,h);
            byte[] pixels=new byte[w*h];in.readFully(pixels);if(in.read()!=-1)throw new IOException("Unexpected sprite record data");
            return new SpriteAsset(id,title,origin,hash,x,y,w,h,pixels);
        }catch(IllegalArgumentException e){throw new IOException("Invalid sprite record",e);}
    }
}
