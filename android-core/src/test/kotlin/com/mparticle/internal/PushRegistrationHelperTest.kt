package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class PushRegistrationHelperTest {
    @Test
    fun testPushRegistrationToStringAndEquals() {
        val reg1 = PushRegistration("inst-1", "send-1")
        Assert.assertEquals("inst-1", reg1.instanceId)
        Assert.assertEquals("send-1", reg1.senderId)
        Assert.assertEquals("[send-1, inst-1]", reg1.toString())

        val nullReg = PushRegistration(null, null)
        Assert.assertEquals("[null, null]", nullReg.toString())
        Assert.assertEquals(nullReg, PushRegistration(null, null))

        Assert.assertEquals(reg1, reg1)
        Assert.assertEquals(reg1, PushRegistration("inst-1", "send-1"))
        Assert.assertNotEquals(reg1, PushRegistration("inst-2", "send-1"))
        Assert.assertNotEquals(reg1, PushRegistration("inst-1", "send-2"))
        Assert.assertNotEquals(reg1, PushRegistration(null, "send-1"))
        Assert.assertNotEquals(reg1, PushRegistration("inst-1", null))
        Assert.assertFalse(reg1.equals(null))
        Assert.assertFalse(reg1.equals("not a PushRegistration"))
    }

    @Test
    fun testGetLatestPushRegistrationAndSetPushRegistration() {
        MParticle.setInstance(null)
        val context = MockContext()
        val configManager = ConfigManager(context)
        configManager.pushRegistration = PushRegistration("stored-token", "stored-sender")

        val latest = PushRegistrationHelper.getLatestPushRegistration(context)
        Assert.assertEquals(PushRegistration("stored-token", "stored-sender"), latest)

        PushRegistrationHelper.setPushRegistration(context, "", "sender-ignored")
        PushRegistrationHelper.setPushRegistration(context, null, "sender-ignored")
        Assert.assertFalse(ConfigManager(context).isPushRegistrationFetched)

        PushRegistrationHelper.setPushRegistration(context, "bg-token", "bg-sender")
        val updatedConfig = ConfigManager(context)
        Assert.assertTrue(updatedConfig.isPushRegistrationFetched)
        Assert.assertEquals(PushRegistration("bg-token", "bg-sender"), updatedConfig.pushRegistration)

        val mockMp = Mockito.mock(MParticle::class.java)
        val mockInternal = Mockito.mock(MParticle.Internal::class.java)
        Mockito.`when`(mockMp.Internal()).thenReturn(mockInternal)
        Mockito.`when`(mockInternal.configManager).thenReturn(updatedConfig)
        MParticle.setInstance(mockMp)
        try {
            PushRegistrationHelper.setPushRegistration(context, "fg-token", "fg-sender")
            Mockito.verify(mockMp).logPushRegistration("fg-token", "fg-sender")
        } finally {
            MParticle.setInstance(null)
        }
    }
}
