package com.mparticle.internal

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.mock.MockContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.lang.ref.WeakReference
import java.util.Random

class ScreenViewIdTest {
    private lateinit var looperStatic: MockedStatic<Looper>
    private lateinit var context: MockContext
    private lateinit var configManager: ConfigManager
    private lateinit var appStateManager: AppStateManager
    private lateinit var messageManager: MessageManager
    private lateinit var messageHandler: MessageHandler
    private lateinit var uploadHandler: UploadHandler
    private lateinit var mp: MockMParticle
    private var nowMs: Long = 10_000L

    @Before
    fun setUp() {
        looperStatic = Mockito.mockStatic(Looper::class.java)
        val looper: Looper = Mockito.mock(Looper::class.java)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)

        mp = MockMParticle()
        MParticle.setInstance(mp)
        context = MockContext()
        configManager = Mockito.mock(ConfigManager::class.java)
        Mockito.`when`(configManager.apiKey).thenReturn("test-api-key")
        Mockito.`when`(configManager.isEnabled).thenReturn(true)
        Mockito.`when`(configManager.mpid).thenReturn(1L)
        Mockito
            .`when`(configManager.userStorage)
            .thenReturn(UserStorage.create(context, Random().nextInt().toLong()))

        nowMs = 10_000L
        appStateManager = AppStateManager(context, true)
        appStateManager.clockOverrideMs = { nowMs }
        appStateManager.delayedBackgroundCheckHandler = Mockito.mock(Handler::class.java)
        appStateManager.setConfigManager(configManager)
        appStateManager.session.start(context)

        messageHandler = Mockito.mock(MessageHandler::class.java)
        uploadHandler = Mockito.mock(UploadHandler::class.java)
        Mockito
            .`when`(messageHandler.obtainMessage(Mockito.anyInt(), Mockito.any()))
            .thenAnswer { invocation ->
                Message().apply {
                    what = invocation.getArgument(0)
                    obj = invocation.getArgument(1)
                }
            }
        Mockito.`when`(messageHandler.obtainMessage(Mockito.anyInt())).thenReturn(Message())

        messageManager =
            MessageManager(
                context,
                configManager,
                MParticle.InstallType.AutoDetect,
                appStateManager,
                Mockito.mock(MParticleDBManager::class.java),
                messageHandler,
                uploadHandler,
            )
        messageManager.attachScreenViewId = true
        appStateManager.setMessageManager(messageManager)
        setMParticleField("mAppStateManager", appStateManager)
        setMParticleField("mConfigManager", configManager)
        setMParticleField("mMessageManager", messageManager)
    }

    private fun setMParticleField(name: String, value: Any) {
        val field = MParticle::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(mp, value)
    }

    @After
    fun tearDown() {
        looperStatic.close()
    }

    @Test
    fun testMintOrAdoptScreenViewIdMintsDistinctIdsPerScreen() {
        val homeActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(homeActivity)

        val firstId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotNull(firstId)
        Assert.assertEquals(firstId, appStateManager.getCurrentScreenViewId())
        Assert.assertEquals(firstId, appStateManager.getOrCreateScreenViewIdForPlacement())

        val secondId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotNull(secondId)
        Assert.assertNotEquals(firstId, secondId)
        Assert.assertEquals(secondId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testPendingScreenViewIdAdoptedWithinWindowOnSameActivity() {
        val checkoutActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(checkoutActivity)

        val reservedId = appStateManager.reservePendingScreenViewId()
        nowMs += 16L
        val placementId = appStateManager.getOrCreateScreenViewIdForPlacement()
        val adoptedId = appStateManager.mintOrAdoptScreenViewId()

        Assert.assertEquals(reservedId, placementId)
        Assert.assertEquals(reservedId, adoptedId)
        Assert.assertEquals(reservedId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testPlacementBeforeLogScreenOnSameActivitySharesScreenViewIdWithinTtl() {
        val productActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(productActivity)

        val placementId = appStateManager.getOrCreateScreenViewIdForPlacement()
        nowMs += 500L
        val adoptedId = appStateManager.mintOrAdoptScreenViewId()

        Assert.assertEquals(placementId, adoptedId)
        Assert.assertEquals(placementId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testPendingScreenViewIdRejectedAfterTtlExpires() {
        val homeActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(homeActivity)

        val pendingId = appStateManager.reservePendingScreenViewId()
        nowMs += 2_001L
        val mintedId = appStateManager.mintOrAdoptScreenViewId()

        Assert.assertNotEquals(pendingId, mintedId)
        Assert.assertEquals(mintedId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testPendingScreenViewIdRejectedWhenActivityChanges() {
        val homeActivity = Mockito.mock(Activity::class.java)
        val settingsActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(homeActivity)

        val homePlacementId = appStateManager.getOrCreateScreenViewIdForPlacement()
        nowMs += 100L
        appStateManager.currentActivity = WeakReference(settingsActivity)

        val settingsScreenId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotEquals(homePlacementId, settingsScreenId)
        Assert.assertEquals(settingsScreenId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testMessageManagerAttachesScreenViewIdToUploadedMessageWithoutMutatingEvent() {
        val checkoutActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(checkoutActivity)

        val screenViewId = appStateManager.mintOrAdoptScreenViewId()
        val event =
            MPEvent
                .Builder("Checkout")
                .customAttributes(mapOf("step" to "payment"))
                .build()

        val message = messageManager.logScreen(event, true)!!
        val attrs = message.getJSONObject(MessageKey.ATTRIBUTES)
        Assert.assertEquals("payment", attrs.getString("step"))
        Assert.assertEquals(screenViewId, attrs.getString("screen_view_id"))
        Assert.assertFalse(event.customAttributeStrings?.containsKey("screen_view_id") ?: false)
    }

    @Test
    fun testMessageManagerAttachesScreenViewIdWhenEventHasNoCustomAttributes() {
        val screenViewId = appStateManager.mintOrAdoptScreenViewId()
        val event = MPEvent.Builder("Home").build()

        val message = messageManager.logScreen(event, true)!!
        val attrs = message.getJSONObject(MessageKey.ATTRIBUTES)
        Assert.assertEquals(screenViewId, attrs.getString("screen_view_id"))
        Assert.assertNull(event.customAttributeStrings)
    }

    @Test
    fun testMessageManagerPreservesCallerProvidedScreenViewIdAndSkipsStoppedEvents() {
        appStateManager.mintOrAdoptScreenViewId()
        val customEvent =
            MPEvent
                .Builder("CustomScreen")
                .customAttributes(mapOf("screen_view_id" to "caller-supplied-id"))
                .build()

        val startedMessage = messageManager.logScreen(customEvent, true)!!
        Assert.assertEquals(
            "caller-supplied-id",
            startedMessage.getJSONObject(MessageKey.ATTRIBUTES).getString("screen_view_id"),
        )

        val stoppedEvent = MPEvent.Builder("StoppedScreen").build()
        val stoppedMessage = messageManager.logScreen(stoppedEvent, false)!!
        Assert.assertFalse(stoppedMessage.has(MessageKey.ATTRIBUTES))
    }

    @Test
    fun testEndSessionAndShutdownClearScreenViewIdState() {
        val activity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(activity)
        val firstId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(firstId, appStateManager.getCurrentScreenViewId())

        appStateManager.endSession()
        Assert.assertNull(appStateManager.getCurrentScreenViewId())

        val secondId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotNull(secondId)
        appStateManager.shutdown()
        Assert.assertNull(appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testPreResumeLogScreenBindsOnResumeAndIsNotReusedByAnotherActivity() {
        val activityA = Mockito.mock(Activity::class.java)
        val activityB = Mockito.mock(Activity::class.java)
        val activityC = Mockito.mock(Activity::class.java)

        appStateManager.onActivityResumed(activityA)
        appStateManager.onActivityPaused(activityA)
        Assert.assertNull(appStateManager.currentActivity)

        // Activity B logs a screen in onCreate before onActivityResumed runs.
        val screenBId = appStateManager.mintOrAdoptScreenViewId()
        appStateManager.onActivityResumed(activityB)
        Assert.assertEquals(screenBId, appStateManager.getOrCreateScreenViewIdForPlacement())

        // Activity B pauses and Activity C resumes without logging a screen.
        appStateManager.onActivityPaused(activityB)
        appStateManager.onActivityResumed(activityC)

        val placementCId = appStateManager.getOrCreateScreenViewIdForPlacement()
        Assert.assertNotEquals(screenBId, placementCId)
    }

    @Test
    fun testScreenKeepsScreenViewIdAfterOverlayActivityCloses() {
        val checkoutActivity = Mockito.mock(Activity::class.java)
        val overlayActivity = Mockito.mock(Activity::class.java)

        appStateManager.onActivityResumed(checkoutActivity)
        val checkoutId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(checkoutId, appStateManager.getOrCreateScreenViewIdForPlacement())

        // A placement opens an overlay Activity, which closes without logging a screen.
        appStateManager.onActivityPaused(checkoutActivity)
        appStateManager.onActivityResumed(overlayActivity)
        appStateManager.onActivityPaused(overlayActivity)
        appStateManager.onActivityResumed(checkoutActivity)

        Assert.assertEquals(checkoutId, appStateManager.getOrCreateScreenViewIdForPlacement())
    }

    @Test
    fun testScreenViewIdIsUploadedOnlyWhenRoktKitIsOnClasspath() {
        // The core unit-test classpath has no Rokt kit.
        Assert.assertFalse(MessageManager().attachScreenViewId)

        messageManager.attachScreenViewId = false
        val screenViewId = appStateManager.mintOrAdoptScreenViewId()
        val message = messageManager.logScreen(MPEvent.Builder("Home").build(), true, screenViewId)!!
        Assert.assertFalse(message.has(MessageKey.ATTRIBUTES))
    }

    @Test
    fun testPreResumePlacementSurvivesActivityResumeAndAdoptsOnLogScreen() {
        val activityA = Mockito.mock(Activity::class.java)
        val activityB = Mockito.mock(Activity::class.java)

        appStateManager.onActivityResumed(activityA)
        val screenAId = appStateManager.mintOrAdoptScreenViewId()
        appStateManager.onActivityPaused(activityA)

        // Placement runs in Activity B's onCreate while currentActivity is null.
        nowMs += 50L
        val placementBId = appStateManager.getOrCreateScreenViewIdForPlacement()
        Assert.assertNotEquals(screenAId, placementBId)

        nowMs += 50L
        appStateManager.onActivityResumed(activityB)
        val adoptedBId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(placementBId, adoptedBId)
        Assert.assertEquals(placementBId, appStateManager.getCurrentScreenViewId())
    }

    @Test
    fun testColdSessionPlacementBeforeMParticleLogScreenPreservesPendingReservation() {
        val launchActivity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(launchActivity)
        appStateManager.session = InternalSession()
        Assert.assertFalse(appStateManager.session.isActive)

        val reservedId = appStateManager.getOrCreateScreenViewIdForPlacement()
        nowMs += 100L

        val event = MPEvent.Builder("LaunchScreen").build()
        MParticle.getInstance()!!.logScreen(event)

        Assert.assertTrue(appStateManager.session.isActive)
        Assert.assertEquals(reservedId, appStateManager.getCurrentScreenViewId())

        val messageCaptor = ArgumentCaptor.forClass(Message::class.java)
        Mockito.verify(messageHandler, Mockito.atLeastOnce()).sendMessage(messageCaptor.capture())
        val screenViewMessage =
            messageCaptor.allValues
                .mapNotNull { it.obj as? BaseMPMessage }
                .first { it.optString("n") == "LaunchScreen" }
        Assert.assertEquals(
            reservedId,
            screenViewMessage.getJSONObject(MessageKey.ATTRIBUTES).getString("screen_view_id"),
        )
    }

    @Test
    fun testBackgroundTransitionClearsCurrentAndPendingScreenViewId() {
        val homeActivity = Mockito.mock(Activity::class.java)
        appStateManager.onActivityResumed(homeActivity)

        val firstVisitId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(firstVisitId, appStateManager.getCurrentScreenViewId())

        val runnableCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        appStateManager.onActivityPaused(homeActivity)
        Mockito
            .verify(appStateManager.delayedBackgroundCheckHandler)
            .postDelayed(runnableCaptor.capture(), Mockito.eq(AppStateManager.ACTIVITY_DELAY))

        nowMs += AppStateManager.ACTIVITY_DELAY + 10L
        runnableCaptor.value.run()
        Assert.assertNull(appStateManager.getCurrentScreenViewId())

        nowMs += 200L
        appStateManager.onActivityResumed(homeActivity)
        val placementAfterResumeId = appStateManager.getOrCreateScreenViewIdForPlacement()
        Assert.assertNotEquals(firstVisitId, placementAfterResumeId)

        val adoptedAfterResumeId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(placementAfterResumeId, adoptedAfterResumeId)
    }

    @Test
    fun testInterleavedMintAndMessageManagerLogScreenPreservesEachScreenViewId() {
        val activity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(activity)

        val firstId = appStateManager.mintOrAdoptScreenViewId()
        val secondId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotEquals(firstId, secondId)

        val firstEvent = MPEvent.Builder("FirstScreen").build()
        val secondEvent = MPEvent.Builder("SecondScreen").build()

        val firstMessage = messageManager.logScreen(firstEvent, true, firstId)!!
        val secondMessage = messageManager.logScreen(secondEvent, true, secondId)!!

        Assert.assertEquals(
            firstId,
            firstMessage.getJSONObject(MessageKey.ATTRIBUTES).getString("screen_view_id"),
        )
        Assert.assertEquals(
            secondId,
            secondMessage.getJSONObject(MessageKey.ATTRIBUTES).getString("screen_view_id"),
        )
    }

    @Test
    fun testCallerSuppliedScreenViewIdAdoptsIntoAppStateManagerAndBlankFallsBackToMintedId() {
        val activity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(activity)

        val callerEvent =
            MPEvent
                .Builder("CallerScreen")
                .customAttributes(mapOf("screen_view_id" to "caller-screen-42"))
                .build()
        MParticle.getInstance()!!.logScreen(callerEvent)
        Assert.assertEquals("caller-screen-42", appStateManager.getCurrentScreenViewId())
        Assert.assertEquals("caller-screen-42", appStateManager.getOrCreateScreenViewIdForPlacement())

        val blankEvent =
            MPEvent
                .Builder("BlankScreen")
                .customAttributes(mapOf("screen_view_id" to "   "))
                .build()
        val mintedForBlank = appStateManager.mintOrAdoptScreenViewId("   ")
        Assert.assertTrue(mintedForBlank.isNotBlank())
        val blankMessage = messageManager.logScreen(blankEvent, true, mintedForBlank)!!
        Assert.assertEquals(
            mintedForBlank,
            blankMessage.getJSONObject(MessageKey.ATTRIBUTES).getString("screen_view_id"),
        )
    }

    @Test
    fun testSingleActivityNavigationOrderingAndPendingReservation() {
        val singleActivity = Mockito.mock(Activity::class.java)
        appStateManager.onActivityResumed(singleActivity)

        // Screen A: logScreen on screen entry followed by placement selection.
        val screenAId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertEquals(screenAId, appStateManager.getOrCreateScreenViewIdForPlacement())

        // Screen B on the same Activity: logScreen on screen entry followed by placement selection.
        val screenBId = appStateManager.mintOrAdoptScreenViewId()
        Assert.assertNotEquals(screenAId, screenBId)
        Assert.assertEquals(screenBId, appStateManager.getOrCreateScreenViewIdForPlacement())

        // Screen C on the same Activity: navigation hook reserves pending ID before placement and logScreen.
        val reservedCId = appStateManager.reservePendingScreenViewId()
        Assert.assertNotEquals(screenBId, reservedCId)
        Assert.assertEquals(reservedCId, appStateManager.getOrCreateScreenViewIdForPlacement())
        Assert.assertEquals(reservedCId, appStateManager.mintOrAdoptScreenViewId())
    }

    @Test
    fun testOversizedCallerScreenViewIdIsIgnoredAndReplacedWithMintedId() {
        val activity = Mockito.mock(Activity::class.java)
        appStateManager.currentActivity = WeakReference(activity)

        val oversizedCallerId = "x".repeat(Constants.LIMIT_ATTR_VALUE + 1)
        val oversizedEvent =
            MPEvent
                .Builder("OversizedScreen")
                .customAttributes(mapOf("screen_view_id" to oversizedCallerId, "valid_key" to "valid_value"))
                .build()

        MParticle.getInstance()!!.logScreen(oversizedEvent)

        val resolvedId = appStateManager.getCurrentScreenViewId()
        Assert.assertNotNull(resolvedId)
        Assert.assertNotEquals(oversizedCallerId, resolvedId)
        Assert.assertTrue(resolvedId!!.length <= Constants.LIMIT_ATTR_VALUE)
        Assert.assertEquals(resolvedId, appStateManager.getOrCreateScreenViewIdForPlacement())

        val messageCaptor = ArgumentCaptor.forClass(Message::class.java)
        Mockito.verify(messageHandler, Mockito.atLeastOnce()).sendMessage(messageCaptor.capture())
        val screenViewMessage =
            messageCaptor.allValues
                .mapNotNull { it.obj as? BaseMPMessage }
                .first { it.optString("n") == "OversizedScreen" }
        val attrs = screenViewMessage.getJSONObject(MessageKey.ATTRIBUTES)
        Assert.assertEquals("valid_value", attrs.getString("valid_key"))
        Assert.assertEquals(resolvedId, attrs.getString("screen_view_id"))
    }
}
