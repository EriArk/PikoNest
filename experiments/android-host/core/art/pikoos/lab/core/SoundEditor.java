package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** One SFX resource and optional ordinary-Lua event assignment, committed together. */
public final class SoundEditor {
    public enum Page { BANK, EVENTS, PICK, EDIT, NOTE, OPTIONS, REVIEW }
    public final WorkshopCartridge base;
    public final P8Sfx bank;
    public final PlayModel model;
    public final boolean fromTools;
    public Page page=Page.BANK;
    public int focus,slot=8,note,event=-1,returnElement=-1;
    public P8Sfx.Sound data;
    public boolean editing,binding;
    private String before="";
    private final ArrayDeque<String> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final GameCanvas game;
    public SoundEditor(WorkshopCartridge cart,boolean fromTools){base=cart;this.fromTools=fromTools;bank=cart.sfx();game=new GameCanvas(cart);PlayModel known=null;if(game.blocked.isEmpty())try{known=PlayModel.fromStatic(cart);}catch(IllegalArgumentException ignored){}model=known;}
    public boolean draft(){return data!=null||binding;}
    public String title(){switch(page){case BANK:return "Sound effects";case EVENTS:return "Sounds in the game";case PICK:return eventName(event);case NOTE:return "SFX "+slot+" / note "+(note+1);case OPTIONS:return "Speed and loop";case REVIEW:return "Save this sound?";default:return "SFX "+slot+" / sequence";}}
    public String eventName(int id){if(id<3)return new String[]{"When the game starts","When you win","When you lose"}[id];return game.label(id-3)+" / interaction";}
    public void openEvent(int id){if(model==null||id<0||id>=model.elements.size()+3)throw new IllegalArgumentException("Choose an event in a recognized Game view project.");event=id;page=Page.PICK;focus=model.sound(event)<0?0:model.sound(event)+2;}
    private List<Integer> events(){List<Integer> result=new ArrayList<>(Arrays.asList(0,1,2));if(model!=null)for(int n=0;n<model.elements.size();n++)if(model.elements.get(n).rule!=0||model.elements.get(n).sound>=0)result.add(n+3);return result;}
    public String[] rows(){
        List<String> r=new ArrayList<>();
        switch(page){
            case BANK:r.add("+ Create sound");r.add("Sounds in the game");for(int n=0;n<64;n++){P8Sfx.Sound s=bank.get(n);r.add("SFX "+n+"\n"+(s.blank()?"Empty":s.editable()?s.audible()+" sounding notes":"Preserved / advanced data"));}r.add("Back");break;
            case EVENTS:for(int id:events())r.add(eventName(id)+"\n"+(model.sound(id)<0?"No sound":"SFX "+model.sound(id)));r.add("Back");break;
            case PICK:r.add("No sound");r.add("+ Create for this event");for(int n=0;n<64;n++)r.add("SFX "+n+"\n"+bank.get(n).audible()+" sounding notes");r.add("Cancel");break;
            case EDIT:for(int n=0;n<32;n++)r.add("Note "+(n+1)+"\n"+(data.notes[n][2]==0?"Rest":P8Sfx.pitch(data.notes[n][0])+" / vol "+data.notes[n][2]));Collections.addAll(r,"Speed and loop","Listen in PICO-8","Make independent copy","Review and save","Discard changes");break;
            case NOTE:{int[] v=data.notes[note];Collections.addAll(r,"Sound\n"+(v[2]==0?"Rest / add note":"On / make rest"),"Pitch\n"+P8Sfx.pitch(v[0]),"Volume\n"+v[2],"Instrument\n"+P8Sfx.instrument(v[1]),"Effect\n"+P8Sfx.EFFECTS[v[3]],"Copy to next note","Clear this note","Back to sequence");break;}
            case OPTIONS:Collections.addAll(r,"Speed\n"+data.speed+" ticks / note","Loop start / length\n"+data.loopStart,"Loop end\n"+data.loopEnd,"Transpose up","Transpose down","Back to sequence");break;
            case REVIEW:r.add("Save sound");r.add("Keep editing");break;
        }return r.toArray(new String[0]);
    }
    public String summary(){
        if(page==Page.BANK)return "Choose or create a sound. X listens; Start tests the game.";
        if(page==Page.EVENTS||page==Page.PICK)return "Events share channel 3. A later sound replaces the earlier one.";
        if(page==Page.REVIEW){if(data==null)return eventName(event)+": "+(slot<0?"remove sound.":"use SFX "+slot+".");int music=bank.musicUses(slot),custom=bank.instrumentUses(slot),uses=0;if(model!=null)for(int id=0;id<model.elements.size()+3;id++)if(model.sound(id)==slot)uses++;
            return "SFX "+slot+": edits all uses. Events: "+(model==null?"unknown":uses)+"; music: "+(music<0?"unknown":music)+"; instruments: "+custom+". "+(binding?"Event binding included.":"Copy first to keep original.");}

        if(page==Page.OPTIONS)return data.loopEnd>data.loopStart?"Loop end is exclusive. O stops playback in the listening cart.":data.loopEnd==0&&data.loopStart>0?"With end 0, start sets the playback length. Other notes stay stored.":"No loop. Lower speed is faster. All 32 notes remain stored.";
        if(page==Page.NOTE)return "A edits a field. B reverts its value. Moving the cursor never changes a note.";
        return "A opens a note. Menu jumps to options. Y / R2 undo / redo note edits. Start tests the game.";
    }
    private int allocate(){
        if(model==null)throw new IllegalArgumentException("Unknown Lua may use any SFX. Choose an explicit slot to edit; automatic allocation needs a recognized Game view project.");
        boolean[] used=bank.referencedByResources();for(int id=0;id<model.elements.size()+3;id++)if(model.sound(id)>=0)used[model.sound(id)]=true;
        if(data!=null)used[slot]=true;
        for(int n=8;n<64;n++)if(!used[n]&&bank.get(n).blank())return n;
        throw new IllegalArgumentException("No safely unused SFX slot. Existing sounds and references were kept.");
    }
    private void begin(int selected){P8Sfx.Sound next=bank.get(selected);if(next.blank()&&next.speed==0)next.speed=16;next.validate();slot=selected;data=next;binding=event>=0;page=Page.EDIT;focus=note=0;undo.clear();redo.clear();}
    private void discard(){data=null;binding=false;editing=false;undo.clear();redo.clear();page=event>=0?Page.PICK:Page.BANK;focus=event>=0?0:slot+2;}
    private void record(String previous){if(!previous.equals(data.line())){undo.push(previous);while(undo.size()>128)undo.removeLast();redo.clear();}}
    private int value(){if(page==Page.OPTIONS)return focus==0?data.speed:focus==1?data.loopStart:data.loopEnd;return data.notes[note][focus==1?0:focus==2?2:focus==3?1:3];}
    private void value(int v){if(page==Page.OPTIONS){if(focus==0)data.speed=v;else if(focus==1)data.loopStart=v;else data.loopEnd=v;}else data.notes[note][focus==1?0:focus==2?2:focus==3?1:3]=v;}
    public GameCanvas.Command act(Action a){
        if(a==Action.TEST)return GameCanvas.Command.TEST;
        if(a==Action.CONTEXT){if(page==Page.BANK||page==Page.PICK){if(focus<2||focus>65)throw new IllegalArgumentException("Select an SFX slot to listen.");slot=focus-2;}if(page==Page.EVENTS){if(focus>=events().size())throw new IllegalArgumentException("Choose an event first.");slot=model.sound(events().get(focus));}if(slot<0)throw new IllegalArgumentException("No sound assigned here yet.");return GameCanvas.Command.LISTEN;}
        if(editing){if(a==Action.CANCEL){data=P8Sfx.Sound.parse(before);editing=false;}else if(a==Action.CONFIRM){record(before);editing=false;}else if(a==Action.LEFT||a==Action.DOWN||a==Action.RIGHT||a==Action.UP){int max=page==Page.OPTIONS?(focus==0?255:32):(focus==1?63:focus==3?15:7),min=page==Page.OPTIONS&&focus==0?1:0;value(Math.max(min,Math.min(max,value()+(a==Action.LEFT||a==Action.DOWN?-1:1))));}return GameCanvas.Command.NONE;}
        if(a==Action.UNDO||a==Action.REDO){if(!draft())return page==Page.BANK||page==Page.EVENTS?(a==Action.UNDO?GameCanvas.Command.UNDO:GameCanvas.Command.REDO):GameCanvas.Command.NONE;if(data!=null){ArrayDeque<String> from=a==Action.UNDO?undo:redo,to=a==Action.UNDO?redo:undo;if(!from.isEmpty()){to.push(data.line());data=P8Sfx.Sound.parse(from.pop());}}return GameCanvas.Command.NONE;}
        if(a==Action.CANCEL){switch(page){case BANK:return fromTools?GameCanvas.Command.TOOLS:GameCanvas.Command.CLOSE_SOUND;case EVENTS:page=Page.BANK;focus=1;break;case PICK:if(returnElement>=0)return GameCanvas.Command.CLOSE_SOUND;page=Page.EVENTS;focus=events().indexOf(event);event=-1;break;case EDIT:discard();break;case NOTE:case OPTIONS:page=Page.EDIT;focus=note;break;case REVIEW:page=data!=null?Page.EDIT:Page.PICK;focus=data!=null?35:0;if(data==null)binding=false;break;}return GameCanvas.Command.NONE;}
        if(a==Action.MENU&&page==Page.EDIT){focus=32;return GameCanvas.Command.NONE;}
        if(a==Action.PREVIOUS||a==Action.NEXT){focus=Math.max(0,Math.min(rows().length-1,focus+(a==Action.NEXT?8:-8)));}
        if(a==Action.UP||a==Action.LEFT)focus=Math.max(0,focus-1);if(a==Action.DOWN||a==Action.RIGHT)focus=Math.min(rows().length-1,focus+1);
        if(page==Page.EDIT&&focus<32)note=focus;
        if(a!=Action.CONFIRM)return GameCanvas.Command.NONE;
        switch(page){
            case BANK:if(focus==0){event=-1;begin(allocate());}else if(focus==1){if(model==null)throw new IllegalArgumentException("Sound editing works here. Automatic event wiring needs a recognized Game view project; unfamiliar Lua stays unchanged.");page=Page.EVENTS;focus=0;}else if(focus==66)return fromTools?GameCanvas.Command.TOOLS:GameCanvas.Command.CLOSE_SOUND;else{event=-1;begin(focus-2);}break;
            case EVENTS:if(focus==events().size()){page=Page.BANK;focus=1;}else{event=events().get(focus);page=Page.PICK;focus=model.sound(event)<0?0:model.sound(event)+2;}break;
            case PICK:if(focus==66){if(returnElement>=0)return GameCanvas.Command.CLOSE_SOUND;page=Page.EVENTS;focus=events().indexOf(event);event=-1;}else if(focus==1)begin(allocate());else{slot=focus==0?-1:focus-2;binding=true;page=Page.REVIEW;focus=0;}break;
            case EDIT:if(focus<32){note=focus;page=Page.NOTE;focus=0;}else if(focus==32){page=Page.OPTIONS;focus=0;}else if(focus==33)return GameCanvas.Command.LISTEN;else if(focus==34){slot=allocate();binding=event>=0;}else if(focus==35){page=Page.REVIEW;focus=0;}else discard();break;
            case NOTE:if(focus==7){page=Page.EDIT;focus=note;}else if(focus>=1&&focus<=4){before=data.line();editing=true;}else{String previous=data.line();if(focus==0){int[] v=data.notes[note];if(v[2]>0)v[2]=0;else{if(Arrays.equals(v,new int[4]))v[0]=24;v[2]=5;}}else if(focus==5){if(note==31)throw new IllegalArgumentException("This is the last of 32 notes.");data.notes[note+1]=data.notes[note].clone();note++;}else data.notes[note][2]=0;record(previous);}break;
            case OPTIONS:if(focus<3){before=data.line();editing=true;}else if(focus==5){page=Page.EDIT;focus=note;}else{int delta=focus==3?1:-1;for(int[] n:data.notes)if(n[2]>0&&(n[0]+delta<0||n[0]+delta>63))throw new IllegalArgumentException("Transposing would exceed PICO-8's pitch range. Notes kept.");String previous=data.line();for(int[] n:data.notes)if(n[2]>0)n[0]+=delta;record(previous);}break;
            case REVIEW:if(focus==0)return GameCanvas.Command.SAVE;page=data!=null?Page.EDIT:Page.PICK;focus=data!=null?35:0;if(data==null)binding=false;break;
        }return GameCanvas.Command.NONE;
    }
    public WorkshopCartridge candidate(){WorkshopCartridge result=data==null?base:base.withSound(slot,data);if(binding){PlayModel changed=model.copy();changed.sound(event,slot);result=game.names.apply(changed.write(result));}return result;}
    public WorkshopCartridge preview(){if(slot<0)throw new IllegalArgumentException("Choose a sound to listen to.");WorkshopCartridge result=candidate();return PlayModel.code(result,"function _init()\n sfx(-1,3)\n sfx("+slot+",3)\n last4=btn(4)\n last5=btn(5)\nend\nfunction _update()\n if btn(4) and not last4 then sfx(-1,3) end\n if btn(5) and not last5 then sfx("+slot+",3) end\n last4=btn(4)\n last5=btn(5)\nend\nfunction _draw()\n cls(1)\n print(\"sfx "+slot+"\",8,32,14)\n print(\"o: stop   x: replay\",8,52,7)\n print(\"exit to return\",8,72,6)\nend\n");}
    public void afterSave(SoundEditor old){returnElement=old.returnElement;slot=Math.max(0,old.slot);page=old.event>=0?Page.EVENTS:Page.BANK;focus=old.event>=0?Math.max(0,events().indexOf(old.event)):slot+2;}
    void encode(DataOutputStream out)throws IOException{out.writeBoolean(fromTools);out.writeUTF(page.name());for(int v:new int[]{focus,slot,note,event,returnElement})out.writeInt(v);out.writeBoolean(editing);out.writeBoolean(binding);out.writeUTF(before);out.writeUTF(data==null?"":data.line());for(ArrayDeque<String> stack:Arrays.asList(undo,redo)){out.writeInt(stack.size());for(String line:stack)out.writeUTF(line);}}
    static SoundEditor decode(DataInputStream in,WorkshopCartridge base)throws IOException{
        SoundEditor e=new SoundEditor(base,in.readBoolean());e.page=Page.valueOf(in.readUTF());e.focus=in.readInt();e.slot=in.readInt();e.note=in.readInt();e.event=in.readInt();e.returnElement=in.readInt();e.editing=in.readBoolean();e.binding=in.readBoolean();e.before=in.readUTF();String line=in.readUTF();e.data=line.isEmpty()?null:P8Sfx.Sound.parse(line);
        for(ArrayDeque<String> stack:Arrays.asList(e.undo,e.redo)){int size=in.readInt();if(size<0||size>128)throw new IOException();for(int n=0;n<size;n++){String v=in.readUTF();P8Sfx.Sound.parse(v).validate();stack.add(v);}}
        if(e.returnElement< -1||e.returnElement>=0&&(e.model==null||e.returnElement>=e.model.elements.size()||e.event!=e.returnElement+3||e.fromTools))throw new IOException();
        if(e.slot< -1||e.slot>63||e.note<0||e.note>31||e.event< -1||e.event>=3+(e.model==null?0:e.model.elements.size())||e.event>=0&&e.model==null)throw new IOException();
        boolean hasData=e.page==Page.EDIT||e.page==Page.NOTE||e.page==Page.OPTIONS||e.page==Page.REVIEW&&e.data!=null;
        if(hasData!=(e.data!=null)||e.data!=null&&e.slot<0||e.binding&&(e.event<0||e.page!=Page.REVIEW&&e.data==null)||e.data!=null&&(e.binding!=(e.event>=0))||e.page==Page.REVIEW&&!e.draft())throw new IOException();
        if(e.data!=null)e.data.validate();if(e.editing){if(e.page!=Page.NOTE&&e.page!=Page.OPTIONS||e.page==Page.NOTE&&(e.focus<1||e.focus>4)||e.page==Page.OPTIONS&&e.focus>2)throw new IOException();P8Sfx.Sound.parse(e.before).validate();}
        if(e.page==Page.EVENTS&&e.model==null||e.page==Page.PICK&&e.event<0||e.focus<0||e.focus>=e.rows().length)throw new IOException();return e;
    }
}
