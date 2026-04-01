package com.mparticle.kits

import android.app.Application
import android.content.Context
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveConfiguration
import apptentive.com.android.feedback.RegisterResult
import apptentive.com.android.util.InternalUseOnly
import apptentive.com.android.util.LogLevel
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

@OptIn(InternalUseOnly::class)
class ApptentiveKit :
    KitIntegration(),
    KitIntegration.EventListener,
    IdentityListener,
    UserAttributeListener {
    private var enableTypeDetection = true
    private var lastKnownFirstName: String? = null
    private var lastKnownLastName: String? = null

    //region KitIntegration
    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val apptentiveAppKey = settings[APPTENTIVE_APP_KEY]
        val apptentiveAppSignature = settings[APPTENTIVE_APP_SIGNATURE]
        require(!KitUtils.isEmpty(apptentiveAppKey)) { KEY_REQUIRED }
        require(!KitUtils.isEmpty(apptentiveAppSignature)) { SIGNATURE_REQUIRED }
        enableTypeDetection = StringUtils.tryParseSettingFlag(settings, ENABLE_TYPE_DETECTION, true)

        if (apptentiveAppKey != null && apptentiveAppSignature != null) {
            val configuration = ApptentiveConfiguration(apptentiveAppKey, apptentiveAppSignature)
            configuration.logLevel = getApptentiveLogLevel()
            configuration.distributionVersion = com.mparticle.BuildConfig.VERSION_NAME
            configuration.distributionName = "mParticle"
            configuration.shouldSanitizeLogMessages =
                StringUtils.tryParseSettingFlag(settings, SHOULD_SANITIZE_LOG_MESSAGES, true)
            configuration.shouldEncryptStorage =
                StringUtils.tryParseSettingFlag(settings, SHOULD_ENCRYPT_STORAGE, false)
            configuration.shouldInheritAppTheme =
                StringUtils.tryParseSettingFlag(settings, SHOULD_INHERIT_APP_THEME, true)
            configuration.customAppStoreURL = settings[CUSTOM_APP_STORE_URL]
            configuration.ratingInteractionThrottleLength =
                StringUtils.tryParseLongSettingFlag(
                    settings,
                    RATING_INTERACTION_THROTTLE_LENGTH,
                    TimeUnit.DAYS.toMillis(7),
                )
            Apptentive.register(
                context.applicationContext as Application,
                configuration,
            ) { registerResult ->
                if (registerResult is RegisterResult.Success) {
                    Apptentive.setMParticleId(currentUser?.id.toString())
                }
            }
        }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun supportsAttributeLists(): Boolean = false

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?,
    ) {
        // Ignored
    }

    //endregion

    //region UserAttributeListener
    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?,
    ) {
        // Ignored
    }

    override fun onRemoveUserAttribute(
        key: String?,
        user: FilteredMParticleUser?,
    ) {
        key?.let {
            Apptentive.removeCustomPersonData(it)
        }
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key != null && value != null) {
            when (key.lowercase()) {
                MParticle.UserAttributes.FIRSTNAME.lowercase() -> {
                    lastKnownFirstName = value.toString()
                }
                MParticle.UserAttributes.LASTNAME.lowercase() -> {
                    lastKnownLastName = value.toString()
                }
                else -> {
                    addCustomPersonData(key, value.toString())
                    return
                }
            }
            val fullName =
                listOfNotNull(lastKnownFirstName, lastKnownLastName)
                    .joinToString(separator = " ")
                    .trim()
            if (fullName.isNotBlank()) {
                Logger.debug("Setting user name $fullName")
                Apptentive.setPersonName(fullName)
            }
        }
    }

    override fun onSetUserTag(
        key: String?,
        user: FilteredMParticleUser?,
    ) {
        // Ignored
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
        user: FilteredMParticleUser?,
    ) {
        // Ignored
    }

    override fun onSetAllUserAttributes(
        userAttributes: MutableMap<String, String>?,
        userAttributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?,
    ) {
        userAttributes?.let { userAttribute ->
            val firstName = userAttribute[MParticle.UserAttributes.FIRSTNAME] ?: ""
            val lastName = userAttribute[MParticle.UserAttributes.LASTNAME] ?: ""
            val fullName =
                listOfNotNull(firstName, lastName)
                    .joinToString(separator = " ")
                    .trim()
            if (fullName.isNotBlank()) {
                Logger.debug("Setting user name $fullName")
                Apptentive.setPersonName(fullName)
            }
            userAttribute
                .filterKeys { key ->
                    key != MParticle.UserAttributes.FIRSTNAME && key != MParticle.UserAttributes.LASTNAME
                }.map {
                    addCustomPersonData(it.key, it.value)
                }
        }
    }

    //endregion

    //region EventListener
    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        engage(event.eventName, event.customAttributeStrings)
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(ReportingMessage.fromEvent(this, event))
        return messageList
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        engage(screenName, eventAttributes)
        val messages = LinkedList<ReportingMessage>()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                eventAttributes,
            ),
        )
        return messages
    } //endregion

    //region IdentityListener
    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?,
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?,
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?,
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?,
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onUserIdentified(mParticleUser: MParticleUser?) {
        // Ignored
    }

    //endregion

    //region Helpers
    private fun getApptentiveLogLevel(): LogLevel =
        when (Logger.getMinLogLevel()) {
            MParticle.LogLevel.VERBOSE -> LogLevel.Verbose
            MParticle.LogLevel.DEBUG -> LogLevel.Debug
            MParticle.LogLevel.INFO -> LogLevel.Info
            MParticle.LogLevel.WARNING -> LogLevel.Warning
            MParticle.LogLevel.ERROR -> LogLevel.Error
            MParticle.LogLevel.NONE -> LogLevel.Info
            null -> LogLevel.Info
        }

    private fun setUserIdentity(user: MParticleUser?) {
        user?.userIdentities?.entries?.let {
            for (i in it.indices) {
                val entry = it.elementAt(i)
                when (entry.key) {
                    IdentityType.CustomerId -> {
                        if (KitUtils.isEmpty(Apptentive.getPersonName())) {
                            // Use id as customer name if no full name is set yet.
                            Logger.debug("Setting customer id as user name ${entry.value}")
                            Apptentive.setPersonName(entry.value)
                        }
                    }
                    IdentityType.Email -> {
                        Logger.debug("Setting customer email ${entry.value}")
                        Apptentive.setPersonEmail(entry.value)
                    }
                    else -> Logger.debug("Other identity type")
                }
            }
        }
    }

    private fun engage(
        event: String,
        customData: Map<String, String>?,
    ) {
        Apptentive.engage(event, parseCustomData(customData))
    }

    // Apptentive SDK does not provide a function which accepts Object as custom data so we need to cast
    private fun addCustomPersonData(
        key: String,
        value: String,
    ) {
        // original key
        Logger.debug("Adding custom person data $key to $value")
        Apptentive.addCustomPersonData(key, value)

        // typed key
        if (enableTypeDetection) {
            when (val typedValue = CustomDataParser.parseValue(value)) {
                is String -> {
                    // the value is already set
                }
                is Boolean -> {
                    Apptentive.addCustomPersonData(key + SUFFIX_KEY_FLAG, typedValue)
                }
                is Number -> {
                    Apptentive.addCustomPersonData(key + SUFFIX_KEY_NUMBER, typedValue)
                }
                else -> {
                    Logger.error(
                        "Unexpected custom person data type:${typedValue?.javaClass}",
                    )
                }
            }
        }
    }

    private fun parseCustomData(map: Map<String, String>?): Map<String, Any>? {
        if (map != null) {
            val res: MutableMap<String, Any> = HashMap()
            for ((key, value) in map) {
                // original key
                res[key] = value

                // typed key
                if (enableTypeDetection) {
                    when (val typedValue = CustomDataParser.parseValue(value)) {
                        is String -> {
                            // the value is already set
                        }
                        is Boolean -> {
                            res[key + SUFFIX_KEY_FLAG] = typedValue
                        }
                        is Number -> {
                            res[key + SUFFIX_KEY_NUMBER] = typedValue
                        }
                        else -> {
                            Logger.error(
                                "Unexpected custom data type:${typedValue?.javaClass}",
                            )
                        }
                    }
                }
            }
            return res
        }
        return null
    }
    //endregion

    companion object {
        private const val APPTENTIVE_APP_KEY = "apptentiveAppKey"
        private const val APPTENTIVE_APP_SIGNATURE = "apptentiveAppSignature"
        private const val ENABLE_TYPE_DETECTION = "enableTypeDetection"
        private const val SHOULD_INHERIT_APP_THEME = "shouldInheritAppTheme"
        private const val SHOULD_SANITIZE_LOG_MESSAGES = "shouldSanitizeLogMessages"
        private const val SHOULD_ENCRYPT_STORAGE = "shouldEncryptStorage"
        private const val RATING_INTERACTION_THROTTLE_LENGTH = "ratingInteractionThrottleLength"
        private const val CUSTOM_APP_STORE_URL = "customAppStoreURL"
        private const val SUFFIX_KEY_FLAG = "_flag"
        private const val SUFFIX_KEY_NUMBER = "_number"
        private const val KEY_REQUIRED =
            "Apptentive App Key is required. If you are migrating from a previous version, you may need to enter the new Apptentive App Key and Signature on the mParticle website."
        private const val SIGNATURE_REQUIRED =
            "Apptentive App Signature is required. If you are migrating from a previous version, you may need to enter the new Apptentive App Key and Signature on the mParticle website."
        const val NAME = "Apptentive"
    }
}
