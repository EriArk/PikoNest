package art.pikoos.lab.core;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Portable interaction state. Input devices and Android persistence remain outside. */
public final class WorkshopSession {
    public enum Action { UP, DOWN, LEFT, RIGHT, CONFIRM, CANCEL, PREVIOUS, NEXT, TEST, UNDO, CONTEXT, MENU,
        SPRITE_SHEET, NEW_SPRITE, COPY_SPRITE, ASSIGN_HERO, DRAW_TOOLS, REGION, ZOOM }
    public enum Mode { NAVIGATE, VALUE, CANVAS, PALETTE, SHEET, HELP, MENU, ERROR, DRAW_TOOLS, REGION, HERO }
    public enum DrawTool { BRUSH, ERASER, FILL, LINE, PICKER }
    public interface Port {
        void save(byte[] bytes) throws Exception;
        void launch(byte[] bytes) throws Exception;
        default void library() throws Exception {}
    }
    private final Port port;
    private final ArrayDeque<WorkshopCartridge> undo = new ArrayDeque<>();
    private final int[] toolFocus = new int[3];
    private WorkshopCartridge cart;
    public Mode mode = Mode.NAVIGATE;
    public int tool, focus, codeLine, cursorX = 7, cursorY = 7, color = 14, paletteCursor = 14;
    public int field, draft, menuItem;
    public int spriteSlot, sheetFocus;
    public boolean swapAB, browsingSprites = true;
    public DrawTool drawTool=DrawTool.BRUSH;
    public DrawTool pickerReturn=DrawTool.BRUSH;
    public int drawToolCursor, lineX=-1, lineY=-1;
    public SpriteRegion region;
    public HeroBinding heroDraft;
    private Mode heroReturn=Mode.NAVIGATE;
    public boolean zoom, choosingEnd;
    public int regionX,regionY,anchorX,anchorY;
    private Mode regionReturn=Mode.SHEET;
    public String notice = "Сохранено", error = "";
    private Mode paletteReturn = Mode.NAVIGATE;
    private Mode overlayReturn = Mode.NAVIGATE;
    public WorkshopSession(WorkshopCartridge cart, Port port) { this.cart = cart; this.port = port; }
    public WorkshopCartridge cart() { return cart; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public int maxFocus(){return tool==2?(region==null?5:6):4;}
    public boolean pendingLine(){return drawTool==DrawTool.LINE&&lineX>=0&&lineY>=0;}
    public SpriteRegion selection(){return region==null?new SpriteRegion(spriteSlot*16,0,16,16):region;}
    public WorkshopCartridge canvasPreview(){return pendingLine()?cart.withLine(selection(),lineX,lineY,cursorX,cursorY,color):cart;}
    public SpriteRegion regionDraft(){
        return choosingEnd?new SpriteRegion(Math.min(anchorX,regionX)*8,Math.min(anchorY,regionY)*8,
            (Math.abs(anchorX-regionX)+1)*8,(Math.abs(anchorY-regionY)+1)*8):new SpriteRegion(regionX*8,regionY*8,8,8);
    }
    public void pointRegion(int x,int y){
        if(mode!=Mode.REGION)return;
        regionX=clamp(x,15);regionY=clamp(y,7);act(Action.CONFIRM);
    }
    private void chooseRegion(){
        regionReturn=mode;SpriteRegion r=selection();regionX=r.x/8;regionY=r.y/8;choosingEnd=false;mode=Mode.REGION;
    }
    public int displayedValue(int which) { return mode == Mode.VALUE && field == which ? draft : cart.value(which); }
    private static int clamp(int n, int max) { return Math.max(0, Math.min(max, n)); }
    private void save(WorkshopCartridge next, boolean remember) throws Exception {
        if (Arrays.equals(next.bytes(), cart.bytes())) return;
        port.save(next.bytes()); // Publish state only after durable storage succeeds.
        if (remember) {
            if (undo.size() == 32) undo.removeLast();
            undo.push(cart);
        }
        cart = next;
        notice = "Сохранено";
    }
    public void fail(Exception e) {
        error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        overlayReturn = mode==Mode.HERO?Mode.HERO:pendingLine()?Mode.CANVAS:tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE; mode = Mode.ERROR;
    }
    public void switchTool(int next) {
        if(pendingLine())return;
        if (mode != Mode.NAVIGATE && mode != Mode.CANVAS && mode != Mode.SHEET) return;
        if (tool == (next + 3) % 3) return;
        toolFocus[tool] = focus;
        tool = (next + 3) % 3; focus = toolFocus[tool]; mode = tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE;
    }
    public void select(int target) {
        if (mode != Mode.NAVIGATE) return;
        focus = clamp(target, maxFocus()); act(Action.CONFIRM);
    }
    public void openSprite(int slot) {
        if(pendingLine())return;
        if (slot < 0 || slot >= WorkshopCartridge.SPRITE_COUNT) throw new IllegalArgumentException("Sprite slot out of range");
        spriteSlot = slot; sheetFocus = slot; browsingSprites = false;
        region=null;cursorX=clamp(cursorX,15);cursorY=clamp(cursorY,15);zoom=false;
        tool = 2; focus = 0; mode = Mode.NAVIGATE;
    }
    public void selectSheet(int target) {
        if (mode != Mode.SHEET) return;
        sheetFocus = clamp(target, 11);
        if (sheetFocus < 8) spriteSlot = sheetFocus;
        act(Action.CONFIRM);
    }
    public void openHero(){
        if(pendingLine())return;
        HeroBinding h=cart.hero();
        if(h.image.sharesMap()){notice="Редактор общей области с картой ещё не готов";return;}
        if(h.card()>=0){openSprite(h.card());return;}
        region=h.image;cursorX=cursorY=0;browsingSprites=false;tool=2;focus=0;zoom=false;mode=Mode.NAVIGATE;
    }
    private void showSheet() { browsingSprites = true; sheetFocus = spriteSlot; region=null;cursorX=clamp(cursorX,15);cursorY=clamp(cursorY,15);mode = Mode.SHEET; }
    /** Read-only even if optional UI preferences outlive a restored/older cartridge. */
    public void previewHero(){
        if(tool!=2||(mode!=Mode.NAVIGATE&&mode!=Mode.CANVAS&&mode!=Mode.SHEET))return;
        try{
            if(cart.empty(selection())){notice="Сначала нарисуй хотя бы один пиксель";return;}
            HeroBinding candidate=cart.proposeHero(selection());
            heroDraft=candidate;heroReturn=mode;mode=Mode.HERO;
        }catch(Exception e){fail(e);}
    }
    private void assignHero() throws Exception {
        if(region!=null||!cart.legacyHero()){
            previewHero();return;
        }
        if (cart.empty(spriteSlot)) { notice = "Сначала нарисуй хотя бы один пиксель"; return; }
        save(cart.withHero(spriteSlot), true);
        notice = "Герой: спрайт " + (spriteSlot + 1);
    }
    private void createSprite(boolean copy) throws Exception {
        if(region!=null&&!browsingSprites){notice="Новый спрайт и копия пока доступны в карточках";return;}
        if (copy && cart.empty(spriteSlot)) { notice = "Пустой спрайт: пока нечего копировать"; return; }
        int destination = cart.firstFreeSlot();
        if (destination < 0) { notice = "Лист заполнен — свободных ячеек нет"; return; }
        if (copy) save(cart.copySprite(spriteSlot, destination), true);
        openSprite(destination);
        mode = Mode.CANVAS;
        notice = copy ? "Копия готова. Оригинал сохранён" : "Новый спрайт: выбери цвет и рисуй";
    }
    public void selectCodeLine(int line) {
        if (mode != Mode.NAVIGATE) return;
        codeLine = clamp(line, cart.code().split("\n", -1).length - 1);
        act(Action.CONFIRM);
    }
    public void paintAt(int x, int y) {
        if (tool != 2 || (mode != Mode.CANVAS && mode != Mode.NAVIGATE)) return;
        cursorX = clamp(x, selection().width-1); cursorY = clamp(y, selection().height-1); mode = Mode.CANVAS;
        act(Action.CONFIRM);
    }
    public void chooseColor(int value) {
        if (mode != Mode.PALETTE) return;
        paletteCursor = clamp(value, 15); act(Action.CONFIRM);
    }
    public void chooseDrawTool(int value){
        if(mode!=Mode.DRAW_TOOLS||value<0||value>=DrawTool.values().length)return;
        drawToolCursor=value;act(Action.CONFIRM);
    }
    private void draw()throws Exception {
        switch(drawTool){
            case BRUSH:save(cart.withPixel(selection(),cursorX,cursorY,color),true);break;
            case ERASER:save(cart.withPixel(selection(),cursorX,cursorY,0),true);break;
            case FILL:save(cart.withFill(selection(),cursorX,cursorY,color),true);break;
            case LINE:lineX=cursorX;lineY=cursorY;notice="Выбери конец линии";break;
            case PICKER:color=cart.pixel(selection(),cursorX,cursorY);drawTool=pickerReturn;notice="Цвет взят";break;
        }
    }
    private void moveCursor(Action action){
        if(action==Action.UP)cursorY=clamp(cursorY-1,selection().height-1);
        if(action==Action.DOWN)cursorY=clamp(cursorY+1,selection().height-1);
        if(action==Action.LEFT)cursorX=clamp(cursorX-1,selection().width-1);
        if(action==Action.RIGHT)cursorX=clamp(cursorX+1,selection().width-1);
    }
    private void edit(int which) { field = which; draft = cart.value(which); mode = Mode.VALUE; }
    public void act(Action action) {
        try {
            if (mode == Mode.ERROR) { if (action == Action.CANCEL || action == Action.CONFIRM) mode = overlayReturn; return; }
            if(mode==Mode.HERO){
                if(action==Action.CANCEL){mode=heroReturn;heroDraft=null;notice="Без изменений";}
                if(action==Action.CONFIRM||action==Action.TEST){
                    save(cart.withHero(heroDraft),true);mode=heroReturn;heroDraft=null;notice="Спрайт и столкновения героя сохранены";
                    if(action==Action.TEST)port.launch(cart.bytes());
                }
                return;
            }
            if(mode==Mode.REGION){
                if(action==Action.LEFT)regionX=clamp(regionX-1,15);
                if(action==Action.RIGHT)regionX=clamp(regionX+1,15);
                if(action==Action.UP)regionY=clamp(regionY-1,7);
                if(action==Action.DOWN)regionY=clamp(regionY+1,7);
                if(action==Action.CANCEL){if(choosingEnd)choosingEnd=false;else mode=regionReturn;}
                if(action==Action.CONFIRM){
                    if(!choosingEnd){anchorX=regionX;anchorY=regionY;choosingEnd=true;}
                    else{region=regionDraft();cursorX=cursorY=0;zoom=false;browsingSprites=false;focus=0;mode=Mode.CANVAS;notice="Область выбрана. Пиксели на своих местах";}
                }
                return; // Selection only: never write, launch, undo or leave via a tab accidentally.
            }
            if(pendingLine()){
                moveCursor(action);
                if(action==Action.CANCEL||action==Action.UNDO){lineX=lineY=-1;notice="Линия отменена";}
                if(action==Action.CONFIRM||action==Action.TEST){
                    save(canvasPreview(),true);lineX=lineY=-1;
                    if(action==Action.TEST)port.launch(cart.bytes());
                }
                return; // A draft cannot leak into other resources, tabs or runtime snapshots.
            }
            if(mode==Mode.DRAW_TOOLS){
                if(action==Action.UP)drawToolCursor=clamp(drawToolCursor-1,4);
                if(action==Action.DOWN)drawToolCursor=clamp(drawToolCursor+1,4);
                if(action==Action.CONFIRM){
                    DrawTool chosen=DrawTool.values()[drawToolCursor];
                    if(chosen==DrawTool.PICKER&&drawTool!=DrawTool.PICKER)pickerReturn=drawTool==DrawTool.ERASER?DrawTool.BRUSH:drawTool;
                    drawTool=chosen;mode=Mode.CANVAS;
                }
                if(action==Action.CANCEL)mode=overlayReturn;
                return;
            }
            if (mode == Mode.HELP) {
                if (action == Action.CONFIRM) {
                    if (tool == 2) showSheet();
                    else { mode = Mode.NAVIGATE; switchTool(1); codeLine = cart.line(field); }
                }
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                return;
            }
            if (mode == Mode.MENU) {
                if (action == Action.UP) menuItem = clamp(menuItem - 1, 4);
                if (action == Action.DOWN) menuItem = clamp(menuItem + 1, 4);
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                if (action == Action.CONFIRM) {
                    if (menuItem == 0) { mode = overlayReturn; act(Action.UNDO); }
                    if (menuItem == 1) { swapAB = !swapAB; notice = "Кнопки изменены"; }
                    if (menuItem == 2) mode = Mode.HELP;
                    if (menuItem == 3) mode = overlayReturn;
                    if (menuItem == 4) { mode = overlayReturn; port.library(); }
                }
                return;
            }
            if (mode == Mode.VALUE) {
                if (action == Action.LEFT) draft = Math.max(1, draft - 1);
                if (action == Action.RIGHT) draft = Math.min(4, draft + 1);
                if (action == Action.CANCEL) { mode = Mode.NAVIGATE; notice = "Без изменений"; }
                if (action == Action.CONFIRM || action == Action.TEST) {
                    save(cart.withValue(field, draft), true); mode = Mode.NAVIGATE;
                    if (action == Action.TEST) port.launch(cart.bytes());
                }
                return;
            }
            if (mode == Mode.PALETTE) {
                if (action == Action.LEFT) paletteCursor = (paletteCursor / 4) * 4 + (paletteCursor + 3) % 4;
                if (action == Action.RIGHT) paletteCursor = (paletteCursor / 4) * 4 + (paletteCursor + 1) % 4;
                if (action == Action.UP) paletteCursor = (paletteCursor + 12) % 16;
                if (action == Action.DOWN) paletteCursor = (paletteCursor + 4) % 16;
                if (action == Action.CONFIRM) { color = paletteCursor; if(drawTool==DrawTool.ERASER)drawTool=DrawTool.BRUSH; mode = paletteReturn; }
                if (action == Action.CANCEL) mode = paletteReturn;
                return;
            }
            if (action == Action.MENU) { overlayReturn = mode; mode = Mode.MENU; menuItem = 0; return; }
            if (action == Action.TEST) { port.launch(cart.bytes()); return; }
            if (action == Action.UNDO) {
                if (!undo.isEmpty()) { save(undo.peek(), false); undo.pop(); notice = "Изменение отменено"; }
                else notice = "Нет изменений для отмены";
                return;
            }
            if (action == Action.PREVIOUS) { switchTool(tool - 1); return; }
            if (action == Action.NEXT) { switchTool(tool + 1); return; }
            if (tool == 2) {
                if(action==Action.REGION){chooseRegion();return;}
                if(action==Action.ZOOM&&!browsingSprites){zoom=!zoom;return;}
                if(action==Action.DRAW_TOOLS&&!browsingSprites){overlayReturn=mode;drawToolCursor=drawTool.ordinal();mode=Mode.DRAW_TOOLS;return;}
                if (action == Action.SPRITE_SHEET) { showSheet(); return; }
                if (action == Action.NEW_SPRITE) { createSprite(false); return; }
                if (action == Action.COPY_SPRITE) { createSprite(true); return; }
                if (action == Action.ASSIGN_HERO) { assignHero(); return; }
            }
            if (mode == Mode.SHEET) {
                if (action == Action.LEFT) {
                    sheetFocus = sheetFocus < 8 ? (sheetFocus / 4) * 4 + (sheetFocus + 3) % 4 : Math.max(8, sheetFocus - 1);
                }
                if (action == Action.RIGHT) {
                    sheetFocus = sheetFocus < 8 ? (sheetFocus / 4) * 4 + (sheetFocus + 1) % 4 : Math.min(11, sheetFocus + 1);
                }
                if (action == Action.UP) sheetFocus = sheetFocus >= 8 ? spriteSlot : Math.max(0, sheetFocus - 4);
                if (action == Action.DOWN) sheetFocus = sheetFocus < 4 ? sheetFocus + 4 : sheetFocus < 8 ? 8 + Math.min(2, sheetFocus % 4) : 11;
                if (sheetFocus < 8) spriteSlot = sheetFocus;
                if (action == Action.CANCEL) switchTool(0);
                if (action == Action.CONTEXT) createSprite(true);
                if (action == Action.CONFIRM) {
                    if (sheetFocus < 8) openSprite(spriteSlot);
                    else if (sheetFocus == 8) createSprite(false);
                    else if (sheetFocus == 9) createSprite(true);
                    else if(sheetFocus==10)assignHero();
                    else chooseRegion();
                }
                return;
            }
            if (action == Action.CONTEXT) {
                if (tool == 2) { paletteReturn = mode; paletteCursor = color; mode = Mode.PALETTE; }
                else { overlayReturn = mode; field = (tool == 0 && focus == 1) || (tool == 1 && codeLine == cart.line(1)) ? 1 : 0; mode = Mode.HELP; }
                return;
            }
            if (mode == Mode.CANVAS) {
                moveCursor(action);
                if (action == Action.CONFIRM) draw();
                if (action == Action.CANCEL) mode = Mode.NAVIGATE;
                return;
            }
            if (action == Action.CANCEL) {
                if (tool == 2) showSheet();
                else { overlayReturn = mode; mode = Mode.MENU; menuItem = 3; }
                return;
            }
            if (tool == 1) {
                int max = cart.code().split("\n", -1).length - 1;
                if (action == Action.UP) codeLine = clamp(codeLine - 1, max);
                if (action == Action.DOWN) codeLine = clamp(codeLine + 1, max);
                if (action == Action.LEFT) codeLine = clamp(codeLine - 8, max);
                if (action == Action.RIGHT) codeLine = clamp(codeLine + 8, max);
                if (action == Action.CONFIRM) {
                    if (codeLine == cart.line(0)) edit(0);
                    else if (codeLine == cart.line(1)) edit(1);
                    else notice = "Пока изменяются speed и jump";
                }
                return;
            }
            if (action == Action.UP || action == Action.LEFT) focus = clamp(focus - 1, maxFocus());
            if (action == Action.DOWN || action == Action.RIGHT) focus = clamp(focus + 1, maxFocus());
            if (action == Action.CONFIRM) {
                if (tool == 0) {
                    if (focus < 2) edit(focus);
                    if (focus == 2) { toolFocus[0] = focus; openHero(); }
                    else if (focus == 3) { switchTool(1); codeLine = cart.line(0); }
                    else if (focus == 4) { overlayReturn = mode; field = 0; mode = Mode.HELP; }
                } else {
                    if (focus == 0) mode = Mode.CANVAS;
                    if (focus == 1) act(Action.DRAW_TOOLS);
                    if (focus == 2) act(Action.CONTEXT);
                    if (focus == 3) showSheet();
                    if (focus == 4) { if(region!=null)chooseRegion();else assignHero(); }
                    if (focus == 5) act(Action.ZOOM);
                    if (focus == 6) assignHero();
                }
            }
        } catch (Exception e) { fail(e); }
    }
}
