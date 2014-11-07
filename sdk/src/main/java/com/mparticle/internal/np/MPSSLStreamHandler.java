package com.mparticle.internal.np;

import com.mparticle.MParticle;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sdozor on 3/4/14.
 */
final class MPSSLStreamHandler extends MPAbstractStreamHandler {
    //these are the typical classes that Android uses for UrlConnections
    private static final String[] classes = {"libcore.net.http.HttpsURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.https.HttpsURLConnection",
            "com.android.okhttp.internal.http.HttpURLConnectionImpl"};

    public MPSSLStreamHandler() {
        super(classes);
    }

    protected final URLConnection openConnection(URL u) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(u);
        if (MParticle.getInstance().internal().shouldProcessUrl(u.toString())) {
            return new MPHttpsUrlConnection(connection);
        } else {
            return connection;
        }

    }

    protected final URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(u, proxy);
        if (MParticle.getInstance().internal().shouldProcessUrl(u.toString())) {
            return new MPHttpsUrlConnection(connection);
        } else {
            return connection;
        }
    }

    @Override
    protected int getDefaultPort() {
        return 443;
    }
}