package art.pikoos.lab.core;

/** Eight flags belong to one 8x8 sprite, not an individual map cell. */
public final class FlagDraft {
    public final int tile,original;
    public int value,focus;
    private final String base;
    public FlagDraft(WorkshopCartridge cart,int tile){this.tile=tile;original=value=cart.flags(tile);base=hash(cart.bytes());}
    public boolean bit(int n){return (value&(1<<n))!=0;}
    public void toggle(){if(focus<8)value^=1<<focus;}
    public void move(int dx,int dy){
        if(focus==8){if(dy<0)focus=4;return;}
        if(dy>0&&focus>=4){focus=8;return;}
        focus=Math.max(0,Math.min(1,focus/4+dy))*4+Math.max(0,Math.min(3,focus%4+dx));
    }
    public WorkshopCartridge candidate(WorkshopCartridge cart){
        if(!base.equals(hash(cart.bytes())))throw new IllegalArgumentException("Картридж изменился. Открой флаги заново.");
        return cart.withFlags(tile,value);
    }
    public String encode(){return "1;"+tile+";"+value+";"+focus+";"+base;}
    public static FlagDraft restore(String state,WorkshopCartridge cart){
        try{
            String[] f=state.split(";",-1);if(f.length!=5||!f[0].equals("1"))throw new IllegalArgumentException();
            FlagDraft result=new FlagDraft(cart,Integer.parseInt(f[1]));int v=Integer.parseInt(f[2]),cursor=Integer.parseInt(f[3]);
            if(v<0||v>255||cursor<0||cursor>8||!result.base.equals(f[4]))throw new IllegalArgumentException();
            result.value=v;result.focus=cursor;return result;
        }catch(Exception e){throw new IllegalArgumentException("Правку флагов восстановить не удалось. Картридж сохранён; открой флаги заново.");}
    }
    private static String hash(byte[] bytes){
        try{StringBuilder b=new StringBuilder();for(byte v:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
