package com.mparticle.networking;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.mparticle.BuildConfig;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MParticleBaseClientImpl implements MParticleBaseClient {

    private Context mContext;
    private ConfigManager mConfigManager;
    private BaseNetworkConnection mRequestHandler;
    private SharedPreferences mPreferences;
    private String mApiKey;

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
        String path = request.getURL().getFile();
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
        return getUrl(endpoint, null);
    }

    protected MPUrl getUrl(Endpoint endpoint, @Nullable String identityPath) throws MalformedURLException {
        NetworkOptions networkOptions = mConfigManager.getNetworkOptions();
        DomainMapping domainMapping = networkOptions.getDomain(endpoint);
        String url = NetworkOptionsManager.getDefaultUrl(endpoint);
        if (domainMapping != null && !MPUtility.isEmpty(domainMapping.getUrl())) {
            url = domainMapping.getUrl();
        }
        Uri uri;
        switch (endpoint) {
            case CONFIG:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(SERVICE_VERSION_4 + "/" + mApiKey + "/config")
                        .appendQueryParameter("av", MPUtility.getAppVersionName(mContext))
                        .appendQueryParameter("sv", Constants.MPARTICLE_VERSION)
                        .build();
                return MPUrl.getUrl(uri.toString());
            case EVENTS:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(SERVICE_VERSION_2 + "/" + mApiKey + "/events")
                        .build();
                return MPUrl.getUrl(uri.toString());
            case ALIAS:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(SERVICE_VERSION_1 + "/identity/" + mApiKey + "/alias")
                        .build();
                return MPUrl.getUrl(uri.toString());
            case IDENTITY:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(identityPath)
                        .build();
                return MPUrl.getUrl(uri.toString());
            case AUDIENCE:
                uri = new Uri.Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(SERVICE_VERSION_2 + "/" + mApiKey + "/audience?mpID=" + mConfigManager.getMpid())
                        .build();
                return MPUrl.getUrl(uri.toString());
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
