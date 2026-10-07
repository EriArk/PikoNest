package art.pikoos.lab.core;

import java.io.*;
import java.util.*;

/** Resource-only snapshot. Shared gfx/map consequences are reviewed before publication. */
public final class SharedEdit {
    public final WorkshopCartridge original, after;
    public final String label, origin;
    public final int pixels, cells, sharedCells;
    public final SpriteRegion crop;
    private final int[] pixelValues=new int[16384], mapValues=new int[8192];
    private final List<Integer> shared=new ArrayList<>();
    public int selected;
    public boolean mapView;
    public SharedEdit(WorkshopCartridge base,WorkshopCartridge candidate,String label,String origin){
        if(!origin.equals("CANVAS")&&!origin.equals("NAVIGATE"))throw new IllegalArgumentException("Invalid resource return context");
        original=base;after=candidate;this.label=label;this.origin=origin;
        Arrays.fill(pixelValues,-1);Arrays.fill(mapValues,-1);
        int pc=0,mc=0,minX=127,minY=127,maxX=0,maxY=0;
        for(int i=0;i<16384;i++)if(base.sheetPixel(i%128,i/128)!=candidate.sheetPixel(i%128,i/128)){
            pixelValues[i]=candidate.sheetPixel(i%128,i/128);pc++;
            minX=Math.min(minX,i%128);minY=Math.min(minY,i/128);maxX=Math.max(maxX,i%128);maxY=Math.max(maxY,i/128);
        }
        P8Map beforeMap=base.map(),afterMap=candidate.map();
        for(int i=0;i<8192;i++)if(beforeMap.tile(i%128,i/128)!=afterMap.tile(i%128,i/128)){
            mc++;if(i>=4096)shared.add(i);else mapValues[i]=afterMap.tile(i%128,i/128);
        }
        // Reconstruct through the ordinary serializers: no hidden code/flags/audio changes.
        WorkshopCartridge rebuilt=base.withMapCells(mapValues).withSheetPixels(pixelValues);
        if(!Arrays.equals(rebuilt.bytes(),candidate.bytes()))throw new IllegalArgumentException("The shared review accepts only graphics/map edits; other source was preserved.");
        pixels=pc;cells=mc;sharedCells=shared.size();
        crop=pc==0?new SpriteRegion(0,64,8,8):new SpriteRegion(minX/8*8,minY/8*8,(maxX/8-minX/8+1)*8,(maxY/8-minY/8+1)*8);
    }
    public int x(){return shared.isEmpty()?0:shared.get(selected)%128;}
    public int y(){return shared.isEmpty()?32:shared.get(selected)/128;}
    public void step(int delta){selected=Math.max(0,Math.min(shared.size()-1,selected+delta));}
    public WorkshopCartridge candidate(WorkshopCartridge current){
        if(!Arrays.equals(current.bytes(),original.bytes()))throw new IllegalArgumentException("The project changed. Cancel this proposal and select the resource again.");
        return after;
    }
    public byte[] encode(){try{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
        o.writeInt(1);o.writeUTF(MapEditor.hash(original.bytes()));o.writeUTF(label);o.writeUTF(origin);o.writeInt(selected);o.writeBoolean(mapView);
        write(o,pixelValues);write(o,Arrays.copyOf(mapValues,4096));return b.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    private static void write(DataOutputStream o,int[] v)throws IOException{
        int count=0;for(int n:v)if(n>=0)count++;o.writeInt(count);
        for(int i=0;i<v.length;i++)if(v[i]>=0){o.writeShort(i);o.writeByte(v[i]);}
    }
    private static int[] read(DataInputStream i,int size,int max)throws IOException{
        int[] v=new int[size];Arrays.fill(v,-1);int count=i.readInt();if(count<0||count>size)throw new IOException();
        for(int n=0;n<count;n++){int at=i.readUnsignedShort(),value=i.readUnsignedByte();if(at>=size||value>max||v[at]>=0)throw new IOException();v[at]=value;}return v;
    }
    public static SharedEdit restore(byte[] bytes,WorkshopCartridge cart){try{
        if(bytes.length>70000)throw new IOException();DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes));
        if(i.readInt()!=1||!i.readUTF().equals(MapEditor.hash(cart.bytes())))throw new IOException();
        String label=i.readUTF(),origin=i.readUTF();int selected=i.readInt();boolean view=i.readBoolean();
        if(label.length()>80)throw new IOException();int[] pixels=read(i,16384,15),upper=read(i,4096,255),cells=new int[8192];Arrays.fill(cells,-1);System.arraycopy(upper,0,cells,0,4096);
        if(i.available()!=0)throw new IOException();
        SharedEdit result=new SharedEdit(cart,cart.withMapCells(cells).withSheetPixels(pixels),label,origin);
        if(result.sharedCells==0||selected<0||selected>=result.sharedCells)throw new IOException();result.selected=selected;result.mapView=view;return result;
    }catch(Exception e){throw new IllegalArgumentException("Could not restore the shared-memory proposal. Saved resources are intact; select the area again.");}}
}
