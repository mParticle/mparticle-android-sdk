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
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.WrapperSdkVersion
import com.mparticle.consent.ConsentState
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.rokt.RoktOptions
import org.json.JSONArray
import java.lang.ref.WeakReference

interface KitManager {

    val currentActivity: WeakReference<Activity>

    fun logEvent(event: BaseEvent?)

    fun logScreen(screenEvent: MPEvent?)

    fun logBatch(jsonObject: String?)

    fun leaveBreadcrumb(breadcrumb: String?)

    fun logError(message: String?, eventData: Map<String, String>?)

    fun logNetworkPerformance(
        url: String?,
        startTime: Long,
        method: String?,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
        responseCode: Int,
    )

    fun logException(exception: Exception?, eventData: Map<String, String>?, message: String?)

    fun setLocation(location: Location?)

    fun logout()

    fun setUserAttribute(key: String?, value: String?, mpid: Long)

    fun setUserAttributeList(key: String?, value: List<String>?, mpid: Long)

    fun removeUserAttribute(key: String?, mpid: Long)

    fun setUserTag(tag: String?, mpid: Long)

    fun incrementUserAttribute(key: String?, incrementValue: Number?, newValue: String?, mpid: Long)

    fun onConsentStateUpdated(oldState: ConsentState?, newState: ConsentState?, mpid: Long)

    fun setUserIdentity(id: String?, identityType: MParticle.IdentityType?)

    fun removeUserIdentity(id: MParticle.IdentityType?)

    fun setOptOut(optOutStatus: Boolean)

    fun getSurveyUrl(serviceProviderId: Int, userAttributes: Map<String, String>?, userAttributeLists: MutableMap<String, List<String>>?): Uri

    fun onMessageReceived(context: Context?, intent: Intent?): Boolean

    fun onPushRegistration(instanceId: String?, senderId: String?): Boolean

    val isEnabled: Boolean

    fun isKitActive(kitId: Int): Boolean

    fun getKitInstance(kitId: Int): Any

    val supportedKits: Set<Int>

    fun updateKits(jsonArray: JSONArray?): KitsLoadedCallback

    fun updateDataplan(dataplanOptions: MParticleOptions.DataplanOptions?)

    val roktOptions: RoktOptions

    val kitStatus: Map<Int, KitStatus>

    fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?)

    fun onActivityStarted(activity: Activity?)

    fun onActivityResumed(activity: Activity?)

    fun onActivityPaused(activity: Activity?)

    fun onActivityStopped(activity: Activity?)

    fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?)

    fun onActivityDestroyed(activity: Activity?)

    fun onSessionEnd()

    fun onSessionStart()

    fun installReferrerUpdated()

    fun onApplicationForeground()

    fun onApplicationBackground()

    val attributionResults: Map<Int, AttributionResult>

    fun onIdentifyCompleted(user: MParticleUser?, request: IdentityApiRequest?)

    fun onLoginCompleted(user: MParticleUser?, request: IdentityApiRequest?)

    fun onLogoutCompleted(user: MParticleUser?, request: IdentityApiRequest?)

    fun onModifyCompleted(user: MParticleUser?, request: IdentityApiRequest?)

    fun reset()

    fun setWrapperSdkVersion(wrapperSdkVersion: WrapperSdkVersion?)

    enum class KitStatus {
        NOT_CONFIGURED,
        STOPPED,
        ACTIVE,
    }
}
