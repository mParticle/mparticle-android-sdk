package com.mparticle.networking;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class Request {
    Map<String, List<String>> headers;
    String url;
    String body;
    MPConnection connection;

    Request(MPConnectionTestImpl connection) {
        headers = connection.getHeaderFields();
        url = connection.getURL().getFile();
        body = connection.getBody();
        this.connection = connection;
    }

    Request(Request request) {
        headers = request.headers;
        url = request.url;
        body = request.body;
        connection = request.connection;
    }

    MPConnection getConnection() {
        return connection;
    }

    public JSONObject getBodyJson() {
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public IdentityRequest asIdentityRequest() {
        return new IdentityRequest(this);
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}