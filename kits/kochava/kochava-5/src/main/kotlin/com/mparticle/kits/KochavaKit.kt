package com.mparticle.kits

import android.content.Context
import android.content.Intent
import android.location.Location
import com.kochava.tracker.Tracker
import com.kochava.tracker.log.LogLevel
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.LogoutListener
import org.json.JSONException
import org.json.JSONObject

class KochavaKit :
    KitIntegration(),
    AttributeListener,
    LogoutListener,
    KitIntegration.IdentityListener {
    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage>? {
        val attributionEnabled = java.lang.Boolean.parseBoolean(getSettings()[RETRIEVE_ATT_DATA])
        var logLevel = LogLevel.NONE
        if (java.lang.Boolean.parseBoolean(getSettings()[ENABLE_LOGGING])) {
            logLevel = LogLevel.DEBUG
        }
        Tracker.getInstance().setLogLevel(logLevel)

        Tracker
            .getInstance()
            .setAppLimitAdTracking(java.lang.Boolean.parseBoolean(getSettings()[LIMIT_ADD_TRACKING]))
        val configuration = getSettings()[APP_ID]
        if (configuration != null) {
            Tracker.getInstance().startWithAppGuid(context.applicationContext, configuration)

            identityLink?.let {
                for (link in it) {
                    Tracker.getInstance().registerIdentityLink(link.key, link.value)
                }
            }
            try {
                if (attributionEnabled) {
                    val currentInstallAttribution = Tracker.getInstance().installAttribution
                    if (!currentInstallAttribution.isRetrieved) {
                        Tracker.getInstance().retrieveInstallAttribution { installAttribution ->
                            try {
                                setAttributionResultParameter(
                                    ATTRIBUTION_PARAMETERS,
                                    installAttribution.toJson(),
                                )
                            } catch (e: JSONException) {
                                val error =
                                    AttributionError()
                                        .setMessage("unable to parse attribution JSON:\n $installAttribution")
                                kitManager.onError(error)
                            }
                        }
                    }
                    Tracker
                        .getInstance()
                        .processDeeplink(kitManager.launchUri.toString()) { deeplink ->
                            setAttributionResultParameter(
                                ENHANCED_DEEPLINK_PARAMETERS,
                                deeplink.toJson(),
                            )
                        }
                }
            } catch (e: Exception) {
                e.toString()
            }
        }
        return null
    }

    override fun setLocation(location: Location) {
    }

    override fun setUserAttribute(
        attributeKey: String,
        attributeValue: String,
    ) {}

    override fun setUserAttributeList(
        s: String,
        list: List<String>,
    ) {}

    override fun supportsAttributeLists(): Boolean = true

    override fun setAllUserAttributes(
        map: Map<String, String>,
        map1: Map<String, List<String>>,
    ) {}

    fun removeUserAttribute(key: String) {}

    override fun onRemoveUserAttribute(
        key: String,
        user: FilteredMParticleUser,
    ) {
        removeUserAttribute(key)
    }

    override fun setInstallReferrer(intent: Intent) {}

    override fun setUserIdentity(
        identityType: IdentityType,
        id: String,
    ) {
        val possibleIdentities = listOf(USER_IDENTIFICATION_TYPE, EMAIL_IDENTIFICATION_TYPE)
        possibleIdentities.forEach {
            if (it == identityType.name) {
                Tracker.getInstance().registerIdentityLink(it, id)
            }
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {}

    override fun logout(): List<ReportingMessage> = emptyList()

    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> {
        Tracker.getInstance().setAppLimitAdTracking(optOutStatus)

        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ).setOptOut(optOutStatus),
        )
    }

    private fun setAttributionResultParameter(
        key: String,
        value: JSONObject,
    ) {
        try {
            val parameters = JSONObject().put(key, value)
            val result =
                AttributionResult()
                    .setServiceProviderId(configuration.kitId)
                    .setParameters(parameters)
            kitManager.onResult(result)
        } catch (e: JSONException) {
            val error =
                AttributionError()
                    .setServiceProviderId(configuration.kitId)
                    .setMessage(e.message)
            kitManager.onError(error)
        }
    }

    companion object {
        const val ATTRIBUTION_PARAMETERS = "attribution"
        const val DEEPLINK_PARAMETERS = "deeplink"
        const val ENHANCED_DEEPLINK_PARAMETERS = "enhancedDeeplink"
        private const val APP_ID = "appId"
        private const val USER_IDENTIFICATION_TYPE = "CustomerId"
        private const val EMAIL_IDENTIFICATION_TYPE = "Email"
        private const val LIMIT_ADD_TRACKING = "limitAdTracking"
        private const val RETRIEVE_ATT_DATA = "retrieveAttributionData"
        private const val ENABLE_LOGGING = "enableLogging"
        const val NAME = "Kochava"
        private var identityLink: Map<String, String>? = null

        fun setIdentityLink(identityLink: Map<String, String>?) {
            Companion.identityLink = identityLink
        }
    }

    override fun onIdentifyCompleted(
        user: MParticleUser?,
        p1: FilteredIdentityApiRequest?,
    ) {
    }

    override fun onLoginCompleted(
        user: MParticleUser?,
        p1: FilteredIdentityApiRequest?,
    ) {
        val identityLinks = mutableMapOf<String, String>()
        user?.userIdentities?.iterator()?.forEach {
            identityLinks.put(it.key.name, it.value)
            setUserIdentity(it.key, it.value)
        }
        setIdentityLink(identityLink)
    }

    override fun onLogoutCompleted(
        user: MParticleUser?,
        p1: FilteredIdentityApiRequest?,
    ) {
    }

    override fun onModifyCompleted(
        user: MParticleUser?,
        p1: FilteredIdentityApiRequest?,
    ) {
    }

    override fun onUserIdentified(user: MParticleUser?) {
    }
}
