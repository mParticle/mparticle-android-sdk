package com.mparticle;

import android.content.SharedPreferences;
import android.util.Log;

import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
class MParticleApiClient {

    public static final String HEADER_SIGNATURE = "x-mp-signature";
    public static final String SECURE_SERVICE_SCHEME = "https";
    public static final String SECURE_SERVICE_HOST = "nativesdks.mparticle.com";
    //public static final String SECURE_SERVICE_HOST = "54.236.165.123";
    //public static final String SECURE_SERVICE_HOST = "api-qa.mparticle.com";
    //public static final String SECURE_SERVICE_HOST = "10.0.16.21";
    public static final String SERVICE_VERSION_1 = "/v1";
    public static final String SERVICE_VERSION_2 = "/v2";
    public static final String COOKIES = "ck";
    public static final String CONSUMER_INFO = "ci";
    public static final String MPID = "mpid";
    // From Stack Overflow:
    // http://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private final ConfigManager configManager;
    private final String apiSecret;
    private final URL configUrl;
    private final URL batchUploadUrl;
    private final String userAgent;
    private final SharedPreferences sharedPreferences;

    public MParticleApiClient(ConfigManager configManager, String key, String secret, SharedPreferences sharedPreferences) throws MalformedURLException {
        this.configManager = configManager;
        this.apiSecret = secret;
        this.sharedPreferences = sharedPreferences;

        this.configUrl = new URL(SECURE_SERVICE_SCHEME, SECURE_SERVICE_HOST, SERVICE_VERSION_2 + "/" + key + "/config");
        this.batchUploadUrl = new URL(SECURE_SERVICE_SCHEME, SECURE_SERVICE_HOST, SERVICE_VERSION_1 + "/" + key + "/events");
        this.userAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION;
    }

    void fetchConfig() throws IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) configUrl.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty(HTTP.USER_AGENT, userAgent);

            addMessageSignature(connection, null);

            ApiResponse response = new ApiResponse(connection);

            if (response.statusCode >= HttpStatus.SC_OK && response.statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
                configManager.updateConfig(response.getJsonResponse());
            }
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, "Error constructing config service URL", e);
        } catch (JSONException e) {
            Log.w(Constants.LOG_TAG, "Config request failed to process response message JSON");
        }
    }

    ApiResponse sendMessageBatch(String message) throws IOException {
        byte[] messageBytes = message.getBytes();
        // POST message to mParticle service
        HttpURLConnection connection = (HttpURLConnection) batchUploadUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(HTTP.CONTENT_TYPE, "application/json");
        connection.setRequestProperty(HTTP.CONTENT_ENCODING, "gzip");
        connection.setRequestProperty(HTTP.USER_AGENT, userAgent);

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

    ApiResponse sendCommand(String commandUrl, String method, String postData, String headers) throws IOException, JSONException {
        if (configManager.isDebug()) {
            Log.d(Constants.LOG_TAG, "Sending data to: " + commandUrl);
        }
        URL url = new URL(commandUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        if (headers != null && headers.length() > 0) {
            JSONObject headersJSON = new JSONObject(headers);
            for (Iterator<?> iter = headersJSON.keys(); iter.hasNext(); ) {
                String headerName = (String) iter.next();
                String headerValue = headersJSON.getString(headerName);
                urlConnection.setRequestProperty(headerName, headerValue);
            }
        }
        if ("POST".equalsIgnoreCase(method)) {
            urlConnection.setDoOutput(true);
            if (postData != null && postData.length() > 0) {
                byte[] postDataBytes = Base64.decode(postData.getBytes());
                urlConnection.setFixedLengthStreamingMode(postDataBytes.length);
                urlConnection.getOutputStream().write(postDataBytes);
            }
        }
        return new ApiResponse(urlConnection);

    }

    private void logUpload(String message) {
        Log.d(Constants.LOG_TAG, "Uploading data to mParticle server:");
        try {
            JSONObject messageJson = new JSONObject(message);
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
            StringBuilder hashString = new StringBuilder()
                                            .append(method)
                                            .append("\n")
                                            .append(dateHeader)
                                            .append("\n")
                                            .append(path);
            if (message != null) {
                hashString.append(message);
            }
            request.setRequestProperty(HTTP.DATE_HEADER, dateHeader);
            request.setRequestProperty(HEADER_SIGNATURE, hmacSha256Encode(apiSecret, hashString.toString()));
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

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public static void addCookies(JSONObject uploadMessage, ConfigManager manager) {
        try {
            if (uploadMessage != null) {
                uploadMessage.put(COOKIES, manager.getCookies());
            }
        }catch (JSONException jse){

        }
    }

    class ApiResponse {
        private static final String LTV = "iltv";
        private int statusCode;
        private JSONObject jsonResponse;
        private HttpURLConnection connection;

        public ApiResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            statusCode = connection.getResponseCode();
            if (statusCode == HttpStatus.SC_BAD_REQUEST && configManager.isDebug()){
                Log.e(Constants.LOG_TAG, "Bad API request - is the correct API key and secret configured?");
            }
        }

        boolean shouldDelete() {
            return HttpStatus.SC_ACCEPTED == statusCode ||
                    (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
                    in.close();
                    jsonResponse = new JSONObject(responseBuilder.toString());
                    if (jsonResponse.has(CONSUMER_INFO)) {
                        JSONObject consumerInfo = jsonResponse.getJSONObject(CONSUMER_INFO);
                        if (consumerInfo.has(MPID)) {
                            configManager.setMpid(consumerInfo.getLong(MPID));
                        }
                        if (consumerInfo.has(COOKIES)){
                            configManager.setCookies(consumerInfo.getJSONObject(COOKIES));
                        }
                    }
                    if (jsonResponse.has(LTV)){
                        BigDecimal serverLtv = new BigDecimal(jsonResponse.getString(LTV));
                        BigDecimal mostRecentClientLtc = new BigDecimal(sharedPreferences.getString(Constants.PrefKeys.LTV, "0"));
                        BigDecimal sum = serverLtv.add(mostRecentClientLtc);
                        sharedPreferences.edit().putString(Constants.PrefKeys.LTV, sum.toPlainString()).commit();
                    }

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
