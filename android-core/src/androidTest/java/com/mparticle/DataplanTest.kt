package com.mparticle

import com.mparticle.api.Logger
import com.mparticle.messages.events.MPEventMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.Mutable
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.utils.randomMPEventRich
import com.mparticle.utils.startMParticle
import junit.framework.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.junit.JUnitAsserter.fail

class DataplanTest : BaseTest() {
    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan(null, null)
        )
        val messageCount: Mutable<Int> = Mutable<Int>(0)
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                assertNull(it.body.dataplanContext)
                it.body.messages
                    .filterIsInstance<MPEventMessage>().let {
                        messageCount.value += it.size
                        messageCount.value == 3
                    }
            }
            .after {
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.upload()
            }
            .blockUntilFinished()
        assertEquals(3, messageCount.value)
        Logger.error("finished")
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanPartialTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("plan1", null)
        )
        val messageCount: Mutable<Int> = Mutable<Int>(0)
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                assertEquals("plan1", it.body.dataplanContext?.dataplan?.dataplanId)
                assertNull(it.body.dataplanContext?.dataplan?.dataplanVersion)
                it.body.messages
                    .filterIsInstance<MPEventMessage>().let {
                        messageCount.value += it.size
                        messageCount.value == 3
                    }
            }
            .after {
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.upload()
            }
            .blockUntilFinished()
        assertEquals(3, messageCount.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanIdTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan(null, 1)
        )
        val messageCount: Mutable<Int> = Mutable<Int>(0)
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                Logger.error("CALLBACK!!")
                assertNull(it.body.dataplanContext)
                it.body.messages
                    .filterIsInstance<MPEventMessage>().let {
                        messageCount.value += it.size
                        messageCount.value == 3
                    }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(randomMPEventRich())
                    logEvent(randomMPEventRich())
                    logEvent(randomMPEventRich())
                    upload()
                }
            }
            .blockUntilFinished()
        assertEquals(3, messageCount.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataPlanSetTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("dataplan1", 1)
        )
        val messageCount: Mutable<Int> = Mutable<Int>(0)
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                assertEquals("dataplan1", it.body.dataplanContext?.dataplan?.dataplanId)
                assertEquals(1, it.body.dataplanContext?.dataplan?.dataplanVersion)
                it.body.messages
                    .filterIsInstance<MPEventMessage>().let {
                        messageCount.value += it.size
                        messageCount.value == 3
                    }
            }
            .after {
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.logEvent(randomMPEventRich())
                MParticle.getInstance()!!.upload()
            }
            .blockUntilFinished()
        assertEquals(3, messageCount.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanChanged() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("dataplan1", 1)
        )
        val totalMessageCount: Mutable<Int> = Mutable<Int>(0)
        val dataplan1MessageCount: Mutable<Int> = Mutable<Int>(0)
        val dataplan2MessageCount: Mutable<Int> = Mutable<Int>(0)

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                assertEquals("dataplan1", it.body.dataplanContext?.dataplan?.dataplanId)
                when (it.body.dataplanContext?.dataplan?.dataplanVersion) {
                    1 -> dataplan1MessageCount.value += it.body.messages.filterIsInstance<MPEventMessage>().size
                    2 -> dataplan2MessageCount.value += it.body.messages.filterIsInstance<MPEventMessage>().size
                    else -> fail("Unknown dataplan version: ${it.body.dataplanContext?.dataplan?.dataplanVersion}")
                }
                it.body.messages
                    .filterIsInstance<MPEventMessage>().let {
                        totalMessageCount.value += it.size
                        totalMessageCount.value == 5
                    }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(randomMPEventRich())
                    logEvent(randomMPEventRich())
                    logEvent(randomMPEventRich())
                }
                MParticle.setInstance(null)
                startMParticle(
                    MParticleOptions.builder(context)
                        .dataplan("dataplan1", 2)
                )
                MParticle.getInstance()?.apply {
                    logEvent(randomMPEventRich())
                    logEvent(randomMPEventRich())
                    upload()
                }
            }
            .blockUntilFinished()

        // not sure why it needs upload() twice, but this cuts the runtime down from 10s to .7s
        MParticle.getInstance()!!.upload()
        MParticle.getInstance()!!.upload()
        assertEquals(3, dataplan1MessageCount.value)
        assertEquals(2, dataplan2MessageCount.value)
        assertEquals(5, totalMessageCount.value)
    }
}
