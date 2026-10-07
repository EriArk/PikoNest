package art.pikoos.lab.core;

/** Cursor movement never paints. Picker changes remain a proposal until confirmed. */
public final class MapEditor {
    public enum Tool { BRUSH, RECTANGLE, FILL }
    public Tool tool=Tool.BRUSH;
    public MapRegion region;
    public boolean choosingTool;
    public int toolChoice,phase,anchorX,anchorY,peekX,peekY;
    private String base="";
    public int x,y,tile=1,choice=1;
    public boolean picking;
    public int left(){return Math.min(112,Math.max(0,(phase==2?peekX:x)-7));}
    public int top(){return Math.min(48,Math.max(0,(phase==2?peekY:y)-7));}
    public void move(int dx,int dy){
        if(phase==2){peekX=clamp(peekX+dx,127);peekY=clamp(peekY+dy,63);return;}
        if(picking)choice=clamp(choice%16+dx,15)+16*clamp(choice/16+dy,15);
        else{x=clamp(x+dx,127);y=clamp(y+dy,63);}
    }
    public boolean pending(){return phase>0;}
    public boolean modal(){return picking||choosingTool||pending()||region!=null;}
    public void tools(){if(!modal()){toolChoice=tool.ordinal();choosingTool=true;}}
    public void start(WorkshopCartridge cart){
        if(tool==Tool.BRUSH||modal())return;
        cart.map();base=hash(cart.bytes());anchorX=x;anchorY=y;phase=1;if(tool==Tool.FILL)review();
    }
    public void review(){phase=2;peekX=x;peekY=y;}
    public MapChange preview(WorkshopCartridge cart){
        if(!pending())return null;
        return tool==Tool.FILL?MapChange.fill(cart.map(),anchorX,anchorY,tile):MapChange.rectangle(cart.map(),anchorX,anchorY,x,y,tile);
    }
    public WorkshopCartridge candidate(WorkshopCartridge cart){
        if(phase!=2||!base.equals(hash(cart.bytes())))throw new IllegalArgumentException("Картридж изменился. Отмени предложение и выбери область заново.");
        return cart.withMapChange(preview(cart));
    }
    public void clear(){phase=0;base="";}
    public void back(){if(phase==2&&tool==Tool.RECTANGLE)phase=1;else clear();}
    public void point(int px,int py){if(phase==2){peekX=clamp(px,127);peekY=clamp(py,63);}else{x=clamp(px,127);y=clamp(py,63);}}
    public String encode(){return region!=null?region.encode():pending()?"1;"+tool.name()+";"+phase+";"+x+";"+y+";"+tile+";"+anchorX+";"+anchorY+";"+base+";"+peekX+";"+peekY:"";}
    public void restore(String encoded,WorkshopCartridge cart){
        if(encoded.isEmpty())return;
        if(encoded.startsWith("2;")){region=MapRegion.restore(encoded,cart);clear();picking=choosingTool=false;return;}
        try{
            String[] f=encoded.split(";",-1);if(f.length!=11||!f[0].equals("1"))throw new IllegalArgumentException();
            Tool t=Tool.valueOf(f[1]);int p=Integer.parseInt(f[2]),px=Integer.parseInt(f[3]),py=Integer.parseInt(f[4]),v=Integer.parseInt(f[5]),ax=Integer.parseInt(f[6]),ay=Integer.parseInt(f[7]);
            if(t==Tool.BRUSH||p<1||p>2||(t==Tool.FILL&&p!=2)||px<0||px>127||py<0||py>63||ax<0||ax>127||ay<0||ay>63||v<0||v>255||!f[8].equals(hash(cart.bytes())))throw new IllegalArgumentException();
            int vx=Integer.parseInt(f[9]),vy=Integer.parseInt(f[10]);if(vx<0||vx>127||vy<0||vy>63)throw new IllegalArgumentException();
            cart.map();tool=t;phase=p;x=px;y=py;tile=v;anchorX=ax;anchorY=ay;base=f[8];peekX=vx;peekY=vy;picking=choosingTool=false;
        }catch(Exception e){throw new IllegalArgumentException("Не удалось восстановить правку карты. Картридж сохранён; выбери область заново.");}
    }
    static String hash(byte[] bytes){
        try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder b=new StringBuilder();for(byte v:digest)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public void begin(){choice=tile;picking=true;}
    public void accept(){tile=choice;picking=false;}
    public void cancel(){picking=false;choice=tile;}
    private static int clamp(int n,int max){return Math.max(0,Math.min(max,n));}
}
