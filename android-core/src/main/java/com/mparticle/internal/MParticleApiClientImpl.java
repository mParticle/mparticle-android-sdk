package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;

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
public class MParticleApiClientImpl extends MParticleBaseClientImpl implements MParticleApiClient {

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
    private static String sSupportedKits;
    private JSONObject mCurrentCookies;

    public MParticleApiClientImpl(ConfigManager configManager, SharedPreferences sharedPreferences, Context context) throws MalformedURLException, MPNoConfigException {
        super(sharedPreferences);
        mContext = context;
        mConfigManager = configManager;
        mApiSecret = configManager.getApiSecret();
        mPreferences = sharedPreferences;
        mApiKey = configManager.getApiKey();
        mUserAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION;
        if (MPUtility.isEmpty(mApiKey) || MPUtility.isEmpty(mApiSecret)) {
            throw new MPNoConfigException();
        }
    }

    /**
     * Only used for unit testing.
     */
    void setConfigUrl(URL configUrl) {
        mConfigUrl = configUrl;
    }

    static void setSupportedKitString(String supportedKitString) {
        sSupportedKits = supportedKitString;
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

            Logger.verbose("Config request attempt:\n" +
                    "URL- " + mConfigUrl.toString());

            makeUrlRequest(connection, true);

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                JSONObject response = MPUtility.getJsonResponse(connection);
                parseMparticleJson(response);

                Logger.verbose("Config result: \n " +
                        connection.getResponseCode() + ": " +
                        connection.getResponseMessage() +"\n" +
                        "response:\n" + response.toString());

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
            }else if (connection.getResponseCode() == 400) {
                throw new MPConfigException();
            } else if (connection.getResponseCode() == 304) {
                Logger.verbose("Config request deferred, configuration already up-to-date");
            } else {
                Logger.error("Config request failed- " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Logger.error("Error constructing config service URL");
        } catch (JSONException e) {
            Logger.error("Config request failed to process response message JSON");
        } catch (AssertionError e) {
            //some devices do not have MD5, and therefore cannot process SSL certificates
            //there's not much to do in that case except catch the error
            Logger.error("Config request failed " + e.toString());
        }
    }

    private URL getAudienceUrl() throws MalformedURLException {
        return new URL(SECURE_SERVICE_SCHEME, API_HOST, SERVICE_VERSION_1 + "/" + mApiKey + "/audience?mpID=" + mConfigManager.getMpid());
    }

    public JSONObject fetchAudiences()  {

        JSONObject response = null;
        try {
            Logger.debug("Starting Segment Network request");
            HttpURLConnection connection = (HttpURLConnection) getAudienceUrl().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", mUserAgent);

            addMessageSignature(connection, null);
            makeUrlRequest(connection, true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN){
                Logger.error("Segment call forbidden: is Segmentation enabled for your account?");
            }
            response =  MPUtility.getJsonResponse(connection);
            parseMparticleJson(response);

        }catch (Exception e){
            Logger.error("Segment call failed: " + e.getMessage());
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

        logUpload(message);

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

        Logger.verbose("Upload request attempt:\n" +
                "URL- " + mEventUrl.toString());

        Logger.verbose(message);

        int responseCode = connection.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            JSONObject response = MPUtility.getJsonResponse(connection);

            Logger.verbose("Upload result response: \n" +
                    connection.getResponseCode() + ": " +
                        connection.getResponseMessage() + "\n" +
                        "response:\n" + response.toString());
            parseMparticleJson(response);
        } else {
            Logger.error("Upload request failed- " + connection.getResponseCode() + ": " + connection.getResponseMessage());
        }
        return connection.getResponseCode();
    }

    private void logUpload(String message) {
        try {
            JSONObject messageJson = new JSONObject(message);
            if (messageJson.has(Constants.MessageKey.MESSAGES)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.MESSAGES);
                Logger.verbose("Uploading message batch...");
                for (int i = 0; i < messages.length(); i++) {
                    Logger.verbose("Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE));
                }
            } else if (messageJson.has(Constants.MessageKey.HISTORY)) {
                JSONArray messages = messageJson.getJSONArray(Constants.MessageKey.HISTORY);
                Logger.verbose("Uploading session history batch...");
                for (int i = 0; i < messages.length(); i++) {
                    Logger.verbose("Message type: " + ((JSONObject) messages.get(i)).getString(Constants.MessageKey.TYPE) + " SID: " + ((JSONObject) messages.get(i)).optString(Constants.MessageKey.SESSION_ID));
                }
            }
        } catch (JSONException jse) {

        }
    }

    void addMessageSignature(HttpURLConnection request, String message) {
        try {
            request.setRequestProperty("Date", getHeaderDateString());
            request.setRequestProperty(HEADER_SIGNATURE, getHeaderHashString(request, message, mApiSecret));
        } catch (InvalidKeyException e) {
            Logger.error("Error signing message.");
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Error signing message.");
        } catch (UnsupportedEncodingException e){
            Logger.error("Error signing message.");
        }
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
                BigDecimal mostRecentClientLtc = new BigDecimal(mConfigManager.getUserConfig().getLtv());
                BigDecimal sum = serverLtv.add(mostRecentClientLtc);
                mConfigManager.getUserConfig().setLtv(sum.toPlainString());
            }

        } catch (JSONException jse) {

        }
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

    public static final class MPNoConfigException extends Exception {
        public MPNoConfigException() {super("No API key and/or API secret"); }
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
        if (sSupportedKits == null) {
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
                sSupportedKits = buffer.toString();
            } else {
                sSupportedKits = "";
            }
        }
        return sSupportedKits;
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
                mConfigManager.getUserConfig().setCookies(mCurrentCookies.toString());
            } catch (JSONException jse) {

            }
        }
    }

    public JSONObject getCookies()  {
        if (mCurrentCookies == null){
            String currentCookies = mConfigManager.getUserConfig().getCookies();
            if (MPUtility.isEmpty(currentCookies)) {
                mCurrentCookies = new JSONObject();
                mConfigManager.getUserConfig().setCookies(mCurrentCookies.toString());
                return mCurrentCookies;
            } else {
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
                mConfigManager.getUserConfig().setCookies(mCurrentCookies.toString());
            }
            return mCurrentCookies;
        }else{
            return mCurrentCookies;
        }
    }
}