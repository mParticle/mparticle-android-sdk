package com.mparticle;

import static org.mockito.Mockito.*;

import com.mparticle.Constants.PrefKeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

public class PushNotificationTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private SharedPreferences mPrefs;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(TestMessageManager.class);
      mPrefs = getContext().getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
      mPrefs.edit().remove(PrefKeys.OPTOUT+"TestAppKey").commit();
      mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    @Override
    protected void tearDown() throws Exception {
      super.tearDown();
      mPrefs.edit().remove(PrefKeys.OPTOUT+"TestAppKey").commit();
    }

    public void testSetPushRegistrationId() {
        mMParticleAPI.setPushRegistrationId("TOKEN1");
        verify(mMockMessageManager, times(1)).setPushRegistrationId(anyString(), anyLong(), anyLong(), eq("TOKEN1"), eq(true));
    }

    public void testClearPushRegistrationId() {
        mMParticleAPI.clearPushRegistrationId();
        verify(mMockMessageManager, times(1)).setPushRegistrationId(anyString(), anyLong(), anyLong(), anyString(), eq(false));
    }

}
