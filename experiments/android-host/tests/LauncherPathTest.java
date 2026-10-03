import art.pikoos.lab.core.LauncherPath;
public final class LauncherPathTest {
    public static void main(String[] args){
        String tree="primary:Documents/PIKOOS/Games";
        check("primary:Documents/PIKOOS/Games/Луна 1.p8.png",LauncherPath.document("/sdcard/Documents/PIKOOS/Games/Луна 1.p8.png",tree));
        check(tree+"/nested/a.p8",LauncherPath.document("/storage/emulated/0/Documents/PIKOOS/Games/nested/a.p8",tree));
        check("ABCD-1234:ROMs/a.p8",LauncherPath.document("/storage/ABCD-1234/ROMs/a.p8","ABCD-1234:ROMs"));
        check("primary:a.p8",LauncherPath.document("/sdcard/a.p8","primary:"));
        for(String path:new String[]{"/sdcard/Documents/PIKOOS/Games2/a.p8","/sdcard/Documents/PIKOOS/Games/../secret.p8","/sdcard/Documents/PIKOOS/Games//a.p8","/data/private.p8","/sdcard/Documents/PIKOOS/Games/a\\b.p8","/storage/emulated/1/Documents/PIKOOS/Games/a.p8","/sdcard/Documents/PIKOOS/Games","/sdcard/Documents/PIKOOS/Games/./a.p8"}){
            try{LauncherPath.document(path,tree);throw new AssertionError(path);}catch(IllegalArgumentException expected){}
        }
        System.out.println("LauncherPathTest passed: storage aliases, SD card, nested/unicode paths and escape rejection");
    }
    static void check(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(actual);}
}
