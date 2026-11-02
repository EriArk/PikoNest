package art.pikoos.lab.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Android-independent typed preferences; no object deserialization or platform paths. */
public final class BackupPreferences {
    private static void text(DataOutputStream out,String s)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);if(b.length>4*1024*1024)throw new IOException("Preference text too large");out.writeInt(b.length);out.write(b);}
    private static String text(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>4*1024*1024)throw new IOException("Invalid preference text");byte[] b=new byte[n];in.readFully(b);String s=new String(b,StandardCharsets.UTF_8);if(!Arrays.equals(b,s.getBytes(StandardCharsets.UTF_8)))throw new IOException("Invalid preference UTF-8");return s;}
    public static byte[] encode(Map<String,?> values)throws IOException{
        if(values.size()>10000)throw new IOException("Too many preferences");ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x504b5031);out.writeInt(values.size());
        for(String key:new TreeSet<>(values.keySet())){text(out,key);Object v=values.get(key);
            if(v instanceof String){out.writeByte(1);text(out,(String)v);}else if(v instanceof Boolean){out.writeByte(2);out.writeBoolean((Boolean)v);}else if(v instanceof Integer){out.writeByte(3);out.writeInt((Integer)v);}else if(v instanceof Long){out.writeByte(4);out.writeLong((Long)v);}else if(v instanceof Float){out.writeByte(5);out.writeFloat((Float)v);}else if(v instanceof Set){out.writeByte(6);TreeSet<String> strings=new TreeSet<>();for(Object o:(Set<?>)v){if(!(o instanceof String))throw new IOException("Invalid preference set");strings.add((String)o);}if(strings.size()>10000)throw new IOException("Preference set too large");out.writeInt(strings.size());for(String s:strings)text(out,s);}else throw new IOException("Unknown preference type");
            if(bytes.size()>8*1024*1024)throw new IOException("Preferences exceed backup limit");
        }return bytes.toByteArray();
    }
    public static Map<String,Object> decode(byte[] bytes)throws IOException{
        if(bytes.length>8*1024*1024)throw new IOException("Preferences exceed backup limit");DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=0x504b5031)throw new IOException("Unknown preference format");int count=in.readInt();if(count<0||count>10000)throw new IOException("Invalid preference count");Map<String,Object> values=new TreeMap<>();
        for(int i=0;i<count;i++){String key=text(in);Object v;switch(in.readUnsignedByte()){
            case 1:v=text(in);break;case 2:v=in.readBoolean();break;case 3:v=in.readInt();break;case 4:v=in.readLong();break;case 5:v=in.readFloat();break;
            case 6:int n=in.readInt();if(n<0||n>10000)throw new IOException("Invalid set count");Set<String> set=new TreeSet<>();for(int j=0;j<n;j++)if(!set.add(text(in)))throw new IOException("Duplicate set value");v=set;break;
            default:throw new IOException("Unknown preference type");}
            if(values.put(key,v)!=null)throw new IOException("Duplicate preference key");
        }if(in.read()!=-1)throw new IOException("Trailing preference bytes");return values;
    }
}
