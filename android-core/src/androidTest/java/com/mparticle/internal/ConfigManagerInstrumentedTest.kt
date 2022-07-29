package com.mparticle.internal

import com.mparticle.Configuration
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager.ConfigLoadedListener
import com.mparticle.internal.ConfigManager.ConfigType
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.KitConfigMessage
import com.mparticle.messages.toJsonString
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.TestingUtils
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.ErrorResponse
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import com.mparticle.utils.startMParticle
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.util.Arrays
import kotlin.random.Random

class ConfigManagerInstrumentedTest : BaseTest() {
    fun simpleConfigWithKits() = ConfigResponseMessage(
        id = "12345",
        kits = listOf(
            KitConfigMessage(
                id = 1
            )
        )
    )

    @Test
    @Throws(InterruptedException::class)
    fun testSetMpidCurrentUserState() {
        val mpid1: Long = Random.Default.nextLong()
        val mpid2: Long = Random.Default.nextLong()
        val mpid3: Long = Random.Default.nextLong()
        startMParticle()
        val configManager = MParticle.getInstance()!!.Internal().configManager
        Assert.assertEquals(
            mStartingMpid,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        Assert.assertEquals(mStartingMpid, configManager.mpid)
        configManager.setMpid(mpid1, Random.Default.nextBoolean())
        TestCase.assertEquals(
            mpid1,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        val newIsLoggedIn = !MParticle.getInstance()!!
            .Identity().currentUser!!.isLoggedIn
        configManager.setMpid(mpid1, newIsLoggedIn)
        TestCase.assertEquals(
            mpid1,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        TestCase.assertEquals(
            newIsLoggedIn,
            MParticle.getInstance()!!
                .Identity().currentUser!!.isLoggedIn
        )
        configManager.setMpid(mpid2, false)
        TestCase.assertEquals(
            mpid2,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        TestCase.assertFalse(
            MParticle.getInstance()!!.Identity().currentUser!!.isLoggedIn
        )
        configManager.setMpid(mpid2, true)
        TestCase.assertEquals(
            mpid2,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        TestCase.assertTrue(
            MParticle.getInstance()!!.Identity().currentUser!!.isLoggedIn
        )
        configManager.setMpid(mpid3, true)
        TestCase.assertEquals(
            mpid3,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        TestCase.assertTrue(
            MParticle.getInstance()!!.Identity().currentUser!!.isLoggedIn
        )
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testConfigResponseParsing() {
        val token: String = RandomUtils.getAlphaNumericString(20)
        val aliasMaxWindow: Int = Random.Default.nextInt()
        val config = ConfigResponseMessage(
            workspaceToken = token,
            aliasMaxWindow = aliasMaxWindow
        )
        Server
            .endpoint(EndpointType.Config)
            .nextResponse {
                SuccessResponse {
                    responseObject = config
                }
            }

        var configLoadedListener = BothConfigsLoadedListener()
        var latch = configLoadedListener.latch
        startMParticle(
            MParticleOptions.builder(context)
                .configuration(AddConfigListener(configLoadedListener))
        )
        latch.await()
        TestCase.assertEquals(
            token,
            MParticle.getInstance()!!.Internal().configManager.workspaceToken
        )
        TestCase.assertEquals(
            aliasMaxWindow,
            MParticle.getInstance()!!
                .Internal().configManager.aliasMaxWindow
        )

        // test set defaults when fields are not present
        MParticle.setInstance(null)
        Server
            .endpoint(EndpointType.Config)
            .nextResponse {
                SuccessResponse { responseObject = ConfigResponseMessage() }
            }
        configLoadedListener = BothConfigsLoadedListener()
        latch = configLoadedListener.latch
        startMParticle(
            MParticleOptions.builder(context)
                .configuration(AddConfigListener(configLoadedListener))
        )
        latch.await()
        TestCase.assertEquals("", MParticle.getInstance()!!.Internal().configManager.workspaceToken)
        TestCase.assertEquals(90, MParticle.getInstance()!!.Internal().configManager.aliasMaxWindow)
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun cachedConfigLoadedExactlyOnce() {
        val latch = FailureLatch()
        val loadedCoreLocal = Mutable(false)
        val loadedKitLocal = Mutable(false)
        ConfigManager.getInstance(context)
            .saveConfigJson(
                JSONObject(simpleConfigWithKits().toJsonString()), null, null, System.currentTimeMillis()
            )
        Server
            .endpoint(EndpointType.Config)
            .nextResponse { ErrorResponse(304) }

        val configLoadedListener: ConfigLoadedListener = object : ConfigLoadedListener {
            override fun onConfigUpdated(configType: ConfigType, isNew: Boolean) {
                if (!isNew) {
                    when (configType) {
                        ConfigType.CORE -> {
                            if (loadedCoreLocal.value) {
                                Assert.fail("core config already loaded")
                            } else {
                                Logger.error("LOADED CACHED Core")
                                loadedCoreLocal.value = true
                            }
                            if (loadedKitLocal.value) {
                                Assert.fail("kit config already loaded")
                            } else {
                                Logger.error("LOADED CACHED Kit")
                                loadedKitLocal.value = true
                            }
                        }
                        ConfigType.KIT -> if (loadedKitLocal.value) {
                            Assert.fail("kit config already loaded")
                        } else {
                            Logger.error("LOADED CACHED Kit")
                            loadedKitLocal.value = true
                        }
                    }
                }
                if (loadedCoreLocal.value && loadedKitLocal.value) {
                    latch.countDown()
                }
                Logger.error("KIT = " + loadedKitLocal.value.toString() + " Core: " + loadedCoreLocal.value)
            }
        }
        val options = MParticleOptions.builder(context)
            .credentials("key", "secret")
            .configuration(AddConfigListener(configLoadedListener))
            .build()
        MParticle.start(options)

        // wait until both local configs are loaded
        latch.await()

        // try to coerce another load...
        ConfigManager(context)
        val instance = MParticle.getInstance()
        instance!!.logEvent(TestingUtils.randomMPEventRich.event())
        instance.optOut = true
        instance.optOut = false

        // and finally, load remote config
        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse { responseObject = simpleConfigWithKits() } }
        AccessUtils.fetchConfig()

        val bothConfigsLoadedListener = BothConfigsLoadedListener()
        val reloadLatch = bothConfigsLoadedListener.latch
        MParticle.getInstance()!!.Internal().configManager.addConfigUpdatedListener(
            bothConfigsLoadedListener
        )
        reloadLatch.await()
    }

    internal inner class BothConfigsLoadedListener(vararg configTypes: ConfigType?) :
        ConfigLoadedListener {
        var types: MutableSet<ConfigType>
        var latch = FailureLatch()
        override fun onConfigUpdated(configType: ConfigType, isNew: Boolean) {
            if (isNew) {
                types.remove(configType)
            }
            if (types.size == 0) {
                latch.countDown()
            }
        }

        init {
            var configTypes: Array<out ConfigType?> = configTypes
            if (configTypes == null || configTypes.size == 0) {
                configTypes = arrayOf(ConfigType.CORE)
            }
            types = HashSet<ConfigType>(Arrays.asList(*configTypes))
            types = types
        }
    }

    internal inner class AddConfigListener(val configLoadedListener: ConfigLoadedListener) :
        Configuration<ConfigManager> {
        override fun configures(): Class<ConfigManager> {
            return ConfigManager::class.java
        }

        override fun apply(configManager: ConfigManager) {
            configManager.addConfigUpdatedListener(configLoadedListener)
        }
    }
}
