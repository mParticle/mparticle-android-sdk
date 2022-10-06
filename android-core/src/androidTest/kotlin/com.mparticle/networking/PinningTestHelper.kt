package com.mparticle.networking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mparticle.MParticle;
import com.mparticle.identity.MParticleIdentityClient;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.testutils.TestingUtils;

import java.io.IOException;
import java.net.MalformedURLException;

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
        com.mparticle.identity.AccessUtils.setDefaultIdentityApiClient(mContext);
//        com.mparticle.identity.AccessUtils.setIdentityApiClientScheme("https");
        MParticleIdentityClient apiClient = com.mparticle.identity.AccessUtils.getIdentityApiClient();
        setRequestClient(apiClient, path);
    }

    private void prepareMParticleApiClient(String path) {
        try {
            AccessUtils.setMParticleApiClient(new MParticleApiClientImpl(MParticle.getInstance().Internal().getConfigManager(), mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE), mContext));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            e.printStackTrace();
        }
//        com.mparticle.internal.AccessUtils.setMParticleApiClientProtocol("https");
        MParticleApiClient apiClient = com.mparticle.internal.AccessUtils.getApiClient();
        setRequestClient(apiClient, path);
    }

    private void setRequestClient(MParticleBaseClient client, final String path) {
        final BaseNetworkConnection requestHandler = client.getRequestHandler();
        client.setRequestHandler(new BaseNetworkConnection(mContext) {
            @Override
            public MPConnection makeUrlRequest(MParticleBaseClientImpl.Endpoint endpoint, MPConnection connection, String payload, boolean identity) throws IOException {
                try {
                    connection = requestHandler.makeUrlRequest(endpoint, connection, null, identity);
                }
                finally {
                    if (connection.getURL().toString().contains(path)) {
                        final MPConnection finalConnection = connection;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onPinningApplied(finalConnection.isHttps() && finalConnection.getSSLSocketFactory() != null);
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
     * whether the HttpsURLConnection is using its origin SSLSocketFactory or not. This does not
     * tell us if it actually has certificates pinned, but rather, if it has ever been set. Not the
     * best approach, but there is no easier way, without doing some Reflection, which we should
     * eventually do.
     */
    private boolean isPinned(HttpsURLConnection connection) {
        return connection.getSSLSocketFactory() != SSLSocketFactory.getDefault();
    }

    interface Callback {
        void onPinningApplied(boolean pinned);
    }
}
