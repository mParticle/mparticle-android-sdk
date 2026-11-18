package com.mparticle.kits

import com.mparticle.TypedUserAttributeListener
import com.mparticle.UserAttributeListenerType
import com.mparticle.identity.MParticleUser
import com.mparticle.mock.MockKitConfiguration
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class FilteredMParticleUserTest {
    private val blockedKey = "blocked attribute"
    private val allowedKey = "allowed attribute"
    private val allAttributes = hashMapOf<String, Any>(blockedKey to "fake value one", allowedKey to "fake value two")
    private val allowedAttributes = mapOf<String, Any>(allowedKey to "fake value two")

    private val noOpListener =
        object : TypedUserAttributeListener {
            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long,
            ) = Unit
        }

    private fun kitIntegration(): KitIntegration {
        val userAttributeFilters = JSONObject().put(KitUtils.hashForFiltering(blockedKey).toString(), 0)
        val configuration =
            MockKitConfiguration.createKitConfiguration(
                JSONObject().put("id", 42).put("hs", JSONObject().put("ua", userAttributeFilters)),
            )
        val integration = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(integration.configuration).thenReturn(configuration)
        return integration
    }

    @Test
    fun testGetUserAttributesFiltersTheMap() {
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(user.userAttributes).thenReturn(allAttributes)
        val filteredUser = FilteredMParticleUser.getInstance(user, kitIntegration())

        Assert.assertEquals(allowedAttributes, filteredUser.userAttributes)
    }

    @Test
    fun testGetUserAttributesWithListenerFiltersTheReturnedMap() {
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito
            .`when`(user.getUserAttributes(Mockito.any(UserAttributeListenerType::class.java)))
            .thenReturn(allAttributes)
        val filteredUser = FilteredMParticleUser.getInstance(user, kitIntegration())

        Assert.assertEquals(allowedAttributes, filteredUser.getUserAttributes(noOpListener))
    }

    @Test
    fun testGetUserAttributesWithListenerReturnsNullWhenOnlyTheListenerIsCalled() {
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito
            .`when`(user.getUserAttributes(Mockito.any(UserAttributeListenerType::class.java)))
            .thenReturn(null)
        val filteredUser = FilteredMParticleUser.getInstance(user, kitIntegration())

        Assert.assertNull(filteredUser.getUserAttributes(noOpListener))
    }

    @Test
    fun testGetUserAttributesWithListenerFiltersTheListenerAndTheReturnedMap() {
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito
            .`when`(user.getUserAttributes(Mockito.any(UserAttributeListenerType::class.java)))
            .thenAnswer { invocation ->
                invocation
                    .getArgument<TypedUserAttributeListener>(0)
                    .onUserAttributesReceived(allAttributes, emptyMap(), 7L)
                allAttributes
            }
        val filteredUser = FilteredMParticleUser.getInstance(user, kitIntegration())
        var received: Map<String, Any?>? = null
        var receivedMpid: Long? = null
        val listener =
            object : TypedUserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, Any?>,
                    userAttributeLists: Map<String, List<String?>?>,
                    mpid: Long,
                ) {
                    received = userAttributes
                    receivedMpid = mpid
                }
            }

        val returned = filteredUser.getUserAttributes(listener)

        Assert.assertEquals(allowedAttributes, received)
        Assert.assertEquals(7L, receivedMpid)
        Assert.assertEquals(allowedAttributes, returned)
    }
}
