package art.pikoos.lab.core;

/** Maps a decoded Android storage path to a descendant of an authorized SAF tree.
 * This is a name mapping only: the platform must still enforce the read grant. */
public final class LauncherPath {
    private LauncherPath() {}
    public static String document(String path,String treeId) {
        if(path==null||treeId==null)throw new IllegalArgumentException("Нет пути к игре");
        String id;
        if(path.startsWith("/sdcard/"))id="primary:"+path.substring(8);
        else if(path.startsWith("/storage/emulated/0/"))id="primary:"+path.substring(20);
        else if(path.startsWith("/storage/")){
            String rest=path.substring(9);int slash=rest.indexOf('/');
            if(slash<1||!rest.substring(0,slash).matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}"))throw outside();
            id=rest.substring(0,slash)+":"+rest.substring(slash+1);
        }else throw outside();
        validate(id);validate(treeId);
        String prefix=treeId.endsWith(":")?treeId:treeId+"/";
        if(!id.startsWith(prefix)||id.length()==prefix.length())throw outside();
        return id;
    }
    private static void validate(String id){
        int colon=id.indexOf(':');if(colon<1||id.indexOf(':',colon+1)>=0||id.indexOf('\\')>=0||id.indexOf('\0')>=0)throw outside();
        String tail=id.substring(colon+1);
        if(tail.isEmpty())return;
        for(String segment:tail.split("/",-1))if(segment.isEmpty()||segment.equals(".")||segment.equals(".."))throw outside();
    }
    private static IllegalArgumentException outside(){return new IllegalArgumentException("Игра вне подключённой папки. Выбери файл заново.");}
}
