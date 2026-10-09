package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleTask
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.IdentityHttpResponse
import com.mparticle.identity.MParticleUser
import com.mparticle.identity.TaskFailureListener
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
        return task
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
}
