package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Class responsible for all network communication to the mParticle SDK server.
 *
 */
public class MParticleApiClientImpl implements MParticleApiClient {

    /**
     * Signature header used for authentication with the client key/secret
     */
    private static final String HEADER_SIGNATURE = "x-mp-signature";
    /**
     * Environment header used to tell the SDK server if this is a development or production request
     */
    private static final String HEADER_ENVIRONMENT = "x-mp-env";
    /**
     * Embedded kit header used to tell both the supported EKs (/config), and the currently active EKs (/events)
     */
    private static final String HEADER_KITS = "x-mp-kits";
    private static final String HEADER_BUNDLED_KITS = "x-mp-bundled-kits";
    private static final String SECURE_SERVICE_SCHEME = MPUtility.isEmpty(BuildConfig.MP_URL) ? "https" : "http";

    private static final String API_HOST = MPUtility.isEmpty(BuildConfig.MP_URL) ? "nativesdks.mparticle.com" : BuildConfig.MP_URL;
    private static final String CONFIG_HOST = MPUtility.isEmpty(BuildConfig.MP_CONFIG_URL) ? "config2.mparticle.com" : BuildConfig.MP_CONFIG_URL;

    private static final String SERVICE_VERSION_1 = "/v1";
    private static final String SERVICE_VERSION_4 = "/v4";
    static final int HTTP_TOO_MANY_REQUESTS = 429;

    /**
     * Crucial LTV value key used to sync LTV between the SDK and the SDK server whenever LTV has changed.
     */
    private static final String LTV = "iltv";
    /**
     * Wrapper around cookies, MPID, and other server-response info that requires parsing.
     */
    private static final String CONSUMER_INFO = "ci";



    private final ConfigManager mConfigManager;
    private final String mApiSecret;
    private URL mConfigUrl;
    private URL mEventUrl;
    private final String mUserAgent;
    private final SharedPreferences mPreferences;
    private final String mApiKey;
    private final Context mContext;
    Integer mDeviceRampNumber = null;
    private static String mSupportedKits;
    private SSLSocketFactory socketFactory;
    private JSONObject mCurrentCookies;
    /**
     * Default throttle time - in the worst case scenario if the server is busy, the soonest
     * the SDK will attempt to contact the server again will be after this 2 hour window.
     */
    static final long DEFAULT_THROTTLE_MILLIS = 1000*60*60*2;
    static final long MAX_THROTTLE_MILLIS = 1000*60*60*24;
    private boolean alreadyWarned;

    public MParticleApiClientImpl(ConfigManager configManager, SharedPreferences sharedPreferences, Context context) throws MalformedURLException {
        mContext = context;
        mConfigManager = configManager;
        mApiSecret = configManager.getApiSecret();
        mPreferences = sharedPreferences;
        mApiKey = configManager.getApiKey();
        mUserAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION;
    }

    /**
     * Only used for unit testing.
     */
    void setConfigUrl(URL configUrl) {
        mConfigUrl = configUrl;
    }

    static void setSupportedKitString(String supportedKitString) {
        mSupportedKits = supportedKitString;
    }

    public void fetchConfig() throws IOException, MPConfigException {
        try {
            if (mConfigUrl == null){
                Uri uri = new Uri.Builder()
                        .scheme(SECURE_SERVICE_SCHEME)
                        .authority(CONFIG_HOST)
                        .path(SERVICE_VERSION_4 + "/" + mApiKey + "/config")
                        .appendQueryParameter("av", MPUtility.getAppVersionName(mContext))
                        .appendQueryParameter("sv", Constants.MPARTICLE_VERSION)
                        .build();
                mConfigUrl = new URL(uri.toString());
            }
            HttpURLConnection connection = (HttpURLConnection) mConfigUrl.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty(HEADER_ENVIRONMENT, Integer.toString(mConfigManager.getEnvironment().getValue()));

            String supportedKits = getSupportedKitString();
            if (!MPUtility.isEmpty(supportedKits)) {
                connection.setRequestProperty(HEADER_KITS, supportedKits);
            }

            connection.setRequestProperty("User-Agent", mUserAgent);
            String etag = mPreferences.getString(Constants.PrefKeys.ETAG, null);
            if (etag != null){
                connection.setRequestProperty("If-None-Match", etag);
            }
            String modified = mPreferences.getString(Constants.PrefKeys.IF_MODIFIED, null);
            if (modified != null){
                connection.setRequestProperty("If-Modified-Since", modified);
            }

            addMessageSignature(connection, null);

            makeUrlRequest(connection, true);

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                JSONObject response = MPUtility.getJsonResponse(connection);
                parseMparticleJson(response);
                mConfigManager.updateConfig(response);
                String newEtag = connection.getHeaderField("ETag");
                String newModified = connection.getHeaderField("Last-Modified");
                SharedPreferences.Editor editor = mPreferences.edit();
                if (!MPUtility.isEmpty(newEtag)) {
                    editor.putString(Constants.PrefKeys.ETAG, newEtag);
                }
                if (!MPUtility.isEmpty(newModified)) {
                    editor.putString(Constants.PrefKeys.IF_MODIFIED, newModified);
                }
                editor.apply();
            }else if (connection.getResponseCode() >= 400) {
                throw new MPConfigException();
            }
        } catch (MalformedURLException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error constructing config service URL");
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Config request failed to process response message JSON");
        } catch (AssertionError e) {
            //some devices do not have MD5, and therefore cannot process SSL certificates
            //there's not much to do in that case except catch the error
            ConfigManager.log(MParticle.LogLevel.ERROR, "Config request failed " + e.toString());
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
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", mUserAgent);

            addMessageSignature(connection, null);
            makeUrlRequest(connection, true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call forbidden: is Segmentation enabled for your account?");
            }
            response =  MPUtility.getJsonResponse(connection);
            parseMparticleJson(response);

        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Segment call failed: " + e.getMessage());
        }
        return response;
    }

    @Override
    public boolean isThrottled() {
        try {
            checkThrottleTime();
        }catch (MPThrottleException t){
            return true;
        }
        return false;
    }

    public int sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
        checkThrottleTime();
        checkRampValue();
        if (mEventUrl == null){
            mEventUrl = new URL(SECURE_SERVICE_SCHEME, API_HOST, SERVICE_VERSION_1 + "/" + mApiKey + "/events");
        }
        byte[] messageBytes = message.getBytes();
        HttpURLConnection connection = (HttpURLConnection) mEventUrl.openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(10000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", mUserAgent);

        String activeKits = mConfigManager.getActiveModuleIds();
        if (!MPUtility.isEmpty(activeKits)) {
            connection.setRequestProperty(HEADER_KITS, activeKits);
        }
        String supportedKits = getSupportedKitString();
        if (!MPUtility.isEmpty(supportedKits)) {
            connection.setRequestProperty(HEADER_BUNDLED_KITS, supportedKits);
        }

        addMessageSignature(connection, message);

        if (BuildConfig.MP_DEBUG) {
            logUpload(message);
        }

        if (!BuildConfig.MP_DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && connection instanceof HttpsURLConnection) {
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
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            JSONObject response = MPUtility.getJsonResponse(connection);
            parseMparticleJson(response);
        }
        return connection.getResponseCode();
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

    void addMessageSignature(HttpURLConnection request, String message) {
        try {
            String method = request.getRequestMethod();
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            String dateHeader = format.format(new Date());
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
            request.setRequestProperty("Date", dateHeader);
            request.setRequestProperty(HEADER_SIGNATURE, MPUtility.hmacSha256Encode(mApiSecret, hashString.toString()));
        } catch (InvalidKeyException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error signing message.");
        } catch (NoSuchAlgorithmException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error signing message.");
        } catch (UnsupportedEncodingException e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error signing message.");
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

    /**
     * Custom socket factory used for certificate pinning.
     */
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
        if (!BuildConfig.MP_DEBUG && mParticle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && connection instanceof HttpsURLConnection) {
            try {
                ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
            }catch (Exception e){

            }
        }
        if (mParticle) {
            int statusCode = connection.getResponseCode();
            if (statusCode == 400 && !alreadyWarned) {
                alreadyWarned = true;
                ConfigManager.log(MParticle.LogLevel.ERROR, "Bad API request - is the correct API key and secret configured?");
            }
            if ((statusCode == 503 || statusCode == HTTP_TOO_MANY_REQUESTS) && !BuildConfig.MP_DEBUG) {
                setNextAllowedRequestTime(connection);
            }
        }
        return connection;
    }


    public void parseMparticleJson(JSONObject jsonResponse){
        try {
            if (jsonResponse.has(CONSUMER_INFO)) {
                JSONObject consumerInfo = jsonResponse.getJSONObject(CONSUMER_INFO);
                if (consumerInfo.has(Constants.MessageKey.MPID)) {
                    mConfigManager.setMpid(consumerInfo.getLong(Constants.MessageKey.MPID));
                }

                setCookies(consumerInfo.optJSONObject(Constants.MessageKey.COOKIES));

            }
            if (jsonResponse.has(LTV)) {
                BigDecimal serverLtv = new BigDecimal(jsonResponse.getString(LTV));
                BigDecimal mostRecentClientLtc = new BigDecimal(mPreferences.getString(Constants.PrefKeys.LTV, "0"));
                BigDecimal sum = serverLtv.add(mostRecentClientLtc);
                mPreferences.edit().putString(Constants.PrefKeys.LTV, sum.toPlainString()).apply();
            }

        } catch (JSONException jse) {

        }
    }

    void setNextAllowedRequestTime(HttpURLConnection connection) {
        long throttle = DEFAULT_THROTTLE_MILLIS;
        if (connection != null) {
            //most HttpUrlConnectionImpl's are case insensitive, but the interface
            //doesn't actually restrict it so let's be safe and check.
            String retryAfter = connection.getHeaderField("Retry-After");
            if (MPUtility.isEmpty(retryAfter)) {
                retryAfter = connection.getHeaderField("retry-after");
            }
            try {
                long parsedThrottle = Long.parseLong(retryAfter) * 1000;
                if (parsedThrottle > 0) {
                    throttle = Math.min(parsedThrottle, MAX_THROTTLE_MILLIS);
                }
            } catch (NumberFormatException nfe) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Unable to parse retry-after header, using default.");
            }
        }

        long nextTime = System.currentTimeMillis() + throttle;
        setNextRequestTime(nextTime);
    }

    long getNextRequestTime() {
        return mPreferences.getLong(Constants.PrefKeys.NEXT_REQUEST_TIME, 0);
    }

    void setNextRequestTime(long timeMillis) {
        mPreferences.edit().putLong(Constants.PrefKeys.NEXT_REQUEST_TIME, timeMillis).apply();
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

    public static final class MPRampException extends Exception {
        public MPRampException() {
            super("This device is being sampled.");
        }
    }

    void checkThrottleTime() throws MPThrottleException {
        if (System.currentTimeMillis() < getNextRequestTime()){
            throw new MPThrottleException();
        }
    }

    private void checkRampValue() throws MPRampException {
        if (mDeviceRampNumber == null){
            mDeviceRampNumber = MPUtility.hashFnv1A(MPUtility.getRampUdid(mContext).getBytes())
                    .mod(BigInteger.valueOf(100))
                    .intValue();
        }
        int currentRamp = mConfigManager.getCurrentRampValue();
        if (currentRamp > 0 && currentRamp < 100 &&
                mDeviceRampNumber > mConfigManager.getCurrentRampValue()){
            throw new MPRampException();
        }
    }

    private String getSupportedKitString(){
        if (mSupportedKits == null) {
            Set<Integer> supportedKitIds = MParticle.getInstance().getKitManager().getSupportedKits();
            if (supportedKitIds != null && supportedKitIds.size() > 0) {
                StringBuilder buffer = new StringBuilder(supportedKitIds.size() * 3);
                Iterator<Integer> it = supportedKitIds.iterator();
                while (it.hasNext()) {
                    Integer next = it.next();
                    buffer.append(next);
                    if (it.hasNext()) {
                        buffer.append(",");
                    }
                }
                mSupportedKits = buffer.toString();
            } else {
                mSupportedKits = "";
            }
        }
        return mSupportedKits;
    }

    public void setCookies(JSONObject serverCookies) {
        if (serverCookies != null) {
            try {
                JSONObject localCookies = getCookies();
                Iterator<?> keys = serverCookies.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    localCookies.put(key, serverCookies.getJSONObject(key));
                }
                mCurrentCookies = localCookies;
                mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).apply();
            } catch (JSONException jse) {

            }
        }
    }

    public JSONObject getCookies()  {
        if (mCurrentCookies == null){
            String currentCookies = mPreferences.getString(Constants.PrefKeys.Cookies, null);
            if (MPUtility.isEmpty(currentCookies)){
                mCurrentCookies = new JSONObject();
                mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).apply();
                return mCurrentCookies;
            }else {
                try {
                    mCurrentCookies = new JSONObject(currentCookies);
                } catch (JSONException e) {
                    mCurrentCookies = new JSONObject();
                }
            }
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.set(Calendar.YEAR, 1990);
            Date oldDate = nowCalendar.getTime();
            SimpleDateFormat parser = new SimpleDateFormat("yyyy");
            Iterator<?> keys = mCurrentCookies.keys();
            ArrayList<String> keysToRemove = new ArrayList<String>();
            while (keys.hasNext()) {
                try {
                    String key = (String) keys.next();
                    if (mCurrentCookies.get(key) instanceof JSONObject) {
                        String expiration = ((JSONObject) mCurrentCookies.get(key)).getString("e");
                        try {
                            Date date = parser.parse(expiration);
                            if (date.before(oldDate)) {
                                keysToRemove.add(key);
                            }
                        } catch (ParseException dpe) {

                        }
                    }
                }catch (JSONException jse){

                }
            }
            for (String key : keysToRemove){
                mCurrentCookies.remove(key);
            }
            if (keysToRemove.size() > 0) {
                mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).apply();
            }
            return mCurrentCookies;
        }else{
            return mCurrentCookies;
        }
    }
}