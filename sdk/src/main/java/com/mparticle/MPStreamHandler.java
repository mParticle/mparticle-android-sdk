package com.mparticle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by sdozor on 3/4/14.
 */
final class MPStreamHandler extends MPAbstractStreamHandler {
    //these are the typical classes that Android uses for UrlConnections
    private static final String[] classes = {"libcore.net.http.HttpURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl",
            "org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnection"};

    public MPStreamHandler() {
        super(classes);
    }

    protected final URLConnection openConnection(URL u) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u);
        if (MParticle.getInstance().networkMonitoring && MParticle.getInstance().shouldProcessUrl(u.toString())) {
            return new MPHttpUrlConnection(connection);
        } else {
            return connection;
        }
    }

    protected final URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) super.openConnection(u, proxy);
        if (MParticle.getInstance().networkMonitoring && MParticle.getInstance().shouldProcessUrl(u.toString())) {
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