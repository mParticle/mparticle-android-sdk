package com.mparticle.networking;

import java.net.MalformedURLException;
import java.net.URL;

public class MPUrlTestImpl extends MPUrl {
    String url;

    public MPUrlTestImpl(String url) {
        this.url = url;
    }

    @Override
    public MPConnection openConnection() {
        return new MPConnectionTestImpl(this);
    }

    @Override
    public String getFile() {
        return url;
    }

    @Override
    public String getAuthority() {
        try {
            return new URL(url).getAuthority();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public String getPath() {
        try {
            return new URL(url).getPath();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
