package com.mparticle.kits

import com.mparticle.Configuration
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.utils.setCredentialsIfEmpty
import com.mparticle.kits.utils.startMParticle
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch

open class BaseKitOptionsTest : BaseTest() {

    fun startMParticle(optionsBuilder: MParticleOptions.Builder) {
        setCredentialsIfEmpty(optionsBuilder)
        val kitsLoadedLatch = FailureLatch()
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

        val configResponseMessage = configuredKitOptions.initialConfig
            .let {
                kitCount = it.size
                ConfigResponseMessage().apply {
                    kits = it.values.toList()
                }
            }
        if (configResponseMessage.kits.isNullOrEmpty()) {
            kitsLoadedLatch.countDown()
        }
        startMParticle(optionsBuilder, configResponseMessage)
        kitsLoadedLatch.await()
    }

    protected fun waitForKitToStart(kitId: Int) {
        val latch = FailureLatch()
        // wait for kit to start/reload
        com.mparticle.internal.AccessUtil.kitManager().addKitsLoadedListener { kits, previousKits, kitConfigs ->
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
        val latch = FailureLatch()
        com.mparticle.internal.AccessUtil.kitManager()
            .addKitsLoadedListener { _: Map<Int?, KitIntegration?>, _: Map<Int?, KitIntegration?>?, _: List<KitConfiguration?>? ->
                latch.countDown()
            }
        after?.invoke()
        latch.await()
    }
}
