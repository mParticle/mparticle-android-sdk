package com.mparticle.networking

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.identity.AccessUtils
import com.mparticle.identity.MParticleIdentityClient
import com.mparticle.internal.Constants
import com.mparticle.internal.MParticleApiClientImpl
import com.mparticle.internal.MParticleApiClientImpl.MPNoConfigException
import java.io.IOException
import java.net.MalformedURLException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class PinningTestHelper internal constructor(
    var mContext: Context,
    path: String,
    var mCallback: Callback
) {
    init {
        prepareIdentityApiClient(path)
        prepareMParticleApiClient(path)
    }

    private fun prepareIdentityApiClient(path: String) {
        AccessUtils.setDefaultIdentityApiClient(mContext)
        //        com.mparticle.identity.AccessUtils.setIdentityApiClientScheme("https");
        val apiClient: MParticleIdentityClient = AccessUtils.getIdentityApiClient()
        setRequestClient(apiClient, path)
    }

    private fun prepareMParticleApiClient(path: String) {
        try {
            com.mparticle.internal.AccessUtils.setMParticleApiClient(
                MParticleApiClientImpl(
                    MParticle.getInstance()?.Internal()?.configManager,
                    mContext.getSharedPreferences(
                        Constants.PREFS_FILE, Context.MODE_PRIVATE
                    ),
                    mContext
                )
            )
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: MPNoConfigException) {
            e.printStackTrace()
        }
        //        com.mparticle.internal.AccessUtils.setMParticleApiClientProtocol("https");
        val apiClient = com.mparticle.internal.AccessUtils.getApiClient()
        setRequestClient(apiClient, path)
    }

    private fun setRequestClient(client: MParticleBaseClient, path: String) {
        val requestHandler = client.requestHandler
        client.requestHandler = object : BaseNetworkConnection(mContext) {
            @Throws(IOException::class)
            override fun makeUrlRequest(
                endpoint: MParticleBaseClientImpl.Endpoint,
                connection: MPConnection,
                payload: String,
                identity: Boolean
            ): MPConnection {
                var connection = connection
                connection = try {
                    requestHandler.makeUrlRequest(endpoint, connection, null, identity)
                } finally {
                    if (connection.url.toString().contains(path)) {
                        val finalConnection = connection
                        Handler(Looper.getMainLooper()).post {
                            mCallback.onPinningApplied(
                                finalConnection.isHttps && finalConnection.sslSocketFactory != null
                            )
                        }
                    }
                }
                return connection
            }
        }
    }

    /**
     * The only way I have been able to find if we are pinning certificates or not, is to test
     * whether the HttpsURLConnection is using its origin SSLSocketFactory or not. This does not
     * tell us if it actually has certificates pinned, but rather, if it has ever been set. Not the
     * best approach, but there is no easier way, without doing some Reflection, which we should
     * eventually do.
     */
    private fun isPinned(connection: HttpsURLConnection): Boolean {
        return connection.sslSocketFactory !== SSLSocketFactory.getDefault()
    }

    interface Callback {
        fun onPinningApplied(pinned: Boolean)
    }
}