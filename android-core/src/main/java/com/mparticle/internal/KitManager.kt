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
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import java.lang.Exception
import java.lang.ref.WeakReference

interface Events {
    fun logEvent(@NotNull event: BaseEvent)
    fun logScreen(@NotNull screenEvent: MPEvent) //The queue which uses this accepts null
    fun logBatch(@Nullable jsonObject: String?) // Should be null?
    fun leaveBreadcrumb(@NotNull breadcrumb: String)
    fun logError(
        @NotNull message: String,
        @Nullable eventData: Map<String, String?>?
    )

    fun logException(
        @NotNull exception: Exception,
        @Nullable eventData: Map<String, String?>?,
        @Nullable message: String?
    )
}

interface ActivityLifecycleCallbacks {
    fun onActivityCreated(@NotNull activity: Activity, @Nullable savedInstanceState: Bundle?)
    fun onActivityStarted(@NotNull activity: Activity)
    fun onActivityResumed(@NotNull activity: Activity)
    fun onActivityPaused(@NotNull activity: Activity)
    fun onActivityStopped(@NotNull activity: Activity)
    fun onActivitySaveInstanceState(@NotNull activity: Activity, @Nullable outState: Bundle?)
    fun onActivityDestroyed(@NotNull activity: Activity)
}

interface Session {
    fun onSessionEnd()
    fun onSessionStart()
    fun logout()
}

interface Identity {
    fun onIdentifyCompleted(@NotNull user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onLoginCompleted(@NotNull user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onLogoutCompleted(@NotNull user: MParticleUser, @Nullable request: IdentityApiRequest?)
    fun onModifyCompleted(@Nullable user: MParticleUser?, @NotNull request: IdentityApiRequest)
}

interface ApplicationState {
    fun onApplicationForeground()
    fun onApplicationBackground()
}

interface Push {
    fun onMessageReceived(
        context: Context,
        intent: Intent
    ): Boolean //safe to make intent and context not null, i think yes

    fun onPushRegistration(@Nullable instanceId: String?, @Nullable senderId: String?): Boolean
}

interface UserAttributes {
    fun setUserAttribute(
        @NotNull key: String,
        @Nullable value: String?,
        @NotNull mpid: Long
    ) //key shouldnt be null, value might be null and can be null?

    fun setUserAttributeList(
        @NotNull key: String,
        value: List<String?>?,
        @NotNull mpid: Long
    ) //key shouldnt be null, value contains null and can be null?

    fun removeUserAttribute(@NotNull key: String, @NotNull mpid: Long) //key shouldnt be null

    fun setUserTag(@NotNull tag: String, @NotNull mpid: Long)//key shouldnt be null

    fun incrementUserAttribute(
        @NotNull key: String,
        incrementValue: Number,
        newValue: String?,
        @NotNull mpid: Long
    )//key shouldnt be null - what about increment value and newValue?


    fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        @NotNull   mpid: Long
    ) // any of the consent state should be notnull?

    fun setUserIdentity(
        id: String,
       identityType: IdentityType
    ) //seems that the id should be notnull, identity type? - MParticleUserDelegate has an if!=null

    fun removeUserIdentity(@NotNull id: IdentityType)// identity type shouldnt be null, its the only param - MParticleUserDelegate has an if!=null

}

interface KitIntegrationOperations {
    val currentActivity: WeakReference<Activity>?
    fun logNetworkPerformance(
        @NotNull url: String,
        @NotNull  startTime: Long,
        @NotNull  method: String,
        @NotNull length: Long,
        @NotNull   bytesSent: Long,
        @NotNull bytesReceived: Long,
        @Nullable requestString: String?,
        @NotNull responseCode: Int
    )

    fun setLocation(@Nullable location: Location?)

    fun setOptOut(@NotNull optOutStatus: Boolean)

    @Nullable
    fun getSurveyUrl(
        @NotNull serviceProviderId: Int,
        @Nullable userAttributes: Map<String, String?>?,
        @Nullable userAttributeLists: Map<String, List<String?>?>?
    ): Uri? // userAttributes and userAttributes list can contains key, values nulls?

    fun reset()

}

interface KitManager : Events, ActivityLifecycleCallbacks, Session, Identity, ApplicationState,
    Push, UserAttributes, KitIntegrationOperations {

    @NotNull fun isKitActive(@NotNull kitId: Int): Boolean
    @Nullable fun getKitInstance(@NotNull kitId: Int): Any?
    val supportedKits: Set<Int>? //should be notnull values and nutnull set (maybe empty set)
    fun updateKits(jsonArray: JSONArray?): KitsLoadedCallback //always returning not null in the current implementation can we make it notnull?
    fun updateDataplan(@NotNull dataplanOptions: DataplanOptions)
    val kitStatus: Map<Int, KitStatus> //Key doesnt make sense to be null, status is from an enum, can key,value be notnull?


    fun installReferrerUpdated()

    val attributionResults: Map<Int, AttributionResult?>? //initialized so never null?, key shouldnt be null, AttrResult should be null?


    enum class KitStatus {
        NOT_CONFIGURED, STOPPED, ACTIVE
    }
}