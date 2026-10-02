package art.pikoos.lab.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Portable, bounded owned-template library. Storage and view/lifecycle live behind Port. */
public final class LibrarySession {
    public interface Port {
        List<String> ids() throws Exception;
        byte[] read(String id) throws Exception;
        // Must publish a complete new project exclusively, never replace an existing project.
        void create(String id, byte[] bytes) throws Exception;
        void open(String id, WorkshopCartridge cart) throws Exception;
        void resume();
    }
    public static final class Entry {
        public final String id, title, error;
        public final WorkshopCartridge cart;
        Entry(String id, WorkshopCartridge cart, String error) {
            this.id=id;this.title=title(id);this.cart=cart;this.error=error;
        }
    }
    public enum Mode { SHELF, CREATE, ERROR }
    private final Port port;
    private final byte[] template;
    private final ArrayList<Entry> entries=new ArrayList<>();
    public int selected, focus; // focus: 0 cards, 1 new, 2 copy
    public Mode mode=Mode.SHELF;
    public String error="";
    public LibrarySession(Port port, byte[] template) {
        this.port=port;this.template=new WorkshopCartridge(template).bytes();
    }
    public static boolean validId(String id) {
        return id!=null&&(id.equals("moon-garden")||id.matches("(?:garden|remix)-[0-9]{4,8}"));
    }
    public static String title(String id) {
        if(!validId(id))throw new IllegalArgumentException("Invalid project ID");
        if(id.equals("moon-garden"))return "Лунный сад";
        return (id.startsWith("garden-")?"Новая игра ":"Копия ")+Integer.parseInt(id.substring(id.indexOf('-')+1));
    }
    public List<Entry> entries(){return Collections.unmodifiableList(entries);}
    public Entry current(){return entries.isEmpty()?null:entries.get(selected);}
    public void refresh(String preferred)throws Exception {
        List<String> ids=new ArrayList<>(port.ids());Collections.sort(ids);
        if(ids.remove("moon-garden"))ids.add(0,"moon-garden");
        ArrayList<Entry> next=new ArrayList<>();
        for(String id:ids) {
            if(!validId(id))continue;
            try{next.add(new Entry(id,new WorkshopCartridge(port.read(id)),""));}
            catch(Exception e){next.add(new Entry(id,null,message(e)));}
        }
        entries.clear();entries.addAll(next);
        selected=Math.min(selected,Math.max(0,entries.size()-1));
        for(int i=0;i<entries.size();i++)if(entries.get(i).id.equals(preferred))selected=i;
        focus=entries.isEmpty()?1:0;mode=Mode.SHELF;
    }
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    public void fail(Exception e){error=message(e);mode=Mode.ERROR;}
    public void choose(int index){if(mode!=Mode.SHELF||index<0||index>=entries.size())return;selected=index;focus=0;act(Action.CONFIRM);}
    public void command(int item){if(mode!=Mode.SHELF)return;focus=item;act(Action.CONFIRM);}
    private void create(boolean copy)throws Exception {
        Entry from=current();
        if(copy&&from==null)return;
        // Read again: copy authoritative saved bytes, never a stale thumbnail or editor draft.
        byte[] bytes=copy?new WorkshopCartridge(port.read(from.id)).bytes():template.clone();
        List<String> ids=port.ids();String prefix=copy?"remix-":"garden-",id;
        int number=1;
        do{id=prefix+String.format(java.util.Locale.ROOT,"%04d",number++);}while(ids.contains(id));
        port.create(id,bytes);
        refresh(id); // A successful file is retained even if opening subsequently fails.
        port.open(id,new WorkshopCartridge(bytes));
    }
    public void act(Action a) {
        try {
            if(mode==Mode.ERROR){if(a==Action.CANCEL||a==Action.CONFIRM)mode=Mode.SHELF;return;}
            if(mode==Mode.CREATE){
                if(a==Action.CANCEL)mode=Mode.SHELF;
                if(a==Action.CONFIRM)create(false);
                return;
            }
            if(a==Action.CANCEL){port.resume();return;}
            if(a==Action.UP)focus=entries.isEmpty()?1:0;
            if(a==Action.DOWN)focus=1;
            if(a==Action.LEFT){if(focus==0)selected=Math.max(0,selected-1);else focus=1;}
            if(a==Action.RIGHT){if(focus==0)selected=Math.min(Math.max(0,entries.size()-1),selected+1);else focus=2;}
            if(a==Action.CONTEXT&&current()!=null){create(true);return;}
            if(a==Action.CONFIRM){
                if(focus==1)mode=Mode.CREATE;
                else if(focus==2)create(true);
                else if(current()!=null)port.open(current().id,new WorkshopCartridge(port.read(current().id)));
            }
        }catch(Exception e){fail(e);}
    }
}
