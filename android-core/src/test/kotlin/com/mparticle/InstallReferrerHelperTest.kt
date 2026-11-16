package com.mparticle

import com.mparticle.internal.MPUtility
import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class InstallReferrerHelperTest {
    @Test
    fun testGetAndSetInstallReferrer() {
        Assert.assertNull(InstallReferrerHelper.getInstallReferrer(null))
        InstallReferrerHelper.setInstallReferrer(null, "ignored")

        val mockMp = Mockito.mock(MParticle::class.java)
        MParticle.setInstance(mockMp)
        try {
            val context = MockContext()
            Assert.assertNull(InstallReferrerHelper.getInstallReferrer(context))

            InstallReferrerHelper.setInstallReferrer(context, "utm_source=google&utm_medium=cpc")
            Assert.assertEquals(
                "utm_source=google&utm_medium=cpc",
                InstallReferrerHelper.getInstallReferrer(context),
            )
            Mockito.verify(mockMp).installReferrerUpdated()
        } finally {
            MParticle.setInstance(null)
        }
    }

    @Test
    fun testFetchInstallReferrerSkipsWhenAlreadyCachedOrCallsOnFailedWhenApiUnavailable() {
        val context = MockContext()
        val callback = Mockito.mock(InstallReferrerHelper.InstallReferrerCallback::class.java)

        InstallReferrerHelper.setInstallReferrer(context, "cached_referrer")
        InstallReferrerHelper.fetchInstallReferrer(context, callback)
        Mockito.verifyNoInteractions(callback)

        InstallReferrerHelper.setInstallReferrer(context, null)
        Mockito.mockStatic(MPUtility::class.java).use { mpUtilityMock ->
            mpUtilityMock.`when`<Boolean> { MPUtility.isInstallRefApiAvailable() }.thenReturn(false)
            InstallReferrerHelper.fetchInstallReferrer(context, callback)
            Mockito.verify(callback).onFailed()
        }
    }
}
