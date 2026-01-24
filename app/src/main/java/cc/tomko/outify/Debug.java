package cc.tomko.outify;

import java.util.concurrent.CompletableFuture;

public class Debug {
    public native void debug1(String access_token);

    public native CompletableFuture debugAsync();
    public native CompletableFuture debugAsyncErr();
}
