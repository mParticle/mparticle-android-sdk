package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.testkits.ListenerTestKit
import com.mparticle.messages.KitConfigMessage
import com.mparticle.testing.context
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Before
import org.junit.Test

class KnownUserKitsLifecycleTest : BaseKitOptionsTest() {
    @Before
    @Throws(JSONException::class)
    fun before() {
        val builder = MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions()
                    .addKit(TestKit1::class.java, KitConfigMessage(-1).apply { excludeAnnonymousUsers = true })
                    .addKit(TestKit2::class.java, KitConfigMessage(-2).apply { excludeAnnonymousUsers = false })
                    .addKit(TestKit3::class.java, KitConfigMessage(-3).apply { excludeAnnonymousUsers = true })
            )
        startMParticle(builder)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testExcludeUnknownUsers() {
        MParticle.getInstance()!!.apply {
            waitForKitReload {
                Internal().configManager.setMpid(123, true)
            }
            TestCase.assertTrue(isKitActive(-1))
            TestCase.assertTrue(isKitActive(-2))
            TestCase.assertTrue(isKitActive(-3))
            waitForKitReload {
                Internal().configManager.setMpid(123, false)
            }
            TestCase.assertFalse(isKitActive(-1))
            TestCase.assertTrue(isKitActive(-2))
            TestCase.assertFalse(isKitActive(-3))
            waitForKitReload {
                Internal().configManager.setMpid(321, false)
            }
            TestCase.assertFalse(isKitActive(-1))
            TestCase.assertTrue(isKitActive(-2))
            TestCase.assertFalse(isKitActive(-3))
            waitForKitReload {
                Internal().configManager.setMpid(123, true)
            }
            TestCase.assertTrue(isKitActive(-1))
            TestCase.assertTrue(isKitActive(-2))
            TestCase.assertTrue(isKitActive(-3))
            waitForKitReload {
                Internal().configManager.setMpid(456, true)
            }
            TestCase.assertTrue(isKitActive(-1))
            TestCase.assertTrue(isKitActive(-2))
            TestCase.assertTrue(isKitActive(-3))
        }
    }

    class TestKit1 : TestKit()
    class TestKit2 : TestKit()
    class TestKit3 : TestKit()
    open class TestKit : ListenerTestKit() {
        override fun getName(): String {
            return "test kit" + i++
        }

        @Throws(IllegalArgumentException::class)
        override fun onKitCreate(
            settings: Map<String, String>?,
            context: Context
        ): List<ReportingMessage> {
            return listOf()
        }

        override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
            return listOf()
        }

        companion object {
            var i = 0
        }
    }
}
