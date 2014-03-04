package com.mparticle.networking;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public final class MPStreamHandler extends MPAbstractStreamHandler {
    private static final String[] f = {"libcore.net.http.HttpURLConnectionImpl", "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl", "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnection"};

    public MPStreamHandler() {
        super(f);
    }

    protected final URLConnection openConnection(URL u) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u);
        return new MPHttpUrlConnection(connection);
    }

    protected final URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u, proxy);
        return new MPHttpUrlConnection(connection);

    }

    protected final int getDefaultPort() {
        return 80;
    }

    protected final String a() {
        return "http";
    }
}