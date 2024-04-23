package com.mparticle.audience

import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Method

class AudienceResponseTest {

    private lateinit var classInstance: AudienceResponse

    @Before
    fun before() {
        classInstance = AudienceResponse(-1, JSONObject())
    }

    @Test
    fun parseJsonObjectTest() {
        val sampleJson =
            "{'dt':'ar','id':'54335128-0b2c-4089-a36d-8b456890dfe8','ct':1713390288601,'audience_memberships':[{'audience_id':13775},{'audience_id':13775}]}"
        val method: Method = AudienceResponse::class.java.getDeclaredMethod(
            "parseJsonObject",
            JSONObject::class.java
        )
        method.isAccessible = true
        val jsonObject1 = JSONObject(sampleJson)
        method.invoke(classInstance, jsonObject1)
        Assert.assertEquals(2, classInstance.getAudienceResult().size)
    }

    @Test
    fun parseJsonObjectTest_whenJSONIsEmpty() {
        val sampleJson = "{}"
        val method: Method = AudienceResponse::class.java.getDeclaredMethod(
            "parseJsonObject",
            JSONObject::class.java
        )
        method.isAccessible = true
        val jsonObject1 = JSONObject(sampleJson)
        method.invoke(classInstance, jsonObject1)

        Assert.assertEquals(0, classInstance.getAudienceResult().size)
    }

    @Test
    fun parseJsonObjectTest_whenObject_Is_NULL() {
        classInstance = AudienceResponse(-1, JSONObject())
        val method: Method = AudienceResponse::class.java.getDeclaredMethod(
            "parseJsonObject",
            JSONObject::class.java
        )
        method.isAccessible = true
        val jsonObject1 = null
        method.invoke(classInstance, jsonObject1)

        Assert.assertEquals(0, classInstance.getAudienceResult().size)
    }

    @Test
    fun parseJsonObjectTest_when_AudienceMemberships_Field_Is_Not_Available() {
        val sampleJson =
            "{'dt':'ar','id':'54335128-0b2c-4089-a36d-8b456890dfe8','ct':1713390288601}"
        val method: Method = AudienceResponse::class.java.getDeclaredMethod(
            "parseJsonObject",
            JSONObject::class.java
        )
        method.isAccessible = true
        val jsonObject1 = JSONObject(sampleJson)
        method.invoke(classInstance, jsonObject1)

        Assert.assertEquals(0, classInstance.getAudienceResult().size)
    }

    @Test
    fun parseJsonObjectTest_when_Aaudience_id_Field_Is_Not_Available() {
        val sampleJson =
            "{'dt':'ar','id':'54335128-0b2c-4089-a36d-8b456890dfe8','ct':1713390288601,'audience_memberships':[{}]}"
        val method: Method = AudienceResponse::class.java.getDeclaredMethod(
            "parseJsonObject",
            JSONObject::class.java
        )
        method.isAccessible = true
        val jsonObject1 = JSONObject(sampleJson)
        method.invoke(classInstance, jsonObject1)

        Assert.assertEquals(0, classInstance.getAudienceResult().size)
    }

    @Test
    fun getErrorTest() {
        val error_Msg = "Unauthorized"
        classInstance = AudienceResponse(401, error_Msg)
        val field: Field = AudienceResponse::class.java.getDeclaredField("code")
        field.isAccessible = true
        Assert.assertEquals(error_Msg, classInstance.getError())
        Assert.assertEquals(401, field.getInt(classInstance))
    }

    @Test
    fun getErrorTest_When_Error_Is_Empty() {
        val error_Msg = ""
        classInstance = AudienceResponse(-1, error_Msg)
        val field: Field = AudienceResponse::class.java.getDeclaredField("code")
        field.isAccessible = true
        Assert.assertEquals(error_Msg, classInstance.getError())
        Assert.assertEquals(-1, field.getInt(classInstance))
    }
}
