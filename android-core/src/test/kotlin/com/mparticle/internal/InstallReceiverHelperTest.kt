package com.mparticle.internal

import android.content.Context
import com.mparticle.mock.MockContext
import com.mparticle.InstallReferrerHelper
import org.junit.Test
import java.lang.Exception

class InstallReceiverHelperTest {
    @Test
    @Throws(Exception::class)
    fun testNullInputs() {
        val context: Context = MockContext()
        InstallReferrerHelper.setInstallReferrer(context, "")
        InstallReferrerHelper.setInstallReferrer(context, null)
    }
}