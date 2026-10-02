package art.pikoos.runtimelab;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AtomicFile;
import art.pikoos.lab.core.PicoRuntimeBackend;
import java.io.File;
import java.io.FileOutputStream;

/** An external-app research adapter, not the production runtime implementation. */
public final class ExternalPicoBackend implements PicoRuntimeBackend {
    public static final String PACKAGE = "io.wip.pico8";
    private final Activity activity;
    public ExternalPicoBackend(Activity activity) { this.activity = activity; }
    @Override public Availability detect() {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(PACKAGE, 0);
            return new Availability(true, info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            return new Availability(false, "");
        }
    }
    @Override public Capabilities capabilities() {
        return new Capabilities(detect().launcherPresent, false, false, false);
    }
    @Override public boolean stop() { return false; }
    @Override public void launch(byte[] cart) throws Exception {
        if (!detect().launcherPresent) throw new IllegalStateException("PICO-8 wrapper is not installed");
        AtomicFile snapshot = new AtomicFile(new File(activity.getFilesDir(), "run.p8"));
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
        intent.setComponent(new ComponentName(PACKAGE, "com.godot.game.GodotAppLauncher"));
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, CartProvider.CART);
        intent.setClipData(ClipData.newRawUri("PICO-8 cartridge", CartProvider.CART));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }
}
