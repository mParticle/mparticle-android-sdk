package com.mparticle.kits

import android.content.Context
import android.text.TextUtils
import com.apptimize.Apptimize
import com.apptimize.Apptimize.IsFirstTestRun
import com.apptimize.Apptimize.OnTestRunListener
import com.apptimize.ApptimizeOptions
import com.apptimize.ApptimizeTestInfo
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.BaseAttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.LogoutListener
import java.math.BigDecimal

class ApptimizeKit :
    KitIntegration(),
    BaseAttributeListener,
    AttributeListener,
    LogoutListener,
    KitIntegration.EventListener,
    CommerceListener,
    OnTestRunListener {
    private fun toMessageList(message: ReportingMessage): List<ReportingMessage> = listOf(message)

    private fun createReportingMessage(messageType: String): ReportingMessage =
        ReportingMessage(
            this,
            messageType,
            System.currentTimeMillis(),
            null,
        )

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val appKey = getSettings()[APP_MP_KEY]
        require(!TextUtils.isEmpty(appKey)) { APP_MP_KEY }
        val options = buildApptimizeOptions(settings)
        Apptimize.setup(context, appKey, options)
        if (java.lang.Boolean.parseBoolean(settings[TRACK_EXPERIMENTS])) {
            Apptimize.setOnTestRunListener(this)
        }
        return emptyList()
    }

    private fun buildApptimizeOptions(settings: Map<String, String>): ApptimizeOptions {
        val o = ApptimizeOptions()
        o.isThirdPartyEventImportingEnabled = false
        o.isThirdPartyEventExportingEnabled = false
        configureApptimizeUpdateMetaDataTimeout(o, settings)
        configureApptimizeDeviceName(o, settings)
        configureApptimizeDeveloperModeDisabled(o, settings)
        configureApptimizeExplicitEnablingRequired(o, settings)
        configureApptimizeMultiprocessModeEnabled(o, settings)
        configureApptimizeLogLevel(o, settings)
        return o
    }

    private fun configureApptimizeUpdateMetaDataTimeout(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        try {
            settings[UPDATE_METDATA_TIMEOUT_MP_KEY]?.let {
                val l = it.toLong()
                o.setUpdateMetadataTimeout(l)
            }
        } catch (nfe: NumberFormatException) {
        }
    }

    private fun configureApptimizeDeviceName(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        val v = settings[DEVICE_NAME_MP_KEY]
        o.deviceName = v
    }

    private fun configureApptimizeDeveloperModeDisabled(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        val b = settings[DEVELOPER_MODE_DISABLED_MP_KEY]
        o.isDeveloperModeDisabled = b.toBoolean()
    }

    private fun configureApptimizeExplicitEnablingRequired(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        val b = settings[EXPLICIT_ENABLING_REQUIRED_MP_KEY]
        o.isExplicitEnablingRequired = b.toBoolean()
    }

    private fun configureApptimizeMultiprocessModeEnabled(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        val b = settings[MULTIPROCESS_MODE_ENABLED_MP_KEY]
        o.setMultiprocessMode(b.toBoolean())
    }

    private fun configureApptimizeLogLevel(
        o: ApptimizeOptions,
        settings: Map<String, String>,
    ) {
        try {
            val l =
                settings[LOG_LEVEL_MP_KEY]
                    ?.let { ApptimizeOptions.LogLevel.valueOf(it) }
                    ?.let { o.logLevel = it }
        } catch (iae: IllegalArgumentException) {
        } catch (npe: NullPointerException) {
        }
    }

    override fun getName(): String = KIT_NAME

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
        user: FilteredMParticleUser?,
    ) {
        // not supported by the Apptimize kit
    }

    override fun supportsAttributeLists(): Boolean = false

    /**
     * [userAttributeLists] is ignored by the Apptimize kit.
     */
    override fun onSetAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, List<String>>,
        user: FilteredMParticleUser,
    ) {
        for ((key, value) in userAttributes) {
            Apptimize.setUserAttribute(key, value)
        }
    }

    override fun onRemoveUserAttribute(
        key: String,
        user: FilteredMParticleUser,
    ) {
        Apptimize.clearUserAttribute(key)
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null || value == null || value !is String) {
            return
        }
        Apptimize.setUserAttribute(key, value)
    }

    /**
     * @param identityType only Alias and CustomerId are suppoted by the Apptimize kit.
     */
    override fun setUserIdentity(
        identityType: IdentityType,
        id: String?,
    ) {
        when (identityType) {
            IdentityType.Alias, IdentityType.CustomerId -> {
                Apptimize.setPilotTargetingId(id)
            }
            else -> {}
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        setUserIdentity(identityType, null)
    }

    override fun logout(): List<ReportingMessage> {
        Apptimize.track(LOGOUT_TAG)
        return toMessageList(ReportingMessage.logoutMessage(this))
    }

    /**
     * Not supported by the Apptimize kit.
     */
    override fun leaveBreadcrumb(s: String): List<ReportingMessage> = emptyList()

    /**
     * Not supported by the Apptimize kit.
     */
    override fun logError(
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    /**
     * Not supported by the Apptimize kit.
     */
    override fun logException(
        e: Exception,
        map: Map<String, String>,
        s: String,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage> {
        Apptimize.track(mpEvent.eventName)
        return toMessageList(ReportingMessage.fromEvent(this, mpEvent))
    }

    /**
     * @param eventAttributes is ignored by the Apptimize kit.
     */
    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        val event = String.format(VIEWED_EVENT_FORMAT, screenName)
        Apptimize.track(event)
        return toMessageList(
            createReportingMessage(ReportingMessage.MessageType.SCREEN_VIEW).setScreenName(
                screenName,
            ),
        )
    }

    /**
     * @param valueTotal is ignored by the Apptimize kit.
     * @param contextInfo is ignored by the Apptimize kit.
     */
    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> {
        // match the iOS style, where only the delta is sent rather than an absolute final value.
        Apptimize.track(LTV_TAG, valueIncreased.toDouble())
        return toMessageList(createReportingMessage(ReportingMessage.MessageType.COMMERCE_EVENT))
    }

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage>? {
        val customEvents = CommerceEventUtils.expand(commerceEvent)
        if (customEvents.size == 0) {
            return null
        }
        for (event in customEvents) {
            Apptimize.track(event.eventName)
        }
        return toMessageList(ReportingMessage.fromEvent(this, commerceEvent))
    }

    /**
     * After opting out, it is not possible to opt back in via the Apptimize kit.
     * @param optedOut only a value of 'true' supported by the Apptimize kit.
     */
    override fun setOptOut(optedOut: Boolean): List<ReportingMessage>? {
        var ret: List<ReportingMessage>? = null
        if (optedOut) {
            Apptimize.disable()
            ret =
                toMessageList(
                    createReportingMessage(ReportingMessage.MessageType.OPT_OUT).setOptOut(optedOut),
                )
        }
        return ret
    }

    override fun onTestRun(
        apptimizeTestInfo: ApptimizeTestInfo,
        isFirstTestRun: IsFirstTestRun,
    ) {
        if (isFirstTestRun != IsFirstTestRun.YES) {
            return
        }
        val testInfoMap = Apptimize.getTestInfo()
        val participatedExperiments: MutableList<String> = ArrayList()
        if (testInfoMap == null) {
            return
        }
        for (key in testInfoMap.keys) {
            val testInfo = testInfoMap[key]
            if (testInfo != null) {
                if (testInfo.userHasParticipated()) {
                    val nameAndVariation = testInfo.testName + "-" + testInfo.enrolledVariantName
                    participatedExperiments.add(nameAndVariation)
                }
            }
        }
        val user = MParticle.getInstance()!!.Identity().currentUser
        user?.setUserAttributeList("Apptimize experiment", participatedExperiments)
        val eventInfo = HashMap<String, String?>(5)
        eventInfo["VariationID"] = apptimizeTestInfo.enrolledVariantId.toString()
        eventInfo["ID"] = apptimizeTestInfo.testId.toString()
        eventInfo["Name"] = apptimizeTestInfo.testName
        eventInfo["Variation"] = apptimizeTestInfo.enrolledVariantName
        eventInfo["Name and Variation"] =
            apptimizeTestInfo.testName + "-" +
            apptimizeTestInfo.enrolledVariantName
        val event =
            MPEvent
                .Builder(
                    "Apptimize experiment",
                    MParticle.EventType
                        .Other,
                ).customAttributes(eventInfo)
                .build()
        MParticle.getInstance()?.logEvent(event)
    }

    companion object {
        private const val APP_MP_KEY = "appKey"
        private const val UPDATE_METDATA_TIMEOUT_MP_KEY = "metadataTimeout"
        private const val DEVICE_NAME_MP_KEY = "deviceName"
        private const val DEVELOPER_MODE_DISABLED_MP_KEY = "developerModeDisabled"
        private const val EXPLICIT_ENABLING_REQUIRED_MP_KEY = "explicitEnablingRequired"
        private const val MULTIPROCESS_MODE_ENABLED_MP_KEY = "multiprocessModeEnabled"
        private const val LOG_LEVEL_MP_KEY = "logLevel"
        private const val LOGOUT_TAG = "logout"
        private const val LTV_TAG = "ltv"
        private const val VIEWED_EVENT_FORMAT = "screenView %s"
        private const val TRACK_EXPERIMENTS = "trackExperiments"
        private const val KIT_NAME = "Apptimize"
    }
}
