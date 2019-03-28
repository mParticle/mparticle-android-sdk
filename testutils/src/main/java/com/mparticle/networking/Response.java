package com.mparticle.networking;

class Response {

    Response() { }

    Response(String responseBody) {
        this(200, responseBody);
    }

    Response(int responseCode, String responseBody) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    MockServer.OnRequestCallback onRequestCallback;

    int responseCode = 200;
    String responseBody = "";
    long delay;

    void setRequest(MPConnectionTestImpl connection) {
        if (onRequestCallback != null) {
            onRequestCallback.onRequest(this, connection);
        }
    }
}