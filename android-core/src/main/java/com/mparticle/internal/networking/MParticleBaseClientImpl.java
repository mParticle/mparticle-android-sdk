package com.mparticle.internal.networking;

import android.content.SharedPreferences;

import com.mparticle.BuildConfig;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class MParticleBaseClientImpl implements MParticleBaseClient {
    private BaseNetworkConnection mRequestHandler;
    private SharedPreferences mPreferences;
    private String customScheme = null;

    public MParticleBaseClientImpl(SharedPreferences sharedPreferences) {
        mPreferences = sharedPreferences;
        mRequestHandler = new NetworkConnection(sharedPreferences);
    }

    public BaseNetworkConnection getRequestHandler() {
        return mRequestHandler;
    }

    public void setRequestHandler(BaseNetworkConnection handler) {
        mRequestHandler = handler;
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection) throws IOException {
        return makeUrlRequest(connection, null);
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload) throws IOException {
        return makeUrlRequest(connection, payload, true);
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection, boolean identity) throws IOException {
        return makeUrlRequest(connection, null, identity);
    }

    public HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload, boolean identity) throws IOException {
        return mRequestHandler.makeUrlRequest(connection, payload, identity);
    }

    protected String getHeaderDateString() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        return format.format(new Date());
    }

    protected String getHeaderHashString(HttpURLConnection request, String date, String message, String apiSecret) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
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

    public long getNextRequestTime() {
        return mPreferences.getLong(Constants.PrefKeys.NEXT_REQUEST_TIME, 0);
    }

    public void setScheme(String customScheme) {
        this.customScheme = customScheme;
    }

    protected String getScheme() {
        if (!MPUtility.isEmpty(customScheme)) {
            return customScheme;
        }
        return BuildConfig.SCHEME;
    }
}
