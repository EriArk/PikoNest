package art.pikoos.runtimelab;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.*;
import art.pikoos.lab.core.*;
import java.io.InputStream;

/** Read-only incoming Android transport. Never treats a raw path as a permission. */
final class ExternalCartSource {
    final byte[] bytes;final RuntimeFileSet files;final String name;final CartridgeFormat format;
    ExternalCartSource(Context context,Intent intent,java.util.function.BooleanSupplier cancelled)throws Exception{
        Uri uri=null;
        if(Intent.ACTION_SEND.equals(intent.getAction()))uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        else if(Intent.ACTION_VIEW.equals(intent.getAction())||Intent.ACTION_MAIN.equals(intent.getAction()))uri=intent.getData();
        if(uri==null&&intent.hasExtra("rom")){
            String path=intent.getStringExtra("rom");
            if(path!=null)uri=path.startsWith("/")?Uri.fromFile(new java.io.File(path)):Uri.parse(path);
        }
        if(uri==null)throw new IllegalArgumentException("Лаунчер не передал игру. Выбери .p8 или .p8.png.");
        String scheme=uri.getScheme();
        if("file".equals(scheme)){
            if(uri.getAuthority()!=null&&!uri.getAuthority().isEmpty())throw new IllegalArgumentException("Неизвестный путь. Выбери файл заново.");
            String configured=context.getSharedPreferences("folder-setup",0).getString("GAMES.uri","");
            if(configured.isEmpty())throw new IllegalArgumentException("Папка игр ещё не подключена. Выбери файл заново.");
            Uri tree=Uri.parse(configured);
            if(!"com.android.externalstorage.documents".equals(tree.getAuthority()))throw new IllegalArgumentException("Для этой папки выбери файл заново.");
            String id=LauncherPath.document(uri.getPath(),DocumentsContract.getTreeDocumentId(tree));
            uri=DocumentsContract.buildDocumentUriUsingTree(tree,id);
        }else if(!"content".equals(scheme))throw new IllegalArgumentException("Нужен файл игры: .p8 или .p8.png.");
        if("art.pikoos.runtimelab.carts".equals(uri.getAuthority()))throw new IllegalArgumentException("Выбери исходный файл игры.");
        ContentResolver resolver=context.getContentResolver();String filename=null;
        try(Cursor c=resolver.query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst())filename=c.getString(0);
        }
        if(filename==null)throw new IllegalArgumentException("Не удалось узнать имя картриджа.");
        format=CartridgeFormat.of(filename);name=filename;
        byte[] original;
        try(InputStream in=resolver.openInputStream(uri)){
            if(in==null)throw new IllegalArgumentException("Не удалось прочитать игру.");
            original=CartridgeImport.readBounded(in);
        }
        IncludeSource.Prepared ready=IncludeSource.launch(resolver,context.getSharedPreferences("folder-setup",0).getString("GAMES.uri",""),uri,name,original,cancelled);
        bytes=ready.bytes;files=ready.files;
    }
}
