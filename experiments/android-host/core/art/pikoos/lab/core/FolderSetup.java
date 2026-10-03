package art.pikoos.lab.core;

import art.pikoos.lab.core.WorkshopSession.Action;

/** Portable folder intentions. Locations are opaque handles, never native paths. */
public final class FolderSetup {
    public enum Role { GAMES, DOWNLOADS, PROJECTS, DATA;
        public boolean writable(){return this!=GAMES;}
    }
    public enum State { EMPTY, UNKNOWN, CHECKING, READY, UNAVAILABLE }
    public interface Port {
        void pick(Role role);
        void check(Role role,String location);
        void save(Role role,String location,String name)throws Exception;
        void leave();
    }
    public static final class Entry {
        public String location,name,problem="";
        public State state;
        Entry(String location,String name){this.location=location;this.name=name;state=location.isEmpty()?State.EMPTY:State.UNKNOWN;}
    }
    private final Port port;
    private final Entry[] entries=new Entry[4];
    public int selected;
    public boolean busy;
    public String notice="";
    private Role pending;
    private String candidate;
    private State previous;
    public FolderSetup(Port port,String[] locations,String[] names){
        this.port=port;for(int i=0;i<4;i++)entries[i]=new Entry(locations[i],names[i]);
    }
    public Entry entry(int index){return entries[index];}
    public void select(int index){if(!busy)selected=Math.max(0,Math.min(3,index));}
    public void act(Action a){
        if(a==Action.CANCEL){
            if(busy){entry(pending.ordinal()).state=previous;busy=false;candidate=null;pending=null;}
            port.leave();return;
        }
        if(busy)return;
        if(a==Action.UP)select(selected-1);
        if(a==Action.DOWN)select(selected+1);
        if(a==Action.CONFIRM)port.pick(Role.values()[selected]);
        if(a==Action.CONTEXT){
            Entry e=entry(selected);
            if(e.location.isEmpty()){notice="Сначала выбери папку";return;}
            begin(Role.values()[selected],e.location);
        }
    }
    public void begin(Role role,String location){
        if(busy)return;
        pending=role;candidate=location;selected=role.ordinal();busy=true;
        Entry e=entry(selected);previous=e.state;e.state=State.CHECKING;notice="Проверяем доступ…";
        port.check(role,location);
    }
    public void complete(String name,String problem){
        if(!busy)return;
        Entry e=entry(pending.ordinal());boolean same=e.location.equals(candidate);
        if(problem==null){
            try{
                port.save(pending,candidate,name);
                e.location=candidate;e.name=name;e.state=State.READY;e.problem="";
                notice="Папка сохранена · доступ проверен";
            }catch(Exception failure){problem="Не удалось сохранить выбор. Попробуй ещё раз.";}
        }
        if(problem!=null){
            e.state=same?State.UNAVAILABLE:previous;e.problem=problem;
            notice=same?"Выбери папку снова для подключения":"Новый выбор не сохранён";
        }
        busy=false;candidate=null;pending=null;
    }
}
