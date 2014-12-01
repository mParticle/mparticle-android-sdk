package com.mparticle.internal.np;

import android.os.Build;

import com.mparticle.internal.MPUtility;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
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
    private Class kitKatClientClass;
    private Object kitKatClient;
    private Method kitKatMethod;


    private Constructor proxyOverload;
    private Constructor urlOverload;

    public MPAbstractStreamHandler(String[] classes) {
        //try all of the given classes and hope that one is available
            for (String c : classes) {
                try {
                    urlOverload = MPUtility.getConstructor(c, urlOverloadSignature);
                    urlOverload.setAccessible(true);
                } catch (Exception ex) {

                }
                try {
                    proxyOverload = MPUtility.getConstructor(c, proxyOverloadSignature);
                    proxyOverload.setAccessible(true);
                } catch (Exception ex) {

                }
            }
            if (urlOverload == null) {
                throw new IllegalStateException();
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                kitKatClientClass = Class.forName("com.android.okhttp.OkHttpClient");
                kitKatClient =  kitKatClientClass.newInstance();
                Class[] params = new Class[1];
                params[0] = URL.class;
                kitKatMethod = kitKatClientClass.getDeclaredMethod("open", params);
            }catch (Exception e){
                throw new IllegalStateException();
            }
        }
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return openNewConnection(url, null);
    }

    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if ((url == null) || (proxy == null)) {
            throw new IllegalArgumentException("url or proxy was null!");
        }
        return openNewConnection(url, proxy);
    }

    public URLConnection openNewConnection(URL url, Proxy proxy) throws IOException {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                return (HttpURLConnection)kitKatMethod.invoke(kitKatClient, url);
            }else {
                if (proxy == null || proxyOverload == null) {
                    return (URLConnection) urlOverload.newInstance(new Object[]{url, Integer.valueOf(getDefaultPort())});
                } else {
                    return (URLConnection) proxyOverload.newInstance(new Object[]{url, Integer.valueOf(getDefaultPort()), proxy});
                }
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception ex) {
            //if all else fails, this should always work.
            return new URL(url.toExternalForm()).openConnection();
        }
    }

    protected abstract int getDefaultPort();
}