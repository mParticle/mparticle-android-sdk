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
import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import java.lang.Exception
import java.lang.ref.WeakReference

interface KitManager : AndroidLifecycleCallbacks {
    val currentActivity: WeakReference<Activity>?

    fun incrementUserAttribute(key: String?, incrementValue: Number?, newValue: String?, mpid: Long)
    fun setUserAttribute(key: String?, value: String?, mpid: Long)
    fun setUserAttributeList(key: String?, value: List<String?>?, mpid: Long)
    fun removeUserAttribute(key: String?, mpid: Long)

    fun logBatch(jsonObject: String?)
    fun logout()

    fun setUserTag(tag: String?, mpid: Long)
    fun onConsentStateUpdated(oldState: ConsentState?, newState: ConsentState?, mpid: Long)
    fun setUserIdentity(id: String?, identityType: IdentityType?)
    fun removeUserIdentity(id: IdentityType?)

    fun onPushRegistration(instanceId: String?, senderId: String?): Boolean

    fun onMessageReceived(context: Context?, intent: Intent?): Boolean

    val supportedKits: Set<Int?>?
    fun updateKits(jsonArray: JSONArray?): KitsLoadedCallback?
    fun updateDataplan(dataplanOptions: DataplanOptions?)
    val kitStatus: Map<Int, KitStatus>

    fun onSessionEnd()
    fun onSessionStart()

    fun onApplicationForeground()
    fun onApplicationBackground()
    fun onIdentifyCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onLoginCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onLogoutCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest)
    fun reset()

    fun logNetworkPerformance(
        url: String,
        startTime: Long,
        method: String,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        @Nullable requestString: String?,
        responseCode: Int
    )

    fun logEvent(event: BaseEvent)
    fun logScreen(screenEvent: MPEvent?)
    fun leaveBreadcrumb(breadcrumb: String)
    fun logError(message: String, @Nullable eventData: Map<String, String>?)
    fun logException(
        exception: Exception,
        @Nullable eventData: Map<String, String>?,
        @Nullable message: String?
    )

    fun setLocation(@Nullable location: Location?)
    fun setOptOut(optOutStatus: Boolean)
    fun getSurveyUrl(
        serviceProviderId: Int,
        @Nullable userAttributes: Map<String, String>?,
        @Nullable userAttributeLists: Map<String, List<String>>?
    ): Uri?

    fun isKitActive(kitId: Int): Boolean
    fun getKitInstance(kitId: Int): Any?
    fun installReferrerUpdated()
    val attributionResults: Map<Int, AttributionResult?> // Check Value nullable

    enum class KitStatus {
        NOT_CONFIGURED, STOPPED, ACTIVE
    }
}

interface AndroidLifecycleCallbacks {
    fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?)
    fun onActivityStarted(activity: Activity?)
    fun onActivityResumed(activity: Activity?)
    fun onActivityPaused(activity: Activity?)
    fun onActivityStopped(activity: Activity?)
    fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?)
    fun onActivityDestroyed(activity: Activity?)
}
