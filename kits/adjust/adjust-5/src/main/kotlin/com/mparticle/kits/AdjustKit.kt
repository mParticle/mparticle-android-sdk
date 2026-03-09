package com.mparticle.kits

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustInstance
import com.adjust.sdk.AdjustReferrerReceiver
import com.adjust.sdk.LogLevel
import com.adjust.sdk.OnAttributionChangedListener
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedList

/**
 * Embedded implementation of the Adjust SDK
 *
 *
 */
class AdjustKit :
    KitIntegration(),
    OnAttributionChangedListener,
    ActivityLifecycleCallbacks {
    override fun getInstance(): AdjustInstance = Adjust.getDefaultInstance()

    override fun getName(): String = KIT_NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val production = MParticle.Environment.Production == MParticle.getInstance()?.environment
        val config =
            AdjustConfig(
                getContext(),
                getSettings()[APP_TOKEN],
                if (production) AdjustConfig.ENVIRONMENT_PRODUCTION else AdjustConfig.ENVIRONMENT_SANDBOX,
            )
        config.setOnAttributionChangedListener(this)
        if (deeplinkResponseListenerProxy != null) {
            val listener = deeplinkResponseListenerProxy
            if (listener != null) {
                config.setOnDeferredDeeplinkResponseListener { deeplink ->
                    listener.launchReceivedDeeplink(
                        deeplink,
                    )
                }
            }
            deeplinkResponseListenerProxy = null
        }
        if (!production) {
            config.setLogLevel(LogLevel.VERBOSE)
        }
        val fbAppId = getSettings()[FB_APP_ID_KEY]
        if (fbAppId != null) {
            config.setFbAppId(fbAppId)
        }
        Adjust.initSdk(config)
        setAdidIntegrationAttribute()
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        return emptyList()
    }

    override fun setInstallReferrer(intent: Intent) {
        AdjustReferrerReceiver().onReceive(context, intent)
    }

    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> {
        if (!optOutStatus) Adjust.enable() else Adjust.disable()
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ).setOptOut(optOutStatus),
        )
        return messageList
    }

    override fun onAttributionChanged(attribution: AdjustAttribution) {
        // if Attribution has not been fetch yet the argument
        // will be null, in this case we should do nothing and wait for
        // the asynchronous callback to return
        var jsonObject = JSONObject()
        try {
            jsonObject = toJSON(attribution)
        } catch (e: JSONException) {
            val error =
                AttributionError()
                    .setMessage(e.message)
                    .setServiceProviderId(MParticle.ServiceProviders.ADJUST)
            kitManager.onError(error)
        }
        val deepLinkResult =
            AttributionResult()
                .setParameters(jsonObject)
                .setServiceProviderId(MParticle.ServiceProviders.ADJUST)
        kitManager.onResult(deepLinkResult)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private fun setAdidIntegrationAttribute() {
        val integrationAttributes = integrationAttributes
        Adjust.getAdid { adid ->
            if (adid != null) {
                integrationAttributes[ADJUST_ID_KEY] = adid
                setIntegrationAttributes(integrationAttributes)
            }
        }
    }

    companion object {
        private const val APP_TOKEN = "appToken"
        private const val ADJUST_ID_KEY = "adid"
        private const val FB_APP_ID_KEY = "fbAppId"
        private const val KIT_NAME = "Adjust"

        var deeplinkResponseListenerProxy: OnDeeplinkEventListener? = null

        @JvmStatic
        @Throws(JSONException::class)
        fun toJSON(attribution: AdjustAttribution): JSONObject =
            JSONObject()
                .putOpt("tracker_token", attribution.trackerToken)
                .putOpt("tracker_name", attribution.trackerName)
                .putOpt("network", attribution.network)
                .putOpt("campaign", attribution.campaign)
                .putOpt("adgroup", attribution.adgroup)
                .putOpt("creative", attribution.creative)
                .putOpt("click_label", attribution.clickLabel)
    }
}
