package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AtomicFile;
import art.pikoos.lab.core.PicoRuntimeBackend;
import art.pikoos.lab.core.CartridgeFormat;
import java.io.File;
import java.io.FileOutputStream;

/** An external-app research adapter, not the production runtime implementation. */
public final class ExternalPicoBackend implements PicoRuntimeBackend {
    public static final String PACKAGE = "io.wip.pico8";
    public static final String RESTART_TEST_PACKAGE = "art.pikoos.runtimeexperiment";
    private final Activity activity;
    public ExternalPicoBackend(Activity activity) { this.activity = activity; }
    private PackageInfo runtime() throws PackageManager.NameNotFoundException {
        try { return activity.getPackageManager().getPackageInfo(RESTART_TEST_PACKAGE, 0); }
        catch (PackageManager.NameNotFoundException absent) {
            return activity.getPackageManager().getPackageInfo(PACKAGE, 0);
        }
    }
    @Override public Availability detect() {
        try {
            PackageInfo info = runtime();
            return new Availability(true, info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            return new Availability(false, "");
        }
    }
    @Override public Capabilities capabilities() {
        return new Capabilities(detect().launcherPresent, false, false, false);
    }
    @Override public boolean stop() { return false; }
    public static final int DIAGNOSTIC_REQUEST=44;
    @Override public void diagnose(byte[] cart,boolean swapAB)throws Exception{
        checkAvailableForLaunch();PackageInfo selected=runtime();
        if(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<5)throw new Exception("Для проверки обнови PIKOOS Runtime Test до версии 5");
        if(activity.getPackageManager().checkSignatures(activity.getPackageName(),selected.packageName)!=PackageManager.SIGNATURE_MATCH)throw new Exception("Подписи приложений не совпадают");
        AtomicFile snapshot=new AtomicFile(new File(activity.getFilesDir(),"diagnostic.p8"));FileOutputStream out=null;
        try{out=snapshot.startWrite();out.write(cart);snapshot.finishWrite(out);}catch(Exception e){snapshot.failWrite(out);throw e;}
        String token=java.util.UUID.randomUUID().toString();
        if(!activity.getSharedPreferences("library-ui",0).edit().putString("diagnosticToken",token).commit())throw new Exception("Не удалось сохранить состояние проверки");
        Intent intent=new Intent(Intent.ACTION_SEND).setComponent(new ComponentName(selected.packageName,"art.pikoos.runtimeexperiment.DiagnosticActivity")).putExtra("token",token);
        intent.setType("text/plain").putExtra(Intent.EXTRA_STREAM,CartProvider.DIAGNOSTIC).putExtra("swapAB",swapAB);
        intent.setClipData(ClipData.newRawUri("PICO-8 trial copy",CartProvider.DIAGNOSTIC));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(intent,DIAGNOSTIC_REQUEST);
    }
    void checkAvailableForLaunch() throws Exception {
        if(activity.getSharedPreferences("runtime-setup",0).getBoolean("dispatched",false))throw new Exception("Сначала заверши пробный запуск PICO-8 и вернись в подключение.");
        boolean external=activity instanceof LaunchActivity;
        if(external&&activity.getSharedPreferences("library-ui",0).getBoolean("awaitingReturn",false))
            throw new Exception("Игра уже запущена из PIKOOS. Вернись в неё и заверши игру перед новым запуском.");
        if(!external&&activity.getSharedPreferences("external-launch",0).getBoolean("dispatched",false))
            throw new Exception("Игра запущена из другого лаунчера. Сначала заверши её и вернись в лаунчер.");
    }
    void checkProbeAvailable()throws Exception{
        checkAvailableForLaunch();
        if(activity.getSharedPreferences("library-ui",0).getBoolean("awaitingReturn",false)||activity.getSharedPreferences("external-launch",0).getBoolean("dispatched",false))throw new Exception("Сначала заверши игру и вернись на полку.");
        PackageInfo selected=runtime();
        if(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<4)throw new Exception("Для проверки архива обнови PIKOOS Runtime Test до версии 4.");
        if(activity.getPackageManager().checkSignatures(activity.getPackageName(),selected.packageName)!=PackageManager.SIGNATURE_MATCH)throw new Exception("Подписи тестовых приложений не совпадают.");
    }
    void launchProbe()throws Exception{
        Intent intent=new Intent(Intent.ACTION_SEND).setComponent(new ComponentName(RESTART_TEST_PACKAGE,"art.pikoos.runtimeexperiment.ProbeActivity"));
        intent.setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM,CartProvider.PROBE);
        intent.setClipData(ClipData.newRawUri("PICO-8 runtime test",CartProvider.PROBE));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);activity.startActivity(intent);
    }
    String probePhase(){
        try(android.database.Cursor rows=activity.getContentResolver().query(android.net.Uri.parse("content://art.pikoos.runtimeexperiment.probe/state"),null,null,null,null)){
            if(rows!=null&&rows.moveToFirst())return rows.getString(0);
        }catch(Exception ignored){}return "UNKNOWN";
    }
    void resume() throws Exception {
        PackageInfo selected=runtime();
        activity.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(selected.packageName,"com.godot.game.GodotAppLauncher")));
    }
    @Override public void launch(byte[] cart) throws Exception {
        launch(cart,CartridgeFormat.P8);
    }
    @Override public void launch(byte[] cart,CartridgeFormat format) throws Exception {
        dispatch(cart,"run"+format.extension,format==CartridgeFormat.P8_PNG?CartProvider.PNG:CartProvider.CART,format.mime,false);
    }
    @Override public void launch(art.pikoos.lab.core.RuntimeFileSet files)throws Exception{
        dispatch(files.transport(),"run.pikoset",CartProvider.FILES,"application/octet-stream",true);
    }
    private void dispatch(byte[] cart,String filename,android.net.Uri uri,String mime,boolean fileSet)throws Exception{
        checkAvailableForLaunch();
        PackageInfo selected = runtime();
        if(fileSet&&(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<3))
            throw new Exception("Обнови PIKOOS Runtime Test до версии 3 для запуска частей игры");
        AtomicFile snapshot = new AtomicFile(new File(activity.getFilesDir(),filename));
        FileOutputStream out = null;
        try {
            out = snapshot.startWrite();
            out.write(cart);
            snapshot.finishWrite(out);
        } catch (Exception e) {
            snapshot.failWrite(out);
            throw e;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setComponent(new ComponentName(selected.packageName, "com.godot.game.GodotAppLauncher"));
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setClipData(ClipData.newRawUri("PICO-8 cartridge", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }
}
