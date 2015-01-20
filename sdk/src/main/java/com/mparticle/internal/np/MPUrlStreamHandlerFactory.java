package com.mparticle.internal.np;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class MPUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public final URLStreamHandler createURLStreamHandler(String protocol) {
        if ("https".equalsIgnoreCase(protocol)) {
            return new MPSSLStreamHandler();
        } else if ("http".equalsIgnoreCase(protocol)) {
            return new MPStreamHandler();
        }
        return null;
    }
}
