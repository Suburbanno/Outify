package cc.tomko.outify;

public class LibrespotFfi {
	static {
		System.loadLibrary("librespot_ffi");
	}

	/**
	 * Returns true if JNI is working correctly.
	 */
	public native boolean isConnected();
}
