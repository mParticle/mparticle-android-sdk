package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleTask
import com.mparticle.TypedUserAttributeListener
import com.mparticle.UserAttributeListenerType
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.IdentityHttpResponse
import com.mparticle.identity.MParticleUser
import com.mparticle.identity.TaskFailureListener
import com.mparticle.identity.TaskSuccessListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoktKitRequestHelperTest {
    private lateinit var identityApi: IdentityApi
    private lateinit var user: MParticleUser
    private lateinit var kitConfiguration: KitConfiguration
    private lateinit var kitIntegration: KitIntegration
    private lateinit var roktListener: RoktKitBridge
    private val userIdentities = HashMap<IdentityType, String>()
    private val identifyRequests = mutableListOf<IdentityApiRequest>()
    private val failureListeners = mutableListOf<TaskFailureListener>()
    private val successListeners = mutableListOf<TaskSuccessListener>()

    @Before
    fun setUp() {
        val mParticle = mockk<MParticle>(relaxed = true)
        identityApi = mockk(relaxed = true)
        user = mockk(relaxed = true)
        every { user.id } returns 1L
        // Builder.withUser mutates the map it is given, so hand out a copy like the SDK does.
        every { user.userIdentities } answers { HashMap(userIdentities) }
        every { mParticle.Identity() } returns identityApi
        every { identityApi.currentUser } returns user
        every { identityApi.getUser(any()) } returns user
        every { identityApi.identify(any()) } answers {
            identifyRequests.add(firstArg())
            pendingTask()
        }
        MParticle.setInstance(mParticle)

        kitConfiguration = mockk(relaxed = true)
        every { kitConfiguration.hashedEmailUserIdentityType } returns "Other"
        kitIntegration = mockk(relaxed = true)
        every { kitIntegration.configuration } returns kitConfiguration
        roktListener = mockk(relaxed = true)
        RoktKitRequestHelper.requestedNoTargetingTag.clear()
    }

    @After
    fun tearDown() {
        MParticle.setInstance(null)
    }

    // A task that never completes on its own, i.e. identify still in flight.
    private fun pendingTask(): MParticleTask<IdentityApiResult> {
        val task = mockk<MParticleTask<IdentityApiResult>>(relaxed = true)
        val failure = slot<TaskFailureListener>()
        every { task.addFailureListener(capture(failure)) } answers {
            failureListeners.add(failure.captured)
            task
        }
        val success = slot<TaskSuccessListener>()
        every { task.addSuccessListener(capture(success)) } answers {
            successListeners.add(success.captured)
            task
        }
        return task
    }

    private fun completeIdentify(index: Int, identifiedUser: MParticleUser) {
        val result = mockk<IdentityApiResult>(relaxed = true)
        every { result.user } returns identifiedUser
        successListeners[index].onSuccess(result)
    }

    // An identified user whose profile reflects its own attribute writes.
    private fun identifiedUser(stored: MutableMap<String, Any?>, mpid: Long = 2L): MParticleUser {
        val identifiedUser = mockk<MParticleUser>(relaxed = true)
        every { identifiedUser.id } returns mpid
        every { identifiedUser.userIdentities } returns HashMap()
        every { identifiedUser.userAttributes } answers { HashMap(stored) }
        every { identifiedUser.setUserAttribute(any(), any()) } answers {
            stored[firstArg()] = secondArg()
            true
        }
        every { identifiedUser.removeUserAttribute(any()) } answers {
            stored.remove(firstArg<String>())
            true
        }
        return identifiedUser
    }

    private fun selectPlacements(attributes: Map<String, String>) {
        RoktKitRequestHelper.selectPlacements(
            kitIntegration = kitIntegration,
            roktListener = roktListener,
            viewName = "checkout",
            attributes = attributes,
            placeHolders = null,
            fontTypefaces = null,
            config = null,
            options = null,
        )
    }

    private fun verifyPlacementRequested(times: Int = 1) {
        verify(exactly = times) {
            roktListener.selectPlacements(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun selectPlacements_keepsPaypalPayerIdOffTheUserAndSendsItToTheKit() {
        val saved = slot<Map<String, Any>>()
        every { user.setUserAttributes(capture(saved)) } returns true
        val sent = slot<Map<String, String>>()
        every {
            roktListener.selectPlacements(any(), capture(sent), any(), any(), any(), any(), any())
        } returns Unit

        selectPlacements(
            mapOf(
                "firstname" to "Jane",
                "paypalpayerid" to "PAYERID123",
                "PayPalPayerID" to "PAYERID456",
            ),
        )

        assertEquals(mapOf<String, Any>("firstname" to "Jane"), saved.captured)
        assertEquals("PAYERID123", sent.captured["paypalpayerid"])
        assertEquals("PAYERID456", sent.captured["PayPalPayerID"])
    }

    @Test
    fun selectPlacements_whenEmailDiffers_requestsPlacementWithoutWaitingForIdentify() {
        userIdentities[IdentityType.Email] = "old@example.com"

        selectPlacements(mapOf("email" to "new@example.com"))

        assertEquals(1, identifyRequests.size)
        assertEquals("new@example.com", identifyRequests[0].userIdentities[IdentityType.Email])
        verifyPlacementRequested()
    }

    @Test
    fun selectPlacements_whenEmailMatches_doesNotIdentify() {
        userIdentities[IdentityType.Email] = "same@example.com"

        selectPlacements(mapOf("email" to "SAME@example.com"))

        assertTrue(identifyRequests.isEmpty())
        verifyPlacementRequested()
    }

    @Test
    fun selectPlacements_whenHashedEmailDiffers_identifiesWithConfiguredType() {
        every { kitConfiguration.hashedEmailUserIdentityType } returns "Other2"

        selectPlacements(mapOf("emailsha256" to "abc123"))

        assertEquals(1, identifyRequests.size)
        assertEquals("abc123", identifyRequests[0].userIdentities[IdentityType.Other2])
        assertFalse(identifyRequests[0].userIdentities.containsKey(IdentityType.Other))
        verifyPlacementRequested()
    }

    @Test
    fun selectPlacements_whenHashedEmailTypeIsUnknown_doesNotIdentify() {
        every { kitConfiguration.hashedEmailUserIdentityType } returns "Unknown"

        selectPlacements(mapOf("emailsha256" to "abc123"))

        assertTrue(identifyRequests.isEmpty())
        verifyPlacementRequested()
    }

    @Test
    fun selectPlacements_whenIdentifyFails_placementIsUnaffected() {
        selectPlacements(mapOf("email" to "new@example.com"))
        failureListeners.single().onFailure(mockk<IdentityHttpResponse>(relaxed = true))
        selectPlacements(mapOf("email" to "new@example.com"))

        assertEquals(2, identifyRequests.size)
        verifyPlacementRequested(times = 2)
    }

    @Test
    fun selectPlacements_whenIdentifyThrows_stillRequestsPlacement() {
        every { identityApi.identify(any()) } answers {
            identifyRequests.add(firstArg())
            throw IllegalStateException("identify unavailable")
        }

        selectPlacements(mapOf("email" to "new@example.com"))
        selectPlacements(mapOf("email" to "new@example.com"))

        assertEquals(2, identifyRequests.size)
        verifyPlacementRequested(times = 2)
    }

    @Test
    fun selectShoppableAds_whenEmailDiffers_requestsPlacementWithoutWaitingForIdentify() {
        userIdentities[IdentityType.Email] = "old@example.com"

        RoktKitRequestHelper.selectShoppableAds(
            kitIntegration = kitIntegration,
            roktListener = roktListener,
            viewName = "checkout",
            attributes = mapOf("email" to "new@example.com"),
            config = null,
        )

        assertEquals(1, identifyRequests.size)
        verify(exactly = 1) { roktListener.selectShoppableAds(any(), any(), any(), any()) }
    }

    @Test
    fun selectPlacements_excludesPrivacyAttributesFromSetUserAttributes_andNormalizesKeys() {
        val capturedAttributes = slot<Map<String, String>>()
        val capturedUserAttributes = slot<Map<String, Any>>()

        selectPlacements(
            mapOf(
                "noFunctional" to "true",
                "NoTargeting" to "true",
                "doNotShareOrSell" to "false",
                "GPCENABLED" to "true",
                "country" to "US",
            ),
        )

        verify(exactly = 1) { user.setUserAttributes(capture(capturedUserAttributes)) }
        assertEquals(mapOf<String, Any>("country" to "US"), capturedUserAttributes.captured)

        verify(exactly = 1) {
            roktListener.selectPlacements(
                any(),
                capture(capturedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertEquals("true", capturedAttributes.captured["noFunctional"])
        assertEquals("true", capturedAttributes.captured["noTargeting"])
        assertEquals("false", capturedAttributes.captured["doNotShareOrSell"])
        assertEquals("true", capturedAttributes.captured["gpcEnabled"])
        assertEquals("US", capturedAttributes.captured["country"])
        assertFalse(capturedAttributes.captured.containsKey("NoTargeting"))
        assertFalse(capturedAttributes.captured.containsKey("GPCENABLED"))
        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
    }

    @Test
    fun selectPlacements_whenOnlyPrivacyAttributesPassed_doesNotCallSetUserAttributes() {
        every { user.userAttributes } returns mapOf("\$NoTargeting" to true)

        selectPlacements(
            mapOf(
                "noFunctional" to "true",
                "noTargeting" to "false",
                "doNotShareOrSell" to "true",
                "gpcEnabled" to "false",
            ),
        )

        verify(exactly = 0) { user.setUserAttributes(any()) }
        verify(exactly = 1) { user.removeUserAttribute("\$NoTargeting") }
        verify(exactly = 0) { user.setUserAttribute("\$NoTargeting", any()) }
    }

    @Test
    fun selectPlacements_whenProfileHasPrivacyAttributes_mergesIntoPlacementAttributesAndSyncsNoTargeting() {
        every { user.userAttributes } returns mapOf(
            "noTargeting" to "true",
            "noFunctional" to "true",
        )
        val capturedAttributes = slot<Map<String, String>>()

        selectPlacements(emptyMap())

        verify(exactly = 0) { user.setUserAttributes(any()) }
        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
        verify(exactly = 1) {
            roktListener.selectPlacements(
                any(),
                capture(capturedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertEquals("true", capturedAttributes.captured["noTargeting"])
        assertEquals("true", capturedAttributes.captured["noFunctional"])
    }

    @Test
    fun selectPlacements_whenProfileHasNoTargetingFalse_removesNoTargetingUserAttributeWhenPresent() {
        every { user.userAttributes } returns mapOf(
            "noTargeting" to "false",
            "\$NoTargeting" to true,
        )

        selectPlacements(emptyMap())

        verify(exactly = 1) { user.removeUserAttribute("\$NoTargeting") }
        verify(exactly = 0) { user.setUserAttribute("\$NoTargeting", any()) }
    }

    @Test
    fun selectPlacements_whenBothProfileAndPerCallHavePrivacyAttributes_optOutWinsFromEitherSource() {
        every { user.userAttributes } returns mapOf(
            "noTargeting" to "true",
            "noFunctional" to "false",
        )
        val capturedAttributes = slot<Map<String, String>>()

        selectPlacements(
            mapOf(
                "notargeting" to "false",
                "nofunctional" to "true",
            ),
        )

        verify(exactly = 0) { user.setUserAttributes(any()) }
        verify(exactly = 0) { user.removeUserAttribute("\$NoTargeting") }
        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
        verify(exactly = 1) {
            roktListener.selectPlacements(
                any(),
                capture(capturedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertEquals("true", capturedAttributes.captured["noTargeting"])
        assertEquals("true", capturedAttributes.captured["noFunctional"])
    }

    @Test
    fun selectPlacements_whenStoredLegacyFalsePrivacyValueIsOverriddenByPerCallTrue_clearsStoredValue() {
        val storedMap = mutableMapOf<String, Any?>(
            "Notargeting" to "false",
            "NoFunctional" to "false",
        )
        every { user.userAttributes } answers { HashMap(storedMap) }
        every { user.removeUserAttribute(any()) } answers {
            storedMap.remove(firstArg<String>())
            true
        }
        every { user.setUserAttribute(any(), any()) } answers {
            storedMap[firstArg<String>()] = secondArg()
            true
        }
        val capturedAttributes = mutableListOf<Map<String, String>>()

        selectPlacements(
            mapOf(
                "noFunctional" to "false",
            ),
        )
        assertTrue(storedMap.containsKey("NoFunctional"))

        selectPlacements(
            mapOf(
                "noTargeting" to "TRUE",
                "noFunctional" to "True",
                "Sandbox" to "true",
            ),
        )
        selectPlacements(emptyMap())

        verify(exactly = 3) {
            roktListener.selectPlacements(
                any(),
                capture(capturedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertEquals("true", capturedAttributes[1]["noTargeting"])
        assertEquals("true", capturedAttributes[1]["noFunctional"])
        assertEquals("true", capturedAttributes[1]["sandbox"])
        assertFalse(capturedAttributes[1].containsKey("Sandbox"))
        assertFalse(storedMap.containsKey("Notargeting"))
        assertFalse(storedMap.containsKey("NoFunctional"))
        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
        assertFalse(capturedAttributes[2].containsKey("noTargeting"))
        // The last call passes no privacy key and the profile holds none, so the tag goes, as on web.
        assertFalse(storedMap.containsKey("\$NoTargeting"))
    }

    @Test
    fun selectPlacements_whenPendingProfileWriteQueuesOptOut_resolvesAfterPendingWrites() {
        every { user.userAttributes } returns mapOf("noTargeting" to "false")
        every { user.getUserAttributes(any<UserAttributeListenerType>()) } answers {
            val listener = firstArg<UserAttributeListenerType>()
            if (listener is TypedUserAttributeListener) {
                listener.onUserAttributesReceived(
                    mapOf("noTargeting" to "true"),
                    emptyMap(),
                    1L,
                )
            }
            mapOf("noTargeting" to "true")
        }
        val capturedAttributes = slot<Map<String, String>>()

        selectPlacements(emptyMap())

        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
        verify(exactly = 0) { user.removeUserAttribute("\$NoTargeting") }
        verify(exactly = 1) {
            roktListener.selectPlacements(
                any(),
                capture(capturedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertEquals("true", capturedAttributes.captured["noTargeting"])
    }

    @Test
    fun selectPlacements_whenIdentifyCompletes_clearsOverriddenPrivacyValueAndSyncsNoTargetingOnIdentifiedUser() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val identifiedUser = identifiedUser(mutableMapOf("NoTargeting" to "false"))

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        completeIdentify(0, identifiedUser)

        verify(exactly = 1) { identifiedUser.removeUserAttribute("NoTargeting") }
        verify(exactly = 1) { identifiedUser.setUserAttribute("\$NoTargeting", true) }
    }

    @Test
    fun selectPlacements_whenOlderIdentifyCompletesAfterNewerOneForSameUser_skipsStaleIdentifiedUserSync() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>()
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "false"))
        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        completeIdentify(1, identifiedUser)
        completeIdentify(0, identifiedUser)

        assertEquals(true, stored["\$NoTargeting"])
    }

    @Test
    fun selectPlacements_whenCallWithoutEmailRunsBeforeIdentifyCompletes_stillSyncsIdentifiedUser() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>()
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        selectPlacements(emptyMap())
        completeIdentify(0, identifiedUser)

        assertEquals(true, stored["\$NoTargeting"])
    }

    @Test
    fun selectPlacements_whenIdentifyKeepsTheCurrentUserAndANewerCallRan_skipsStaleSync() {
        userIdentities[IdentityType.Email] = "old@example.com"

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        selectPlacements(mapOf("noTargeting" to "false"))
        completeIdentify(0, user)

        // Only the first call's own preparation sets it; the stale identify callback does not.
        verify(exactly = 1) { user.setUserAttribute("\$NoTargeting", true) }
    }

    @Test
    fun selectPlacements_whenCallWithoutPrivacyRunsForIdentifiedUserFirst_newerCallDecidesNoTargeting() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>()
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        // The SDK notifies identity listeners of the new user before identify's success listener.
        every { identityApi.currentUser } returns identifiedUser
        selectPlacements(emptyMap())
        completeIdentify(0, identifiedUser)

        // That newer call's sync wrote $NoTargeting for this user, so the older identify leaves it alone.
        assertFalse(stored.containsKey("\$NoTargeting"))
    }

    @Test
    fun selectPlacements_whenNewerOptOutRanForIdentifiedUser_skipsOlderCallWithoutPrivacy() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val identifiedUser = identifiedUser(mutableMapOf("noTargeting" to "false"))
        // Removals stay queued, as they are in the SDK.
        every { identifiedUser.removeUserAttribute(any()) } returns true

        selectPlacements(mapOf("email" to "new@example.com"))
        every { identityApi.currentUser } returns identifiedUser
        selectPlacements(mapOf("noTargeting" to "true"))
        completeIdentify(0, identifiedUser)

        verify(exactly = 0) { identifiedUser.removeUserAttribute("\$NoTargeting") }
    }

    @Test
    fun selectPlacements_whenNewerCallForTheIdentifiedUserPassesOnlyNoFunctional_newerSyncDecidesNoTargeting() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>()
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        every { identityApi.currentUser } returns identifiedUser
        selectPlacements(mapOf("noFunctional" to "true"))
        completeIdentify(0, identifiedUser)

        // Every placement sync now writes $NoTargeting, so the newer call for this user decides it.
        assertFalse(stored.containsKey("\$NoTargeting"))
    }

    @Test
    fun selectPlacements_whenNewerCallClearsTheTag_olderIdentifyDoesNotRestoreIt() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>("\$NoTargeting" to true)
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true"))
        every { identityApi.currentUser } returns identifiedUser
        selectPlacements(mapOf("shippingcountry" to "US"))
        assertFalse(stored.containsKey("\$NoTargeting"))
        completeIdentify(0, identifiedUser)

        assertFalse(stored.containsKey("\$NoTargeting"))
    }

    @Test
    fun selectPlacements_whenNewerCallPassesOneKey_identifySyncSkipsThatKeyAndNoTargeting() {
        userIdentities[IdentityType.Email] = "old@example.com"
        val stored = mutableMapOf<String, Any?>("noFunctional" to "false")
        val identifiedUser = identifiedUser(stored)

        selectPlacements(mapOf("email" to "new@example.com", "noTargeting" to "true", "noFunctional" to "true"))
        every { identityApi.currentUser } returns identifiedUser
        selectPlacements(mapOf("noFunctional" to "false"))
        completeIdentify(0, identifiedUser)

        // noTargeting is skipped too: the newer call's sync wrote $NoTargeting for this user.
        assertFalse(stored.containsKey("\$NoTargeting"))
        assertEquals("false", stored["noFunctional"])
    }

    @Test
    fun selectPlacements_doesNotPersistScreenViewIdInUserAttributes() {
        val persistedAttributes = slot<Map<String, Any>>()
        val forwardedAttributes = slot<Map<String, String>>()
        every { user.setUserAttributes(capture(persistedAttributes)) } returns true
        every {
            roktListener.selectPlacements(
                any(),
                capture(forwardedAttributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit

        selectPlacements(mapOf("screen_view_id" to "caller-sv-123", "cart_value" to "42.00"))

        assertFalse(persistedAttributes.captured.containsKey("screen_view_id"))
        assertEquals("42.00", persistedAttributes.captured["cart_value"])
        assertEquals("caller-sv-123", forwardedAttributes.captured["screen_view_id"])
    }

    // What the kit's identity callbacks receive: a filtered, read-only view of the user.
    private fun readOnlyViewOf(user: MParticleUser): MParticleUser {
        val view = mockk<MParticleUser>(relaxed = true)
        every { view.id } returns user.id
        every { view.userAttributes } returns emptyMap()
        every { view.setUserAttribute(any(), any()) } returns false
        every { view.removeUserAttribute(any()) } returns false
        return view
    }

    private fun currentUserWith(id: Long, attributes: Map<String, Any?>): MParticleUser {
        val newUser = mockk<MParticleUser>(relaxed = true)
        every { newUser.id } returns id
        every { newUser.userAttributes } returns attributes
        every { newUser.userIdentities } returns HashMap()
        every { identityApi.currentUser } returns newUser
        every { identityApi.getUser(any()) } returns newUser
        return newUser
    }

    @Test
    fun selectPlacements_whenNoSourceSaysTrue_removesStaleNoTargeting() {
        val capturedAttributes = slot<Map<String, String>>()
        every { user.userAttributes } returns mapOf("\$NoTargeting" to true)
        every {
            roktListener.selectPlacements(any(), capture(capturedAttributes), any(), any(), any(), any(), any())
        } returns Unit

        selectPlacements(mapOf("shippingcountry" to "US"))

        verify(exactly = 1) { user.removeUserAttribute("\$NoTargeting") }
        assertFalse(capturedAttributes.captured.containsKey("\$NoTargeting"))
    }

    // The stored profile below never changes, as when core has not yet applied the kit's queued write.
    @Test
    fun selectPlacements_optOutAfterAQueuedRemoval_stillSetsTag() {
        val currentUser = currentUserWith(508L, mapOf("\$NoTargeting" to true))

        selectPlacements(mapOf("shippingcountry" to "US"))
        verify(exactly = 1) { currentUser.removeUserAttribute("\$NoTargeting") }
        selectPlacements(mapOf("noTargeting" to "true"))

        verify(exactly = 1) { currentUser.setUserAttribute("\$NoTargeting", true) }
    }

    @Test
    fun selectPlacements_removalAfterAQueuedSet_stillRemovesTag() {
        val currentUser = currentUserWith(509L, emptyMap())

        selectPlacements(mapOf("noTargeting" to "true"))
        verify(exactly = 1) { currentUser.setUserAttribute("\$NoTargeting", true) }
        selectPlacements(mapOf("shippingcountry" to "US"))

        verify(exactly = 1) { currentUser.removeUserAttribute("\$NoTargeting") }
    }

    @Test
    fun syncNoTargetingAfterIdentityChange_whenNewUsersProfileOptsOut_setsTag() {
        val newUser = currentUserWith(501L, mapOf("noTargeting" to "true"))

        RoktKitRequestHelper.syncNoTargetingAfterIdentityChange(readOnlyViewOf(newUser))

        verify(exactly = 1) { newUser.setUserAttribute("\$NoTargeting", true) }
    }

    @Test
    fun syncNoTargetingAfterIdentityChange_whenNoSourceSaysTrue_removesHandSetTag() {
        val newUser = currentUserWith(502L, mapOf("\$NoTargeting" to true))

        RoktKitRequestHelper.syncNoTargetingAfterIdentityChange(readOnlyViewOf(newUser))

        verify(exactly = 1) { newUser.removeUserAttribute("\$NoTargeting") }
    }

    @Test
    fun syncNoTargetingAfterIdentityChange_keepsPerCallOptOutFromThatUsersLastPlacement() {
        val optedOutUser = currentUserWith(503L, mapOf("\$NoTargeting" to true))
        selectPlacements(mapOf("noTargeting" to "true"))

        RoktKitRequestHelper.syncNoTargetingAfterIdentityChange(readOnlyViewOf(optedOutUser))

        verify(exactly = 0) { optedOutUser.removeUserAttribute("\$NoTargeting") }
    }

    @Test
    fun syncNoTargetingAfterIdentityChange_toAnotherUser_ignoresPreviousUsersPerCallOptOut() {
        currentUserWith(504L, emptyMap())
        selectPlacements(mapOf("noTargeting" to "true"))
        val nextUser = currentUserWith(505L, mapOf("\$NoTargeting" to true))

        RoktKitRequestHelper.syncNoTargetingAfterIdentityChange(readOnlyViewOf(nextUser))

        verify(exactly = 1) { nextUser.removeUserAttribute("\$NoTargeting") }
    }

    @Test
    fun syncNoTargetingAfterIdentityChange_forAUserWhoIsNoLongerCurrent_changesNothing() {
        val staleUser = mockk<MParticleUser>(relaxed = true)
        every { staleUser.id } returns 506L
        every { staleUser.userAttributes } returns mapOf("\$NoTargeting" to true)
        currentUserWith(507L, emptyMap())

        RoktKitRequestHelper.syncNoTargetingAfterIdentityChange(staleUser)

        verify(exactly = 0) { staleUser.removeUserAttribute(any()) }
        verify(exactly = 0) { staleUser.setUserAttribute(any(), any()) }
    }
}
