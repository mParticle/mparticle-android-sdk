package com.mparticle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import com.mparticle.Constants.PrefKeys;

public class InstallReferrerTests extends AndroidTestCase {

    private SharedPreferences mPrefs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPrefs = getContext().getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mPrefs.edit().remove(PrefKeys.INSTALL_REFERRER).commit();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mPrefs.edit().remove(PrefKeys.INSTALL_REFERRER).commit();
    }

    public void testRegistrationReceived() {
        final String TEST_REFERRER = "testArg1=testVal1&testArg2=testVal2";
        InstallReferrerTracker installReferrerTracker = new InstallReferrerTracker();
        Intent installIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        installIntent.putExtra("referrer", TEST_REFERRER);

        installReferrerTracker.onReceive(getContext(), installIntent);

        assertEquals(TEST_REFERRER, mPrefs.getString(PrefKeys.INSTALL_REFERRER, null));
    }

}
