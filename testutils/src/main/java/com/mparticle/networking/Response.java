package com.mparticle.networking;

import java.util.concurrent.CountDownLatch;

class Response {

    int responseCode = 200;
    String responseBody = "";
    long delay;
    // When set, the response is held until it counts down.
    CountDownLatch release;

    Response() {
    }

    Response(String responseBody) {
        this(200, responseBody);
    }

    Response(int responseCode, String responseBody) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    MockServer.OnRequestCallback onRequestCallback;

    void setRequest(MPConnectionTestImpl connection) {
        if (onRequestCallback != null) {
            onRequestCallback.onRequest(this, connection);
        }
    }
}