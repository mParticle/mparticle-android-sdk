package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.SdkListener;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.BaseAudienceTask;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.networking.MPConnection;
import com.mparticle.networking.MPUrl;
import com.mparticle.networking.MParticleBaseClientImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Class responsible for all network communication to the mParticle Events and Configuration APIs.
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

    /**
     * Wrapper around cookies, MPID, and other server-response customAttributes that requires parsing.
     */
    private static final String CONSUMER_INFO = "ci";

    /**
     * Human readable error message field for "alias" responses
     */
    private static final String ALIAS_ERROR_MESSAGE = "message";

    private final ConfigManager mConfigManager;
    private final String mApiSecret;
    private MPUrl mConfigUrl;
    private final String mUserAgent;
    private final SharedPreferences mPreferences;
    private final String mApiKey;
    private final Context mContext;
    Integer mDeviceRampNumber = null;
    private JSONObject mCurrentCookies;

    /**
     * Default throttle time - in the worst case scenario if the server is busy, the soonest
     * the SDK will attempt to contact the server again will be after this 2 hour window.
     */
    static final long DEFAULT_THROTTLE_MILLIS = 1000 * 60 * 60 * 2;
    static final long MAX_THROTTLE_MILLIS = 1000 * 60 * 60 * 24;
    /**
     * Minimum time between passive Config requests, 10 minutes
     */
    private static final int MIN_CONFIG_REQUEST_INTERVAL = 10 * 60 * 1000;
    private long mConfigLastFetched = -1;
    private boolean alreadyWarned;

    public MParticleApiClientImpl(ConfigManager configManager, SharedPreferences sharedPreferences, Context context) throws MalformedURLException, MPNoConfigException {
        super(context, configManager);
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
    void setConfigUrl(MPUrl configUrl) {
        mConfigUrl = configUrl;
    }

    @Override
    public void fetchConfig() throws IOException, MPConfigException {
        fetchConfig(false);
    }

    /**
     * Fetches a remote configuration. Minimum time constraint based on MIN_CONFIG_REQUEST_INTERVAL
     * if not forced, configuration request will not take place if minimum time has not elapsed
     *
     * @param force: if true, minimum elpsed time criteria will be ignored, and configuration
     *               request will take place regardless of elapsed time
     */
    public void fetchConfig(boolean force) throws IOException, MPConfigException {
        if (!force) {
            if (System.currentTimeMillis() - mConfigLastFetched > MIN_CONFIG_REQUEST_INTERVAL) {
                mConfigLastFetched = System.currentTimeMillis();
            } else {
                Logger.verbose("Config request deferred, not enough time has elapsed since last request.");
                return;
            }
        }
        try {
            if (mConfigUrl == null) {
                mConfigUrl = getUrl(Endpoint.CONFIG);
            }
            MPConnection connection = mConfigUrl.openConnection();
            connection.setConnectTimeout(mConfigManager.getConnectionTimeout());
            connection.setReadTimeout(mConfigManager.getConnectionTimeout());
            connection.setRequestProperty(HEADER_ENVIRONMENT, Integer.toString(mConfigManager.getEnvironment().getValue()));

            String supportedKits = mConfigManager.getSupportedKitString();
            if (!MPUtility.isEmpty(supportedKits)) {
                connection.setRequestProperty(HEADER_KITS, supportedKits);
            }

            connection.setRequestProperty("User-Agent", mUserAgent);
            String etag = mConfigManager.getEtag();
            if (etag != null) {
                connection.setRequestProperty("If-None-Match", etag);
            }
            String modified = mConfigManager.getIfModified();
            if (modified != null) {
                connection.setRequestProperty("If-Modified-Since", modified);
            }

            addMessageSignature(connection, null);

            Logger.verbose("Config request attempt:\n" +
                    "URL- " + mConfigUrl.toString());

            if (InternalListenerManager.isEnabled()) {
                InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.CONFIG, connection.getURL().toString(), new JSONObject());
            }

            makeUrlRequest(Endpoint.CONFIG, connection, true);

            JSONObject response = new JSONObject();
            int responseCode = connection.getResponseCode();
            try {
                response = MPUtility.getJsonResponse(connection);
                InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.CONFIG, connection.getURL().toString(), response, responseCode);
            } catch (Exception ex) {
            }
            if (responseCode >= 200 && responseCode < 300) {
                parseCookies(response);

                Logger.verbose("Config result: \n " +
                        connection.getResponseCode() + ": " +
                        connection.getResponseMessage() + "\n" +
                        "response:\n" + response.toString());

                String newEtag = connection.getHeaderField("ETag");
                String newModified = connection.getHeaderField("Last-Modified");

                mConfigManager.updateConfig(response, newEtag, newModified);
            } else if (connection.getResponseCode() == 400) {
                throw new MPConfigException();
            } else if (connection.getResponseCode() == 304) {
                mConfigManager.configUpToDate();
                Logger.verbose("Config request deferred, configuration already up-to-date.");
            } else {
                Logger.error("Config request failed- " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Logger.error("Error constructing config service URL.");
        } catch (JSONException e) {
            Logger.error("Config request failed to process response message JSON.");
        } catch (AssertionError e) {
            //some devices do not have MD5, and therefore cannot process SSL certificates
            //there's not much to do in that case except catch the error
            Logger.error("Config request failed " + e.toString());
        }
    }

    public void fetchUserAudience(BaseAudienceTask task, long mpId) {
        JSONObject jsonResponse = null;
        try {
            MPConnection connection = getUrl(Endpoint.AUDIENCE,mpId).openConnection();
            Logger.verbose("Audience API request: \n" + connection.getURL().toString());
            connection.setRequestProperty("User-Agent", mUserAgent);
            addMessageSignature(connection, null);
            connection.setRequestProperty("x-mp-key", mApiKey);
            makeUrlRequest(Endpoint.AUDIENCE, connection, true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Logger.error("Your workspace is not enabled to retrieve user audiences");
            }
            jsonResponse = MPUtility.getJsonResponse(connection);
            Logger.verbose("Audience API response: \n Status code: " +connection.getResponseCode()+ "  JSON response: "+ jsonResponse);
            if (jsonResponse != null && connection.getResponseCode() == 200) {
                task.setSuccessful(new AudienceResponse(connection.getResponseCode(), jsonResponse));
            } else {
                task.setFailed(new AudienceResponse(connection.getResponseCode(), "mParticle Audience API failed."));
            }
        } catch (Exception e) {
            Logger.error("mParticle Audience API failed. " + e);
            task.setFailed(new AudienceResponse(IdentityApi.UNKNOWN_ERROR, Objects.requireNonNull(e.getMessage())));
        }
    }

    public int sendMessageBatch(@NonNull String message, @NonNull UploadSettings uploadSettings) throws IOException, MPThrottleException, MPRampException {
        checkThrottleTime(Endpoint.EVENTS);
        checkRampValue();
        MPUrl eventUrl = getUrl(Endpoint.EVENTS, null,null, uploadSettings);
        MPConnection connection = eventUrl.openConnection();
        connection.setConnectTimeout(mConfigManager.getConnectionTimeout());
        connection.setReadTimeout(mConfigManager.getConnectionTimeout());
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", mUserAgent);

        String activeKits = uploadSettings.getActiveKits();
        if (!MPUtility.isEmpty(activeKits)) {
            connection.setRequestProperty(HEADER_KITS, activeKits);
        }
        String supportedKits = uploadSettings.getSupportedKits();
        if (!MPUtility.isEmpty(supportedKits)) {
            connection.setRequestProperty(HEADER_BUNDLED_KITS, supportedKits);
        }

        addMessageSignature(connection, message);

        logUpload(message);

        try {
            InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.EVENTS, connection.getURL().toString(), new JSONObject(message), message);
        } catch (Exception e) {
        }

        makeUrlRequest(Endpoint.EVENTS, connection, message, true);

        Logger.verbose("Upload request attempt:\n" +
                "URL- " + eventUrl.toString());

        Logger.verbose(message);

        int responseCode = connection.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            JSONObject response = MPUtility.getJsonResponse(connection);
            if (InternalListenerManager.isEnabled()) {
                InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.EVENTS, connection.getURL().toString(), response, responseCode);
            }

            Logger.verbose("Upload result response: \n" +
                    connection.getResponseCode() + ": " +
                    connection.getResponseMessage() + "\n" +
                    "response:\n" + response.toString());
            parseCookies(response);
        } else {
            Logger.error("Upload request failed- " + responseCode + ": " + connection.getResponseMessage());
            try {
                InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.EVENTS, connection.getURL().getFile(), new JSONObject().put(SdkListener.ERROR_MESSAGE, connection.getResponseMessage()), responseCode);
            } catch (Exception e) {
            }
        }
        return connection.getResponseCode();
    }

    @NonNull
    @Override
    public AliasNetworkResponse sendAliasRequest(@NonNull String message, @NonNull UploadSettings uploadSettings) throws IOException, MPThrottleException, MPRampException {
        checkThrottleTime(Endpoint.ALIAS);
        Logger.verbose("Identity alias request:\n" + message);

        MPUrl aliasUrl = getUrl(Endpoint.ALIAS, null, null, uploadSettings);
        MPConnection connection = aliasUrl.openConnection();
        connection.setConnectTimeout(mConfigManager.getConnectionTimeout());
        connection.setReadTimeout(mConfigManager.getConnectionTimeout());
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", mUserAgent);
        addMessageSignature(connection, message);

        String url = "";
        try {
            url = connection.getURL().toString();
            InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.EVENTS, url, new JSONObject(message), message);
        } catch (Exception ignore) {
        }

        connection = makeUrlRequest(Endpoint.ALIAS, connection, message, false);
        int responseCode = connection.getResponseCode();
        String error = "";
        JSONObject response = new JSONObject();
        if (responseCode >= 200 && responseCode < 300) {
            Logger.verbose("Alias Request response: \n " +
                    connection.getResponseCode() + ": " +
                    connection.getResponseMessage());
        } else {
            response = MPUtility.getJsonResponse(connection);
            if (response != null) {
                error = response.optString(ALIAS_ERROR_MESSAGE);
            }
            Logger.error("Alias Request failed- " + responseCode + ": " + error);
        }
        InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.EVENTS, url, response, responseCode);
        return new AliasNetworkResponse(responseCode, error);
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
            }
        } catch (JSONException jse) {

        }
    }

    void addMessageSignature(MPConnection request, String message) {
        try {
            String date = getHeaderDateString();
            request.setRequestProperty("Date", date);
            request.setRequestProperty(HEADER_SIGNATURE, getHeaderHashString(request, date, message, mApiSecret));
        } catch (InvalidKeyException e) {
            Logger.error("Error signing message.");
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Error signing message.");
        } catch (UnsupportedEncodingException e) {
            Logger.error("Error signing message.");
        }
    }

    private void parseCookies(JSONObject jsonResponse) {
        try {
            if (jsonResponse.has(CONSUMER_INFO)) {
                JSONObject consumerInfo = jsonResponse.getJSONObject(CONSUMER_INFO);
                setCookies(consumerInfo.optJSONObject(Constants.MessageKey.COOKIES));
            }
        } catch (JSONException ignored) {
        }
    }

    public final class MPThrottleException extends Exception {
        MPThrottleException() {
            super("mParticle servers are busy, API connections have been throttled.");
        }
    }

    public final class MPConfigException extends Exception {
        MPConfigException() {
            super("mParticle configuration request failed.");
        }
    }

    public static final class MPRampException extends Exception {
        MPRampException() {
            super("This device is being sampled.");
        }
    }

    public static final class MPNoConfigException extends Exception {
        MPNoConfigException() {
            super("No API key and/or API secret.");
        }
    }

    void checkThrottleTime(Endpoint endpoint) throws MPThrottleException {
        if (System.currentTimeMillis() < getNextRequestTime(endpoint)) {
            throw new MPThrottleException();
        }
    }

    private void checkRampValue() throws MPRampException {
        if (mDeviceRampNumber == null) {
            mDeviceRampNumber = MPUtility.hashFnv1A(MPUtility.getRampUdid(mContext).getBytes())
                    .mod(BigInteger.valueOf(100))
                    .intValue();
        }
        int currentRamp = mConfigManager.getCurrentRampValue();
        if (currentRamp > 0 && currentRamp < 100 &&
                mDeviceRampNumber > mConfigManager.getCurrentRampValue()) {
            throw new MPRampException();
        }
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
                mConfigManager.getUserStorage().setCookies(mCurrentCookies.toString());
            } catch (JSONException jse) {

            }
        }
    }

    public JSONObject getCookies() {
        if (mCurrentCookies == null) {
            String currentCookies = mConfigManager.getUserStorage().getCookies();
            if (MPUtility.isEmpty(currentCookies)) {
                mCurrentCookies = new JSONObject();
                mConfigManager.getUserStorage().setCookies(mCurrentCookies.toString());
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
                } catch (JSONException jse) {

                }
            }
            for (String key : keysToRemove) {
                mCurrentCookies.remove(key);
            }
            if (keysToRemove.size() > 0) {
                mConfigManager.getUserStorage().setCookies(mCurrentCookies.toString());
            }
            return mCurrentCookies;
        } else {
            return mCurrentCookies;
        }
    }
}