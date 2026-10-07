package art.pikoos.lab.core;

import java.io.*;

/** Controller draft for the existing ordinary-Lua horizontal strip, not a new recipe. */
public final class BackgroundUse {
    public final LuaInsert form;
    public int field,previewMillis,previewCameraX;
    public boolean playing;
    public SpritePlacement picker;
    public BackgroundUse(SpriteRegion region){form=create();BackgroundLayer.setRegion(form,region);}
    public BackgroundUse(LuaInsert original){
        form=create();for(int n=0;n<8;n++)form.set(n,original.value(n));
    }
    private static LuaInsert create(){
        LuaInsert f=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("background"))f.choose(n);
        f.screen=LuaInsert.Screen.FIELDS;return f;
    }
    public SpriteRegion region(){return BackgroundLayer.region(form);}
    public void change(int delta){
        if(field>=1&&field<=4){form.field=field+3;form.step(delta);}
        if(field==5)previewCameraX=Math.max(-32768,Math.min(32767,previewCameraX+delta));
    }
    public void chooseRegion(){playing=false;picker=new SpritePlacement(region());}
    public void acceptPick(){if(picker.phase==0)picker.next();else{BackgroundLayer.setRegion(form,picker.source());picker=null;}}
    public void cancelPick(){if(picker.phase==1)picker.phase=0;else picker=null;}
    public void advance(int millis){if(playing&&picker==null&&millis>0)previewMillis=(int)Math.min(60000L,previewMillis+(long)millis);if(previewMillis==60000)playing=false;}
    public void toggle(){if(!playing&&previewMillis==60000)previewMillis=0;playing=!playing;}
    public int pixel(WorkshopCartridge cart,int x,int y){return BackgroundLayer.pixel(cart,form,x,y,previewMillis/1000.0,previewCameraX);}
    public void write(DataOutputStream out)throws IOException{
        for(int n=0;n<8;n++)out.writeUTF(form.value(n));out.writeInt(field);out.writeInt(previewMillis);out.writeInt(previewCameraX);
        out.writeBoolean(picker!=null);if(picker!=null)picker.write(out);
    }
    public static BackgroundUse read(DataInputStream in)throws IOException{
        LuaInsert f=create();for(int n=0;n<8;n++)f.set(n,in.readUTF());BackgroundUse b=new BackgroundUse(f);
        b.region();b.field=in.readInt();b.previewMillis=in.readInt();b.previewCameraX=in.readInt();
        if(in.readBoolean())b.picker=SpritePlacement.read(in);
        if(b.field<0||b.field>6||b.previewMillis<0||b.previewMillis>60000||b.previewCameraX < -32768||b.previewCameraX>32767||b.picker!=null&&(b.picker.phase>1||b.field!=0))throw new IOException("background state");
        return b;
    }
}
