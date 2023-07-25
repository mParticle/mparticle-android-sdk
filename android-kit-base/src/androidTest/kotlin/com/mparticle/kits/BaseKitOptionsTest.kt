package com.mparticle.kits

import com.mparticle.AccessUtils
import com.mparticle.Configuration
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONArray
import org.json.JSONObject

open class BaseKitOptionsTest : BaseCleanInstallEachTest() {

    override fun startMParticle(optionsBuilder: MParticleOptions.Builder) {
        startMParticle(optionsBuilder, true)
    }

    fun startMParticle(optionsBuilder: MParticleOptions.Builder, awaitKitLoaded: Boolean) {
        AccessUtils.setCredentialsIfEmpty(optionsBuilder)
        val kitsLoadedLatch = MPLatch(1)
        var kitCount = 0
        val kitsLoadedListener = object : Configuration<KitManagerImpl> {
            override fun configures() = KitManagerImpl::class.java

            override fun apply(kitManager: KitManagerImpl) {
                kitManager.addKitsLoadedListener { kits, previousKits, kitConfigs ->
                    if (kitConfigs.size == kitCount) {
                        kitsLoadedLatch.countDown()
                    }
                }
            }
        }

        val options: MParticleOptions = optionsBuilder
            .configuration(kitsLoadedListener)
            .build()
        val kitOptions = options.getConfiguration(KitOptions::class.java) ?: KitOptions()
        val configuredKitOptions =
            options.getConfiguration(ConfiguredKitOptions::class.java) ?: ConfiguredKitOptions()

        val kitConfigurations: MutableMap<Int, JSONObject?> =
            mutableMapOf()
        (kitOptions.kits + configuredKitOptions.kits).forEach { id, clazz ->
            kitConfigurations[id] = JSONObject().put("id", id)
        }
        configuredKitOptions.testingConfiguration.forEach { id, config ->
            kitConfigurations[id] = config
        }
        kitConfigurations.values
            .filterNotNull()
            .fold(JSONArray()) { init, next -> init.apply { put(next) } }
            .let {
                kitCount = it.length()
                JSONObject().put("eks", it)
            }
            .let {
                mServer.setupConfigResponse(it.toString())
            }
        if (kitConfigurations.isEmpty()) {
            kitsLoadedLatch.countDown()
        }
        super.startMParticle(optionsBuilder)
        if (awaitKitLoaded) {
            kitsLoadedLatch.await()
        }
    }

    protected fun waitForKitToStart(kitId: Int) {
        val latch = MPLatch(1)
        // wait for kit to start/reload
        com.mparticle.internal.AccessUtils.getKitManager()
            .addKitsLoadedListener { kits, previousKits, kitConfigs ->
                if (kits.containsKey(kitId)) {
                    latch.countDown()
                }
            }
        // check if the kit has already been started and short-circut if it has
        if (MParticle.getInstance()?.isKitActive(kitId) == true) {
            latch.countDown()
        }
        latch.await()
    }

    @Throws(InterruptedException::class)
    @JvmOverloads
    fun waitForKitReload(after: (() -> Unit)? = null) {
        val latch = MPLatch(1)
        com.mparticle.internal.AccessUtils.getKitManager()
            .addKitsLoadedListener { _: Map<Int?, KitIntegration?>, _: Map<Int?, KitIntegration?>?, _: List<KitConfiguration?>? ->
                latch.countDown()
            }
        after?.invoke()
        latch.await()
    }
}
