package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.embedded.EmbeddedKitManager;

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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MParticleApiClient implements IMPApiClient {

    private static final String HEADER_SIGNATURE = "x-mp-signature";
    private static final String HEADER_ENVIRONMENT = "x-mp-env";
    private static final String HEADER_KITS = "x-mp-kits";
    private static final String SECURE_SERVICE_SCHEME = TextUtils.isEmpty(BuildConfig.MP_URL) ? "https" : "http";

    private static final String API_HOST = TextUtils.isEmpty(BuildConfig.MP_URL) ? "nativesdks.mparticle.com" : BuildConfig.MP_URL;
    private static final String CONFIG_HOST = TextUtils.isEmpty(BuildConfig.MP_CONFIG_URL) ? "config2.mparticle.com" : BuildConfig.MP_CONFIG_URL;

    private static boolean DEBUGGING = !TextUtils.isEmpty(BuildConfig.MP_URL) && BuildConfig.MP_URL.equals("api-qa.mparticle.com");

    private static final String SERVICE_VERSION_1 = "/v1";
    private static final String SERVICE_VERSION_3 = "/v3";
    private static final String COOKIES = "ck";
    private static final String LTV = "iltv";
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
    private static String supportedKits;
    private SSLSocketFactory socketFactory;
    private String etag = null;
    private String modified = null;
    private static final long THROTTLE = 1000*60*60*2;

    public MParticleApiClient(ConfigManager configManager, SharedPreferences sharedPreferences, Context context) throws MalformedURLException {
        mConfigManager = configManager;
        mApiSecret = configManager.getApiSecret();
        mPreferences = sharedPreferences;
        mApiKey = configManager.getApiKey();
        mConfigUrl = new URL(SECURE_SERVICE_SCHEME, CONFIG_HOST, SERVICE_VERSION_3 + "/" + mApiKey + "/config");
        mEventUrl = new URL(SECURE_SERVICE_SCHEME, API_HOST, SERVICE_VERSION_1 + "/" + mApiKey + "/events");
        mUserAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION;
        mDeviceRampNumber = MPUtility.hashDeviceIdForRamping(
                        Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID).getBytes())
                    .mod(BigInteger.valueOf(100))
                    .intValue();

        supportedKits = getSupportedKitString();
    }

    public void fetchConfig() throws IOException, MPThrottleException, MPConfigException {
        try {
            checkThrottleTime();
            HttpURLConnection connection = (HttpURLConnection) mConfigUrl.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty(HEADER_ENVIRONMENT, Integer.toString(mConfigManager.getEnvironment().getValue()));
            connection.setRequestProperty(HEADER_KITS, supportedKits);
            connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);
            if (etag != null){
                connection.setRequestProperty("If-None-Match", etag);
            }
            if (modified != null){
                connection.setRequestProperty("If-Modified-Since", modified);
            }

            addMessageSignature(connection, null);

            makeUrlRequest(connection, true);

            if (connection.getResponseCode() >= HttpStatus.SC_OK && connection.getResponseCode() < HttpStatus.SC_MULTIPLE_CHOICES) {
                JSONObject response = getJsonResponse(connection);
                parseMparticleJson(response);
                mConfigManager.updateConfig(response);
                etag = connection.getHeaderField("ETag");
                modified = connection.getHeaderField("Last-Modified");
            }else if (connection.getResponseCode() >= HttpStatus.SC_BAD_REQUEST) {
                throw new MPConfigException();
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

    public JSONObject fetchAudiences()  {

        JSONObject response = null;
        try {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Starting Segment Network request");
            HttpURLConnection connection = (HttpURLConnection) getAudienceUrl().openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);

            addMessageSignature(connection, null);
            makeUrlRequest(connection, true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call forbidden: is Segmentation enabled for your account?");
            }
            response =  getJsonResponse(connection);
            parseMparticleJson(response);

        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call failed: " + e.getMessage());
        }
        return response;
    }

    public HttpURLConnection sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
        checkThrottleTime();
        checkRampValue();

        if (DEBUGGING){
            try{
                JSONObject messageJson = new JSONObject(message);
                Log.d("mParticle API request", messageJson.toString(4));
            }catch (Exception e){

            }
        }

        byte[] messageBytes = message.getBytes();
        HttpURLConnection connection = (HttpURLConnection) mEventUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(HTTP.CONTENT_TYPE, "application/json");
        connection.setRequestProperty(HTTP.CONTENT_ENCODING, "gzip");
        connection.setRequestProperty(HTTP.USER_AGENT, mUserAgent);
        connection.setRequestProperty(HEADER_KITS, MParticle.getInstance().internal().getEmbeddedKitManager().getActiveModuleIds());

        addMessageSignature(connection, message);

        if (mConfigManager.getEnvironment().equals(MParticle.Environment.Development)) {
            logUpload(message);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && connection instanceof HttpsURLConnection) {
            try {
                ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
            }catch (Exception e){
            }
        }

        GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(connection.getOutputStream()));
        try {
            zos.write(messageBytes);
        } finally {
            zos.close();
        }

        makeUrlRequest(connection, true);
        if (connection.getResponseCode() >= HttpStatus.SC_OK && connection.getResponseCode() < HttpStatus.SC_MULTIPLE_CHOICES) {
            JSONObject response = getJsonResponse(connection);
            parseMparticleJson(response);
        }
        return connection;
    }

    public HttpURLConnection sendCommand(String commandUrl, String method, String postData, String headers) throws IOException, JSONException {
        ConfigManager.log(MParticle.LogLevel.DEBUG, "Sending data to: " + commandUrl);

        URL url = new URL(commandUrl);
        MParticle.getInstance().excludeUrlFromNetworkPerformanceMeasurement(url.getHost().toString());
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
        return makeUrlRequest(urlConnection, false);

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


    private static Certificate generateCertificate(CertificateFactory certificateFactory, String encodedCertificate) throws IOException, CertificateException {
        Certificate certificate = null;
        InputStream inputStream = new ByteArrayInputStream( encodedCertificate.getBytes() );
        try {
            certificate = certificateFactory.generateCertificate(inputStream);
        }finally {
            inputStream.close();
        }
        return certificate;
    }

    private SSLSocketFactory getSocketFactory() throws Exception{
        if (socketFactory == null){
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("intca", generateCertificate(cf, Constants.GODADDY_INTERMEDIATE_CRT));
            keyStore.setCertificateEntry("rootca", generateCertificate(cf, Constants.GODADDY_ROOT_CRT));
            keyStore.setCertificateEntry("fiddlerroot", generateCertificate(cf, Constants.FIDDLER_ROOT_CRT));

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            socketFactory = context.getSocketFactory();
        }
        return socketFactory;
    }

    public HttpURLConnection makeUrlRequest(HttpURLConnection connection, boolean mParticle) throws IOException{
        //gingerbread seems to dislike pinning w/ godaddy. Being that GB is near-dead anyway, just disable pinning for it.
        if (mParticle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && connection instanceof HttpsURLConnection) {
            try {
                ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
            }catch (Exception e){

            }
        }
        if (mParticle) {
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Bad API request - is the correct API key and secret configured?");
            }
            if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE && !DEBUGGING) {
                setNextAllowedRequestTime();
            }
        }
        return connection;
    }
    static JSONObject getJsonResponse(HttpURLConnection connection) {
        try {
            StringBuilder responseBuilder = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line + '\n');
            }
            in.close();
            return new JSONObject(responseBuilder.toString());
        } catch (IOException ex) {

        } catch (JSONException jse) {

        }
        return null;
    }

    public void parseMparticleJson(JSONObject jsonResponse){
        try {
            if (jsonResponse.has(CONSUMER_INFO)) {
                JSONObject consumerInfo = jsonResponse.getJSONObject(CONSUMER_INFO);
                if (consumerInfo.has(MPID)) {
                    mConfigManager.setMpid(consumerInfo.getLong(MPID));
                }
                if (consumerInfo.has(COOKIES)) {
                    mConfigManager.setCookies(consumerInfo.getJSONObject(COOKIES));
                }
            }
            if (jsonResponse.has(LTV)) {
                BigDecimal serverLtv = new BigDecimal(jsonResponse.getString(LTV));
                BigDecimal mostRecentClientLtc = new BigDecimal(mPreferences.getString(Constants.PrefKeys.LTV, "0"));
                BigDecimal sum = serverLtv.add(mostRecentClientLtc);
                mPreferences.edit().putString(Constants.PrefKeys.LTV, sum.toPlainString()).commit();
            }

        } catch (JSONException jse) {

        }
    }

    private void setNextAllowedRequestTime() {
        long nextTime = System.currentTimeMillis() + THROTTLE;
        mPreferences.edit().putLong(Constants.PrefKeys.NEXT_REQUEST_TIME, nextTime).commit();
    }

    public final class MPThrottleException extends Exception {
        public MPThrottleException() {
            super("mP servers are busy, API connections have been throttled.");
        }
    }

    public final class MPConfigException extends Exception {
        public MPConfigException() {
            super("mP configuration request failed, deferring next batch.");
        }
    }

    public final class MPRampException extends Exception {
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
        if (currentRamp > 0 && currentRamp < 100 &&
                mDeviceRampNumber > mConfigManager.getCurrentRampValue()){
            throw new MPRampException();
        }
    }

    private static String getSupportedKitString(){
        ArrayList<Integer> supportedKitIds = EmbeddedKitManager.BaseEmbeddedKitFactory.getSupportedKits();
        if (supportedKitIds.size() > 0) {
            StringBuilder buffer = new StringBuilder(supportedKitIds.size() * 3);
            Iterator<Integer> it = supportedKitIds.iterator();
            while (it.hasNext()) {
                Integer next = it.next();
                buffer.append(next);
                if (it.hasNext()) {
                    buffer.append(",");
                }
            }
            return buffer.toString();
        }else {
            return "";
        }
    }
}
