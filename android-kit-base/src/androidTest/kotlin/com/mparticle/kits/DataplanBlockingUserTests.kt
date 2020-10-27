package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.Utils.randomAttributes
import com.mparticle.Utils.randomEventType
import com.mparticle.Utils.randomIdentities
import com.mparticle.Utils.randomProductAction
import com.mparticle.Utils.randomPromotionAction
import com.mparticle.Utils.randomString
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.internal.AccessUtils
import com.mparticle.kits.DataplanFilterImpl.Companion.getEventsApiName
import com.mparticle.kits.testkits.AttributeListenerTestKit
import com.mparticle.kits.testkits.IdentityListenerTestKit
import com.mparticle.kits.testkits.KitIntegrationTestKit
import com.mparticle.kits.testkits.UserAttributeListenerTestKit
import com.mparticle.testutils.MPLatch
import junit.framework.Assert.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class DataplanBlockingUserTests: BaseKitManagerStarted() {
    lateinit var attributeListenerKitKit: AttributeListenerTestKit
    lateinit var identityListenerKitKit: IdentityListenerTestKit
    lateinit var userAttributeListenerKitKit: UserAttributeListenerTestKit

    lateinit var kitIntegrationTestKits: List<KitIntegrationTestKit>

    override fun registerCustomKits(): Map<String, JSONObject> {
        return mapOf(
                AttributeListenerTestKit::class.java.name to JSONObject(),
                IdentityListenerTestKit::class.java.name to JSONObject(),
                UserAttributeListenerTestKit::class.java.name to JSONObject(),
        )
    }

    @Before
    fun beforeTests() {
        attributeListenerKitKit = MParticle.getInstance()?.getKitInstance(-1) as AttributeListenerTestKit
        identityListenerKitKit = MParticle.getInstance()?.getKitInstance(-2) as IdentityListenerTestKit
        userAttributeListenerKitKit = MParticle.getInstance()?.getKitInstance(-3) as UserAttributeListenerTestKit
        kitIntegrationTestKits = listOf(attributeListenerKitKit, identityListenerKitKit, userAttributeListenerKitKit)
        assertTrue(randomAttributes().size > 0)
        assertTrue(randomIdentities().size > 0)
    }

    @Test
    fun testAttributesBlocked() {
        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes()
        val blockedAttributes = randomAttributes().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        mKitManager.setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, Random.nextBoolean()))

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
        //sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedAttributes + blockedAttributes, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes)
    }

    @Test
    fun testRemoveAttributeBlocked() {
        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes()
        val blockedAttributes = randomAttributes().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        mKitManager.setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, false))

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
                //make sure these are the attributes that are being removed
                count++;
            }
        }
        userAttributeListenerKitKit.onRemoveUserAttribute = { key, user ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
            //make sure these are the attributes that are being removed
            count++;
        }
        attributeListenerKitKit.removeUserAttribute = {
            assertTrue(allowedAttributes.containsKey(it))
            assertFalse(blockedAttributes.containsKey(it))
            count++;
        }
        (allowedAttributes + blockedAttributes).keys.forEach { MParticle.getInstance()?.Identity()?.currentUser?.removeUserAttribute(it) }

        MParticle.getInstance()?.Identity()?.currentUser?.userAttributes = mapOf()
        AccessUtils.awaitMessageHandler()
        Thread.sleep(1000)
        assertEquals(count, allowedAttributes.size * 4)

        kitIntegrationTestKits.forEach {
            assertEquals(0, it.allUserAttributes.size)
        }
        //sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(0, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes?.size)
    }

    @Test
    fun testAttributeListsBlocked() {
        attributeListenerKitKit.supportsAttributeLists = {true}
        userAttributeListenerKitKit.supportsAttributeLists = {true}

        val datapoints = getRandomDataplanPoints()
        val allowedAttributes = randomAttributes().map { it.key to listOf(it.value) }.toMap()
        val blockedAttributes = randomAttributes().map { it.key to listOf(it.value) }.toMap().filterKeys { !allowedAttributes.containsKey(it) }
        assertTrue(blockedAttributes.size > 0)

        datapoints[DataplanFilterImpl.USER_ATTRIBUTES_KEY] = allowedAttributes.keys.toHashSet()
        mKitManager.setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), true, Random.nextBoolean()))

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
            count++;
        }
        attributeListenerKitKit.setUserAttributeList = { key, value ->
            assertTrue(allowedAttributes.containsKey(key))
            assertFalse(blockedAttributes.containsKey(key))
            count++;
        }
        MParticle.getInstance()?.Identity()?.currentUser?.userAttributes = allowedAttributes + blockedAttributes
        AccessUtils.awaitMessageHandler()

        assertEquals(count, allowedAttributes.size * 4)
        kitIntegrationTestKits.forEach { kit ->
            assertEquals(allowedAttributes.size, kit.allUserAttributes.size)
            assertEquals(allowedAttributes.keys, kit.allUserAttributes.keys)
        }
        //sanity check to make sure the non-filtered User has the blocked attributes
        assertEquals(allowedAttributes + blockedAttributes, MParticle.getInstance()?.Identity()?.currentUser?.userAttributes)
    }

    @Test
    fun testBlockingInFilteredIdentityApiRequest() {
        val datapoints = getRandomDataplanPoints()
        val allowedIdentities = randomIdentities()
        val blockIdentities = randomIdentities().filterKeys { !allowedIdentities.containsKey(it) }
        assertTrue(blockIdentities.size > 0)

        datapoints[DataplanFilterImpl.USER_IDENTITIES_KEY] = allowedIdentities.keys.map { it.getEventsApiName() }.toHashSet()
        mKitManager.setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), Random.nextBoolean(), true))


        MParticle.getInstance()?.Identity()?.login(IdentityApiRequest.withEmptyUser()
                .userIdentities(blockIdentities + allowedIdentities)
                .build())
        val latch = MPLatch(1)
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
        //sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedIdentities + blockIdentities, MParticle.getInstance()?.Identity()?.currentUser?.userIdentities)
    }

    @Test
    fun testBlockingInFilteredMParticleUser() {
        val datapoints = getRandomDataplanPoints()
        val allowedIdentities = randomIdentities()
        val blockedIdentities = randomIdentities().filterKeys { !allowedIdentities.containsKey(it) }
        assertTrue(blockedIdentities.size > 0)

        datapoints[DataplanFilterImpl.USER_IDENTITIES_KEY] = allowedIdentities.keys.map { it.getEventsApiName() }.toHashSet()
        mKitManager.setDataplanFilter(DataplanFilterImpl(datapoints, Random.nextBoolean(), Random.nextBoolean(), Random.nextBoolean(), true))

        mServer.addConditionalLoginResponse(mStartingMpid, Random.Default.nextLong())
        MParticle.getInstance()?.Identity()?.login(IdentityApiRequest.withEmptyUser()
                .userIdentities(blockedIdentities + allowedIdentities)
                .build())
        val latch = MPLatch(1)
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

        //sanity check to make sure the non-filtered User has the blocked identities
        assertEquals(allowedIdentities + blockedIdentities, MParticle.getInstance()?.Identity()?.currentUser?.userIdentities)
    }

    internal fun getRandomDataplanEventKey(): DataplanFilterImpl.DataPoint {
        return when (Random.Default.nextInt(0, 5)) {
            0 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.CUSTOM_EVENT_KEY, randomString(5), randomEventType().ordinal.toString())
            1 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.SCREEN_EVENT_KEY, randomString(8))
            2 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.PRODUCT_ACTION_KEY, randomProductAction())
            3 -> DataplanFilterImpl.DataPoint(DataplanFilterImpl.PROMOTION_ACTION_KEY, randomPromotionAction())
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
}