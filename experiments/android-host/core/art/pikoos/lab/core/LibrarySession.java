package art.pikoos.lab.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Portable project shelf. Storage, file selection and view/lifecycle live behind Port. */
public final class LibrarySession {
    public interface Port {
        List<String> ids() throws Exception;
        byte[] read(String id) throws Exception;
        // Must publish a complete new project exclusively, never replace an existing project.
        void create(String id, byte[] bytes) throws Exception;
        void open(String id, WorkshopCartridge cart) throws Exception;
        void resume();
        default void folders(){}
        default String title(String id)throws Exception{return LibrarySession.title(id);}
        default void pickImport()throws Exception{throw new Exception("File picker is unavailable");}
        // Exclusive publication; retrying the same snapshot must be idempotent.
        default void importProject(CartridgeImport draft)throws Exception{throw new Exception("Import is unavailable");}
        default void clearImport()throws Exception{}
        default void saveExport(CartridgeExport draft)throws Exception{throw new Exception("Export is unavailable");}
        default void pickExport(CartridgeExport draft)throws Exception{throw new Exception("Folder picker is unavailable");}
        default void clearExport()throws Exception{}
    }
    public static final class Entry {
        public final String id, title, error;
        public final WorkshopCartridge cart;
        Entry(String id, String title, WorkshopCartridge cart, String error) {
            this.id=id;this.title=title;this.cart=cart;this.error=error;
        }
    }
    public enum Mode { SHELF, CREATE, READING, IMPORT, EXPORT, EXPORT_PICKER, EXPORT_WRITING, EXPORT_SAVED, EXPORT_UNCERTAIN, ERROR, COPY }
    public final ShelfMenu menu=new ShelfMenu(ShelfMenu.Command.NEW,ShelfMenu.Command.COPY,
        ShelfMenu.Command.IMPORT,ShelfMenu.Command.EXPORT,ShelfMenu.Command.PLAY,ShelfMenu.Command.FOLDERS);
    public CartridgeImport importing;
    public CartridgeExport exporting;
    private Mode errorReturn=Mode.SHELF;
    private final Port port;
    private final byte[][] templates;
    public int templateChoice;
    public int templateCount(){return templates.length;}
    private final ArrayList<Entry> entries=new ArrayList<>();
    public int selected, focus; // focus: 0 cards, 1 new, 2 copy, 3 import, 4 export
    public Mode mode=Mode.SHELF;
    public String error="";
    public LibrarySession(Port port, byte[]... templates) {
        if(templates.length<1||templates.length>3)throw new IllegalArgumentException("Invalid template count");
        this.port=port;this.templates=new byte[templates.length][];
        for(int i=0;i<templates.length;i++)this.templates[i]=new WorkshopCartridge(templates[i]).bytes();
    }
    public static boolean validId(String id) {
        return id!=null&&(id.equals("moon-garden")||CartridgeImport.validId(id)||id.matches("(?:garden|remix|blank|puzzle)-[0-9]{4,8}"));
    }
    public static String title(String id) {
        if(!validId(id))throw new IllegalArgumentException("Invalid project ID");
        if(id.equals("moon-garden"))return "Moon Garden";
        if(CartridgeImport.validId(id))return "Imported game";
        return (id.startsWith("garden-")?"New game ":id.startsWith("blank-")?"Blank project ":id.startsWith("puzzle-")?"Lights ":"Copy ")+Integer.parseInt(id.substring(id.indexOf('-')+1));
    }
    public List<Entry> entries(){return Collections.unmodifiableList(entries);}
    public Entry current(){return entries.isEmpty()?null:entries.get(selected);}
    public void refresh(String preferred)throws Exception {
        List<String> ids=new ArrayList<>(port.ids());Collections.sort(ids);
        if(ids.remove("moon-garden"))ids.add(0,"moon-garden");
        ArrayList<Entry> next=new ArrayList<>();
        for(String id:ids) {
            if(!validId(id))continue;
            try{next.add(new Entry(id,port.title(id),new WorkshopCartridge(port.read(id)),""));}
            catch(Exception e){next.add(new Entry(id,title(id),null,message(e)));}
        }
        entries.clear();entries.addAll(next);
        selected=Math.min(selected,Math.max(0,entries.size()-1));
        for(int i=0;i<entries.size();i++)if(entries.get(i).id.equals(preferred))selected=i;
        focus=entries.isEmpty()?1:0;mode=Mode.SHELF;
    }
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    public void fail(Exception e){error=message(e);errorReturn=importing!=null?Mode.IMPORT:exporting!=null?exportMode():Mode.SHELF;mode=Mode.ERROR;}
    public void stageImport(CartridgeImport draft){importing=draft;mode=Mode.IMPORT;}
    private Mode exportMode(){
        switch(exporting.state){case WRITING:return Mode.EXPORT_WRITING;case SAVED:return Mode.EXPORT_SAVED;case UNCERTAIN:return Mode.EXPORT_UNCERTAIN;default:return Mode.EXPORT;}
    }
    public void stageExport(CartridgeExport draft){exporting=draft;mode=exportMode();}
    public void exportPickerCancelled(){if(exporting!=null)mode=exportMode();}
    private void prepareExport()throws Exception{
        Entry entry=current();if(entry==null)return;
        CartridgeExport draft=new CartridgeExport(entry.id,port.title(entry.id),port.read(entry.id));
        port.saveExport(draft);stageExport(draft);
    }
    public void choose(int index){if(mode!=Mode.SHELF||index<0||index>=entries.size())return;selected=index;focus=0;act(Action.CONFIRM);}
    public void command(int item){if(mode!=Mode.SHELF||item<1||item>4)return;focus=item;act(Action.CONFIRM);}
    private void create(boolean copy)throws Exception {
        Entry from=current();
        if(copy&&from==null)return;
        // Read again: copy authoritative saved bytes, never a stale thumbnail or editor draft.
        byte[] bytes=copy?new WorkshopCartridge(port.read(from.id)).bytes():templates[templateChoice].clone();
        List<String> ids=port.ids();String prefix=copy?"remix-":new String[]{"garden-","blank-","puzzle-"}[templateChoice],id;
        int number=1;
        do{id=prefix+String.format(java.util.Locale.ROOT,"%04d",number++);}while(ids.contains(id));
        port.create(id,bytes);
        refresh(id); // A successful file is retained even if opening subsequently fails.
        port.open(id,new WorkshopCartridge(bytes));
    }
    public void act(Action a) {
        try {
            if(mode==Mode.SHELF&&menu.open){
                ShelfMenu.Command command=menu.act(a);
                if(command==ShelfMenu.Command.NEW)command(1);
                if(command==ShelfMenu.Command.COPY)command(2);
                if(command==ShelfMenu.Command.IMPORT)command(3);
                if(command==ShelfMenu.Command.EXPORT)command(4);
                if(command==ShelfMenu.Command.PLAY)port.resume();
                if(command==ShelfMenu.Command.FOLDERS)port.folders();
                return;
            }
            if(mode==Mode.COPY){
                if(a==Action.CANCEL){mode=Mode.SHELF;focus=0;}
                if(a==Action.CONFIRM)create(true);
                return;
            }
            if(mode==Mode.ERROR){if(a==Action.CANCEL||a==Action.CONFIRM){mode=errorReturn;if(mode==Mode.SHELF)focus=entries.isEmpty()?1:0;}return;}
            if(mode==Mode.READING){if(a==Action.CANCEL){mode=Mode.SHELF;focus=entries.isEmpty()?1:0;}return;}
            if(mode==Mode.EXPORT_PICKER||mode==Mode.EXPORT_WRITING)return;
            if(mode==Mode.EXPORT||mode==Mode.EXPORT_SAVED||mode==Mode.EXPORT_UNCERTAIN){
                if(a==Action.CANCEL||(a==Action.CONFIRM&&mode==Mode.EXPORT_SAVED)){
                    port.clearExport();exporting=null;mode=Mode.SHELF;focus=entries.isEmpty()?1:0;
                }else if(a==Action.CONFIRM){
                    CartridgeExport next=exporting.withState(CartridgeExport.State.PREVIEW,"");
                    port.saveExport(next);stageExport(next);mode=Mode.EXPORT_PICKER;
                    port.pickExport(next);
                }
                return;
            }
            if(mode==Mode.IMPORT){
                if(a==Action.CANCEL){port.clearImport();importing=null;mode=Mode.SHELF;focus=entries.isEmpty()?1:0;}
                if(a==Action.CONFIRM){
                    CartridgeImport draft=importing;
                    port.importProject(draft);port.clearImport();importing=null;
                    refresh(draft.id);port.open(draft.id,draft.cart);
                }
                return;
            }
            if(mode==Mode.CREATE){
                if(a==Action.LEFT||a==Action.UP)templateChoice=Math.max(0,templateChoice-1);
                if(a==Action.RIGHT||a==Action.DOWN)templateChoice=Math.min(templates.length-1,templateChoice+1);
                if(a==Action.CANCEL){mode=Mode.SHELF;focus=entries.isEmpty()?1:0;}
                if(a==Action.CONFIRM)create(false);
                return;
            }
            if(a==Action.CANCEL){port.resume();return;}
            if(a==Action.MENU){menu.show();return;}
            if(a==Action.UP||a==Action.LEFT||a==Action.PREVIOUS){selected=Math.max(0,selected-1);focus=entries.isEmpty()?1:0;}
            if(a==Action.DOWN||a==Action.RIGHT||a==Action.NEXT){selected=Math.min(Math.max(0,entries.size()-1),selected+1);focus=entries.isEmpty()?1:0;}
            if(a==Action.CONFIRM){
                if(focus==1){templateChoice=Math.min(1,templates.length-1);mode=Mode.CREATE;}
                else if(focus==2&&current()!=null)mode=Mode.COPY;
                else if(focus==3)port.pickImport();
                else if(focus==4)prepareExport();
                else if(current()!=null)port.open(current().id,new WorkshopCartridge(port.read(current().id)));
            }
        }catch(Exception e){fail(e);}
    }
}
