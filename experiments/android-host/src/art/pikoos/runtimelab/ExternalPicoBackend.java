package art.pikoos.runtimelab;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

/** Replaceable Android runtime port: integrated app or historical external lab adapter. */
public final class ExternalPicoBackend implements PicoRuntimeBackend {
    static final String RETURN="art.pikoos.RETURN";
    private static final Uri LEGACY_SESSION=Uri.parse("content://art.pikoos.runtimeexperiment.probe/session");
    private Uri endpoint(){return RuntimeInstallation.integrated(activity)?Uri.parse("content://art.pikoos.runtimelab.runtime/session"):LEGACY_SESSION;}
    public static final String PACKAGE = "io.wip.pico8";
    public static final String RESTART_TEST_PACKAGE = "art.pikoos.runtimeexperiment";
    private final Activity activity;
    public ExternalPicoBackend(Activity activity) { this.activity = activity; }
    private PackageInfo runtime() throws PackageManager.NameNotFoundException {
        if(RuntimeInstallation.integrated(activity))return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
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
    SharedPreferences sessionPrefs(){return activity.getSharedPreferences("runtime-session",0);}
    private Uri sessionEndpoint(){return sessionPrefs().getBoolean("integrated",false)?Uri.parse("content://art.pikoos.runtimelab.runtime/session"):LEGACY_SESSION;}
    @Override public boolean recoverSession()throws Exception{
        String token=sessionPrefs().getString("token","");
        Bundle result=activity.getContentResolver().call(sessionEndpoint(),"recoverSession",token,null);
        return result!=null&&result.getBoolean("ok")&&sessionEnded();
    }
    @Override public boolean hasSession(){return !sessionPrefs().getString("token","").isEmpty();}
    @Override public art.pikoos.lab.core.RuntimeSession.Phase sessionPhase(){
        String token=sessionPrefs().getString("token","");
        try(android.database.Cursor rows=activity.getContentResolver().query(sessionEndpoint().buildUpon().appendQueryParameter("token",token).build(),null,null,null,null)){
            if(rows!=null&&rows.moveToFirst())return art.pikoos.lab.core.RuntimeSession.Phase.valueOf(rows.getString(0));
        }catch(Exception ignored){}return art.pikoos.lab.core.RuntimeSession.Phase.UNKNOWN;
    }
    boolean isReturn(Intent intent){return RETURN.equals(intent.getAction())&&intent.getData()!=null&&sessionPrefs().getString("token","").equals(intent.getData().getLastPathSegment())&&sessionEnded();}
    public static final int DIAGNOSTIC_REQUEST=44;
    @Override public void diagnose(byte[] cart,boolean swapAB)throws Exception{
        checkAvailableForLaunch();PackageInfo selected=runtime();
        if(!RuntimeInstallation.integrated(activity)&&(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<5))throw new Exception("Для проверки обнови PIKOOS Runtime Test до версии 5");
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
        if(!detect().launcherPresent)throw new Exception("PICO-8 is not set up. Open runtime setup from Play to import your purchased runtime.");
        String token=sessionPrefs().getString("token","");
        if(!token.isEmpty()&&!sessionEnded())throw new Exception("A game is still open. Return to it and finish the session first.");
        if(!(activity instanceof RuntimeSetupActivity)&&activity.getSharedPreferences("runtime-setup",0).getBoolean("dispatched",false))throw new Exception("Finish the PICO-8 trial launch and return to runtime setup first.");
        boolean external=activity instanceof LaunchActivity;
        if(external&&activity.getSharedPreferences("library-ui",0).getBoolean("awaitingReturn",false))
            throw new Exception("A game is already running from PikoNest. Return to it and finish before launching another.");
        if(!external&&activity.getSharedPreferences("external-launch",0).getBoolean("dispatched",false))
            throw new Exception("A game was started by another launcher. Finish it and return to that launcher first.");
    }
    void checkProbeAvailable()throws Exception{
        checkAvailableForLaunch();
        if(activity.getSharedPreferences("library-ui",0).getBoolean("awaitingReturn",false)||activity.getSharedPreferences("external-launch",0).getBoolean("dispatched",false))throw new Exception("Сначала заверши игру и вернись на полку.");
        PackageInfo selected=runtime();
        if(!RuntimeInstallation.integrated(activity)&&(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<4))throw new Exception("Для проверки архива обнови PIKOOS Runtime Test до версии 4.");
        if(activity.getPackageManager().checkSignatures(activity.getPackageName(),selected.packageName)!=PackageManager.SIGNATURE_MATCH)throw new Exception("Подписи тестовых приложений не совпадают.");
    }
    void launchProbe()throws Exception{
        if(RuntimeInstallation.integrated(activity)){
            try(java.io.InputStream in=activity.getAssets().open("runtime-probe.p8");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);launch(out.toByteArray());return;
            }
        }
        Intent intent=new Intent(Intent.ACTION_SEND).setComponent(new ComponentName(runtime().packageName,"art.pikoos.runtimeexperiment.ProbeActivity"));
        intent.setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM,CartProvider.PROBE);
        intent.setClipData(ClipData.newRawUri("PICO-8 runtime test",CartProvider.PROBE));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);activity.startActivity(intent);
    }
    String probePhase(){
        if(RuntimeInstallation.integrated(activity)){
            art.pikoos.lab.core.RuntimeSession.Phase phase=sessionPhase();
            return phase==art.pikoos.lab.core.RuntimeSession.Phase.INTERRUPTED?"FAILED":phase.name();
        }
        try(android.database.Cursor rows=activity.getContentResolver().query(endpoint().buildUpon().path("/state").build(),null,null,null,null)){
            if(rows!=null&&rows.moveToFirst())return rows.getString(0);
        }catch(Exception ignored){}return "UNKNOWN";
    }
    @Override public void resume() throws Exception {
        if(hasSession()&&sessionEnded())throw new Exception("The previous session has ended. Start a new Test or choose a game.");
        PackageInfo selected=runtime();
        if(hasSession()&&!sessionPrefs().getBoolean("integrated",false))selected=activity.getPackageManager().getPackageInfo(RESTART_TEST_PACKAGE,0);
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
        if(RuntimeInstallation.integrated(activity)&&!RuntimeInstallation.ready(activity))throw new Exception("Import and prepare your Raspberry Pi PICO-8 ZIP in runtime setup first.");
        PackageInfo selected = runtime();
        if(!RuntimeInstallation.integrated(activity)&&(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<7))throw new Exception("Update PIKOOS Runtime Test to version 7 for reliable return.");
        if(fileSet&&(!RuntimeInstallation.integrated(activity)&&(!RESTART_TEST_PACKAGE.equals(selected.packageName)||selected.versionCode<3)))
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
        intent.putExtra("pikoos.swapAB",activity.getSharedPreferences("library-ui",0).getBoolean("swapAB",false));
        intent.putExtra("pikoos.controls",true);
        intent.setClipData(ClipData.newRawUri("PICO-8 cartridge", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String token=java.util.UUID.randomUUID().toString();
        Intent back=new Intent(activity,activity instanceof LaunchActivity?LaunchActivity.class:activity instanceof RuntimeSetupActivity?RuntimeSetupActivity.class:MainActivity.class).setAction(RETURN)
            .setData(Uri.parse("pikoos-return://session/"+token))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent callback=PendingIntent.getActivity(activity,0,back,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT);
        if(!sessionPrefs().edit().putString("token",token).putBoolean("integrated",RuntimeInstallation.integrated(activity)).commit()){callback.cancel();throw new Exception("Не удалось сохранить сеанс");}
        try{
            Bundle result=activity.getContentResolver().call(endpoint(),"beginSession",token,null);
            if(result==null||!result.getBoolean("ok"))throw new Exception("Не удалось подготовить сеанс PICO-8");
            intent.putExtra("pikoos.session",token).putExtra("pikoos.return",callback);
            activity.startActivity(intent);
        }catch(Exception e){
            try{activity.getContentResolver().call(endpoint(),"cancelSession",token,null);}catch(Exception ignored){}
            sessionPrefs().edit().remove("token").commit();callback.cancel();throw e;
        }
    }
}
