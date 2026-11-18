package com.mparticle.networking;

import android.os.Handler;
import android.os.Looper;

public class CallbackResponse {
    public final MockServer.RequestReceivedCallback callback;

    public CallbackResponse(MockServer.RequestReceivedCallback callback) {
        this.callback = callback;
    }

    public void invokeCallback(final MPConnectionTestImpl connection) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            public void run() {
                try {
                    callback.onRequestReceived(new Request(connection));
                } catch (Throwable t) {
                    // Thrown here, on the main looper, it would end the looper's thread instead of failing the test,
                    // and every later test that waits on the main looper would hang. The test fails in its @After.
                    MockServer.recordCallbackFailure(t);
                }
            }
        });
    }
}
