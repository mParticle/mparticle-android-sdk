package com.mparticle;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.apache.http.HttpHost;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by sdozor on 3/25/14.
 */
public class MParticleApiClient {
    public static final String HEADER_SIGNATURE = "x-mp-signature";

    public static final String SECURE_SERVICE_SCHEME = "https";
    //public static final String SECURE_SERVICE_HOST = "nativesdks.mparticle.com";
    public static final String SECURE_SERVICE_HOST = "api-qa.mparticle.com";
    private final Context context;
    private final ConfigManager configManager;
    private final String apiKey;
    private final String apiSecret;
    private final URL configUrl;
    private final URL batchUploadUrl;

    // public static final String SECURE_SERVICE_HOST = "10.0.16.21";

    private boolean mAccessNetworkStateAvailable = true;
    public static final String SERVICE_VERSION = "v1";
    private HttpHost mProxyHost;
    private Proxy mProxy;

    public MParticleApiClient(Context context, ConfigManager configManager, String key, String secret) throws MalformedURLException {
        this.context = context;
        this.configManager = configManager;
        this.apiKey = key;
        this.apiSecret = secret;

        this.configUrl = new URL(SECURE_SERVICE_SCHEME, SECURE_SERVICE_HOST, SERVICE_VERSION + "/" + apiKey + "/config");
        this.batchUploadUrl = new URL(SECURE_SERVICE_SCHEME, SECURE_SERVICE_HOST, SERVICE_VERSION + "/" + apiKey + "/events");

        if (PackageManager.PERMISSION_GRANTED != this.context
                .checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {
            Log.w(Constants.LOG_TAG, "Application manifest missing ACCESS_NETWORK_STATE permission");
            mAccessNetworkStateAvailable = false;
        }
    }

    void fetchConfig() throws IOException{
        try {
            HttpURLConnection connection = (HttpURLConnection) configUrl.openConnection();
            if (configManager.isCompressionEnabled()) {
                connection.setRequestProperty("Accept-Encoding", "gzip");
            }
            addMessageSignature(connection, null);

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder responseBuilder = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line + '\n');
                }
                JSONObject responseJSON = new JSONObject(responseBuilder.toString());
                configManager.updateConfig(responseJSON);

            }
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, "Error constructing config service URL", e);
        } catch (JSONException e) {
            Log.w(Constants.LOG_TAG, "Config request failed to process response message JSON");
        }
    }

    public void setConnectionProxy(String host, int port) {
        // HttpClient and UrlConnection use separate proxies
        mProxyHost = new HttpHost(host, port);
        mProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    ApiResponse sendMessageBatch(String message) throws IOException {
        byte[] messageBytes = message.getBytes();
        // POST message to mParticle service
        HttpURLConnection connection = (HttpURLConnection) batchUploadUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-type", "application/json");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", null);
        if (!configManager.isCompressionEnabled()) {
            connection.setRequestProperty("Accept-Encoding", "identity");
        }

        addMessageSignature(connection, message);

        if (configManager.isDebug()) {
            logUpload(message);
        }

        GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(connection.getOutputStream()));
        try {
            zos.write(messageBytes);
        } finally {
            zos.close();
        }

        return new ApiResponse(connection);
    }

    private void logUpload(String message) {
        Log.d(Constants.LOG_TAG, "Uploading data to mParticle server:");
        try {
            JSONObject messageJson = new JSONObject(message);
            //   Log.bytesOut(TAG, messageJson.toString(4));
            if (messageJson.has(Constants.MessageKey.MESSAGES)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.MESSAGES);
                Log.d(Constants.LOG_TAG, "SENDING MESSAGES");
                for (int i = 0; i < messages.length(); i++) {
                    Log.d(Constants.LOG_TAG, "Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE));
                }
            } else if (messageJson.has(Constants.MessageKey.HISTORY)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.HISTORY);
                Log.d(Constants.LOG_TAG, "SENDING HISTORY");
                for (int i = 0; i < messages.length(); i++) {

                    Log.d(Constants.LOG_TAG, "Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE) + " SID: " + ((JSONObject) messages.get(i)).optString(Constants.MessageKey.SESSION_ID));
                    // Log.bytesOut(TAG, ((JSONObject)messages.get(i)).toString(4));
                }
            }
        } catch (JSONException jse) {

        }
    }

    private void addMessageSignature(HttpURLConnection request, String message) {
        try {
            String method = request.getRequestMethod();
            String dateHeader = DateUtils.formatDate(new Date());
            if (dateHeader.length() > DateUtils.PATTERN_RFC1123.length()) {
                // handle a problem on some devices where TZ offset is appended
                dateHeader = dateHeader.substring(0, DateUtils.PATTERN_RFC1123.length());
            }
            String path = request.getURL().getPath();
            String signatureMessage = method + "\n" + dateHeader + "\n" + path;
            if ("POST".equalsIgnoreCase(method) && null != message) {
                signatureMessage += message;
            }
            request.setRequestProperty("Date", dateHeader);
            request.setRequestProperty(HEADER_SIGNATURE, hmacSha256Encode(apiSecret, signatureMessage));
        } catch (InvalidKeyException e) {
            Log.e(Constants.LOG_TAG, "Error signing message", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(Constants.LOG_TAG, "Error signing message", e);
        }
    }

    // From Stack Overflow:
    // http://stackoverflow.com/questions/7124735/hmac-sha256-algorithm-for-signature-calculation
    private static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException,
            InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes()));
    }

    // From Stack Overflow:
    // http://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public ApiResponse sendCommand(String commandUrl, String method, String postData, String headers) throws IOException, JSONException {
        if (configManager.isDebug()) {
            Log.d(Constants.LOG_TAG, "Sending data to: " + commandUrl);
        }
        URL url = new URL(commandUrl);
        HttpURLConnection urlConnection;
        if (mProxy == null) {
            urlConnection = (HttpURLConnection) url.openConnection();
        } else {
            urlConnection = (HttpURLConnection) url.openConnection(mProxy);
        }
        try {
            JSONObject headersJSON = new JSONObject(headers);
            for (Iterator<?> iter = headersJSON.keys(); iter.hasNext(); ) {
                String headerName = (String) iter.next();
                String headerValue = headersJSON.getString(headerName);
                urlConnection.setRequestProperty(headerName, headerValue);
            }
            if ("POST".equalsIgnoreCase(method)) {
                urlConnection.setDoOutput(true);
                byte[] postDataBytes = Base64.decode(postData.getBytes());
                urlConnection.setFixedLengthStreamingMode(postDataBytes.length);
                urlConnection.getOutputStream().write(postDataBytes);
            }
            return new ApiResponse(urlConnection);
        } finally {
            urlConnection.disconnect();
        }
    }

    class ApiResponse {
        private int statusCode;
        private JSONObject jsonResponse;
        private HttpURLConnection connection;

        public ApiResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            statusCode = connection.getResponseCode();
        }

        boolean shouldDelete() {
            return 202 == statusCode || (statusCode >= 400 && statusCode < 500);
        }

        JSONObject getJsonResponse() {
            if (jsonResponse == null) {
                try {
                    StringBuilder responseBuilder = new StringBuilder();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseBuilder.append(line + '\n');
                    }
                    jsonResponse = new JSONObject(responseBuilder.toString());
                } catch (IOException ex) {

                } catch (JSONException jse) {

                }
            }
            return jsonResponse;
        }

        public int getResponseCode() {
            return statusCode;
        }
    }

}
