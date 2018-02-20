package com.mparticle.networking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mparticle.identity.MParticleIdentityClient;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.testutils.MParticleUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class PinningTestHelper {
    Context mContext;
    Callback mCallback;

    PinningTestHelper(Context context, String path, Callback callback) {
        mContext = context;
        mCallback = callback;
        prepareIdentityApiClient(path);
        prepareMParticleApiClient(path);
    }

    private void prepareIdentityApiClient(String path) {
        MParticleUtils.getInstance().setDefaultIdentityClient(mContext);
        com.mparticle.identity.AccessUtils.setIdentityApiClientScheme("https");
        MParticleIdentityClient apiClient = com.mparticle.identity.AccessUtils.getIdentityApiClient();
        setRequestClient(apiClient, path);
    }

    private void prepareMParticleApiClient(String path) {
        MParticleUtils.getInstance().setDefaultClient(mContext);
        com.mparticle.internal.AccessUtils.setMParticleApiClientProtocol("https");
        MParticleApiClient apiClient = com.mparticle.internal.AccessUtils.getApiClient();
        setRequestClient(apiClient, path);
    }

    private void setRequestClient(MParticleBaseClient client, final String path) {
        final BaseNetworkConnection requestHandler = client.getRequestHandler();
        client.setRequestHandler(new BaseNetworkConnection(mContext) {
            @Override
            public HttpURLConnection makeUrlRequest(MParticleBaseClientImpl.Endpoint endpoint, HttpURLConnection connection, String payload, boolean identity) throws IOException {
                try {
                    connection = requestHandler.makeUrlRequest(endpoint, connection, null, identity);
                }
                finally {
                    if (connection.getURL().toString().contains(path)) {
                        final HttpURLConnection finalConnection = connection;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalConnection instanceof HttpsURLConnection) {
                                    HttpsURLConnection sslConnection = (HttpsURLConnection) finalConnection;
                                    mCallback.onPinningApplied(isPinned(sslConnection));
                                } else {
                                    mCallback.onPinningApplied(false);
                                }
                            }
                        });
                    }
                }
                return connection;
            }
        });
    }

    /**
     * The only way I have been able to find if we are pinning certificates or not, is to test
     * whether the HttpsURLConnection is using it's origin SSLSocketFactory or not. This does not
     * tell us if it actually has certificates pinned, but rather, if it has ever been set. Not the
     * best approach, but there is not easier way, without doing some Reflection, which we should
     * eventually do
     */
    private boolean isPinned(HttpsURLConnection connection) {
        return connection.getSSLSocketFactory() != SSLSocketFactory.getDefault();
    }

    interface Callback {
        void onPinningApplied(boolean pinned);
    }
}
