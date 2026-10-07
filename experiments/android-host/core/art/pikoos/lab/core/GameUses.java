package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Bounded ordinary draw calls. No scene graph, source annotations or runtime dependency. */
public final class GameUses {
    public enum Screen { LIST, MENU, FORM, REVIEW, PICK, ANIMATION, CAMERA, BACKGROUND, LOGIC }
    public final WorkshopCartridge base;
    public final String source;
    public final List<Entry> entries=new ArrayList<>();
    public String blocked="";
    public int index,field,menu,returnTool;
    public Screen screen=Screen.LIST;
    public boolean creating,deleting;
    public String kind="sspr";
    public int[] values={0,0,8,8,60,60};
    public String[] links={"",""};
    public final GameValues gameValues;
    public SpritePlacement picker;
    public SpriteAnimation animation;
    public boolean converting;
    public CameraUse camera;
    public BackgroundUse background;
    public GameRules logic;
    public int layerMove;
    public boolean copyAfter;
    private byte[] fieldBaseline;
    public boolean editingField(){return fieldBaseline!=null;}
    /** A field edit is a reversible part of the draft, never a cart write. */
    public void beginField(){
        if(editingField())return;
        try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
            if(background!=null)background.write(o);else if(camera!=null)camera.write(o);else if(animation!=null)animation.write(o);else if(screen==Screen.FORM){writePosition(o);}else return;
            fieldBaseline=b.toByteArray();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public void finishField(){fieldBaseline=null;}
    public void cancelField(){
        if(!editingField())return;
        try{DataInputStream i=new DataInputStream(new ByteArrayInputStream(fieldBaseline));
            if(background!=null)background=BackgroundUse.read(i);else if(camera!=null)camera=CameraUse.read(i,this);else if(animation!=null)animation=SpriteAnimation.read(i);else readPosition(i);
            fieldBaseline=null;
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private int insertAt;
    private boolean newDraw;
    private String newline,indent=" ";
    private P8Map previewMap;
    public static final class Entry {
        public final LuaCall call;
        public final int[] values;
        public final AnimationEdit animation;
        public LuaCall view;
        public String[] links={"",""};
        public String coordinate(int axis){return links[axis].isEmpty()?Integer.toString(axis==0?x():y()):links[axis];}
        public boolean linked(){return !links[0].isEmpty()||!links[1].isEmpty();}
        Entry(LuaCall call,int[] values){this.call=call;this.values=values;animation=null;}
        Entry(AnimationEdit animation){this.animation=animation;call=null;values=new int[0];}
        public String kind(){return animation!=null?"animation":call.name;}
        public int start(){return animation!=null?animation.start:call.start;}
        public int end(){return animation!=null?animation.end:call.end;}
        public boolean isBackground(){return call!=null&&call.name.equals("background");}
        public boolean isCamera(){return call!=null&&call.form.cameraRecipe();}
        public String title(){return isBackground()?"Background layer":isCamera()?CameraUse.title(call.form):animation!=null?"Анимация · "+animation.initial.count()+" кадров":call.name.equals("map")?"Карта":"Спрайт";}
        public int x(){return isCamera()||isBackground()?0:animation!=null?animation.initial.x:values[call.name.equals("sspr")?4:call.name.equals("map")?2:1];}
        public int y(){return isBackground()?Integer.parseInt(call.form.value(4)):isCamera()?0:animation!=null?animation.initial.y:values[call.name.equals("sspr")?5:call.name.equals("map")?3:2];}
    }
    public GameUses(WorkshopCartridge cart,int returnTool){
        base=cart;source=new LuaDraft(cart,0).text();this.returnTool=returnTool;gameValues=new GameValues(source);
        newline=source.contains("\r\n")?"\r\n":source.contains("\r")?"\r":"\n";
        try{scan();}catch(IllegalArgumentException e){entries.clear();blocked=e.getMessage();}
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("This draw code is not supported by the placement editor yet. The source is preserved.");}
    private void scan(){
        LuaContext context=new LuaContext(source);String mask=context.masked();
        Matcher names=Pattern.compile("(?<![A-Za-z0-9_])_draw(?![A-Za-z0-9_])").matcher(mask);int count=0;while(names.find())count++;
        if(count==0){newDraw=true;insertAt=source.length();if(!context.allowsLine(insertAt)||depth(mask)!=0)throw unsupported();return;}
        Matcher header=Pattern.compile("(?m)^function[ \\t]+_draw[ \\t]*\\([ \\t]*\\)[ \\t]*\\r?$").matcher(mask);
        if(count!=1||!header.find()||depth(mask.substring(0,header.start()))!=0)throw unsupported();
        int at=afterLine(header.end());boolean closed=false;LuaCall view=null;
        while(at<source.length()){
            int end=lineEnd(at);String line=mask.substring(at,end).trim();
            if(line.equals("end")){insertAt=at;closed=true;break;}
            if(!line.isEmpty()){
                if(line.equals("do")){
                    LuaCall layer=BackgroundLayer.find(source,at);
                    if(layer!=null&&layer.start==at){Entry e=new Entry(layer,new int[0]);e.view=view;entries.add(e);at=afterLine(layer.end);continue;}
                    AnimationEdit animation=AnimationEdit.find(source,at);
                    if(animation==null||animation.start!=at)throw unsupported();
                    Entry e=new Entry(animation);e.view=view;entries.add(e);indent=animation.indent;at=animation.end;continue;
                }
                LuaCall call;
                try{call=LuaCall.parse(source,at,end);}catch(IllegalArgumentException e){throw unsupported();}
                if(call.form.cameraRecipe()){
                    if(call.end!=end)throw unsupported();Entry e=new Entry(call,new int[0]);e.view=view;entries.add(e);view=call;at=afterLine(end);continue;
                }
                // A call spanning generated recipes or containing dynamic expressions is not a flat draw row.
                if(call.end!=end||!Arrays.asList("cls","spr","sspr","map","camera","print","circfill","rectfill").contains(call.name))throw unsupported();
                for(int n=0;n<call.form.item().fields.length;n++)
                    if(call.form.kind(n)!=LuaInsert.Kind.STRING&&!call.form.value(n).matches("-?[0-9]+")
                        &&!(call.name.equals("print")&&n==0&&call.form.value(n).matches("[A-Za-z_][A-Za-z_0-9]*"))
                        &&!(coordinateAxis(call.name,n)>=0&&gameValues.initial.containsKey(call.form.value(n))))throw unsupported();
                if(call.name.equals("spr")||call.name.equals("sspr")||call.name.equals("map")){
                    int[] v=new int[call.form.item().fields.length];
                    String[] linked={"",""};
                    for(int n=0;n<v.length;n++)try{v[n]=Integer.parseInt(call.form.value(n));}catch(NumberFormatException e){int axis=coordinateAxis(call.name,n);if(axis<0)throw unsupported();linked[axis]=call.form.value(n);v[n]=gameValues.value(linked[axis]);}
                    validate(call.name,v);Entry e=new Entry(call,v);e.links=linked;e.view=view;entries.add(e);
                }
                if(call.name.equals("cls"))for(Entry e:entries)if(!e.isCamera())throw unsupported();
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
    private void begin(boolean create){if(create)links=new String[]{"",""};finishField();background=null;layerMove=0;creating=create;deleting=converting=copyAfter=false;animation=null;camera=null;field=0;screen=Screen.FORM;validate(kind,values);}
    public void addAnimation(SpriteRegion region){available();builtin("sspr");SpriteAnimation.validateSource(source);finishField();background=null;layerMove=0;kind="animation";creating=true;deleting=converting=copyAfter=false;camera=null;animation=new SpriteAnimation(region);screen=Screen.ANIMATION;}
    public void addCamera(boolean reset){available();if(current()==null||current().isCamera())throw new IllegalArgumentException("Select a sprite, animation, map or background first.");WorldCamera.validateSource(source,false);finishField();background=null;layerMove=0;camera=new CameraUse(this,reset);animation=null;creating=true;deleting=converting=copyAfter=false;kind="camera";screen=Screen.CAMERA;}
    public void addBackground(SpriteRegion region){
        available();builtin("sspr");BackgroundUse next=new BackgroundUse(region);BackgroundLayer.validate(source,next.form);
        finishField();animation=null;camera=null;background=next;kind="background";creating=true;
        deleting=converting=copyAfter=false;layerMove=0;screen=Screen.BACKGROUND;
    }
    public void reorderLayer(int direction){
        available();if(current()==null||!current().isBackground())throw new IllegalArgumentException("Select a background layer first.");
        BackgroundLayers list=new BackgroundLayers(source,current().start());list.change(direction);
        edit();layerMove=direction;screen=Screen.REVIEW;
    }
    public int resultIndex(){
        if(layerMove!=0)return index+layerMove;
        if(background!=null)return index+(creating&&copyAfter?1:0);
        return camera!=null?index:creating?(copyAfter?index+1:entries.size()):index;
    }
    public void animateSelected(){
        available();Entry e=current();if(e==null||e.isCamera()||e.isBackground()||e.kind().equals("map")||e.animation!=null)throw new IllegalArgumentException("Select a placed sprite to animate.");
        if(e.linked())throw new IllegalArgumentException("This sprite follows game values. State-linked animation is not available yet; its coordinates were preserved.");
        if(e.x()<-127||e.x()>127||e.y()<-127||e.y()>127)throw new IllegalArgumentException("This animation editor supports positions from -127 to 127. The source is preserved.");
        edit();SpriteRegion r=region();addAnimation(r);animation.x=e.x();animation.y=e.y();creating=false;converting=true;
    }
    public void edit(){available();if(current()==null)return;
        Entry e=current();
        finishField();background=null;layerMove=0;camera=null;copyAfter=false;
        if(e.isBackground()){kind="background";creating=deleting=converting=false;animation=null;background=new BackgroundUse(e.call.form);screen=Screen.BACKGROUND;}
        else if(e.isCamera()){kind="camera";creating=deleting=converting=false;animation=null;camera=new CameraUse(this,e.call);screen=Screen.CAMERA;}
        else if(e.animation!=null){kind="animation";creating=deleting=converting=false;animation=e.animation.initial.copy();screen=Screen.ANIMATION;}
        else{kind=e.kind();values=e.values.clone();links=e.links.clone();begin(false);}
    }
    public void duplicate(){if(current()!=null&&current().isCamera())throw new IllegalArgumentException("Choose a use, then add a camera from Actions.");edit();if(current()!=null){creating=true;copyAfter=background!=null||current().view!=null;}}
    public void delete(){edit();if(current()!=null){deleting=true;screen=Screen.REVIEW;}}
    public void review(){finishField();if(background!=null){BackgroundLayer.validate(source,background.form);background.playing=false;}else if(camera!=null){camera.validate();camera.invalidatePreview();}else if(animation==null){validate(kind,values);validateLinks();}else{animation.playing=false;animation.review=false;}screen=Screen.REVIEW;}
    public void back(){finishField();if(screen==Screen.PICK){picker=null;screen=Screen.FORM;}else if(screen==Screen.REVIEW&&!deleting&&layerMove==0)screen=background!=null?Screen.BACKGROUND:camera!=null?Screen.CAMERA:animation!=null?Screen.ANIMATION:Screen.FORM;else{animation=null;camera=null;background=null;layerMove=0;creating=deleting=converting=copyAfter=false;screen=Screen.LIST;}}
    public String[] labels(){return kind.equals("map")?new String[]{"Карта X, кл","Карта Y, кл","В игре X","В игре Y","Ширина, кл","Высота, кл"}:kind.equals("spr")?new String[]{"Тайл","В игре X","В игре Y"}:new String[]{"Лист X","Лист Y","Ширина, px","Высота, px","В игре X","В игре Y"};}
    private static int coordinateAxis(String kind,int field){int x=kind.equals("sspr")?4:kind.equals("map")?2:kind.equals("spr")?1:-10;return field==x?0:field==x+1?1:-1;}
    public int coordinateField(int axis){return kind.equals("sspr")?4+axis:kind.equals("map")?2+axis:1+axis;}
    public int x(){return animation!=null?animation.x:links[0].isEmpty()?values[coordinateField(0)]:gameValues.value(links[0]);}
    public int y(){return animation!=null?animation.y:links[1].isEmpty()?values[coordinateField(1)]:gameValues.value(links[1]);}
    public boolean linked(){return !links[0].isEmpty()||!links[1].isEmpty();}
    public String coordinate(int axis){return links[axis].isEmpty()?Integer.toString(values[coordinateField(axis)]):links[axis];}
    public int placementRows(){return values.length+3;}
    public String placementLabel(int at){
        if(at==placementRows()-1)return "Preview changes";
        if(at>=values.length)return (at==values.length?"X":"Y")+" follows: "+(links[at-values.length].isEmpty()?"Fixed position":links[at-values.length]);
        String[] labels=kind.equals("map")?new String[]{"Map X (tiles)","Map Y (tiles)","Game X","Game Y","Width (tiles)","Height (tiles)"}:kind.equals("spr")?new String[]{"Tile","Game X","Game Y"}:new String[]{"Sheet X","Sheet Y","Width (px)","Height (px)","Game X","Game Y"};
        int axis=coordinateAxis(kind,at);return labels[at]+": "+(axis>=0&&!links[axis].isEmpty()?links[axis]+" (starts "+gameValues.value(links[axis])+")":values[at]);
    }
    public void placementField(){
        int axis=coordinateAxis(kind,field);
        if(field<values.length&&axis>=0&&!links[axis].isEmpty())throw new IllegalArgumentException("This coordinate follows "+links[axis]+". Change its starting value in Rules & state, or choose Fixed position below to unlink it.");
        if(field>=values.length&&gameValues.initial.isEmpty())throw new IllegalArgumentException("Add a starting value in Rules & state first. B cancels this unsaved placement; X from the game list opens Rules & state. Then reopen the placement.");
        beginField();
    }
    public void changePlacement(int delta){
        if(field<values.length){adjust(delta);return;}
        int axis=field-values.length;List<String> names=new ArrayList<>();names.add("");names.addAll(gameValues.initial.keySet());
        int at=names.indexOf(links[axis]);String next=names.get(Math.max(0,Math.min(names.size()-1,at+Integer.signum(delta))));
        if(next.isEmpty()&&!links[axis].isEmpty())values[coordinateField(axis)]=gameValues.value(links[axis]);links[axis]=next;
    }
    private void validateLinks(){if(links.length!=2)throw unsupported();for(String name:links)if(!name.isEmpty())gameValues.value(name);}
    private void writePosition(DataOutputStream o)throws IOException{o.writeInt(values.length);for(int v:values)o.writeInt(v);o.writeUTF(links[0]);o.writeUTF(links[1]);}
    private void readPosition(DataInputStream i)throws IOException{int size=i.readInt();if(size!=values.length)throw new IOException();int[] v=new int[size];for(int n=0;n<size;n++)v[n]=i.readInt();String[] l={i.readUTF(),i.readUTF()};validate(kind,v);for(String name:l)if(!name.isEmpty())gameValues.value(name);values=v;links=l;}

    public void adjust(int delta){int axis=coordinateAxis(kind,field);if(axis>=0&&!links[axis].isEmpty())throw new IllegalArgumentException("Change the linked starting value in Rules & state, or unlink this coordinate first.");int old=values[field];values[field]+=delta;try{validate(kind,values);}catch(IllegalArgumentException e){values[field]=old;}}
    private static void range(int v,int lo,int hi){if(v<lo||v>hi)throw new IllegalArgumentException("The region or position is outside this form's supported range.");}
    private static void validate(String kind,int[] v){
        if(kind.equals("spr")){if(v.length!=3)throw unsupported();range(v[0],0,255);range(v[1],-32768,32767);range(v[2],-32768,32767);return;}
        if(v.length!=6)throw unsupported();
        if(kind.equals("sspr")){range(v[0],0,127);range(v[1],0,127);range(v[2],1,128-v[0]);range(v[3],1,128-v[1]);range(v[4],-32768,32767);range(v[5],-32768,32767);}
        else if(kind.equals("map")){range(v[0],0,127);range(v[1],0,63);range(v[4],1,128-v[0]);range(v[5],1,64-v[1]);range(v[2],-32768,32767);range(v[3],-32768,32767);}
        else throw unsupported();
    }
    public SpriteRegion region(){return kind.equals("spr")?new SpriteRegion(values[0]%16*8,values[0]/16*8,8,8):new SpriteRegion(values[0],values[1],values[2],values[3]);}
    public void pick(){if(kind.equals("map"))return;picker=new SpritePlacement(region());screen=Screen.PICK;}
    public void picked(){SpriteRegion r=picker.source();if(kind.equals("spr")&&(r.width!=8||r.height!=8))throw new IllegalArgumentException("Choose an 8 × 8 region for this tile.");
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
        available();if(background!=null)return backgroundProposal();if(camera!=null)return camera.proposal(deleting);if(animation==null){validate(kind,values);validateLinks();}String result;
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
        else if(!creating){Entry e=current();if(e==null)throw unsupported();LuaInsert form=e.call.form;for(int n=0;n<values.length;n++){int axis=coordinateAxis(kind,n);form.set(n,axis<0?Integer.toString(values[n]):coordinate(axis));}result=e.call.replacement(source,form);}
        else{
            StringBuilder call=new StringBuilder(kind+"(");for(int n=0;n<values.length;n++){if(n>0)call.append(',');int axis=coordinateAxis(kind,n);call.append(axis<0?Integer.toString(values[n]):coordinate(axis));}call.append(')');
            String insert=indent+call+newline;
            if(newDraw)insert=(source.isEmpty()||source.endsWith("\n")||source.endsWith("\r")?"":newline)+"function _draw()"+newline+" cls(1)"+newline+insert+"end"+newline;
            int at=creationAt();result=source.substring(0,at)+insert+source.substring(at);
        }
        LuaDraft draft=new LuaDraft(base,0);draft.selectAll();draft.replace(result);return draft.edit();
    }
    private CartEdit backgroundProposal(){
        BackgroundLayer.validate(source,background.form);String result;
        if(layerMove!=0||deleting)result=new BackgroundLayers(source,current().start()).change(deleting?3:layerMove).result;
        else if(!creating)result=current().call.replacement(source,background.form);
        else if(copyAfter){
            Entry e=current();int at=afterLine(e.end());
            result=source.substring(0,at)+e.call.preview(background.form)+newline+source.substring(at);
        }else{
            StringBuilder block=new StringBuilder();
            for(String row:background.form.code().split("\n"))block.append(indent).append(row).append(newline);
            if(newDraw)result=insert(block.toString());
            else{int at=current()==null?insertAt:current().start();result=source.substring(0,at)+block+source.substring(at);}
        }
        LuaDraft draft=new LuaDraft(base,0);draft.selectAll();draft.replace(result);return draft.edit();
    }
    private String animationBlock(String prefix){StringBuilder b=new StringBuilder();for(String row:animation.code().split("\n"))b.append(prefix).append(row).append(newline);return b.toString();}
    private String insert(String block){
        if(newDraw)block=(source.isEmpty()||source.endsWith("\n")||source.endsWith("\r")?"":newline)+"function _draw()"+newline+" cls(1)"+newline+block+"end"+newline;
        int at=creationAt();return source.substring(0,at)+block+source.substring(at);
    }
    private int creationAt(){return copyAfter&&current()!=null?(current().animation!=null?current().end():afterLine(current().end())):insertAt;}
    public byte[] encode(){try{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(7);byte[] cart=base.bytes();o.writeInt(cart.length);o.write(cart);
        o.writeInt(returnTool);o.writeInt(index);o.writeUTF(screen.name());o.writeInt(field);o.writeInt(menu);o.writeBoolean(creating);o.writeBoolean(deleting);o.writeUTF(kind);o.writeInt(values.length);for(int v:values)o.writeInt(v);o.writeBoolean(picker!=null);if(picker!=null)picker.write(o);
        o.writeBoolean(converting);o.writeBoolean(animation!=null);if(animation!=null)animation.write(o);o.writeBoolean(copyAfter);o.writeBoolean(camera!=null);if(camera!=null)camera.write(o);o.writeInt(fieldBaseline==null?0:fieldBaseline.length);if(fieldBaseline!=null)o.write(fieldBaseline);o.writeBoolean(background!=null);if(background!=null)background.write(o);o.writeInt(layerMove);o.writeBoolean(logic!=null);if(logic!=null)logic.write(o);o.writeUTF(links[0]);o.writeUTF(links[1]);return b.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static GameUses restore(byte[] bytes,WorkshopCartridge current){try{
        if(bytes.length>3*1024*1024)throw new IOException();DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes));int version=i.readInt();if(version<1||version>7)throw new IOException();int n=i.readInt();if(n<0||n>2*1024*1024||n>i.available())throw new IOException();byte[] cart=new byte[n];i.readFully(cart);
        if(!Arrays.equals(cart,current.bytes()))throw new IOException("The project changed; its source was not overwritten.");
        int tool=i.readInt();if(tool<0||tool>3)throw new IOException();GameUses g=new GameUses(current,tool);g.index=i.readInt();g.screen=Screen.valueOf(i.readUTF());g.field=i.readInt();g.menu=i.readInt();g.creating=i.readBoolean();g.deleting=i.readBoolean();g.kind=i.readUTF();int size=i.readInt();if(size!=3&&size!=6)throw new IOException();g.values=new int[size];for(int v=0;v<size;v++)g.values[v]=i.readInt();if(!g.kind.equals("animation")&&!g.kind.equals("camera")&&!g.kind.equals("background"))validate(g.kind,g.values);if(i.readBoolean())g.picker=SpritePlacement.read(i);
        if(version>=2){g.converting=i.readBoolean();if(i.readBoolean())g.animation=SpriteAnimation.read(i);}
        if(version>=3){g.copyAfter=i.readBoolean();if(i.readBoolean())g.camera=CameraUse.read(i,g);}
        if(version>=4){int length=i.readInt();if(length<0||length>8192||length>i.available())throw new IOException();
            if(length>0){g.fieldBaseline=new byte[length];i.readFully(g.fieldBaseline);}
        }
        if(version>=5){if(i.readBoolean())g.background=BackgroundUse.read(i);g.layerMove=i.readInt();}
        if(version>=6&&i.readBoolean())g.logic=GameRules.read(i,current);
        if(version>=7){g.links=new String[]{i.readUTF(),i.readUTF()};g.validateLinks();}
        else if(g.current()!=null&&!g.creating&&g.screen==Screen.FORM)g.links=g.current().links.clone();
        if((g.screen==Screen.LOGIC)!=(g.logic!=null))throw new IOException();
        if(g.fieldBaseline!=null){
            if(g.screen!=Screen.CAMERA&&g.screen!=Screen.ANIMATION&&g.screen!=Screen.BACKGROUND&&g.screen!=Screen.FORM||g.animation!=null&&g.animation.picker!=null||g.background!=null&&g.background.picker!=null)throw new IOException();
            DataInputStream f=new DataInputStream(new ByteArrayInputStream(g.fieldBaseline));
            if(g.background!=null)BackgroundUse.read(f);else if(g.camera!=null)CameraUse.read(f,g);else if(g.animation!=null)SpriteAnimation.read(f);else{int[] saved=g.values.clone();String[] links=g.links.clone();g.readPosition(f);g.values=saved;g.links=links;}
            if(f.available()!=0)throw new IOException();
        }
        if(i.available()!=0||g.index<0||g.index>=Math.max(1,g.entries.size())||g.field<0||g.field>=((g.screen==Screen.FORM||g.screen==Screen.PICK||g.screen==Screen.REVIEW&&g.animation==null&&g.camera==null&&g.background==null)?g.placementRows():size)||g.menu<0||g.menu>13||(g.screen==Screen.PICK)!=(g.picker!=null))throw new IOException();
        if(g.screen!=Screen.LIST&&g.screen!=Screen.MENU&&g.screen!=Screen.LOGIC&&!g.blocked.isEmpty())throw new IOException();
        if(g.screen==Screen.FORM||g.screen==Screen.REVIEW||g.screen==Screen.PICK||g.screen==Screen.ANIMATION||g.screen==Screen.CAMERA||g.screen==Screen.BACKGROUND){
            if(!g.creating&&(g.current()==null||!g.converting&&g.camera==null&&!g.kind.equals(g.current().kind())))throw new IOException();
            if(g.deleting&&(g.creating||g.screen!=Screen.REVIEW))throw new IOException();
        }
        boolean animated=g.screen==Screen.ANIMATION||g.screen==Screen.REVIEW&&g.kind.equals("animation");
        if(animated!=(g.animation!=null)||animated&&!g.kind.equals("animation")||g.converting&&(!animated||g.creating||g.deleting||g.current()==null||g.current().animation!=null||g.current().kind().equals("map")||g.current().isBackground()))throw new IOException();
        if(animated&&(g.animation.review||g.screen==Screen.REVIEW&&g.animation.picker!=null))throw new IOException();
        if((g.screen==Screen.CAMERA||g.screen==Screen.REVIEW&&g.kind.equals("camera"))!=(g.camera!=null)||g.copyAfter&&(!g.creating||g.current()==null||g.current().isCamera()))throw new IOException();
        if((g.screen==Screen.BACKGROUND||g.screen==Screen.REVIEW&&g.kind.equals("background"))!=(g.background!=null))throw new IOException();
        if(g.background!=null){
            if(!g.kind.equals("background")||g.camera!=null||g.animation!=null||g.screen==Screen.REVIEW&&g.background.picker!=null||g.copyAfter&&(g.current()==null||!g.current().isBackground()))throw new IOException();
            BackgroundLayer.validate(g.source,g.background.form);
        }
        if(g.layerMove!=0){if(Math.abs(g.layerMove)!=1||g.background==null||g.screen!=Screen.REVIEW||g.creating||g.deleting)throw new IOException();new BackgroundLayers(g.source,g.current().start()).change(g.layerMove);}
        if(g.picker!=null&&(g.kind.equals("map")||g.picker.phase>1))throw new IOException();return g;
    }catch(Exception e){throw new IllegalArgumentException("Could not restore game uses. The saved cartridge is intact; reopen the list.",e);}}
}
