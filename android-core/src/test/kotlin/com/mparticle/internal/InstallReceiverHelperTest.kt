package com.mparticle.internal

import android.content.Context
import com.mparticle.InstallReferrerHelper
import com.mparticle.mock.MockContext
import org.junit.Test

class InstallReceiverHelperTest {
    @Test
    @Throws(Exception::class)
    fun testNullInputs() {
        val context: Context = MockContext()
        InstallReferrerHelper.setInstallReferrer(context, "")
        InstallReferrerHelper.setInstallReferrer(context, null)
    }
}
