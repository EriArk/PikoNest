package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** A recoverable composition transaction, including nested shared SFX edits. */
public final class MusicEditor {
    public enum Page { BANK, PATTERN, PART, PICK, BIND, COPY, CLEAR, REVIEW }
    public final WorkshopCartridge base;
    public final boolean fromTools;
    public final PlayModel model;
    public Page page=Page.BANK;
    public int focus,index,channel,musicStart,musicParts;
    public SoundEditor sound;
    private final TreeMap<Integer,P8Music.Pattern> patterns=new TreeMap<>();
    private final TreeMap<Integer,P8Sfx.Sound> sounds=new TreeMap<>();
    private final ArrayDeque<String> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final String original;
    public MusicEditor(WorkshopCartridge base,boolean fromTools){
        this.base=base;this.fromTools=fromTools;base.music();PlayModel known=null;try{known=PlayModel.fromStatic(base);}catch(IllegalArgumentException ignored){}model=known;musicStart=model==null?-1:model.musicStart;musicParts=model==null?3:model.musicParts;original=state();
    }
    public P8Music.Pattern pattern(int at){return patterns.containsKey(at)?patterns.get(at).copy():base.music().get(at);}
    public boolean draft(){return sound!=null&&sound.draft()||!original.equals(state());}
    private String state(){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(musicStart);o.writeInt(musicParts);o.writeInt(patterns.size());for(Map.Entry<Integer,P8Music.Pattern> e:patterns.entrySet()){o.writeInt(e.getKey());o.writeUTF(e.getValue().line());}o.writeInt(sounds.size());for(Map.Entry<Integer,P8Sfx.Sound> e:sounds.entrySet()){o.writeInt(e.getKey());o.writeUTF(e.getValue().line());}return Base64.getEncoder().encodeToString(b.toByteArray());}catch(IOException ex){throw new IllegalStateException(ex);}}
    private void state(String encoded)throws IOException{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));int start=in.readInt(),parts=in.readInt();if(start< -1||start>63||parts<3||parts>4||model==null&&(start!=-1||parts!=3))throw new IOException();
        TreeMap<Integer,P8Music.Pattern> p=new TreeMap<>();TreeMap<Integer,P8Sfx.Sound> s=new TreeMap<>();
        int count=in.readInt();if(count<0||count>64)throw new IOException();for(int n=0;n<count;n++){int at=in.readInt();P8Music.Pattern v=P8Music.Pattern.parse(in.readUTF());base.withPattern(at,v);if(p.put(at,v)!=null)throw new IOException();}
        count=in.readInt();if(count<0||count>64)throw new IOException();for(int n=0;n<count;n++){int at=in.readInt();P8Sfx.Sound v=P8Sfx.Sound.parse(in.readUTF());base.withSound(at,v);if(s.put(at,v)!=null)throw new IOException();}
        if(in.available()!=0)throw new IOException();musicStart=start;musicParts=parts;patterns.clear();patterns.putAll(p);sounds.clear();sounds.putAll(s);
    }
    private void record(String before){if(!before.equals(state())){undo.push(before);while(undo.size()>32)undo.removeLast();redo.clear();}}
    private void set(int at,P8Music.Pattern p){base.withPattern(at,p);if(p.line().equals(base.music().get(at).line()))patterns.remove(at);else patterns.put(at,p.copy());}
    private void known(){if(model==null)throw new IllegalArgumentException("Edit music resources here; automatic game wiring needs a recognized Game view project. Unknown Lua stays unchanged.");}
    public String title(){if(sound!=null)return sound.page==SoundEditor.Page.REVIEW?"Keep phrase in draft?":sound.title();switch(page){case BANK:return "Music";case PART:return "Pattern "+index+" / part "+(channel+1);case PICK:return "Choose a phrase";case BIND:return "Music in the game";case COPY:return "Copy to pattern "+(index+1)+"?";case CLEAR:return "Clear pattern "+index+"?";case REVIEW:return "Save composition?";default:return "Pattern "+index;}}
    public String flow(P8Music.Pattern p){return (p.flags&4)!=0?"Stop after this":(p.flags&2)!=0?"Loop back":(p.flags&1)!=0?"Loop starts here":"Continue to next";}
    public String[] rows(){if(sound!=null){String[] rows=sound.rows();if(sound.page==SoundEditor.Page.REVIEW)rows[0]="Keep phrase";return rows;}List<String> r=new ArrayList<>();P8Music.Pattern p=pattern(index);
        switch(page){
            case BANK:for(int n=0;n<64;n++){P8Music.Pattern v=pattern(n);r.add("Pattern "+n+(musicStart==n?" / game start":"")+"\n"+(v.silent()?"Silent":flow(v)));}r.add("Review and save");r.add("Back");break;
            case PATTERN:for(int n=0;n<4;n++)r.add("Part "+(n+1)+"\n"+(p.active(n)?"SFX "+p.sound(n):"Muted / SFX "+p.sound(n)));Collections.addAll(r,"Loop start\n"+((p.flags&1)!=0?"Yes":"No"),"Loop back\n"+((p.flags&2)!=0?"Yes":"No"),"Stop after this\n"+((p.flags&4)!=0?"Yes":"No"),"Copy to next pattern","Clear this pattern","Use in the game","Back to sequence");break;
            case PART:Collections.addAll(r,"Choose existing SFX","Edit shared phrase","+ Create phrase","Edit a copy",p.active(channel)?"Mute this part":"Unmute this part","Back to pattern");break;
            case PICK:for(int n=0;n<64;n++)r.add("SFX "+n);r.add("Cancel");break;
            case BIND:Collections.addAll(r,"Use 3 music parts\nPart 4 muted","Use all 4 parts\nPart 4 can cut out","Remove game music","Back");break;
            case COPY:Collections.addAll(r,"Replace next pattern","Cancel");break;
            case CLEAR:Collections.addAll(r,"Clear / keep SFX data","Cancel");break;
            case REVIEW:Collections.addAll(r,"Save composition","Keep editing","Discard changes");break;
        }return r.toArray(new String[0]);
    }
    public String summary(){if(sound!=null)return sound.summary();switch(page){
        case BANK:return "Patterns play in order. X listens from the selected pattern.";
        case PART:return "Phrases share SFX data. Copy before editing to keep other uses.";
        case BIND:return "Starts with the game; stops at win/loss. Choose how music shares audio with effects.";
        case REVIEW:return patterns.size()+" patterns; "+sounds.size()+" SFX changed. "+(model==null?"Unknown Lua kept.":musicStart<0?"No game music.":"Game starts at "+musicStart+" / "+musicParts+" parts.")+" Shared uses change too.";
        case COPY:return "Copies channel references and flags. Existing next-pattern data is replaced; SFX notes stay shared.";
        case CLEAR:return "All parts and flow flags are cleared. SFX notes are kept. Pattern numbers do not shift.";
        case PICK:return "Select a shared phrase. X listens to this SFX before choosing.";
        default:return "Four simultaneous parts. Loop flags control the sequence; Stop takes priority.";
    }}
    private void editPhrase(int kind){
        WorkshopCartridge current=raw(false);SoundEditor next=new SoundEditor(current,false);next.focus=kind==2?0:pattern(index).sound(channel)+2;next.act(Action.CONFIRM);
        if(kind==3){next.focus=34;next.act(Action.CONFIRM);next.focus=0;}sound=next;focus=0;
    }
    public GameCanvas.Command act(Action a){
        if(sound!=null){sound.focus=focus;GameCanvas.Command cmd=sound.act(a);focus=sound.focus;if(cmd==GameCanvas.Command.SAVE){String before=state();P8Sfx.Sound notes=sound.data.copy();if(notes.line().equals(base.sfx().get(sound.slot).line()))sounds.remove(sound.slot);else sounds.put(sound.slot,notes);P8Music.Pattern p=pattern(index);p.channels[channel]=sound.slot;set(index,p);record(before);sound=null;focus=1;}else if(!sound.draft()){sound=null;focus=1;}return cmd==GameCanvas.Command.SAVE||cmd==GameCanvas.Command.CLOSE_SOUND?GameCanvas.Command.NONE:cmd;}
        if(a==Action.TEST)return GameCanvas.Command.TEST;
        if(a==Action.CONTEXT){if(page==Page.BANK){if(focus>=64)throw new IllegalArgumentException("Select a pattern to listen.");index=focus;}return GameCanvas.Command.LISTEN;}
        if(a==Action.UNDO||a==Action.REDO){ArrayDeque<String> from=a==Action.UNDO?undo:redo,to=a==Action.UNDO?redo:undo;if(!from.isEmpty()){String prior=state();try{state(from.peek());}catch(IOException ex){throw new IllegalArgumentException("Music history could not be restored.",ex);}from.pop();to.push(prior);}else if(!draft()&&page==Page.BANK)return a==Action.UNDO?GameCanvas.Command.UNDO:GameCanvas.Command.REDO;return GameCanvas.Command.NONE;}
        if(a==Action.CANCEL){switch(page){case BANK:if(draft()){page=Page.REVIEW;focus=1;}else return fromTools?GameCanvas.Command.TOOLS:GameCanvas.Command.CLOSE_MUSIC;break;case PATTERN:page=Page.BANK;focus=index;break;case PART:page=Page.PATTERN;focus=channel;break;case PICK:page=Page.PART;focus=0;break;case REVIEW:page=Page.BANK;focus=index;break;default:page=Page.PATTERN;focus=9;break;}return GameCanvas.Command.NONE;}
        if(a==Action.MENU){page=Page.REVIEW;focus=0;return GameCanvas.Command.NONE;}
        int delta=a==Action.NEXT?8:a==Action.PREVIOUS?-8:a==Action.DOWN||a==Action.RIGHT?1:a==Action.UP||a==Action.LEFT?-1:0;focus=Math.max(0,Math.min(rows().length-1,focus+delta));if(a!=Action.CONFIRM)return GameCanvas.Command.NONE;
        String before=state();P8Music.Pattern p=pattern(index);
        switch(page){
            case BANK:if(focus<64){index=focus;pattern(index).validate();page=Page.PATTERN;focus=0;}else if(focus==64||draft()){page=Page.REVIEW;focus=0;}else return fromTools?GameCanvas.Command.TOOLS:GameCanvas.Command.CLOSE_MUSIC;break;
            case PATTERN:if(focus<4){channel=focus;page=Page.PART;focus=0;}else if(focus<7){p.flags^=1<<(focus-4);set(index,p);}else if(focus==7){if(index==63)throw new IllegalArgumentException("Pattern 63 is the last slot. Existing data kept.");pattern(index+1).validate();page=Page.COPY;focus=1;}else if(focus==8){page=Page.CLEAR;focus=1;}else if(focus==9){known();page=Page.BIND;focus=0;}else{page=Page.BANK;focus=index;}break;
            case PART:if(focus==0){page=Page.PICK;focus=p.sound(channel);}else if(focus<=3)editPhrase(focus);else if(focus==4){p.mute(channel,p.active(channel));set(index,p);}else{page=Page.PATTERN;focus=channel;}break;
            case PICK:if(focus<64){p.channels[channel]=focus;set(index,p);}page=Page.PART;focus=0;break;
            case BIND:if(focus==0||focus==1){known();WorkshopCartridge current=raw(false);List<Integer> track=current.music().track(index);if(focus==0)for(int at:track){P8Music.Pattern v=pattern(at);v.mute(3,true);set(at,v);}musicStart=index;musicParts=focus==0?3:4;page=Page.REVIEW;focus=0;}else if(focus==2){known();musicStart=-1;musicParts=3;page=Page.REVIEW;focus=0;}else{page=Page.PATTERN;focus=9;}break;
            case COPY:if(focus==0){set(index+1,p);index++;}page=Page.PATTERN;focus=7;break;
            case CLEAR:if(focus==0)set(index,new P8Music.Pattern());page=Page.PATTERN;focus=8;break;
            case REVIEW:if(focus==0)return GameCanvas.Command.SAVE;if(focus==2){patterns.clear();sounds.clear();musicStart=model==null?-1:model.musicStart;musicParts=model==null?3:model.musicParts;undo.clear();redo.clear();page=Page.BANK;focus=index;return GameCanvas.Command.NONE;}page=Page.BANK;focus=index;break;
        }record(before);return GameCanvas.Command.NONE;
    }
    private WorkshopCartridge raw(boolean nested){
        WorkshopCartridge result=base;for(Map.Entry<Integer,P8Music.Pattern> e:patterns.entrySet())result=result.withPattern(e.getKey(),e.getValue());for(Map.Entry<Integer,P8Sfx.Sound> e:sounds.entrySet())result=result.withSound(e.getKey(),e.getValue());
        if(nested&&sound!=null&&sound.data!=null){result=result.withSound(sound.slot,sound.data);P8Music.Pattern p=pattern(index);p.channels[channel]=sound.slot;result=result.withPattern(index,p);}
        if(model!=null&&(model.musicStart!=musicStart||model.musicParts!=musicParts)){PlayModel next=model.copy();next.musicStart=musicStart;next.musicParts=musicParts;result=next.write(result);}return result;
    }
    public WorkshopCartridge candidate(){WorkshopCartridge result=raw(true);if(musicStart>=0&&musicParts==3)result.music().requireThreeParts(musicStart);return result;}
    public WorkshopCartridge preview(){
        if(sound!=null)return sound.preview();if(page==Page.PICK){if(focus>=64)throw new IllegalArgumentException("Select a phrase first.");SoundEditor audition=new SoundEditor(raw(false),false);audition.slot=focus;return audition.preview();}
        WorkshopCartridge result=raw(false);result.music().track(index);return PlayModel.code(result,"function _init()\n music(-1)\n music("+index+",0,15)\n last4=btn(4)\n last5=btn(5)\nend\nfunction _update()\n if btn(4) and not last4 then music(-1) end\n if btn(5) and not last5 then music("+index+",0,15) end\n last4=btn(4)\n last5=btn(5)\nend\nfunction _draw()\n cls(1)\n print(\"music from "+index+"\",8,32,14)\n print(\"o: stop   x: replay\",8,52,7)\n print(\"exit to return\",8,72,6)\nend\n");
    }
    void encode(DataOutputStream out)throws IOException{out.writeBoolean(fromTools);out.writeUTF(page.name());out.writeInt(focus);out.writeInt(index);out.writeInt(channel);out.writeUTF(state());for(ArrayDeque<String> stack:Arrays.asList(undo,redo)){out.writeInt(stack.size());for(String s:stack)out.writeUTF(s);}out.writeBoolean(sound!=null);if(sound!=null){sound.focus=focus;sound.encode(out);}}
    static MusicEditor decode(DataInputStream in,WorkshopCartridge base)throws IOException{
        MusicEditor e=new MusicEditor(base,in.readBoolean());e.page=Page.valueOf(in.readUTF());e.focus=in.readInt();e.index=in.readInt();e.channel=in.readInt();e.state(in.readUTF());String current=e.state();
        for(ArrayDeque<String> stack:Arrays.asList(e.undo,e.redo)){int count=in.readInt();if(count<0||count>32)throw new IOException();for(int n=0;n<count;n++){String v=in.readUTF();e.state(v);stack.add(v);}}e.state(current);
        if(e.index<0||e.index>63||e.channel<0||e.channel>3||e.page==Page.BIND&&e.model==null)throw new IOException();
        if(in.readBoolean()){e.sound=SoundEditor.decode(in,e.raw(false));if(e.page!=Page.PART||!e.sound.draft()||e.sound.binding||e.sound.event!=-1||e.sound.fromTools||e.focus!=e.sound.focus)throw new IOException();}
        if(e.focus<0||e.focus>=e.rows().length)throw new IOException();return e;
    }
}
