package com.mparticle.test;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by sdozor on 1/2/15.
 */
public class MockHttpUrlConnection extends HttpURLConnection {
    private int mockResponseCode = 202;
    protected MockHttpUrlConnection(URL url) {
        super(url);
    }

    public void setResponseCode(int code){
        mockResponseCode = code;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public int getResponseCode() throws IOException {
        Log.d("unit test", "RETURN RESPONSE CODE:" + mockResponseCode);
        return mockResponseCode;
    }
}
