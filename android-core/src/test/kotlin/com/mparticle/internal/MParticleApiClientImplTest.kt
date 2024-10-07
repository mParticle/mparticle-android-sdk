package com.mparticle.internal

import android.content.SharedPreferences
import androidx.test.filters.LargeTest
import com.mparticle.internal.MParticleApiClientImpl.MPConfigException
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockSharedPreferences
import com.mparticle.networking.MPConnection
import com.mparticle.networking.MPUrl
import com.mparticle.networking.MParticleBaseClientImpl
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.math.BigInteger
import java.net.URL

@RunWith(PowerMockRunner::class)
class MParticleApiClientImplTest {
    lateinit var client: MParticleApiClientImpl
    private lateinit var mockConnection: MPConnection
    private lateinit var configManager: ConfigManager
    private lateinit var sharedPrefs: MockSharedPreferences

    @Throws(Exception::class)
    fun setup() {
        configManager = Mockito.mock(ConfigManager::class.java)
        Mockito.`when`(configManager.apiKey).thenReturn("some api key")
        Mockito.`when`(configManager.apiSecret).thenReturn("some api secret")
        sharedPrefs = MockSharedPreferences()
        client = MParticleApiClientImpl(
            configManager,
            sharedPrefs,
            MockContext()
        )
        client.mDeviceRampNumber = 50
        val mockUrl = PowerMockito.mock(MPUrl::class.java)
        mockConnection = PowerMockito.mock(MPConnection::class.java)
        Mockito.`when`(mockUrl.openConnection()).thenReturn(mockConnection)
        Mockito.`when`(mockConnection.url).thenReturn(mockUrl)
        Mockito.`when`(mockUrl.defaultUrl).thenReturn(mockUrl)
        Mockito.`when`(mockUrl.file).thenReturn("/config")
        client.setConfigUrl(mockUrl)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testAddMessageSignature() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        val headerCapture = ArgumentCaptor.forClass(
            String::class.java
        )
        val headerValueCapture = ArgumentCaptor.forClass(
            String::class.java
        )
        client.addMessageSignature(mockConnection, "this is a sample batch")
        Mockito.verify(mockConnection, Mockito.times(2))
            .setRequestProperty(headerCapture.capture(), headerValueCapture.capture())
        val headerKeys = headerCapture.allValues
        val headerValues = headerValueCapture.allValues
        val dateIndex = headerKeys.indexOf("Date")
        Assert.assertTrue(headerKeys.toString(), dateIndex >= 0)
        val dateValue = headerValues[dateIndex]
        Assert.assertNotNull(dateValue)
        Assert.assertTrue(dateValue.isNotEmpty())
        val signatureIndex = headerKeys.indexOf("x-mp-signature")
        Assert.assertTrue(headerValues.toString(), signatureIndex >= 0)
        val signatureValue = headerValues[signatureIndex]
        Assert.assertNotNull(signatureValue)
        Assert.assertTrue(signatureValue.isNotEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testRampNumber() {
        val context = MockContext()
        val sharedPreferences: SharedPreferences = MockSharedPreferences()
        context.setSharedPreferences(sharedPreferences)
        val test = MPUtility.hashFnv1A(MPUtility.getRampUdid(context).toByteArray())
            .mod(BigInteger.valueOf(100))
            .toInt()
        Assert.assertTrue("Ramp should be between 0 and 100: $test", test in 0..99)
        val test2 = MPUtility.hashFnv1A(MPUtility.getRampUdid(context).toByteArray())
            .mod(BigInteger.valueOf(100))
            .toInt()
        Assert.assertTrue(test == test2)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testFetchConfigSuccess() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        Mockito.`when`(mockConnection.responseCode).thenReturn(200)
        val response = JSONObject()
        response.put("test", "value")
        Mockito.`when`(MPUtility.getJsonResponse(mockConnection)).thenReturn(response)
        val captor = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        client.fetchConfig()
        Mockito.verify(configManager)?.updateConfig(
            captor.capture(),
            Mockito.nullable(
                String::class.java
            ),
            Mockito.nullable(String::class.java)
        )
        Assert.assertEquals(response, captor.value)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testFetchConfigFailure() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        Mockito.`when`(mockConnection.responseCode).thenReturn(400)
        var e: Exception? = null
        try {
            client.fetchConfig()
        } catch (cfe: MPConfigException) {
            e = cfe
        }
        Assert.assertNotNull(e)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testConfigDelay() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        Mockito.`when`(mockConnection.responseCode).thenReturn(400)
        var e: Exception? = null
        try {
            client.fetchConfig()
        } catch (cfe: MPConfigException) {
            e = cfe
        }
        Assert.assertNotNull(e)
        e = null
        try {
            client.fetchConfig()
        } catch (cfe: MPConfigException) {
            e = cfe
        }
        Assert.assertNull(e)
        e = null
        try {
            client.fetchConfig(true)
        } catch (cfe: MPConfigException) {
            e = cfe
        }
        Assert.assertNotNull(e)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testCheckThrottleTime() {
        setup()
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            client.checkThrottleTime(endpoint)
            client.requestHandler.setNextRequestTime(endpoint, System.currentTimeMillis() + 1000)
            var e: Exception? = null
            try {
                client.checkThrottleTime(endpoint)
            } catch (cfe: MPThrottleException) {
                e = cfe
            }
            Assert.assertNotNull(e)
        }
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testMessageBatchWhileThrottled() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        try {
            client.sendMessageBatch("", configManager.uploadSettings)
        } catch (e: Exception) {
            if (e is MPThrottleException) {
                throw e
            }
        }
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.EVENTS,
            System.currentTimeMillis() + 1000
        )
        var e: Exception? = null
        try {
            client.sendMessageBatch("", configManager.uploadSettings)
        } catch (cfe: MPThrottleException) {
            e = cfe
        }
        Assert.assertNotNull(e)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testConfigRequestWhileThrottled() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        // set all endpoints throttled, Config should not be listening to any throttle
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            client.requestHandler.setNextRequestTime(endpoint, System.currentTimeMillis() + 1000)
        }
        Mockito.`when`(mockConnection.responseCode).thenReturn(200)
        val response = JSONObject()
        response.put("test", "value")
        Mockito.`when`(MPUtility.getJsonResponse(mockConnection)).thenReturn(response)
        val captor = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        client.fetchConfig()
        Mockito.verify(configManager)?.updateConfig(
            captor.capture(),
            Mockito.nullable(
                String::class.java
            ),
            Mockito.nullable(String::class.java)
        )
        Assert.assertEquals(response, captor.value)
    }

    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testAliasRequestWhileThrottled() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        try {
            client.sendAliasRequest("", configManager.uploadSettings)
        } catch (e: Exception) {
            if (e is MPThrottleException) {
                throw e
            }
        }
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.ALIAS,
            System.currentTimeMillis() + 1000
        )
        var e: Exception? = null
        try {
            client.sendAliasRequest("", configManager.uploadSettings)
        } catch (cfe: MPThrottleException) {
            e = cfe
        }
        Assert.assertNotNull(e)
    }

    /**
     * make sure that both Events and Alias requests go through when the Other's requests
     * are throttled
     * @throws Exception
     */
    @Test
    @PrepareForTest(URL::class, MParticleApiClientImpl::class, MPUtility::class)
    @Throws(
        Exception::class
    )
    fun testAliasEventsOnSeparateThrottles() {
        setup()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString()))
            .thenReturn("encoded")
        try {
            client.sendMessageBatch("", configManager.uploadSettings)
            client.sendAliasRequest("", configManager.uploadSettings)
        } catch (e: Exception) {
            if (e is MPThrottleException) {
                throw e
            }
        }

        // make sure Events still works when Alias is throttled
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.ALIAS,
            System.currentTimeMillis() + 1000
        )
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.EVENTS,
            System.currentTimeMillis() - 1000
        )
        var ex: MPThrottleException? = null
        try {
            client.sendMessageBatch("", configManager.uploadSettings)
        } catch (e: Exception) {
            if (e is MPThrottleException) {
                throw e
            }
        }
        try {
            client.sendAliasRequest("", configManager.uploadSettings)
        } catch (e: MPThrottleException) {
            ex = e
        }
        Assert.assertNotNull(ex)

        // make sure Alias still works when Events is throttled
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.ALIAS,
            System.currentTimeMillis() - 1000
        )
        client.requestHandler.setNextRequestTime(
            MParticleBaseClientImpl.Endpoint.EVENTS,
            System.currentTimeMillis() + 1000
        )
        ex = null
        try {
            client.sendAliasRequest("", configManager.uploadSettings)
        } catch (e: Exception) {
            if (e is MPThrottleException) {
                throw e
            }
        }
        try {
            client.sendMessageBatch("", configManager.uploadSettings)
        } catch (e: MPThrottleException) {
            ex = e
        }
        Assert.assertNotNull(ex)
    }

    @LargeTest
    @PrepareForTest(URL::class, MParticleApiClientImpl::class)
    @Throws(
        Exception::class
    )
    fun testSetNextAllowedRequestTime() {
        setup()
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            // need a delta to account for test timing variation
            val delta: Long = 50
            client.requestHandler.setNextAllowedRequestTime(null, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > System.currentTimeMillis() + MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS - delta)
            Mockito.`when`(mockConnection.getHeaderField(Mockito.anyString())).thenReturn(null)
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta)
            Mockito.`when`(mockConnection.getHeaderField("Retry-After")).thenReturn("")
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta)
            Mockito.`when`(mockConnection.getHeaderField("Retry-After")).thenReturn("-1000")
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > MParticleApiClientImpl.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta)
            Mockito.`when`(mockConnection.getHeaderField("Retry-After")).thenReturn("60")
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= 60 * 1000 + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > 60 * 1000 + System.currentTimeMillis() - delta)
            Mockito.`when`(mockConnection.getHeaderField("Retry-After")).thenReturn("")
            Mockito.`when`(mockConnection.getHeaderField("retry-after")).thenReturn("100")
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= 100 * 1000 + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > 100 * 1000 + System.currentTimeMillis() - 10)
            Mockito.`when`(mockConnection.getHeaderField("Retry-After")).thenReturn(
                (60 * 60 * 25).toString()
            )
            client.requestHandler.setNextRequestTime(endpoint, 0)
            Assert.assertEquals(0, client.getNextRequestTime(endpoint))
            client.requestHandler.setNextAllowedRequestTime(mockConnection, endpoint)
            Assert.assertTrue(client.getNextRequestTime(endpoint) <= MParticleApiClientImpl.MAX_THROTTLE_MILLIS + System.currentTimeMillis())
            Assert.assertTrue(client.getNextRequestTime(endpoint) > MParticleApiClientImpl.MAX_THROTTLE_MILLIS + System.currentTimeMillis() - delta)
        }
    }
}
