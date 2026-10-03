import art.pikoos.lab.core.*;
import java.util.*;
import java.util.concurrent.CancellationException;

public class GameTreeTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static GameTree.Entry dir(String id,String name){return new GameTree.Entry(id,name,true);}
    static GameTree.Entry cart(String id,String name){return new GameTree.Entry(id,name,false);}
    static class Tree implements GameTree.Source {
        Map<String,List<GameTree.Entry>> entries=new HashMap<>();int opened,closed,reads;boolean cancel;
        Tree put(String id,GameTree.Entry... rows){entries.put(id,Arrays.asList(rows));return this;}
        public GameTree.Children open(String id)throws Exception{
            if(!entries.containsKey(id))throw new Exception("permission revoked");
            opened++;Iterator<GameTree.Entry> rows=entries.get(id).iterator();
            return new GameTree.Children(){
                public GameTree.Entry next(){reads++;return rows.hasNext()?rows.next():null;}
                public void close(){closed++;}
            };
        }
        GameTree.Result scan()throws Exception{return GameTree.scan(this,"root",()->cancel);}
    }
    public static void main(String[] args)throws Exception{
        Tree tree=new Tree().put("root",dir("puzzles","Puzzles"),cart("one","Same.p8"),dir("bad","Unavailable"),cart("other","photo.png"))
            .put("puzzles",cart("two","Same.P8"),dir("deep","Chapters"),dir("root","Loop"),cart("one","Alias.p8"))
            .put("deep",cart("three","Same.P8.PNG"));
        GameTree.Result r=tree.scan();
        check(r.carts.size()==3&&r.carts.get(0).id.equals("one"),"root first, nested carts, aliases deduplicated");
        check(r.carts.get(1).folder.equals("Puzzles")&&r.carts.get(2).folder.equals("Puzzles / Chapters"),"relative folder breadcrumbs");
        check(r.folders==2&&r.unreadable==1&&!r.limited,"unreadable child does not hide accessible siblings");
        check(tree.opened==tree.closed,"all cursors closed, cycle terminates");
        try{new Tree().scan();throw new AssertionError("root failure hidden");}catch(Exception expected){checks++;}
        Tree empty=new Tree().put("root");check(empty.scan().carts.isEmpty()&&!empty.scan().limited,"empty root");
        Tree many=new Tree();List<GameTree.Entry> rows=new ArrayList<>();
        for(int i=0;i<GameTree.MAX_GAMES;i++)rows.add(cart("c"+i,"g.p8"));
        many.entries.put("root",rows);check(!many.scan().limited,"exact cart cap is complete");
        rows.add(cart("extra","extra.p8"));r=many.scan();
        check(r.limited&&r.carts.size()==GameTree.MAX_GAMES&&many.opened==many.closed,"cart cap and early close");
        Tree entries=new Tree();rows=new ArrayList<>();
        for(int i=0;i<GameTree.MAX_ENTRIES-1;i++)rows.add(cart("n"+i,"not-a-cart.txt"));
        rows.add(dir("sub","Sub"));entries.entries.put("root",rows);entries.put("sub",cart("overflow","game.p8"));r=entries.scan();
        check(r.limited&&r.carts.isEmpty()&&entries.opened==entries.closed,"entry budget global across folders");
        Tree deep=new Tree().put("root",dir("d1","1"));
        for(int i=1;i<=GameTree.MAX_DEPTH;i++)deep.put("d"+i,cart("c"+i,"game.p8"),dir("d"+(i+1),""+(i+1)));
        r=deep.scan();check(r.limited&&r.carts.size()==GameTree.MAX_DEPTH&&deep.opened==17,"bounded depth still reads final allowed level");
        Tree cancelled=new Tree().put("root",cart("x","game.p8"));
        try{GameTree.scan(cancelled,"root",()->cancelled.reads>0);throw new AssertionError("ignored cancellation");}
        catch(CancellationException expected){check(cancelled.opened==cancelled.closed&&cancelled.reads==1,"cancel closes active cursor before more work");}
        PlaySession s=new PlaySession(new PlaySession.Port(){
            public void launch(PlaySession.Game g){}public void refresh(){}public void workshop(){}public void folders(){}
            public void favorite(PlaySession.Game g,boolean b){}
        });
        PlaySession.Game a=new PlaySession.Game("a","Same","",null,false,0,CartridgeFormat.P8,"A"),
            b=new PlaySession.Game("b","Same","",null,true,17,CartridgeFormat.P8,"B"),
            png=new PlaySession.Game("p","Same","",null,false,0,CartridgeFormat.of("a.p8.png"),"A");
        s.replace(Arrays.asList(b,png,a),"b");check(s.current()==b&&s.games().get(0)==a&&s.games().get(1)==png,"same names sorted by folder then format");
        s.replace(Arrays.asList(png,a,b),"b");check(s.current()==b,"provider reordering preserves selected opaque id");
        s.filter=2;s.rebuild("b");check(s.games().size()==1&&s.current()==b&&!a.favorite,"favorite belongs only to its file");
        s.filter=1;s.rebuild("b");check(s.games().size()==1&&s.current()==b,"recent belongs only to its file");
        System.out.println("GameTreeTest: "+checks+" checks passed");
    }
}
