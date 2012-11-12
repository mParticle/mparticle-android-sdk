package com.mparticle;

import static org.mockito.Mockito.*;

import org.json.JSONException;

import android.test.AndroidTestCase;

public class UserSessionAttributesTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(MessageManager.class);
      mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
      mMParticleAPI.clearUserProperties();
    }

    public void testSetUserProperty() throws JSONException {
        mMParticleAPI.setUserProperty("testKey1", "testValue1");
        mMParticleAPI.setUserProperty("testKey2", "testValue2");
        assertEquals("testValue1",mMParticleAPI.mUserAttributes.getString("testKey1"));
        assertEquals("testValue2",mMParticleAPI.mUserAttributes.getString("testKey2"));
    }

    public void testTooManyAttributes() throws JSONException {
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserProperty("testKey"+i, "testValue"+i);
        }
        mMParticleAPI.setUserProperty("testKeyOverLimit","testValue");

        assertEquals(Constants.LIMIT_ATTR_COUNT,mMParticleAPI.mUserAttributes.length());
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyOverLimit"));
    }

    public void testAttributesValueTooLarge() throws JSONException {
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        mMParticleAPI.setUserProperty("testKeyLongString",longString);

        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyLongString"));
    }

    public void testAttributesKeyTooLarge() throws JSONException {
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }

        mMParticleAPI.setUserProperty(longString, "testValue2");
        assertEquals(0,mMParticleAPI.mUserAttributes.length());
    }

    public void testUserAttributesShared() throws JSONException {
        mMParticleAPI.setUserProperty("testKey1", "testValue1");
        MParticleAPI mParticleAPI2 = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
        assertEquals(mParticleAPI2.mUserAttributes.get("testKey1"), mMParticleAPI.mUserAttributes.get("testKey1"));
    }

    public void testClearUserAttribute() throws JSONException {
        mMParticleAPI.setUserProperty("testKeyToClear","testValue");
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserProperty("testKey"+i, "testValue"+i);
        }
        assertEquals(Constants.LIMIT_ATTR_COUNT,mMParticleAPI.mUserAttributes.length());
        assertTrue(mMParticleAPI.mUserAttributes.has("testKeyToClear"));
        mMParticleAPI.setUserProperty("testKeyToClear",null);
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyToClear"));
    }

    public void testUpdateUserAttribute() throws JSONException {
        mMParticleAPI.setUserProperty("testKeyToUpdate","testValue1");
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserProperty("testKey"+i, "testValue"+i);
        }
        assertEquals(Constants.LIMIT_ATTR_COUNT,mMParticleAPI.mUserAttributes.length());
        assertTrue(mMParticleAPI.mUserAttributes.has("testKeyToUpdate"));
        mMParticleAPI.setUserProperty("testKeyToUpdate","testValueUpdated");
        assertEquals("testValueUpdated", mMParticleAPI.mUserAttributes.get("testKeyToUpdate"));
    }

    public void testSessionAttributesCleared() throws JSONException {
        mMParticleAPI.start();
        mMParticleAPI.setSessionProperty("testKey1", "testValue1");
        assertTrue(mMParticleAPI.mSessionAttributes.has("testKey1"));
        mMParticleAPI.newSession();
        assertFalse(mMParticleAPI.mSessionAttributes.has("testKey1"));
    }

}
