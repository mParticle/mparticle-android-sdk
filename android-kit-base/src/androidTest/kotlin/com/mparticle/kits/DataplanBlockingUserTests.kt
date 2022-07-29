package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.api.identity.toIdentityType
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.internal.AccessUtil
import com.mparticle.internal.AccessUtils
import com.mparticle.kits.DataplanFilterImpl.Companion.getEventsApiName
import com.mparticle.kits.testkits.AttributeListenerTestKit
import com.mparticle.kits.testkits.IdentityListenerTestKit
import com.mparticle.kits.testkits.ListenerTestKit
import com.mparticle.kits.testkits.UserAttributeListenerTestKit
import com.mparticle.messages.IdentityResponseMessage
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.RandomUtils.getAlphaString
import com.mparticle.testing.RandomUtils.randomAttributes
import com.mparticle.testing.RandomUtils.randomEventType
import com.mparticle.testing.RandomUtils.randomIdentities
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class DataplanBlockingUserTests : BaseKitOptionsTest() {
    lateinit var attributeListenerKitKit: AttributeListenerTestKit
    lateinit var identityListenerKitKit: IdentityListenerTestKit
    lateinit var userAttributeListenerKitKit: UserAttributeListenerTestKit

    lateinit var kitIntegrationTestKits: List<ListenerTestKit>

    @Before
    fun beforeTests() {
        MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions {
                    addKit(AttributeListenerTestKit::class.java, -1)
                    addKit(IdentityListenerTestKit::class.java, -2)
                    addKit(UserAttributeListenerTestKit::class.java, -3)
                }
            ).let {
                startMParticle(it)
            }
        attributeListenerKitKit = MParticle.getInstance()?.getKitInstance(-1) as AttributeListenerTestKit
        identityListenerKitKit = MParticle.getInstance()?.getKitInstance(-2) as IdentityListenerTestKit
        userAttributeListenerKitKit = MParticle.getInstance()?.getKitInstance(-3) as UserAttributeListenerTestKit
        kitIntegrationTestKits = listOf(attributeListenerKitKit, identityListenerKitKit, userAttributeListenerKitKit)
        assertTrue(randomAttributes().isNotEmpty())
        assertTrue(randomIdentities().isNotEmpty())
    }

    @Test
    fun testAttributesBlocked() {
        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes()
        val blockedAttributes = randomAttributes().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        AccessUtil.kitManager().setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, Random.nextBoolean()))

        userAttributeListenerKitKit.onSetUserAttribute = { key, value, user ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
        }
        attributeListenerKitKit.setUserAttribute = { key, value ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
        }
        MParticle.getInstance()?.Identity()?.currentUser?.userAttributes = allowedAttributes + blockedAttributes
        AccessUtils.awaitMessageHandler()

        kitIntegrationTestKits.forEach { kit ->
            assertEquals(kit.name, allowedAttributes, kit.allUserAttributes)
        }
        // sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedAttributes + blockedAttributes, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes)
    }

    @Test
    fun testRemoveAttributeBlocked() {
        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes()
        val blockedAttributes = randomAttributes().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        AccessUtil.kitManager().setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, false))

        kitIntegrationTestKits.forEach { kit ->
            kit.onAttributeReceived = { key, value ->
                assertTrue(allowedAttributes.containsKey(key))
                assertFalse(blockedAttributes.containsKey(key))
            }
        }
        (allowedAttributes + blockedAttributes).entries.forEach { MParticle.getInstance()?.Identity()?.currentUser?.setUserAttribute(it.key, it.value) }
        AccessUtils.awaitMessageHandler()

        var count = 0
        kitIntegrationTestKits.forEach {
            it.onAttributeReceived = { key, value ->
                assertTrue(allowedAttributes.containsKey(key))
                assertFalse(blockedAttributes.containsKey(key))
                assertNull(value)
                // make sure these are the attributes that are being removed
                count++
            }
        }
        userAttributeListenerKitKit.onRemoveUserAttribute = { key, user ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
            // make sure these are the attributes that are being removed
            count++
        }
        attributeListenerKitKit.removeUserAttribute = {
            assertTrue(allowedAttributes.containsKey(it))
            assertFalse(blockedAttributes.containsKey(it))
            count++
        }
        (allowedAttributes + blockedAttributes).keys.forEach { MParticle.getInstance()?.Identity()?.currentUser?.removeUserAttribute(it) }

        MParticle.getInstance()?.Identity()?.currentUser?.userAttributes = mapOf()
        AccessUtils.awaitMessageHandler()
        assertEquals(count, allowedAttributes.size * 4)

        kitIntegrationTestKits.forEach {
            assertEquals(0, it.allUserAttributes.size)
        }
        // sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(0, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes?.size)
    }

    @Test
    fun testAttributeListsBlocked() {
        attributeListenerKitKit.supportsAttributeLists = { true }
        userAttributeListenerKitKit.supportsAttributeLists = { true }

        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes().map { it.key to listOf(it.value) }.toMap()
        val blockedAttributes = randomAttributes().map { it.key to listOf(it.value) }.toMap().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        AccessUtil.kitManager().setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, Random.nextBoolean()))

        var count = 0
        kitIntegrationTestKits.forEach { kit ->
            kit.onAttributeReceived = { key, value ->
                assertTrue(allowedAttributes.containsKey(key))
                assertFalse(blockedAttributes.containsKey(key))
                count++
            }
        }
        userAttributeListenerKitKit.onSetUserAttributeList = { key, value, user ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
            count++
        }
        attributeListenerKitKit.setUserAttributeList = { key, value ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
            count++
        }
        MParticle.getInstance()?.Identity()?.currentUser?.userAttributes = allowedAttributes + blockedAttributes
        AccessUtils.awaitMessageHandler()

        assertEquals(count, allowedAttributes.size * 4)
        kitIntegrationTestKits.forEach { kit ->
            assertEquals(allowedAttributes.size, kit.allUserAttributes.size)
            assertEquals(allowedAttributes.keys, kit.allUserAttributes.keys)
        }
        // sanity check to make sure the non-filtered User has the blocked attributes
        assertEquals(allowedAttributes + blockedAttributes, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes)
    }

    @Test
    fun testBlockingInFilteredIdentityApiRequest() {
        val datapoints = getRandomDataplanPoints()
        val allowedIdentities = randomAndroidIdentities()
        val blockIdentities = randomAndroidIdentities().filterKeys { !allowedIdentities.containsKey(it) }

        datapoints[DataplanFilterImpl.USER_IDENTITIES_KEY] = allowedIdentities.keys.map { it.getEventsApiName() }.toHashSet()
        AccessUtil.kitManager().setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), Random.nextBoolean(), true))

        MParticle.getInstance()?.Identity()?.login(
            IdentityApiRequest.withEmptyUser()
                .userIdentities(blockIdentities + allowedIdentities)
                .build()
        )
        val latch = FailureLatch()
        kitIntegrationTestKits.forEach { kit ->
            kit.onIdentityReceived = { identityType, value ->
                assertTrue(allowedIdentities.containsKey(identityType))
                assertFalse(blockIdentities.containsKey(identityType))
            }
        }
        identityListenerKitKit.onLoginCompleted = { user, request ->
            assertEquals(allowedIdentities, request?.userIdentities)
            latch.countDown()
        }
        latch.await()
        kitIntegrationTestKits.forEach { kit ->
            assertEquals(allowedIdentities, kit.userIdentities)
        }
        // sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedIdentities + blockIdentities, MParticle.getInstance()?.Identity()?.currentUser?.userIdentities)
    }

    @Test
    fun testBlockingInFilteredMParticleUser() {
        val datapoints = getRandomDataplanPoints()
        val allowedIdentities = randomAndroidIdentities()
        val blockedIdentities = randomAndroidIdentities().filterKeys { !allowedIdentities.containsKey(it) }
        assertTrue(blockedIdentities.size > 0)

        datapoints[DataplanFilterImpl.USER_IDENTITIES_KEY] = allowedIdentities.keys.map { it.getEventsApiName() }.toHashSet()
        AccessUtil.kitManager().setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), Random.nextBoolean(), true))

        Server
            .endpoint(EndpointType.Identity_Login)
            .addResponseLogic {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(Random.Default.nextLong())
                }
            }

        MParticle.getInstance()?.Identity()?.login(
            IdentityApiRequest.withEmptyUser()
                .userIdentities(blockedIdentities + allowedIdentities)
                .build()
        )
        val latch = FailureLatch()
        kitIntegrationTestKits.forEach { kit ->
            kit.onUserReceived = {
                assertEquals(allowedIdentities, it?.userIdentities)
            }
        }
        identityListenerKitKit.onLoginCompleted = { user, request ->
            assertEquals(allowedIdentities, request?.userIdentities)
            assertEquals(allowedIdentities, user?.userIdentities)
            latch.countDown()
        }
        latch.await()

        // sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedIdentities + blockedIdentities, MParticle.getInstance()?.Identity()?.currentUser?.userIdentities)
    }

    internal fun getRandomDataplanEventKey(): DataplanFilterImpl.DataPoint {
        return when (Random.Default.nextInt(0, 5)) {
            0 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.CUSTOM_EVENT_KEY, getAlphaString(5), randomEventType().ordinal.toString())
            1 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.SCREEN_EVENT_KEY, getAlphaString(8))
            2 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.PRODUCT_ACTION_KEY, Product.DETAIL)
            3 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.PROMOTION_ACTION_KEY, Promotion.VIEW)
            4 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.PRODUCT_IMPRESSION_KEY)
            else -> throw IllegalArgumentException("messed this implementation up :/")
        }
    }

    fun getRandomDataplanPoints(): MutableMap<String, HashSet<String>> {
        return (0..Random.Default.nextInt(0, 10))
            .associate {
                getRandomDataplanEventKey().toString() to randomAttributes().keys.toHashSet()
            }
            .toMutableMap()
    }

    private fun randomAndroidIdentities(): Map<MParticle.IdentityType, String> {
        return randomIdentities().entries.associate { it.key.toIdentityType() to it.value }
    }
}
