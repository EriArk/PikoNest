package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Bounded ordinary draw calls. No scene graph, source annotations or runtime dependency. */
public final class GameUses {
    public enum Screen { LIST, MENU, FORM, REVIEW, PICK, ANIMATION }
    public final WorkshopCartridge base;
    public final String source;
    public final List<Entry> entries=new ArrayList<>();
    public String blocked="";
    public int index,field,menu,returnTool;
    public Screen screen=Screen.LIST;
    public boolean creating,deleting;
    public String kind="sspr";
    public int[] values={0,0,8,8,60,60};
    public SpritePlacement picker;
    public SpriteAnimation animation;
    public boolean converting;
    private int insertAt;
    private boolean newDraw;
    private String newline,indent=" ";
    private P8Map previewMap;
    public static final class Entry {
        public final LuaCall call;
        public final int[] values;
        public final AnimationEdit animation;
        Entry(LuaCall call,int[] values){this.call=call;this.values=values;animation=null;}
        Entry(AnimationEdit animation){this.animation=animation;call=null;values=new int[0];}
        public String kind(){return animation!=null?"animation":call.name;}
        public int start(){return animation!=null?animation.start:call.start;}
        public int end(){return animation!=null?animation.end:call.end;}
        public String title(){return animation!=null?"Анимация · "+animation.initial.count()+" кадров":call.name.equals("map")?"Карта":"Спрайт";}
        public int x(){return animation!=null?animation.initial.x:values[call.name.equals("sspr")?4:call.name.equals("map")?2:1];}
        public int y(){return animation!=null?animation.initial.y:values[call.name.equals("sspr")?5:call.name.equals("map")?3:2];}
    }
    public GameUses(WorkshopCartridge cart,int returnTool){
        base=cart;source=new LuaDraft(cart,0).text();this.returnTool=returnTool;
        newline=source.contains("\r\n")?"\r\n":source.contains("\r")?"\r":"\n";
        try{scan();}catch(IllegalArgumentException e){entries.clear();blocked=e.getMessage();}
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Здесь сложная отрисовка. Авторазмещение недоступно; исходник сохранён.");}
    private void scan(){
        LuaContext context=new LuaContext(source);String mask=context.masked();
        Matcher names=Pattern.compile("(?<![A-Za-z0-9_])_draw(?![A-Za-z0-9_])").matcher(mask);int count=0;while(names.find())count++;
        if(count==0){newDraw=true;insertAt=source.length();if(!context.allowsLine(insertAt)||depth(mask)!=0)throw unsupported();return;}
        Matcher header=Pattern.compile("(?m)^function[ \\t]+_draw[ \\t]*\\([ \\t]*\\)[ \\t]*\\r?$").matcher(mask);
        if(count!=1||!header.find()||depth(mask.substring(0,header.start()))!=0)throw unsupported();
        int at=afterLine(header.end());boolean closed=false;
        while(at<source.length()){
            int end=lineEnd(at);String line=mask.substring(at,end).trim();
            if(line.equals("end")){insertAt=at;closed=true;break;}
            if(!line.isEmpty()){
                if(line.equals("do")){
                    AnimationEdit animation=AnimationEdit.find(source,at);
                    if(animation==null||animation.start!=at)throw unsupported();
                    entries.add(new Entry(animation));indent=animation.indent;at=animation.end;continue;
                }
                LuaCall call;
                try{call=LuaCall.parse(source,at,end);}catch(IllegalArgumentException e){throw unsupported();}
                // A call spanning generated recipes or containing dynamic expressions is not a flat draw row.
                if(call.end!=end||!Arrays.asList("cls","spr","sspr","map","camera","print","circfill","rectfill").contains(call.name))throw unsupported();
                for(int n=0;n<call.form.item().fields.length;n++)
                    if(call.form.kind(n)!=LuaInsert.Kind.STRING&&!call.form.value(n).matches("-?[0-9]+"))throw unsupported();
                if(call.name.equals("camera")){
                    for(int n=0;n<call.form.item().fields.length;n++)if(!call.form.value(n).equals("0"))throw unsupported();
                }
                if(call.name.equals("spr")||call.name.equals("sspr")||call.name.equals("map")){
                    int[] v=new int[call.form.item().fields.length];
                    for(int n=0;n<v.length;n++)try{v[n]=Integer.parseInt(call.form.value(n));}catch(NumberFormatException e){throw unsupported();}
                    validate(call.name,v);entries.add(new Entry(call,v));
                }
                if(call.name.equals("cls")&&!entries.isEmpty())throw unsupported();
                String raw=source.substring(at,end);indent=raw.substring(0,raw.length()-raw.replaceFirst("^[ \\t]*","").length());
            }
            at=afterLine(end);
        }
        if(!closed)throw unsupported();
    }
    private int lineEnd(int at){while(at<source.length()&&source.charAt(at)!='\r'&&source.charAt(at)!='\n')at++;return at;}
    private int afterLine(int at){if(at<source.length()&&source.charAt(at)=='\r')at++;if(at<source.length()&&source.charAt(at)=='\n')at++;return at;}
    // Conservative prefix balance: compact if/ambiguous syntax is refused, never guessed.
    private static int depth(String mask){
        int depth=0,loop=0;Matcher m=Pattern.compile("\\b(function|if|for|while|do|repeat|end|until)\\b").matcher(mask);
        while(m.find()){String t=m.group();if(t.equals("end")||t.equals("until")){if(--depth<0)return -1;}
            else if(t.equals("do")){if(loop>0)loop--;else depth++;}
            else{depth++;if(t.equals("for")||t.equals("while"))loop++;}}
        return depth;
    }
    public Entry current(){return entries.isEmpty()?null:entries.get(Math.max(0,Math.min(index,entries.size()-1)));}
    public void move(int delta){index=Math.max(0,Math.min(entries.size()-1,index+delta));}
    private void available(){if(!blocked.isEmpty())throw new IllegalArgumentException(blocked);}
    private void builtin(String name){LuaSymbols symbols=new LuaSymbols(source);if(symbols.shadows(name)||(newDraw&&symbols.shadows("cls")))throw unsupported();}
    public void addSprite(SpriteRegion region){available();builtin("sspr");kind="sspr";values=new int[]{region.x,region.y,region.width,region.height,60,60};begin(true);}
    public void addMap(int x,int y){available();builtin("map");kind="map";values=new int[]{x,y,0,0,Math.min(16,128-x),Math.min(16,64-y)};begin(true);}
    private void begin(boolean create){creating=create;deleting=converting=false;animation=null;field=0;screen=Screen.FORM;validate(kind,values);}
    public void addAnimation(SpriteRegion region){available();builtin("sspr");SpriteAnimation.validateSource(source);kind="animation";creating=true;deleting=converting=false;animation=new SpriteAnimation(region);screen=Screen.ANIMATION;}
    public void animateSelected(){
        available();Entry e=current();if(e==null||e.kind().equals("map")||e.animation!=null)throw new IllegalArgumentException("Выбери размещённый спрайт для анимации.");
        if(e.x()<-127||e.x()>127||e.y()<-127||e.y()>127)throw new IllegalArgumentException("Положение вне диапазона редактора анимации −127…127.");
        edit();SpriteRegion r=region();addAnimation(r);animation.x=e.x();animation.y=e.y();creating=false;converting=true;
    }
    public void edit(){available();if(current()==null)return;
        Entry e=current();
        if(e.animation!=null){kind="animation";creating=deleting=converting=false;animation=e.animation.initial.copy();screen=Screen.ANIMATION;}
        else{kind=e.kind();values=e.values.clone();begin(false);}
    }
    public void duplicate(){edit();if(current()!=null)creating=true;}
    public void delete(){edit();if(current()!=null){deleting=true;screen=Screen.REVIEW;}}
    public void review(){if(animation==null)validate(kind,values);else{animation.playing=false;animation.review=false;}screen=Screen.REVIEW;}
    public void back(){if(screen==Screen.PICK){picker=null;screen=Screen.FORM;}else if(screen==Screen.REVIEW&&!deleting)screen=animation!=null?Screen.ANIMATION:Screen.FORM;else{animation=null;creating=deleting=converting=false;screen=Screen.LIST;}}
    public String[] labels(){return kind.equals("map")?new String[]{"Карта X, кл","Карта Y, кл","В игре X","В игре Y","Ширина, кл","Высота, кл"}:kind.equals("spr")?new String[]{"Тайл","В игре X","В игре Y"}:new String[]{"Лист X","Лист Y","Ширина, px","Высота, px","В игре X","В игре Y"};}
    public int x(){return animation!=null?animation.x:values[kind.equals("sspr")?4:kind.equals("map")?2:1];}
    public int y(){return animation!=null?animation.y:values[kind.equals("sspr")?5:kind.equals("map")?3:2];}
    public void adjust(int delta){int old=values[field];values[field]+=delta;try{validate(kind,values);}catch(IllegalArgumentException e){values[field]=old;}}
    private static void range(int v,int lo,int hi){if(v<lo||v>hi)throw new IllegalArgumentException("Область или положение вне диапазона формы");}
    private static void validate(String kind,int[] v){
        if(kind.equals("spr")){if(v.length!=3)throw unsupported();range(v[0],0,255);range(v[1],-32768,32767);range(v[2],-32768,32767);return;}
        if(v.length!=6)throw unsupported();
        if(kind.equals("sspr")){range(v[0],0,127);range(v[1],0,127);range(v[2],1,128-v[0]);range(v[3],1,128-v[1]);range(v[4],-32768,32767);range(v[5],-32768,32767);}
        else if(kind.equals("map")){range(v[0],0,127);range(v[1],0,63);range(v[4],1,128-v[0]);range(v[5],1,64-v[1]);range(v[2],-32768,32767);range(v[3],-32768,32767);}
        else throw unsupported();
    }
    public SpriteRegion region(){return kind.equals("spr")?new SpriteRegion(values[0]%16*8,values[0]/16*8,8,8):new SpriteRegion(values[0],values[1],values[2],values[3]);}
    public void pick(){if(kind.equals("map"))return;picker=new SpritePlacement(region());screen=Screen.PICK;}
    public void picked(){SpriteRegion r=picker.source();if(kind.equals("spr")&&(r.width!=8||r.height!=8))throw new IllegalArgumentException("Для этого тайла выбери область 8×8");
        if(kind.equals("spr"))values[0]=r.y/8*16+r.x/8;else{values[0]=r.x;values[1]=r.y;values[2]=r.width;values[3]=r.height;}picker=null;screen=Screen.FORM;}
    public String summary(){return (deleting?"Убрать использование":creating?"Добавить использование":"Изменить использование")+" · "+(kind.equals("map")?"карта":"спрайт");}
    public String target(){return newDraw?"Новая отрисовка · после очистки экрана":"Отрисовка · после существующих элементов";}
    public int pixel(WorkshopCartridge cart,int px,int py){
        if(animation!=null){SpriteRegion r=animation.frame(animation.visibleFrame()).region;return px<0||py<0||px>=r.width||py>=r.height?0:cart.pixel(r,px,py);}
        if(kind.equals("map")){
            if(px<0||py<0||px>=values[4]*8||py>=values[5]*8)return 0;
            if(previewMap==null)previewMap=cart.map();int t=previewMap.tile(values[0]+px/8,values[1]+py/8);return t==0?0:cart.sheetPixel(t%16*8+px%8,t/16*8+py%8);
        }
        SpriteRegion r=region();return px<0||py<0||px>=r.width||py>=r.height?0:cart.pixel(r,px,py);
    }
    public CartEdit proposal(){
        available();if(animation==null)validate(kind,values);String result;
        if(deleting){Entry e=current();if(e==null)throw unsupported();result=source.substring(0,e.start())+source.substring(e.animation!=null?e.end():afterLine(e.end()));}
        else if(animation!=null){
            SpriteAnimation.validateSource(source);
            if(!creating&&!converting)result=current().animation.replacement(source,animation);
            else if(converting){
                Entry e=current();String raw=source.substring(e.start(),e.end());String lead=raw.substring(0,raw.length()-raw.replaceFirst("^[ \\t]*","").length());
                int close=raw.indexOf(')');String suffix=raw.substring(close+1);
                String comment=suffix.trim().isEmpty()?"":lead+suffix+newline;
                result=source.substring(0,e.start())+comment+animationBlock(lead)+source.substring(afterLine(e.end()));
            }else result=insert(animationBlock(indent));
        }
        else if(!creating){Entry e=current();if(e==null)throw unsupported();LuaInsert form=e.call.form;for(int n=0;n<values.length;n++)form.set(n,Integer.toString(values[n]));result=e.call.replacement(source,form);}
        else{
            StringBuilder call=new StringBuilder(kind+"(");for(int n=0;n<values.length;n++){if(n>0)call.append(',');call.append(values[n]);}call.append(')');
            String insert=indent+call+newline;
            if(newDraw)insert=(source.isEmpty()||source.endsWith("\n")||source.endsWith("\r")?"":newline)+"function _draw()"+newline+" cls(1)"+newline+insert+"end"+newline;
            result=source.substring(0,insertAt)+insert+source.substring(insertAt);
        }
        LuaDraft draft=new LuaDraft(base,0);draft.selectAll();draft.replace(result);return draft.edit();
    }
    private String animationBlock(String prefix){StringBuilder b=new StringBuilder();for(String row:animation.code().split("\n"))b.append(prefix).append(row).append(newline);return b.toString();}
    private String insert(String block){
        if(newDraw)block=(source.isEmpty()||source.endsWith("\n")||source.endsWith("\r")?"":newline)+"function _draw()"+newline+" cls(1)"+newline+block+"end"+newline;
        return source.substring(0,insertAt)+block+source.substring(insertAt);
    }
    public byte[] encode(){try{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(2);byte[] cart=base.bytes();o.writeInt(cart.length);o.write(cart);
        o.writeInt(returnTool);o.writeInt(index);o.writeUTF(screen.name());o.writeInt(field);o.writeInt(menu);o.writeBoolean(creating);o.writeBoolean(deleting);o.writeUTF(kind);o.writeInt(values.length);for(int v:values)o.writeInt(v);o.writeBoolean(picker!=null);if(picker!=null)picker.write(o);
        o.writeBoolean(converting);o.writeBoolean(animation!=null);if(animation!=null)animation.write(o);return b.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static GameUses restore(byte[] bytes,WorkshopCartridge current){try{
        if(bytes.length>3*1024*1024)throw new IOException();DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes));int version=i.readInt();if(version<1||version>2)throw new IOException();int n=i.readInt();if(n<0||n>2*1024*1024||n>i.available())throw new IOException();byte[] cart=new byte[n];i.readFully(cart);
        if(!Arrays.equals(cart,current.bytes()))throw new IOException("Проект изменился; исходник не перезаписан");
        int tool=i.readInt();if(tool<0||tool>3)throw new IOException();GameUses g=new GameUses(current,tool);g.index=i.readInt();g.screen=Screen.valueOf(i.readUTF());g.field=i.readInt();g.menu=i.readInt();g.creating=i.readBoolean();g.deleting=i.readBoolean();g.kind=i.readUTF();int size=i.readInt();if(size!=3&&size!=6)throw new IOException();g.values=new int[size];for(int v=0;v<size;v++)g.values[v]=i.readInt();if(!g.kind.equals("animation"))validate(g.kind,g.values);if(i.readBoolean())g.picker=SpritePlacement.read(i);
        if(version==2){g.converting=i.readBoolean();if(i.readBoolean())g.animation=SpriteAnimation.read(i);}
        if(i.available()!=0||g.index<0||g.index>=Math.max(1,g.entries.size())||g.field<0||g.field>=size||g.menu<0||g.menu>7||(g.screen==Screen.PICK)!=(g.picker!=null))throw new IOException();
        if(g.screen!=Screen.LIST&&g.screen!=Screen.MENU&&!g.blocked.isEmpty())throw new IOException();
        if(g.screen==Screen.FORM||g.screen==Screen.REVIEW||g.screen==Screen.PICK||g.screen==Screen.ANIMATION){
            if(!g.creating&&(g.current()==null||!g.converting&&!g.kind.equals(g.current().kind())))throw new IOException();
            if(g.deleting&&(g.creating||g.screen!=Screen.REVIEW))throw new IOException();
        }
        boolean animated=g.screen==Screen.ANIMATION||g.screen==Screen.REVIEW&&g.kind.equals("animation");
        if(animated!=(g.animation!=null)||animated&&!g.kind.equals("animation")||g.converting&&(!animated||g.creating||g.deleting||g.current()==null||g.current().animation!=null||g.current().kind().equals("map")))throw new IOException();
        if(animated&&(g.animation.review||g.screen==Screen.REVIEW&&g.animation.picker!=null))throw new IOException();
        if(g.picker!=null&&(g.kind.equals("map")||g.picker.phase>1))throw new IOException();return g;
    }catch(Exception e){throw new IllegalArgumentException("Размещения не восстановлены. Картридж сохранён; открой список заново.",e);}}
}
