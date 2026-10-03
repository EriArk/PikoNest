package art.pikoos.lab.core;

import java.io.*;

/** Small editable catalogue of ordinary Lua. No bindings or extra runtime components. */
public final class LuaInsert {
    public enum Screen { CATALOG, FIELDS, TEXT, SYMBOLS }
    public enum Kind { NAME, EXPR, STRING, COLOR, BUTTON, INPUT, COMPARE }
    public static final String[] COMPARISONS={"==","~=","!=","<","<=",">",">="};
    public static final class Field {
        public final String label,initial;public final Kind kind;
        Field(String label,String initial,Kind kind){this.label=label;this.initial=initial;this.kind=kind;}
    }
    public static final class Item {
        public final String id,title,help;public final Field[] fields;public final boolean block;
        Item(String id,String title,String help,boolean block,Field... fields){this.id=id;this.title=title;this.help=help;this.block=block;this.fields=fields;}
    }
    private static Field f(String label,String value,Kind kind){return new Field(label,value,kind);}
    public static final Item[] ITEMS={
        new Item("init","_init · начало игры","Вызывается один раз при запуске. Здесь удобно задать начальные значения.",true),
        new Item("update","_update · правила","PICO-8 вызывает эту функцию 30 раз в секунду. Здесь проверяют ввод и меняют состояние.",true),
        new Item("draw","_draw · изображение","Вызывается для отрисовки кадра. Здесь очищают экран и показывают состояние игры.",true),
        new Item("function","function · своя функция","Назови действие и напиши его тело. Потом его можно вызвать по имени.",true,f("Имя","tick",Kind.NAME)),
        new Item("if","if · условие","Тело выполняется, когда выражение после if истинно.",true,f("Условие Lua","score>=5",Kind.EXPR)),
        new Item("for","for · повторение","Повторяет тело для значений счётчика от начала до конца включительно.",true,f("Счётчик","i",Kind.NAME),f("От","1",Kind.EXPR),f("До","5",Kind.EXPR),f("Шаг","1",Kind.EXPR)),
        new Item("while","while · пока условие","Повторяет тело, пока условие истинно. Оно должно стать ложным, иначе цикл не закончится.",true,f("Условие Lua","false",Kind.EXPR)),
        new Item("set","Переменная · задать","Сохраняет значение в переменной. Дальше её можно читать и изменять.",false,f("Имя","score",Kind.NAME),f("Значение Lua","0",Kind.EXPR)),
        new Item("add","Переменная · прибавить","Меняет уже заданную переменную. += — обычный синтаксис PICO-8.",false,f("Имя","score",Kind.NAME),f("Сколько","1",Kind.EXPR)),
        new Item("button","Кнопка · условие","btnp проверяет нажатие с автоповтором, btn — удержание. Используй внутри _update.",true,f("Кнопка PICO-8","4",Kind.BUTTON),f("Проверка","btnp",Kind.INPUT)),
        new Item("cls","cls · очистить экран","Очищает кадр выбранным цветом. Обычно стоит в начале _draw.",false,f("Цвет","1",Kind.COLOR)),
        new Item("print","print · значение","Показывает значение выражения, например счёт игры.",false,f("Выражение Lua","score",Kind.EXPR),f("X","16",Kind.EXPR),f("Y","60",Kind.EXPR),f("Цвет","7",Kind.COLOR)),
        new Item("text","print · надпись","Показывает текст. Кавычки и обратные слеши будут экранированы автоматически.",false,f("Текст","hello!",Kind.STRING),f("X","16",Kind.EXPR),f("Y","16",Kind.EXPR),f("Цвет","14",Kind.COLOR)),
        new Item("circle","circfill · круг","Рисует заполненный круг по центру и радиусу; это обычная графика PICO-8.",false,f("X","64",Kind.EXPR),f("Y","64",Kind.EXPR),f("Радиус","6",Kind.EXPR),f("Цвет","14",Kind.COLOR)),
        new Item("rect","rectfill · прямоугольник","Рисует заполненный прямоугольник между двумя углами включительно.",false,f("X0","8",Kind.EXPR),f("Y0","8",Kind.EXPR),f("X1","119",Kind.EXPR),f("Y1","119",Kind.EXPR),f("Цвет","12",Kind.COLOR)),
        new Item("sprite","spr · спрайт","Рисует один тайл спрайта 8×8. Размеры и другие параметры можно изменить в Lua.",false,f("Номер","0",Kind.EXPR),f("X","64",Kind.EXPR),f("Y","64",Kind.EXPR)),
        new Item("call","Вызвать функцию","Вызывает функцию без аргументов. Например, _init() может сбросить состояние игры.",false,f("Имя","_init",Kind.NAME)),
        new Item("compare","if · сравнить значения","Сравнивает два значения. Если сравнение истинно, выполняется тело условия.",true,f("Слева","score",Kind.EXPR),f("Сравнение",">=",Kind.COMPARE),f("Справа","5",Kind.EXPR)),
        new Item("sspr","Спрайт · разместить","Выбери область своего листа и место на экране. Вставляй внутри _draw после очистки кадра.",false,f("На листе X","0",Kind.EXPR),f("На листе Y","0",Kind.EXPR),f("Ширина","8",Kind.EXPR),f("Высота","8",Kind.EXPR),f("Экран X","60",Kind.EXPR),f("Экран Y","60",Kind.EXPR)) ,
        new Item("map","map · показать карту","Рисует тайлы карты. Вставляй в _draw после cls(). Размер задаётся в клетках 8×8, положение на экране — в пикселях.",false,f("Карта X","0",Kind.EXPR),f("Карта Y","0",Kind.EXPR),f("Экран X","0",Kind.EXPR),f("Экран Y","0",Kind.EXPR),f("Ширина, кл","16",Kind.EXPR),f("Высота, кл","16",Kind.EXPR))
    };
    public Screen screen=Screen.CATALOG;
    public int selected,field,page,key;
    public boolean replaceAll=true;
    public String input="";
    private String[] values;
    public boolean editing;
    public Kind kind(int index){return editing&&item().fields[index].kind==Kind.COLOR?Kind.EXPR:item().fields[index].kind;}
    public boolean colorChoice(int index){if(item().fields[index].kind!=Kind.COLOR)return false;try{int n=Integer.parseInt(values[index]);return n>=0&&n<=15;}catch(NumberFormatException e){return false;}}
    public int symbolGroup,symbolIndex;
    private LuaSymbols symbols;
    public LuaInsert(){choose(0);}
    public Item item(){return ITEMS[selected];}
    public String value(int i){return values[i];}
    public void choose(int index){
        if(index<0||index>=ITEMS.length)throw new IllegalArgumentException("Unknown snippet");
        selected=index;field=0;values=new String[item().fields.length];
        for(int i=0;i<values.length;i++)values[i]=item().fields[i].initial;
    }
    public void set(int index,String value){
        if(index<0||index>=values.length)throw new IllegalArgumentException("Unknown field");
        validate(kind(index),value);values[index]=value;
    }
    private static void validate(Kind kind,String value){
        if(value==null||value.length()>256||value.indexOf('\n')>=0||value.indexOf('\r')>=0||value.indexOf('\0')>=0)
            throw new IllegalArgumentException("Параметр должен быть одной строкой до 256 символов");
        if(!java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(value))throw new IllegalArgumentException("Недопустимый символ в параметре");
        if(kind!=Kind.STRING&&value.trim().isEmpty())throw new IllegalArgumentException("Введи значение");
        if(kind==Kind.NAME&&(!value.matches("[a-zA-Z_][a-zA-Z_0-9]*")||(" and break do else elseif end false for function if in local nil not or repeat return then true until while ").contains(" "+value+" ")))
            throw new IllegalArgumentException("Имя: латинские буквы, цифры и _. Начни с буквы или _; не используй слово Lua.");
        if(kind==Kind.COLOR||kind==Kind.BUTTON){
            int n;try{n=Integer.parseInt(value);}catch(NumberFormatException e){throw new IllegalArgumentException("Выбери номер стрелками");}
            if(n<0||n>(kind==Kind.COLOR?15:5))throw new IllegalArgumentException("Номер вне списка выбора");
        }
        if(kind==Kind.INPUT&&!value.equals("btn")&&!value.equals("btnp"))throw new IllegalArgumentException("Выбери btn или btnp");
        if(kind==Kind.COMPARE&&!java.util.Arrays.asList(COMPARISONS).contains(value))throw new IllegalArgumentException("Выбери знак сравнения стрелками");
    }
    public void step(int direction){
        if(field>=values.length)return;
        Kind kind=colorChoice(field)?Kind.COLOR:kind(field);
        if(kind==Kind.COMPARE){int at=java.util.Arrays.asList(COMPARISONS).indexOf(values[field]);values[field]=COMPARISONS[Math.floorMod(at+direction,COMPARISONS.length)];return;}
        if(kind==Kind.INPUT){values[field]=values[field].equals("btn")?"btnp":"btn";return;}
        if(kind!=Kind.COLOR&&kind!=Kind.BUTTON&&kind!=Kind.EXPR)return;
        try{int old=Integer.parseInt(values[field]);long n=(long)old+direction;
            int min=kind==Kind.EXPR?-32768:0,max=kind==Kind.COLOR?15:kind==Kind.BUTTON?5:32767;
            values[field]=""+Math.max(min,Math.min(max,n));
        }catch(NumberFormatException e){/* Expressions remain literal; open text input to change them. */}
    }
    public void beginText(){
        if(field>=values.length)return;
        Kind kind=kind(field);if(kind==Kind.COLOR||kind==Kind.BUTTON||kind==Kind.INPUT||kind==Kind.COMPARE){step(1);return;}
        input=values[field];replaceAll=true;page=key=0;screen=Screen.TEXT;
    }
    public void type(String added){
        String next=(replaceAll?"":input)+added;
        if(next.length()>256||next.indexOf('\n')>=0||next.indexOf('\r')>=0)throw new IllegalArgumentException("Параметр: до 256 символов в одной строке");
        input=next;replaceAll=false;
    }
    public void erase(){if(replaceAll)input="";else if(!input.isEmpty())input=input.substring(0,input.offsetByCodePoints(input.length(),-1));replaceAll=false;}
    public void acceptText(){set(field,input);screen=Screen.FIELDS;}
    public boolean canBrowse(){return field<values.length&&(kind(field)==Kind.EXPR||
        (item().fields[field].kind==Kind.NAME&&(item().id.equals("set")||item().id.equals("add")||item().id.equals("call"))));}
    public boolean canBrowseApi(){return canBrowse()&&kind(field)==Kind.EXPR;}
    public void attachSource(String source){symbols=new LuaSymbols(source);}
    public boolean completeSymbols(){return symbols!=null&&symbols.complete;}
    public java.util.List<LuaSymbols.Entry> choices(){
        if(symbols==null||!canBrowse())return java.util.Collections.emptyList();
        return symbolGroup==1?symbols.api():symbols.project(item().id.equals("call"));
    }
    public void beginSymbols(String source){if(!canBrowse())return;attachSource(source);symbolGroup=symbolIndex=0;screen=Screen.SYMBOLS;}
    public void symbolMove(int delta){symbolIndex=Math.max(0,Math.min(Math.max(0,choices().size()-1),symbolIndex+delta));}
    public void symbolTab(){if(canBrowseApi()){symbolGroup=1-symbolGroup;symbolIndex=0;}}
    public void acceptSymbol(){java.util.List<LuaSymbols.Entry> list=choices();if(symbolIndex<list.size()){set(field,list.get(symbolIndex).value);screen=Screen.FIELDS;}}
    public void moveKey(int dx,int dy){key=Math.max(0,Math.min(LuaDraft.PAGES[page].length()-1,(key/10+dy)*10+Math.max(0,Math.min(9,key%10+dx))));}
    public void changePage(int delta){page=(page+delta+3)%3;key=Math.min(key,LuaDraft.PAGES[page].length()-1);}
    public String functionName(){return selected<3?new String[]{"_init","_update","_draw"}[selected]:item().id.equals("function")?values[0]:null;}
    public String code(){
        for(int i=0;i<values.length;i++)validate(kind(i),values[i]);
        String name=functionName();if(name!=null)return "function "+name+"()\n  \nend\n";
        String a=values[0];
        switch(item().id){
            case "if":return "if "+a+" then\n  \nend\n";
            case "compare":return "if "+a+values[1]+values[2]+" then\n  \nend\n";
            case "for":return "for "+a+"="+values[1]+","+values[2]+","+values[3]+" do\n  \nend\n";
            case "while":return "while "+a+" do\n  \nend\n";
            case "set":return a+"="+values[1]+"\n";
            case "add":return a+"+="+values[1]+"\n";
            case "button":return "if "+values[1]+"("+a+") then\n  \nend\n";
            case "cls":return "cls("+a+")\n";
            case "print":case "text":return "print("+(item().id.equals("text")?quote(a):a)+","+values[1]+","+values[2]+","+values[3]+")\n";
            case "circle":return "circfill("+join()+")\n";
            case "rect":return "rectfill("+join()+")\n";
            case "sprite":return "spr("+join()+")\n";
            case "map":return "map("+join()+")\n";
            case "sspr":return "sspr("+join()+")\n";
            case "call":return a+"()\n";
            default:throw new IllegalStateException("Unknown snippet");
        }
    }
    private String join(){StringBuilder b=new StringBuilder();for(String value:values){if(b.length()>0)b.append(',');b.append(value);}return b.toString();}
    static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    public String display(int index){
        String value=values[index];Kind kind=item().fields[index].kind;
        if(kind==Kind.BUTTON)return new String[]{"0 · влево","1 · вправо","2 · вверх","3 · вниз","4 · O","5 · X"}[Integer.parseInt(value)];
        if(kind==Kind.INPUT)return value.equals("btnp")?"btnp · нажатие / повтор":"btn · удержание";
        if(kind==Kind.COMPARE){String[] meanings={"равно","не равно","не равно","меньше","не больше","больше","не меньше"};return value+" · "+meanings[java.util.Arrays.asList(COMPARISONS).indexOf(value)];}
        return value;
    }
    public void write(DataOutputStream out)throws IOException{
        out.writeUTF(item().id);out.writeInt(screen.ordinal());out.writeInt(field);out.writeInt(page);out.writeInt(key);
        out.writeBoolean(replaceAll);out.writeUTF(input);for(String value:values)out.writeUTF(value);
        out.writeInt(symbolGroup);out.writeInt(symbolIndex);
    }
    public static LuaInsert read(DataInputStream in,boolean browserState,boolean editing)throws IOException{
        LuaInsert insert=new LuaInsert();String id=in.readUTF();int found=-1;
        for(int i=0;i<ITEMS.length;i++)if(ITEMS[i].id.equals(id))found=i;
        if(found<0)throw new IOException("snippet id");insert.choose(found);insert.editing=editing;int screen=in.readInt();
        insert.field=in.readInt();insert.page=in.readInt();insert.key=in.readInt();insert.replaceAll=in.readBoolean();insert.input=in.readUTF();
        if(screen<0||screen>=Screen.values().length||insert.field<0||insert.field>insert.values.length||insert.page<0||insert.page>2
            ||insert.key<0||insert.key>=LuaDraft.PAGES[insert.page].length()||insert.input.length()>256
            ||(screen==Screen.TEXT.ordinal()&&insert.field==insert.values.length))throw new IOException("snippet state");
        insert.screen=Screen.values()[screen];for(int i=0;i<insert.values.length;i++)insert.set(i,in.readUTF());
        if(browserState){insert.symbolGroup=in.readInt();insert.symbolIndex=in.readInt();}
        if(insert.symbolGroup<0||insert.symbolGroup>1||insert.symbolIndex<0||insert.symbolIndex>100000
            ||(insert.screen==Screen.SYMBOLS&&(!browserState||!insert.canBrowse()||(insert.symbolGroup==1&&!insert.canBrowseApi()))))throw new IOException("symbol state");
        return insert;
    }
}
