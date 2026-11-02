package art.pikoos.runtimelab;

import android.app.Application;
import android.content.Context;

/** Gate normal entry while a cold-start restore needs its progress/recovery screen. */
public final class PikoApplication extends Application {
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(base);if(BackupRecovery.pending(base))BackupRecovery.problem="Finishing the requested restore. Both versions are retained.";}
}
