package com.mparticle.networking;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class MPUrlImpl extends MPUrl {
    private URL url;

    MPUrlImpl(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    MPUrlImpl(URL url) {
        this.url = url;
    }

    @Override
    public MPConnection openConnection() throws IOException {
        return new MPConnectionImpl((HttpURLConnection) url.openConnection(), this);
    }

    @Override
    public String getFile() {
        return url.getFile();
    }

    @Override
    public String getAuthority() {
        return url.getAuthority();
    }

    @Override
    public String getPath() {
        return url.getPath();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MPUrlImpl) {
            return url.equals(((MPUrlImpl) obj).url);
        }
        return url.equals(obj);
    }
}
