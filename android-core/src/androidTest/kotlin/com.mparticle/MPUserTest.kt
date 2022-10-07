package com.mparticle

import com.mparticle.internal.AccessUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MPUserTest : BaseCleanStartedEachTest() {

    @Test
    fun testGetAttributeSyncWithAndroidHack() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { userAttributes.isEmpty() }
            setUserAttribute("foo", "bar")

            android_test_hack()

            assertEquals(1, userAttributes.size)
            assertEquals("bar", userAttributes["foo"])
        }
    }

    @Test
    fun testGetAttributeAsync() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { getUserAttributes().isEmpty() }
            setUserAttribute("foo", "bar")
            setUserAttribute("fooInt", 123)
            setUserAttribute("fooLong", 12345L)
            setUserAttribute("fooDouble", 10.15)
            setUserAttribute("fooNegInt", -10L)
            setUserAttribute("fooNegLong", -1010L)
            android_test_hack()

            getUserAttributes(object : UserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, String?>?,
                    userAttributeLists: Map<String, List<String?>>?,
                    mpid: Long?
                ) {
                    assertNotNull(userAttributes)
                    assertEquals(6, userAttributes.size)
                    assertEquals("bar", userAttributes["foo"])
                    assertEquals("123", userAttributes["fooInt"])
                    assertEquals("12345", userAttributes["fooLong"])
                    assertEquals("10.15", userAttributes["fooDouble"])
                    assertEquals("-10", userAttributes["fooNegInt"])
                    assertEquals("-1010", userAttributes["fooNegLong"])
                }
            })

            getUserAttributes(object : TypedUserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, Any?>,
                    userAttributeLists: Map<String, List<String?>?>,
                    mpid: Long
                ) {
                    assertEquals(6, userAttributes.size)
                    assertEquals("bar", userAttributes["foo"])
                    assertEquals(123L, userAttributes["fooInt"])
                    assertEquals(12345L, userAttributes["fooLong"])
                    assertEquals(10.15, userAttributes["fooDouble"])
                    assertEquals(-10L, userAttributes["fooNegInt"])
                    assertEquals(-1010L, userAttributes["fooNegLong"])
                }
            })
        }
    }

    @Test
    fun testIncrementIntegerAttribute() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { getUserAttributes().isEmpty() }
            setUserAttribute("foo", 1)

            android_test_hack()
            assertEquals(1, userAttributes.size)
            incrementUserAttribute("foo", 3)

            android_test_hack()
            assertEquals(4L, userAttributes["foo"])

            // test negative increment
            incrementUserAttribute("foo", -2)
            android_test_hack()
            assertEquals(2L, userAttributes["foo"])

            // test remove incremented attribute
            removeUserAttribute("foo")
            android_test_hack()
            assertEquals(0, userAttributes.size)
        }
    }

    @Test
    fun testIncrementDoubleAttribute() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { getUserAttributes().isEmpty() }
            android_test_hack()

            setUserAttribute("foo", 1.5)

            android_test_hack()
            assertEquals(1, userAttributes.size)
            incrementUserAttribute("foo", 3.2)

            android_test_hack()
            assertEquals(4.7, userAttributes["foo"])

            android_test_hack()

            // test negative increment
            incrementUserAttribute("foo", -2.1)
            android_test_hack()
            assertEquals(2.6, userAttributes["foo"])

            // test remove incremented attribute
            removeUserAttribute("foo")
            android_test_hack()
            assertEquals(0, userAttributes.size)
        }
    }

    @Test
    fun testIncrementLongAttribute() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { getUserAttributes().isEmpty() }
            setUserAttribute("foo", 10L)

            android_test_hack()
            assertEquals(1, userAttributes.size)
            assertEquals(10L, userAttributes["foo"])
            incrementUserAttribute("foo", 37L)

            android_test_hack()
            assertEquals(47L, userAttributes["foo"])

            // test negative increment
            incrementUserAttribute("foo", -21L)
            android_test_hack()
            assertEquals(26L, userAttributes["foo"])

            // test remove incremented attribute
            removeUserAttribute("foo")
            android_test_hack()
            assertEquals(0, userAttributes.size)
        }
    }

    @Test
    fun testRemoveUserAttribute() {
        MParticle.getInstance()!!.Identity().currentUser!!.apply {
            assertTrue { userAttributes.isEmpty() }
            setUserAttribute("foo", "bar")
            removeUserAttribute("foo")

            android_test_hack()
            assertEquals(0, userAttributes.size)

            setUserAttribute("foo", "bar")
            setUserAttribute("fuzz", "baz")

            android_test_hack()
            assertEquals(2, userAttributes.size)
            assertEquals("bar", userAttributes["foo"])
            assertEquals("baz", userAttributes["fuzz"])

            // remove just 1
            removeUserAttribute("fuzz")
            android_test_hack()
            assertEquals(1, userAttributes.size)
            assertEquals("bar", userAttributes["foo"])

            // remove last
            removeUserAttribute("foo")
            android_test_hack()
            assertEquals(0, userAttributes.size)
        }
    }

    private fun android_test_hack() {
        // force sync attribute writes to complete for Android
        AccessUtils.awaitMessageHandler()
    }
}
