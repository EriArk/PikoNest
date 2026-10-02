package art.pikoos.lab.core;

/** Small controller text draft. No platform keyboard or storage dependency. */
public final class NameEditor {
    public static final int LIMIT=80;
    private static final String RU="абвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789 -_.!?+";
    private static final String EN="abcdefghijklmnopqrstuvwxyz0123456789 -_.!?+:()/&'=";
    private String text;
    public int key;
    public boolean latin,uppercase=true,replaceAll=true;
    public String warning="";
    public NameEditor(String initial){restoreText(initial);}
    public String text(){return text;}
    public void restoreText(String value){
        if(value==null||value.length()>LIMIT)throw new IllegalArgumentException("Название слишком длинное");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Недопустимый символ в названии");
        text=value;
    }
    public int count(){return (latin?EN:RU).length();}
    public char character(int index){
        if(index<0||index>=count())return 0;
        char c=(latin?EN:RU).charAt(index);return uppercase?Character.toUpperCase(c):c;
    }
    public void move(int dx,int dy){
        int row=key/10,col=key%10;
        row=Math.max(0,Math.min(4,row+dy));col=Math.max(0,Math.min(9,col+dx));
        key=Math.min(count()-1,row*10+col);
    }
    public void language(){latin=!latin;key=Math.min(key,count()-1);}
    public void type(int index){
        char c=character(index);if(c==0)return;key=index;warning="";
        if(replaceAll){text="";replaceAll=false;}
        if(text.length()>=LIMIT){warning="Достигнут предел: 80 символов";return;}
        text+=c;
    }
    public void erase(){
        warning="";
        if(replaceAll)text="";
        else if(!text.isEmpty())text=text.substring(0,text.offsetByCodePoints(text.length(),-1));
        replaceAll=false;
    }
    public String value(){
        String value=text.trim();if(value.isEmpty()){warning="Название не должно быть пустым";return null;}return value;
    }
}
