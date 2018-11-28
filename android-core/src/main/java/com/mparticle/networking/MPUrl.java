package com.mparticle.networking;

import java.io.IOException;
import java.net.MalformedURLException;

public abstract class MPUrl {

    private static UrlFactory mpUrlFactory = null;

    static void setMPUrlFactory(UrlFactory urlConstructor) {
        mpUrlFactory = urlConstructor;
    }

    public static MPUrl getUrl(String url) throws MalformedURLException {
        if (mpUrlFactory != null) {
            try {
                return mpUrlFactory.getInstance(url);
            } catch (Exception ex) {

            }
        }
        return new MPUrlImpl(url);
    }

    @Override
    public String toString() {
        return getFile();
    }

    public abstract MPConnection openConnection() throws IOException;
    public abstract String getFile();
    public abstract String getAuthority();
    public abstract String getPath();

    interface UrlFactory {
        MPUrl getInstance(String url);
    }
}
