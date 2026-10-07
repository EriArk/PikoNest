package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Forms over bounded ordinary callbacks. The cartridge, not this draft, owns the game. */
public final class GameRules {
    public enum Page { LIST, MENU, FORM, REVIEW }
    public static final String[] EVENTS={"Every update","Button pressed (repeats)","Button held"};
    public static final String[] BUTTONS={"Left","Right","Up","Down","O","X"};
    public static final String[] COMPARES={"==","~=","<","<=",">",">="};
    public static final String[] ACTIONS={"Add to value","Set value","Reset starting values"};
    private static final String NAME="[a-zA-Z_][a-zA-Z_0-9]*", NUM="-?[0-9]+";
    private static final Pattern CONDITION=Pattern.compile("(?:true|(btnp|btn)\\(([0-5])\\))(?: and \\(("+NAME+")(==|~=|<=|>=|<|>)("+NUM+")\\))?");
    public static final class Entry {
        public String type,name="",guard="";
        public int value,event,button=4,compare=5,threshold=5,action,x=16,y=16,color=7;
        public String target="";
        int start,end,header=-1,actionStart=-1,actionEnd=-1;
        LuaCall call;
        Entry(String type){this.type=type;}
        Entry copy(){Entry e=new Entry(type);e.name=name;e.guard=guard;e.value=value;e.event=event;e.button=button;e.compare=compare;e.threshold=threshold;e.action=action;e.target=target;e.x=x;e.y=y;e.color=color;e.start=start;e.end=end;e.header=header;e.actionStart=actionStart;e.actionEnd=actionEnd;e.call=call;return e;}
    }
    public final WorkshopCartridge base;
    public final String source;
    public final List<Entry> entries=new ArrayList<>();
    public final List<String> states=new ArrayList<>();
    public String blocked="";
    public Page page=Page.LIST;
    public int index,menu,field,move;
    public boolean creating,deleting;
    public Entry draft;
    private Entry baseline;
    private final String nl;
    private String[] rows,mask;
    private int[] offsets;
    private final Map<String,int[]> callbacks=new HashMap<>();
    public GameRules(WorkshopCartridge cart){
        base=cart;source=new LuaDraft(cart,0).text();nl=source.contains("\r\n")?"\r\n":source.contains("\r")?"\r":"\n";
        try{scan();}catch(IllegalArgumentException e){entries.clear();states.clear();blocked=e.getMessage();}
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("These callbacks are outside the rules editor's supported forms. Your Lua is preserved. Blank projects and simple numeric state/rules are supported.");}
    private void available(){if(!blocked.isEmpty())throw new IllegalArgumentException(blocked);}
    private void scan(){
        LuaContext context=new LuaContext(source);String masked=context.masked();LuaBranches blocks=new LuaBranches(source,0);
        if(!blocks.warning.isEmpty()||!context.allowsLine(source.length())||Pattern.compile("(?m)^\\s*#include\\b|\\b(?:_ENV|_G|_update60)\\b").matcher(masked).find())throw unsupported();
        rows=source.split("\\r\\n|\\r|\\n",-1);mask=masked.split("\\r\\n|\\r|\\n",-1);offsets=new int[rows.length+1];
        int at=0;for(int n=0;n<rows.length;n++){offsets[n]=at;at+=rows[n].length();if(at<source.length()&&source.charAt(at)=='\r')at++;if(at<source.length()&&source.charAt(at)=='\n')at++;}offsets[rows.length]=at;
        for(int n=0;n<rows.length;n++)for(String cb:new String[]{"_init","_update","_draw"}){
            String row=mask[n].trim();
            if(row.matches("function\\s+"+cb+"\\s*\\(\\s*\\)")){
                if(callbacks.containsKey(cb)||nested(blocks,n)||!blocks.blockEnds.containsKey(n))throw unsupported();
                callbacks.put(cb,new int[]{n,blocks.blockEnds.get(n)-1});
            }else if(Pattern.compile("\\b"+cb+"\\s*=|\\bfunction\\s+"+cb+"\\b|\\blocal\\s+"+cb+"\\b").matcher(row).find())throw unsupported();
        }
        int[] init=callbacks.get("_init");
        if(init!=null)for(int n=init[0]+1;n<init[1];n++){
            if(mask[n].trim().isEmpty())continue;
            LuaCall call=LuaCall.parse(source,offsets[n],offsets[n]+rows[n].length());
            if(!call.name.equals("rule:assign=")||!call.form.value(1).matches(NUM))throw unsupported();
            Entry e=new Entry("state");e.name=call.form.value(0);e.value=number(call.form.value(1));e.call=call;e.start=offsets[n];e.end=offsets[n+1];
            if(states.contains(e.name)||Pattern.compile("\\blocal\\s+(?:function\\s+)?"+Pattern.quote(e.name)+"\\b|\\bfor\\s+"+Pattern.quote(e.name)+"\\b").matcher(masked).find())throw unsupported();
            states.add(e.name);entries.add(e);
        }
        int[] update=callbacks.get("_update");
        Set<String> globals=new HashSet<>();for(LuaSymbols.Entry symbol:new LuaSymbols(source).project(false))globals.add(symbol.name);
        if(!globals.containsAll(states))throw unsupported();
        for(int n=0;n<mask.length;n++)if(!nested(blocks,n)&&Pattern.compile("\\breturn\\b").matcher(mask[n]).find())throw unsupported();
        if(update!=null)for(int n=update[0]+1;n<update[1];n++){
            String row=mask[n].trim();if(row.isEmpty())continue;
            Integer end=blocks.blockEnds.get(n);
            if(end==null||end!=n+3||!row.startsWith("if ")||!row.endsWith(" then"))throw unsupported();
            Matcher m=CONDITION.matcher(row.substring(3,row.length()-5));if(!m.matches())throw unsupported();
            Entry e=new Entry("rule");e.event=m.group(1)==null?0:m.group(1).equals("btnp")?1:2;e.button=m.group(2)==null?4:Integer.parseInt(m.group(2));
            if(m.group(3)!=null){e.guard=m.group(3);e.compare=Arrays.asList(COMPARES).indexOf(m.group(4));e.threshold=number(m.group(5));}
            if(mask[n+1].trim().equals("_init()"))e.action=2;
            else{LuaCall call=LuaCall.parse(source,offsets[n+1],offsets[n+1]+rows[n+1].length());
                if(!call.name.equals("rule:assign=")&&!call.name.equals("rule:assign+="))throw unsupported();
                e.action=call.name.endsWith("+=")?0:1;e.target=call.form.value(0);e.value=number(call.form.value(1));}
            e.start=offsets[n];e.end=offsets[end];e.header=n;e.actionStart=offsets[n+1];e.actionEnd=e.actionStart+rows[n+1].length();
            validate(e);entries.add(e);n=end-1;
        }
        // Readouts can coexist with all supported sprite/map/animation/background uses.
        int[] draw=callbacks.get("_draw");
        if(draw!=null){GameUses uses=new GameUses(base,0);if(!uses.blocked.isEmpty())throw unsupported();
            for(int n=draw[0]+1;n<draw[1];n++){
                if(!mask[n].trim().startsWith("print("))continue;
                LuaCall call=LuaCall.parse(source,offsets[n],offsets[n]+rows[n].length());String name=call.form.value(0);
                if(!states.contains(name))continue;
                Entry e=new Entry("readout");e.name=name;e.x=number(call.form.value(1));e.y=number(call.form.value(2));e.color=number(call.form.value(3));e.call=call;e.start=offsets[n];e.end=offsets[n+1];validate(e);entries.add(e);
            }
        }
        for(String api:new String[]{"btn","btnp","print"})if(new LuaSymbols(source).shadows(api))throw unsupported();
    }
    private static boolean nested(LuaBranches b,int line){for(Map.Entry<Integer,Integer> e:b.blockEnds.entrySet())if(e.getKey()<line&&line<e.getValue())return true;return false;}
    private static int number(String s){try{int n=Integer.parseInt(s);if(!s.matches(NUM)||n<-32768||n>32767)throw unsupported();return n;}catch(NumberFormatException e){throw unsupported();}}
    public Entry current(){return entries.isEmpty()?null:entries.get(Math.max(0,Math.min(index,entries.size()-1)));}
    public boolean editing(){return baseline!=null;}
    public void beginField(){if(draft.type.equals("state")&&field==0&&!creating)throw new IllegalArgumentException("Renaming linked values is not available in this form yet. The name and all references are preserved.");baseline=draft.copy();}
    public void finishField(){baseline=null;}
    public void revertField(){if(baseline!=null){draft=baseline;baseline=null;}}
    public void add(String type){
        available();if(!type.equals("state")&&states.isEmpty())throw new IllegalArgumentException("Add a starting value first, then connect a rule or show it in the game.");
        if(!Arrays.asList("state","rule","readout").contains(type))throw unsupported();
        draft=new Entry(type);draft.value=type.equals("rule")?1:0;
        if(type.equals("state")){int n=1;do{draft.name="value"+n++;}while(containsName(draft.name,source));}
        else{draft.name=states.get(0);draft.target=states.get(0);draft.event=1;}
        creating=true;deleting=false;move=0;field=0;baseline=null;page=Page.FORM;
    }
    public void edit(){available();if(current()==null)return;draft=current().copy();creating=deleting=false;move=0;field=0;baseline=null;page=Page.FORM;}
    public void duplicate(){if(current()==null)return;if(current().type.equals("state"))throw new IllegalArgumentException("Add a new starting value instead; its name must be unique.");edit();creating=true;}
    public void remove(){edit();if(draft!=null){deleting=true;proposal();page=Page.REVIEW;}}
    public void reorder(int direction){edit();if(draft==null||!draft.type.equals("rule"))throw new IllegalArgumentException("Choose a rule to change its execution order.");move=direction;proposal();page=Page.REVIEW;}
    public void review(){finishField();proposal();page=Page.REVIEW;}
    public void back(){if(editing()){revertField();return;}if(page==Page.REVIEW&&!deleting&&move==0){page=Page.FORM;return;}draft=null;baseline=null;creating=deleting=false;move=0;page=Page.LIST;}
    public int rows(){return draft.type.equals("state")?3:draft.type.equals("readout")?5:9;}
    private String choose(String current,List<String> values,int delta){int i=values.indexOf(current);return values.get(Math.max(0,Math.min(values.size()-1,i+(delta<0?-1:1))));}
    public void change(int delta){
        Entry e=draft;if(e==null)return;
        if(e.type.equals("state")){
            if(field==0&&creating){List<String> names=new ArrayList<>(Arrays.asList("score","phase","x","y","timer"));for(int n=1;n<=states.size()+2;n++)names.add("value"+n);names.removeIf(name->containsName(name,source));if(!names.isEmpty())e.name=choose(e.name,names,delta);}
            if(field==1)e.value=clamp(e.value+delta,-32768,32767);
        }else if(e.type.equals("readout")){
            if(field==0)e.name=choose(e.name,states,delta);if(field==1)e.x=clamp(e.x+delta,-32768,32767);if(field==2)e.y=clamp(e.y+delta,-32768,32767);if(field==3)e.color=clamp(e.color+delta,0,15);
        }else{
            if(field==0)e.event=clamp(e.event+Integer.signum(delta),0,2);if(field==1)e.button=clamp(e.button+Integer.signum(delta),0,5);
            if(field==2){List<String> guards=new ArrayList<>();guards.add("");guards.addAll(states);e.guard=choose(e.guard,guards,delta);}
            if(field==3)e.compare=clamp(e.compare+Integer.signum(delta),0,5);if(field==4)e.threshold=clamp(e.threshold+delta,-32768,32767);
            if(field==5)e.action=clamp(e.action+Integer.signum(delta),0,2);if(field==6)e.target=choose(e.target,states,delta);if(field==7)e.value=clamp(e.value+delta,-32768,32767);
        }
    }
    private static int clamp(int n,int lo,int hi){return Math.max(lo,Math.min(hi,n));}
    public String label(int n){Entry e=draft;if(n==rows()-1)return "Preview changes";
        if(e.type.equals("state"))return n==0?"Name: "+e.name+(creating?"":" (linked)"):"Starting value: "+e.value;
        if(e.type.equals("readout"))return new String[]{"Value: "+e.name,"X: "+e.x,"Y: "+e.y,"Color: "+e.color}[n];
        return new String[]{"When: "+EVENTS[e.event],"Button: "+(e.event==0?"Not used":BUTTONS[e.button]),"Only if: "+(e.guard.isEmpty()?"Always":e.guard),"Compare: "+(e.guard.isEmpty()?"Not used":COMPARES[e.compare]),"With number: "+(e.guard.isEmpty()?"Not used":e.threshold),"Action: "+ACTIONS[e.action],"Value: "+(e.action==2?"All starting values":e.target),"Amount: "+(e.action==2?"Initial values":e.value)}[n];
    }
    public String title(Entry e){return e.type.equals("state")?e.name+" starts at "+e.value:e.type.equals("readout")?"Show "+e.name:"When "+(e.event==0?"updating":BUTTONS[e.button]+(e.event==1?" pressed":" held"));}
    public String detail(Entry e){return e.type.equals("state")?"Number · initialized at game start":e.type.equals("readout")?"X "+e.x+"  Y "+e.y+" · current camera":(e.guard.isEmpty()?"Always":e.guard+" "+COMPARES[e.compare]+" "+e.threshold)+" → "+(e.action==2?"reset values":e.target+(e.action==0?" += ":" = ")+e.value);}
    public String help(){return draft.type.equals("state")?"A variable keeps a number between updates. Reset restores its starting value.":draft.type.equals("readout")?"Shows the live value after existing draws, using the current camera. Pixels stay intact.":"Rules run from top to bottom. Pressed uses PICO-8 btnp, including its normal repeat.";}
    private static boolean containsName(String name,String text){return Pattern.compile("(?<![A-Za-z_0-9])"+Pattern.quote(name)+"(?![A-Za-z_0-9])").matcher(new LuaContext(text).masked()).find();}
    private void validate(Entry e){
        number(""+e.value);number(""+e.threshold);number(""+e.x);number(""+e.y);
        if(!Arrays.asList("state","rule","readout").contains(e.type)||e.event<0||e.event>2||e.button<0||e.button>5||e.action<0||e.action>2||e.compare<0||e.compare>5||e.color<0||e.color>15)throw unsupported();
        if(e.type.equals("state")){form(7,e.name,""+e.value);if(!e.name.matches(NAME)||e.name.startsWith("_"))throw new IllegalArgumentException("Choose a simple variable name.");}
        else if(e.type.equals("readout")){if(!states.contains(e.name))throw new IllegalArgumentException("Choose an existing starting value.");}
        else if((!e.guard.isEmpty()&&!states.contains(e.guard))||(e.action!=2&&!states.contains(e.target))||(e.action==2&&states.isEmpty()))throw new IllegalArgumentException("This rule needs an existing starting value.");
    }
    private static LuaInsert form(int item,String... values){LuaInsert f=new LuaInsert();f.choose(item);for(int n=0;n<values.length;n++)f.set(n,values[n]);return f;}
    private String condition(Entry e){return (e.event==0?"true":(e.event==1?"btnp":"btn")+"("+e.button+")")+(e.guard.isEmpty()?"":" and ("+e.guard+COMPARES[e.compare]+e.threshold+")");}
    private String action(Entry e){return e.action==2?form(16,"_init").code().trim():form(e.action==0?8:7,e.target,""+e.value).code().trim();}
    private String code(Entry e,String indent){
        if(e.type.equals("state"))return indent+form(7,e.name,""+e.value).code().trim()+nl;
        if(e.type.equals("readout"))return indent+form(11,e.name,""+e.x,""+e.y,""+e.color).code().trim()+nl;
        return indent+"if "+condition(e)+" then"+nl+indent+" "+action(e)+nl+indent+"end"+nl;
    }
    private String indent(int at){int end=at;while(end<source.length()&&(source.charAt(end)==' '||source.charAt(end)=='\t'))end++;return source.substring(at,end);}
    public CartEdit proposal(){
        available();if(draft==null)throw new IllegalArgumentException("Choose a value or rule first.");validate(draft);String result;
        Entry original=current();
        if(deleting){
            String rest=source.substring(0,original.start)+source.substring(original.end);
            if(draft.type.equals("state")&&containsName(draft.name,rest))throw new IllegalArgumentException("This value is still used. Remove or change its rules/readouts first; references were preserved.");
            if(draft.type.equals("rule")){String raw=source.substring(original.start,original.end);if(!new LuaContext(raw).masked().replaceAll("\\s","").equals(raw.replaceAll("\\s","")))throw new IllegalArgumentException("This rule has comments. Keep it or edit it in Lua so those notes are preserved.");}
            else if(original.call.original.contains("--"))throw new IllegalArgumentException("This line has a comment. Its source is preserved.");
            result=rest;
        }else if(move!=0){
            int next=index+move;if(Math.abs(move)!=1||next<0||next>=entries.size()||!entries.get(next).type.equals("rule"))throw new IllegalArgumentException("There is no adjacent rule in that direction.");
            Entry a=entries.get(Math.min(index,next)),b=entries.get(Math.max(index,next));String gap=source.substring(a.end,b.start);
            if(!gap.trim().isEmpty())throw new IllegalArgumentException("A comment separates these rules. Their order was preserved.");
            result=source.substring(0,a.start)+source.substring(b.start,b.end)+gap+source.substring(a.start,a.end)+source.substring(b.end);
        }else if(creating){
            if(draft.type.equals("state")&&containsName(draft.name,source))throw new IllegalArgumentException("That name already appears in this cartridge. Choose a new name.");
            String cb=draft.type.equals("state")?"_init":draft.type.equals("rule")?"_update":"_draw";int[] range=callbacks.get(cb);
            if(range==null){if(cb.equals("_draw")&&new LuaSymbols(source).shadows("cls"))throw unsupported();
                result=source+(source.isEmpty()||source.endsWith("\n")||source.endsWith("\r")?"":nl)+"function "+cb+"()"+nl+(cb.equals("_draw")?" cls(1)"+nl:"")+code(draft," ")+"end"+nl;
            }else{int at=offsets[range[1]];result=source.substring(0,at)+code(draft," ")+source.substring(at);}
        }else if(draft.type.equals("rule")){
            LuaCall header=LuaCall.parse(source,original.start,original.start+rows[original.header].length());
            String changed=header.preview(form(4,condition(draft)));
            String raw=source.substring(original.actionStart,original.actionEnd),masked=new LuaContext(raw).masked();int tail=masked.length();while(tail>0&&Character.isWhitespace(masked.charAt(tail-1)))tail--;
            String body=action(draft).equals(action(original))?raw:indent(original.actionStart)+action(draft)+raw.substring(tail);
            result=source.substring(0,original.start)+changed+source.substring(original.start+rows[original.header].length(),original.actionStart)+body+source.substring(original.actionEnd);
        }else result=original.call.replacement(source,draft.type.equals("state")?form(7,original.name,""+draft.value):form(11,draft.name,""+draft.x,""+draft.y,""+draft.color));
        GameRules check=new GameRules(withCode(result));if(!check.blocked.isEmpty())throw new IllegalArgumentException(check.blocked);
        return new CartEdit(base,check.base);
    }
    private WorkshopCartridge withCode(String text){LuaDraft d=new LuaDraft(base,0);d.selectAll();d.replace(text);return d.edit().candidate(base);}
    public int resultIndex(){if(move!=0)return index+move;if(!creating)return index;int n=0;for(Entry e:entries)if(e.type.equals(draft.type))n=entries.indexOf(e)+1;if(n==0&&draft.type.equals("rule"))n=states.size();if(n==0&&draft.type.equals("readout"))n=entries.size();return n;}
    private static void writeEntry(DataOutputStream o,Entry e)throws IOException {o.writeUTF(e.type);o.writeUTF(e.name);o.writeUTF(e.guard);o.writeUTF(e.target);for(int n:new int[]{e.value,e.event,e.button,e.compare,e.threshold,e.action,e.x,e.y,e.color})o.writeInt(n);}
    private static Entry readEntry(DataInputStream i)throws IOException {Entry e=new Entry(i.readUTF());e.name=i.readUTF();e.guard=i.readUTF();e.target=i.readUTF();e.value=i.readInt();e.event=i.readInt();e.button=i.readInt();e.compare=i.readInt();e.threshold=i.readInt();e.action=i.readInt();e.x=i.readInt();e.y=i.readInt();e.color=i.readInt();return e;}
    public void write(DataOutputStream o)throws IOException {o.writeUTF(page.name());o.writeInt(index);o.writeInt(menu);o.writeInt(field);o.writeInt(move);o.writeBoolean(creating);o.writeBoolean(deleting);o.writeBoolean(draft!=null);if(draft!=null)writeEntry(o,draft);o.writeBoolean(baseline!=null);if(baseline!=null)writeEntry(o,baseline);}
    public static GameRules read(DataInputStream i,WorkshopCartridge cart)throws IOException {
        GameRules g=new GameRules(cart);g.page=Page.valueOf(i.readUTF());g.index=i.readInt();g.menu=i.readInt();g.field=i.readInt();g.move=i.readInt();g.creating=i.readBoolean();g.deleting=i.readBoolean();if(i.readBoolean())g.draft=readEntry(i);if(i.readBoolean())g.baseline=readEntry(i);
        if(g.index<0||g.index>=Math.max(1,g.entries.size())||g.menu<0||g.menu>7||g.field<0||Math.abs(g.move)>1)throw new IOException("rules state");
        if((g.page==Page.FORM||g.page==Page.REVIEW)!=(g.draft!=null)||g.baseline!=null&&(g.page!=Page.FORM||g.draft==null)||g.deleting&&(g.creating||g.page!=Page.REVIEW)||g.move!=0&&(g.creating||g.deleting||g.page!=Page.REVIEW))throw new IOException("rules draft");
        if(g.draft!=null){g.available();g.validate(g.draft);if(g.field>=g.rows()||!g.creating&&(g.current()==null||!g.current().type.equals(g.draft.type)||g.draft.type.equals("state")&&!g.current().name.equals(g.draft.name)))throw new IOException("rules selection");if(g.baseline!=null){g.validate(g.baseline);if(!g.baseline.type.equals(g.draft.type))throw new IOException("rules field");}}
        return g;
    }
}
