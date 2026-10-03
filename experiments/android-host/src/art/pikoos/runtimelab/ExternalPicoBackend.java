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
    void checkAvailableForLaunch() throws Exception {
        boolean external=activity instanceof LaunchActivity;
        if(external&&activity.getSharedPreferences("library-ui",0).getBoolean("awaitingReturn",false))
            throw new Exception("Игра уже запущена из PIKOOS. Вернись в неё и заверши игру перед новым запуском.");
        if(!external&&activity.getSharedPreferences("external-launch",0).getBoolean("dispatched",false))
            throw new Exception("Игра запущена из другого лаунчера. Сначала заверши её и вернись в лаунчер.");
    }
    void resume() throws Exception {
        PackageInfo selected=runtime();
        activity.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(selected.packageName,"com.godot.game.GodotAppLauncher")));
    }
    @Override public void launch(byte[] cart) throws Exception {
        launch(cart,CartridgeFormat.P8);
    }
    @Override public void launch(byte[] cart,CartridgeFormat format) throws Exception {
        checkAvailableForLaunch();
        PackageInfo selected = runtime();
        AtomicFile snapshot = new AtomicFile(new File(activity.getFilesDir(), "run"+format.extension));
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
        android.net.Uri uri=format==CartridgeFormat.P8_PNG?CartProvider.PNG:CartProvider.CART;
        intent.setType(format.mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setClipData(ClipData.newRawUri("PICO-8 cartridge", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }
}
