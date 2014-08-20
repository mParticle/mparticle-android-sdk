package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;


import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by sdozor on 3/25/14.
 */
class MParticleApiClient {

    private static final String HEADER_SIGNATURE = "x-mp-signature";
    private static final String HEADER_ENVIRONMENT = "x-mp-env";
    private static final String HEADER_VARIANT = "x-mp-variant";
    private static final String SECURE_SERVICE_SCHEME = "https";

    private static final String API_HOST = TextUtils.isEmpty(BuildConfig.MP_URL) ? "nativesdks.mparticle.com" : BuildConfig.MP_URL;
    private static final String CONFIG_HOST = TextUtils.isEmpty(BuildConfig.MP_CONFIG_URL) ? "config.mparticle.com" : BuildConfig.MP_CONFIG_URL;

    private static final String SERVICE_VERSION_1 = "/v1";
    private static final String SERVICE_VERSION_2 = "/v2";
    private static final String COOKIES = "ck";
    private static final String CONSUMER_INFO = "ci";
    private static final String MPID = "mpid";

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private final ConfigManager mConfigManager;
    private final String mApiSecret;
    private final URL mConfigUrl;
    private final URL mEventUrl;
    private final String mUserAgent;
    private final SharedPreferences mPreferences;
    private final String mApiKey;
    private final int mDeviceRampNumber;
    private static String variant;
    private SSLSocketFactory socketFactory;
    private static final long THROTTLE = 1000*60*60*2;

    public MParticleApiClient(ConfigManager configManager, SharedPreferences sharedPreferences, Context context) throws MalformedURLException {
        mConfigManager = configManager;
        mApiSecret = configManager.getApiSecret();
        mPreferences = sharedPreferences;
        mApiKey = configManager.getApiKey();
        mConfigUrl = new URL(SECURE_SERVICE_SCHEME, API_HOST, SERVICE_VERSION_2 + "/" + mApiKey + "/config");
        mEventUrl = new URL(SECURE_SERVICE_SCHEME, CONFIG_HOST, SERVICE_VERSION_1 + "/" + mApiKey + "/events");
        mUserAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION;
        mDeviceRampNumber = MPUtility.hashDeviceIdForRamping(
                        Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID).getBytes())
                    .mod(BigInteger.valueOf(100))
                    .intValue();
        variant = BuildConfig.FLAVOR.equals("kahuna") ? BuildConfig.FLAVOR : "main";
    }

    void fetchConfig() throws IOException, MPThrottleException {
        try {
            checkThrottleTime();
            HttpURLConnection connection = (HttpURLConnection) mConfigUrl.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty(HEADER_ENVIRONMENT, Integer.toString(mConfigManager.getEnvironment().getValue()));
            connection.setRequestProperty(HEADER_VARIANT, variant);
            connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);

            addMessageSignature(connection, null);

            ApiResponse response = new ApiResponse(connection);

            if (response.statusCode >= HttpStatus.SC_OK && response.statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
                mConfigManager.updateConfig(response.getJsonResponse());
            }
        } catch (MalformedURLException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error constructing config service URL");
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Config request failed to process response message JSON");
        }
    }

    private URL getAudienceUrl() throws MalformedURLException {
        return new URL(SECURE_SERVICE_SCHEME, API_HOST, SERVICE_VERSION_1 + "/" + mApiKey + "/audience?mpID=" + mConfigManager.getMpid());
    }

    JSONObject fetchAudiences()  {

        JSONObject response = null;
        try {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Starting Segment Network request");
            HttpURLConnection connection = (HttpURLConnection) getAudienceUrl().openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);
            connection.setRequestProperty(HEADER_VARIANT, variant);

            addMessageSignature(connection, null);
            ApiResponse apiResponse = new ApiResponse(connection);
            if (apiResponse.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call forbidden: is Segment enabled for the current mParticle org?");
            }
            response =  apiResponse.getJsonResponse();

        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call failed: " + e.getMessage());
        }
        return response;
    }

    ApiResponse sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
        checkThrottleTime();
        checkRampValue();

        byte[] messageBytes = message.getBytes();
        // POST message to mParticle service
        HttpURLConnection connection = (HttpURLConnection) mEventUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(HTTP.CONTENT_TYPE, "application/json");
        connection.setRequestProperty(HTTP.CONTENT_ENCODING, "gzip");
        connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);
        connection.setRequestProperty(HEADER_VARIANT, variant);

        addMessageSignature(connection, message);

        if (mConfigManager.getEnvironment().equals(MParticle.Environment.Development)) {
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
        ConfigManager.log(MParticle.LogLevel.DEBUG, "Sending data to: " + commandUrl);

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
                byte[] postDataBytes = Base64.decode(postData.getBytes(), Base64.DEFAULT);
                urlConnection.setFixedLengthStreamingMode(postDataBytes.length);
                urlConnection.getOutputStream().write(postDataBytes);
            }
        }
        return new ApiResponse(urlConnection);

    }

    private void logUpload(String message) {
        try {
            JSONObject messageJson = new JSONObject(message);
            if (messageJson.has(Constants.MessageKey.MESSAGES)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.MESSAGES);
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Uploading message batch...");
                for (int i = 0; i < messages.length(); i++) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE));
                }
            } else if (messageJson.has(Constants.MessageKey.HISTORY)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.HISTORY);
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Uploading session history batch...");
                for (int i = 0; i < messages.length(); i++) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE) + " SID: " + ((JSONObject) messages.get(i)).optString(Constants.MessageKey.SESSION_ID));
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
            String path = request.getURL().getFile();
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
            request.setRequestProperty(HEADER_SIGNATURE, hmacSha256Encode(mApiSecret, hashString.toString()));
        } catch (InvalidKeyException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error signing message.");
        } catch (NoSuchAlgorithmException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error signing message.");
        } catch (UnsupportedEncodingException e){
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error signing message.");
        }
    }

    private static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("utf-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes("utf-8")));
    }

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    static void addCookies(JSONObject uploadMessage, ConfigManager manager) {
        try {
            if (uploadMessage != null) {
                uploadMessage.put(COOKIES, manager.getCookies());
            }
        }catch (JSONException jse){

        }
    }

    private final static String GODADDY_INTERMEDIATE_CRT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE0DCCA7igAwIBAgIBBzANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n" +
            "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n" +
            "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTExMDUwMzA3MDAwMFoXDTMxMDUwMzA3\n" +
            "MDAwMFowgbQxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n" +
            "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UE\n" +
            "CxMkaHR0cDovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQD\n" +
            "EypHbyBEYWRkeSBTZWN1cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwggEi\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC54MsQ1K92vdSTYuswZLiBCGzD\n" +
            "BNliF44v/z5lz4/OYuY8UhzaFkVLVat4a2ODYpDOD2lsmcgaFItMzEUz6ojcnqOv\n" +
            "K/6AYZ15V8TPLvQ/MDxdR/yaFrzDN5ZBUY4RS1T4KL7QjL7wMDge87Am+GZHY23e\n" +
            "cSZHjzhHU9FGHbTj3ADqRay9vHHZqm8A29vNMDp5T19MR/gd71vCxJ1gO7GyQ5HY\n" +
            "pDNO6rPWJ0+tJYqlxvTV0KaudAVkV4i1RFXULSo6Pvi4vekyCgKUZMQWOlDxSq7n\n" +
            "eTOvDCAHf+jfBDnCaQJsY1L6d8EbyHSHyLmTGFBUNUtpTrw700kuH9zB0lL7AgMB\n" +
            "AAGjggEaMIIBFjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
            "HQ4EFgQUQMK9J47MNIMwojPX+2yz8LQsgM4wHwYDVR0jBBgwFoAUOpqFBxBnKLbv\n" +
            "9r0FQW4gwZTaD94wNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8v\n" +
            "b2NzcC5nb2RhZGR5LmNvbS8wNQYDVR0fBC4wLDAqoCigJoYkaHR0cDovL2NybC5n\n" +
            "b2RhZGR5LmNvbS9nZHJvb3QtZzIuY3JsMEYGA1UdIAQ/MD0wOwYEVR0gADAzMDEG\n" +
            "CCsGAQUFBwIBFiVodHRwczovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkv\n" +
            "MA0GCSqGSIb3DQEBCwUAA4IBAQAIfmyTEMg4uJapkEv/oV9PBO9sPpyIBslQj6Zz\n" +
            "91cxG7685C/b+LrTW+C05+Z5Yg4MotdqY3MxtfWoSKQ7CC2iXZDXtHwlTxFWMMS2\n" +
            "RJ17LJ3lXubvDGGqv+QqG+6EnriDfcFDzkSnE3ANkR/0yBOtg2DZ2HKocyQetawi\n" +
            "DsoXiWJYRBuriSUBAA/NxBti21G00w9RKpv0vHP8ds42pM3Z2Czqrpv1KrKQ0U11\n" +
            "GIo/ikGQI31bS/6kA1ibRrLDYGCD+H1QQc7CoZDDu+8CL9IVVO5EFdkKrqeKM+2x\n" +
            "LXY2JtwE65/3YR8V3Idv7kaWKK2hJn0KCacuBKONvPi8BDAB\n" +
            "-----END CERTIFICATE-----\n";

    private final static String GODADDY_ROOT_CRT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDxTCCAq2gAwIBAgIBADANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n" +
            "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n" +
            "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTA5MDkwMTAwMDAwMFoXDTM3MTIzMTIz\n" +
            "NTk1OVowgYMxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n" +
            "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjExMC8GA1UE\n" +
            "AxMoR28gRGFkZHkgUm9vdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkgLSBHMjCCASIw\n" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL9xYgjx+lk09xvJGKP3gElY6SKD\n" +
            "E6bFIEMBO4Tx5oVJnyfq9oQbTqC023CYxzIBsQU+B07u9PpPL1kwIuerGVZr4oAH\n" +
            "/PMWdYA5UXvl+TW2dE6pjYIT5LY/qQOD+qK+ihVqf94Lw7YZFAXK6sOoBJQ7Rnwy\n" +
            "DfMAZiLIjWltNowRGLfTshxgtDj6AozO091GB94KPutdfMh8+7ArU6SSYmlRJQVh\n" +
            "GkSBjCypQ5Yj36w6gZoOKcUcqeldHraenjAKOc7xiID7S13MMuyFYkMlNAJWJwGR\n" +
            "tDtwKj9useiciAF9n9T521NtYJ2/LOdYq7hfRvzOxBsDPAnrSTFcaUaz4EcCAwEA\n" +
            "AaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYE\n" +
            "FDqahQcQZyi27/a9BUFuIMGU2g/eMA0GCSqGSIb3DQEBCwUAA4IBAQCZ21151fmX\n" +
            "WWcDYfF+OwYxdS2hII5PZYe096acvNjpL9DbWu7PdIxztDhC2gV7+AJ1uP2lsdeu\n" +
            "9tfeE8tTEH6KRtGX+rcuKxGrkLAngPnon1rpN5+r5N9ss4UXnT3ZJE95kTXWXwTr\n" +
            "gIOrmgIttRD02JDHBHNA7XIloKmf7J6raBKZV8aPEjoJpL1E/QYVN8Gb5DKj7Tjo\n" +
            "2GTzLH4U/ALqn83/B2gX2yKQOC16jdFU8WnjXzPKej17CuPKf1855eJ1usV2GDPO\n" +
            "LPAvTK33sefOT6jEm0pUBsV/fdUID+Ic/n4XuKxe9tQWskMJDE32p2u0mYRlynqI\n" +
            "4uJEvlz36hz1\n" +
            "-----END CERTIFICATE-----\n";

    private SSLSocketFactory getSocketFactory() throws Exception{
        if (socketFactory == null){
            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            Certificate intCa;
            InputStream caInput = new ByteArrayInputStream( GODADDY_INTERMEDIATE_CRT.getBytes() );
            try {
                intCa = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            Certificate rootCa;
            InputStream rooaCaInput = new ByteArrayInputStream( GODADDY_ROOT_CRT.getBytes() );
            try {
                rootCa = cf.generateCertificate(rooaCaInput);
            } finally {
                rooaCaInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("intca", intCa);
            keyStore.setCertificateEntry("rootca", rootCa);
            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            socketFactory = context.getSocketFactory();
        }
        return socketFactory;
    }

    public static String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.startsWith("http")){
            return relativeUrl;
        }else{
            return new StringBuilder(SECURE_SERVICE_SCHEME)
                    .append("://")
                    .append(API_HOST)
                    .append("/")
                    .append(relativeUrl).toString();
        }
    }

    class ApiResponse {
        private static final String LTV = "iltv";
        private int statusCode;
        private JSONObject jsonResponse;
        private HttpURLConnection connection;

        public ApiResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && connection instanceof HttpsURLConnection) {
                try {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
                }catch (Exception e){

                }
            }
            statusCode = connection.getResponseCode();
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Bad API request - is the correct API key and secret configured?");
            }
            if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE){
                setNextValidTime();
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
                            mConfigManager.setMpid(consumerInfo.getLong(MPID));
                        }
                        if (consumerInfo.has(COOKIES)){
                            mConfigManager.setCookies(consumerInfo.getJSONObject(COOKIES));
                        }
                    }
                    if (jsonResponse.has(LTV)){
                        BigDecimal serverLtv = new BigDecimal(jsonResponse.getString(LTV));
                        BigDecimal mostRecentClientLtc = new BigDecimal(mPreferences.getString(Constants.PrefKeys.LTV, "0"));
                        BigDecimal sum = serverLtv.add(mostRecentClientLtc);
                        mPreferences.edit().putString(Constants.PrefKeys.LTV, sum.toPlainString()).commit();
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

    private void setNextValidTime() {
        long nextTime = System.currentTimeMillis() + THROTTLE;
        mPreferences.edit().putLong(Constants.PrefKeys.NEXT_REQUEST_TIME, nextTime).commit();
    }

    final class MPThrottleException extends Exception {
        public MPThrottleException() {
            super("mP servers are busy, API connections have been throttled.");
        }
    }

    final class MPRampException extends Exception {
        public MPRampException() {
            super("This device is being sampled.");
        }
    }

    private void checkThrottleTime() throws MPThrottleException {
        if (System.currentTimeMillis() < mPreferences.getLong(Constants.PrefKeys.NEXT_REQUEST_TIME, 0)){
            throw new MPThrottleException();
        }
    }

    private void checkRampValue() throws MPRampException {
        int currentRamp = mConfigManager.getCurrentRampValue();
        if (currentRamp >= 0 && currentRamp < 100 &&
                mDeviceRampNumber <= mConfigManager.getCurrentRampValue()){
            throw new MPRampException();
        }
    }
}
