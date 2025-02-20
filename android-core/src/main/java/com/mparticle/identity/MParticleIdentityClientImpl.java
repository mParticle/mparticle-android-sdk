package com.mparticle.identity;

import android.content.Context;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.SdkListener;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.networking.MPConnection;
import com.mparticle.networking.MPUrl;
import com.mparticle.networking.MParticleBaseClientImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MParticleIdentityClientImpl extends MParticleBaseClientImpl implements MParticleIdentityClient {
    private Context mContext;
    private ConfigManager mConfigManager;

    static final String LOGIN_PATH = "login";
    static final String LOGOUT_PATH = "logout";
    static final String IDENTIFY_PATH = "identify";
    static final String MODIFY_PATH = "modify";

    static final String PLATFORM = "platform";
    static final String SDK_VENDOR = "sdk_vendor";
    static final String SDK_VERSION = "sdk_version";
    static final String CLIENT_SDK = "client_sdk";
    static final String CONTEXT = "context";
    static final String ENVIRONMENT = "environment";
    static final String REQUEST_TIMESTAMP_MS = "request_timestamp_ms";

    static final String ANDROID_AAID = "android_aaid";
    static final String FIRE_AID = "fire_aid";
    static final String PUSH_TOKEN = "push_token";
    static final String ANDROID_UUID = "android_uuid";
    static final String DEVICE_APPLICATION_STAMP = "device_application_stamp";
    static final String KNOWN_IDENTITIES = "known_identities";
    static final String PREVIOUS_MPID = "previous_mpid";

    static final String NEW_VALUE = "new_value";
    static final String OLD_VALUE = "old_value";
    static final String IDENTITY_TYPE = "identity_type";
    static final String IDENTITY_CHANGES = "identity_changes";

    static final String X_MP_KEY = "x-mp-key";
    static final String X_MP_SIGNATURE = "x-mp-signature";

    static final String PLATFORM_ANDROID = "android";
    static final String PLATFORM_FIRE = "fire";

    private static final String SERVICE_VERSION_1 = "/v1";
    private MParticle.OperatingSystem mOperatingSystem;

    public MParticleIdentityClientImpl(Context context, ConfigManager configManager, MParticle.OperatingSystem operatingSystem) {
        super(context, configManager);
        this.mContext = context;
        this.mConfigManager = configManager;
        this.mOperatingSystem = operatingSystem;
    }

    public IdentityHttpResponse login(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity login request: " + jsonObject.toString());
        MPConnection connection = getPostConnection(LOGIN_PATH, jsonObject.toString());
        String url = connection.getURL().toString();
        InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.IDENTITY_LOGIN, url, jsonObject, request);
        connection = makeUrlRequest(Endpoint.IDENTITY, connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.IDENTITY_LOGIN, url, response, responseCode);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse logout(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity logout request: \n" + jsonObject.toString());
        MPConnection connection = getPostConnection(LOGOUT_PATH, jsonObject.toString());
        String url = connection.getURL().toString();
        InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.IDENTITY_LOGOUT, url, jsonObject, request);
        connection = makeUrlRequest(Endpoint.IDENTITY, connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.IDENTITY_LOGOUT, url, response, responseCode);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse identify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getStateJson(request);
        Logger.verbose("Identity identify request: \n" + jsonObject.toString());
        MPConnection connection = getPostConnection(IDENTIFY_PATH, jsonObject.toString());
        String url = connection.getURL().toString();
        InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.IDENTITY_IDENTIFY, url, jsonObject, request);
        connection = makeUrlRequest(Endpoint.IDENTITY, connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.IDENTITY_IDENTIFY, url, response, responseCode);
        return parseIdentityResponse(responseCode, response);
    }

    public IdentityHttpResponse modify(IdentityApiRequest request) throws JSONException, IOException {
        JSONObject jsonObject = getChangeJson(request);
        Logger.verbose("Identity modify request: \n" + jsonObject.toString());
        JSONArray identityChanges = jsonObject.optJSONArray("identity_changes");
        if (identityChanges != null && identityChanges.length() == 0) {
            return new IdentityHttpResponse(200, request.mpid, "", null);
        }
        MPConnection connection = getPostConnection(request.mpid, MODIFY_PATH, jsonObject.toString());
        String url = connection.getURL().toString();
        InternalListenerManager.getListener().onNetworkRequestStarted(SdkListener.Endpoint.IDENTITY_MODIFY, url, jsonObject, request);
        connection = makeUrlRequest(Endpoint.IDENTITY, connection, jsonObject.toString(), false);
        int responseCode = connection.getResponseCode();
        JSONObject response = MPUtility.getJsonResponse(connection);
        InternalListenerManager.getListener().onNetworkRequestFinished(SdkListener.Endpoint.IDENTITY_MODIFY, url, response, responseCode);
        return parseIdentityResponse(responseCode, response);
    }

    private JSONObject getBaseJson() throws JSONException {
        JSONObject clientSdkObject = new JSONObject();
        clientSdkObject.put(PLATFORM, getOperatingSystemString());
        clientSdkObject.put(SDK_VENDOR, "mparticle");
        clientSdkObject.put(SDK_VERSION, BuildConfig.VERSION_NAME);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CLIENT_SDK, clientSdkObject);
        String context = mConfigManager.getIdentityApiContext();
        if (context != null) {
            jsonObject.put(CONTEXT, context);
        }
        String environment = getStringValue(mConfigManager.getEnvironment());
        if (!MPUtility.isEmpty(environment)) {
            jsonObject.put(ENVIRONMENT, environment);
        }
        jsonObject.put(REQUEST_TIMESTAMP_MS, System.currentTimeMillis());
        jsonObject.put(REQUEST_ID, UUID.randomUUID().toString());
        return jsonObject;
    }

    private JSONObject getStateJson(IdentityApiRequest request) throws JSONException {
        JSONObject jsonObject = getBaseJson();

        JSONObject identitiesJson = new JSONObject();
        MPUtility.AdIdInfo adIdInfo = MPUtility.getAdIdInfo(mContext);
        if (adIdInfo != null && !adIdInfo.isLimitAdTrackingEnabled) {
            switch (adIdInfo.advertiser) {
                case AMAZON:
                    identitiesJson.put(FIRE_AID, adIdInfo.id);
                    break;
                case GOOGLE:
                    identitiesJson.put(ANDROID_AAID, adIdInfo.id);
                    break;
            }
        }
        String pushToken = mConfigManager.getPushInstanceId();
        if (!MPUtility.isEmpty(pushToken)) {
            identitiesJson.put(PUSH_TOKEN, pushToken);
        }
        String androidId = MPUtility.getAndroidID(mContext);
        if (!MPUtility.isEmpty(androidId)) {
            identitiesJson.put(ANDROID_UUID, androidId);
        }
        String das = mConfigManager.getDeviceApplicationStamp();
        if (!MPUtility.isEmpty(das)) {
            identitiesJson.put(DEVICE_APPLICATION_STAMP, das);
        }
        if (request != null) {
            if (!MPUtility.isEmpty(request.getUserIdentities())) {
                for (Map.Entry<MParticle.IdentityType, String> entry : request.getUserIdentities().entrySet()) {
                    String idTypeString = getStringValue(entry.getKey());
                    if (!MPUtility.isEmpty(idTypeString)) {
                        identitiesJson.put(idTypeString, entry.getValue());
                    }
                }
            }
        }
        jsonObject.put(KNOWN_IDENTITIES, identitiesJson);

        Long mpId = mConfigManager.getMpid();
        if (!mpId.equals(Constants.TEMPORARY_MPID)) {
            jsonObject.put(PREVIOUS_MPID, mpId);
        }
        return jsonObject;
    }

    private JSONObject getChangeJson(IdentityApiRequest request) throws JSONException {
        if (request.mpid == null) {
            request.mpid = mConfigManager.getMpid();
        }
        JSONObject jsonObject = getBaseJson();
        JSONArray changesJson = new JSONArray();
        Map<MParticle.IdentityType, String> userIdentities = request.getUserIdentities();
        Map<MParticle.IdentityType, String> previousIdentities = mConfigManager.getUserIdentities(request.mpid);

        Set<MParticle.IdentityType> identityTypes = new HashSet<MParticle.IdentityType>(userIdentities.keySet());
        identityTypes.addAll(userIdentities.keySet());

        for (MParticle.IdentityType identityType : identityTypes) {
            String idTypeString = getStringValue(identityType);
            if (!MPUtility.isEmpty(idTypeString)) {
                JSONObject changeJson = new JSONObject();
                String newValue = userIdentities.get(identityType);
                String oldValue = previousIdentities.get(identityType);
                if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                    changeJson.put(NEW_VALUE, newValue == null ? JSONObject.NULL : newValue);
                    changeJson.put(OLD_VALUE, oldValue == null ? JSONObject.NULL : oldValue);
                    changeJson.put(IDENTITY_TYPE, idTypeString);
                    changesJson.put(changeJson);
                }
            }
        }
        for (Map.Entry<String, String> otherIdentities : request.getOtherNewIdentities().entrySet()) {
            String identityType = otherIdentities.getKey();
            String newValue = otherIdentities.getValue();
            String oldValue = request.getOtherOldIdentities().get(identityType);
            JSONObject changeJson = new JSONObject();
            if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                changeJson.put(NEW_VALUE, newValue == null ? JSONObject.NULL : newValue);
                changeJson.put(OLD_VALUE, oldValue == null ? JSONObject.NULL : oldValue);
                changeJson.put(IDENTITY_TYPE, identityType);
                changesJson.put(changeJson);
            }
        }
        jsonObject.put(IDENTITY_CHANGES, changesJson);
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

    private MPConnection getPostConnection(Long mpId, String endpoint, String message) throws IOException {
        MPUrl url;
        if (mpId == null) {
            url = getUrl(endpoint);
        } else {
            url = getUrl(mpId, endpoint);
        }
        MPConnection connection = url.openConnection();
        connection.setConnectTimeout(mConfigManager.getIdentityConnectionTimeout());
        connection.setReadTimeout(mConfigManager.getIdentityConnectionTimeout());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty(X_MP_KEY, getApiKey());
        connection.setRequestProperty("Content-Type", "application/json");
        String date = getHeaderDateString();
        connection.setRequestProperty("Date", date);
        try {
            connection.setRequestProperty(X_MP_SIGNATURE, getHeaderHashString(connection, date, message, getApiSecret()));
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Error signing message.");
        } catch (InvalidKeyException e) {
            Logger.error("Error signing message.");
        }
        return connection;
    }

    private MPConnection getPostConnection(String endpoint, String message) throws IOException {
        return getPostConnection(null, endpoint, message);
    }


    MPUrl getUrl(long mpId, String endpoint) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mpId);
        if (endpoint.indexOf("/") != 0) {
            stringBuilder.append("/");
        }
        stringBuilder.append(endpoint);
        return getUrl(stringBuilder.toString());
    }

    MPUrl getUrl(String endpoint) throws MalformedURLException {
        return getUrl(Endpoint.IDENTITY, endpoint, null);
    }

    private String getApiKey() {
        return mConfigManager.getApiKey();
    }

    private String getApiSecret() {
        return mConfigManager.getApiSecret();
    }

    public static String getStringValue(MParticle.IdentityType identityType) {
        switch (identityType) {
            case Other:
                return "other";
            case Other2:
                return "other2";
            case Other3:
                return "other3";
            case Other4:
                return "other4";
            case Other5:
                return "other5";
            case Other6:
                return "other6";
            case Other7:
                return "other7";
            case Other8:
                return "other8";
            case Other9:
                return "other9";
            case Other10:
                return "other10";
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
            case Alias:
                return "alias";
            case Email:
                return "email";
            case FacebookCustomAudienceId:
                return "facebookcustomaudienceid";
            case MobileNumber:
                return "mobile_number";
            case PhoneNumber2:
                return "phone_number_2";
            case PhoneNumber3:
                return "phone_number_3";
            default:
                return "";
        }
    }

    static MParticle.IdentityType getIdentityType(String idTypeString) {
        if ("other".equals(idTypeString)) {
            return MParticle.IdentityType.Other;
        } else if ("other2".equals(idTypeString)) {
            return MParticle.IdentityType.Other2;
        } else if ("other3".equals(idTypeString)) {
            return MParticle.IdentityType.Other3;
        } else if ("other4".equals(idTypeString)) {
            return MParticle.IdentityType.Other4;
        } else if ("other5".equals(idTypeString)) {
            return MParticle.IdentityType.Other5;
        } else if ("other6".equals(idTypeString)) {
            return MParticle.IdentityType.Other6;
        } else if ("other7".equals(idTypeString)) {
            return MParticle.IdentityType.Other7;
        } else if ("other8".equals(idTypeString)) {
            return MParticle.IdentityType.Other8;
        } else if ("other9".equals(idTypeString)) {
            return MParticle.IdentityType.Other9;
        } else if ("other10".equals(idTypeString)) {
            return MParticle.IdentityType.Other10;
        } else if ("customerid".equals(idTypeString)) {
            return MParticle.IdentityType.CustomerId;
        } else if ("facebook".equals(idTypeString)) {
            return MParticle.IdentityType.Facebook;
        } else if ("twitter".equals(idTypeString)) {
            return MParticle.IdentityType.Twitter;
        } else if ("google".equals(idTypeString)) {
            return MParticle.IdentityType.Google;
        } else if ("microsoft".equals(idTypeString)) {
            return MParticle.IdentityType.Microsoft;
        } else if ("yahoo".equals(idTypeString)) {
            return MParticle.IdentityType.Yahoo;
        } else if ("email".equals(idTypeString)) {
            return MParticle.IdentityType.Email;
        } else if ("facebookcustomaudienceid".equals(idTypeString)) {
            return MParticle.IdentityType.FacebookCustomAudienceId;
        } else if ("alias".equals(idTypeString)) {
            return MParticle.IdentityType.Alias;
        } else if ("mobile_number".equals(idTypeString)) {
            return MParticle.IdentityType.MobileNumber;
        } else if ("phone_number_2".equals(idTypeString)) {
            return MParticle.IdentityType.PhoneNumber2;
        } else if ("phone_number_3".equals(idTypeString)) {
            return MParticle.IdentityType.PhoneNumber3;
        }
        return null;
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

    String getOperatingSystemString() {
        switch (mOperatingSystem) {
            case ANDROID:
                return PLATFORM_ANDROID;
            case FIRE_OS:
                return PLATFORM_FIRE;
            default:
                return PLATFORM_ANDROID;
        }
    }
}