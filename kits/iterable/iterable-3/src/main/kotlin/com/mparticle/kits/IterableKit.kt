package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.iterable.iterableapi.BuildConfig
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableConfigHelper
import com.iterable.iterableapi.IterableConstants
import com.iterable.iterableapi.IterableFirebaseMessagingService
import com.iterable.iterableapi.IterableHelper.IterableActionHandler
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.ActivityListener
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.iterable.Future

class IterableKit :
    KitIntegration(),
    ActivityListener,
    ApplicationStateListener,
    IdentityListener,
    PushListener {
    private val previousLinks: MutableSet<String> = HashSet()
    private var mpidEnabled = false

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        checkForAttribution()
        val userIdField = settings[SETTING_USER_ID_FIELD]
        mpidEnabled = userIdField != null && userIdField == IDENTITY_MPID
        val configBuilder =
            IterableConfigHelper.createConfigBuilderFromIterableConfig(
                customConfig,
            )
        settings[SETTING_GCM_INTEGRATION_NAME]?.let { configBuilder.setPushIntegrationName(it) }
        settings[SETTING_API_KEY]?.let {
            IterableApi.initialize(context, it, configBuilder.build())
        }
        initIntegrationAttributes()
        return emptyList()
    }

    override fun getName(): String = NAME

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    private fun initIntegrationAttributes() {
        val integrationAttributes = HashMap<String, String>()
        integrationAttributes[INTEGRATION_ATTRIBUTE_SDK_VERSION] =
            BuildConfig.ITERABLE_SDK_VERSION
        setIntegrationAttributes(integrationAttributes)
    }

    private fun checkForAttribution() {
        val activityRef = kitManager.currentActivity
        if (activityRef != null) {
            val activity = activityRef.get()
            if (activity != null) {
                val currentLink = activity.intent.dataString
                if (!currentLink.isNullOrEmpty() && !previousLinks.contains(currentLink)
                ) {
                    previousLinks.add(currentLink)
                    val clickCallback =
                        IterableActionHandler { result ->
                            if (!KitUtils.isEmpty(result)) {
                                val attributionResult = AttributionResult().setLink(result)
                                attributionResult.serviceProviderId = configuration.kitId
                                kitManager.onResult(attributionResult)
                            }
                        }
                    IterableApi.getInstance().getAndTrackDeepLink(currentLink, clickCallback)
                }
            }
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        bundle: Bundle?,
    ): List<ReportingMessage> = emptyList()

    override fun onActivityStarted(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityResumed(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityPaused(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityStopped(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivitySaveInstanceState(
        activity: Activity,
        bundle: Bundle?,
    ): List<ReportingMessage> = emptyList()

    override fun onActivityDestroyed(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onApplicationForeground() {
        checkForAttribution()
    }

    override fun onApplicationBackground() {}

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateIdentity(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateIdentity(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateIdentity(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        updateIdentity(mParticleUser)
    }

    private fun isEmpty(string: String?): Boolean = string == null || "" == string

    private fun getUserId(mParticleUser: MParticleUser): Future<String?> =
        Future.runAsync {
            var id: String? = null
            if (mpidEnabled) {
                if (mParticleUser.id != 0L) {
                    id = mParticleUser.id.toString()
                }
            } else {
                id = IterableDeviceIdHelper.getGoogleAdId(context)
                if (isEmpty(id)) {
                    id = KitUtils.getAndroidID(context)
                }
                if (isEmpty(id)) {
                    val userIdentities = mParticleUser.userIdentities
                    id = userIdentities[IdentityType.CustomerId]
                }
                if (isEmpty(id)) {
                    id = MParticle.getInstance()?.Identity()?.deviceApplicationStamp
                }
            }
            id
        }

    private fun String?.getPlaceholderEmail(): String? = this?.let { "$it@placeholder.email" }

    private fun handleOnSuccess(
        userId: String?,
        mParticleUser: MParticleUser,
    ) {
        if (prefersUserId) {
            IterableApi.getInstance().setUserId(userId)
        } else {
            val userIdentities = mParticleUser.userIdentities
            val mpEmail = userIdentities[IdentityType.Email]
            val placeholderEmail = userId.getPlaceholderEmail()

            val email =
                if (!mpEmail.isNullOrEmpty()) {
                    mpEmail
                } else if (!isEmpty(placeholderEmail)) {
                    placeholderEmail
                } else {
                    null
                }

            IterableApi.getInstance().setEmail(email)
        }
    }

    private fun updateIdentity(mParticleUser: MParticleUser) {
        val userId = getUserId(mParticleUser)

        userId
            .onSuccess(
                object : Future.SuccessCallback<String?> {
                    override fun onSuccess(userId: String?) {
                        handleOnSuccess(userId, mParticleUser)
                        return
                    }
                },
            ).onFailure(
                object : Future.FailureCallback {
                    override fun onFailure(throwable: Throwable?) {
                        Log.e(ITERABLE_KIT_ERROR_TAG, ITERABLE_KIT_ERROR_MESSAGE)
                    }
                },
            )
    }

    override fun willHandlePushMessage(intent: Intent): Boolean {
        val extras = intent.extras
        return extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)
    }

    override fun onPushMessageReceived(
        context: Context,
        intent: Intent,
    ) {
        IterableFirebaseMessagingService.handleMessageReceived(
            context,
            RemoteMessage(intent.extras),
        )
    }

    override fun onPushRegistration(
        instanceId: String,
        senderId: String,
    ): Boolean {
        IterableApi.getInstance().registerForPush()
        return true
    }

    companion object {
        var prefersUserId = false
        private var customConfig: IterableConfig? = null
        private const val SETTING_API_KEY = "apiKey"
        private const val SETTING_GCM_INTEGRATION_NAME = "gcmIntegrationName"
        private const val SETTING_USER_ID_FIELD = "userIdField"
        private const val IDENTITY_CUSTOMER_ID = "customerId"
        private const val IDENTITY_MPID = "mpid"
        private const val INTEGRATION_ATTRIBUTE_KIT_VERSION_CODE = "Iterable.kitVersionCode"
        private const val INTEGRATION_ATTRIBUTE_SDK_VERSION = "Iterable.sdkVersion"
        private const val NAME = "Iterable"
        private const val ITERABLE_KIT_ERROR_TAG = "IterableKit"
        private const val ITERABLE_KIT_ERROR_MESSAGE = "Error while getting the placeholder email"

        /**
         * Set a custom config to be used when initializing Iterable SDK
         * @param config `IterableConfig` instance with configuration data for Iterable SDK
         */
        fun setCustomConfig(config: IterableConfig?) {
            customConfig = config
        }
    }
}
