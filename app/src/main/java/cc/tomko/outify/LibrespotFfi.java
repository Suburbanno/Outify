package cc.tomko.outify;

public class LibrespotFfi {
    /**
     * Initializes the Rust backend library
     */
    public static native void libInit(int runtime_size);

	/**
	 * Returns true if JNI is working correctly.
	 */
	public static native boolean isConnected();
}
