package art.pikoos.lab.core;

import java.util.*;
import java.security.MessageDigest;

/** Logical replacement through retained versions. Provider names/IDs may change on rename. */
public final class RecoverableReplace {
    public static final class Record {
        public final String token,oldHash,nextHash;
        public Record(String token,String oldHash,String nextHash){
            if(!token.matches("[0-9a-f-]{36}")||!oldHash.matches("[0-9a-f]{64}")||!nextHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid save journal");
            this.token=token;this.oldHash=oldHash;this.nextHash=nextHash;
        }
        public String candidate(){return ".pikonest-next-"+token+".p8";}
        public String backup(){return ".pikonest-before-"+token+".p8";}
    }
    public interface Port {
        byte[] read(String name)throws Exception;
        boolean exists(String name)throws Exception;
        void create(String name,byte[] bytes)throws Exception;
        void rename(String from,String to)throws Exception;
        Record pending()throws Exception;
        void journal(Record record)throws Exception;
        void clear()throws Exception;
    }
    public static String hash(byte[] bytes)throws Exception{
        StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();
    }
    public static void recover(Port port)throws Exception{
        Record r=port.pending();if(r==null)return;
        if(port.exists("game.p8")){
            String actual=hash(port.read("game.p8"));
            if(!actual.equals(r.oldHash)&&!actual.equals(r.nextHash))throw new IllegalStateException("Project changed during interrupted save. All versions kept; reconnect or inspect the files.");
        }else{
            if(!port.exists(r.backup())||!hash(port.read(r.backup())).equals(r.oldHash))throw new IllegalStateException("Previous project version is unavailable. All files and journal kept.");
            port.rename(r.backup(),"game.p8");
            if(!hash(port.read("game.p8")).equals(r.oldHash))throw new IllegalStateException("Could not verify recovered project");
        }
        port.clear();
    }
    public static void replace(Port port,byte[] expected,byte[] next)throws Exception{
        recover(port);
        if(!Arrays.equals(expected,port.read("game.p8")))throw new IllegalStateException("Project changed before save. Source kept.");
        Record r=new Record(UUID.randomUUID().toString(),hash(expected),hash(next));
        port.create(r.candidate(),next);
        if(!hash(port.read(r.candidate())).equals(r.nextHash))throw new IllegalStateException("Could not verify new version. Original kept.");
        port.journal(r);
        if(!Arrays.equals(expected,port.read("game.p8")))throw new IllegalStateException("Source changed while preparing save. Journal and versions kept.");
        port.rename("game.p8",r.backup());
        if(!hash(port.read(r.backup())).equals(r.oldHash))throw new IllegalStateException("Source changed during rename. Versions kept.");
        port.rename(r.candidate(),"game.p8");
        if(!hash(port.read("game.p8")).equals(r.nextHash))throw new IllegalStateException("Could not verify saved project. Versions and journal kept.");
        port.clear();
    }
}
