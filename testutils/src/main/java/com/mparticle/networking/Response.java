package com.mparticle.networking;

import java.util.HashMap;
import java.util.Map;

class Response {

    int responseCode = 200;
    String responseBody = "";
    long delay;
    Map<String, String> headers = new HashMap<>();


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

    void setHeader(String key, String value) {
        headers.put(key, value);
    }

    String getHeader(String key) {
        return headers.get(key);
    }

}