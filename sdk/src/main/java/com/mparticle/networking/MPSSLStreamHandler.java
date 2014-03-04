package com.mparticle.networking;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public final class MPSSLStreamHandler extends MPAbstractStreamHandler {
    private static final String[] f = {"libcore.net.http.HttpsURLConnectionImpl", "org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnectionImpl", "org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnection"};

    public MPSSLStreamHandler() {
        super(f);
    }

    protected final URLConnection openConnection(URL u) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(u);
        return new MPHttpsUrlConnection(connection);

    }

    protected final URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(u, proxy);
        return new MPHttpsUrlConnection(connection);
    }

    protected final int getDefaultPort() {
        return 443;
    }

    public final String a() {
        return "https";
    }
}