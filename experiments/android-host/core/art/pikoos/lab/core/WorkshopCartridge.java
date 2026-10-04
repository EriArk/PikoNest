package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;

/** Cart resources with an optional owned-template adapter; not a general Lua parser. */
public final class WorkshopCartridge {
    /** This owned template exposes eight non-overlapping 16x16 regions in its first two tile rows. */
    public static final int SPRITE_COUNT = 8;
    private final P8Document document;
    private final P8Graphics graphics;
    private final int lua;
    private final MoonGardenBinding garden;
    public WorkshopCartridge(byte[] source) {
        document = P8Document.parse(source);
        graphics = new P8Graphics(document);
        int found=-1;
        for(P8Document.Section s:document.sections())if(s.name.equals("lua"))found=document.uniqueSection("lua");
        lua=found;garden=MoonGardenBinding.detect(code());
    }
    public boolean hasHero(){return garden!=null;}
    private MoonGardenBinding binding(){if(garden==null)throw new IllegalArgumentException("В проекте нет привязки героя");return garden;}
    public byte[] bytes() { return document.bytes(); }
    public int pixel(SpriteRegion region,int x,int y){return graphics.pixel(region,x,y);}
    public int sheetPixel(int x,int y){return graphics.pixel(x,y);}
    public P8Map map(){return new P8Map(document);}
    public int flags(int tile){return new P8Flags(document).get(tile);}
    public WorkshopCartridge withFlags(int tile,int value){return edited(new P8Flags(document).withFlags(tile,value));}
    public WorkshopCartridge withTile(int x,int y,int tile){return edited(map().withTile(x,y,tile));}
    public WorkshopCartridge withMapChange(MapChange change){return edited(change.apply(map()));}
    public WorkshopCartridge withMapCells(int[] values){return edited(map().withCells(values));}
    public WorkshopCartridge insert(SpriteAsset asset,SpriteRegion target){return edited(graphics.insert(asset,target));}
    public WorkshopCartridge replaceColor(SpriteRegion region,int from,int to){return edited(graphics.replaceColor(region,from,to));}
    private WorkshopCartridge edited(P8Document next){return next==document?this:new WorkshopCartridge(next.bytes());}
    public WorkshopCartridge withPixel(SpriteRegion r,int x,int y,int color){return edited(graphics.withPixel(r,x,y,color));}
    public WorkshopCartridge withFill(SpriteRegion r,int x,int y,int color){return edited(graphics.withFill(r,x,y,color));}
    public WorkshopCartridge withLine(SpriteRegion r,int x0,int y0,int x1,int y1,int color){return edited(graphics.withLine(r,x0,y0,x1,y1,color));}
    public WorkshopCartridge withRectangle(SpriteRegion r,int x0,int y0,int x1,int y1,int color,boolean filled){return edited(graphics.withRectangle(r,x0,y0,x1,y1,color,filled));}
    public WorkshopCartridge withOval(SpriteRegion r,int x0,int y0,int x1,int y1,int color,boolean filled){return edited(graphics.withOval(r,x0,y0,x1,y1,color,filled));}
    public WorkshopCartridge moved(SpriteRegion source,SpriteRegion target){return edited(graphics.move(source,target));}
    public int heroSlot() { return hasHero()?hero().card():-1; }
    public HeroBinding hero(){return binding().hero.binding;}
    public boolean legacyHero(){return binding().hero.legacy;}
    public HeroBinding proposeHero(SpriteRegion r){binding();return HeroBinding.fromPixels(this,r);}
    public WorkshopCartridge withHero(HeroBinding binding){
        proposeHero(binding.image); // Refuse assigning an empty area; keep explicit body dimensions.
        String changed=binding().hero.withBinding(code(),binding);
        return edited(document.edit(lua,0,document.body(lua).length,changed.getBytes(StandardCharsets.ISO_8859_1)));
    }
    public WorkshopCartridge withHero(int slot) {
        checkSlot(slot);
        if (empty(slot)) throw new IllegalArgumentException("Сначала нарисуй хотя бы один пиксель");
        if(!legacyHero())return withHero(proposeHero(legacyRegion(slot)));
        String changed=binding().hero.withLegacyCard(code(),slot);
        return edited(document.edit(lua,0,document.body(lua).length,changed.getBytes(StandardCharsets.ISO_8859_1)));
    }
    public String code() { return lua<0?"":new String(document.body(lua), StandardCharsets.ISO_8859_1); }
    public int value(int field) { return document.body(lua)[binding().fields[field]] - '0'; }
    public int line(int field) {
        if(!hasHero())return -1;
        String prefix = code().substring(0, binding().fields[field]);
        return prefix.length() - prefix.replace("\n", "").length();
    }
    public WorkshopCartridge withValue(int field, int value) {
        if (field < 0 || field > 1 || value < 1 || value > 4) throw new IllegalArgumentException("Field out of range");
        int offset=binding().fields[field];
        return new WorkshopCartridge(document.edit(lua, offset, offset + 1,
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
        // Arbitrary Lua can reference visually empty areas; no inferred allocator for unknown carts.
        if(!hasHero())return -1;
        // An erased but still assigned hero is not available for automatic allocation.
        for (int i = 0; i < SPRITE_COUNT; i++) if (!hero().overlaps(legacyRegion(i)) && empty(i)) return i;
        return -1;
    }
    /** Rearrange pixels without changing references, dimensions or game-role bindings. */
    public WorkshopCartridge transformed(SpriteRegion region,SpriteTransform operation) {
        return edited(graphics.transform(region,operation));
    }
    /** Explicit replacement of pixels; no allocation or game-role inference. */
    public WorkshopCartridge copyRegion(SpriteRegion source, SpriteRegion destination) {
        if(source.x<destination.x+destination.width&&destination.x<source.x+source.width
            &&source.y<destination.y+destination.height&&destination.y<source.y+source.height)
            throw new IllegalArgumentException("Выбери место вне исходной области");
        return edited(graphics.copy(source,destination));
    }
    public WorkshopCartridge copySprite(int source, int destination) {
        if(!hasHero())throw new IllegalArgumentException("Automatic allocation requires a known binding; use explicit copyRegion");
        checkSlot(source); checkSlot(destination);
        if (source == destination || hero().overlaps(legacyRegion(destination)) || !empty(destination))
            throw new IllegalArgumentException("Copy destination is occupied");
        if (empty(source)) throw new IllegalArgumentException("Нечего копировать: спрайт пустой");
        return edited(graphics.copy(legacyRegion(source),legacyRegion(destination)));
    }
    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SPRITE_COUNT) throw new IllegalArgumentException("Sprite slot out of range");
    }
}
