package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.TypedUserAttributeListener
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.KitIntegration.CommerceListener
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import com.optimizely.ab.android.sdk.OptimizelyStartListener
import java.math.BigDecimal
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.Queue

open class OptimizelyKit :
    KitIntegration(),
    KitIntegration.EventListener,
    CommerceListener,
    OptimizelyStartListener {
    @JvmField
    val mEventQueue: Queue<OptimizelyEvent> = LinkedList()

    override fun getName(): String = KIT_NAME

    @Throws(IllegalArgumentException::class)
    public override fun onKitCreate(
        map: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val sdkKey = map[PROJECT_ID]
        val eventInterval = tryParse(map[EVENT_INTERVAL])
        val datafileDownloadInterval = tryParse(map[DATAFILE_INTERVAL])
        if (!providedClient && (mOptimizelyClient == null || !mOptimizelyClient!!.isValid)) {
            val builder =
                OptimizelyManager
                    .builder()
                    .withSDKKey(sdkKey)
            if (eventInterval != null) {
                builder.withEventDispatchInterval(eventInterval)
            }
            if (datafileDownloadInterval != null) {
                builder.withDatafileDownloadInterval(datafileDownloadInterval)
            }
            val optimizelyManager = builder.build(context)
            optimizelyManager.initialize(context, null, this)
        }
        return emptyList()
    }

    override fun setOptOut(b: Boolean): List<ReportingMessage> = emptyList()

    override fun leaveBreadcrumb(s: String): List<ReportingMessage> = emptyList()

    override fun logError(
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        e: Exception,
        map: Map<String, String>,
        s: String,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage>? {
        val user = currentUser
        var customUserId: String? = null
        var valueString: String? = null
        mpEvent.customFlags?.let {
            val valueList = it[OPTIMIZELY_VALUE_KEY]
            if (valueList != null && valueList.size > 0) {
                valueString = valueList[0]
            }
            val userIdList = it[OPTIMIZELY_USER_ID]
            if (userIdList != null && userIdList.size > 0) {
                customUserId = userIdList[0]
            }
        }
        val optimizelyCustomUserId = customUserId
        val optimizelyValueString = valueString
        val eventCreated =
            getOptimizelyEvent(
                mpEvent,
                user,
                object : OptimizelyEventCallback {
                    override fun onOptimizelyEventCreated(event: OptimizelyEvent) {
                        if (!MPUtility.isEmpty(optimizelyValueString)) {
                            try {
                                val value = optimizelyValueString!!.toDouble()
                                event.addEventAttribute("value", value)
                                Logger.debug(
                                    String.format(
                                        "Applying custom value: \"%s\" to Optimizely Event based on customFlag",
                                        value.toString(),
                                    ),
                                )
                            } catch (ex: NumberFormatException) {
                                Logger.error(
                                    String.format(
                                        "Unable to log Optimizely Value \"%s\", failed to parse as a Double",
                                        optimizelyValueString,
                                    ),
                                )
                            }
                        }
                        if (!MPUtility.isEmpty(optimizelyCustomUserId)) {
                            event.userId = optimizelyCustomUserId
                            Logger.debug(
                                String.format(
                                    "Applying custom userId: \"%s\" to Optimizely Event based on customFlag",
                                    optimizelyCustomUserId,
                                ),
                            )
                        }
                        logOptimizelyEvent(event)
                    }
                },
            )
        return if (!eventCreated) {
            null
        } else {
            listOf(ReportingMessage.fromEvent(this, mpEvent))
        }
    }

    override fun logScreen(
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logLtvIncrease(
        bigDecimal: BigDecimal,
        bigDecimal1: BigDecimal,
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        val user = currentUser
        var customEventName: String? = null
        var customUserId: String? = null
        var valueString: String? = null
        commerceEvent.customFlags?.let {
            val eventNameFlags = it[OPTIMIZELY_EVENT_NAME]
            val userIdFlags = it[OPTIMIZELY_USER_ID]
            val valueList = it[OPTIMIZELY_VALUE_KEY]
            if (eventNameFlags != null) {
                customEventName = eventNameFlags[0]
            }
            if (userIdFlags != null) {
                customUserId = userIdFlags[0]
            }
            if (valueList != null && valueList.size > 0) {
                valueString = valueList[0]
            }
        }
        val events = CommerceEventUtils.expand(commerceEvent)
        val optimizelyCustomEventName = customEventName
        val optimizelyCustomUserId = customUserId
        val optimizelyValueString = valueString
        for (event in events) {
            getOptimizelyEvent(
                event,
                user,
                object : OptimizelyEventCallback {
                    override fun onOptimizelyEventCreated(optimizelyEvent: OptimizelyEvent) {
                        // If the event is a Purchase or Refund expanded event
                        if (commerceEvent.productAction != null &&
                            event.eventName ==
                            String.format(
                                CommerceEventUtils.PLUSONE_NAME,
                                commerceEvent.productAction,
                            )
                        ) {
                            // parse and apply the "revenue"
                            val totalAmountString =
                                event.customAttributeStrings?.get(CommerceEventUtils.Constants.ATT_TOTAL)
                            if (!MPUtility.isEmpty(totalAmountString)) {
                                try {
                                    val totalAmount = java.lang.Double.valueOf(totalAmountString)
                                    val revenueInCents =
                                        java.lang.Double
                                            .valueOf(totalAmount * 100)
                                            .toInt()
                                    optimizelyEvent.eventAttributes?.set("revenue", revenueInCents)
                                    Logger.debug(
                                        String.format(
                                            "Applying revenue: \"%s\" to Optimizely Event based on transactionAttributes",
                                            revenueInCents,
                                        ),
                                    )
                                } catch (ex: NumberFormatException) {
                                    Logger.error("Unable to parse Revenue value")
                                }
                            }
                            // And apply the custom name, if there is one
                            if (optimizelyCustomEventName != null) {
                                optimizelyEvent.eventName = optimizelyCustomEventName
                                Logger.debug(
                                    String.format(
                                        "Applying custom eventName: \"%s\" to Optimizely Event based on customFlag",
                                        optimizelyCustomEventName,
                                    ),
                                )
                            }
                        }
                        // Apply customId, if there is one, to all expanded events
                        if (optimizelyCustomUserId != null) {
                            optimizelyEvent.userId = optimizelyCustomUserId
                            Logger.debug(
                                String.format(
                                    "Applying custom userId: \"%s\" to Optimizely Event based on customFlag",
                                    optimizelyCustomUserId,
                                ),
                            )
                        }
                        if (!MPUtility.isEmpty(optimizelyValueString)) {
                            try {
                                val value = optimizelyValueString?.toDouble()
                                optimizelyEvent.addEventAttribute("value", value)
                                Logger.debug(
                                    String.format(
                                        "Applying custom value: \"%s\" to Optimizely Event based on customFlag",
                                        value,
                                    ),
                                )
                            } catch (ex: NumberFormatException) {
                                Logger.error(
                                    String.format(
                                        "Unable to log Optimizely Value \"%s\", failed to parse as a Double",
                                        optimizelyValueString,
                                    ),
                                )
                            }
                        }
                        logOptimizelyEvent(optimizelyEvent)
                    }
                },
            )
        }
        return listOf(ReportingMessage.fromEvent(this, commerceEvent))
    }

    public override fun onKitDestroy() {
        super.onKitDestroy()
        mOptimizelyClient = null
        mStartListeners.clear()
    }

    override fun onStart(optimizelyClient: OptimizelyClient) {
        // check providedClient, so we don't override a client that the was set explicitly
        if (!providedClient && optimizelyClient.isValid) {
            mOptimizelyClient = optimizelyClient
            for (listener in mStartListeners) {
                try {
                    listener.onOptimizelyClientAvailable(mOptimizelyClient)
                } catch (e: Exception) {
                }
            }
            mStartListeners.clear()
            replayQueue()
        }
    }

    open fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
        if (mOptimizelyClient != null && mOptimizelyClient!!.isValid) {
            if (trackEvent.eventAttributes == null) {
                mOptimizelyClient?.track(
                    trackEvent.eventName!!,
                    trackEvent.userId!!,
                    trackEvent.userAttributes!!,
                )
            } else {
                mOptimizelyClient?.track(
                    trackEvent.eventName!!,
                    trackEvent.userId!!,
                    trackEvent.userAttributes!!,
                    trackEvent.eventAttributes!!,
                )
            }
        } else {
            queueEvent(trackEvent)
        }
    }

    open fun getUserId(user: MParticleUser?): String {
        var userId: String? = null
        if (user != null) {
            val userIdField = settings[USER_ID_FIELD_KEY]
            if (USER_ID_CUSTOMER_ID_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.CustomerId]
            } else if (USER_ID_EMAIL_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Email]
            } else if (USER_ID_OTHER_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other]
            } else if (USER_ID_OTHER2_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other2]
            } else if (USER_ID_OTHER3_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other3]
            } else if (USER_ID_OTHER4_VALUE.equals(userIdField, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other4]
            } else if (USER_ID_MPID_VALUE.equals(userIdField, true)) {
                userId = user.id.toString()
            } else if (USER_ID_DAS_VALUE.equals(userIdField, true)) {
                userId = MParticle.getInstance()!!.Identity().deviceApplicationStamp
            }
        }
        if (userId == null) {
            userId = MParticle.getInstance()!!.Identity().deviceApplicationStamp
            Logger.debug("Optimizely userId not found, applying DAS as userId by default")
        }
        return userId
    }

    private fun getOptimizelyEvent(
        mpEvent: MPEvent,
        user: MParticleUser?,
        onEventCreated: OptimizelyEventCallback,
    ): Boolean =
        if (!MPUtility.isEmpty(getUserId(user))) {
            val listener: TypedUserAttributeListener =
                object : TypedUserAttributeListener {
                    override fun onUserAttributesReceived(
                        userAttributes: Map<String, *>,
                        map1: Map<String, List<String?>?>,
                        mpid: Long,
                    ) {
                        val attributes: MutableMap<String, Any?> = HashMap()
                        for ((key, value) in userAttributes) {
                            attributes[key] = value
                        }
                        val event: OptimizelyEvent = OptimizelyEvent()
                        val eventName = mpEvent.eventName
                        val userId = getUserId(user)
                        event.eventName = eventName
                        event.userId = userId
                        event.userAttributes = attributes
                        if (mpEvent.customAttributes != null) {
                            event.eventAttributes = mpEvent.customAttributes?.let { HashMap(it) }
                        }
                        onEventCreated.onOptimizelyEventCreated(event)
                    }
                }
            if (user != null) {
                user.getUserAttributes(listener)
            } else {
                listener.onUserAttributesReceived(HashMap<String, String?>(), HashMap(), 0)
            }
            true
        } else {
            false
        }

    private fun queueEvent(event: OptimizelyEvent) {
        mEventQueue.offer(event)
        if (mEventQueue.size > 10) {
            mEventQueue.remove()
        }
    }

    private fun replayQueue() {
        while (!mEventQueue.isEmpty()) {
            mEventQueue.poll()?.let { logOptimizelyEvent(it) }
        }
    }

    private fun tryParse(value: String?): Long? =
        try {
            value!!.toLong()
        } catch (e: Exception) {
            null
        }

    inner class OptimizelyEvent {
        @JvmField
        var eventName: String? = null

        @JvmField
        var userId: String? = null

        @JvmField
        var userAttributes: Map<String, Any?>? = null

        @JvmField
        var eventAttributes: MutableMap<String, Any?>? = null

        fun addEventAttribute(
            key: String,
            value: Any?,
        ) {
            if (eventAttributes == null) {
                eventAttributes = HashMap()
            }
            eventAttributes!![key] = value
        }
    }

    interface OptimizelyClientListener {
        fun onOptimizelyClientAvailable(optimizelyClient: OptimizelyClient?)
    }

    private interface OptimizelyEventCallback {
        fun onOptimizelyEventCreated(event: OptimizelyEvent)
    }

    companion object {
        private var providedClient = false
        private var mOptimizelyClient: OptimizelyClient? = null
        private val mStartListeners: MutableList<OptimizelyClientListener> = ArrayList()
        const val USER_ID_FIELD_KEY = "userIdField"
        const val EVENT_INTERVAL = "eventInterval"
        const val DATAFILE_INTERVAL = "datafileInterval"
        const val PROJECT_ID = "projectId"
        const val USER_ID_CUSTOMER_ID_VALUE = "customerId"
        const val USER_ID_EMAIL_VALUE = "email"
        const val USER_ID_OTHER_VALUE = "otherid"
        const val USER_ID_OTHER2_VALUE = "otherid2"
        const val USER_ID_OTHER3_VALUE = "otherid3"
        const val USER_ID_OTHER4_VALUE = "otherid4"
        const val USER_ID_MPID_VALUE = "mpid"
        const val USER_ID_DAS_VALUE = "deviceApplicationStamp"
        const val OPTIMIZELY_VALUE_KEY = "Optimizely.Value"
        const val OPTIMIZELY_EVENT_NAME = "Optimizely.EventName"
        const val OPTIMIZELY_USER_ID = "Optimizely.UserId"
        const val KIT_NAME = "Optimizely"

        @JvmStatic
        var optimizelyClient: OptimizelyClient?
            get() = mOptimizelyClient
            set(optimizelyClient) {
                mOptimizelyClient = optimizelyClient
                providedClient = optimizelyClient != null
            }

        /**
         * Add a single use callback for Optimizely Client startup. Your listener will be automatically
         * removed after it is invoked. To remove it earlier, simply null our your reference
         * @param startListener
         */
        fun getOptimizelyClient(startListener: OptimizelyClientListener) {
            if (mOptimizelyClient != null) {
                startListener.onOptimizelyClientAvailable(mOptimizelyClient)
            } else {
                mStartListeners.add(startListener)
            }
        }
    }
}
