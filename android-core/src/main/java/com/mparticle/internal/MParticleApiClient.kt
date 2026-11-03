package com.mparticle.internal

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.mparticle.audience.BaseAudienceTask
import com.mparticle.internal.MParticleApiClientImpl.MPConfigException
import com.mparticle.internal.MParticleApiClientImpl.MPRampException
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.internal.database.UploadSettings
import com.mparticle.networking.MParticleBaseClient
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * API client interface primarily needed for mocking/testing.
 */
interface MParticleApiClient : MParticleBaseClient {
    @Throws(IOException::class, MPConfigException::class)
    fun fetchConfig()

    @Throws(IOException::class, MPConfigException::class)
    fun fetchConfig(force: Boolean)

    @Throws(IOException::class, MPThrottleException::class, MPRampException::class)
    fun sendMessageBatch(message: String?, uploadSettings: UploadSettings?): Int

    fun fetchUserAudience(task: BaseAudienceTask?, mpId: Long)

    fun getCookies(): JSONObject?

    @NonNull
    @Throws(JSONException::class, IOException::class, MPThrottleException::class, MPRampException::class)
    fun sendAliasRequest(message: String?, uploadSettings: UploadSettings?): AliasNetworkResponse

    open class AliasNetworkResponse internal constructor(private val responseCode: Int, private var errorMessage: String?) {
        constructor(responseCode: Int) : this(responseCode, null)

        open fun getResponseCode(): Int = responseCode

        @Nullable
        open fun getErrorMessage(): String? = errorMessage

        @JvmName("setErrorMessage")
        internal fun setErrorMessage(errorMessage: String?) {
            this.errorMessage = errorMessage
        }
    }
}
