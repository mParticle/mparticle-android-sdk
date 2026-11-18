package com.mparticle.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApi.SingleUserIdentificationCallback
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.listeners.InternalListenerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * This class is responsible for maintaining the session state by listening to the Activity lifecycle.
 */
open class AppStateManager
@JvmOverloads
constructor(context: Context, unitTesting: Boolean = false) {
    private var mConfigManager: ConfigManager? = null
    var mContext: Context
    private val mPreferences: SharedPreferences
    open var session: InternalSession = InternalSession()

    open var currentActivity: WeakReference<Activity?>? = null
        set

    var currentActivityName: String? = null
        private set
    var mLastStoppedTime: AtomicLong

    /**
     * it can take some time between when an activity stops and when a new one (or the same one on a configuration change/rotation)
     * starts again, so use this handler and ACTIVITY_DELAY to determine when we're *really" in the background
     */
    @JvmField
    var delayedBackgroundCheckHandler: Handler = Handler(Looper.getMainLooper())

    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    /**
     * Some providers need to know for the given session, how many 'interruptions' there were - how many
     * times did the user leave and return prior to the session timing out.
     */
    var mInterruptionCount: AtomicInteger = AtomicInteger(0)

    /**
     * Important to determine foreground-time length for a given session.
     * Uses the system-uptime clock to avoid devices which wonky clocks, or clocks
     * that change while the app is running.
     */
    private var mLastForegroundTime: Long = 0

    var mUnitTesting: Boolean = false
    internal var clockOverrideMs: (() -> Long)? = null
    private var mMessageManager: MessageManager? = null
    open var launchUri: Uri? = null
        set
    var launchAction: String? = null
        private set

    @Volatile
    private var currentScreenViewIdValue: String? = null

    @Volatile
    private var currentScreenViewActivityHash: Int? = null

    @Volatile
    private var pendingScreenViewId: PendingScreenViewId? = null

    private class PendingScreenViewId(val id: String, val activityHash: Int?, val mintedAtElapsedRealtimeMs: Long)

    init {
        mUnitTesting = unitTesting
        mContext = context.applicationContext
        mLastStoppedTime = AtomicLong(time)
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        ConfigManager.addMpIdChangeListener { newMpid, previousMpid ->
            if (session != null) {
                session.addMpid(newMpid)
            }
        }
    }

    fun init(apiVersion: Int) {
        if (apiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setupLifecycleCallbacks()
        }
    }

    fun setConfigManager(manager: ConfigManager?) {
        mConfigManager = manager
    }

    fun setMessageManager(manager: MessageManager?) {
        mMessageManager = manager
    }

    private val time: Long
        get() =
            clockOverrideMs?.invoke()
                ?: if (mUnitTesting) {
                    System.currentTimeMillis()
                } else {
                    SystemClock.elapsedRealtime()
                }

    private fun currentActivityIdentityHash(): Int? = currentActivity?.get()?.let { System.identityHashCode(it) }

    private fun isPendingScreenViewIdValid(pending: PendingScreenViewId, activityHash: Int?, nowMs: Long): Boolean {
        val elapsedMs = nowMs - pending.mintedAtElapsedRealtimeMs
        if (elapsedMs < 0L || elapsedMs > PENDING_SCREEN_VIEW_ID_TTL_MS) {
            return false
        }
        return pending.activityHash == null || pending.activityHash == activityHash
    }

    private fun clearScreenViewIds() {
        synchronized(this) {
            currentScreenViewIdValue = null
            currentScreenViewActivityHash = null
            pendingScreenViewId = null
        }
    }

    private fun clearScreenViewIdsOnSessionStart() {
        synchronized(this) {
            val nowMs = time
            val activityHash = currentActivityIdentityHash()
            val pending = pendingScreenViewId
            if (pending != null && isPendingScreenViewIdValid(pending, activityHash, nowMs)) {
                if (currentScreenViewIdValue != pending.id) {
                    currentScreenViewIdValue = null
                    currentScreenViewActivityHash = null
                }
            } else {
                currentScreenViewIdValue = null
                currentScreenViewActivityHash = null
                pendingScreenViewId = null
            }
        }
    }

    // Binds pre-resume IDs to this Activity; an ID bound to another Activity survives so it returns after an overlay closes.
    private fun reconcileScreenViewIdsOnActivityResumed(activity: Activity?) {
        val resumedActivityHash = activity?.let { System.identityHashCode(it) } ?: return
        synchronized(this) {
            val pending = pendingScreenViewId
            if (pending != null) {
                if (!isPendingScreenViewIdValid(pending, resumedActivityHash, time)) {
                    pendingScreenViewId = null
                } else if (pending.activityHash == null) {
                    pendingScreenViewId = PendingScreenViewId(pending.id, resumedActivityHash, pending.mintedAtElapsedRealtimeMs)
                }
            }
            if (currentScreenViewIdValue != null && currentScreenViewActivityHash == null) {
                currentScreenViewActivityHash = resumedActivityHash
            }
        }
    }

    /**
     * Returns the active per-screen visit identifier, or null when no screen is active.
     *
     * Supported ordering:
     * - Multi-Activity apps: placements called before or after `MParticle.logScreen` on the same
     *   Activity instance share one `screen_view_id` within the 2,000 ms pending window.
     * - Single-Activity apps (Fragment or Compose navigation): the app-facing contract is to call
     *   `MParticle.logScreen` on screen entry before `selectPlacements` ([reservePendingScreenViewId]
     *   is an internal library-group hook for SDK navigation integrations).
     * - Configuration changes (rotation, theme, locale): recreating an Activity gives it a new
     *   instance identity that no longer matches the previous ID, so apps that re-request placements
     *   after recreation must also re-log the screen.
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun getCurrentScreenViewId(): String? = currentScreenViewIdValue

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun reservePendingScreenViewId(): String = synchronized(this) {
        val nowMs = time
        val activityHash = currentActivityIdentityHash()
        val existing = pendingScreenViewId
        if (existing != null && isPendingScreenViewIdValid(existing, activityHash, nowMs)) {
            return existing.id
        }
        val minted = UUID.randomUUID().toString()
        pendingScreenViewId = PendingScreenViewId(minted, activityHash, nowMs)
        currentScreenViewIdValue = null
        currentScreenViewActivityHash = null
        minted
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmOverloads
    open fun mintOrAdoptScreenViewId(callerScreenViewId: String? = null): String = synchronized(this) {
        val nowMs = time
        val activityHash = currentActivityIdentityHash()
        val pending = pendingScreenViewId
        pendingScreenViewId = null
        val nonBlankCallerId = callerScreenViewId?.takeIf { it.isNotBlank() && it.length <= Constants.LIMIT_ATTR_VALUE }
        val resolved =
            if (nonBlankCallerId != null) {
                nonBlankCallerId
            } else if (pending != null && isPendingScreenViewIdValid(pending, activityHash, nowMs)) {
                pending.id
            } else {
                UUID.randomUUID().toString()
            }
        currentScreenViewIdValue = resolved
        currentScreenViewActivityHash = activityHash
        resolved
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun getOrCreateScreenViewIdForPlacement(): String = synchronized(this) {
        val nowMs = time
        val activityHash = currentActivityIdentityHash()
        val pending = pendingScreenViewId
        if (pending != null) {
            if (isPendingScreenViewIdValid(pending, activityHash, nowMs)) {
                currentScreenViewIdValue = pending.id
                currentScreenViewActivityHash = activityHash ?: pending.activityHash
                return pending.id
            }
            pendingScreenViewId = null
        }
        val current = currentScreenViewIdValue
        if (current != null && currentScreenViewActivityHash == activityHash) {
            return current
        }
        val minted = UUID.randomUUID().toString()
        pendingScreenViewId = PendingScreenViewId(minted, activityHash, nowMs)
        currentScreenViewIdValue = minted
        currentScreenViewActivityHash = activityHash
        minted
    }

    fun onActivityResumed(activity: Activity?) {
        try {
            currentActivityName = getActivityName(activity)

            val interruptions = mInterruptionCount.get()
            if (!mInitialized || !session.isActive) {
                mInterruptionCount = AtomicInteger(0)
            }
            var previousSessionPackage: String? = null
            var previousSessionUri: String? = null
            var previousSessionParameters: String? = null
            if (activity != null) {
                val callingApplication = activity.callingActivity
                if (callingApplication != null) {
                    previousSessionPackage = callingApplication.packageName
                }
                if (activity.intent != null) {
                    previousSessionUri = activity.intent.dataString
                    if (launchUri == null) {
                        launchUri = activity.intent.data
                    }
                    if (launchAction == null) {
                        launchAction = activity.intent.action
                    }
                    if (activity.intent.extras?.getBundle(Constants.External.APPLINK_KEY) != null) {
                        val parameters = JSONObject()
                        try {
                            parameters.put(
                                Constants.External.APPLINK_KEY,
                                MPUtility.wrapExtras(
                                    activity.intent.extras?.getBundle(Constants.External.APPLINK_KEY),
                                ),
                            )
                        } catch (e: Exception) {
                            Logger.error("Exception on onActivityResumed ")
                        }
                        previousSessionParameters = parameters.toString()
                    }
                }
            }

            session.updateBackgroundTime(mLastStoppedTime, time)

            var isBackToForeground = false
            if (!mInitialized) {
                initialize(
                    currentActivityName,
                    previousSessionUri,
                    previousSessionParameters,
                    previousSessionPackage,
                )
            } else if (isBackgrounded() && mLastStoppedTime.get() > 0) {
                isBackToForeground = true
                mMessageManager?.postToMessageThread(CheckAdIdRunnable(mConfigManager))
                logStateTransition(
                    Constants.StateTransitionType.STATE_TRANS_FORE,
                    currentActivityName,
                    mLastStoppedTime.get() - mLastForegroundTime,
                    time - mLastStoppedTime.get(),
                    previousSessionUri,
                    previousSessionParameters,
                    previousSessionPackage,
                    interruptions,
                )
            }
            CoroutineScope(Dispatchers.IO).launch {
                mConfigManager?.setPreviousAdId()
            }
            mLastForegroundTime = time

            reconcileScreenViewIdsOnActivityResumed(activity)

            if (currentActivity != null) {
                currentActivity?.clear()
                currentActivity = null
            }
            currentActivity = WeakReference(activity)

            val instance = MParticle.getInstance()
            if (instance != null) {
                if (isBackToForeground) {
                    instance.Internal().kitManager.onApplicationForeground()
                    Logger.debug("App foregrounded.")
                }
                instance.Internal().kitManager.onActivityResumed(activity)
            }
        } catch (e: Exception) {
            Logger.verbose("Failed while trying to track activity resume: " + e.message)
        }
    }

    fun onActivityPaused(activity: Activity) {
        try {
            mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).apply()
            mLastStoppedTime = AtomicLong(time)
            if (currentActivity != null && activity === currentActivity?.get()) {
                currentActivity?.clear()
                currentActivity = null
            }

            delayedBackgroundCheckHandler.postDelayed(
                {
                    try {
                        if (isBackgrounded()) {
                            checkSessionTimeout()
                            logBackgrounded()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                ACTIVITY_DELAY,
            )

            val instance = MParticle.getInstance()
            if (instance != null) {
                instance.Internal().kitManager.onActivityPaused(activity)
            }
        } catch (e: Exception) {
            Logger.verbose("Failed while trying to track activity pause: " + e.message)
        }
    }

    fun ensureActiveSession() {
        if (!mInitialized) {
            initialize(null, null, null, null)
        }
        session.mLastEventTime = System.currentTimeMillis()
        if (!session.isActive) {
            newSession()
        } else {
            mMessageManager?.updateSessionEnd(this.session)
        }
    }

    fun logStateTransition(
        transitionType: String?,
        currentActivity: String?,
        previousForegroundTime: Long,
        suspendedTime: Long,
        dataString: String?,
        launchParameters: String?,
        launchPackage: String?,
        interruptions: Int,
    ) {
        if (mConfigManager?.isEnabled == true) {
            ensureActiveSession()
            mMessageManager?.logStateTransition(
                transitionType,
                currentActivity,
                dataString,
                launchParameters,
                launchPackage,
                previousForegroundTime,
                suspendedTime,
                interruptions,
            )
        }
    }

    fun logStateTransition(transitionType: String?, currentActivity: String?) {
        logStateTransition(transitionType, currentActivity, 0, 0, null, null, null, 0)
    }

    /**
     * Creates a new session and generates the start-session message.
     */
    private fun newSession() {
        startSession()
        mMessageManager?.startSession(session)
        Logger.debug("Started new session")
        mMessageManager?.startUploadLoop()
        enableLocationTracking()
        checkSessionTimeout()
    }

    private fun enableLocationTracking() {
        if (mPreferences.contains(Constants.PrefKeys.LOCATION_PROVIDER)) {
            val provider = mPreferences.getString(Constants.PrefKeys.LOCATION_PROVIDER, null)
            val minTime = mPreferences.getLong(Constants.PrefKeys.LOCATION_MINTIME, 0)
            val minDistance = mPreferences.getLong(Constants.PrefKeys.LOCATION_MINDISTANCE, 0)
            if (provider != null && minTime > 0 && minDistance > 0) {
                val instance = MParticle.getInstance()
                instance?.enableLocationTracking(provider, minTime, minDistance)
            }
        }
    }

    fun shouldEndSession(): Boolean {
        val instance = MParticle.getInstance()
        return (
            0L != session?.mSessionStartTime &&
                isBackgrounded() &&
                mConfigManager?.sessionTimeout?.let { session.isTimedOut(it) } == true &&
                (instance == null || !instance.Media().audioPlaying)
            )
    }

    private fun checkSessionTimeout() {
        mConfigManager?.sessionTimeout?.toLong()?.let {
            delayedBackgroundCheckHandler.postDelayed({
                if (shouldEndSession()) {
                    Logger.debug("Session timed out")
                    endSession()
                }
            }, it)
        }
    }

    private fun initialize(
        currentActivityName: String?,
        previousSessionUri: String?,
        previousSessionParameters: String?,
        previousSessionPackage: String?,
    ) {
        mInitialized = true
        logStateTransition(
            Constants.StateTransitionType.STATE_TRANS_INIT,
            currentActivityName,
            0,
            0,
            previousSessionUri,
            previousSessionParameters,
            previousSessionPackage,
            0,
        )
    }

    fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onActivityCreated(activity, savedInstanceState)
    }

    fun onActivityStarted(activity: Activity?) {
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onActivityStarted(activity)
    }

    fun onActivityStopped(activity: Activity?) {
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onActivityStopped(activity)
    }

    private fun logBackgrounded() {
        clearScreenViewIds()
        val instance = MParticle.getInstance()
        if (instance != null) {
            logStateTransition(Constants.StateTransitionType.STATE_TRANS_BG, currentActivityName)
            instance.Internal().kitManager.onApplicationBackground()
            currentActivityName = null
            Logger.debug("App backgrounded.")
            mInterruptionCount.incrementAndGet()
        }
    }

    @TargetApi(14)
    private fun setupLifecycleCallbacks() {
        lifecycleCallbacks =
            MPLifecycleCallbackDelegate(this).also {
                (mContext as Application).registerActivityLifecycleCallbacks(it)
            }
    }

    /**
     * Detaches this instance from the process, so that the next start of the SDK behaves like a new app launch.
     */
    fun shutdown() {
        lifecycleCallbacks?.let { (mContext as Application).unregisterActivityLifecycleCallbacks(it) }
        lifecycleCallbacks = null
        delayedBackgroundCheckHandler.removeCallbacksAndMessages(null)
        clearScreenViewIds()
        mInitialized = false
    }

    open fun isBackgrounded(): Boolean = !mInitialized || (currentActivity == null && (time - mLastStoppedTime.get() >= ACTIVITY_DELAY))

    open fun fetchSession(): InternalSession = session

    fun endSession() {
        Logger.debug("Ended session")
        mMessageManager?.endSession(session)
        disableLocationTracking()
        session = InternalSession()
        clearScreenViewIds()
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onSessionEnd()
        InternalListenerManager.listener.onSessionUpdated(session)
    }

    private fun disableLocationTracking() {
        val editor = mPreferences.edit()
        editor
            .remove(Constants.PrefKeys.LOCATION_PROVIDER)
            .remove(Constants.PrefKeys.LOCATION_MINTIME)
            .remove(Constants.PrefKeys.LOCATION_MINDISTANCE)
            .apply()
        val instance = MParticle.getInstance()
        instance?.disableLocationTracking()
    }

    fun startSession() {
        session = InternalSession().start(mContext)
        clearScreenViewIdsOnSessionStart()
        mLastStoppedTime = AtomicLong(time)
        enableLocationTracking()
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onSessionStart()
    }

    fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onActivitySaveInstanceState(activity, outState)
    }

    fun onActivityDestroyed(activity: Activity?) {
        val instance = MParticle.getInstance()
        instance?.Internal()?.kitManager?.onActivityDestroyed(activity)
    }

    internal class CheckAdIdRunnable(var configManager: ConfigManager?) : Runnable {
        override fun run() {
            val adIdInfo =
                MPUtility.getAdIdInfo(
                    MParticle
                        .getInstance()
                        ?.Internal()
                        ?.appStateManager
                        ?.mContext,
                )
            val currentAdId =
                (if (adIdInfo == null) null else (if (adIdInfo.isLimitAdTrackingEnabled) null else adIdInfo.id))
            val previousAdId = configManager?.previousAdId
            if (currentAdId != null && currentAdId != previousAdId) {
                val instance = MParticle.getInstance()
                if (instance != null) {
                    val user = instance.Identity().currentUser
                    if (user != null) {
                        instance.Identity().modify(
                            Builder(user)
                                .googleAdId(currentAdId, previousAdId)
                                .build(),
                        )
                    } else {
                        instance
                            .Identity()
                            .addIdentityStateListener(
                                object : SingleUserIdentificationCallback() {
                                    override fun onUserFound(user: MParticleUser) {
                                        instance.Identity().modify(
                                            Builder(user)
                                                .googleAdId(currentAdId, previousAdId)
                                                .build(),
                                        )
                                    }
                                },
                            )
                    }
                }
            }
        }
    }

    internal class Builder : IdentityApiRequest.Builder {
        constructor(user: MParticleUser?) : super(user)

        constructor() : super()

        public override fun googleAdId(newGoogleAdId: String?, oldGoogleAdId: String?): IdentityApiRequest.Builder =
            super.googleAdId(newGoogleAdId, oldGoogleAdId)
    }

    companion object {
        /**
         * This boolean is important in determining if the app is running due to the user opening the app,
         * or if we're running due to the reception of a Intent such as an FCM message.
         */
        @JvmField
        var mInitialized: Boolean = false

        const val ACTIVITY_DELAY: Long = 1000

        /**
         * Constants used by the messaging/push framework to describe the app state when various
         * interactions occur (receive/show/tap).
         */
        const val APP_STATE_FOREGROUND: String = "foreground"
        const val APP_STATE_BACKGROUND: String = "background"
        const val APP_STATE_NOTRUNNING: String = "not_running"
        private const val PENDING_SCREEN_VIEW_ID_TTL_MS: Long = 2000L

        private fun getActivityName(activity: Activity?): String = activity?.javaClass?.canonicalName ?: ""
    }
}
