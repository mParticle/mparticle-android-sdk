package com.mparticle.integrationtests;

import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** WireMock's admin API, reached over the same HTTPS port the SDK uses. */
final class WireMockAdmin {
    static final String HOST = "10.0.2.2:" + port();

    private WireMockAdmin() {
    }

    private static String port() {
        String port = InstrumentationRegistry.getArguments().getString("wireMockHttpsPort");
        return port == null ? "18443" : port;
    }

    static void resetRequests() throws IOException {
        call("DELETE", "/__admin/requests", null);
    }

    /** Drops stubs added by {@link #stub} and reloads the ones in wiremock/mappings. */
    static void resetMappings() throws IOException {
        call("POST", "/__admin/mappings/reset", null);
    }

    /** Adds a stub for the current test only; see {@link #resetMappings}. */
    static void stub(JSONObject mapping) throws IOException {
        call("POST", "/__admin/mappings", mapping.toString());
    }

    /** Every request WireMock has received since the last reset, oldest first. */
    static JSONArray requests() throws IOException, JSONException {
        JSONArray newestFirst = new JSONObject(call("GET", "/__admin/requests", null)).getJSONArray("requests");
        JSONArray oldestFirst = new JSONArray();
        for (int i = newestFirst.length() - 1; i >= 0; i--) {
            oldestFirst.put(newestFirst.getJSONObject(i).getJSONObject("request"));
        }
        return oldestFirst;
    }

    private static String call(String method, String path, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + HOST + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (java.io.OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            if (status / 100 != 2) {
                throw new IOException(method + " " + path + " returned " + status);
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(RequestNormalizer.readAll(in), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
