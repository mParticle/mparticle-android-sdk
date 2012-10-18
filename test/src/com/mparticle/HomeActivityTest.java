package com.mparticle;

import android.test.ActivityInstrumentationTestCase2;

import com.mparticle.MParticleAPI;
import com.mparticle.demo.HomeActivity;

public class HomeActivityTest extends ActivityInstrumentationTestCase2<HomeActivity> {

    private HomeActivity mActivity;
    private MParticleAPI mParticleAPI;

    public HomeActivityTest() {
        super(HomeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();

      setActivityInitialTouchMode(false);
      mActivity = getActivity();
      mParticleAPI = MParticleAPI.getInstance(mActivity, "01234567890123456789012345678901", "secret");

    }

    public void testPreConditions() {
        assertTrue(MParticleAPI.debugMode);
        assertFalse(mParticleAPI.getOptOut());
    }

}
