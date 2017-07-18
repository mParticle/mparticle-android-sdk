package com.mparticle.identity;

import android.content.Context;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleBaseClientImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mparticle.MParticle.IdentityType.CustomerId;
import static com.mparticle.MParticle.IdentityType.Email;
import static com.mparticle.MParticle.IdentityType.Facebook;
import static com.mparticle.MParticle.IdentityType.FacebookCustomAudienceId;
import static com.mparticle.MParticle.IdentityType.Google;
import static com.mparticle.MParticle.IdentityType.Microsoft;
import static com.mparticle.MParticle.IdentityType.Other;
import static com.mparticle.MParticle.IdentityType.Twitter;
import static com.mparticle.MParticle.IdentityType.Yahoo;

/** package-private **/ class MParticleIdentityClientImpl extends MParticleBaseClientImpl implements MParticleIdentityClient {
    private Context mContext;
    private ConfigManager mConfigManager;

    private static final String SECURE_SERVICE_SCHEME = "https";
    private static final String API_HOST = MPUtility.isEmpty(BuildConfig.MP_IDENTITY_URL) ? "identity.mparticle.com" : BuildConfig.MP_IDENTITY_URL;
    private static final String SERVICE_VERSION_1 = "/v1";

    public MParticleIdentityClientImpl(ConfigManager configManager, Context context) {
        super(context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE));
        this.mContext = context;
        this.mConfigManager = configManager;
    }

    public IdentityHttpResponse login(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity login request: " + jsonObject.toString());
        HttpURLConnection connection = getPostConnection("/login", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse logout(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity logout request: \n" + jsonObject.toString());
        HttpURLConnection connection = getPostConnection("/logout", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse identify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity identify request: \n" + jsonObject.toString());
        HttpURLConnection connection = getPostConnection("/identify", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse modify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getChangeJson(request);
        Logger.verbose("Identity modify request: \n" + jsonObject.toString());
        HttpURLConnection connection = getPostConnection(mConfigManager.getMpid(), "/modify", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        return parseIdentityResponse(responseCode, response);
    }

    static void setListener(BaseNetworkListener listener) {
        mListener = listener;
    }

    private JSONObject getBaseJson() throws JSONException {
        JSONObject clientSdkObject = new JSONObject();
        clientSdkObject.put("platform", "android");
        clientSdkObject.put("sdk_vendor", "mparticle");
        clientSdkObject.put("sdk_version", BuildConfig.VERSION_NAME);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_sdk", clientSdkObject);
        String context = mConfigManager.getIdentityApiContext();
        if (context != null) {
            jsonObject.put("context", context);
        }
        String environment = getStringValue(mConfigManager.getEnvironment());
        if (!MPUtility.isEmpty(environment)) {
            jsonObject.put("environment", environment);
        }
        jsonObject.put("request_timestamp_ms", System.currentTimeMillis());
        jsonObject.put("request_id", UUID.randomUUID().toString());
        return jsonObject;
    }

    private JSONObject getStateJson(IdentityApiRequest request) throws JSONException {
        JSONObject jsonObject = getBaseJson();
        if (request == null) {
            return jsonObject;
        }
        JSONObject identitiesJson = new JSONObject();
        MPUtility.AndroidAdIdInfo adIdInfo = MPUtility.getGoogleAdIdInfo(mContext);
        if (adIdInfo != null) {
            identitiesJson.put("android_aaid", adIdInfo.id);
        }
        String pushToken = mConfigManager.getPushToken();
        if (!MPUtility.isEmpty(pushToken)) {
            identitiesJson.put("push_token", pushToken);
        }
        String androidId = MPUtility.getAndroidID(mContext);
        if (!MPUtility.isEmpty(androidId)) {
            identitiesJson.put("android_uuid", androidId);
        }
        String das = mConfigManager.getDeviceApplicationStamp();
        if (!MPUtility.isEmpty(das)) {
            identitiesJson.put("device_application_stamp", das);
        }
        if (!MPUtility.isEmpty(request.getUserIdentities())) {
            for (Map.Entry<MParticle.IdentityType, String> entry : request.getUserIdentities().entrySet()) {
                String idTypeString = getStringValue(entry.getKey());
                if (!MPUtility.isEmpty(idTypeString)) {
                    identitiesJson.put(idTypeString, entry.getValue());
                }
            }
        }
        jsonObject.put("known_identities", identitiesJson);

        Long mpId = mConfigManager.getMpid();
        if (mpId != null && mpId != Constants.TEMPORARY_MPID) {
            jsonObject.put("previous_mpid", mpId);
        }
        return jsonObject;
    }

    private JSONObject getChangeJson(IdentityApiRequest request) throws JSONException {
        JSONObject jsonObject = getBaseJson();
        JSONArray changesJson = new JSONArray();
        Map<MParticle.IdentityType, String> oldIdentities = mConfigManager.getUserIdentities(mConfigManager.getMpid());
        Map<MParticle.IdentityType, String> newIdentities = request.getUserIdentities();

        Set<MParticle.IdentityType> identityTypes = new HashSet<MParticle.IdentityType>(oldIdentities.keySet());
        identityTypes.addAll(newIdentities.keySet());

        for (MParticle.IdentityType identityType: identityTypes) {
            String idTypeString = getStringValue(identityType);
            if (!MPUtility.isEmpty(idTypeString)) {
                JSONObject changeJson = new JSONObject();
                String newValue = newIdentities.get(identityType);
                String oldValue = oldIdentities.get(identityType);
                if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                    changeJson.put("new_value", newValue == null ? JSONObject.NULL : newValue);
                    changeJson.put("old_value", oldValue == null ? JSONObject.NULL : oldValue);
                    changeJson.put("identity_type", idTypeString);
                    changesJson.put(changeJson);
                }
            }
        }
        for (Map.Entry<String, String> otherIdentities: request.getOtherNewIdentities().entrySet()) {
            String identityType = otherIdentities.getKey();
            String newValue = otherIdentities.getValue();
            String oldValue = request.getOtherOldIdentities().get(identityType);
            JSONObject changeJson = new JSONObject();
            if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                changeJson.put("new_value", newValue == null ? JSONObject.NULL : newValue);
                changeJson.put("old_value", oldValue == null ? JSONObject.NULL : oldValue);
                changeJson.put("identity_type", identityType);
                changesJson.put(changeJson);
            }
        }
        jsonObject.put("identity_changes", changesJson);
        return jsonObject;
    }

    private IdentityHttpResponse parseIdentityResponse(int httpCode, JSONObject jsonObject) {
        try {
            Logger.verbose("Identity response code: " + httpCode);
            if (jsonObject != null) {
                Logger.verbose("Identity result: " + jsonObject.toString());
            }
            IdentityHttpResponse httpResponse = new IdentityHttpResponse(httpCode, jsonObject);
            if (!MPUtility.isEmpty(httpResponse.getContext())) {
                mConfigManager.setIdentityApiContext(httpResponse.getContext());
            }
            return httpResponse;
        } catch (JSONException e) {
            return new IdentityHttpResponse(httpCode, e.getMessage());
        }
    }

    private HttpURLConnection getPostConnection(Long mpId, String endpoint, String message) throws IOException {
        URL url;
        if (mpId == null) {
            url = getUrl(endpoint);
        } else {
            url = getUrl(mpId, endpoint);
        }
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("x-mp-key", getApiKey());
        connection.setRequestProperty("Content-Type", "application/json");
        String date = getHeaderDateString();
        connection.setRequestProperty("Date", date);
        try {
            connection.setRequestProperty("x-mp-signature", getHeaderHashString(connection, date, message, getApiSecret()));
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Error signing message.");
        } catch (InvalidKeyException e) {
            Logger.error("Error signing message.");
        }
        return connection;
    }

    private HttpURLConnection getPostConnection(String endpoint, String message) throws IOException {
        return getPostConnection(null, endpoint, message);
    }



    private URL getUrl(long mpId, String endpoint) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mpId);
        if (endpoint.indexOf("/") != 0) {
            stringBuilder.append("/");
        }
        stringBuilder.append(endpoint);
        return getUrl(stringBuilder.toString());
    }

    private URL getUrl(String endpoint) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(SERVICE_VERSION_1);
        if (endpoint.indexOf("/") != 0) {
            stringBuilder.append("/");
        }
        stringBuilder.append(endpoint);
        return new URL(SECURE_SERVICE_SCHEME, API_HOST, stringBuilder.toString());
    }

    private String getApiKey() {
        return mConfigManager.getApiKey();
    }

    private String getApiSecret() {
        return mConfigManager.getApiSecret();
    }

    private String getStringValue(MParticle.IdentityType identityType) {
        switch (identityType) {
            case Other:
                return "other";
            case CustomerId:
                return "customerid";
            case Facebook:
                return "facebook";
            case Twitter:
                return "twitter";
            case Google:
                return "google";
            case Microsoft:
                return "microsoft";
            case Yahoo:
                return "yahoo";
            case Email:
                return "email";
            case FacebookCustomAudienceId:
                return "facebookcustomaudienceid";
            default:
                return "";
        }
    }

    private MParticle.IdentityType getIdentityType(String idTypeString) {
        if (idTypeString.equals("other")) {
            return Other;
        } else if (idTypeString.equals("customerid")) {
            return CustomerId;
        } else if (idTypeString.equals("facebook")) {
            return Facebook;
        } else if (idTypeString.equals("twitter")) {
            return Twitter;
        } else if (idTypeString.equals("google")) {
            return Google;
        } else if (idTypeString.equals("microsoft")) {
            return Microsoft;
        } else if (idTypeString.equals("yahoo")) {
            return Yahoo;
        } else if (idTypeString.equals("email")) {
            return Email;
        } else if (idTypeString.equals("facebookcustomaudienceid")) {
            return FacebookCustomAudienceId;
        } else {
            return null;
        }
    }

    private String getStringValue(MParticle.Environment environment) {
        switch (environment) {
            case Development:
                return "development";
            case Production:
                return "production";
            default:
                return "";
        }
    }
}