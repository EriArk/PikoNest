package art.pikoos.lab.core;

import java.util.*;

/** Read-only navigation over the recognized recipe, never an independent rule store. */
public final class GameRulesOverview {
    public enum Kind { GOAL, BACKGROUND, MOVEMENT, INTERACTION, ANIMATION, ADD }
    public static final class Entry {
        public final Kind kind;
        public final int element;
        public final String row,description;
        Entry(Kind k,int e,String r,String d){kind=k;element=e;row=r;description=d;}
    }
    public final List<Entry> entries=new ArrayList<>();
    public static String goalName(int goal){return new String[]{"No automatic win","Collect all items","All switches on"}[goal];}
    public static String goalDescription(PlayModel model,int goal){
        int count=0;for(PlayModel.Element e:model.elements)if(e.rule==(goal==1?1:4))count++;
        if(goal==0)return "No automatic goal. Contact win / lose rules still apply. X restarts.";
        if(count==0)return goal==1?"No collect rules yet. Give an element Collect this in Behavior.":"No switches yet. Give an element Toggle switch in Behavior.";
        return goal==1?"Win after collecting "+count+(count==1?" item.":" items.")+" Only Collect interactions count.":"Win when all "+count+(count==1?" switch is on.":" switches are on.")+" Linked switches count too.";
    }
    public GameRulesOverview(PlayModel model,GameElementLabels labels){
        entries.add(new Entry(Kind.GOAL,-1,"Game goal\n"+goalName(model.goal),goalDescription(model,model.goal)));
        entries.add(new Entry(Kind.BACKGROUND,-1,"Background\nColor "+model.background,"Change the starting background color. X restarts the game."));
        for(int n=0;n<model.elements.size();n++){
            PlayModel.Element e=model.elements.get(n);String name=labels.get(n);boolean any=false;
            if(e.movement!=0){any=true;entries.add(new Entry(Kind.MOVEMENT,n,name+"\nMove: "+PlayModel.MOVEMENT[e.movement],name+" moves "+(e.movement==2?"left / right":e.movement==3?"up / down":"in all directions")+" at "+e.speed+" px per step."));}
            if(e.rule!=0){any=true;String description=e.rule==4?"O toggles "+name+(e.neighbor>=0?" + "+labels.get(e.neighbor):"")+".":labels.get(e.target)+" touches "+name+": "+new String[]{"","collect once","win","lose"}[e.rule]+".";
                entries.add(new Entry(Kind.INTERACTION,n,name+"\n"+new String[]{"","Collect on contact","Win on contact","Lose on contact","Toggle switch"}[e.rule],description));}
            if(e.animation!=null){any=true;entries.add(new Entry(Kind.ANIMATION,n,name+"\nAnimation: "+e.animation.frames.size()+" frames",name+": "+new String[]{"looping animation","animate while moving","animate while switched on"}[e.animation.mode]+". Edit frames or timing."));}
            if(!any)entries.add(new Entry(Kind.ADD,n,name+"\nAdd behavior","No movement or interaction yet. Choose what "+name+" does."));
        }
    }
    public int find(Kind kind,int element){
        for(int n=0;n<entries.size();n++){Entry e=entries.get(n);if(e.kind==kind&&e.element==element)return n;}
        for(int n=0;n<entries.size();n++)if(entries.get(n).element==element)return n;
        return 0;
    }
}
