package com.mparticle.internal

import com.mparticle.audience.BaseAudienceTask
import com.mparticle.internal.MParticleApiClientImpl.MPConfigException
import com.mparticle.internal.MParticleApiClientImpl.MPRampException
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.internal.database.UploadSettings
import com.mparticle.networking.BaseNetworkConnection
import com.mparticle.networking.MParticleBaseClient
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Modifier

class MParticleApiClientTest {
    @Test
    fun testOneArgumentResponseHasNoErrorMessage() {
        val response = MParticleApiClient.AliasNetworkResponse(202)

        Assert.assertEquals(202, response.getResponseCode())
        Assert.assertNull(response.getErrorMessage())
    }

    @Test
    fun testTwoArgumentResponseKeepsItsErrorMessage() {
        val response = MParticleApiClient.AliasNetworkResponse(400, "bad request")

        Assert.assertEquals(400, response.getResponseCode())
        Assert.assertEquals("bad request", response.getErrorMessage())
    }

    @Test
    fun testSetErrorMessageReplacesTheMessage() {
        val response = MParticleApiClient.AliasNetworkResponse(-1)

        response.setErrorMessage("This device is being sampled.")
        Assert.assertEquals("This device is being sampled.", response.getErrorMessage())
        response.setErrorMessage(null)
        Assert.assertNull(response.getErrorMessage())
        Assert.assertEquals(-1, response.getResponseCode())
    }

    @Test
    fun testAliasNetworkResponseKeepsItsJvmNameAndStaysOpen() {
        val type = MParticleApiClient.AliasNetworkResponse::class.java

        Assert.assertEquals("com.mparticle.internal.MParticleApiClient\$AliasNetworkResponse", type.name)
        Assert.assertTrue(Modifier.isStatic(type.modifiers))
        Assert.assertFalse(Modifier.isFinal(type.modifiers))
        Assert.assertFalse(Modifier.isFinal(type.getMethod("getResponseCode").modifiers))
        Assert.assertFalse(Modifier.isFinal(type.getMethod("getErrorMessage").modifiers))
        Assert.assertEquals(Void.TYPE, type.getDeclaredMethod("setErrorMessage", String::class.java).returnType)
    }

    @Test
    fun testInterfaceMethodsDeclareTheirCheckedExceptions() {
        val api = MParticleApiClient::class.java
        val boolean = Boolean::class.javaPrimitiveType
        val long = Long::class.javaPrimitiveType
        val upload = arrayOf(String::class.java, UploadSettings::class.java)

        Assert.assertEquals(setOf(IOException::class.java, MPConfigException::class.java), api.getMethod("fetchConfig").exceptionTypes.toSet())
        Assert.assertEquals(setOf(IOException::class.java, MPConfigException::class.java), api.getMethod("fetchConfig", boolean).exceptionTypes.toSet())
        Assert.assertEquals(
            setOf(IOException::class.java, MPThrottleException::class.java, MPRampException::class.java),
            api.getMethod("sendMessageBatch", *upload).exceptionTypes.toSet(),
        )
        Assert.assertEquals(
            setOf(JSONException::class.java, IOException::class.java, MPThrottleException::class.java, MPRampException::class.java),
            api.getMethod("sendAliasRequest", *upload).exceptionTypes.toSet(),
        )
        Assert.assertEquals(0, api.getMethod("fetchUserAudience", BaseAudienceTask::class.java, long).exceptionTypes.size)
        Assert.assertEquals(0, api.getMethod("getCookies").exceptionTypes.size)
        Assert.assertEquals(Int::class.javaPrimitiveType, api.getMethod("sendMessageBatch", *upload).returnType)
        Assert.assertEquals(MParticleApiClient.AliasNetworkResponse::class.java, api.getMethod("sendAliasRequest", *upload).returnType)
    }

    @Test
    fun testBaseClientDeclaresTheRequestHandlerAccessors() {
        val base = MParticleBaseClient::class.java

        Assert.assertTrue(base.isAssignableFrom(MParticleApiClient::class.java))
        Assert.assertEquals(BaseNetworkConnection::class.java, base.getMethod("getRequestHandler").returnType)
        Assert.assertEquals(Void.TYPE, base.getMethod("setRequestHandler", BaseNetworkConnection::class.java).returnType)
    }
}
