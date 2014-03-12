package com.mparticle;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Created by sdozor on 3/4/14.
 */
abstract class MPAbstractStreamHandler extends URLStreamHandler {
    public static final String[] proxyOverloadSignature = {"java.net.URL", "int", "java.net.Proxy"};
    public static final String[] urlOverloadSignature = {"java.net.URL", "int"};

    private Constructor proxyOverload;
    private Constructor urlOverload;

    public MPAbstractStreamHandler(String[] classes) {
        //try all of the given classes and hope that one is available
        for (String c : classes) {
            try {
                urlOverload = MPUtility.getConstructor(c, urlOverloadSignature);
                proxyOverload = MPUtility.getConstructor(c, proxyOverloadSignature);
                urlOverload.setAccessible(true);
                proxyOverload.setAccessible(true);
            } catch (ClassNotFoundException ex) {

            }
        }
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return openNewConnection(url, null);
    }

    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if ((url == null) || (proxy == null))
            throw new IllegalArgumentException("url or proxy was null!");
        return openNewConnection(url, proxy);
    }

    private URLConnection openNewConnection(URL paramURL, Proxy paramProxy) throws IOException {
        try {
            if (paramProxy == null)
                return (URLConnection) urlOverload.newInstance(new Object[]{paramURL, Integer.valueOf(getDefaultPort())});
            else
                return (URLConnection) proxyOverload.newInstance(new Object[]{paramURL, Integer.valueOf(getDefaultPort()), paramProxy});
        } catch (Exception ex) {
            //if all else fails, this should always work.
            return new URL(paramURL.toExternalForm()).openConnection();
        }
    }

    protected abstract int getDefaultPort();
}