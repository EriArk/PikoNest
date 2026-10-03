package art.pikoos.lab.core;

import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Game library, not editable projects. IDs are opaque storage handles. */
public final class PlaySession {
    public static final class Game {
        public final String id,title,problem;
        public final int[] cover;
        public final CartridgeFormat format;
        public boolean favorite;public long recent;
        public Game(String id,String title,String problem,int[] cover,boolean favorite,long recent){this(id,title,problem,cover,favorite,recent,CartridgeFormat.P8);}
        public Game(String id,String title,String problem,int[] cover,boolean favorite,long recent,CartridgeFormat format){this.id=id;this.title=title;this.problem=problem;this.cover=cover;this.favorite=favorite;this.recent=recent;this.format=format;}
    }
    public interface Port {
        void launch(Game game);void refresh();void workshop();void folders();
        void favorite(Game game,boolean value)throws Exception;
    }
    private final Port port;
    private List<Game> all=new ArrayList<>(),visible=new ArrayList<>();
    public int selected,filter,columns=3;
    public boolean busy;
    public String notice="",error="";
    public PlaySession(Port port){this.port=port;}
    public List<Game> games(){return Collections.unmodifiableList(visible);}
    public Game current(){return visible.isEmpty()?null:visible.get(selected);}
    public void replace(List<Game> games,String preferred){all=new ArrayList<>(games);rebuild(preferred);busy=false;error="";}
    public void rebuild(String preferred){
        visible=new ArrayList<>();for(Game g:all)if(filter==0||filter==1&&g.recent>0||filter==2&&g.favorite)visible.add(g);
        visible.sort(filter==1?(a,b)->{int n=Long.compare(b.recent,a.recent);return n!=0?n:a.title.compareToIgnoreCase(b.title);}:(a,b)->a.title.compareToIgnoreCase(b.title));
        selected=0;for(int i=0;i<visible.size();i++)if(visible.get(i).id.equals(preferred))selected=i;
    }
    public void select(int index){if(!busy&&!visible.isEmpty()){selected=Math.max(0,Math.min(visible.size()-1,index));error="";}}
    public void fail(String message){busy=false;error=message;}
    public void act(Action a){
        if(a==Action.NEXT){port.workshop();return;}
        if(a==Action.MENU){port.folders();return;}
        if(a==Action.CANCEL){if(!error.isEmpty())error="";return;}
        if(busy)return;
        if(a==Action.PREVIOUS){port.refresh();return;}
        Game g=current();
        if(a==Action.LEFT)select(selected-1);if(a==Action.RIGHT)select(selected+1);
        if(a==Action.UP)select(selected-columns);if(a==Action.DOWN)select(selected+columns);
        if(a==Action.UNDO){filter=(filter+1)%3;rebuild(g==null?"":g.id);error="";}
        if(a==Action.CONTEXT&&g!=null){
            try{port.favorite(g,!g.favorite);g.favorite=!g.favorite;rebuild(g.id);}catch(Exception e){fail("Не удалось сохранить избранное");}
        }
        if((a==Action.CONFIRM||a==Action.TEST)&&g!=null){
            if(!g.problem.isEmpty()){fail(g.problem);return;}
            error="";busy=true;port.launch(g);
        }
    }
}
