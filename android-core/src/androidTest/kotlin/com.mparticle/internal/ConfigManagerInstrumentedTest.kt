package com.mparticle.internal

import com.mparticle.Configuration
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager.ConfigLoadedListener
import com.mparticle.internal.ConfigManager.ConfigType
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseAbstractTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ConfigManagerInstrumentedTest : BaseAbstractTest() {
    @Test
    @Throws(InterruptedException::class)
    fun testSetMpidCurrentUserState() {
        val mpid1 = ran.nextLong()
        val mpid2 = ran.nextLong()
        val mpid3 = ran.nextLong()
        startMParticle()
        val configManager = MParticle.getInstance()?.Internal()?.configManager
        TestCase.assertEquals(
            mStartingMpid.toLong(), MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        TestCase.assertEquals(mStartingMpid.toLong(), configManager?.mpid)
        configManager?.setMpid(mpid1, ran.nextBoolean())
        TestCase.assertEquals(
            mpid1, MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        val newIsLoggedIn = !MParticle.getInstance()
            ?.Identity()?.currentUser?.isLoggedIn!!
        configManager?.setMpid(mpid1, newIsLoggedIn)
        TestCase.assertEquals(
            mpid1, MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        TestCase.assertEquals(
            newIsLoggedIn, MParticle.getInstance()
                ?.Identity()?.currentUser?.isLoggedIn
        )
        configManager?.setMpid(mpid2, false)
        TestCase.assertEquals(
            mpid2, MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        MParticle.getInstance()?.Identity()?.currentUser?.isLoggedIn?.let {
            TestCase.assertFalse(
                it
            )
        }
        configManager?.setMpid(mpid2, true)
        TestCase.assertEquals(
            mpid2, MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        MParticle.getInstance()?.Identity()?.currentUser?.isLoggedIn?.let { TestCase.assertTrue(it) }
        configManager?.setMpid(mpid3, true)
        TestCase.assertEquals(
            mpid3, MParticle.getInstance()
                ?.Identity()?.currentUser?.id
        )
        MParticle.getInstance()?.Identity()?.currentUser?.isLoggedIn?.let { TestCase.assertTrue(it) }
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testConfigResponseParsing() {
        val token = mRandomUtils.getAlphaNumericString(20)
        val aliasMaxWindow = ran.nextInt()
        val config = JSONObject()
            .put("wst", token)
            .put(ConfigManager.ALIAS_MAX_WINDOW, aliasMaxWindow)
        mServer.setupConfigResponse(config.toString())
        var configLoadedListener = BothConfigsLoadedListener()
        var latch = configLoadedListener.latch
        startMParticle(
            MParticleOptions.builder(mContext)
                .configuration(AddConfigListener(configLoadedListener))
        )
        latch.await()
        TestCase.assertEquals(
            token,
            MParticle.getInstance()?.Internal()?.configManager?.workspaceToken
        )
        TestCase.assertEquals(
            aliasMaxWindow,
            MParticle.getInstance()?.Internal()?.configManager?.aliasMaxWindow
        )

        //test set defaults when fields are not present
        MParticle.setInstance(null)
        mServer.setupConfigResponse(JSONObject().toString())
        configLoadedListener = BothConfigsLoadedListener()
        latch = configLoadedListener.latch
        startMParticle(
            MParticleOptions.builder(mContext)
                .configuration(AddConfigListener(configLoadedListener))
        )
        latch.await()
        TestCase.assertEquals("", MParticle.getInstance()?.Internal()?.configManager?.workspaceToken)
        TestCase.assertEquals(90, MParticle.getInstance()?.Internal()?.configManager?.aliasMaxWindow)
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun cachedConfigLoadedExactlyOnce() {
        val latch = MPLatch(1)
        val loadedCoreLocal = AndroidUtils.Mutable(false)
        val loadedKitLocal = AndroidUtils.Mutable(false)
        setCachedConfig(simpleConfigWithKits)
        mServer.setupConfigDeferred()
        val configLoadedListener = ConfigLoadedListener { configType, isNew ->
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
            Logger.error("KIT = " + loadedKitLocal.value + " Core: " + loadedCoreLocal.value)
        }
        val options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configuration(AddConfigListener(configLoadedListener))
            .build()
        MParticle.start(options)

        //wait until both local configs are loaded
        latch.await()

        //try to coerce another load...
        ConfigManager(mContext)
        val instance = MParticle.getInstance()
        instance?.logEvent(TestingUtils.getInstance().randomMPEventSimple)
        instance?.optOut = true
        instance?.optOut = false

        //and finally, load remote config
        mServer.setupConfigResponse(simpleConfigWithKits.toString())
        fetchConfig()
        val bothConfigsLoadedListener = BothConfigsLoadedListener()
        val reloadLatch = bothConfigsLoadedListener.latch
        MParticle.getInstance()?.Internal()?.configManager?.addConfigUpdatedListener(
            bothConfigsLoadedListener
        )
        reloadLatch.await()
    }

    internal inner class BothConfigsLoadedListener(vararg configTypes: ConfigType) :
        ConfigLoadedListener {
        private var types: MutableSet<ConfigType>
        var latch = MPLatch(1)

        init {
            var configTypes = configTypes
            if (configTypes.isEmpty()) {
                configTypes = arrayOf(ConfigType.CORE)
            }
            types = HashSet(listOf(*configTypes))
        }

        override fun onConfigUpdated(configType: ConfigType, isNew: Boolean) {
            if (isNew) {
                types.remove(configType)
            }
            if (types.size == 0) {
                latch.countDown()
            }
        }
    }

    internal inner class AddConfigListener(private var configLoadedListener: ConfigLoadedListener) :
        Configuration<ConfigManager> {
        override fun configures(): Class<ConfigManager> {
            return ConfigManager::class.java
        }
        override fun apply(configManager: ConfigManager?) {
            configManager?.addConfigUpdatedListener(configLoadedListener)
        }
    }
}