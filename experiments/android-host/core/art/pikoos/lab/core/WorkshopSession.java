package art.pikoos.lab.core;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Portable interaction state. Input devices and Android persistence remain outside. */
public final class WorkshopSession {
    public enum Action { UP, DOWN, LEFT, RIGHT, CONFIRM, CANCEL, PREVIOUS, NEXT, TEST, UNDO, CONTEXT, MENU,
        SPRITE_SHEET, NEW_SPRITE, COPY_SPRITE, ASSIGN_HERO }
    public enum Mode { NAVIGATE, VALUE, CANVAS, PALETTE, SHEET, HELP, MENU, ERROR }
    public interface Port {
        void save(byte[] bytes) throws Exception;
        void launch(byte[] bytes) throws Exception;
    }
    private final Port port;
    private final ArrayDeque<WorkshopCartridge> undo = new ArrayDeque<>();
    private final int[] toolFocus = new int[3];
    private WorkshopCartridge cart;
    public Mode mode = Mode.NAVIGATE;
    public int tool, focus, codeLine, cursorX = 7, cursorY = 7, color = 14, paletteCursor = 14;
    public int field, draft, menuItem;
    public int spriteSlot, sheetFocus;
    public boolean eraser, swapAB, browsingSprites = true;
    public String notice = "Сохранено", error = "";
    private Mode paletteReturn = Mode.NAVIGATE;
    private Mode overlayReturn = Mode.NAVIGATE;
    public WorkshopSession(WorkshopCartridge cart, Port port) { this.cart = cart; this.port = port; }
    public WorkshopCartridge cart() { return cart; }
    public boolean canUndo() { return !undo.isEmpty(); }
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
        overlayReturn = tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE; mode = Mode.ERROR;
    }
    public void switchTool(int next) {
        if (mode != Mode.NAVIGATE && mode != Mode.CANVAS && mode != Mode.SHEET) return;
        if (tool == (next + 3) % 3) return;
        toolFocus[tool] = focus;
        tool = (next + 3) % 3; focus = toolFocus[tool]; mode = tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE;
    }
    public void select(int target) {
        if (mode != Mode.NAVIGATE) return;
        focus = clamp(target, 4); act(Action.CONFIRM);
    }
    public void openSprite(int slot) {
        if (slot < 0 || slot >= WorkshopCartridge.SPRITE_COUNT) throw new IllegalArgumentException("Sprite slot out of range");
        spriteSlot = slot; sheetFocus = slot; browsingSprites = false;
        tool = 2; focus = 0; mode = Mode.NAVIGATE;
    }
    public void selectSheet(int target) {
        if (mode != Mode.SHEET) return;
        sheetFocus = clamp(target, 10);
        if (sheetFocus < 8) spriteSlot = sheetFocus;
        act(Action.CONFIRM);
    }
    private void showSheet() { browsingSprites = true; sheetFocus = spriteSlot; mode = Mode.SHEET; }
    private void assignHero() throws Exception {
        if (cart.empty(spriteSlot)) { notice = "Сначала нарисуй хотя бы один пиксель"; return; }
        save(cart.withHero(spriteSlot), true);
        notice = "Герой: рисунок " + (spriteSlot + 1);
    }
    private void createSprite(boolean copy) throws Exception {
        if (copy && cart.empty(spriteSlot)) { notice = "Пустой рисунок: пока нечего копировать"; return; }
        int destination = cart.firstFreeSlot();
        if (destination < 0) { notice = "Лист заполнен — свободных ячеек нет"; return; }
        if (copy) save(cart.copySprite(spriteSlot, destination), true);
        openSprite(destination);
        mode = Mode.CANVAS;
        notice = copy ? "Копия готова. Оригинал сохранён" : "Новый рисунок: выбери цвет и рисуй";
    }
    public void selectCodeLine(int line) {
        if (mode != Mode.NAVIGATE) return;
        codeLine = clamp(line, cart.code().split("\n", -1).length - 1);
        act(Action.CONFIRM);
    }
    public void paintAt(int x, int y) {
        if (tool != 2 || (mode != Mode.CANVAS && mode != Mode.NAVIGATE)) return;
        cursorX = clamp(x, 15); cursorY = clamp(y, 15); mode = Mode.CANVAS;
        act(Action.CONFIRM);
    }
    public void chooseColor(int value) {
        if (mode != Mode.PALETTE) return;
        paletteCursor = clamp(value, 15); act(Action.CONFIRM);
    }
    private void edit(int which) { field = which; draft = cart.value(which); mode = Mode.VALUE; }
    public void act(Action action) {
        try {
            if (mode == Mode.ERROR) { if (action == Action.CANCEL || action == Action.CONFIRM) mode = overlayReturn; return; }
            if (mode == Mode.HELP) {
                if (action == Action.CONFIRM) {
                    if (tool == 2) showSheet();
                    else { mode = Mode.NAVIGATE; switchTool(1); codeLine = cart.line(field); }
                }
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                return;
            }
            if (mode == Mode.MENU) {
                if (action == Action.UP) menuItem = clamp(menuItem - 1, 3);
                if (action == Action.DOWN) menuItem = clamp(menuItem + 1, 3);
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                if (action == Action.CONFIRM) {
                    if (menuItem == 0) { mode = overlayReturn; act(Action.UNDO); }
                    if (menuItem == 1) { swapAB = !swapAB; notice = "Кнопки изменены"; }
                    if (menuItem == 2) mode = Mode.HELP;
                    if (menuItem == 3) mode = overlayReturn;
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
                if (action == Action.CONFIRM) { color = paletteCursor; eraser = false; mode = paletteReturn; }
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
                    sheetFocus = sheetFocus < 8 ? (sheetFocus / 4) * 4 + (sheetFocus + 1) % 4 : Math.min(10, sheetFocus + 1);
                }
                if (action == Action.UP) sheetFocus = sheetFocus >= 8 ? spriteSlot : Math.max(0, sheetFocus - 4);
                if (action == Action.DOWN) sheetFocus = sheetFocus < 4 ? sheetFocus + 4 : sheetFocus < 8 ? 8 + Math.min(2, sheetFocus % 4) : sheetFocus;
                if (sheetFocus < 8) spriteSlot = sheetFocus;
                if (action == Action.CANCEL) switchTool(0);
                if (action == Action.CONTEXT) createSprite(true);
                if (action == Action.CONFIRM) {
                    if (sheetFocus < 8) openSprite(spriteSlot);
                    else if (sheetFocus == 8) createSprite(false);
                    else if (sheetFocus == 9) createSprite(true);
                    else assignHero();
                }
                return;
            }
            if (action == Action.CONTEXT) {
                if (tool == 2) { paletteReturn = mode; paletteCursor = color; mode = Mode.PALETTE; }
                else { overlayReturn = mode; field = (tool == 0 && focus == 1) || (tool == 1 && codeLine == cart.line(1)) ? 1 : 0; mode = Mode.HELP; }
                return;
            }
            if (mode == Mode.CANVAS) {
                if (action == Action.UP) cursorY = clamp(cursorY - 1, 15);
                if (action == Action.DOWN) cursorY = clamp(cursorY + 1, 15);
                if (action == Action.LEFT) cursorX = clamp(cursorX - 1, 15);
                if (action == Action.RIGHT) cursorX = clamp(cursorX + 1, 15);
                if (action == Action.CONFIRM) save(cart.withPixel(spriteSlot, cursorX, cursorY, eraser ? 0 : color), true);
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
            if (action == Action.UP || action == Action.LEFT) focus = clamp(focus - 1, 4);
            if (action == Action.DOWN || action == Action.RIGHT) focus = clamp(focus + 1, 4);
            if (action == Action.CONFIRM) {
                if (tool == 0) {
                    if (focus < 2) edit(focus);
                    if (focus == 2) { toolFocus[0] = focus; openSprite(cart.heroSlot()); }
                    else if (focus == 3) { switchTool(1); codeLine = cart.line(0); }
                    else if (focus == 4) { overlayReturn = mode; field = 0; mode = Mode.HELP; }
                } else {
                    if (focus == 0) mode = Mode.CANVAS;
                    if (focus == 1) eraser = !eraser;
                    if (focus == 2) act(Action.CONTEXT);
                    if (focus == 3) showSheet();
                    if (focus == 4) assignHero();
                }
            }
        } catch (Exception e) { fail(e); }
    }
}
