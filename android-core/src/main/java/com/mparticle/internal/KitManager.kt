package com.mparticle.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import com.mparticle.AttributionResult
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.consent.ConsentState
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import org.json.JSONArray
import java.lang.ref.WeakReference

interface Events {
    fun logEvent(event: BaseEvent)
    fun logScreen(screenEvent: MPEvent)
    fun logBatch(jsonObject: String)
    fun leaveBreadcrumb(breadcrumb: String)
    fun logError(message: String, eventData: Map<String, String?>?)
    fun logException(exception: Exception, eventData: Map<String, String?>?, message: String?)
    fun logNetworkPerformance(
        url: String, startTime: Long, method: String, length: Long,
        bytesSent: Long, bytesReceived: Long, requestString: String?,
        responseCode: Int
    )
}

interface ActivityLifecycleCallbacks {
    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?)
    fun onActivityStarted(activity: Activity)
    fun onActivityResumed(activity: Activity)
    fun onActivityPaused(activity: Activity)
    fun onActivityStopped(activity: Activity)
    fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?)
    fun onActivityDestroyed(activity: Activity)
}

interface Session {
    fun onSessionEnd()
    fun onSessionStart()
    fun logout()
}

interface Identity {
    fun onIdentifyCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onLoginCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onLogoutCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onModifyCompleted(user: MParticleUser?, request: IdentityApiRequest)
}

interface ApplicationState {
    fun onApplicationForeground()
    fun onApplicationBackground()
}

interface Push {
    fun onMessageReceived(context: Context, intent: Intent): Boolean
    fun onPushRegistration(instanceId: String?, senderId: String?): Boolean
}

interface UserAttributes {
    fun setUserAttribute(key: String, value: String?, mpid: Long)
    fun setUserAttributeList(key: String, value: List<String?>?, mpid: Long)
    fun removeUserAttribute(key: String, mpid: Long)
    fun setUserTag(tag: String, mpid: Long)
    fun incrementUserAttribute(key: String, incrementValue: Number, newValue: String?, mpid: Long)
    fun onConsentStateUpdated(oldState: ConsentState?, newState: ConsentState?, mpid: Long)
    fun setUserIdentity(id: String, identityType: IdentityType)
    fun removeUserIdentity(id: IdentityType)
}

interface KitIntegrationOperations {
    val currentActivity: WeakReference<Activity>?

    fun setLocation(location: Location?)
    fun setOptOut(optOutStatus: Boolean)
    fun getSurveyUrl(
        serviceProviderId: Int, userAttributes: Map<String, String?>?,
        userAttributeLists: Map<String, List<String?>?>?
    ): Uri?

    fun reset()
}

interface KitManager : Events, ActivityLifecycleCallbacks, Session, Identity, ApplicationState,
    Push, UserAttributes, KitIntegrationOperations {

    val supportedKits: Set<Int>
    val kitStatus: Map<Int, KitStatus>
    val attributionResults: Map<Int, AttributionResult>

    fun isKitActive(kitId: Int): Boolean
    fun getKitInstance(kitId: Int): Any?
    fun updateKits(jsonArray: JSONArray?): KitsLoadedCallback
    fun updateDataplan(dataplanOptions: DataplanOptions?)
    fun installReferrerUpdated()

    enum class KitStatus {
        NOT_CONFIGURED, STOPPED, ACTIVE
    }
}