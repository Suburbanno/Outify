package cc.tomko.outify;

public class LibrespotFfi {

    /**
     * Initializes the LibrespotFfi rust library
     */
    public static native void libInit();

	/**
	 * Returns true if JNI is working correctly.
	 */
	public static native boolean isConnected();
}
