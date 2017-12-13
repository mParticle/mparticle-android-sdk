package com.mparticle;

import com.mparticle.internal.MPUtility;
import com.mparticle.utils.TestingUtils;

import org.junit.Test;

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

    /**
     * in the test case, when the InstallReferrerAPI is not available, the fetchInstallReferrer should
     * fail synchronously
     */
    @Test
    public void testFetchInstallReferrer() throws Exception {
        final boolean[] finished = {false};

        assertTrue(MPUtility.isInstallRefApiAvailable());
        InstallReferrerHelper.fetchInstallReferrer(mContext, new InstallReferrerHelper.InstallReferrerCallback() {
                    @Override
                    public void onReceived(String installReferrer) {
                        assertNull(installReferrer);
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
