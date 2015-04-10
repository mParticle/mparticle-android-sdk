package com.mparticle.internal.np;

import com.mparticle.MParticle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

final class MPStreamHandler extends MPAbstractStreamHandler {
    //these are the typical classes that Android uses for UrlConnections
    private static final String[] classes = {"libcore.net.http.HttpURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnection",
            "com.android.okhttp.internal.http.HttpURLConnectionImpl"};

    public MPStreamHandler() {
        super(classes);
    }

    protected final URLConnection openConnection(URL u) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u);
        if (u != null && MeasuredRequestManager.INSTANCE.shouldProcessUrl(u.toString())) {
            return new MPHttpUrlConnection(connection);
        } else {
            return connection;
        }
    }

    protected final URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u, proxy);
        if (u != null && MeasuredRequestManager.INSTANCE.shouldProcessUrl(u.toString())) {
            return new MPHttpUrlConnection(connection);
        } else {
            return connection;
        }
    }

    @Override
    protected int getDefaultPort() {
        return 80;
    }
}