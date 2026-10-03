package art.pikoos.lab.core;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Portable interaction state. Input devices and Android persistence remain outside. */
public final class WorkshopSession {
    public enum Action { UP, DOWN, LEFT, RIGHT, CONFIRM, CANCEL, PREVIOUS, NEXT, TEST, UNDO, CONTEXT, MENU,
        SPRITE_SHEET, NEW_SPRITE, COPY_SPRITE, ASSIGN_HERO, DRAW_TOOLS, REGION, ZOOM, ASSETS }
    public enum Mode { NAVIGATE, VALUE, CANVAS, PALETTE, SHEET, HELP, MENU, ERROR, DRAW_TOOLS, REGION, HERO, COPY_PLACE, COPY_CONFIRM, ASSETS, ASSET_SAVE, NAME, TRANSFORM, RECOLOR }
    public enum DrawTool { BRUSH, ERASER, FILL, LINE, PICKER }
    public interface Port {
        void save(byte[] bytes) throws Exception;
        void launch(byte[] bytes) throws Exception;
        default void library() throws Exception {}
        default java.util.List<SpriteAsset> assets()throws Exception{return java.util.Collections.emptyList();}
        default void storeAsset(SpriteAsset asset)throws Exception{throw new Exception("Хранилище ресурсов не подключено");}
        default void renameAsset(SpriteAsset expected,String title)throws Exception{throw new Exception("Переименование не подключено");}
        default String projectOrigin(){return "Проект";}
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
    private WorkshopCartridge recolorPreview;
    private Mode recolorReturn=Mode.CANVAS;
    private int recolorFrom,recolorTo,recolorField,recolorCount;
    public boolean recoloring(){return recolorPreview!=null;}
    public WorkshopCartridge recolorPreview(){return recolorPreview;}
    public int recolorFrom(){return recolorFrom;}
    public int recolorTo(){return recolorTo;}
    public int recolorField(){return recolorField;}
    public int recolorCount(){return recolorCount;}
    public String recolorReturnMode(){return recolorReturn.name();}
    public void selectRecolorField(int field){if(mode==Mode.RECOLOR&&field>=0&&field<=1)recolorField=field;}
    public void chooseRecolor(int value){
        if(mode!=Mode.RECOLOR||value<0||value>15)return;
        if(recolorField==0)recolorFrom=value;else recolorTo=value;
        refreshRecolor();
    }
    private void refreshRecolor(){
        SpriteRegion r=selection();recolorCount=0;
        if(recolorFrom!=recolorTo)for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)
            if(cart.pixel(r,x,y)==recolorFrom)recolorCount++;
        recolorPreview=cart.replaceColor(r,recolorFrom,recolorTo);
    }
    /** Restore UI intent against current canonical pixels; never write on restore. */
    public void restoreRecolor(int from,int to,int field,String origin){
        if(tool!=2||browsingSprites||pendingLine()||selection().sharesMap()
            ||(mode!=Mode.CANVAS&&mode!=Mode.NAVIGATE)||from<0||from>15||to<0||to>15||field<0||field>1)return;
        Mode parsed;try{parsed=Mode.valueOf(origin);}catch(IllegalArgumentException e){return;}
        if(parsed!=Mode.CANVAS&&parsed!=Mode.NAVIGATE)return;
        recolorFrom=from;recolorTo=to;recolorField=field;recolorReturn=parsed;
        refreshRecolor();mode=Mode.RECOLOR;
    }
    private SpriteTransform transform;
    private WorkshopCartridge transformPreview;
    private Mode transformReturn=Mode.CANVAS;
    public boolean transforming(){return transform!=null;}
    public SpriteTransform transformOperation(){return transform;}
    public String transformReturnMode(){return transformReturn.name();}
    public WorkshopCartridge transformPreview(){return transformPreview;}
    public boolean transformAllowed(){return transforming()&&transform.supports(selection());}
    public void chooseTransform(int index){
        if(mode!=Mode.TRANSFORM||index<0||index>=SpriteTransform.values().length)return;
        transform=SpriteTransform.values()[index];
        transformPreview=transformAllowed()?cart.transformed(selection(),transform):cart;
    }
    /** A restored operation is only a preview of current canonical pixels. Never auto-apply. */
    public void restoreTransform(String operation,String origin){
        if(tool!=2||browsingSprites||pendingLine()||selection().sharesMap()
            ||(mode!=Mode.CANVAS&&mode!=Mode.NAVIGATE))return;
        try{
            Mode parsed=Mode.valueOf(origin);SpriteTransform chosen=SpriteTransform.valueOf(operation);
            if(parsed!=Mode.CANVAS&&parsed!=Mode.NAVIGATE)return;
            transformReturn=parsed;mode=Mode.TRANSFORM;chooseTransform(chosen.ordinal());
        }catch(IllegalArgumentException e){/* Ignore invalid optional UI metadata. */}
    }
    public SpriteRegion region;
    public HeroBinding heroDraft;
    private Mode heroReturn=Mode.NAVIGATE;
    public boolean zoom, choosingEnd;
    public int regionX,regionY,anchorX,anchorY;
    private Mode regionReturn=Mode.SHEET;
    public SpriteRegion copySource;
    public int copyX,copyY;
    private Mode copyReturn=Mode.SHEET;
    private Mode assetsReturn=Mode.NAVIGATE;
    private final java.util.ArrayList<SpriteAsset> assets=new java.util.ArrayList<>();
    public int assetIndex;
    public SpriteAsset assetDraft,copyAsset;
    public SpriteAsset nameTarget;
    public NameEditor nameEditor;
    public boolean namingNewAsset;
    public java.util.List<SpriteAsset> assets(){return java.util.Collections.unmodifiableList(assets);}
    public SpriteAsset currentAsset(){return assets.isEmpty()?null:assets.get(clamp(assetIndex,assets.size()-1));}
    public String assetsReturnMode(){return assetsReturn.name();}
    private void showAssets()throws Exception{
        String selected=currentAsset()==null?"":currentAsset().id;
        assetsReturn=mode;mode=Mode.ASSETS;
        java.util.List<SpriteAsset> loaded=port.assets();assets.clear();assets.addAll(loaded);assetIndex=clamp(assetIndex,assets.size()-1);
        selectAssetId(selected);
    }
    public void selectAssetId(String id){for(int i=0;i<assets.size();i++)if(assets.get(i).id.equals(id)){assetIndex=i;return;}}
    private void publishAsset(SpriteAsset asset){
        boolean found=false;
        for(int i=0;i<assets.size();i++)if(assets.get(i).id.equals(asset.id)){assets.set(i,asset);found=true;break;}
        if(!found)assets.add(asset);
        assets.sort((a,b)->{int c=a.title.compareTo(b.title);return c==0?a.id.compareTo(b.id):c;});
        selectAssetId(asset.id);
    }
    private void beginName(){
        namingNewAsset=mode==Mode.ASSET_SAVE;nameTarget=namingNewAsset?assetDraft:currentAsset();
        if(nameTarget==null)return;nameEditor=new NameEditor(nameTarget.title);mode=Mode.NAME;
    }
    public void typeName(int index){if(mode==Mode.NAME)nameEditor.type(index);}
    public void restoreName(SpriteAsset target,boolean isNew,NameEditor editor){
        if(mode!=Mode.ASSETS&&mode!=Mode.ASSET_SAVE)return;
        nameTarget=target;namingNewAsset=isNew;nameEditor=editor;
        if(isNew)assetDraft=target;mode=Mode.NAME;
    }
    private void finishName()throws Exception{
        String title=nameEditor.value();if(title==null)return;
        SpriteAsset renamed=nameTarget.withTitle(title);
        if(namingNewAsset)assetDraft=renamed;
        else if(!title.equals(nameTarget.title)){
            port.renameAsset(nameTarget,title);
            publishAsset(renamed);
        }
        mode=namingNewAsset?Mode.ASSET_SAVE:Mode.ASSETS;nameEditor=null;nameTarget=null;notice="Название сохранено";
    }
    public void selectAsset(int index){if(mode!=Mode.ASSETS)return;assetIndex=clamp(index,assets.size()-1);act(Action.CONFIRM);}
    public void restoreAssets(String returnMode,int index)throws Exception{
        Mode origin;
        try{origin=Mode.valueOf(returnMode);}catch(IllegalArgumentException e){return;}
        if(origin!=Mode.SHEET&&origin!=Mode.NAVIGATE&&origin!=Mode.CANVAS)return;
        mode=origin;showAssets();assetIndex=clamp(index,assets.size()-1);
    }
    public void restoreInsertion(SpriteAsset asset,int x,int y){
        if(mode!=Mode.ASSETS||asset.height>64||asset.width%8!=0||asset.height%8!=0
            ||x<0||y<0||x%8!=0||y%8!=0||x+asset.width>128||y+asset.height>64)return;
        copyAsset=asset;copySource=new SpriteRegion(0,0,asset.width,asset.height);
        copyX=x;copyY=y;copyReturn=Mode.ASSETS;mode=Mode.COPY_PLACE;
    }
    private void beginInsertion(SpriteAsset asset){
        if(asset.height>64||asset.width%8!=0||asset.height%8!=0){notice="Размещение этого размера пока не поддерживается";return;}
        copyAsset=asset;copySource=new SpriteRegion(0,0,asset.width,asset.height);copyReturn=Mode.ASSETS;
        suggestCopyPlace();
    }
    public boolean copying(){return copySource!=null;}
    public SpriteRegion copyDestination(){return new SpriteRegion(copyX,copyY,copySource.width,copySource.height);}
    public boolean copyOverlaps(){
        if(copyAsset!=null)return false;
        SpriteRegion a=copySource,b=copyDestination();
        return a.x<b.x+b.width&&b.x<a.x+a.width&&a.y<b.y+b.height&&b.y<a.y+a.height;
    }
    private void beginCopy(){
        if(selection().sharesMap()){notice="Редактор общей области с картой ещё не готов";return;}
        if(cart.empty(selection())){notice="Пустая область: пока нечего копировать";return;}
        copySource=selection();copyReturn=mode;copyX=copySource.x;copyY=copySource.y;
        suggestCopyPlace();
    }
    private void suggestCopyPlace(){
        // Suggest a visually empty destination, never assume it is unused by Lua.
        search:for(int y=0;y<=64-copySource.height;y+=8)for(int x=0;x<=128-copySource.width;x+=8){
            copyX=x;copyY=y;if(!copyOverlaps()&&cart.empty(copyDestination()))break search;
        }
        mode=Mode.COPY_PLACE;
    }
    public void pointCopy(int x,int y){
        if(mode!=Mode.COPY_PLACE)return;
        copyX=clamp(x, (128-copySource.width)/8)*8;copyY=clamp(y,(64-copySource.height)/8)*8;
    }
    /** Restore only a validated draft; always re-show the replacement before saving. */
    public void restoreCopy(int x,int y,String returnMode){
        if(tool!=2||pendingLine()||cart.empty(selection())||selection().sharesMap())return;
        Mode origin;
        try{origin=Mode.valueOf(returnMode);}catch(IllegalArgumentException e){return;}
        if(origin!=Mode.SHEET&&origin!=Mode.CANVAS&&origin!=Mode.NAVIGATE)return;
        if(x<0||y<0||x%8!=0||y%8!=0||x+selection().width>128||y+selection().height>64)return;
        copySource=selection();copyX=x;copyY=y;copyReturn=origin;mode=Mode.COPY_PLACE;
    }
    public String copyReturnMode(){return copyReturn.name();}
    public String notice = "Сохранено", error = "";
    private Mode paletteReturn = Mode.NAVIGATE;
    private Mode overlayReturn = Mode.NAVIGATE;
    public WorkshopSession(WorkshopCartridge cart, Port port) { this.cart = cart; this.port = port; }
    public WorkshopCartridge cart() { return cart; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public int maxFocus(){return tool==2?(cart.hasHero()?7:6):cart.hasHero()?4:2;}
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
        overlayReturn = mode==Mode.RECOLOR||mode==Mode.TRANSFORM||mode==Mode.NAME||mode==Mode.COPY_CONFIRM||mode==Mode.ASSETS||mode==Mode.ASSET_SAVE?mode:mode==Mode.HERO?Mode.HERO:pendingLine()?Mode.CANVAS:tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE; mode = Mode.ERROR;
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
        if(pendingLine()||transforming()||recoloring())return;
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
        if(!cart.hasHero())return;
        if(pendingLine()||transforming()||recoloring())return;
        HeroBinding h=cart.hero();
        if(h.image.sharesMap()){notice="Редактор общей области с картой ещё не готов";return;}
        if(h.card()>=0){openSprite(h.card());return;}
        region=h.image;cursorX=cursorY=0;browsingSprites=false;tool=2;focus=0;zoom=false;mode=Mode.NAVIGATE;
    }
    private void showSheet() { browsingSprites = true; sheetFocus = spriteSlot; region=null;cursorX=clamp(cursorX,15);cursorY=clamp(cursorY,15);mode = Mode.SHEET; }
    /** Read-only even if optional UI preferences outlive a restored/older cartridge. */
    public void previewHero(){
        if(!cart.hasHero())return;
        if(tool!=2||(mode!=Mode.NAVIGATE&&mode!=Mode.CANVAS&&mode!=Mode.SHEET))return;
        try{
            if(cart.empty(selection())){notice="Сначала нарисуй хотя бы один пиксель";return;}
            HeroBinding candidate=cart.proposeHero(selection());
            heroDraft=candidate;heroReturn=mode;mode=Mode.HERO;
        }catch(Exception e){fail(e);}
    }
    private void assignHero() throws Exception {
        if(!cart.hasHero())return;
        if(region!=null||!cart.legacyHero()){
            previewHero();return;
        }
        if (cart.empty(spriteSlot)) { notice = "Сначала нарисуй хотя бы один пиксель"; return; }
        save(cart.withHero(spriteSlot), true);
        notice = "Герой: спрайт " + (spriteSlot + 1);
    }
    private void createSprite(boolean copy) throws Exception {
        if(copy){beginCopy();return;}
        if(!cart.hasHero()){
            chooseRegion();notice="Выбери место на листе";return;
        }
        if(region!=null&&!browsingSprites){chooseRegion();return;}
        int destination = cart.firstFreeSlot();
        if (destination < 0) { notice = "Лист заполнен — свободных ячеек нет"; return; }
        openSprite(destination);
        mode = Mode.CANVAS;
        notice = "Новый спрайт: выбери цвет и рисуй";
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
        if(mode!=Mode.DRAW_TOOLS||value<0||value>DrawTool.values().length+1)return;
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
            if(mode==Mode.RECOLOR){
                int selected=recolorField==0?recolorFrom:recolorTo;
                if(action==Action.LEFT)chooseRecolor(selected/8*8+(selected+7)%8);
                if(action==Action.RIGHT)chooseRecolor(selected/8*8+(selected+1)%8);
                if(action==Action.UP||action==Action.DOWN)chooseRecolor((selected+8)%16);
                if(action==Action.CONTEXT)selectRecolorField(1-recolorField);
                if(action==Action.CONFIRM){
                    boolean differs=recolorCount>0;save(recolorPreview,true);mode=recolorReturn;recolorPreview=null;
                    notice=differs?"Цвет заменён · Y отмена":"Пиксели не изменились";
                }
                if(action==Action.CANCEL){mode=recolorReturn;recolorPreview=null;notice="Без изменений";}
                return;
            }
            if(mode==Mode.TRANSFORM){
                if(action==Action.LEFT||action==Action.UP)chooseTransform((transform.ordinal()+3)%4);
                if(action==Action.RIGHT||action==Action.DOWN)chooseTransform((transform.ordinal()+1)%4);
                if(action==Action.CONFIRM&&transformAllowed()){
                    boolean differs=!Arrays.equals(cart.bytes(),transformPreview.bytes());
                    save(transformPreview,true);mode=transformReturn;transform=null;transformPreview=null;
                    notice=differs?"Применено · Y отмена":"Пиксели не изменились";
                }
                if(action==Action.CANCEL){mode=transformReturn;transform=null;transformPreview=null;notice="Без изменений";}
                return;
            }
            if(mode==Mode.NAME){
                if(action==Action.CANCEL){mode=namingNewAsset?Mode.ASSET_SAVE:Mode.ASSETS;nameEditor=null;nameTarget=null;notice="Название не изменено";return;}
                if(action==Action.LEFT)nameEditor.move(-1,0);
                if(action==Action.RIGHT)nameEditor.move(1,0);
                if(action==Action.UP)nameEditor.move(0,-1);
                if(action==Action.DOWN)nameEditor.move(0,1);
                if(action==Action.CONFIRM)nameEditor.type(nameEditor.key);
                if(action==Action.CONTEXT)nameEditor.erase();
                if(action==Action.UNDO)nameEditor.uppercase=!nameEditor.uppercase;
                if(action==Action.MENU)nameEditor.replaceAll=!nameEditor.replaceAll;
                if(action==Action.PREVIOUS||action==Action.NEXT)nameEditor.language();
                if(action==Action.TEST)finishName();
                return;
            }
            if(mode==Mode.ASSET_SAVE){
                if(action==Action.CONTEXT)beginName();
                if(action==Action.CANCEL){assetDraft=null;mode=Mode.ASSETS;notice="Сохранение ресурса отменено";}
                if(action==Action.CONFIRM){
                    port.storeAsset(assetDraft);
                    publishAsset(assetDraft);assetDraft=null;mode=Mode.ASSETS;notice="Спрайт в библиотеке";
                }
                return;
            }
            if(mode==Mode.ASSETS){
                if(action==Action.UNDO){beginName();return;}
                if(action==Action.CANCEL){mode=assetsReturn;return;}
                if(action==Action.LEFT||action==Action.UP)assetIndex=clamp(assetIndex-1,assets.size()-1);
                if(action==Action.RIGHT||action==Action.DOWN)assetIndex=clamp(assetIndex+1,assets.size()-1);
                if(action==Action.CONFIRM){
                    if(currentAsset()!=null)beginInsertion(currentAsset());
                    else if(tool==2)act(Action.CONTEXT);
                    else{mode=assetsReturn;switchTool(2);}
                }
                if(action==Action.CONTEXT){
                    if(tool!=2){notice="Сначала выбери спрайт во вкладке «Спрайты»";return;}
                    if(cart.empty(selection())){notice="В выделении пока нет пикселей";return;}
                    assetDraft=SpriteAsset.capture(cart,selection(),"Спрайт "+(assets.size()+1),port.projectOrigin());mode=Mode.ASSET_SAVE;
                }
                return;
            }
            if(mode==Mode.COPY_PLACE||mode==Mode.COPY_CONFIRM){
                if(action==Action.CANCEL){
                    if(mode==Mode.COPY_CONFIRM)mode=Mode.COPY_PLACE;
                    else{mode=copyReturn;copySource=null;copyAsset=null;notice="Размещение отменено";}
                    return;
                }
                if(mode==Mode.COPY_PLACE){
                    if(action==Action.LEFT)pointCopy(copyX/8-1,copyY/8);
                    if(action==Action.RIGHT)pointCopy(copyX/8+1,copyY/8);
                    if(action==Action.UP)pointCopy(copyX/8,copyY/8-1);
                    if(action==Action.DOWN)pointCopy(copyX/8,copyY/8+1);
                    if(action==Action.CONFIRM&&!copyOverlaps())mode=Mode.COPY_CONFIRM;
                }else if(action==Action.CONFIRM){
                    SpriteRegion target=copyDestination();
                    boolean insertion=copyAsset!=null;
                    save(insertion?cart.insert(copyAsset,target):cart.copyRegion(copySource,target),true);
                    copySource=null;copyAsset=null;
                    if(target.y==0&&target.width==16&&target.height==16&&target.x%16==0)openSprite(target.x/16);
                    else{region=target;tool=2;focus=0;browsingSprites=false;cursorX=cursorY=0;zoom=false;}
                    mode=Mode.CANVAS;notice=insertion?"Спрайт вставлен. Можно редактировать":"Копия готова. Исходные пиксели сохранены";
                }
                return; // No launch, tab switching, undo or implicit commit during placement.
            }
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
                if(action==Action.UP)drawToolCursor=clamp(drawToolCursor-1,6);
                if(action==Action.DOWN)drawToolCursor=clamp(drawToolCursor+1,6);
                if(action==Action.CONFIRM){
                    if(drawToolCursor==6){
                        Mode origin=overlayReturn;mode=origin;
                        restoreRecolor(cart.pixel(selection(),cursorX,cursorY),color,1,origin.name());return;
                    }
                    if(drawToolCursor==5){
                        Mode origin=overlayReturn;mode=origin;
                        restoreTransform(SpriteTransform.FLIP_HORIZONTAL.name(),origin.name());return;
                    }
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
                    else { mode = Mode.NAVIGATE; switchTool(1); codeLine = Math.max(0,cart.line(field)); }
                }
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                return;
            }
            if (mode == Mode.MENU) {
                if (action == Action.UP) menuItem = clamp(menuItem - 1, 5);
                if (action == Action.DOWN) menuItem = clamp(menuItem + 1, 5);
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                if (action == Action.CONFIRM) {
                    if (menuItem == 0) { mode = overlayReturn; act(Action.UNDO); }
                    if (menuItem == 1) { swapAB = !swapAB; notice = "Кнопки изменены"; }
                    if (menuItem == 2) mode = Mode.HELP;
                    if (menuItem == 3) mode = overlayReturn;
                    if (menuItem == 4) { mode = overlayReturn; port.library(); }
                    if (menuItem == 5) { mode = overlayReturn; showAssets(); }
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
            if (action == Action.ASSETS) { showAssets();return; }
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
                    else if(sheetFocus==10){if(cart.hasHero())assignHero();else{overlayReturn=mode;mode=Mode.HELP;}}
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
                    else notice = cart.hasHero()?"Пока изменяются speed и jump":"Код открыт для просмотра";
                }
                return;
            }
            if (action == Action.UP || action == Action.LEFT) focus = clamp(focus - 1, maxFocus());
            if (action == Action.DOWN || action == Action.RIGHT) focus = clamp(focus + 1, maxFocus());
            if (action == Action.CONFIRM) {
                if (tool == 0) {
                    if(!cart.hasHero()){
                        if(focus==0)switchTool(2);
                        else if(focus==1)switchTool(1);
                        else{overlayReturn=mode;mode=Mode.HELP;}
                        return;
                    }
                    if (focus < 2) edit(focus);
                    if (focus == 2) { toolFocus[0] = focus; openHero(); }
                    else if (focus == 3) { switchTool(1); codeLine = cart.line(0); }
                    else if (focus == 4) { overlayReturn = mode; field = 0; mode = Mode.HELP; }
                } else {
                    if (focus == 0) mode = Mode.CANVAS;
                    if (focus == 1) act(Action.DRAW_TOOLS);
                    if (focus == 2) act(Action.CONTEXT);
                    if (focus == 3) showSheet();
                    if (focus == 4) chooseRegion();
                    if (focus == 5) act(Action.ZOOM);
                    if (focus == 6) createSprite(true);
                    if (focus == 7) assignHero();
                }
            }
        } catch (Exception e) { fail(e); }
    }
}
