package cc.tomko.outify;

import android.app.Application;

public class OutifyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        System.loadLibrary("librespot_ffi");
    }
}
