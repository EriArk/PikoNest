package art.pikoos.lab.core;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Portable interaction state. Input devices and Android persistence remain outside. */
public final class WorkshopSession {
    public enum Action { UP, DOWN, LEFT, RIGHT, CONFIRM, CANCEL, PREVIOUS, NEXT, TEST, UNDO, REDO, CONTEXT, MENU,
        SPRITE_SHEET, NEW_SPRITE, COPY_SPRITE, ASSIGN_HERO, DRAW_TOOLS, REGION, ZOOM, ASSETS, CHECK }
    public enum Mode { NAVIGATE, VALUE, CANVAS, PALETTE, SHEET, HELP, MENU, ERROR, DRAW_TOOLS, REGION, HERO, COPY_PLACE, COPY_CONFIRM, ASSETS, ASSET_SAVE, NAME, TRANSFORM, RECOLOR, MOVE, CODE, DIAGNOSTIC, ASSET_CATEGORY, USES }
    public enum DrawTool { BRUSH, ERASER, FILL, LINE, PICKER, RECTANGLE, FILLED_RECTANGLE, OVAL, FILLED_OVAL }
    // Keep existing operation entries 5/6 stable while adding brushes after them.
    public static int moveMenuIndex(){return DrawTool.values().length+2;}
    public static int drawMenuCount(){return moveMenuIndex()+1;}
    public static int drawMenuIndex(DrawTool tool){return tool.ordinal()<5?tool.ordinal():tool.ordinal()+2;}
    public static DrawTool drawMenuTool(int index){
        if(index<0||index>=drawMenuCount())throw new IllegalArgumentException("Tool outside menu");
        return index==5||index==6||index==moveMenuIndex()?null:DrawTool.values()[index<5?index:index-2];
    }
    public interface Port {
        void save(byte[] bytes) throws Exception;
        default void save(byte[] expected,byte[] bytes)throws Exception{save(bytes);}
        void launch(byte[] bytes) throws Exception;
        default void diagnose(byte[] bytes)throws Exception{throw new Exception("Проверка недоступна в этом runtime");}
        default void library() throws Exception {}
        default java.util.List<SpriteAsset> assets()throws Exception{return java.util.Collections.emptyList();}
        default void storeAsset(SpriteAsset asset)throws Exception{throw new Exception("Хранилище ресурсов не подключено");}
        default void renameAsset(SpriteAsset expected,String title)throws Exception{throw new Exception("Переименование не подключено");}
        default void categorizeAsset(SpriteAsset expected,SpriteAsset.Category category)throws Exception{throw new Exception("Категории не подключены");}
        default String projectOrigin(){return "Проект";}
        default void favoriteAsset(SpriteAsset expected,boolean favorite)throws Exception{throw new Exception("Избранное ресурсов не подключено");}
        default java.util.List<ParameterPreset> presets()throws Exception{return java.util.Collections.emptyList();}
        default void storePreset(ParameterPreset preset)throws Exception{throw new Exception("Библиотека наборов не подключена");}
        default void updatePreset(ParameterPreset expected,ParameterPreset next)throws Exception{throw new Exception("Библиотека наборов не подключена");}
    }
    private final Port port;
    private final ArrayDeque<WorkshopCartridge> undo = new ArrayDeque<>();
    private final ArrayDeque<WorkshopCartridge> redo = new ArrayDeque<>();
    private final int[] toolFocus = new int[4];
    public final MapEditor mapEditor=new MapEditor();
    public GameUses uses;
    public void openUses(boolean create){
        if(codeDraft!=null||pendingStroke()||mapEditor.modal()||flagDraft!=null)return;
        try{uses=new GameUses(cart,tool);mode=Mode.USES;
            if(create){if(tool==3)uses.addMap(mapEditor.x,mapEditor.y);else uses.addSprite(selection());}
        }catch(Exception e){fail(e);}
    }
    public void restoreUses(byte[] bytes){uses=GameUses.restore(bytes,cart);tool=uses.returnTool;mode=Mode.USES;}
    private void usesAction(Action action)throws Exception{
        GameUses g=uses;
        if(action==Action.TEST&&(g.screen==GameUses.Screen.PICK||g.animation!=null&&g.animation.picker!=null||g.background!=null&&g.background.picker!=null))
            throw new IllegalArgumentException("Finish choosing the region or position before Test. Confirm the selection or return to the form.");
        // Test an isolated candidate. The saved cart, history and edit context stay intact.
        if(action==Action.TEST&&(g.screen==GameUses.Screen.CAMERA||g.screen==GameUses.Screen.ANIMATION||g.screen==GameUses.Screen.BACKGROUND||g.screen==GameUses.Screen.REVIEW)){
            port.launch(g.proposal().candidate(cart).bytes());return;
        }
        if(g.screen==GameUses.Screen.BACKGROUND){
            BackgroundUse b=g.background;
            if(b.picker!=null){
                if(action==Action.LEFT)b.picker.move(-1,0);if(action==Action.RIGHT)b.picker.move(1,0);
                if(action==Action.UP)b.picker.move(0,-1);if(action==Action.DOWN)b.picker.move(0,1);
                if(action==Action.CONTEXT)b.picker.toggleStep();
                if(action==Action.CONFIRM)b.acceptPick();if(action==Action.CANCEL)b.cancelPick();
            }else if(g.editingField()){
                if(action==Action.LEFT)b.change(-1);if(action==Action.RIGHT)b.change(1);
                if(action==Action.PREVIOUS)b.change(-8);if(action==Action.NEXT)b.change(8);
                if(action==Action.CONFIRM)g.finishField();if(action==Action.CANCEL)g.cancelField();
            }else{
                if(action==Action.UP)b.field=Math.max(0,b.field-1);if(action==Action.DOWN)b.field=Math.min(6,b.field+1);
                if(action==Action.CONFIRM){if(b.field==0)b.chooseRegion();else if(b.field==6)g.review();else{b.playing=false;g.beginField();}}
                if(action==Action.CONTEXT)b.toggle();if(action==Action.UNDO){b.playing=false;b.previewMillis=0;}
                if(action==Action.MENU)g.review();if(action==Action.CANCEL)g.back();
            }return;
        }
        if(g.screen==GameUses.Screen.CAMERA){
            CameraUse camera=g.camera;
            if(g.editingField()){
                if(action==Action.LEFT)camera.change(-1);if(action==Action.RIGHT)camera.change(1);
                if(action==Action.PREVIOUS)camera.change(-8);if(action==Action.NEXT)camera.change(8);
                if(action==Action.CONTEXT)camera.symbol();
                if(action==Action.CONFIRM)g.finishField();if(action==Action.CANCEL)g.cancelField();
            }else{
                if(action==Action.UP)camera.field=Math.max(0,camera.field-1);if(action==Action.DOWN)camera.field=Math.min(camera.rows()-1,camera.field+1);
                if(action==Action.CONFIRM){if(camera.field==camera.rows()-1)g.review();else g.beginField();}
                if(action==Action.MENU)g.review();if(action==Action.CANCEL)g.back();
            }return;
        }
        if(g.screen==GameUses.Screen.ANIMATION){
            SpriteAnimation a=g.animation;
            if(a.picker!=null){
                if(action==Action.LEFT)a.picker.move(-1,0);if(action==Action.RIGHT)a.picker.move(1,0);
                if(action==Action.UP)a.picker.move(0,-1);if(action==Action.DOWN)a.picker.move(0,1);
                if(action==Action.CONTEXT||action==Action.PREVIOUS||action==Action.NEXT)a.picker.toggleStep();
                if(action==Action.CONFIRM)a.acceptPick();if(action==Action.CANCEL)a.cancelPick();
            }else if(g.editingField()){
                if(action==Action.LEFT)a.change(-1);if(action==Action.RIGHT)a.change(1);
                if(action==Action.CONFIRM)g.finishField();if(action==Action.CANCEL)g.cancelField();
            }else{
                if(action==Action.UP)a.field=Math.max(0,a.field-1);if(action==Action.DOWN)a.field=Math.min(8,a.field+1);
                if(action==Action.PREVIOUS)a.select(a.selected-1);if(action==Action.NEXT)a.select(a.selected+1);
                if(action==Action.CONTEXT)a.toggle();if(action==Action.UNDO)a.stop();
                if(action==Action.CONFIRM){if(a.field==8)g.review();else if(a.field==0||a.field==2||a.field==4||a.field==6)g.beginField();else a.choose();}
                if(action==Action.MENU)g.review();if(action==Action.CANCEL)g.back();
            }return;
        }
        if(g.screen==GameUses.Screen.PICK){
            if(action==Action.LEFT)g.picker.move(-1,0);if(action==Action.RIGHT)g.picker.move(1,0);
            if(action==Action.UP)g.picker.move(0,-1);if(action==Action.DOWN)g.picker.move(0,1);
            if(action==Action.CONTEXT)g.picker.toggleStep();
            if(action==Action.CANCEL)g.back();if(action==Action.CONFIRM){if(g.picker.phase==0)g.picker.next();else g.picked();}return;
        }
        if(g.screen==GameUses.Screen.REVIEW){
            if(action==Action.CANCEL)g.back();
            if(action==Action.CONFIRM){int selected=g.resultIndex();save(g.proposal().candidate(cart),true);uses=new GameUses(cart,g.returnTool);uses.move(selected);}
            return;
        }
        if(g.screen==GameUses.Screen.FORM){
            if(action==Action.UP)g.field=Math.max(0,g.field-1);if(action==Action.DOWN)g.field=Math.min(g.values.length-1,g.field+1);
            if(action==Action.LEFT)g.adjust(-1);if(action==Action.RIGHT)g.adjust(1);
            if(action==Action.PREVIOUS)g.adjust(-8);if(action==Action.NEXT)g.adjust(8);
            if(action==Action.CONTEXT)g.pick();if(action==Action.CONFIRM)g.review();if(action==Action.CANCEL)g.back();return;
        }
        if(g.screen==GameUses.Screen.MENU){
            if(action==Action.UP)g.menu=Math.max(0,g.menu-1);if(action==Action.DOWN)g.menu=Math.min(12,g.menu+1);
            if(action==Action.CANCEL||action==Action.MENU){g.screen=GameUses.Screen.LIST;return;}
            if(action==Action.CONFIRM){
                if(g.menu==0)g.addSprite(selection());if(g.menu==1)g.addMap(mapEditor.x,mapEditor.y);
                if(g.menu==2)g.duplicate();if(g.menu==3)g.delete();
                if(g.menu==4){GameUses.Entry e=g.current();int at=e==null?0:e.start();uses=null;tool=1;beginCode();codeDraft.point(g.source.substring(0,at).split("\r\n|\r|\n",-1).length-1,0);}
                if(g.menu==5)g.screen=GameUses.Screen.LIST;
                if(g.menu==6)g.addAnimation(selection());if(g.menu==7)g.animateSelected();
                if(g.menu==8)g.addCamera(false);if(g.menu==9)g.addCamera(true);
                if(g.menu==10)g.addBackground(selection());if(g.menu==11)g.reorderLayer(-1);if(g.menu==12)g.reorderLayer(1);
            }return;
        }
        if(action==Action.UP)g.move(-1);if(action==Action.DOWN)g.move(1);
        if(action==Action.CONFIRM){if(g.current()==null){if(tool==3)g.addMap(mapEditor.x,mapEditor.y);else g.addSprite(selection());}else g.edit();}
        if(action==Action.NEXT){if(tool==3)g.addMap(mapEditor.x,mapEditor.y);else g.addSprite(selection());}
        if(action==Action.MENU){g.screen=GameUses.Screen.MENU;g.menu=0;}
        if(action==Action.TEST)port.launch(cart.bytes());
        if(action==Action.CANCEL){uses=null;mode=tool==2&&browsingSprites?Mode.SHEET:Mode.NAVIGATE;}
        if(action==Action.UNDO||action==Action.REDO){
            mode=Mode.NAVIGATE;act(action);
            if(mode==Mode.ERROR){overlayReturn=Mode.USES;return;}
            uses=new GameUses(cart,g.returnTool);uses.move(g.index);mode=Mode.USES;
        }
    }
    public FlagDraft flagDraft;
    private WorkshopCartridge cart;
    public LuaDraft codeDraft;
    public PresetPanel presets;
    public void restorePresets(String encoded)throws Exception{if(!encoded.isEmpty()&&codeDraft!=null)presets=PresetPanel.restore(encoded,codeDraft.insertion,port);}
    public ToolCatalogue toolCatalogue=new ToolCatalogue();
    public int codeColumn;
    public RuntimeDiagnostic diagnostic;
    public boolean diagnosticPending,diagnosticDetails,diagnosticStale;
    public int diagnosticScroll;
    private byte[] diagnosticSource;
    public void diagnosticResult(byte[] checked,String log,boolean cancelled,boolean completed,boolean ended){
        if(codeDraft==null)return;
        diagnosticPending=false;diagnosticSource=checked.clone();diagnosticScroll=0;diagnosticDetails=false;
        String source=new LuaDraft(new WorkshopCartridge(checked),0).text();
        try{diagnosticStale=!Arrays.equals(checked,codeDraft.edit().candidate(cart).bytes());}catch(Exception e){diagnosticStale=true;}
        diagnostic=RuntimeDiagnostic.read(log,cancelled,completed,ended,source);mode=Mode.DIAGNOSTIC;
    }
    private void diagnosticAction(Action action)throws Exception{
        if(diagnosticPending)return;
        if(action==Action.CANCEL){mode=Mode.CODE;return;}
        if(action==Action.MENU&&diagnostic!=null){diagnosticDetails=!diagnosticDetails;diagnosticScroll=0;return;}
        if(diagnosticDetails){
            if(action==Action.UP)diagnosticScroll=Math.max(0,diagnosticScroll-1);
            if(action==Action.DOWN)diagnosticScroll=Math.min(Math.max(0,diagnostic.log.split("\n",-1).length-1),diagnosticScroll+1);
            return;
        }
        if((action==Action.CONFIRM&&diagnostic==null)||action==Action.CONTEXT){
            diagnosticSource=codeDraft.edit().candidate(cart).bytes();
            if(PicoIncludes.needed(diagnosticSource))throw new Exception("Проверка #include пока недоступна. Исходник сохранён.");
            diagnostic=null;diagnosticPending=true;diagnosticStale=false;
            try{port.diagnose(diagnosticSource.clone());}catch(Exception e){diagnosticPending=false;throw e;}
        }else if(action==Action.CONFIRM&&diagnostic!=null&&diagnostic.line>=0&&!diagnosticStale){
            if(!Arrays.equals(diagnosticSource,codeDraft.edit().candidate(cart).bytes())){diagnosticStale=true;return;}
            codeDraft.beginNavigation();codeDraft.navigation.tab=1;codeDraft.navigation.target=diagnostic.line+1;codeDraft.jump();mode=Mode.CODE;
        }
    }
    public void restoreCode(byte[] bytes){codeDraft=LuaDraft.restore(bytes);tool=1;mode=Mode.CODE;}
    private void beginCode(){codeDraft=new LuaDraft(cart,codeLine);codeDraft.point(codeLine,codeColumn);mode=Mode.CODE;}
    private void finishCode(boolean test)throws Exception{
        WorkshopCartridge candidate=codeDraft.edit().candidate(cart);
        save(candidate,true);codeLine=codeDraft.line();codeColumn=codeDraft.column();codeDraft=null;mode=Mode.NAVIGATE;
        if(test)port.launch(cart.bytes());
    }
    private void leaveCode(){
        if(codeDraft.dirty()){codeDraft.panel=LuaDraft.Panel.EXIT;codeDraft.menu=0;}
        else{codeLine=codeDraft.line();codeColumn=codeDraft.column();codeDraft=null;mode=Mode.NAVIGATE;}
    }
    public void codeCommand(int command){
        if(presets!=null)return;
        if(mode!=Mode.CODE||codeDraft==null)return;
        if(codeDraft.panel==LuaDraft.Panel.LAYERS)return;
        if(codeDraft.proposal()||codeDraft.panel==LuaDraft.Panel.NAVIGATION||codeDraft.panel==LuaDraft.Panel.SPRITE||codeDraft.panel==LuaDraft.Panel.ANIMATION||codeDraft.panel==LuaDraft.Panel.BRANCHES||codeDraft.panel==LuaDraft.Panel.ACTIONS)return;
        try{
            LuaDraft d=codeDraft;
            switch(command){
                case 0:finishCode(false);return;
                case 1:finishCode(true);return;
                case 2:d.insertNewline();break;
                case 3:d.replace(" ");break;
                case 4:d.replace("\t");break;
                case 5:d.erase(false);break;
                case 6:d.erase(true);break;
                case 7:d.select();break;
                case 8:d.selectAll();break;
                case 9:d.copy();break;
                case 10:d.cut();break;
                case 11:d.paste();break;
                case 12:d.history(false);break;
                case 13:d.history(true);break;
                case 14:d.home();break;
                case 15:d.end();break;
                case 16:leaveCode();return;
                case 17:d.changePage(1);d.panel=LuaDraft.Panel.KEYS;return;
                case 18:d.beginInsert();return;
                case 19:d.beginParameters();return;
                case 20:d.beginNavigation();return;
                case 21:d.goBack();break;
                case 22:d.panel=LuaDraft.Panel.CURSOR;diagnostic=null;diagnosticPending=false;diagnosticDetails=false;mode=Mode.DIAGNOSTIC;return;
                case 23:d.beginBranches();return;
                case 24:d.beginLayers();return;
                default:return;
            }
            d.panel=LuaDraft.Panel.CURSOR;
        }catch(Exception e){fail(e);}
    }
    public void codeText(String value){if(presets==null&&mode==Mode.CODE&&codeDraft!=null&&(codeDraft.panel==LuaDraft.Panel.CURSOR||codeDraft.panel==LuaDraft.Panel.KEYS))try{codeDraft.replace(value);}catch(Exception e){fail(e);}}
    public void insertText(String value){if(mode==Mode.CODE&&codeDraft!=null&&codeDraft.insertion!=null&&codeDraft.insertion.screen==LuaInsert.Screen.TEXT)try{codeDraft.insertion.type(value);}catch(Exception e){fail(e);}}
    private void insertAction(Action action)throws Exception{
        LuaDraft d=codeDraft;LuaInsert i=d.insertion;
        if(i.screen==LuaInsert.Screen.PREVIEW){
            if(action==Action.UP)i.previewLine=Math.max(0,i.previewLine-1);
            if(action==Action.DOWN)i.previewLine=Math.min(i.code().length(),i.previewLine+1);
            if(action==Action.LEFT||action==Action.PREVIOUS)i.previewLine=Math.max(0,i.previewLine-6);
            if(action==Action.RIGHT||action==Action.NEXT)i.previewLine=Math.min(i.code().length(),i.previewLine+6);
            if(action==Action.CANCEL)i.screen=LuaInsert.Screen.FIELDS;
            if(action==Action.CONFIRM)d.applyInsert();
            return;
        }
        if(i.screen==LuaInsert.Screen.SYMBOLS){
            if(action==Action.UP)i.symbolMove(-1);if(action==Action.DOWN)i.symbolMove(1);
            if(action==Action.LEFT)i.symbolMove(-6);if(action==Action.RIGHT)i.symbolMove(6);
            if(action==Action.PREVIOUS||action==Action.NEXT||action==Action.MENU)i.symbolTab();
            if(action==Action.CONFIRM)i.acceptSymbol();if(action==Action.CANCEL)i.screen=LuaInsert.Screen.FIELDS;
            return;
        }
        if(i.screen==LuaInsert.Screen.TEXT){
            if(action==Action.UP)i.moveKey(0,-1);if(action==Action.DOWN)i.moveKey(0,1);
            if(action==Action.LEFT)i.moveKey(-1,0);if(action==Action.RIGHT)i.moveKey(1,0);
            if(action==Action.PREVIOUS)i.changePage(-1);if(action==Action.NEXT||action==Action.MENU)i.changePage(1);
            if(action==Action.CONFIRM)i.type(LuaDraft.PAGES[i.page].substring(i.key,i.key+1));
            if(action==Action.CONTEXT)i.erase();if(action==Action.UNDO)i.replaceAll=!i.replaceAll;
            if(action==Action.TEST)i.acceptText();
            if(action==Action.CANCEL)i.screen=LuaInsert.Screen.FIELDS;
            return;
        }
        if(i.screen==LuaInsert.Screen.CATALOG){
            toolCatalogue.normalize(i);
            if(action==Action.LEFT||action==Action.PREVIOUS)toolCatalogue.change(-1,i);
            if(action==Action.RIGHT||action==Action.NEXT)toolCatalogue.change(1,i);
            if(action==Action.UP)toolCatalogue.move(-1,i);
            if(action==Action.DOWN)toolCatalogue.move(1,i);
            if(action==Action.UNDO&&!toolCatalogue.items().isEmpty()){toolCatalogue.toggle(i.selected);toolCatalogue.normalize(i);}
            if(toolCatalogue.items().isEmpty()){if(action==Action.CANCEL)d.cancelInsert();return;}
            if(action==Action.CONFIRM){
                if(i.item().id.equals("branches"))d.beginBranches();
                else if(i.item().id.equals("background_layers"))d.beginLayers();
                else if(d.branchInsertion()&&(i.item().id.equals("sspr")||i.item().id.equals("animation")))throw new IllegalArgumentException("Для визуального размещения сначала открой код ветви. Здесь доступна форма spr и другие действия.");
                else if(i.item().id.equals("sspr"))d.beginSprite(selection());else if(i.item().id.equals("animation"))d.beginAnimation(selection());else {if(i.backgroundRecipe())BackgroundLayer.setRegion(i,selection());i.screen=LuaInsert.Screen.FIELDS;}
            }
            if(action==Action.CANCEL)d.cancelInsert();
            return;
        }
        if(i.backgroundRecipe()&&(action==Action.UP||action==Action.DOWN)){
            int at=0;while(at<BackgroundLayer.FORM_FIELDS.length-1&&BackgroundLayer.FORM_FIELDS[at]<i.field)at++;
            i.field=BackgroundLayer.FORM_FIELDS[Math.max(0,Math.min(5,at+(action==Action.UP?-1:1)))];return;
        }
        if(i.backgroundRecipe()&&action==Action.CONFIRM&&i.field<4){d.beginBackgroundPick();return;}
        if(action==Action.UP)i.field=Math.max(0,i.field-1);
        if(action==Action.MENU){presets=new PresetPanel(i,port);return;}
        if(action==Action.DOWN)i.field=Math.min(i.item().fields.length,i.field+1);
        if(action==Action.LEFT)i.step(-1);if(action==Action.RIGHT)i.step(1);
        if(action==Action.UNDO&&i.cameraMode())i.switchCameraMode();
        if(action==Action.CONTEXT){if(i.fullPreview()&&!i.canBrowse())i.beginPreview();else i.beginSymbols(d.text());}
        if(action==Action.CONFIRM){if(i.field==i.item().fields.length){if(i.fullPreview())i.beginPreview();else d.applyInsert();}else i.beginText();}
        if(action==Action.CANCEL){if(d.callEdit!=null||d.layerForm())d.cancelInsert();else i.screen=LuaInsert.Screen.CATALOG;}
        // Start is intentionally not a launch/commit shortcut while reviewing a proposal.
    }
    private void codeAction(Action action)throws Exception{
        if(presets!=null){presets.act(action);if(presets.closed)presets=null;return;}
        LuaDraft d=codeDraft;
        if(d.panel==LuaDraft.Panel.LAYERS){
            if(d.layerChange!=null){
                if(action==Action.CONTEXT)d.layerBefore=!d.layerBefore;
                if(action==Action.CONFIRM)d.applyLayer();
                if(action==Action.CANCEL){d.layerChange=null;d.layerBefore=false;}
                return;
            }
            if(d.layerMenu>=0){
                if(action==Action.UP)d.layerMenu=Math.max(0,d.layerMenu-1);if(action==Action.DOWN)d.layerMenu=Math.min(4,d.layerMenu+1);
                if(action==Action.CONFIRM){if(d.layerMenu==4)d.newLayer(selection());else d.reviewLayer();}if(action==Action.CANCEL)d.layerMenu=-1;
                return;
            }
            if(action==Action.UP)d.layers.move(-1);if(action==Action.DOWN)d.layers.move(1);
            if(action==Action.LEFT)d.layers.move(-4);if(action==Action.RIGHT)d.layers.move(4);
            if(action==Action.CONFIRM)d.openLayer(false);if(action==Action.CONTEXT)d.openLayer(true);
            if(action==Action.MENU&&d.layers.current()!=null)d.layerMenu=0;
            if(action==Action.NEXT)d.newLayer(selection());
            if(action==Action.UNDO)d.layerHistory(false);if(action==Action.REDO)d.layerHistory(true);
            if(action==Action.CANCEL)d.cancelLayers();
            return;
        }
        if(d.backgroundPicker!=null){
            SpritePlacement pick=d.backgroundPicker;
            if(action==Action.LEFT)pick.move(-1,0);if(action==Action.RIGHT)pick.move(1,0);if(action==Action.UP)pick.move(0,-1);if(action==Action.DOWN)pick.move(0,1);
            if(action==Action.CONTEXT||action==Action.PREVIOUS||action==Action.NEXT)pick.toggleStep();
            if(action==Action.CONFIRM)d.acceptBackgroundPick();if(action==Action.CANCEL)d.cancelBackgroundPick();return;
        }
        if(d.panel==LuaDraft.Panel.ANIMATION){
            SpriteAnimation a=d.animation;
            if(a.picker!=null){
                if(action==Action.LEFT)a.picker.move(-1,0);if(action==Action.RIGHT)a.picker.move(1,0);
                if(action==Action.UP)a.picker.move(0,-1);if(action==Action.DOWN)a.picker.move(0,1);
                if(action==Action.CONTEXT||action==Action.PREVIOUS||action==Action.NEXT)a.picker.toggleStep();
                if(action==Action.CONFIRM)a.acceptPick();if(action==Action.CANCEL)a.cancelPick();
            }else if(a.review){
                if(action==Action.UP)a.previewLine=Math.max(0,a.previewLine-1);if(action==Action.DOWN)a.previewLine=Math.min(a.code().length(),a.previewLine+1);
                if(action==Action.LEFT||action==Action.PREVIOUS)a.previewLine=Math.max(0,a.previewLine-6);
                if(action==Action.RIGHT||action==Action.NEXT)a.previewLine=Math.min(a.code().length(),a.previewLine+6);
                if(action==Action.CONTEXT&&d.animationEdit!=null){d.animationBefore=!d.animationBefore;a.previewLine=0;}
                if(action==Action.CANCEL){a.review=false;d.animationBefore=false;}
                if(action==Action.CONFIRM){if(d.animationBefore){d.animationBefore=false;a.previewLine=0;}else d.applyAnimation();}
            }else{
                if(action==Action.UP)a.field=Math.max(0,a.field-1);if(action==Action.DOWN)a.field=Math.min(8,a.field+1);
                if(action==Action.LEFT)a.change(-1);if(action==Action.RIGHT)a.change(1);
                if(action==Action.PREVIOUS)a.select(a.selected-1);if(action==Action.NEXT)a.select(a.selected+1);
                if(action==Action.CONTEXT)a.toggle();if(action==Action.UNDO)a.stop();
                if(action==Action.CONFIRM)a.choose();if(action==Action.CANCEL)d.cancelAnimation();
            }
            return;
        }
        if(d.panel==LuaDraft.Panel.SPRITE){
            SpritePlacement p=d.placement;
            if(action==Action.LEFT)p.move(-1,0);if(action==Action.RIGHT)p.move(1,0);
            if(action==Action.UP)p.move(0,-1);if(action==Action.DOWN)p.move(0,1);
            if(action==Action.PREVIOUS||action==Action.NEXT||action==Action.CONTEXT)p.toggleStep();
            if(action==Action.CONFIRM){if(p.phase==3)d.applySprite();else p.next();}
            if(action==Action.CANCEL&&p.back())d.cancelSprite();
            return;
        }
        if(d.proposal()){insertAction(action);return;}
        if(d.panel==LuaDraft.Panel.BRANCHES){
            if(action==Action.UP)d.branches.move(-1);if(action==Action.DOWN)d.branches.move(1);
            if(action==Action.LEFT)d.branches.move(-5);if(action==Action.RIGHT)d.branches.move(5);
            if(action==Action.CONFIRM)d.beginActions();
            if(action==Action.CONTEXT)d.branchAction(true);
            if(action==Action.CANCEL)d.cancelBranches();
            return;
        }
        if(d.panel==LuaDraft.Panel.ACTIONS){
            if(d.actionChange!=null){
                if(action==Action.UP)d.actionScroll=Math.max(0,d.actionScroll-1);
                if(action==Action.DOWN)d.actionScroll=Math.min(d.actionChange.before.split("\\r\\n|\\r|\\n").length-1,d.actionScroll+1);
                if(action==Action.LEFT)d.actionColumn=Math.max(0,d.actionColumn-8);
                if(action==Action.RIGHT)d.actionColumn=Math.min(Math.max(d.actionChange.before.length(),d.actionChange.after.length()),d.actionColumn+8);
                if(action==Action.CONTEXT){d.actionBefore=!d.actionBefore;d.actionScroll=d.actionColumn=0;}
                if(action==Action.CONFIRM)d.applyAction();
                if(action==Action.CANCEL){d.actionChange=null;d.actionBefore=false;d.actionScroll=d.actionColumn=0;}
                return;
            }
            if(d.actionMenu>=0){
                if(action==Action.UP)d.actionMenu=Math.max(0,d.actionMenu-1);
                if(action==Action.DOWN)d.actionMenu=Math.min(2,d.actionMenu+1);
                if(action==Action.CONFIRM)d.reviewAction();if(action==Action.CANCEL)d.actionMenu=-1;
                return;
            }
            if(action==Action.UP)d.actions.move(-1);if(action==Action.DOWN)d.actions.move(1);
            if(action==Action.LEFT)d.actions.move(-5);if(action==Action.RIGHT)d.actions.move(5);
            if(action==Action.CONFIRM)d.openAction(false);if(action==Action.CONTEXT)d.openAction(true);
            if(action==Action.CANCEL)d.actionsToBranches();
            if(action==Action.MENU)d.actionOptions();
            return;
        }
        if(d.panel==LuaDraft.Panel.NAVIGATION){
            LuaNavigation n=d.navigation;
            if(action==Action.UP)n.move(0,-1);if(action==Action.DOWN)n.move(0,1);
            if(action==Action.LEFT)n.move(-1,0);if(action==Action.RIGHT)n.move(1,0);
            if(action==Action.PREVIOUS||action==Action.NEXT||action==Action.MENU)n.tab=1-n.tab;
            if(action==Action.CONFIRM)d.jump();if(action==Action.CANCEL)d.cancelNavigation();
            if(action==Action.CONTEXT)d.goBack();
            return;
        }
        if(d.panel==LuaDraft.Panel.EXIT){
            if(action==Action.UP)d.menu=Math.max(0,d.menu-1);
            if(action==Action.DOWN)d.menu=Math.min(2,d.menu+1);
            if(action==Action.CANCEL)d.panel=LuaDraft.Panel.CURSOR;
            if(action==Action.CONFIRM){
                if(d.menu==0)d.panel=LuaDraft.Panel.CURSOR;
                if(d.menu==1)finishCode(false);
                if(d.menu==2){codeLine=d.line();codeColumn=d.column();codeDraft=null;mode=Mode.NAVIGATE;notice="Черновик отменён";}
            }
            return;
        }
        if(action==Action.CHECK&&d.panel==LuaDraft.Panel.CURSOR){codeCommand(22);return;}
        if(action==Action.TEST){finishCode(true);return;}
        if(action==Action.UNDO){d.history(false);return;}
        if(action==Action.REDO){d.history(true);return;}
        if(action==Action.CONTEXT&&d.panel==LuaDraft.Panel.CURSOR){d.beginInsert();return;}
        if(action==Action.NEXT&&d.panel==LuaDraft.Panel.CURSOR){d.beginNavigation();return;}
        if(action==Action.PREVIOUS&&d.panel==LuaDraft.Panel.CURSOR){d.beginParameters();return;}
        if(action==Action.MENU||action==Action.CONTEXT){
            d.panel=d.panel==LuaDraft.Panel.MENU?LuaDraft.Panel.CURSOR:LuaDraft.Panel.MENU;d.menu=0;return;
        }
        if(d.panel==LuaDraft.Panel.MENU){
            if(action==Action.UP)d.menu=Math.max(0,d.menu-1);
            if(action==Action.DOWN)d.menu=Math.min(LuaDraft.COMMANDS.length-1,d.menu+1);
            if(action==Action.CONFIRM)codeCommand(d.menu);
            if(action==Action.CANCEL)d.panel=LuaDraft.Panel.CURSOR;
            return;
        }
        if(d.panel==LuaDraft.Panel.KEYS){
            if(action==Action.UP)d.moveKey(0,-1);if(action==Action.DOWN)d.moveKey(0,1);
            if(action==Action.LEFT)d.moveKey(-1,0);if(action==Action.RIGHT)d.moveKey(1,0);
            if(action==Action.PREVIOUS)d.changePage(-1);if(action==Action.NEXT)d.changePage(1);
            if(action==Action.CONFIRM)d.typeKey();
            if(action==Action.CANCEL)d.panel=LuaDraft.Panel.CURSOR;
            return;
        }
        if(action==Action.UP)d.move(0,-1);if(action==Action.DOWN)d.move(0,1);
        if(action==Action.LEFT)d.move(-1,0);if(action==Action.RIGHT)d.move(1,0);
        if(action==Action.CONFIRM)d.panel=LuaDraft.Panel.KEYS;
        if(action==Action.CANCEL)leaveCode();
    }
    public SpriteMove move;
    private Mode moveReturn=Mode.CANVAS;
    public String moveReturnMode(){return moveReturn.name();}
    public void restoreMove(String encoded,String origin){
        if(tool!=2||browsingSprites||pendingStroke()||selection().sharesMap()||(mode!=Mode.CANVAS&&mode!=Mode.NAVIGATE))return;
        try{
            Mode parsed=Mode.valueOf(origin);if(parsed!=Mode.CANVAS&&parsed!=Mode.NAVIGATE)return;
            SpriteMove restored=SpriteMove.restore(selection(),encoded);move=restored;moveReturn=parsed;mode=Mode.MOVE;
        }catch(IllegalArgumentException e){/* Invalid optional UI preferences never mutate a cart. */}
    }
    public Mode mode = Mode.NAVIGATE;
    public int tool, focus, codeLine, cursorX = 7, cursorY = 7, color = 14, paletteCursor = 14;
    public int field, draft, menuItem;
    public boolean menuRedo;
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
        if(tool!=2||browsingSprites||pendingStroke()||selection().sharesMap()
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
        if(tool!=2||browsingSprites||pendingStroke()||selection().sharesMap()
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
    public int assetFilter,assetCategoryChoice;
    private boolean categoryForDraft;
    public static int favoriteAssetFilter(){return SpriteAsset.Category.values().length+1;}
    public String assetFilterTitle(){return assetFilter==0?"Все":assetFilter==favoriteAssetFilter()?"Избранное":SpriteAsset.Category.values()[assetFilter-1].title;}
    public java.util.List<SpriteAsset> assets(){
        java.util.List<SpriteAsset> result=new java.util.ArrayList<>();
        for(SpriteAsset a:assets)if(assetFilter==0||assetFilter==favoriteAssetFilter()&&a.favorite||a.category.ordinal()==assetFilter-1)result.add(a);
        return java.util.Collections.unmodifiableList(result);
    }
    public SpriteAsset currentAsset(){java.util.List<SpriteAsset> list=assets();return list.isEmpty()?null:list.get(clamp(assetIndex,list.size()-1));}
    private void beginCategory(){
        categoryForDraft=mode==Mode.ASSET_SAVE;SpriteAsset a=categoryForDraft?assetDraft:currentAsset();
        if(a==null)return;assetCategoryChoice=a.category.ordinal();mode=Mode.ASSET_CATEGORY;
    }
    public void restoreCategory(int choice){if((mode==Mode.ASSETS||mode==Mode.ASSET_SAVE)&&choice>=0&&choice<SpriteAsset.Category.values().length){beginCategory();assetCategoryChoice=choice;}}
    private void finishCategory()throws Exception{
        SpriteAsset a=categoryForDraft?assetDraft:currentAsset();SpriteAsset.Category category=SpriteAsset.Category.values()[assetCategoryChoice];
        if(categoryForDraft)assetDraft=a.withCategory(category);
        else if(a.category!=category){port.categorizeAsset(a,category);publishAsset(a.withCategory(category));}
        mode=categoryForDraft?Mode.ASSET_SAVE:Mode.ASSETS;notice="Категория сохранена";
    }
    public String assetsReturnMode(){return assetsReturn.name();}
    private void showAssets()throws Exception{
        String selected=currentAsset()==null?"":currentAsset().id;
        assetsReturn=mode;mode=Mode.ASSETS;
        java.util.List<SpriteAsset> loaded=port.assets();assets.clear();assets.addAll(loaded);assetIndex=clamp(assetIndex,assets.size()-1);
        selectAssetId(selected);
    }
    public void selectAssetId(String id){java.util.List<SpriteAsset> list=assets();for(int i=0;i<list.size();i++)if(list.get(i).id.equals(id)){assetIndex=i;return;}assetIndex=clamp(assetIndex,list.size()-1);}
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
    public void typeName(int index){if(mode==Mode.CODE&&presets!=null&&presets.page==PresetPanel.Page.NAME)presets.name.type(index);else if(mode==Mode.NAME)nameEditor.type(index);}
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
    public void selectAsset(int index){if(mode!=Mode.ASSETS)return;assetIndex=clamp(index,assets().size()-1);act(Action.CONFIRM);}
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
        if(tool!=2||pendingStroke()||cart.empty(selection())||selection().sharesMap())return;
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
    public boolean canRedo() { return !redo.isEmpty(); }
    public int undoCount() { return undo.size(); }
    public int redoCount() { return redo.size(); }
    public int maxFocus(){return tool==2?(cart.hasHero()?7:6):cart.hasHero()?4:2;}
    public boolean ovalTool(){return drawTool==DrawTool.OVAL||drawTool==DrawTool.FILLED_OVAL;}
    public boolean twoPointTool(){return drawTool==DrawTool.LINE||drawTool==DrawTool.RECTANGLE||drawTool==DrawTool.FILLED_RECTANGLE||ovalTool();}
    public boolean pendingStroke(){return twoPointTool()&&lineX>=0&&lineY>=0;}
    public boolean pendingLine(){return drawTool==DrawTool.LINE&&pendingStroke();}
    /** The legacy lineX/lineY preference keys hold the first point for all two-point tools. */
    public void restoreStroke(int x,int y){
        SpriteRegion r=selection();
        if(tool==2&&mode==Mode.CANVAS&&!browsingSprites&&twoPointTool()&&!r.sharesMap()
            &&x>=0&&y>=0&&x<r.width&&y<r.height){lineX=x;lineY=y;}
    }
    public SpriteRegion selection(){return region==null?new SpriteRegion(spriteSlot*16,0,16,16):region;}
    public WorkshopCartridge canvasPreview(){
        if(!pendingStroke())return cart;
        return drawTool==DrawTool.LINE?cart.withLine(selection(),lineX,lineY,cursorX,cursorY,color)
            :ovalTool()?cart.withOval(selection(),lineX,lineY,cursorX,cursorY,color,drawTool==DrawTool.FILLED_OVAL)
            :cart.withRectangle(selection(),lineX,lineY,cursorX,cursorY,color,drawTool==DrawTool.FILLED_RECTANGLE);
    }
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
        CartEdit operation=new CartEdit(cart,next);
        next=operation.candidate(cart);
        port.save(cart.bytes(),next.bytes()); // Publish state only after durable storage succeeds.
        if (remember) {
            if (undo.size() == 32) undo.removeLast();
            undo.push(cart);
            redo.clear();
        }
        cart = next;
        notice = "Сохранено";
    }
    public void fail(Exception e) {
        error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        overlayReturn = mode==Mode.USES||mode==Mode.CODE||mode==Mode.MOVE||mode==Mode.RECOLOR||mode==Mode.TRANSFORM||mode==Mode.NAME||mode==Mode.COPY_CONFIRM||mode==Mode.ASSETS||mode==Mode.ASSET_SAVE||mode==Mode.ASSET_CATEGORY?mode:mode==Mode.HERO?Mode.HERO:pendingStroke()?Mode.CANVAS:tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE; mode = Mode.ERROR;
    }
    public void switchTool(int next) {
        if(pendingStroke())return;
        if (mode != Mode.NAVIGATE && mode != Mode.CANVAS && mode != Mode.SHEET) return;
        if(mapEditor.modal()||flagDraft!=null)return;
        if (tool == Math.floorMod(next,4)) return;
        toolFocus[tool] = focus;
        tool = Math.floorMod(next,4); focus = toolFocus[tool]; mode = tool == 2 && browsingSprites ? Mode.SHEET : Mode.NAVIGATE;
    }
    public void select(int target) {
        if (mode != Mode.NAVIGATE) return;
        focus = clamp(target, maxFocus()); act(Action.CONFIRM);
    }
    public void openSprite(int slot) {
        if(pendingStroke()||transforming()||recoloring()||move!=null)return;
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
        if(pendingStroke()||transforming()||recoloring()||move!=null)return;
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
        if(mode!=Mode.DRAW_TOOLS||value<0||value>=drawMenuCount())return;
        drawToolCursor=value;act(Action.CONFIRM);
    }
    private void draw()throws Exception {
        switch(drawTool){
            case BRUSH:save(cart.withPixel(selection(),cursorX,cursorY,color),true);break;
            case ERASER:save(cart.withPixel(selection(),cursorX,cursorY,0),true);break;
            case FILL:save(cart.withFill(selection(),cursorX,cursorY,color),true);break;
            case LINE:lineX=cursorX;lineY=cursorY;notice="Выбери конец линии";break;
            case RECTANGLE:case FILLED_RECTANGLE:lineX=cursorX;lineY=cursorY;notice="Выбери противоположный угол";break;
            case OVAL:case FILLED_OVAL:lineX=cursorX;lineY=cursorY;notice="Выбери второй угол рамки овала";break;
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
            if(mode==Mode.USES){usesAction(action);return;}
            if(mode==Mode.DIAGNOSTIC){diagnosticAction(action);return;}
            if(mode==Mode.CODE){codeAction(action);return;}
            if(mode==Mode.MOVE){
                if(action==Action.UP)move.step(0,-1);if(action==Action.DOWN)move.step(0,1);
                if(action==Action.LEFT)move.step(-1,0);if(action==Action.RIGHT)move.step(1,0);
                if(action==Action.UNDO||(action==Action.CANCEL&&!move.back())){move=null;mode=moveReturn;notice="Перенос отменён";return;}
                if(action==Action.CONFIRM){
                    if(move.phase<2)move.next();
                    else{save(move.preview(cart),true);move=null;mode=moveReturn;notice="Перенос завершён · Y отмена";}
                }
                return;
            }
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
                if(action==Action.UNDO){beginCategory();return;}
                if(action==Action.CONTEXT)beginName();
                if(action==Action.CANCEL){assetDraft=null;mode=Mode.ASSETS;notice="Сохранение ресурса отменено";}
                if(action==Action.CONFIRM){
                    port.storeAsset(assetDraft);
                    assetFilter=assetDraft.category.ordinal()+1;publishAsset(assetDraft);assetDraft=null;mode=Mode.ASSETS;notice="Спрайт в библиотеке";
                }
                return;
            }
            if(mode==Mode.ASSETS){
                if(action==Action.PREVIOUS||action==Action.NEXT){assetFilter=Math.floorMod(assetFilter+(action==Action.NEXT?1:-1),favoriteAssetFilter()+1);assetIndex=0;return;}
                if(action==Action.MENU){beginCategory();return;}
                if(action==Action.UNDO){SpriteAsset a=currentAsset();if(a!=null){port.favoriteAsset(a,!a.favorite);publishAsset(a.withFavorite(!a.favorite));notice=a.favorite?"Убрано из избранного":"Спрайт в избранном";}return;}
                if(action==Action.CANCEL){mode=assetsReturn;return;}
                if(action==Action.LEFT||action==Action.UP)assetIndex=clamp(assetIndex-1,assets().size()-1);
                if(action==Action.RIGHT||action==Action.DOWN)assetIndex=clamp(assetIndex+1,assets().size()-1);
                if(action==Action.CONFIRM){
                    if(currentAsset()!=null)beginInsertion(currentAsset());
                    else if(assetFilter!=0){notice="Категория пуста · L/R другие категории";}
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
            if(pendingStroke()){
                moveCursor(action);
                if(action==Action.CANCEL||action==Action.UNDO){lineX=lineY=-1;notice=drawTool==DrawTool.LINE?"Линия отменена":ovalTool()?"Овал отменён":"Прямоугольник отменён";}
                if(action==Action.CONFIRM||action==Action.TEST){
                    save(canvasPreview(),true);lineX=lineY=-1;
                    if(action==Action.TEST)port.launch(cart.bytes());
                }
                return; // A draft cannot leak into other resources, tabs or runtime snapshots.
            }
            if(mode==Mode.DRAW_TOOLS){
                if(action==Action.UP)drawToolCursor=clamp(drawToolCursor-1,drawMenuCount()-1);
                if(action==Action.DOWN)drawToolCursor=clamp(drawToolCursor+1,drawMenuCount()-1);
                if(action==Action.CONFIRM){
                    if(drawToolCursor==moveMenuIndex()){
                        if(selection().sharesMap()){notice="Редактор общей области с картой ещё не готов";return;}
                        moveReturn=overlayReturn;move=new SpriteMove(selection(),cursorX,cursorY);mode=Mode.MOVE;return;
                    }
                    if(drawToolCursor==6){
                        Mode origin=overlayReturn;mode=origin;
                        restoreRecolor(cart.pixel(selection(),cursorX,cursorY),color,1,origin.name());return;
                    }
                    if(drawToolCursor==5){
                        Mode origin=overlayReturn;mode=origin;
                        restoreTransform(SpriteTransform.FLIP_HORIZONTAL.name(),origin.name());return;
                    }
                    DrawTool chosen=drawMenuTool(drawToolCursor);
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
                if (action == Action.UNDO || action == Action.REDO) { mode = overlayReturn; act(action); return; }
                if (action == Action.UP) menuItem = clamp(menuItem - 1, 7);
                if (action == Action.DOWN) menuItem = clamp(menuItem + 1, 7);
                if (menuItem == 0 && action == Action.LEFT) menuRedo = false;
                if (menuItem == 0 && action == Action.RIGHT) menuRedo = true;
                if (action == Action.CANCEL || action == Action.MENU) mode = overlayReturn;
                if (action == Action.CONFIRM) {
                    if (menuItem == 0) { mode = overlayReturn; act(menuRedo ? Action.REDO : Action.UNDO); }
                    if (menuItem == 1) { swapAB = !swapAB; notice = "Кнопки изменены"; }
                    if (menuItem == 2) {if(tool==3){mode=overlayReturn;mapEditor.tools();}else mode = Mode.HELP;}
                    if (menuItem == 3) mode = overlayReturn;
                    if (menuItem == 4) { mode = overlayReturn; port.library(); }
                    if (menuItem == 5) { mode = overlayReturn; showAssets(); }
                    if (menuItem == 6 || menuItem == 7) { mode = overlayReturn; openUses(menuItem == 7); }
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
            if(flagDraft!=null){
                if(action==Action.UP)flagDraft.move(0,-1);
                if(action==Action.DOWN)flagDraft.move(0,1);
                if(action==Action.LEFT)flagDraft.move(-1,0);
                if(action==Action.RIGHT)flagDraft.move(1,0);
                if(action==Action.CONFIRM){
                    if(flagDraft.focus<8)flagDraft.toggle();
                    else{boolean differs=flagDraft.value!=flagDraft.original;save(flagDraft.candidate(cart),true);flagDraft=null;notice=differs?"Флаги сохранены · Y отмена":"Флаги не изменились";}
                }
                if(action==Action.CANCEL||action==Action.UNDO){flagDraft=null;notice="Без изменений";}
                return;
            }
            if(mode==Mode.ASSET_CATEGORY){
                if(action==Action.UNDO){mode=categoryForDraft?Mode.ASSET_SAVE:Mode.ASSETS;beginName();return;}
                if(action==Action.UP||action==Action.LEFT)assetCategoryChoice=clamp(assetCategoryChoice-1,SpriteAsset.Category.values().length-1);
                if(action==Action.DOWN||action==Action.RIGHT)assetCategoryChoice=clamp(assetCategoryChoice+1,SpriteAsset.Category.values().length-1);
                if(action==Action.CANCEL)mode=categoryForDraft?Mode.ASSET_SAVE:Mode.ASSETS;
                if(action==Action.CONFIRM)finishCategory();
                return;
            }
            if(tool==3&&mapEditor.choosingTool){
                if(action==Action.UP||action==Action.LEFT)mapEditor.toolChoice=clamp(mapEditor.toolChoice-1,6);
                if(action==Action.DOWN||action==Action.RIGHT)mapEditor.toolChoice=clamp(mapEditor.toolChoice+1,6);
                if(action==Action.CONFIRM){
                    if(mapEditor.toolChoice==3)flagDraft=new FlagDraft(cart,mapEditor.tile);
                    else if(mapEditor.toolChoice>=4)mapEditor.region=new MapRegion(cart,MapRegion.Operation.values()[mapEditor.toolChoice-4],mapEditor.x,mapEditor.y);
                    else mapEditor.tool=MapEditor.Tool.values()[mapEditor.toolChoice];
                    mapEditor.choosingTool=false;
                }
                if(action==Action.CANCEL)mapEditor.choosingTool=false;
                return;
            }
            if(tool==3&&mapEditor.region!=null){
                MapRegion r=mapEditor.region;
                if(action==Action.LEFT)r.move(-1,0);if(action==Action.RIGHT)r.move(1,0);
                if(action==Action.UP)r.move(0,-1);if(action==Action.DOWN)r.move(0,1);
                if(action==Action.PREVIOUS)r.move(-8,0);if(action==Action.NEXT)r.move(8,0);
                if(action==Action.CHECK)r.move(0,-8);if(action==Action.REDO)r.move(0,8);
                if((action==Action.CANCEL&&r.back())||action==Action.UNDO){mapEditor.region=null;notice="Область не изменена";}
                if(action==Action.CONFIRM&&r.confirm()){
                    boolean differs=r.proposal().count>0;save(r.candidate(cart),true);
                    mapEditor.x=r.dx;mapEditor.y=r.dy;mapEditor.region=null;
                    notice=differs?"Область сохранена · Y отмена":"Клетки не изменились";
                }
                return;
            }
            if(tool==3&&mapEditor.pending()){
                if(action==Action.LEFT)mapEditor.move(-1,0);
                if(action==Action.RIGHT)mapEditor.move(1,0);
                if(action==Action.UP)mapEditor.move(0,-1);
                if(action==Action.DOWN)mapEditor.move(0,1);
                if(action==Action.CANCEL)mapEditor.back();
                if(action==Action.UNDO){mapEditor.clear();notice="Предложение отменено";}
                if(action==Action.CONFIRM){
                    if(mapEditor.phase==1)mapEditor.review();
                    else{boolean differs=mapEditor.preview(cart).count>0;save(mapEditor.candidate(cart),true);mapEditor.clear();notice=differs?"Карта изменена · Y отмена":"Клетки не изменились";}
                }
                return;
            }
            if(tool==3&&mapEditor.picking){
                if(action==Action.LEFT)mapEditor.move(-1,0);
                if(action==Action.RIGHT)mapEditor.move(1,0);
                if(action==Action.UP)mapEditor.move(0,-1);
                if(action==Action.DOWN)mapEditor.move(0,1);
                if(action==Action.CONFIRM)mapEditor.accept();
                if(action==Action.CANCEL)mapEditor.cancel();
                return;
            }
            if (action == Action.MENU) { overlayReturn = mode; mode = Mode.MENU; menuItem = 0; menuRedo = false; return; }
            if (action == Action.ASSETS) { showAssets();return; }
            if (action == Action.TEST) { port.launch(cart.bytes()); return; }
            if (action == Action.UNDO || action == Action.REDO) {
                boolean returning = action == Action.REDO;
                ArrayDeque<WorkshopCartridge> from = returning ? redo : undo, to = returning ? undo : redo;
                if (!from.isEmpty()) {
                    WorkshopCartridge previous = cart;
                    save(from.peek(), false);
                    from.pop(); to.push(previous); // Failed writes leave both stacks intact.
                    notice = returning ? "Правка возвращена" : "Отменено · вернуть можно в меню";
                } else notice = returning ? "Нет правок для возврата" : "Нет изменений для отмены";
                return;
            }
            if (action == Action.PREVIOUS) { switchTool(tool - 1); return; }
            if (action == Action.NEXT) { switchTool(tool + 1); return; }
            if(tool==3){
                if(action==Action.DRAW_TOOLS||action==Action.CHECK){mapEditor.tools();return;}
                if(action==Action.LEFT)mapEditor.move(-1,0);
                if(action==Action.RIGHT)mapEditor.move(1,0);
                if(action==Action.UP)mapEditor.move(0,-1);
                if(action==Action.DOWN)mapEditor.move(0,1);
                if(action==Action.CONTEXT)mapEditor.begin();
                if(action==Action.CONFIRM){if(mapEditor.tool==MapEditor.Tool.BRUSH)save(cart.withTile(mapEditor.x,mapEditor.y,mapEditor.tile),true);else mapEditor.start(cart);}
                if(action==Action.CANCEL){overlayReturn=mode;mode=Mode.MENU;menuItem=3;}
                return;
            }
            if (tool == 2) {
                if(action==Action.REGION){chooseRegion();return;}
                if(action==Action.ZOOM&&!browsingSprites){zoom=!zoom;return;}
                if(action==Action.DRAW_TOOLS&&!browsingSprites){overlayReturn=mode;drawToolCursor=drawMenuIndex(drawTool);mode=Mode.DRAW_TOOLS;return;}
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
                else if(tool==0){port.library();}
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
                    beginCode();
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
