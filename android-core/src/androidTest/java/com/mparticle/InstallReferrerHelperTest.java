package com.mparticle;

import com.mparticle.internal.MPUtility;
import com.mparticle.utils.TestingUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InstallReferrerHelperTest extends BaseCleanStartedEachTest {

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {

    }

    @Test
    public void testFetchInstallReferrer() throws Exception {
        final boolean[] finished = {false};

        assertTrue(MPUtility.isInstallRefApiAvailable());
        InstallReferrerHelper.fetchInstallReferrer(mContext, new InstallReferrerHelper.InstallReferrerCallback() {
                    @Override
                    public void onReceived(String installReferrer) {
                        assertEquals("utm_source=google-play&utm_medium=organic", installReferrer);
                        fail();
                        finished[0] = true;
                    }

                    @Override
                    public void onFailed() {
                        finished[0] = true;
                    }
                }
        );
        TestingUtils.checkAllBool(finished, 1, 1);
    }
}
