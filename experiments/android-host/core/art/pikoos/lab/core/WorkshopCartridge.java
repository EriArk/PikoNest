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
    private final P8Graphics graphics;
    private final int lua, gfx;
    private final HeroCode heroCode;
    private final byte[] pixels;
    private final int[] fields = new int[2];
    private final int[] rows = new int[16];
    public WorkshopCartridge(byte[] source) {
        document = P8Document.parse(source);
        graphics = new P8Graphics(document);
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
        heroCode=new HeroCode(code);
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
    public int pixel(SpriteRegion region,int x,int y){return graphics.pixel(region,x,y);}
    public int sheetPixel(int x,int y){return graphics.pixel(x,y);}
    private WorkshopCartridge edited(P8Document next){return next==document?this:new WorkshopCartridge(next.bytes());}
    public WorkshopCartridge withPixel(SpriteRegion r,int x,int y,int color){return edited(graphics.withPixel(r,x,y,color));}
    public WorkshopCartridge withFill(SpriteRegion r,int x,int y,int color){return edited(graphics.withFill(r,x,y,color));}
    public WorkshopCartridge withLine(SpriteRegion r,int x0,int y0,int x1,int y1,int color){return edited(graphics.withLine(r,x0,y0,x1,y1,color));}
    public int heroSlot() { return hero().card(); }
    public HeroBinding hero(){return heroCode.binding;}
    public boolean legacyHero(){return heroCode.legacy;}
    public HeroBinding proposeHero(SpriteRegion r){return HeroBinding.fromPixels(this,r);}
    public WorkshopCartridge withHero(HeroBinding binding){
        proposeHero(binding.image); // Refuse assigning an empty area; keep explicit body dimensions.
        String changed=heroCode.withBinding(code(),binding);
        return edited(document.edit(lua,0,document.body(lua).length,changed.getBytes(StandardCharsets.ISO_8859_1)));
    }
    public WorkshopCartridge withHero(int slot) {
        checkSlot(slot);
        if (empty(slot)) throw new IllegalArgumentException("Сначала нарисуй хотя бы один пиксель");
        if(!legacyHero())return withHero(proposeHero(legacyRegion(slot)));
        String changed=heroCode.withLegacyCard(code(),slot);
        return edited(document.edit(lua,0,document.body(lua).length,changed.getBytes(StandardCharsets.ISO_8859_1)));
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
        return pixel(legacyRegion(slot),x,y);
    }
    public WorkshopCartridge withPixel(int x, int y, int color) {
        return withPixel(0, x, y, color);
    }
    public WorkshopCartridge withPixel(int slot, int x, int y, int color) {
        checkSlot(slot);
        return withPixel(legacyRegion(slot),x,y,color);
    }
    public boolean empty(int slot) {
        checkSlot(slot);
        return empty(legacyRegion(slot));
    }
    public boolean empty(SpriteRegion r){
        for (int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)if(pixel(r,x,y)!=0)return false;
        return true;
    }
    /** Four-connected fill restricted to one 16x16 region, preserving all other bytes. */
    public WorkshopCartridge withFill(int slot, int x, int y, int color) {
        checkSlot(slot);return withFill(legacyRegion(slot),x,y,color);
    }
    /** Inclusive Bresenham line. Canonical endpoint order makes reversal identical. */
    public WorkshopCartridge withLine(int slot, int x0, int y0, int x1, int y1, int color) {
        checkSlot(slot);return withLine(legacyRegion(slot),x0,y0,x1,y1,color);
    }
    private static SpriteRegion legacyRegion(int slot){return new SpriteRegion(slot*16,0,16,16);}
    public int firstFreeSlot() {
        // An erased but still assigned hero is not available for automatic allocation.
        for (int i = 0; i < SPRITE_COUNT; i++) if (!hero().overlaps(legacyRegion(i)) && empty(i)) return i;
        return -1;
    }
    public WorkshopCartridge copySprite(int source, int destination) {
        checkSlot(source); checkSlot(destination);
        if (source == destination || hero().overlaps(legacyRegion(destination)) || !empty(destination))
            throw new IllegalArgumentException("Copy destination is occupied");
        if (empty(source)) throw new IllegalArgumentException("Нечего копировать: спрайт пустой");
        byte[] changed = pixels.clone();
        for (int y = 0; y < 16; y++)
            System.arraycopy(pixels, rows[y] + source * 16, changed, rows[y] + destination * 16, 16);
        return new WorkshopCartridge(document.edit(gfx, 0, pixels.length, changed).bytes());
    }
    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SPRITE_COUNT) throw new IllegalArgumentException("Sprite slot out of range");
    }
}
