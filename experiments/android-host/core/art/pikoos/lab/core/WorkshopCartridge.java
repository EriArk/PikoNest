package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Byte-scoped edits for our owned Moon Garden template, not a general Lua parser. */
public final class WorkshopCartridge {
    /** This owned template exposes eight non-overlapping 16x16 regions in its first two tile rows. */
    public static final int SPRITE_COUNT = 8;
    private final P8Document document;
    private final int lua, gfx;
    private final int heroFrom, heroTo, heroSlot;
    private final byte[] pixels;
    private final int[] fields = new int[2];
    private final int[] rows = new int[16];
    public WorkshopCartridge(byte[] source) {
        document = P8Document.parse(source);
        lua = document.uniqueSection("lua");
        gfx = document.uniqueSection("gfx");
        String code = code();
        String[] names = {"speed", "jump"};
        for (int i = 0; i < 2; i++) {
            Matcher m = Pattern.compile("(?m)^-- pikoos-" + names[i]
                + "\\r?\\n" + names[i] + "=([1-4])(?=\\r?$)").matcher(code);
            if (!m.find()) throw new IllegalArgumentException("Missing owned field: " + names[i]);
            fields[i] = m.start(1);
            if (m.find()) throw new IllegalArgumentException("Ambiguous field: " + names[i]);
        }
        Matcher hero = Pattern.compile("(?m)^ spr\\(([0-9]{1,2}),x,y,2,2\\)(?=\\r?$)").matcher(code);
        if (!hero.find()) throw new IllegalArgumentException("Owned hero drawing call missing");
        heroFrom = hero.start(1); heroTo = hero.end(1);
        int number = Integer.parseInt(hero.group(1));
        if (number % 2 != 0 || number >= SPRITE_COUNT * 2 || hero.find())
            throw new IllegalArgumentException("Ambiguous or unsupported hero sprite");
        heroSlot = number / 2;
        pixels = document.body(gfx);
        int start = 0;
        for (int y = 0; y < 16; y++) {
            rows[y] = start;
            for (int x = 0; x < 128; x++) {
                if (start + x >= pixels.length || Character.digit((char)pixels[start + x], 16) < 0)
                    throw new IllegalArgumentException("Invalid sprite row " + y);
            }
            start += 128;
            if (start < pixels.length && pixels[start] == '\r') start++;
            if (start >= pixels.length || pixels[start++] != '\n')
                throw new IllegalArgumentException("Missing sprite row ending");
        }
    }
    public byte[] bytes() { return document.bytes(); }
    public int heroSlot() { return heroSlot; }
    public WorkshopCartridge withHero(int slot) {
        checkSlot(slot);
        if (empty(slot)) throw new IllegalArgumentException("Сначала нарисуй хотя бы один пиксель");
        return new WorkshopCartridge(document.edit(lua, heroFrom, heroTo,
            Integer.toString(slot * 2).getBytes(StandardCharsets.US_ASCII)).bytes());
    }
    public String code() { return new String(document.body(lua), StandardCharsets.ISO_8859_1); }
    public int value(int field) { return document.body(lua)[fields[field]] - '0'; }
    public int line(int field) {
        String prefix = code().substring(0, fields[field]);
        return prefix.length() - prefix.replace("\n", "").length();
    }
    public WorkshopCartridge withValue(int field, int value) {
        if (field < 0 || field > 1 || value < 1 || value > 4) throw new IllegalArgumentException("Field out of range");
        return new WorkshopCartridge(document.edit(lua, fields[field], fields[field] + 1,
            new byte[]{(byte)('0' + value)}).bytes());
    }
    public int pixel(int x, int y) {
        return pixel(0, x, y);
    }
    public int pixel(int slot, int x, int y) {
        checkSlot(slot);
        checkPixel(x, y);
        return Character.digit((char)pixels[rows[y] + slot * 16 + x], 16);
    }
    public WorkshopCartridge withPixel(int x, int y, int color) {
        return withPixel(0, x, y, color);
    }
    public WorkshopCartridge withPixel(int slot, int x, int y, int color) {
        checkSlot(slot);
        checkPixel(x, y);
        if (color < 0 || color > 15) throw new IllegalArgumentException("Color out of range");
        int at = rows[y] + slot * 16 + x;
        return new WorkshopCartridge(document.edit(gfx, at, at + 1,
            new byte[]{(byte)"0123456789abcdef".charAt(color)}).bytes());
    }
    public boolean empty(int slot) {
        checkSlot(slot);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (pixel(slot, x, y) != 0) return false;
        return true;
    }
    /** Four-connected fill restricted to one 16x16 region, preserving all other bytes. */
    public WorkshopCartridge withFill(int slot, int x, int y, int color) {
        checkSlot(slot);checkPixel(x,y);checkColor(color);
        int old=pixel(slot,x,y);if(old==color)return this;
        byte[] changed=pixels.clone();int[] queue=new int[256];int head=0,tail=0;
        queue[tail++]=y*16+x;changed[rows[y]+slot*16+x]=hex(color);
        while(head<tail){
            int at=queue[head++],px=at%16,py=at/16;
            int[] neighbors={px>0?at-1:-1,px<15?at+1:-1,py>0?at-16:-1,py<15?at+16:-1};
            for(int next:neighbors)if(next>=0){
                int offset=rows[next/16]+slot*16+next%16;
                if(Character.digit((char)changed[offset],16)==old){changed[offset]=hex(color);queue[tail++]=next;}
            }
        }
        return new WorkshopCartridge(document.edit(gfx,0,pixels.length,changed).bytes());
    }
    /** Inclusive Bresenham line. Canonical endpoint order makes reversal identical. */
    public WorkshopCartridge withLine(int slot, int x0, int y0, int x1, int y1, int color) {
        checkSlot(slot);checkPixel(x0,y0);checkPixel(x1,y1);checkColor(color);
        if(x0>x1||(x0==x1&&y0>y1)){int swap=x0;x0=x1;x1=swap;swap=y0;y0=y1;y1=swap;}
        byte[] changed=pixels.clone();int dx=Math.abs(x1-x0),dy=-Math.abs(y1-y0);
        int sx=x0<x1?1:-1,sy=y0<y1?1:-1,error=dx+dy;
        while(true){
            int offset=rows[y0]+slot*16+x0;
            if(Character.digit((char)changed[offset],16)!=color)changed[offset]=hex(color);
            if(x0==x1&&y0==y1)break;
            int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}
        }
        if(java.util.Arrays.equals(changed,pixels))return this;
        return new WorkshopCartridge(document.edit(gfx,0,pixels.length,changed).bytes());
    }
    private static byte hex(int color){return (byte)"0123456789abcdef".charAt(color);}
    private static void checkColor(int color){if(color<0||color>15)throw new IllegalArgumentException("Color out of range");}
    public int firstFreeSlot() {
        // An erased but still assigned hero is not available for automatic allocation.
        for (int i = 0; i < SPRITE_COUNT; i++) if (i != heroSlot && empty(i)) return i;
        return -1;
    }
    public WorkshopCartridge copySprite(int source, int destination) {
        checkSlot(source); checkSlot(destination);
        if (source == destination || destination == heroSlot || !empty(destination))
            throw new IllegalArgumentException("Copy destination is occupied");
        if (empty(source)) throw new IllegalArgumentException("Нечего копировать: рисунок пустой");
        byte[] changed = pixels.clone();
        for (int y = 0; y < 16; y++)
            System.arraycopy(pixels, rows[y] + source * 16, changed, rows[y] + destination * 16, 16);
        return new WorkshopCartridge(document.edit(gfx, 0, pixels.length, changed).bytes());
    }
    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SPRITE_COUNT) throw new IllegalArgumentException("Sprite slot out of range");
    }
    private static void checkPixel(int x, int y) {
        if (x < 0 || y < 0 || x > 15 || y > 15) throw new IllegalArgumentException("Pixel out of range");
    }
}
