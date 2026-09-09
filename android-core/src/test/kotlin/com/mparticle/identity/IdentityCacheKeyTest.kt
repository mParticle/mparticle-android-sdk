package com.mparticle.identity

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class IdentityCacheKeyTest {
    @Test
    fun serializeIdentitiesMatchesAppleSdkFormat() {
        val identities = JSONObject()
        identities.put("email", "test1@test2.com")
        identities.put("customerid", "12345")
        identities.put("google", JSONObject.NULL)
        identities.put("ios_idfv", "abcdefg")

        Assert.assertEquals(
            "::customerid:12345::email:test1@test2.com::google:null::ios_idfv:abcdefg",
            MParticleIdentityClientImpl.serializeIdentities(identities),
        )
    }

    @Test
    fun identityCacheKeyIsStableAcrossJsonKeyOrder() {
        val first = JSONObject()
        first.put("email", "test1@test2.com")
        first.put("customerid", "12345")
        val second = JSONObject()
        second.put("customerid", "12345")
        second.put("email", "test1@test2.com")

        Assert.assertEquals(
            MParticleIdentityClientImpl.identityCacheKey(first, "login"),
            MParticleIdentityClientImpl.identityCacheKey(second, "login"),
        )
        Assert.assertNotEquals(
            MParticleIdentityClientImpl.identityCacheKey(first, "login"),
            MParticleIdentityClientImpl.identityCacheKey(first, "identify"),
        )
    }

    @Test
    fun emptyIdentitiesAreNotHashed() {
        Assert.assertNull(MParticleIdentityClientImpl.hashIdentities(JSONObject()))
        Assert.assertNull(MParticleIdentityClientImpl.hashIdentities(null))
        Assert.assertNull(MParticleIdentityClientImpl.identityCacheKey(JSONObject(), "login"))
    }

    @Test
    fun sha256HexUsesFullDigest() {
        val hash = MParticleIdentityClientImpl.sha256Hex("::email:test@test.com::customerid:12435")
        Assert.assertNotNull(hash)
        Assert.assertEquals(64, hash!!.length)
    }
}
