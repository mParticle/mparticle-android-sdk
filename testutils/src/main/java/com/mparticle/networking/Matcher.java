package com.mparticle.networking;

import org.json.JSONObject;

public class Matcher {

    public Matcher() {
    }

    public Matcher(MPUrl url) {
        this.url = url.getFile();
    }

    String url;
    MockServer.UrlMatcher urlMatcher;
    MockServer.JSONMatch bodyMatch;
    boolean keepAfterMatch;
    Long timestamp = System.currentTimeMillis();

    boolean isMatch(MPConnectionTestImpl mockConnection) {
        if (url != null) {
            if (!url.equals(mockConnection.getURL().getFile())) {
                return false;
            }
        }
        if (urlMatcher != null) {
            if (!urlMatcher.isMatch(mockConnection.getURL())) {
                return false;
            }
        }
        if (bodyMatch != null) {
            try {
                if (!bodyMatch.isMatch(new JSONObject(mockConnection.getBody()))) {
                    return false;
                }
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
        return true;
    }

    public Matcher bodyMatch(MockServer.JSONMatch jsonMatch) {
        this.bodyMatch = jsonMatch;
        return this;
    }

    public Matcher urlMatcher(MockServer.UrlMatcher urlMatcher) {
        this.urlMatcher = urlMatcher;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matcher) {
            Matcher matcher = (Matcher) obj;

            if (matcher.bodyMatch == bodyMatch &&
                    matcher.url == url &&
                    matcher.urlMatcher == urlMatcher) {
                return true;
            }
        }
        return false;
    }
}