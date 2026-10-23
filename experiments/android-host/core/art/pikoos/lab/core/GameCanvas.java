package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Game-first authoring over static placements and a verified ordinary-Lua behavior recipe. */
public final class GameCanvas {
    public enum Screen { HOME, ACTIONS, SIZE, DRAW, DRAW_MENU, PALETTE, PLACE, SHARED, DELETE, BEHAVIOR, SETTINGS, ANIMATION, RELATION, NAME, CHOICE, GOAL, OVERVIEW }
    public enum Operation { NONE, ADD, APPEARANCE, INDEPENDENT, MOVE, DUPLICATE, CONFIG, ANIMATION, RENAME }
    public enum Command { NONE, SAVE, TEST, TOOLS, PROJECTS, UNDO, REDO }
    public final WorkshopCartridge base;
    public final GameUses uses;
    public final PlayModel play;
    private final WorkshopCartridge layoutBase;
    public PlayModel settings;
    public GameAnimation animation;
    public boolean editingField;
    private int fieldBefore;
    private int relationshipField=-1,relationshipReturnFocus;
    private final GameElementLabels labels;
    public final GameElementNames names;
    public NameEditor nameDraft;
    private int choiceField=-1,choiceBefore;
    private boolean relationshipFromChoice;
    private GameRulesOverview overview;
    private GameRulesOverview.Kind overviewReturn;
    private int overviewElement=-1;
    public final String blocked;
    public Screen screen=Screen.HOME;
    public Operation operation=Operation.NONE;
    public int selected,focus,color=14,cursorX,cursorY,x=56,y=56;
    public boolean eraser;
    private SpriteRegion target;
    private int[] pixels;
    private int background=1;
    public GameCanvas(WorkshopCartridge cart){
        base=cart;play=PlayModel.read(cart);layoutBase=play==null?cart:play.layout(cart);uses=new GameUses(layoutBase,0);String reason="";
        GameElementNames parsed=GameElementNames.empty(uses.entries.size());
        try{recognize();parsed=GameElementNames.read(cart,uses);}catch(IllegalArgumentException e){reason=e.getMessage();}
        names=parsed;blocked=reason;
        List<SpriteRegion> regions=new ArrayList<>();if(blocked.isEmpty())for(int n=0;n<uses.entries.size();n++)regions.add(region(n));labels=new GameElementLabels(cart,regions,names.values);
    }
    private void recognize(){
        // Full-source allowlist, not a search for a marker or an apparently empty sheet.
        if(!uses.blocked.isEmpty())throw unsupported();
        String mask=new LuaContext(layoutBase.code()).masked();int phase=0;
        for(String line:mask.split("\\r\\n|\\r|\\n")){
            String t=line.trim();if(t.isEmpty())continue;
            if(phase==0&&t.matches("function\\s+_draw\\s*\\(\\s*\\)")){phase=1;continue;}
            if(phase==1&&t.matches("cls\\(\\s*(?:[0-9]|1[0-5])\\s*\\)")){
                background=Integer.parseInt(t.substring(t.indexOf('(')+1,t.indexOf(')')).trim());phase=2;continue;
            }
            if(phase==2&&t.matches("(?:sspr|spr)\\(\\s*-?[0-9]+(?:\\s*,\\s*-?[0-9]+)*\\s*\\)"))continue;
            if(phase==2&&t.equals("end")){phase=3;continue;}
            throw unsupported();
        }
        if(phase!=3)throw unsupported();
        for(GameUses.Entry e:uses.entries)if(e.linked()||(!e.kind().equals("spr")&&!e.kind().equals("sspr")))throw unsupported();
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Game view currently supports Blank, static sprites and Game view behaviors. Open All tools for this project's code, maps and other resources.");}
    private void supported(){if(!blocked.isEmpty())throw new IllegalArgumentException(blocked);}
    public boolean draft(){return operation!=Operation.NONE;}
    public SpriteRegion region(){return target;}
    public int pixel(int px,int py){return pixels[py*target.width+px];}
    public SpriteRegion region(int index){
        GameUses.Entry e=uses.entries.get(index);return e.kind().equals("spr")?new SpriteRegion(e.values[0]%16*8,e.values[0]/16*8,8,8):new SpriteRegion(e.values[0],e.values[1],e.values[2],e.values[3]);
    }
    public String label(int index){return labels.get(index);}
    /** Candidate order is also the controller-accessible list, including hidden/off-field placements. */
    public int[] relationshipChoices(){
        ArrayList<Integer> choices=new ArrayList<>();
        if(relationshipField==5)choices.add(-1);
        if(settings!=null)for(int n=0;n<settings.elements.size();n++)
            if(n!=selected&&(relationshipField==3||relationshipField==5&&settings.elements.get(n).rule==4))choices.add(n);
        int[] result=new int[choices.size()];for(int n=0;n<result.length;n++)result[n]=choices.get(n);return result;
    }
    private void beginRelationship(int field){
        relationshipReturnFocus=focus;relationshipField=field;screen=Screen.RELATION;editingField=false;
        PlayModel.Element e=settings.elements.get(selected);int before=field==3?e.target:e.neighbor;focus=0;
        int[] choices=relationshipChoices();for(int n=0;n<choices.length;n++)if(choices[n]==before)focus=n;
    }
    private void endRelationship(boolean confirm){
        if(relationshipFromChoice&&!confirm){int chosen=settings.elements.get(selected).rule;settings.elements.get(selected).rule=choiceBefore;screen=Screen.CHOICE;focus=chosen;}
        else{screen=Screen.BEHAVIOR;focus=relationshipReturnFocus;choiceField=-1;}
        relationshipField=-1;relationshipFromChoice=false;
    }
    private Command relationshipAction(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){endRelationship(false);return Command.NONE;}
        if(a==WorkshopSession.Action.UP||a==WorkshopSession.Action.LEFT)focus=Math.max(0,focus-1);
        if(a==WorkshopSession.Action.DOWN||a==WorkshopSession.Action.RIGHT)focus=Math.min(rows().length-1,focus+1);
        if(a==WorkshopSession.Action.CONFIRM){
            int[] choices=relationshipChoices();
            if(focus<choices.length){PlayModel.Element e=settings.elements.get(selected);if(relationshipField==3)e.target=choices[focus];else e.neighbor=choices[focus];endRelationship(true);}
            else if(focus==rows().length-1)endRelationship(false);
            else throw new IllegalArgumentException("Add another sprite from Game view, then choose it here.");
        }
        return Command.NONE;
    }
    private PlayModel relationshipPreview(){
        if(screen==Screen.GOAL&&focus<3){PlayModel preview=settings.copy();preview.goal=focus;return preview;}
        if(screen==Screen.CHOICE&&focus<rows().length-1){
            PlayModel preview=settings.copy();if(choiceField==0)preview.elements.get(selected).movement=focus;
            else{if(focus>=1&&focus<=3)throw new IllegalArgumentException("Confirm this interaction and choose who touches the element before Test.");preview.elements.get(selected).rule=focus;}return preview;
        }
        if(screen!=Screen.RELATION||focus>=relationshipChoices().length)return settings;
        PlayModel preview=settings.copy();PlayModel.Element e=preview.elements.get(selected);int other=relationshipChoices()[focus];
        if(relationshipField==3)e.target=other;else e.neighbor=other;return preview;
    }
    public int relatedElement(){
        if(screen==Screen.RELATION){int[] choices=relationshipChoices();return focus<choices.length?choices[focus]:-1;}
        if(screen!=Screen.BEHAVIOR&&screen!=Screen.OVERVIEW)return -1;
        if(screen==Screen.OVERVIEW&&(focus>=overview().entries.size()||overview().entries.get(focus).kind!=GameRulesOverview.Kind.INTERACTION))return -1;
        PlayModel.Element e=(settings!=null?settings:play).elements.get(selected);int other=e.rule>=1&&e.rule<=3?e.target:e.rule==4?e.neighbor:-1;
        return other>=0&&other<uses.entries.size()&&other!=selected?other:-1;
    }
    public boolean globalRuleSelection(){return screen==Screen.GOAL||screen==Screen.SETTINGS||screen==Screen.OVERVIEW&&rowElement(focus)<0;}
    public int[] goalElements(){
        if(screen!=Screen.GOAL&&!(screen==Screen.OVERVIEW&&focus==0)&&!(screen==Screen.SETTINGS&&focus==0))return new int[0];
        PlayModel model=settings!=null?settings:play;if(model==null)return new int[0];
        int goal=screen==Screen.GOAL&&focus<3?focus:model.goal;if(goal==0)return new int[0];
        ArrayList<Integer> result=new ArrayList<>();for(int n=0;n<model.elements.size();n++)if(model.elements.get(n).rule==(goal==1?1:4))result.add(n);
        int[] indices=new int[result.size()];for(int n=0;n<indices.length;n++)indices[n]=result.get(n);return indices;
    }
    public int rowElement(int row){
        if(screen==Screen.HOME&&blocked.isEmpty()&&row>0&&row<=uses.entries.size())return row-1;
        if(screen==Screen.OVERVIEW&&row>=0&&row<overview().entries.size())return overview().entries.get(row).element;
        if(screen==Screen.RELATION&&row>=0&&row<relationshipChoices().length)return relationshipChoices()[row];return -1;
    }
    /** Hit testing never makes overlap or transparency a prerequisite for list selection. */
    public boolean pickElementAt(int px,int py){
        if(!blocked.isEmpty()||screen!=Screen.HOME&&screen!=Screen.RELATION)return false;
        for(int n=uses.entries.size()-1;n>=0;n--){
            GameUses.Entry e=uses.entries.get(n);SpriteRegion r=region(n);
            if(px<e.x()||py<e.y()||px>=e.x()+r.width||py>=e.y()+r.height)continue;
            if(screen==Screen.HOME){selected=n;focus=n+1;return true;}
            int[] choices=relationshipChoices();for(int k=0;k<choices.length;k++)if(choices[k]==n){focus=k;return true;}
        }return false;
    }
    public String summary(){
        if(!blocked.isEmpty())return "Open All tools to edit this cartridge.";
        if(screen==Screen.OVERVIEW)return focus<overview().entries.size()?overview().entries.get(focus).description:"Return to the game field. All tools remains available there.";
        if(screen==Screen.GOAL)return GameRulesOverview.goalDescription(settings,focus<3?focus:settings.goal);
        if(screen==Screen.SETTINGS)return GameRulesOverview.goalDescription(settings,settings.goal);
        if(screen==Screen.CHOICE)return choiceField==0?"Choose how "+label(selected)+" moves during the game.":"Choose what happens to "+label(selected)+". Contact actions next ask who touches it.";
        if(screen==Screen.RELATION){int other=relatedElement();return other<0?(relationshipField==5?"Choose a switch, or keep this switch independent.":"Choose on the field or in the list. A confirms; B cancels."):
            (relationshipField==3?"When "+label(other)+" touches "+label(selected)+", "+contactAction()+".":"O toggles "+label(selected)+" and "+label(other)+" together.");}
        PlayModel model=settings!=null?settings:play;
        if(model==null||model.elements.isEmpty())return "Select an element to change its look, movement or behavior.";
        if(screen==Screen.SETTINGS)return settings.readiness();
        PlayModel.Element e=model.elements.get(selected);
        if(e.rule>=1&&e.rule<=3)return "When "+label(e.target)+" touches this, "+(e.rule==1?"collect it once":e.rule==2?"win the game":"lose the game")+".";
        if(e.rule==4)return "O toggles this"+(e.neighbor>=0?" and "+label(e.neighbor):"")+". Left/right select a switch.";
        return e.movement==0?"No behavior yet. Choose Behavior to add movement or an interaction.":"Directions move this "+(e.movement==2?"horizontally":e.movement==3?"vertically":"across the field")+".";
    }
    private String contactAction(){int rule=settings.elements.get(selected).rule;return rule==1?"collect it once":rule==2?"win the game":"lose the game";}
    public String title(){
        switch(screen){case DRAW:case DRAW_MENU:case PALETTE:return "Appearance";case PLACE:return operation==Operation.MOVE?"Move sprite":"Place sprite";
            case OVERVIEW:return "How the game works";case GOAL:return "Choose a game goal";case NAME:return "Name this element";case CHOICE:return choiceField==0?"How does it move?":"What should happen?";case RELATION:return relationshipField==3?"Choose who touches this":"Choose another switch";case ANIMATION:return "Animation: "+label(selected);case BEHAVIOR:return "Behavior: "+label(selected);case SETTINGS:return "Game rules";case SIZE:return "New sprite";case SHARED:return "Shared appearance";case DELETE:return "Remove sprite?";case ACTIONS:return label(selected);default:return "Game view";}
    }
    public String[] rows(){
        switch(screen){
            case HOME:{ArrayList<String> rows=new ArrayList<>();rows.add("+ Add sprite");if(blocked.isEmpty())for(int n=0;n<uses.entries.size();n++)rows.add(label(n));if(blocked.isEmpty())rows.add("Game rules");Collections.addAll(rows,"All tools","Undo","Redo","Projects");return rows.toArray(new String[0]);}
            case ACTIONS:return new String[]{"Edit appearance","Move on field","Duplicate","Remove","Behavior","Animation","Name","Back"};
            case OVERVIEW:{ArrayList<String> r=new ArrayList<>();for(GameRulesOverview.Entry e:overview().entries)r.add(e.row);r.add("Back to game");return r.toArray(new String[0]);}
            case GOAL:return new String[]{"Keep playing\nNo automatic win","Win by collecting\nAll items","Win with switches\nAll switches on","Cancel"};
            case NAME:return new String[]{"Keyboard","Save name","Cancel"};
            case CHOICE:return choiceField==0?new String[]{"Stay in place","D-pad movement\nAll directions","D-pad movement\nLeft / right","D-pad movement\nUp / down","Cancel"}:new String[]{"No interaction","Collect this\nWhen touched","Win the game\nWhen touched","Lose the game\nWhen touched","Toggle switch\nPress O","Cancel"};
            case ANIMATION:return animation.rows();
            case RELATION:{ArrayList<String> r=new ArrayList<>();for(int n:relationshipChoices())r.add(n<0?"No linked switch":label(n));if(r.isEmpty())r.add("Add another element first");r.add("Cancel selection");return r.toArray(new String[0]);}
            case BEHAVIOR:{PlayModel.Element e=settings.elements.get(selected);String[] labels={"Movement\n"+PlayModel.MOVEMENT[e.movement],"Speed\n"+e.speed+" px / step","On contact / press\n"+new String[]{"Nothing yet","Collect this","Win the game","Lose the game","O: toggle"}[e.rule],"Touched by\n"+label(e.target),"Starts\n"+(e.on==1?"On":"Off"),"Also toggle\n"+(e.neighbor<0?"Choose a switch":label(e.neighbor))};ArrayList<String> result=new ArrayList<>();for(int f:formFields())result.add(labels[f]);result.add("Save behavior");result.add("Discard changes");return result.toArray(new String[0]);}
            case SETTINGS:return new String[]{"Win when\n"+GameRulesOverview.goalName(settings.goal),"Background\nColor "+settings.background,"Save rules","Discard changes"};
            case SIZE:return new String[]{"Small  /  8 x 8","Medium  /  16 x 16","Wide  /  32 x 16","Large  /  32 x 32","Cancel"};
            case DRAW_MENU:return new String[]{operation==Operation.ADD?"Next: place":operation==Operation.ANIMATION?"Done: frames":"Done: save","Choose color",eraser?"Use brush":"Use eraser","Keep drawing","Discard changes"};
            case SHARED:return new String[]{"Edit all uses","Make independent","Back"};
            case DELETE:return new String[]{"Keep sprite","Remove placement"};
            default:return new String[0];
        }
    }
    /** All occupied pixels, draw references, map references and flags reserve their cells. */
    public SpriteRegion allocate(int width,int height){
        supported();boolean[] occupied=new boolean[128*64];
        for(int yy=0;yy<64;yy++)for(int xx=0;xx<128;xx++)occupied[yy*128+xx]=base.sheetPixel(xx,yy)!=0;
        for(int n=0;n<uses.entries.size();n++)reserve(occupied,region(n));
        if(play!=null)for(PlayModel.Element e:play.elements)if(e.animation!=null)for(PlayAnimation.Frame f:e.animation.frames)reserve(occupied,f.region);
        // Keep tile zero out of allocation, including empty maps where zero has special meaning.
        reserve(occupied,new SpriteRegion(0,0,8,8));
        for(int n=0;n<128;n++)if(base.flags(n)!=0)reserve(occupied,new SpriteRegion(n%16*8,n/16*8,8,8));
        P8Map map=base.map();for(int yy=0;yy<64;yy++)for(int xx=0;xx<128;xx++){
            int tile=map.tile(xx,yy);if(tile>0&&tile<128)reserve(occupied,new SpriteRegion(tile%16*8,tile/16*8,8,8));
        }
        for(int yy=0;yy+height<=64;yy+=8)for(int xx=0;xx+width<=128;xx+=8){
            boolean free=true;for(int dy=0;dy<height&&free;dy++)for(int dx=0;dx<width;dx++)if(occupied[(yy+dy)*128+xx+dx]){free=false;break;}
            if(free)return new SpriteRegion(xx,yy,width,height);
        }
        throw new IllegalArgumentException("No safe space for this size in the upper sprite sheet. Try a smaller sprite or manage resources in All tools. Existing pixels were kept.");
    }
    private static void reserve(boolean[] occupied,SpriteRegion r){for(int yy=r.y;yy<Math.min(64,r.y+r.height);yy++)for(int xx=r.x;xx<r.x+r.width;xx++)occupied[yy*128+xx]=true;}
    private static boolean overlaps(SpriteRegion a,SpriteRegion b){return a.x<b.x+b.width&&b.x<a.x+a.width&&a.y<b.y+b.height&&b.y<a.y+a.height;}
    public int sharedUses(){SpriteRegion r=region(selected);int count=0;for(int n=0;n<uses.entries.size();n++)if(overlaps(r,region(n)))count++;if(play!=null)for(PlayModel.Element e:play.elements)if(e.animation!=null)for(int n=1;n<e.animation.frames.size();n++)if(overlaps(r,e.animation.frames.get(n).region))count++;return count;}
    public boolean mapReference(){SpriteRegion r=region(selected);P8Map map=base.map();for(int yy=0;yy<64;yy++)for(int xx=0;xx<128;xx++){int t=map.tile(xx,yy);if(t!=0&&overlaps(r,new SpriteRegion(t%16*8,t/16*8,8,8)))return true;}return false;}
    private void start(Operation op){
        supported();if(op==Operation.DUPLICATE&&play!=null&&uses.entries.size()>=16)throw new IllegalArgumentException("This behavior editor currently supports 16 sprites.");SpriteRegion original=region(selected);
        if(op==Operation.APPEARANCE&&original.sharesMap())throw new IllegalArgumentException("This sprite shares pixels with map data. Use the sprite editor in All tools for its consequence review, or make an independent copy here.");
        SpriteRegion next=op==Operation.INDEPENDENT?allocate(original.width,original.height):original;
        int[] copy=new int[original.width*original.height];for(int yy=0;yy<original.height;yy++)for(int xx=0;xx<original.width;xx++)copy[yy*original.width+xx]=base.pixel(original,xx,yy);
        operation=op;target=next;pixels=copy;GameUses.Entry e=uses.entries.get(selected);x=e.x();y=e.y();cursorX=cursorY=0;focus=0;
        screen=(op==Operation.MOVE||op==Operation.DUPLICATE)?Screen.PLACE:Screen.DRAW;
    }
    public void add(int size){
        if(play!=null&&uses.entries.size()>=16)throw new IllegalArgumentException("This behavior editor currently supports 16 sprites. Existing data was kept; All tools remains available.");
        int[] widths={8,16,32,32},heights={8,16,16,32};if(size<0||size>=4)throw new IllegalArgumentException("Choose a sprite size");
        SpriteRegion next=allocate(widths[size],heights[size]);target=next;pixels=new int[target.width*target.height];operation=Operation.ADD;
        x=(128-target.width)/2;y=(128-target.height)/2;cursorX=cursorY=0;eraser=false;screen=Screen.DRAW;focus=0;
    }
    public void paint(int px,int py){if(screen!=Screen.DRAW)return;cursorX=clamp(px,0,target.width-1);cursorY=clamp(py,0,target.height-1);pixels[cursorY*target.width+cursorX]=eraser?0:color;}
    public void point(int px,int py){if(screen==Screen.PLACE){x=clamp(px,-target.width+1,127);y=clamp(py,-target.height+1,127);}else if(screen==Screen.HOME&&blocked.isEmpty()){
        for(int n=uses.entries.size()-1;n>=0;n--){GameUses.Entry e=uses.entries.get(n);SpriteRegion r=region(n);if(px>=e.x()&&py>=e.y()&&px<e.x()+r.width&&py<e.y()+r.height){selected=n;focus=n+1;return;}}
    }}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private void home(){nameDraft=null;choiceField=-1;relationshipFromChoice=false;relationshipField=-1;animation=null;settings=null;editingField=false;screen=Screen.HOME;operation=Operation.NONE;target=null;pixels=null;focus=uses.entries.isEmpty()?0:selected+1;if(overviewReturn!=null){GameRulesOverview.Kind kind=overviewReturn;int element=overviewElement;overviewReturn=null;overviewElement=-1;openOverview();focus=overview().find(kind,element);syncOverviewSelection();}}
    public Command act(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.TEST)return Command.TEST;
        if(screen==Screen.OVERVIEW)return overviewAction(a);
        if(screen==Screen.GOAL)return goalAction(a);
        if(screen==Screen.NAME)return nameAction(a);
        if(screen==Screen.CHOICE)return choiceAction(a);
        if(screen==Screen.RELATION)return relationshipAction(a);
        if(screen==Screen.ANIMATION)return animationAction(a);
        if(screen==Screen.BEHAVIOR||screen==Screen.SETTINGS)return formAction(a);
        if(screen==Screen.DRAW){
            if(a==WorkshopSession.Action.LEFT)cursorX=clamp(cursorX-1,0,target.width-1);if(a==WorkshopSession.Action.RIGHT)cursorX=clamp(cursorX+1,0,target.width-1);
            if(a==WorkshopSession.Action.UP)cursorY=clamp(cursorY-1,0,target.height-1);if(a==WorkshopSession.Action.DOWN)cursorY=clamp(cursorY+1,0,target.height-1);
            if(a==WorkshopSession.Action.CONFIRM)paint(cursorX,cursorY);
            if(a==WorkshopSession.Action.CANCEL||a==WorkshopSession.Action.MENU){screen=Screen.DRAW_MENU;focus=0;}
            if(a==WorkshopSession.Action.CONTEXT){screen=Screen.PALETTE;focus=color;}
            return Command.NONE;
        }
        if(screen==Screen.PALETTE){
            if(a==WorkshopSession.Action.LEFT)focus=(focus+15)%16;if(a==WorkshopSession.Action.RIGHT)focus=(focus+1)%16;
            if(a==WorkshopSession.Action.UP||a==WorkshopSession.Action.DOWN)focus=(focus+8)%16;
            if(a==WorkshopSession.Action.CONFIRM){color=focus;eraser=false;screen=Screen.DRAW;}if(a==WorkshopSession.Action.CANCEL)screen=Screen.DRAW;
            return Command.NONE;
        }
        if(screen==Screen.PLACE){
            if(a==WorkshopSession.Action.LEFT)point(x-1,y);if(a==WorkshopSession.Action.RIGHT)point(x+1,y);if(a==WorkshopSession.Action.UP)point(x,y-1);if(a==WorkshopSession.Action.DOWN)point(x,y+1);
            if(a==WorkshopSession.Action.CONFIRM)return Command.SAVE;
            if(a==WorkshopSession.Action.CANCEL){if(operation==Operation.ADD)screen=Screen.DRAW;else home();}return Command.NONE;
        }
        if(a==WorkshopSession.Action.UP)focus=Math.max(0,focus-1);if(a==WorkshopSession.Action.DOWN)focus=Math.min(rows().length-1,focus+1);
        if(screen==Screen.HOME&&(focus>0&&focus<=uses.entries.size())&&blocked.isEmpty())selected=focus-1;
        if(a==WorkshopSession.Action.CANCEL){if(screen==Screen.HOME)return Command.PROJECTS;if(screen==Screen.DRAW_MENU)screen=Screen.DRAW;else home();return Command.NONE;}
        if(a==WorkshopSession.Action.UNDO&&screen==Screen.HOME)return Command.UNDO;
        if(a==WorkshopSession.Action.REDO&&screen==Screen.HOME)return Command.REDO;
        if(a==WorkshopSession.Action.MENU&&screen==Screen.HOME){focus=rows().length-4;return Command.NONE;}
        if(a!=WorkshopSession.Action.CONFIRM)return Command.NONE;
        switch(screen){
            case HOME:{int count=blocked.isEmpty()?uses.entries.size():0;
                if(focus==0){supported();screen=Screen.SIZE;focus=1;}
                else if(focus<=count){selected=focus-1;screen=Screen.ACTIONS;focus=0;}
                else if(blocked.isEmpty()&&focus==count+1)openOverview();
                else return new Command[]{Command.TOOLS,Command.UNDO,Command.REDO,Command.PROJECTS}[focus-count-1-(blocked.isEmpty()?1:0)];break;}
            case ACTIONS:
                if(focus==0){if(sharedUses()>1||mapReference()||region(selected).sharesMap()){screen=Screen.SHARED;focus=0;}else start(Operation.APPEARANCE);}
                else if(focus==1)start(Operation.MOVE);else if(focus==2)start(Operation.DUPLICATE);else if(focus==3){screen=Screen.DELETE;focus=0;}else if(focus==4)configure(true);else if(focus==5)animate();else if(focus==6)rename();else home();break;
            case SIZE:if(focus==4)home();else add(focus);break;
            case DRAW_MENU:
                if(focus==0){if(operation==Operation.ADD){boolean visible=false;for(int p:pixels)visible|=p!=0;if(!visible)throw new IllegalArgumentException("Paint at least one visible pixel before placing the sprite.");screen=Screen.PLACE;}else if(operation==Operation.ANIMATION){animation.paint(pixels);endFrame();}else return Command.SAVE;}
                else if(focus==1){screen=Screen.PALETTE;focus=color;}else if(focus==2){eraser=!eraser;screen=Screen.DRAW;}else if(focus==3)screen=Screen.DRAW;else if(operation==Operation.ANIMATION){animation.cancelPaint();endFrame();}else home();break;
            case SHARED:if(focus==2)home();else start(focus==0?Operation.APPEARANCE:Operation.INDEPENDENT);break;
            case DELETE:if(focus==0)home();else return Command.SAVE;break;
            default:break;
        }return Command.NONE;
    }
    public WorkshopCartridge candidate(WorkshopCartridge current){
        if(!Arrays.equals(base.bytes(),current.bytes()))throw new IllegalArgumentException("The project changed. The saved source and this draft were kept; reopen the project before editing.");
        supported();if(operation==Operation.RENAME){GameElementNames edited=names.copy();edited.values.set(selected,GameElementNames.normalize(nameDraft.text()));return edited.apply(base);}
        if(operation==Operation.ANIMATION)return pixels==null?animation.candidate():animation.candidate(pixels);if(operation==Operation.CONFIG){PlayModel preview=relationshipPreview();validateConnections(preview);return names.apply(preview.write(base));}if(!draft()&&screen!=Screen.DELETE)return base;
        GameUses edit=new GameUses(layoutBase,0);edit.index=selected;
        if(screen==Screen.DELETE){edit.delete();return finish(edit.proposal().candidate(layoutBase));}
        WorkshopCartridge next=layoutBase;
        if(operation==Operation.ADD||operation==Operation.APPEARANCE||operation==Operation.INDEPENDENT){
            int[] sheet=new int[128*128];for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++)sheet[yy*128+xx]=base.sheetPixel(xx,yy);
            for(int yy=0;yy<target.height;yy++)for(int xx=0;xx<target.width;xx++)sheet[(target.y+yy)*128+target.x+xx]=pixels[yy*target.width+xx];
            next=layoutBase.withSheetPixels(sheet);
        }
        if(operation==Operation.APPEARANCE)return finish(next);
        if(operation==Operation.ADD)edit.addSprite(target);else if(operation==Operation.DUPLICATE)edit.duplicate();else edit.edit();
        if(operation==Operation.INDEPENDENT){
            if(edit.kind.equals("spr"))edit.values[0]=target.y/8*16+target.x/8;
            else{edit.values[0]=target.x;edit.values[1]=target.y;}
        }
        edit.values[edit.coordinateField(0)]=x;edit.values[edit.coordinateField(1)]=y;
        WorkshopCartridge code=edit.proposal().candidate(layoutBase);
        LuaDraft combined=new LuaDraft(next,0);combined.selectAll();combined.replace(code.code());return finish(combined.edit().candidate(next));
    }
    private void validateConnections(PlayModel model){
        for(int n=0;n<model.elements.size();n++){
            PlayModel.Element e=model.elements.get(n);
            if(e.rule==4&&e.neighbor>=0&&e.neighbor<model.elements.size()&&model.elements.get(e.neighbor).rule!=4)
                throw new IllegalArgumentException(label(e.neighbor)+" is linked from "+label(n)+". Change that element's Also toggle setting first.");
            if(e.animation!=null&&e.animation.mode==1&&e.movement==0)
                throw new IllegalArgumentException(label(n)+" animates when moving. Set its Animation to Loop before removing movement.");
            if(e.animation!=null&&e.animation.mode==2&&e.rule!=4)
                throw new IllegalArgumentException(label(n)+" animates when on. Set its Animation to Loop before removing Toggle.");
        }
    }
    private WorkshopCartridge finish(WorkshopCartridge result){
        WorkshopCartridge next=play==null?result:play.withLayout(result,operation,selected,screen==Screen.DELETE).write(result);
        GameElementNames edited=names.copy();if(screen==Screen.DELETE)edited.values.remove(selected);else if(operation==Operation.ADD)edited.values.add("");else if(operation==Operation.DUPLICATE)edited.values.add(edited.values.get(selected));
        return edited.apply(next);
    }
    public void rename(){supported();nameDraft=new NameEditor(names.values.get(selected),GameElementNames.LIMIT,true);operation=Operation.RENAME;screen=Screen.NAME;focus=0;}
    private Command nameAction(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){home();return Command.NONE;}
        if(focus==0){
            if(a==WorkshopSession.Action.LEFT)nameDraft.move(-1,0);if(a==WorkshopSession.Action.RIGHT)nameDraft.move(1,0);
            if(a==WorkshopSession.Action.UP)nameDraft.move(0,-1);if(a==WorkshopSession.Action.DOWN){if(nameDraft.key/10==4)focus=1;else nameDraft.move(0,1);}
            if(a==WorkshopSession.Action.CONFIRM)nameDraft.type(nameDraft.key);
        }else{
            if(a==WorkshopSession.Action.UP)focus--;if(a==WorkshopSession.Action.DOWN)focus=Math.min(2,focus+1);
            if(a==WorkshopSession.Action.CONFIRM){if(focus==1)return Command.SAVE;home();}
        }
        if(a==WorkshopSession.Action.CONTEXT)nameDraft.erase();if(a==WorkshopSession.Action.UNDO)nameDraft.uppercase=!nameDraft.uppercase;
        if(a==WorkshopSession.Action.NEXT||a==WorkshopSession.Action.PREVIOUS)nameDraft.language();if(a==WorkshopSession.Action.MENU)nameDraft.replaceAll=!nameDraft.replaceAll;
        return Command.NONE;
    }
    private GameRulesOverview overview(){if(overview==null)overview=new GameRulesOverview(PlayModel.fromStatic(base),labels);return overview;}
    public void openOverview(){supported();overview();screen=Screen.OVERVIEW;focus=0;operation=Operation.NONE;}
    private void syncOverviewSelection(){int element=rowElement(focus);if(element>=0)selected=element;}
    /** Preserve the semantic location even if an edited rule disappears or rows shift. */
    public void returnAfterSave(GameCanvas previous){
        if(previous.overviewReturn==null)return;
        openOverview();focus=overview().find(previous.overviewReturn,previous.overviewElement);syncOverviewSelection();
    }
    public void returnAfterHistory(GameCanvas previous){
        if(previous.screen!=Screen.OVERVIEW||!blocked.isEmpty())return;
        openOverview();
        if(previous.focus<previous.overview().entries.size()){GameRulesOverview.Entry entry=previous.overview().entries.get(previous.focus);focus=overview().find(entry.kind,entry.element);}
        else focus=rows().length-1;
        syncOverviewSelection();
    }
    private Command overviewAction(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){home();focus=uses.entries.size()+1;return Command.NONE;}
        if(a==WorkshopSession.Action.UNDO)return Command.UNDO;if(a==WorkshopSession.Action.REDO)return Command.REDO;
        if(a==WorkshopSession.Action.UP||a==WorkshopSession.Action.LEFT)focus=Math.max(0,focus-1);
        if(a==WorkshopSession.Action.DOWN||a==WorkshopSession.Action.RIGHT)focus=Math.min(rows().length-1,focus+1);
        syncOverviewSelection();
        if(a==WorkshopSession.Action.CONFIRM){
            if(focus==overview().entries.size()){home();focus=uses.entries.size()+1;return Command.NONE;}
            GameRulesOverview.Entry entry=overview().entries.get(focus);overviewReturn=entry.kind;overviewElement=entry.element;
            if(entry.kind==GameRulesOverview.Kind.ANIMATION)animate();
            else if(entry.element>=0){configure(true);focus=fieldFocus(entry.kind==GameRulesOverview.Kind.INTERACTION?2:0);}
            else{configure(false);focus=entry.kind==GameRulesOverview.Kind.GOAL?0:1;}
        }return Command.NONE;
    }
    private Command goalAction(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){screen=Screen.SETTINGS;focus=0;return Command.NONE;}
        if(a==WorkshopSession.Action.UP||a==WorkshopSession.Action.LEFT)focus=Math.max(0,focus-1);
        if(a==WorkshopSession.Action.DOWN||a==WorkshopSession.Action.RIGHT)focus=Math.min(3,focus+1);
        if(a==WorkshopSession.Action.CONFIRM){if(focus<3)settings.goal=focus;screen=Screen.SETTINGS;focus=0;}
        return Command.NONE;
    }
    private void beginChoice(int field){choiceField=field;choiceBefore=field==0?settings.elements.get(selected).movement:settings.elements.get(selected).rule;focus=choiceBefore;screen=Screen.CHOICE;}
    private int fieldFocus(int field){int[] fields=formFields();for(int n=0;n<fields.length;n++)if(fields[n]==field)return n;return 0;}
    private void endChoice(){screen=Screen.BEHAVIOR;focus=fieldFocus(choiceField);choiceField=-1;}
    private Command choiceAction(WorkshopSession.Action a){
        if(a==WorkshopSession.Action.CANCEL){endChoice();return Command.NONE;}
        if(a==WorkshopSession.Action.UP||a==WorkshopSession.Action.LEFT)focus=Math.max(0,focus-1);
        if(a==WorkshopSession.Action.DOWN||a==WorkshopSession.Action.RIGHT)focus=Math.min(rows().length-1,focus+1);
        if(a==WorkshopSession.Action.CONFIRM){
            if(focus==rows().length-1){endChoice();return Command.NONE;}
            PlayModel.Element e=settings.elements.get(selected);
            if(choiceField==0){e.movement=focus;endChoice();}
            else{e.rule=focus;if(focus>=1&&focus<=3){focus=fieldFocus(3);beginRelationship(3);relationshipFromChoice=true;}else endChoice();}
        }return Command.NONE;
    }
    public void animate(){supported();animation=new GameAnimation(base,selected);operation=Operation.ANIMATION;screen=Screen.ANIMATION;focus=0;editingField=false;}
    private void endFrame(){target=null;pixels=null;screen=Screen.ANIMATION;focus=3;animation.focus=focus;editingField=false;}
    private Command animationAction(WorkshopSession.Action a){
        animation.focus=focus;animation.editing=editingField;
        if(editingField){
            if(a==WorkshopSession.Action.CONFIRM)editingField=false;
            else if(a==WorkshopSession.Action.CANCEL){animation.value(animation.before);editingField=false;}
            else if(a==WorkshopSession.Action.LEFT||a==WorkshopSession.Action.RIGHT||a==WorkshopSession.Action.UP||a==WorkshopSession.Action.DOWN)animation.step(a==WorkshopSession.Action.LEFT||a==WorkshopSession.Action.DOWN?-1:1);
        }else if(a==WorkshopSession.Action.CANCEL){home();return Command.NONE;}
        else if(a==WorkshopSession.Action.UP)focus=Math.max(0,focus-1);
        else if(a==WorkshopSession.Action.DOWN)focus=Math.min(rows().length-1,focus+1);
        else if(a==WorkshopSession.Action.CONFIRM){
            if(focus<3){animation.before=animation.value();editingField=true;}
            else if(focus==3){target=animation.beginPaint();pixels=new int[target.width*target.height];for(int yy=0;yy<target.height;yy++)for(int xx=0;xx<target.width;xx++)pixels[yy*target.width+xx]=animation.resources.pixel(target,xx,yy);cursorX=cursorY=0;screen=Screen.DRAW;focus=0;}
            else if(focus==4)animation.duplicate();else if(focus==5)animation.reorder(-1);else if(focus==6)animation.reorder(1);else if(focus==7)animation.remove();
            else if(focus==8)return Command.SAVE;else{home();return Command.NONE;}
        }
        animation.focus=screen==Screen.ANIMATION?focus:3;animation.editing=editingField;return Command.NONE;
    }
    public void configure(boolean element){supported();settings=PlayModel.fromStatic(base);operation=Operation.CONFIG;screen=element?Screen.BEHAVIOR:Screen.SETTINGS;focus=0;editingField=false;}
    public int[] formFields(){if(screen==Screen.SETTINGS||screen==Screen.GOAL)return new int[]{0,1};PlayModel.Element e=settings.elements.get(selected);ArrayList<Integer> f=new ArrayList<>();f.add(0);if(e.movement!=0)f.add(1);f.add(2);if(e.rule>=1&&e.rule<=3)f.add(3);if(e.rule==4){f.add(4);f.add(5);}int[] result=new int[f.size()];for(int n=0;n<result.length;n++)result[n]=f.get(n);return result;}
    private int field(){int focus=formFields()[this.focus];if(screen==Screen.SETTINGS)return focus==0?settings.goal:settings.background;PlayModel.Element e=settings.elements.get(selected);return new int[]{e.movement,e.speed,e.rule,e.target,e.on,e.neighbor+1}[focus];}
    private void field(int v){int focus=formFields()[this.focus];if(screen==Screen.SETTINGS){if(focus==0)settings.goal=v;else settings.background=v;return;}PlayModel.Element e=settings.elements.get(selected);switch(focus){case 0:e.movement=v;break;case 1:e.speed=v;break;case 2:e.rule=v;break;case 3:e.target=v;break;case 4:e.on=v;break;case 5:e.neighbor=v-1;break;}}
    private Command formAction(WorkshopSession.Action a){
        int fields=formFields().length;
        if(editingField){
            if(a==WorkshopSession.Action.CONFIRM)editingField=false;
            else if(a==WorkshopSession.Action.CANCEL){field(fieldBefore);editingField=false;}
            else if(a==WorkshopSession.Action.LEFT||a==WorkshopSession.Action.RIGHT||a==WorkshopSession.Action.UP||a==WorkshopSession.Action.DOWN){
                int delta=a==WorkshopSession.Action.LEFT||a==WorkshopSession.Action.DOWN?-1:1;
                int active=formFields()[focus];int max=screen==Screen.SETTINGS?(active==0?2:15):new int[]{3,4,4,settings.elements.size()-1,1,settings.elements.size()}[active];
                int min=screen==Screen.BEHAVIOR&&active==1?1:0;field(clamp(field()+delta,min,max));
                // Adding speed before the rule row must keep the active field selected.
                for(int n=0;n<formFields().length;n++)if(formFields()[n]==active){focus=n;break;}
            }return Command.NONE;
        }
        if(a==WorkshopSession.Action.CANCEL){home();return Command.NONE;}
        if(a==WorkshopSession.Action.UP)focus=Math.max(0,focus-1);
        if(a==WorkshopSession.Action.DOWN)focus=Math.min(fields+1,focus+1);
        if(a==WorkshopSession.Action.CONFIRM){if(focus<fields){int active=formFields()[focus];if(screen==Screen.SETTINGS&&active==0){screen=Screen.GOAL;focus=settings.goal;}else if(screen==Screen.BEHAVIOR&&(active==0||active==2))beginChoice(active);else if(screen==Screen.BEHAVIOR&&(active==3||active==5))beginRelationship(active);else{fieldBefore=field();editingField=true;}}else if(focus==fields)return Command.SAVE;else home();}
        return Command.NONE;
    }
    public String guidance(){
        if(animation!=null)return animation.guidance();
        if(settings==null)return play==null?"Draw, place, then add Behavior.":play.readiness();
        if(screen==Screen.SETTINGS)return settings.readiness();
        PlayModel.Element e=settings.elements.get(selected);
        int active=focus<formFields().length?formFields()[focus]:-1;
        if(active==0||active==1)return "Directions move this sprite. Higher speed is faster. It stops at the screen edge.";
        if(active==3||e.rule>=1&&e.rule<=3)return "When the chosen sprite overlaps this sprite: "+(e.rule==1?"collect this sprite once.":e.rule==2?"win the game.":e.rule==3?"lose the game.":"choose a touch rule first.");
        if(active==5)return "Also toggle another switch with this one. Set that sprite to Toggle first.";
        return "Toggle: left/right select a switch, O changes it once per press. X restarts the game.";
    }
    public int[] image(){
        WorkshopCartridge cart=animation!=null?animation.model.layout(animation.resources):operation==Operation.CONFIG?settings.layout(base):draft()?candidate(base):base;PlayModel model=PlayModel.read(cart);if(model!=null)cart=model.layout(cart);GameUses list=new GameUses(cart,0);int[] image=new int[128*128];Arrays.fill(image,settings==null?background:settings.background);
        if(!blocked.isEmpty())return image;
        int placement=0;for(GameUses.Entry e:list.entries){SpriteRegion r=e.kind().equals("spr")?new SpriteRegion(e.values[0]%16*8,e.values[0]/16*8,8,8):new SpriteRegion(e.values[0],e.values[1],e.values[2],e.values[3]);
            if(animation!=null&&placement==selected)r=animation.current().region;placement++;
            for(int yy=0;yy<r.height;yy++)for(int xx=0;xx<r.width;xx++){int dx=e.x()+xx,dy=e.y()+yy,p=cart.pixel(r,xx,yy);if(p!=0&&dx>=0&&dx<128&&dy>=0&&dy<128)image[dy*128+dx]=p;}
        }
        return image;
    }
    public byte[] encode(){try{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeInt(6);byte[] source=base.bytes();o.writeInt(source.length);o.write(source);
        o.writeUTF(screen.name());o.writeUTF(operation.name());for(int v:new int[]{selected,focus,color,cursorX,cursorY,x,y})o.writeInt(v);o.writeBoolean(eraser);
        o.writeBoolean(target!=null);if(target!=null){for(int v:new int[]{target.x,target.y,target.width,target.height})o.writeInt(v);for(int p:pixels)o.writeByte(p);}o.writeBoolean(settings!=null);if(settings!=null){settings.encode(o);o.writeBoolean(editingField);o.writeInt(fieldBefore);settings.encodeAnimations(o);}o.writeBoolean(animation!=null);if(animation!=null){animation.focus=screen==Screen.ANIMATION?focus:3;animation.editing=editingField;animation.encode(o);}o.writeInt(relationshipField);o.writeInt(relationshipReturnFocus);o.writeBoolean(nameDraft!=null);if(nameDraft!=null){o.writeUTF(nameDraft.text());o.writeInt(nameDraft.key);o.writeBoolean(nameDraft.latin);o.writeBoolean(nameDraft.uppercase);o.writeBoolean(nameDraft.replaceAll);}o.writeInt(choiceField);o.writeInt(choiceBefore);o.writeBoolean(relationshipFromChoice);o.writeUTF(overviewReturn==null?"":overviewReturn.name());o.writeInt(overviewElement);return bytes.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static GameCanvas restore(byte[] bytes,WorkshopCartridge cart){try{
        if(bytes.length>3*1024*1024)throw new IOException();DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes));int version=i.readInt();if(version!=1&&version!=2&&version!=3&&version!=4&&version!=5&&version!=6)throw new IOException();int length=i.readInt();if(length<0||length>2*1024*1024||length>i.available())throw new IOException();byte[] source=new byte[length];i.readFully(source);
        if(!Arrays.equals(source,cart.bytes()))throw new IOException("The project changed; its source was preserved.");
        GameCanvas g=new GameCanvas(cart);g.screen=Screen.valueOf(i.readUTF());g.operation=Operation.valueOf(i.readUTF());g.selected=i.readInt();g.focus=i.readInt();g.color=i.readInt();g.cursorX=i.readInt();g.cursorY=i.readInt();g.x=i.readInt();g.y=i.readInt();g.eraser=i.readBoolean();
        if(i.readBoolean()){g.target=new SpriteRegion(i.readInt(),i.readInt(),i.readInt(),i.readInt());g.pixels=new int[g.target.width*g.target.height];for(int n=0;n<g.pixels.length;n++){g.pixels[n]=i.readUnsignedByte();if(g.pixels[n]>15)throw new IOException();}}
        if(version==1&&g.blocked.isEmpty()){if(g.screen==Screen.HOME&&g.focus>g.uses.entries.size())g.focus++;if(g.screen==Screen.ACTIONS&&g.focus==4)g.focus=5;}
        if(version>=2&&i.readBoolean()){g.settings=PlayModel.decode(i);g.editingField=i.readBoolean();g.fieldBefore=i.readInt();if(version>=3)g.settings.decodeAnimations(i);}
        if(version>=3&&i.readBoolean()){g.animation=GameAnimation.decode(i,cart);g.editingField=g.animation.editing;if(g.animation.element!=g.selected)throw new IOException();}
        if(version>=4){g.relationshipField=i.readInt();g.relationshipReturnFocus=i.readInt();}
        if(version>=5){if(i.readBoolean()){g.nameDraft=new NameEditor(i.readUTF(),GameElementNames.LIMIT,true);g.nameDraft.key=i.readInt();g.nameDraft.latin=i.readBoolean();g.nameDraft.uppercase=i.readBoolean();g.nameDraft.replaceAll=i.readBoolean();GameElementNames.normalize(g.nameDraft.text());if(g.nameDraft.key<0||g.nameDraft.key>=g.nameDraft.count())throw new IOException();}g.choiceField=i.readInt();g.choiceBefore=i.readInt();g.relationshipFromChoice=i.readBoolean();}
        if(version>=6){String origin=i.readUTF();g.overviewReturn=origin.isEmpty()?null:GameRulesOverview.Kind.valueOf(origin);g.overviewElement=i.readInt();}
        if(version<3&&g.screen==Screen.ACTIONS&&g.focus==5)g.focus=6;
        if(version<5&&g.screen==Screen.ACTIONS&&g.focus==6)g.focus=7;
        if(i.available()!=0||g.color<0||g.color>15||g.selected<0||g.selected>=Math.max(1,g.uses.entries.size())||g.focus<0||g.focus>Math.max(16,g.rows().length)||g.x<-32768||g.x>32767||g.y<-32768||g.y>32767)throw new IOException();
        boolean editing=g.screen==Screen.DRAW||g.screen==Screen.DRAW_MENU||g.screen==Screen.PALETTE||g.screen==Screen.PLACE;
        if((g.operation!=Operation.NONE&&g.operation!=Operation.CONFIG&&g.operation!=Operation.ANIMATION&&g.operation!=Operation.RENAME)!=editing&&g.operation!=Operation.ANIMATION||editing!=(g.target!=null))throw new IOException();
        boolean form=g.screen==Screen.BEHAVIOR||g.screen==Screen.SETTINGS||g.screen==Screen.RELATION||g.screen==Screen.CHOICE||g.screen==Screen.GOAL;
        if(form!=(g.settings!=null)||form!=(g.operation==Operation.CONFIG)||g.editingField&&!form&&g.screen!=Screen.ANIMATION)throw new IOException();
        if((g.animation!=null)!=(g.operation==Operation.ANIMATION)||g.screen==Screen.ANIMATION&&g.animation==null||g.animation!=null&&!(g.screen==Screen.ANIMATION||editing))throw new IOException();
        if((g.nameDraft!=null)!=(g.screen==Screen.NAME)||(g.operation==Operation.RENAME)!=(g.screen==Screen.NAME))throw new IOException();
        if((g.choiceField!=-1)!=(g.screen==Screen.CHOICE||g.relationshipFromChoice)||g.relationshipFromChoice&&(g.screen!=Screen.RELATION||g.choiceField!=2||g.relationshipField!=3))throw new IOException();
        if(g.choiceField!=-1&&(g.choiceField!=0&&g.choiceField!=2||g.choiceBefore<0||g.choiceBefore>(g.choiceField==0?3:4)||g.editingField))throw new IOException();
        if(g.screen==Screen.CHOICE&&(g.choiceField==0?g.settings.elements.get(g.selected).movement:g.settings.elements.get(g.selected).rule)!=g.choiceBefore)throw new IOException();
        if((g.relationshipField!=-1)!=(g.screen==Screen.RELATION))throw new IOException();
        if(g.screen==Screen.RELATION){
            if(g.relationshipField!=3&&g.relationshipField!=5||g.editingField)throw new IOException();
            PlayModel.Element e=g.settings.elements.get(g.selected);if(g.relationshipField==3&&(e.rule<1||e.rule>3)||g.relationshipField==5&&e.rule!=4)throw new IOException();
            if(g.relationshipReturnFocus<0||g.relationshipReturnFocus>=g.formFields().length||g.formFields()[g.relationshipReturnFocus]!=g.relationshipField)throw new IOException();
        }
        if(form){PlayModel original=PlayModel.fromStatic(cart);if(g.settings.elements.size()!=original.elements.size()||!g.settings.prefix.equals(original.prefix)||!g.settings.newline.equals(original.newline))throw new IOException();
            PlayModel checked=g.settings.copy();for(int n=0;n<checked.elements.size();n++){PlayModel.Element a=checked.elements.get(n),b=original.elements.get(n);if(a.sx!=b.sx||a.sy!=b.sy||a.w!=b.w||a.h!=b.h||a.x!=b.x||a.y!=b.y)throw new IOException();a.rule=0;a.neighbor=-1;if(a.animation!=null)a.animation.mode=0;}checked.validate();
            if(g.editingField){if(g.focus>=g.formFields().length)throw new IOException();int active=g.formFields()[g.focus];int min=g.screen==Screen.BEHAVIOR&&active==1?1:0,max=g.screen==Screen.SETTINGS?(active==0?2:15):new int[]{3,4,4,16,1,16}[active];if(g.fieldBefore<min||g.fieldBefore>max)throw new IOException();}
        }
        if(g.screen==Screen.GOAL&&g.editingField)throw new IOException();
        if(g.screen==Screen.OVERVIEW&&(g.draft()||g.overviewReturn!=null))throw new IOException();
        if(g.overviewReturn!=null){
            if(g.operation!=Operation.CONFIG&&g.operation!=Operation.ANIMATION)throw new IOException();
            if(g.overviewElement< -1||g.overviewElement>=g.uses.entries.size())throw new IOException();
            boolean global=g.overviewReturn==GameRulesOverview.Kind.GOAL||g.overviewReturn==GameRulesOverview.Kind.BACKGROUND;
            if(global!=(g.overviewElement==-1)||!global&&g.selected!=g.overviewElement)throw new IOException();
            if((g.overviewReturn==GameRulesOverview.Kind.ANIMATION)!=(g.operation==Operation.ANIMATION)||global!=(g.screen==Screen.SETTINGS||g.screen==Screen.GOAL))throw new IOException();
        }else if(g.overviewElement!=-1)throw new IOException();
        if(g.screen==Screen.PALETTE&&g.focus>15||g.rows().length>0&&g.focus>=g.rows().length)throw new IOException();
        if(g.screen!=Screen.HOME&&!g.blocked.isEmpty())throw new IOException();
        if((g.screen==Screen.ACTIONS||g.screen==Screen.SHARED||g.screen==Screen.DELETE||g.draft()&&g.operation!=Operation.ADD&&g.screen!=Screen.SETTINGS&&g.screen!=Screen.GOAL)&&g.uses.entries.isEmpty())throw new IOException();
        if(editing){
            g.supported();if(g.cursorX<0||g.cursorX>=g.target.width||g.cursorY<0||g.cursorY>=g.target.height)throw new IOException();
            SpriteRegion expected=g.animation!=null?g.animation.current().region:g.operation==Operation.ADD||g.operation==Operation.INDEPENDENT?g.allocate(g.target.width,g.target.height):g.region(g.selected);
            if(expected.x!=g.target.x||expected.y!=g.target.y||expected.width!=g.target.width||expected.height!=g.target.height)throw new IOException();
            if(g.operation==Operation.APPEARANCE&&g.target.sharesMap())throw new IOException();g.candidate(cart);
        }return g;
    }catch(Exception e){throw new IllegalArgumentException("Game view draft could not be restored. Saved source was preserved.",e);}}
}
