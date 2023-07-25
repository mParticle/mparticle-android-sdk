package com.mparticle.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;

public abstract class MPUrl {

    private static UrlFactory mpUrlFactory = null;
    private MPUrl defaultUrl;

    static void setMPUrlFactory(UrlFactory urlConstructor) {
        mpUrlFactory = urlConstructor;
    }

    public static MPUrl getUrl(String url, @Nullable MPUrl defaultUrl) throws MalformedURLException {
        if (mpUrlFactory != null) {
            try {
                return mpUrlFactory.getInstance(url)
                        .setDefaultUrl(defaultUrl);
            } catch (Exception ex) {

            }
        }
        return new MPUrlImpl(url)
                .setDefaultUrl(defaultUrl);
    }

    @Override
    public String toString() {
        return getFile();
    }

    public abstract MPConnection openConnection() throws IOException;

    public abstract String getFile();

    public abstract String getAuthority();

    public abstract String getPath();

    /**
     * returns an instance of the Default URL, if NetworkOptions is being used to override it. Otherwise,
     * a reference to itself will be returned
     *
     * @return an MPUrl instance with the the default URL
     */
    @NonNull
    public MPUrl getDefaultUrl() {
        if (defaultUrl != null) {
            return defaultUrl;
        } else {
            return this;
        }
    }

    MPUrl setDefaultUrl(@Nullable MPUrl url) {
        this.defaultUrl = url;
        return this;
    }

    interface UrlFactory {
        MPUrl getInstance(String url);
    }
}
