package com.mparticle.internal

import android.app.Activity
import android.content.Context
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.consent.ConsentState
import com.mparticle.MParticle.IdentityType
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import com.mparticle.internal.KitsLoadedCallback
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.AttributionResult
import com.mparticle.identity.MParticleUser
import com.mparticle.identity.IdentityApiRequest
import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import java.lang.Exception
import java.lang.ref.WeakReference

interface Events {
    fun logEvent(event: BaseEvent)
    fun logScreen(screenEvent: MPEvent) //The queue which uses this accepts null
    fun logBatch(jsonObject: String?) // Should be null?
    fun leaveBreadcrumb(breadcrumb: String)
    fun logError(
        message: String,
        @Nullable eventData: Map<String?, String?>?
    ) //Key and value nullable? - event data must is nullable

    fun logException(
        exception: Exception,
        @Nullable eventData: Map<String?, String?>?,
        @Nullable message: String?
    ) // event data key, values nullable?
}

interface ActivityLifecycleCallbacks {
    fun onActivityCreated(activity: Activity, @Nullable savedInstanceState: Bundle?)
    fun onActivityStarted(activity: Activity)
    fun onActivityResumed(activity: Activity)
    fun onActivityPaused(activity: Activity)
    fun onActivityStopped(activity: Activity)
    fun onActivitySaveInstanceState(activity: Activity, @Nullable outState: Bundle?)
    fun onActivityDestroyed(activity: Activity)
}

interface Session {
    fun onSessionEnd()
    fun onSessionStart()
    fun logout()
}

interface Identity {
    fun onIdentifyCompleted(user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onLoginCompleted(user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onLogoutCompleted(user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onModifyCompleted(@Nullable user: MParticleUser?, request: IdentityApiRequest)
}

interface ApplicationState {
    fun onApplicationForeground()
    fun onApplicationBackground()
}

interface Push {
    fun onMessageReceived(
        context: Context?,
        intent: Intent?
    ): Boolean //safe to make intent and context not null, i think yes

    fun onPushRegistration(@Nullable instanceId: String?, @Nullable senderId: String?): Boolean
}

interface UserAttributes {
    fun setUserAttribute(
        key: String?,
        value: String?,
        mpid: Long
    ) //key shouldnt be null, value might be null and can be null?

    fun setUserAttributeList(
        key: String?,
        value: List<String?>?,
        mpid: Long
    ) //key shouldnt be null, value contains null and can be null?

    fun removeUserAttribute(key: String?, mpid: Long) //key shouldnt be null

    fun setUserTag(tag: String?, mpid: Long)//key shouldnt be null

    fun incrementUserAttribute(
        key: String?,
        incrementValue: Number?,
        newValue: String?,
        mpid: Long
    )//key shouldnt be null - what about increment value and newValue?


    fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        mpid: Long
    ) // any of the consent state should be notnull?

    fun setUserIdentity(
        id: String?,
        identityType: IdentityType?
    ) //seems that the id should be notnull, identity type? - MParticleUserDelegate has an if!=null

    fun removeUserIdentity(id: IdentityType?)// identity type shouldnt be null, its the only param - MParticleUserDelegate has an if!=null

}

interface KitIntegrationOperations {
    val currentActivity: WeakReference<Activity>?
    fun logNetworkPerformance(
        url: String,
        startTime: Long,
        method: String,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
        responseCode: Int
    )

    fun setLocation(location: Location?)

    fun setOptOut(optOutStatus: Boolean)

    fun getSurveyUrl(
        serviceProviderId: Int,
        @Nullable userAttributes: Map<String?, String?>?,
        @Nullable userAttributeLists: Map<String?, List<String?>?>?
    ): Uri? // userAttributes and userAttributes list can contains key, values nulls?

    fun reset()

}

interface KitManager : Events, ActivityLifecycleCallbacks, Session, Identity, ApplicationState,
    Push, UserAttributes, KitIntegrationOperations {

    fun isKitActive(kitId: Int): Boolean
    fun getKitInstance(kitId: Int): Any?
    val supportedKits: Set<Int?>? //should be notnull values and nutnull set (maybe empty set)
    fun updateKits(jsonArray: JSONArray?): KitsLoadedCallback? //always returning not null in the current implementation can we make it notnull?
    fun updateDataplan(dataplanOptions: DataplanOptions)
    val kitStatus: Map<Int?, KitStatus?> //Key doesnt make sense to be null, status is from an enum, can key,value be notnull?


    fun installReferrerUpdated()

    val attributionResults: Map<Int?, AttributionResult?>? //initialized so never null?, key shouldnt be null, AttrResult should be null?


    enum class KitStatus {
        NOT_CONFIGURED, STOPPED, ACTIVE
    }
}