package com.mparticle.identity;

import android.content.Context;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.DatabaseTables;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleBaseClientImpl;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.dto.MParticleUserDTO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mparticle.MParticle.IdentityType.*;

/** package-private **/ class MParticleIdentityClientImpl extends MParticleBaseClientImpl implements MParticleIdentityClient {
    private Context mContext;
    private ConfigManager mConfigManager;

    private static final String SECURE_SERVICE_SCHEME = MPUtility.isEmpty(BuildConfig.MP_IDENTITY_URL) ? "https" : "http";
    private static final String API_HOST = MPUtility.isEmpty(BuildConfig.MP_IDENTITY_URL) ? "identity.mparticle.com" : BuildConfig.MP_IDENTITY_URL;
    private static final String SERVICE_VERSION_1 = "/v1";

    public MParticleIdentityClientImpl(ConfigManager configManager, Context context) {
        super(context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE));
        this.mContext = context;
        this.mConfigManager = configManager;
    }

    public MParticleUserDTO login(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        HttpURLConnection connection = getPostConnection("/login", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        if (responseCode == 202) {
            return getUser(response, request);
        } else {
            return getError(response);
        }
    }

    public MParticleUserDTO logout(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        HttpURLConnection connection = getPostConnection("/logout", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        if (responseCode == 202) {
            return getUser(response, request);
        } else {
            return getError(response);
        }
    }

    public MParticleUserDTO identify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        HttpURLConnection connection = getPostConnection("/identify", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        if (responseCode == 202) {
            return getUser(response, request);
        } else {
            return getError(response);
        }
    }

    public Boolean modify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getChangeJson(request);
        HttpURLConnection connection = getPostConnection(request.getMpId(), "/modify", jsonObject.toString());
        makeUrlRequest(connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        if (responseCode == 202) {
            return true;
        } else {
            return false;
        }
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
        if (!MPUtility.isEmpty(request.getUserIdentities())) {
            for (Map.Entry<MParticle.IdentityType, String> entry : request.getUserIdentities().entrySet()) {
                String idTypeString = getStringValue(entry.getKey());
                if (!MPUtility.isEmpty(idTypeString)) {
                    identitiesJson.put(idTypeString, entry.getValue());
                }
            }
        }
        jsonObject.put("known_identities", identitiesJson);

        Long mpId = request.getMpId();
        if (mpId != null && mpId != 0) {
            jsonObject.put("previous_mpid", mpId);
        }
        return jsonObject;
    }

    private JSONObject getChangeJson(IdentityApiRequest request) throws JSONException {
        JSONObject jsonObject = getBaseJson();
        JSONArray changesJson = new JSONArray();
        Map<MParticle.IdentityType, String> oldIdentities = mConfigManager.getUserIdentities(request.getMpId());
        Map<MParticle.IdentityType, String> newIdentities = request.getUserIdentities();

        Set<MParticle.IdentityType> identityTypes = oldIdentities.keySet();
        identityTypes.addAll(newIdentities.keySet());

        for (MParticle.IdentityType identityType: identityTypes) {
            String idTypeString = getStringValue(identityType);
            if (!MPUtility.isEmpty(idTypeString)) {
                JSONObject changeJson = new JSONObject();
                String newValue = newIdentities.get(identityType);
                String oldValue = oldIdentities.get(identityType);
                changeJson.put("new_value", newValue == null ? "null" : newValue);
                changeJson.put("old_value", oldValue == null ? "null" : oldValue);
                changeJson.put("identity_type", idTypeString);
                changesJson.put(changeJson);
            }
        }
        jsonObject.put("identity_changes", changesJson);
        return jsonObject;
    }

    private MParticleUserDTO getUser(JSONObject jsonObject, IdentityApiRequest request) throws JSONException {
        String context = jsonObject.optString("context");
        if (!MPUtility.isEmpty(context)) {
            mConfigManager.setIdentityApiContext(context);
        }
        long mpId = jsonObject.getLong("mpid");
        Map<MParticle.IdentityType, String> identityTypeMap = request.getUserIdentities();
        //if the mpid did not change as a result of the request OR the request has shouldCopyUserAttributes == true,
        //keep the same userAttributes
        if (mpId == request.getMpId() || request.shouldCopyUserAttributes()) {
            Map<String, Object> userAttributes = new MParticleDBManager(mContext, DatabaseTables.getInstance(mContext)).getUserAttributes(request.getMpId());
            return new MParticleUserDTO(mpId, identityTypeMap, userAttributes);
        } else {
            return new MParticleUserDTO(mpId, identityTypeMap);
        }
    }

    private MParticleUserDTO getError(JSONObject jsonObject) {
        JSONArray errorsArray = null;
        StringBuilder builder = new StringBuilder();
        try {
            errorsArray = jsonObject.getJSONArray("errors");
            if (!MPUtility.isEmpty(errorsArray)) {
                for (int i = 0; i < errorsArray.length(); i++) {
                    try {
                        JSONObject object = errorsArray.getJSONObject(i);
                        builder.append(object.getString("code"));
                        builder.append(": ");
                        builder.append(object.getString("message"));
                        builder.append("/n");
                    }
                    catch (JSONException ignore) {}
                }
            }
        } catch (JSONException ignore) {
                builder.append("could not parse errors");
        }
        return new MParticleUserDTO.Error(builder.toString());
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
        connection.setRequestProperty("x-mp-key", getApiKey());
        connection.setRequestProperty("x-mp-date", getHeaderDateString());
        try {
            connection.setRequestProperty("x-mp-signature", getHeaderHashString(connection, message, getApiSecret()));
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