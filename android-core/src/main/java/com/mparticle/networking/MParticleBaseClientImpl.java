package com.mparticle.networking;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.mparticle.BuildConfig;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MParticleBaseClientImpl implements MParticleBaseClient {

    private Context mContext;
    private ConfigManager mConfigManager;
    private BaseNetworkConnection mRequestHandler;
    private SharedPreferences mPreferences;
    String mApiKey;

    private static final String SERVICE_VERSION_1 = "/v1";
    private static final String SERVICE_VERSION_2 = "/v2";
    private static final String SERVICE_VERSION_4 = "/v4";

    protected static final String REQUEST_ID = "request_id";

    public MParticleBaseClientImpl(Context context, ConfigManager configManager) {
        mContext = context;
        mConfigManager = configManager;
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mRequestHandler = new NetworkConnection(configManager, mPreferences);
        mApiKey = configManager.getApiKey();
    }

    public BaseNetworkConnection getRequestHandler() {
        return mRequestHandler;
    }

    public void setRequestHandler(BaseNetworkConnection handler) {
        mRequestHandler = handler;
    }

    protected MPConnection makeUrlRequest(Endpoint endpoint, MPConnection connection) throws IOException {
        return makeUrlRequest(endpoint, connection, null);
    }

    protected MPConnection makeUrlRequest(Endpoint endpoint, MPConnection connection, String payload) throws IOException {
        return makeUrlRequest(endpoint, connection, payload, true);
    }

    protected MPConnection makeUrlRequest(Endpoint endpoint, MPConnection connection, boolean identity) throws IOException {
        return makeUrlRequest(endpoint, connection, null, identity);
    }

    public MPConnection makeUrlRequest(Endpoint endpoint, MPConnection connection, String payload, boolean identity) throws IOException {
        return mRequestHandler.makeUrlRequest(endpoint, connection, payload, identity);
    }

    protected String getHeaderDateString() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        return format.format(new Date());
    }

    protected String getHeaderHashString(MPConnection request, String date, String message, String apiSecret) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String method = request.getRequestMethod();
        String path = request.getURL().getDefaultUrl().getFile();
        StringBuilder hashString = new StringBuilder()
                .append(method)
                .append("\n")
                .append(date)
                .append("\n")
                .append(path);
        if (message != null) {
            hashString.append(message);
        }
        return MPUtility.hmacSha256Encode(apiSecret, hashString.toString());
    }

    public long getNextRequestTime(Endpoint endpoint) {
        return mPreferences.getLong(endpoint.name() + ":" + Constants.PrefKeys.NEXT_REQUEST_TIME, 0);
    }

    protected MPUrl getUrl(Endpoint endpoint) throws MalformedURLException {
        return getUrl(endpoint, null,null);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable long mpId) throws MalformedURLException {
        HashMap<String, String> audienceQueryParams = new HashMap<>();
        audienceQueryParams.put("mpid", String.valueOf(mpId));
        return getUrl(endpoint, null, audienceQueryParams);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath, HashMap<String, String> audienceQueryParams) throws MalformedURLException {
        return getUrl(endpoint, identityPath, false,audienceQueryParams);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath, boolean forceDefaultUrl, HashMap<String, String> audienceQueryParams) throws MalformedURLException {
        NetworkOptions networkOptions = mConfigManager.getNetworkOptions();
        DomainMapping domainMapping = networkOptions.getDomain(endpoint);
        String url = NetworkOptionsManager.getDefaultUrl(endpoint);
        boolean isDefaultUrl = true;
        if (domainMapping != null && !MPUtility.isEmpty(domainMapping.getUrl()) && !forceDefaultUrl) {
            String domainMappingUrl = domainMapping.getUrl();
            isDefaultUrl = url.equals(domainMappingUrl);
            url = domainMappingUrl;
        }
        Uri.Builder uri;
        MPUrl defaultUrl = !isDefaultUrl ? getUrl(endpoint, identityPath, true, null) : null;
        String subdirectory;
        boolean overridesSubdirectory = domainMapping.isOverridesSubdirectory() && !forceDefaultUrl;
        switch (endpoint) {
            case CONFIG:
                subdirectory = overridesSubdirectory ? "" : SERVICE_VERSION_4 + "/";
                Uri.Builder builder = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + mApiKey + "/config")
                        .appendQueryParameter("av", MPUtility.getAppVersionName(mContext))
                        .appendQueryParameter("sv", Constants.MPARTICLE_VERSION);
                if (mConfigManager.getDataplanId() != null) {
                    builder.appendQueryParameter("plan_id", mConfigManager.getDataplanId());
                    Integer dataplanVersion = mConfigManager.getDataplanVersion();
                    if (dataplanVersion != null) {
                        if (dataplanVersion > 0 && dataplanVersion < 1001) {
                            builder.appendQueryParameter("plan_version", mConfigManager.getDataplanVersion().toString());
                        } else {
                            Logger.warning("Dataplan version of " + dataplanVersion + " is out of range and will not be used to fetch remote dataplan. Version must be between 1 and 1000.");
                        }
                    }
                }
                uri = builder;
                return MPUrl.getUrl(uri.build().toString(), defaultUrl);
            case EVENTS:
                subdirectory = overridesSubdirectory ? "" : SERVICE_VERSION_2 + "/";
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + mApiKey + "/events");
                return MPUrl.getUrl(uri.build().toString(), defaultUrl);
            case ALIAS:
                subdirectory = overridesSubdirectory ? "" : SERVICE_VERSION_1 + "/identity/";
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + mApiKey + "/alias");
                return MPUrl.getUrl(uri.build().toString(), defaultUrl);
            case IDENTITY:
                subdirectory = overridesSubdirectory ? "" : SERVICE_VERSION_1 + "/";
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + identityPath);
                return MPUrl.getUrl(uri.build().toString(), defaultUrl);
            case AUDIENCE:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(SERVICE_VERSION_1 + "/" + mApiKey + "/audience");
                if (audienceQueryParams != null && !audienceQueryParams.isEmpty()) {
                    for (Map.Entry<String, String> entry : audienceQueryParams.entrySet()) {
                        uri.appendQueryParameter(entry.getKey(), entry.getValue());
                    }
                }

                return MPUrl.getUrl(uri.build().toString(), defaultUrl);
            default:
                return null;
        }
    }

    public enum Endpoint {
        CONFIG(1),
        IDENTITY(2),
        EVENTS(3),
        AUDIENCE(4),
        ALIAS(5);

        public int value;

        Endpoint(int value) {
            this.value = value;
        }

        static Endpoint parseInt(int value) {
            switch (value) {
                case 1:
                    return CONFIG;
                case 2:
                    return IDENTITY;
                case 3:
                    return EVENTS;
                case 4:
                    return AUDIENCE;
                case 5:
                    return ALIAS;
                default:
                    return null;
            }
        }
    }
}
