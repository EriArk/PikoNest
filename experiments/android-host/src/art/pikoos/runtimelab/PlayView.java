package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import art.pikoos.lab.core.PlaySession;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Local games stay separate from editable projects; both shelves share a visual grammar. */
final class PlayView extends ShelfSurface {
    private final PlaySession s;private final Runnable changed;
    PlayView(Context context,PlaySession session,boolean swap,Runnable changed){
        super(context,swap);s=session;this.changed=changed;
        setContentDescription("Play. Up and down: game. Confirm: play. X: favorite. Y: filter. Select: actions. Back: launcher.");
    }
    void action(Action action){s.act(action);changed.run();invalidate();}
    private void workshop(){s.menu.show();s.menu.selected=0;action(Action.CONFIRM);}
    @Override protected void onDraw(Canvas canvas){
        begin(canvas);s.columns=1;
        header(false,()->{},()->workshop());
        String[] filters={"All games","Recent","Favorites"};
        text(filters[s.filter],16,132,24,7);
        fit(s.games().size()+(s.games().size()==1?" game":" games"),w-135,132,18,6,119);
        int count=rows(),start=s.selected/count*count;float width=listWidth();
        for(int i=start;i<Math.min(s.games().size(),start+count);i++){
            final int chosen=i;PlaySession.Game g=s.games().get(i);
            row(g.title,(g.favorite?"♥ ":"")+(!g.problem.isEmpty()?"Needs attention":g.format.extension+"  ·  "+(g.folder.isEmpty()?"Games":g.folder)),i,s.selected,start,width,
                ()->{s.select(chosen);changed.run();invalidate();});
        }
        PlaySession.Game game=s.current();
        if(w>=540&&game!=null){float x=40+width,size=w-x-16;cover(game.cover,x,150,size);}
        if(game==null){text(s.busy?"Finding your games…":"A little room for play",16,189,20,14);
            wrap(s.filter==0?"Choose your games folder from the menu.":"No games here yet. Change the filter with Y.",16,225,w-32,6,3);}
        String message=!s.error.isEmpty()?s.error:s.busy?"Reading games…":game!=null&&!game.problem.isEmpty()?game.problem:s.notice;
        if(game!=null)fit(game.title,16,h-109,20,7,w-32);
        fit(message,16,h-79,18,!s.error.isEmpty()?9:6,w-32);
        footer("Play",w<500?"X Fav":"X Favorite",Action.CONTEXT);
        menu(s.menu);c.restore();
    }
}
