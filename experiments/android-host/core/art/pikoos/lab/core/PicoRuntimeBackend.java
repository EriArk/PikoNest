package art.pikoos.lab.core;

/** Experimental portable port. No Android types or process assumptions. */
public interface PicoRuntimeBackend {
    final class Availability {
        public final boolean launcherPresent;
        public final String launcherVersion;
        public Availability(boolean present, String version) {
            launcherPresent = present;
            launcherVersion = version;
        }
    }

    final class Capabilities {
        public final boolean canLaunch, canStop, canObserveResult, canImportRuntime;
        public Capabilities(boolean launch, boolean stop, boolean result, boolean importRuntime) {
            canLaunch = launch;
            canStop = stop;
            canObserveResult = result;
            canImportRuntime = importRuntime;
        }
    }

    Availability detect();
    Capabilities capabilities();
    /** Launch acceptance is not proof of successful official-runtime execution. */
    void launch(byte[] standardCart) throws Exception;
    default void launch(RuntimeFileSet files)throws Exception{
        throw new UnsupportedOperationException("Обнови адаптер PICO-8 для запуска нескольких картриджей");
    }
    default void launch(byte[] standardCart,CartridgeFormat format)throws Exception{
        if(format!=CartridgeFormat.P8)throw new UnsupportedOperationException("Этот runtime пока не запускает PNG-картриджи");
        launch(standardCart);
    }
    /** False when this backend has no supported remote stop mechanism. */
    boolean stop();
}
