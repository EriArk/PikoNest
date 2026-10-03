package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.ArrayDeque;

/** Portable literal-text editor with explicit snippet proposals; never normalizes unrelated Lua. */
public final class LuaDraft {
    public enum Panel { CURSOR, KEYS, MENU, EXIT, INSERT }
    public static final String[] PAGES={"abcdefghijklmnopqrstuvwxyz0123456789_ ",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_ ","()[]{}=+-*/%^#<>~!;:,.\"'\\|&?$@_ "};
    public static final String[] COMMANDS={"Сохранить и закрыть","Сохранить и тест","Новая строка","Пробел","Табуляция",
        "Стереть слева","Удалить справа","Начать / снять выделение","Выделить всё","Копировать","Вырезать","Вставить",
        "Отменить правку","Вернуть правку","В начало строки","В конец строки","Закрыть черновик","Буквы / символы","Вставить конструкцию / API"};
    private static final int LIMIT=2*1024*1024; // Lab memory guard, not a PICO-8 code budget.
    private final WorkshopCartridge original;
    private final int lua;
    private final String initial,newline;
    private String text,clipboard="";
    private int cursor,anchor=-1;
    public Panel panel=Panel.CURSOR;
    public int page,key,menu;
    public LuaInsert insertion;
    private final ArrayDeque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private static final class State {
        final String text;final int cursor,anchor;
        State(LuaDraft d){text=d.text;cursor=d.cursor;anchor=d.anchor;}
        void apply(LuaDraft d){d.text=text;d.cursor=cursor;d.anchor=anchor;}
    }
    public LuaDraft(WorkshopCartridge cart,int line){
        original=cart;P8Document doc=P8Document.parse(cart.bytes());lua=doc.uniqueSection("lua");
        initial=decode(doc.body(lua));text=initial;
        int lf=text.indexOf('\n'),cr=text.indexOf('\r');
        newline=cr>=0&&(lf<0||cr<lf)?(cr+1==lf?"\r\n":"\r"):"\n";
        cursor=lineStart(Math.max(0,Math.min(line,lineCount()-1)));
    }
    private static String decode(byte[] bytes){
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException e){throw new IllegalArgumentException("Этот Lua не в UTF-8. Просмотр и исходные байты сохранены; редактор этой кодировки ещё не готов.");}
    }
    public String text(){return text;}
    public int cursor(){return cursor;}
    public int anchor(){return anchor;}
    public boolean dirty(){return !text.equals(initial);}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}
    public int selectionStart(){return anchor<0?cursor:Math.min(anchor,cursor);}
    public int selectionEnd(){return anchor<0?cursor:Math.max(anchor,cursor);}
    private boolean breakAt(int i){return i<text.length()&&(text.charAt(i)=='\n'||text.charAt(i)=='\r');}
    private int next(int i){
        if(i>=text.length())return text.length();
        if(text.charAt(i)=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')return i+2;
        return text.offsetByCodePoints(i,1);
    }
    private int previous(int i){
        if(i<=0)return 0;
        if(i>1&&text.charAt(i-1)=='\n'&&text.charAt(i-2)=='\r')return i-2;
        return text.offsetByCodePoints(i,-1);
    }
    public int lineCount(){int n=1;for(int i=0;i<text.length();i=next(i))if(breakAt(i))n++;return n;}
    public int line(){int n=0;for(int i=0;i<cursor;i=next(i))if(breakAt(i))n++;return n;}
    public int lineStart(int line){int n=0,i=0;while(i<text.length()&&n<line){if(breakAt(i))n++;i=next(i);}return i;}
    public int lineEnd(int line){int i=lineStart(line);while(i<text.length()&&!breakAt(i))i=next(i);return i;}
    public String lineText(int line){return text.substring(lineStart(line),lineEnd(line));}
    public int column(){return text.codePointCount(lineStart(line()),cursor);}
    public int offset(int line,int column){int i=lineStart(line),end=lineEnd(line);while(column-->0&&i<end)i=next(i);return i;}
    public void point(int line,int column){cursor=offset(Math.max(0,Math.min(line,lineCount()-1)),Math.max(0,column));}
    public void move(int dx,int dy){
        if(dy!=0)point(line()+dy,column());
        if(dx<0)cursor=previous(cursor);if(dx>0)cursor=next(cursor);
    }
    public void home(){cursor=lineStart(line());}
    public void end(){cursor=lineEnd(line());}
    public void select(){anchor=anchor<0?cursor:-1;}
    public void selectAll(){anchor=0;cursor=text.length();}
    public void copy(){if(selectionStart()!=selectionEnd())clipboard=text.substring(selectionStart(),selectionEnd());}
    public void cut(){copy();if(selectionStart()!=selectionEnd())replace("");}
    public void paste(){if(!clipboard.isEmpty())replace(clipboard);}
    public void insertNewline(){replace(newline);}
    public void replace(String value){
        if(value==null)throw new IllegalArgumentException("Missing text");
        // Public input is literal UTF-8; never silently replace an invalid surrogate.
        if(!StandardCharsets.UTF_8.newEncoder().canEncode(value))throw new IllegalArgumentException("Недопустимый символ");
        int from=selectionStart(),to=selectionEnd();
        String changed=text.substring(0,from)+value+text.substring(to);
        if(changed.getBytes(StandardCharsets.UTF_8).length>LIMIT)throw new IllegalArgumentException("Достигнут предел памяти черновика PIKOOS");
        if(!changed.equals(text)){remember(undo);redo.clear();text=changed;}
        cursor=from+value.length();anchor=-1;
    }
    private void remember(ArrayDeque<State> stack){if(stack.size()==32)stack.removeLast();stack.push(new State(this));}
    public void erase(boolean forward){
        if(selectionStart()!=selectionEnd()){replace("");return;}
        int from=forward?cursor:previous(cursor),to=forward?next(cursor):cursor;
        if(from==to)return;
        remember(undo);redo.clear();text=text.substring(0,from)+text.substring(to);cursor=from;anchor=-1;
    }
    public void history(boolean returning){
        ArrayDeque<State> from=returning?redo:undo,to=returning?undo:redo;
        if(!from.isEmpty()){remember(to);from.pop().apply(this);}
    }
    public void moveKey(int dx,int dy){key=Math.max(0,Math.min(PAGES[page].length()-1,(key/10+dy)*10+Math.max(0,Math.min(9,key%10+dx))));}
    public void changePage(int delta){page=(page+delta+PAGES.length)%PAGES.length;key=Math.min(key,PAGES[page].length()-1);}
    public void typeKey(){replace(PAGES[page].substring(key,key+1));}
    public void beginInsert(){
        if(selectionStart()!=selectionEnd())throw new IllegalArgumentException("Сними выделение перед вставкой. Существующий текст не заменяется.");
        if(!new LuaContext(text).allowsLine(lineStart(line())))throw new IllegalArgumentException("Выбранная строка внутри текста или комментария. Перейди к строке Lua.");
        insertion=new LuaInsert();panel=Panel.INSERT;
    }
    public void applyInsert(){
        if(insertion==null||panel!=Panel.INSERT||insertion.screen!=LuaInsert.Screen.FIELDS)return;
        int at=lineStart(line());String current=lineText(line()),indent="";
        for(int i=0;i<current.length()&&(current.charAt(i)==' '||current.charAt(i)=='\t');i++)indent+=current.charAt(i);
        LuaContext context=new LuaContext(text);
        if(!context.allowsLine(at))throw new IllegalArgumentException("Выбранная строка внутри текста или комментария");
        String function=insertion.functionName();
        if(function!=null&&context.defines(function))throw new IllegalArgumentException("Функция "+function+" уже задана. Перейди к её телу; существующий код не заменён.");
        String raw=insertion.code();StringBuilder inserted=new StringBuilder();
        for(String row:raw.split("\n"))inserted.append(indent).append(row).append(newline);
        String changed=text.substring(0,at)+inserted+text.substring(at);
        if(changed.getBytes(StandardCharsets.UTF_8).length>LIMIT)throw new IllegalArgumentException("Достигнут предел памяти черновика PIKOOS");
        int caret=insertion.item().block?at+inserted.indexOf(newline)+newline.length()+indent.length()+2:at+inserted.length()+indent.length();
        remember(undo);redo.clear();text=changed;cursor=caret;anchor=-1;insertion=null;panel=Panel.CURSOR;
    }
    public void cancelInsert(){insertion=null;panel=Panel.CURSOR;}
    public CartEdit edit(){
        P8Document doc=P8Document.parse(original.bytes());
        byte[] body=text.getBytes(StandardCharsets.UTF_8);
        // A changed last line must not swallow the following resource marker.
        if(!text.equals(initial)&&lua+1<doc.sections().size()&&!text.isEmpty()
            &&!text.endsWith("\n")&&!text.endsWith("\r"))body=(text+newline).getBytes(StandardCharsets.UTF_8);
        return new CartEdit(original,new WorkshopCartridge(doc.edit(lua,0,doc.body(lua).length,body).bytes()));
    }
    /** Recovery snapshot, including original bytes for stale-draft detection. History is session-only. */
    public byte[] encode(){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(3);write(out,original.bytes());write(out,text.getBytes(StandardCharsets.UTF_8));
            out.writeInt(cursor);out.writeInt(anchor);out.writeInt(page);out.writeInt(key);out.writeInt(panel.ordinal());out.writeInt(menu);
            write(out,clipboard.getBytes(StandardCharsets.UTF_8));out.writeBoolean(insertion!=null);if(insertion!=null)insertion.write(out);
            out.close();return bytes.toByteArray();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static LuaDraft restore(byte[] bytes){
        try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));int version=in.readInt();if(version<1||version>3)throw new IOException("version");
            LuaDraft d=new LuaDraft(new WorkshopCartridge(read(in)),0);d.text=decode(read(in));
            d.cursor=in.readInt();d.anchor=in.readInt();d.page=in.readInt();d.key=in.readInt();int panel=in.readInt();d.menu=in.readInt();
            d.clipboard=decode(read(in));
            if(version>=2&&in.readBoolean())d.insertion=LuaInsert.read(in,version>=3);
            if(in.available()!=0||!d.boundary(d.cursor)||(d.anchor!=-1&&!d.boundary(d.anchor))||d.page<0||d.page>=PAGES.length
                ||d.key<0||d.key>=PAGES[d.page].length()||panel<0||panel>=Panel.values().length||d.menu<0||d.menu>=COMMANDS.length)throw new IOException("state");
            d.panel=Panel.values()[panel];if((d.panel==Panel.INSERT)!=(d.insertion!=null))throw new IOException("insert state");
            if(d.insertion!=null&&d.insertion.screen==LuaInsert.Screen.SYMBOLS){d.insertion.attachSource(d.text);d.insertion.symbolMove(0);}
            return d;
        }catch(IOException|IllegalArgumentException e){throw new IllegalArgumentException("Не удалось прочитать черновик кода; исходный проект сохранён",e);}
    }
    private boolean boundary(int i){return i>=0&&i<=text.length()&&(i==0||i==text.length()
        ||(!Character.isLowSurrogate(text.charAt(i))&&!(text.charAt(i)=='\n'&&text.charAt(i-1)=='\r')));}
    private static void write(DataOutputStream out,byte[] value)throws IOException{out.writeInt(value.length);out.write(value);}
    private static byte[] read(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>LIMIT||n>in.available())throw new IOException("size");byte[] b=new byte[n];in.readFully(b);return b;}
}
