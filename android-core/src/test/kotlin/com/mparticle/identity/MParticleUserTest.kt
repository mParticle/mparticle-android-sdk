package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MockMParticle
import com.mparticle.TypedUserAttributeListener
import com.mparticle.UserAttributeListener
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class MParticleUserTest {
    private var mp: MParticle? = null
    private var id: IdentityApi? = null
    private val defaultMpId = 1L

    @Before
    fun setup() {
        MParticle.setInstance(MockMParticle())
        mp = MParticle.getInstance()
        id = mp?.Identity()
        Mockito.`when`(mp?.Identity()?.mConfigManager?.mpid).thenReturn(defaultMpId)
    }

    /**
     * This tests that when you add a call setUserIdentity, with a user name and id that already exists,
     * we will not log it.
     */
    @Test
    @Throws(Exception::class)
    fun testAddExistingUserIdentity() {
        val identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 8, \"i\": \"alias test\", \"dfs\": 1473869816521, \"f\": true }"))
        Mockito.`when`(id?.mMessageManager?.getUserIdentityJson(defaultMpId)).thenReturn(identities)
        Mockito.`when`(
            id?.mMessageManager?.logUserIdentityChangeMessage(
                Mockito.any(
                    JSONObject::class.java
                ),
                Mockito.any(JSONObject::class.java),
                Mockito.any(
                    JSONArray::class.java
                ),
                ArgumentMatchers.eq<Long>(defaultMpId)
            )
        ).thenThrow(
            AssertionError("Should not log redundent User Identity")
        )
        val ids = HashMap<IdentityType, String>()
        ids[IdentityType.Alias] = "alias test"
        (id?.currentUser as MParticleUserImpl?)?.userIdentities = ids
    }

    @Test
    @Throws(Exception::class)
    fun testChangeUserIdentity() {
        val identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        Mockito.`when`(id?.mMessageManager?.getUserIdentityJson(1)).thenReturn(identities)
        (id?.currentUser as MParticleUserImpl?)?.setUserIdentity(
            IdentityType.Email,
            "email value 2"
        )
        val argument1 = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        val argument2 = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        val argument3 = ArgumentCaptor.forClass(
            JSONArray::class.java
        )
        Mockito.verify(
            id!!.mMessageManager,
            Mockito.times(1)
        ).logUserIdentityChangeMessage(
            argument1.capture(),
            argument2.capture(),
            argument3.capture(),
            ArgumentMatchers.eq<Long>(defaultMpId)
        )
        val oldIdentity = argument2.value
        Assert.assertEquals(oldIdentity["i"], "email value 1")
        Assert.assertEquals(oldIdentity["n"], 7)
        Assert.assertEquals(oldIdentity.getDouble("dfs"), 1473869816521.0, 100.0)
        Assert.assertEquals(oldIdentity["f"], true)
        val newIdentity = argument1.value
        Assert.assertEquals(newIdentity["i"], "email value 2")
        Assert.assertEquals(newIdentity["n"], 7)
        Assert.assertEquals(newIdentity.getDouble("dfs"), 1473869816521.0, 100.0)
        Assert.assertEquals(newIdentity["f"], false)
        Mockito.verify(
            MParticle.getInstance()?.Internal()?.kitManager,
            Mockito.times(1)
        )
            ?.setUserIdentity(
                ArgumentMatchers.eq("email value 2"),
                ArgumentMatchers.eq(
                    IdentityType.Email
                )
            )
        val allIdentities = argument3.value
        Assert.assertEquals(1, allIdentities.length().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserAttribute() {
        val mp = MParticle.getInstance()?.Identity()
        mp?.currentUser?.removeUserAttribute("")?.let { Assert.assertFalse(it) }
        mp?.currentUser?.removeUserAttribute("")?.let { Assert.assertFalse(it) }
        mp?.currentUser?.removeUserAttribute("test")?.let { Assert.assertTrue(it) }
        Mockito.verify(
            mp?.mMessageManager,
            Mockito.times(1)
        )?.removeUserAttribute("test", 1)
        Mockito.verify(MParticle.getInstance()!!.Internal().kitManager, Mockito.times(1))
            .removeUserAttribute("test", 1)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserTag() {
        id?.currentUser?.removeUserAttribute("")?.let { Assert.assertFalse(it) }
        id?.currentUser?.removeUserAttribute("")?.let { Assert.assertFalse(it) }
        id?.currentUser?.removeUserAttribute("test")?.let { Assert.assertTrue(it) }
        Mockito.verify(
            id?.mMessageManager,
            Mockito.times(1)
        )?.removeUserAttribute("test", 1)
        Mockito.verify(
            mp?.Internal()?.kitManager,
            Mockito.times(1)
        )?.removeUserAttribute("test", 1)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributes() {
        val listener = Mockito.mock(
            UserAttributeListener::class.java
        )
        id?.currentUser?.getUserAttributes(listener)
        Mockito.verify(
            mp?.Identity()?.mMessageManager,
            Mockito.times(1)
        )?.getUserAttributes(
            ArgumentMatchers.any(
                UserAttributeListenerWrapper::class.java
            ),
            ArgumentMatchers.eq<Long>(defaultMpId)
        )
        val typedListener = Mockito.mock(
            TypedUserAttributeListener::class.java
        )
        id?.currentUser?.getUserAttributes(typedListener)
        Mockito.verify(
            mp?.Identity()?.mMessageManager,
            Mockito.times(2)
        )?.getUserAttributes(
            ArgumentMatchers.any(
                UserAttributeListenerWrapper::class.java
            ),
            ArgumentMatchers.eq<Long>(defaultMpId)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserIdentityWhenNoneExist() {
        val identities = JSONArray()
        Mockito.`when`(mp?.Identity()?.mMessageManager?.getUserIdentityJson(defaultMpId))
            .thenReturn(identities)
        (id?.currentUser as MParticleUserImpl?)?.setUserIdentity(IdentityType.Alias, null)
        Mockito.verify(
            mp?.Identity()?.mMessageManager,
            Mockito.times(0)
        )?.logUserIdentityChangeMessage(
            Mockito.any(JSONObject::class.java),
            Mockito.any(JSONObject::class.java),
            Mockito.any(JSONArray::class.java),
            ArgumentMatchers.eq<Long>(defaultMpId)
        )
        Mockito.verify(
            mp?.Internal()?.kitManager,
            Mockito.times(0)
        )?.removeUserIdentity(
            Mockito.any(
                IdentityType::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserIdentity() {
        val identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        Mockito.`when`(mp?.Identity()?.mMessageManager?.getUserIdentityJson(defaultMpId))
            .thenReturn(identities)
        (id?.currentUser as MParticleUserImpl?)?.setUserIdentity(IdentityType.Email, null)
        val argument2 = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        val argument3 = ArgumentCaptor.forClass(
            JSONArray::class.java
        )
        val argument4 = ArgumentCaptor.forClass(
            Long::class.java
        )
        Mockito.verify(
            mp?.Identity()?.mMessageManager,
            Mockito.times(1)
        )?.logUserIdentityChangeMessage(
            Mockito.isNull(JSONObject::class.java),
            argument2.capture(),
            argument3.capture(),
            argument4.capture()
        )
        val oldIdentity = argument2.value
        Assert.assertEquals(oldIdentity["i"], "email value 1")
        Assert.assertEquals(oldIdentity["n"], 7)
        Assert.assertEquals(oldIdentity.getDouble("dfs"), 1473869816521.0, 100.0)
        Assert.assertEquals(oldIdentity["f"], true)
        Assert.assertTrue(argument4.value == defaultMpId)
        Mockito.verify(
            mp?.Internal()?.kitManager,
            Mockito.times(1)
        )?.removeUserIdentity(
            IdentityType.Email
        )
        val allIdentities = argument3.value
        Assert.assertEquals(0, allIdentities.length().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAddUserIdentity() {
        val identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        Mockito.`when`(mp?.Identity()?.mMessageManager?.getUserIdentityJson(Mockito.anyLong()))
            .thenReturn(identities)
        (id?.currentUser as MParticleUserImpl?)?.setUserIdentity(IdentityType.Alias, "alias test")
        val argument2 = ArgumentCaptor.forClass(
            JSONObject::class.java
        )
        val argument3 = ArgumentCaptor.forClass(
            JSONArray::class.java
        )
        val argument4 = ArgumentCaptor.forClass(
            Long::class.java
        )
        Mockito.verify(
            mp?.Identity()?.mMessageManager,
            Mockito.times(1)
        )?.logUserIdentityChangeMessage(
            argument2.capture(),
            Mockito.isNull(JSONObject::class.java),
            argument3.capture(),
            argument4.capture()
        )
        val oldIdentity = argument2.value
        Assert.assertEquals(oldIdentity["i"], "alias test")
        Assert.assertEquals(
            oldIdentity["n"],
            IdentityType.Alias.value
        )
        Assert.assertEquals(
            oldIdentity.getDouble("dfs"),
            System.currentTimeMillis().toDouble(),
            1000.0
        )
        Assert.assertEquals(oldIdentity["f"], true)
        Assert.assertTrue(argument4.value == defaultMpId)
        Mockito.verify(mp?.Internal()?.kitManager, Mockito.times(1))?.setUserIdentity(
            ArgumentMatchers.eq("alias test"),
            ArgumentMatchers.eq(
                IdentityType.Alias
            )
        )
        val allIdentities = argument3.value
        Assert.assertEquals(2, allIdentities.length().toLong())
    }
}
