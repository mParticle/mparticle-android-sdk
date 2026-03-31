package com.mparticle.kits

import android.content.Context
import android.content.Intent
import com.mparticle.MParticle.IdentityType
import com.mparticle.internal.MPUtility
import com.mparticle.internal.MPUtility.AdIdInfo
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.LogoutListener
import com.mparticle.kits.KitIntegration.PushListener
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

abstract class AdobeKitBase :
    KitIntegration(),
    AttributeListener,
    LogoutListener,
    PushListener,
    ApplicationStateListener {
    private val dVer = "2"
    var url: String? = DEFAULT_URL
    private var mOrgId: String? = null
    private var requestInProgress = false

    @Throws(IllegalArgumentException::class)
    public override fun onKitCreate(
        map: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        mOrgId = map[ORG_ID_KEY]
        if (map.containsKey(AUDIENCE_MANAGER_SERVER)) {
            url = map[AUDIENCE_MANAGER_SERVER]
        }
        syncIds()
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun onApplicationForeground() {
        syncIds()
    }

    override fun onApplicationBackground() {
        syncIds()
    }

    fun setUserAttribute(
        s: String,
        s1: String,
    ) {
        syncIds()
    }

    override fun setUserAttributeList(
        s: String,
        list: List<String>,
    ) {
        syncIds()
    }

    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        map: Map<String, String>,
        map1: Map<String, List<String>>,
    ) {
        syncIds()
    }

    override fun onRemoveUserAttribute(
        key: String,
        user: FilteredMParticleUser,
    ) {
        syncIds()
    }

    override fun onSetUserAttribute(
        key: String,
        value: Any?,
        user: FilteredMParticleUser,
    ) {
        if (value == null || value !is String) {
            return
        }
        setUserAttribute(key, value)
    }

    override fun setUserIdentity(
        identityType: IdentityType,
        s: String,
    ) {
        syncIds()
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        syncIds()
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    override fun willHandlePushMessage(intent: Intent): Boolean = false

    override fun onPushMessageReceived(
        context: Context,
        intent: Intent,
    ) {
        // No-op: this kit does not implement push message handling.
    }

    override fun onPushRegistration(
        instanceId: String,
        senderId: String,
    ): Boolean {
        syncIds()
        return false
    }

    @Synchronized
    private fun syncIds() {
        if (this.requestInProgress) return
        requestInProgress = true
        executeNetworkRequest {
            try {
                val url = URL("https", url, "/id?" + encodeIds())
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 2000
                urlConnection.readTimeout = 10000
                if (urlConnection.responseCode in 200..299) {
                    val response = MPUtility.getJsonResponse(urlConnection)
                    parseResponse(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            requestInProgress = false
        }
    }

    private fun encodeIds(): String {
        var gaid: String? = null
        val adId = MPUtility.getAdIdInfo(context)
        if (adId != null && adId.advertiser == AdIdInfo.Advertiser.GOOGLE) {
            gaid = adId.id
        }
        val pushId = kitManager.pushInstanceId
        val dBlob = dBlob
        val dcsRegion = dcsRegion
        return encodeIds(
            marketingCloudId,
            mOrgId,
            dBlob,
            dcsRegion,
            pushId,
            gaid,
            currentUser?.userIdentities ?: emptyMap(),
        )
    }

    fun encodeIds(
        marketingCloudId: String?,
        orgId: String?,
        dBlob: String?,
        dcsRegion: String?,
        pushId: String?,
        gaid: String?,
        userIdentities: Map<IdentityType, String>,
    ): String {
        val builder = UrlBuilder()
        builder
            .append(
                D_MID_KEY,
                marketingCloudId,
            ).append(
                D_ORIG_ID_KEY,
                orgId,
            ).append(
                D_BLOB_KEY,
                dBlob,
            ).append(
                DCS_REGION_KEY,
                dcsRegion,
            ).append(
                D_PLATFORM_KEY,
                "android",
            ).append(
                D_VER,
                dVer,
            ).appendCustomIdentity(
                PUSH_TOKEN_KEY,
                pushId,
            ).appendCustomIdentity(
                GOOGLE_AD_ID_KEY,
                gaid,
            )
        for ((key, value) in userIdentities) {
            builder.appendCustomIdentity(
                getServerString(key),
                value,
            )
        }
        return builder.toString()
    }

    override fun getInstance(): Any = AdobeApi(marketingCloudId)

    private fun parseResponse(jsonObject: JSONObject) {
        try {
            val marketingCloudIdKey = jsonObject.getString(D_MID_KEY)
            val dcsRegion = jsonObject.optString(DCS_REGION_KEY)
            val dBlob = jsonObject.optString(D_BLOB_KEY)
            marketingCloudId = marketingCloudIdKey
            setDcsRegion(dcsRegion)
            setDBlob(dBlob)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * fetch the MarketingCloudId. If it can't be found in our storage, assume that this
     * user is migrating from the Adobe SDK and try to fetch it from where the Adobe SDK would store it
     */
    private var marketingCloudId: String?
        get() {
            var marketingCloudIdKey = integrationAttributes[MARKETING_CLOUD_ID_KEY]
            if (KitUtils.isEmpty(marketingCloudIdKey)) {
                var adobeSharedPrefs =
                    context.getSharedPreferences("visitorIDServiceDataStore", Context.MODE_PRIVATE)
                marketingCloudIdKey = adobeSharedPrefs.getString("ADOBEMOBILE_PERSISTED_MID", null)
                if (KitUtils.isEmpty(marketingCloudIdKey)) {
                    adobeSharedPrefs =
                        context.getSharedPreferences("APP_MEASUREMENT_CACHE", Context.MODE_PRIVATE)
                    marketingCloudIdKey = adobeSharedPrefs.getString("ADBMOBILE_PERSISTED_MID", null)
                }
                if (!KitUtils.isEmpty(marketingCloudIdKey)) {
                    marketingCloudId = marketingCloudIdKey
                }
            }
            return marketingCloudIdKey
        }

        private set(id) {
            val integrationAttributes = integrationAttributes
            if (!id.isNullOrEmpty() && !id.equals(integrationAttributes[MARKETING_CLOUD_ID_KEY])) {
                integrationAttributes[MARKETING_CLOUD_ID_KEY] = id
                setIntegrationAttributes(integrationAttributes)
                val adobeSharedPrefs =
                    context.getSharedPreferences("visitorIDServiceDataStore", Context.MODE_PRIVATE)
                adobeSharedPrefs.edit().putString("ADOBEMOBILE_PERSISTED_MID", id).apply()
            }
        }

    private val dcsRegion: String?
        get() = integrationAttributes[AUDIENCE_MANAGER_LOCATION_HINT]

    private fun setDcsRegion(dcsRegion: String) {
        val attrs = integrationAttributes
        attrs[AUDIENCE_MANAGER_LOCATION_HINT] = dcsRegion
        integrationAttributes = attrs
    }

    private val dBlob: String?
        get() = integrationAttributes[AUDIENCE_MANAGER_BLOB]

    private fun setDBlob(dBlob: String) {
        val attrs = integrationAttributes
        attrs[AUDIENCE_MANAGER_BLOB] = dBlob
        integrationAttributes = attrs
    }

    private inner class UrlBuilder {
        var builder: StringBuilder = StringBuilder()
        var hasValue = false

        fun append(
            key: String?,
            value: String?,
        ): UrlBuilder {
            if (KitUtils.isEmpty(key) || KitUtils.isEmpty(value)) {
                return this
            }
            if (hasValue) {
                builder.append("&")
            } else {
                hasValue = true
            }
            builder.append(key)
            builder.append("=")
            builder.append(value)
            return this
        }

        fun appendCustomIdentity(
            key: Int,
            value: String?,
        ): UrlBuilder = append("d_cid", "$key%01$value")

        fun appendCustomIdentity(
            key: String,
            value: String,
        ): UrlBuilder = append("d_cid_ic", "$key%01$value")

        override fun toString(): String = builder.toString()
    }

    companion object {
        const val MARKETING_CLOUD_ID_KEY = "mid"
        private const val ORG_ID_KEY = "organizationID"
        private const val AUDIENCE_MANAGER_BLOB = "aamb"
        private const val AUDIENCE_MANAGER_LOCATION_HINT = "aamlh"
        const val AUDIENCE_MANAGER_SERVER = "audienceManagerServer"
        private const val D_MID_KEY = "d_mid"
        private const val D_ORIG_ID_KEY = "d_orgid"
        private const val D_BLOB_KEY = "d_blob"
        private const val DCS_REGION_KEY = "dcs_region"
        private const val D_PLATFORM_KEY = "d_ptfm"
        private const val D_VER = "d_ver"
        private const val DEFAULT_URL = "dpm.demdex.net"
        private const val PUSH_TOKEN_KEY = 20919
        private const val GOOGLE_AD_ID_KEY = 20914

        // TODO
        // check if these are actually correct and replace
        private fun getServerString(identityType: IdentityType): String =
            when (identityType) {
                IdentityType.Other -> "other"
                IdentityType.CustomerId -> "customerid"
                IdentityType.Facebook -> "facebook"
                IdentityType.Twitter -> "twitter"
                IdentityType.Google -> "google"
                IdentityType.Microsoft -> "microsoft"
                IdentityType.Yahoo -> "yahoo"
                IdentityType.Email -> "email"
                IdentityType.Alias -> "alias"
                IdentityType.FacebookCustomAudienceId -> "facebookcustomaudienceid"
                else -> ""
            }
    }
}
