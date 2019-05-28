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
                callback.onRequestReceived(new Request(connection));
            }
        });
    }
}
