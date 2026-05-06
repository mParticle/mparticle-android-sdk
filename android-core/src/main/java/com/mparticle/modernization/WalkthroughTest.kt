package com.mparticle.modernization

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.modernization.core.MParticle
import com.mparticle.modernization.eventlogging.MParticleEventLogging


class WalkthroughTest(private val context: Context) {

    private var eventLogging: MParticleEventLogging? = null

    //Only setup
    fun initialize() {
        val options = MParticleOptions.builder(context).credentials("key", "secret").build()
        MParticle.start(options)
        eventLogging = MParticle.getInstance().EventLogging()
    }

    init {
        initialize()
    }

    //===================

    fun runTest() {
        logCommerceEvent()
        logNormalEvent()
    }

    fun logCommerceEvent() {
        val product = Product.Builder("testName", "testSku", 100.0).build()
        val commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product).build()
        eventLogging?.logEvent(commerceEvent)
    }
    fun logNormalEvent() {
        val customAttributes =
            mutableMapOf(Pair("key1", "value1"), Pair("key2", 4), Pair("key3", null))
        val event = MPEvent.Builder("myEvent").customAttributes(customAttributes).build()
        eventLogging?.logEvent(event)
    }
}