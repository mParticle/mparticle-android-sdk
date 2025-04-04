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
import com.mparticle.internal.database.UploadSettings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MParticleBaseClientImpl implements MParticleBaseClient {

    private final Context mContext;
    private final ConfigManager mConfigManager;
    private BaseNetworkConnection mRequestHandler;
    private final SharedPreferences mPreferences;
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
        return getUrl(endpoint, null, null,null);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable long mpId) throws MalformedURLException {
        HashMap<String, String> audienceQueryParams = new HashMap<>();
        audienceQueryParams.put("mpid", String.valueOf(mpId));
        return getUrl(endpoint, null, audienceQueryParams);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath) throws MalformedURLException {
        return getUrl(endpoint, identityPath, null,null);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath, HashMap<String, String> audienceQueryParams) throws MalformedURLException {
        return getUrl(endpoint, identityPath,  audienceQueryParams,null);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath,HashMap<String, String> audienceQueryParams, @Nullable UploadSettings uploadSettings) throws MalformedURLException {
        NetworkOptions networkOptions = uploadSettings == null ? mConfigManager.getNetworkOptions() : uploadSettings.getNetworkOptions();
        DomainMapping domainMapping = networkOptions.getDomain(endpoint);
        String url = NetworkOptionsManager.getDefaultUrl(endpoint);
        String apiKey = uploadSettings == null ? mApiKey : uploadSettings.getApiKey();

        // `defaultDomain` variable is for URL generation when domain mapping is specified.
        String defaultDomain = url;
        boolean isDefaultDomain = true;

        // Check if domain mapping is specified and update the URL based on domain mapping
        String domainMappingUrl = domainMapping != null ? domainMapping.getUrl() : null;
        if (!MPUtility.isEmpty(domainMappingUrl)) {
            isDefaultDomain = url.equals(domainMappingUrl);
            url = domainMappingUrl;
        }

        if (endpoint != Endpoint.CONFIG) {
            // Set URL with pod prefix if it’s the default domain and endpoint is not CONFIG
            if (isDefaultDomain) {
                url = getPodUrl(url, mConfigManager.getPodPrefix(apiKey), mConfigManager.isDirectUrlRoutingEnabled());
            } else {
                // When domain mapping is specified, generate the default domain. Whether podRedirection is enabled or not, always use the original URL.
                defaultDomain = getPodUrl(defaultDomain, null, false);
            }
        }

        Uri uri;
        String subdirectory;
        String pathPrefix;
        String pathPostfix;
        boolean overridesSubdirectory = domainMapping != null && domainMapping.isOverridesSubdirectory();
        switch (endpoint) {
            case CONFIG:
                pathPrefix = SERVICE_VERSION_4 + "/";
                subdirectory = overridesSubdirectory ? "" : pathPrefix;
                pathPostfix = mApiKey + "/config";
                Uri.Builder builder = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
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
                return MPUrl.getUrl(builder.build().toString(), generateDefaultURL(isDefaultDomain, builder.build(), defaultDomain, (pathPrefix + pathPostfix)));
            case EVENTS:
                pathPrefix = SERVICE_VERSION_2 + "/";
                subdirectory = overridesSubdirectory ? "" : pathPrefix;
                pathPostfix = apiKey + "/events";
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build();

                return MPUrl.getUrl(uri.toString(), generateDefaultURL(isDefaultDomain, uri, defaultDomain, (pathPrefix + pathPostfix)));
            case ALIAS:
                pathPrefix = SERVICE_VERSION_1 + "/identity/";
                subdirectory = overridesSubdirectory ? "" : pathPrefix;
                pathPostfix = apiKey + "/alias";
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build();
                return MPUrl.getUrl(uri.toString(), generateDefaultURL(isDefaultDomain, uri, defaultDomain, (pathPrefix + pathPostfix)));
            case IDENTITY:
                pathPrefix = SERVICE_VERSION_1 + "/";
                subdirectory = overridesSubdirectory ? "" : SERVICE_VERSION_1 + "/";
                pathPostfix = identityPath;
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build();
                return MPUrl.getUrl(uri.toString(), generateDefaultURL(isDefaultDomain, uri, defaultDomain, (pathPrefix + pathPostfix)));
            case AUDIENCE:
                    pathPostfix = SERVICE_VERSION_1 + "/" + mApiKey + "/audience";
                    uri = new Uri.Builder()
                            .scheme(BuildConfig.SCHEME)
                            .encodedAuthority(url)
                            .path(pathPostfix)
                            .appendQueryParameter("mpid", String.valueOf(mConfigManager.getMpid()))
                            .build();
                    return MPUrl.getUrl(uri.toString(), generateDefaultURL(isDefaultDomain, uri, defaultDomain, pathPostfix));
            default:
                return null;
        }
    }

    /**
     * Generates a new URL using the default domain when domain mapping is specified.
     * This method creates a new URI based on the existing URI, but replaces the domain with the default domain.
     *
     * @param isDefaultDomain If the default domain is true, return null without generating a new URL. If the default domain is false, generate and return a new URL.
     * @param uri             The existing URI which includes the domain mapping.
     * @param defaultDomain   The default domain name to be used in the new URI.
     * @param path            path to be used in the new URI.
     * @return A new URI with the same path and scheme as the original URI, but with the default domain.
     */
    protected MPUrl generateDefaultURL(boolean isDefaultDomain, Uri uri, String defaultDomain, String path) throws MalformedURLException {
        if (isDefaultDomain) {
            return null;
        }
        if (uri != null) {
            Uri.Builder uriBuilder = Uri.parse(uri.toString()).buildUpon();
            if (defaultDomain != null && !defaultDomain.isEmpty()) {
                uriBuilder.encodedAuthority(defaultDomain);
            }
            if (path != null && !path.isEmpty()) {
                uriBuilder.path(path);
            }
            return MPUrl.getUrl(uriBuilder.build().toString(), null);
        }
        return null;
    }

    String getPodUrl(String URLPrefix, String pod, boolean enablePodRedirection) {
        if (URLPrefix != null) {
            String newUrl = enablePodRedirection ? URLPrefix + "." + pod : URLPrefix;
            return newUrl + ".mparticle.com";
        }
        return null;
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
