package com.mparticle;

import static org.mockito.Mockito.*;

import com.mparticle.Constants.PrefKeys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

public class PushNotificationTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private SharedPreferences mPrefs;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(MockableMessageManager.class);
      mPrefs = getContext().getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
      mPrefs.edit().remove(PrefKeys.PUSH_REGISTRATION_ID).commit();
      mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    @Override
    protected void tearDown() throws Exception {
      super.tearDown();
      mPrefs.edit().remove(PrefKeys.PUSH_REGISTRATION_ID).commit();
    }

    private void setupInitialToken(String token) {
        mPrefs.edit().putString(PrefKeys.PUSH_REGISTRATION_ID, token).commit();
    }

    public void testSetPushRegistrationId() {
        final String TEST_REG_ID = "test-set-pushregistrationid";

        mMParticleAPI.setPushRegistrationId(TEST_REG_ID);

        verify(mMockMessageManager, times(1)).setPushRegistrationId(eq(TEST_REG_ID), eq(true));
        assertEquals(TEST_REG_ID,mPrefs.getString(PrefKeys.PUSH_REGISTRATION_ID,null));
    }

    public void testClearPushRegistrationId() {
        final String TEST_REG_ID = "test-cleartoken";
        setupInitialToken(TEST_REG_ID);

        mMParticleAPI.clearPushRegistrationId();

        verify(mMockMessageManager, times(1)).setPushRegistrationId(eq(TEST_REG_ID), eq(false));
        assertFalse(mPrefs.contains(PrefKeys.PUSH_REGISTRATION_ID));
    }

    public void testRegistrationReceived() {
        final String TEST_REG_ID = "test-registration-received";
        GCMIntentService gcmIntentService = new GCMIntentService(mMParticleAPI);
        Intent registrationReceivedIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        registrationReceivedIntent.putExtra("registration_id", TEST_REG_ID);

        gcmIntentService.onHandleIntent(registrationReceivedIntent);

        verify(mMockMessageManager, times(1)).setPushRegistrationId(eq(TEST_REG_ID), eq(true));
        assertEquals(TEST_REG_ID,mPrefs.getString(PrefKeys.PUSH_REGISTRATION_ID,null));
    }

    public void testUnregistrationReceived() {
        final String TEST_REG_ID = "test-unregister";
        setupInitialToken(TEST_REG_ID);

        GCMIntentService gcmIntentService = new GCMIntentService(mMParticleAPI);
        Intent registrationReceivedIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        registrationReceivedIntent.putExtra("unregistered", getContext().getPackageName());

        gcmIntentService.onHandleIntent(registrationReceivedIntent);

        verify(mMockMessageManager, times(1)).setPushRegistrationId(eq(TEST_REG_ID), eq(false));
        assertFalse(mPrefs.contains(PrefKeys.PUSH_REGISTRATION_ID));
    }

}
