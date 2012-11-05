package com.mparticle;

import static org.mockito.Mockito.*;

import org.json.JSONException;
import org.json.JSONObject;

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

    public void testSetUserProperties() throws JSONException {
        JSONObject props1=new JSONObject();
        props1.put("testKey1", "testValue1");
        props1.put("testKey2", "testValue2");
        JSONObject props2=new JSONObject();
        props2.put("testKey2", "testValue2-updated");
        props2.put("testKey3", "testValue3");
        mMParticleAPI.setUserProperties(props1);
        mMParticleAPI.setUserProperties(props2);
        assertEquals("testValue1",mMParticleAPI.mUserAttributes.getString("testKey1"));
        assertEquals("testValue2-updated",mMParticleAPI.mUserAttributes.getString("testKey2"));
        assertEquals("testValue3",mMParticleAPI.mUserAttributes.getString("testKey3"));
    }

    public void testTooManyAttributes() throws JSONException {
        JSONObject userProps=new JSONObject();
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            userProps.put("testKey"+i, "testValue"+i);
        }
        mMParticleAPI.setUserProperties(userProps);
        mMParticleAPI.setUserProperty("testKeyOverLimit","testValue");

        assertEquals(Constants.LIMIT_ATTR_COUNT,mMParticleAPI.mUserAttributes.length());
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyOverLimit"));
    }

    public void testAttributesValueTooLarge() throws JSONException {
        JSONObject userProps=new JSONObject();
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        userProps.put("testKey1", longString);
        mMParticleAPI.setUserProperties(userProps);
        mMParticleAPI.setUserProperty("testKeyLongString",longString);

        assertFalse(mMParticleAPI.mUserAttributes.has("testKey1"));
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyLongString"));
    }

    public void testAttributesKeyTooLarge() throws JSONException {
        JSONObject userProps=new JSONObject();
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        userProps.put(longString, "testValue1");
        mMParticleAPI.setUserProperties(userProps);
        assertEquals(0,mMParticleAPI.mUserAttributes.length());

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

    public void testUserIdentity() throws JSONException {
        mMParticleAPI.identifyUser("appUser1");
        mMParticleAPI.identifyUser("service1","appUser2");

        assertEquals("appUser1", mMParticleAPI.mUserAttributes.get("mp::id::"+"app"));
        assertEquals("appUser2", mMParticleAPI.mUserAttributes.get("mp::id::"+"service1"));
    }

    public void testSessionAttributesCleared() throws JSONException {
        mMParticleAPI.start();
        mMParticleAPI.setSessionProperty("testKey1", "testValue1");
        assertTrue(mMParticleAPI.mSessionAttributes.has("testKey1"));
        mMParticleAPI.newSession();
        assertFalse(mMParticleAPI.mSessionAttributes.has("testKey1"));
    }

}
