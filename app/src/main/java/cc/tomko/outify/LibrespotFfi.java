package cc.tomko.outify;

import android.content.Context;

public class LibrespotFfi {

    /**
     * Initializes the LibrespotFfi rust library
     * @param context used for getting cache dir and files dir
     */
    public static native void libInit(Context context);
}
