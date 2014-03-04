package com.mparticle.networking;

import com.mparticle.MPUtility;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public abstract class MPAbstractStreamHandler extends URLStreamHandler {
    public static final String[] a = {"java.net.URL", "int", "java.net.Proxy"};
    public static final String[] b = {"java.net.URL", "int"};

    private Constructor g;
    private Constructor f;

    public MPAbstractStreamHandler(String[] paramArrayOfString) {
        this(paramArrayOfString, a, b);
    }

    private MPAbstractStreamHandler(String[] paramArrayOfString1, String[] paramArrayOfString2, String[] paramArrayOfString3){
        int i = 0;
        while (i < paramArrayOfString1.length) {
            try {
                f = MPUtility.getConstructor(paramArrayOfString1[i], paramArrayOfString3);
                g = MPUtility.getConstructor(paramArrayOfString1[i], paramArrayOfString2);
                f.setAccessible(true);
                g.setAccessible(true);
            } catch (ClassNotFoundException localClassNotFoundException) {

            } finally {
                i++;
            }
        }
    }

    protected URLConnection openConnection(URL u) throws IOException {
        return a(u, null);
    }

    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if ((url == null) || (proxy == null))
            throw new IllegalArgumentException("url == null || proxy == null");
        return a(url, proxy);
    }

    private URLConnection a(URL paramURL, Proxy paramProxy) throws IOException {

        try {
            if (paramProxy == null)
                return (URLConnection) this.f.newInstance(new Object[]{paramURL, Integer.valueOf(getDefaultPort())});
            else
                return (URLConnection) this.g.newInstance(new Object[]{paramURL, Integer.valueOf(getDefaultPort()), paramProxy});
        } catch (Exception ex) {
            return new URL(paramURL.toExternalForm()).openConnection();
        }
    }

    protected abstract int getDefaultPort();

    protected abstract String a();
}