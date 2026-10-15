package com.mparticle

import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test

class MParticleOptionsTest {
    @Test
    fun testInternalRestartDoesNotCollectCustomModulePreferences() {
        Assert.assertTrue(MParticleOptions.builder(MockContext()).build().collectCustomModulePreferences)
        Assert.assertFalse(MParticleOptions.builder(MockContext()).buildForInternalRestart().collectCustomModulePreferences)
    }
}
