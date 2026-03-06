package com.mparticle.kits

import android.content.Context
import android.content.Intent
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.EventListener
import com.mparticle.kits.KitIntegration.IdentityListener
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchReferralInitListener
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.util.BranchEvent
import org.json.JSONObject
import java.math.BigDecimal
import java.util.HashMap
import java.util.LinkedList

/**
 *
 *
 * Embedded implementation of the Branch Metrics SDK
 *
 *
 */
class BranchMetricsKit :
    KitIntegration(),
    EventListener,
    CommerceListener,
    AttributeListener,
    ApplicationStateListener,
    IdentityListener,
    BranchReferralInitListener {
    private val branchAppKey = "branchKey"
    private var isMpidIdentityType = false
    var identityType: IdentityType? = IdentityType.CustomerId
    private var mSendScreenEvents = false
    private var branchUtil: BranchUtil? = null

    override fun getInstance(): Branch? = branch

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        branchUtil = BranchUtil()
        Branch.registerPlugin(
            "mParticle",
            javaClass.getPackage()?.specificationVersion ?: "0",
        )
        getSettings()[branchAppKey]
            ?.let { Branch.getAutoInstance(getContext().applicationContext, it) }
        Branch.sessionBuilder(null).withCallback(this).init()
        if (Logger.getMinLogLevel() != MParticle.LogLevel.NONE) {
            Branch.enableLogging()
        }
        val sendScreenEvents = settings[FORWARD_SCREEN_VIEWS]
        mSendScreenEvents =
            sendScreenEvents != null &&
            sendScreenEvents.equals("true", ignoreCase = true)
        setIdentityType(settings)
        return emptyList()
    }

    fun setIdentityType(settings: Map<String, String>) {
        val userIdentificationType = settings[USER_IDENTIFICATION_TYPE]

        userIdentificationType?.let {
            if (!KitUtils.isEmpty(it)) {
                when (it) {
                    "MPID" -> isMpidIdentityType = true
                    "Email" -> identityType = null
                    else -> identityType = IdentityType.valueOf(it)
                }
            }
        }
    }

    override fun setOptOut(b: Boolean): List<ReportingMessage> {
        branch?.disableTracking(b)
        val messages = LinkedList<ReportingMessage>()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messages
    }

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

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        branchUtil?.createBranchEventFromMPEvent(event)?.logEvent(context)
        val messages = LinkedList<ReportingMessage>()
        messages.add(ReportingMessage.fromEvent(this, event))
        return messages
    }

    override fun logLtvIncrease(
        bigDecimal: BigDecimal,
        bigDecimal1: BigDecimal,
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        branchUtil?.createBranchEventFromMPCommerceEvent(commerceEvent)?.logEvent(context)
        val messages: MutableList<ReportingMessage> = LinkedList()
        messages.add(ReportingMessage.fromEvent(this, commerceEvent))
        return messages
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        var eventAttributes: Map<String, String>? = eventAttributes
        return if (mSendScreenEvents) {
            val logScreenEvent = BranchEvent(screenName)
            if (eventAttributes == null) {
                eventAttributes = HashMap()
            }
            branchUtil?.updateBranchEventWithCustomData(logScreenEvent, eventAttributes)
            logScreenEvent.logEvent(context)
            val messages =
                LinkedList<ReportingMessage>()
            messages.add(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.SCREEN_VIEW,
                    System.currentTimeMillis(),
                    eventAttributes,
                ),
            )
            messages
        } else {
            emptyList()
        }
    }

    private val branch: Branch?
        get() = settings[branchAppKey]?.let { Branch.getInstance() }

    override fun setInstallReferrer(intent: Intent) {
        BranchLogger.w(
            "setInstallReferrer(intent) was ignored, INSTALL_REFERRER broadcast intent is deprecated, relevant data is now collected automatically using the Play Install Referrer Library bundled together with Branch SDK.",
        )
    }

    override fun setUserAttribute(
        s: String,
        s1: String,
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

    override fun removeUserAttribute(s: String) {}

    override fun setUserIdentity(
        identityType: IdentityType,
        s: String,
    ) {}

    override fun removeUserIdentity(identityType: IdentityType) {}

    override fun logout(): List<ReportingMessage> {
        branch?.logout()
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(ReportingMessage.logoutMessage(this))
        return messageList
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser)
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    private fun updateUser(mParticleUser: MParticleUser) {
        var identity: String? = null
        if (isMpidIdentityType) {
            identity = mParticleUser.id.toString()
        } else if (identityType != null) {
            val mPIdentity = mParticleUser.userIdentities[identityType]
            if (mPIdentity != null) {
                identity = mPIdentity
            }
        }
        if (identity != null) {
            branch?.setIdentity(identity)
        }
    }

    /**
     * Don't do anything here - we make the call to get the latest deep link info during onResume, below.
     */
    override fun onInitFinished(
        jsonResult: JSONObject?,
        branchError: BranchError?,
    ) {
        if (jsonResult != null && jsonResult.length() > 0) {
            val result =
                AttributionResult()
                    .setParameters(jsonResult)
                    .setServiceProviderId(this.configuration.kitId)
            kitManager.onResult(result)
        }
        if (branchError != null) {
            if (branchError.errorCode != BranchError.ERR_BRANCH_ALREADY_INITIALIZED) {
                val error =
                    AttributionError()
                        .setMessage(branchError.toString())
                        .setServiceProviderId(this.configuration.kitId)
                kitManager.onError(error)
            }
        }
    }

    override fun onApplicationForeground() {
        Branch.sessionBuilder(null).withCallback(this).init()
    }

    override fun onApplicationBackground() {}

    companion object {
        private const val FORWARD_SCREEN_VIEWS = "forwardScreenViews"
        private const val USER_IDENTIFICATION_TYPE = "userIdentificationType"
        const val NAME = "Branch Metrics"
    }
}
