package com.mparticle.kits

import android.content.Context
import android.util.SparseBooleanArray
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.internal.MPUtility
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.mock.MockMParticle
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.LinkedList

class KitIntegrationTest {
    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributesWithoutLists() {
        MParticle.setInstance(MockMParticle())
        val integration: KitIntegration = object : KitIntegration() {
            override fun getName(): String? {
                return null
            }

            override fun onKitCreate(
                settings: Map<String, String>,
                context: Context
            ): List<ReportingMessage>? {
                return null
            }

            override fun setOptOut(optedOut: Boolean): List<ReportingMessage>? {
                return null
            }
        }
        integration.configuration = Mockito.mock(KitConfiguration::class.java)
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["key 1"] = "value 1"
        val list: MutableList<String> = LinkedList()
        list.add("value 2")
        list.add("value 3")
        attributes["key 2"] = list
        Mockito.`when`(
            MParticle.getInstance()!!.Identity().currentUser!!.userAttributes
        ).thenReturn(attributes)
        val filteredAttributes = integration.allUserAttributes
        Assert.assertEquals("value 1", filteredAttributes["key 1"])
        Assert.assertEquals("value 2,value 3", filteredAttributes["key 2"])
    }

    internal inner class MockSparseBooleanArray : SparseBooleanArray() {
        override fun get(key: Int): Boolean {
            return get(key, false)
        }

        override fun get(key: Int, valueIfKeyNotFound: Boolean): Boolean {
            print("SparseArray getting: $key")
            return if (map.containsKey(key)) {
                map[key]!!
            } else {
                valueIfKeyNotFound
            }
        }

        var map: MutableMap<Int, Boolean> = HashMap()
        override fun put(key: Int, value: Boolean) {
            map[key] = value
        }

        override fun clear() {
            map.clear()
        }

        override fun size(): Int {
            return map.size
        }

        override fun toString(): String {
            return map.toString()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributesWithoutListsWithFilters() {
        MParticle.setInstance(MockMParticle())
        val integration: KitIntegration = object : KitIntegration() {
            override fun getName(): String? {
                return null
            }

            override fun onKitCreate(
                settings: Map<String, String>,
                context: Context
            ): List<ReportingMessage>? {
                return null
            }

            override fun setOptOut(optedOut: Boolean): List<ReportingMessage>? {
                return null
            }
        }
        val configuration = Mockito.mock(
            KitConfiguration::class.java
        )
        val mockArray = MockSparseBooleanArray()
        mockArray.put(MPUtility.mpHash("key 4"), false)
        mockArray.put(MPUtility.mpHash("key 3"), false)
        Mockito.`when`(configuration.userAttributeFilters).thenReturn(mockArray)
        integration.configuration = configuration
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["key 1"] = "value 1"
        attributes["key 4"] = "value 4"
        val list: MutableList<String> = LinkedList()
        list.add("value 2")
        list.add("value 3")
        attributes["key 2"] = list
        attributes["key 3"] = list
        Mockito.`when`(
            MParticle.getInstance()!!.Identity().currentUser!!.userAttributes
        ).thenReturn(attributes)
        val filteredAttributes = integration.allUserAttributes
        Assert.assertEquals("value 1", filteredAttributes["key 1"])
        Assert.assertEquals("value 2,value 3", filteredAttributes["key 2"])
        Assert.assertNull(filteredAttributes["key 3"])
        Assert.assertNull(filteredAttributes["key 4"])
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributesWithLists() {
        MParticle.setInstance(MockMParticle())
        val integration: KitIntegration = AttributeListenerIntegration()
        integration.configuration = Mockito.mock(KitConfiguration::class.java)
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["key 1"] = "value 1"
        val list: MutableList<String> = LinkedList()
        list.add("value 2")
        list.add("value 3")
        attributes["key 2"] = list
        Mockito.`when`(
            MParticle.getInstance()!!.Identity().currentUser!!.userAttributes
        ).thenReturn(attributes)
        val filteredAttributes = integration.allUserAttributes
        Assert.assertEquals("value 1", filteredAttributes["key 1"])
        Assert.assertEquals(list, filteredAttributes["key 2"])
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributesWithListsAndFilters() {
        MParticle.setInstance(MockMParticle())
        val integration: KitIntegration = AttributeListenerIntegration()
        val configuration = Mockito.mock(
            KitConfiguration::class.java
        )
        val mockArray = MockSparseBooleanArray()
        mockArray.put(MPUtility.mpHash("key 4"), false)
        mockArray.put(MPUtility.mpHash("key 3"), false)
        Mockito.`when`(configuration.userAttributeFilters).thenReturn(mockArray)
        integration.configuration = configuration
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["key 1"] = "value 1"
        attributes["key 4"] = "value 4"
        val list: MutableList<String> = LinkedList()
        list.add("value 2")
        list.add("value 3")
        attributes["key 2"] = list
        attributes["key 3"] = list
        Mockito.`when`(
            MParticle.getInstance()!!.Identity().currentUser!!.userAttributes
        ).thenReturn(attributes)
        val filteredAttributes = integration.allUserAttributes
        Assert.assertEquals("value 1", filteredAttributes["key 1"])
        Assert.assertEquals(list, filteredAttributes["key 2"])
        Assert.assertNull(filteredAttributes["key 3"])
        Assert.assertNull(filteredAttributes["key 4"])
    }

    private inner class AttributeListenerIntegration : KitIntegration(), AttributeListener {
        override fun setUserAttribute(attributeKey: String, attributeValue: String) {}
        override fun setUserAttributeList(attributeKey: String, attributeValueList: List<String>) {}
        override fun supportsAttributeLists(): Boolean {
            return true
        }

        override fun setAllUserAttributes(
            userAttributes: Map<String, String>,
            userAttributeLists: Map<String, List<String>>
        ) {
        }

        override fun removeUserAttribute(key: String) {}
        override fun setUserIdentity(identityType: IdentityType, identity: String) {}
        override fun removeUserIdentity(identityType: IdentityType) {}
        override fun logout(): List<ReportingMessage>? {
            return null
        }

        override fun getName(): String? {
            return null
        }

        override fun onKitCreate(
            settings: Map<String, String>,
            context: Context
        ): List<ReportingMessage>? {
            return null
        }

        override fun setOptOut(optedOut: Boolean): List<ReportingMessage>? {
            return null
        }
    }
}
