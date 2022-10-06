package com.mparticle.internal;

import android.content.Context;

import com.mparticle.InstallReferrerHelper;
import com.mparticle.mock.MockContext;

import org.junit.Test;

public class InstallReceiverHelperTest {

    @Test
    public void testNullInputs() throws Exception {
        Context context = new MockContext();
        InstallReferrerHelper.setInstallReferrer(null, "");
        InstallReferrerHelper.setInstallReferrer(context, "");
        InstallReferrerHelper.setInstallReferrer(null, null);
    }
}
