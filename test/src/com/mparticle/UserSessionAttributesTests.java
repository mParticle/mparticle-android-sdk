package com.mparticle;

import static org.mockito.Mockito.mock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.test.AndroidTestCase;

import com.mparticle.Constants.MessageKey;
import com.mparticle.MParticleAPI.IdentityType;

public class UserSessionAttributesTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMockMessageManager = mock(MockableMessageManager.class);
        mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
        mMParticleAPI.clearUserAttributes();
    }

    public void testSetUserAttributes() throws JSONException {
        mMParticleAPI.setUserAttribute("testKey1", "testValue1");
        mMParticleAPI.setUserAttribute("testKey2", "testValue2");
        assertEquals("testValue1", mMParticleAPI.mUserAttributes.getString("testKey1"));
        assertEquals("testValue2", mMParticleAPI.mUserAttributes.getString("testKey2"));
    }
    
    public void testSetUserIdentity() throws JSONException {
    	// first remove all identities!
    	mMParticleAPI.mUserIdentities = new JSONArray();
    	
    	mMParticleAPI.setUserIdentity("tbreffni@mparticle.com", IdentityType.MICROSOFT);
    	
    	assertEquals("tbreffni@mparticle.com", ((JSONObject)mMParticleAPI.mUserIdentities.get(0)).get(MessageKey.IDENTITY_VALUE));
    	assertEquals(IdentityType.MICROSOFT.getValue(), ((JSONObject)mMParticleAPI.mUserIdentities.get(0)).get(MessageKey.IDENTITY_NAME));
    	
    	mMParticleAPI.setUserIdentity("me@myEmail.com", IdentityType.GOOGLE);
    	mMParticleAPI.setUserIdentity("myFacebook", IdentityType.FACEBOOK);
    	mMParticleAPI.setUserIdentity("555-555-5555", IdentityType.FACEBOOK);
    	
    	assertEquals(3, mMParticleAPI.mUserIdentities.length());
    	mMParticleAPI.setUserIdentity(null, IdentityType.FACEBOOK);
    	assertEquals(2, mMParticleAPI.mUserIdentities.length());
    	
    	int type = (Integer) ((JSONObject)mMParticleAPI.mUserIdentities.get(0)).get(MessageKey.IDENTITY_NAME);
    	if (type == IdentityType.MICROSOFT.getValue()) {
        	assertEquals("tbreffni@mparticle.com", ((JSONObject)mMParticleAPI.mUserIdentities.get(0)).get(MessageKey.IDENTITY_VALUE));
    	} else {
        	assertEquals("me@myEmail.com", ((JSONObject)mMParticleAPI.mUserIdentities.get(0)).get(MessageKey.IDENTITY_VALUE));
    	}
   }

    public void testTooManyAttributes() throws JSONException {
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserAttribute("testKey" + i, "testValue" + i);
        }
        mMParticleAPI.setUserAttribute("testKeyOverLimit", "testValue");

        assertEquals(Constants.LIMIT_ATTR_COUNT, mMParticleAPI.mUserAttributes.length());
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyOverLimit"));
    }

    public void testAttributesValueTooLarge() throws JSONException {
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length() < Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        mMParticleAPI.setUserAttribute("testKeyLongString", longString);

        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyLongString"));
    }

    public void testAttributesKeyTooLarge() throws JSONException {
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length() < Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }

        mMParticleAPI.setUserAttribute(longString, "testValue2");
        assertEquals(0, mMParticleAPI.mUserAttributes.length());
    }

    public void testUserAttributesShared() throws JSONException {
        mMParticleAPI.setUserAttribute("testKey1", "testValue1");
        MParticleAPI mParticleAPI2 = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
        assertEquals(mParticleAPI2.mUserAttributes.get("testKey1"), mMParticleAPI.mUserAttributes.get("testKey1"));
    }

    public void testClearUserAttribute() throws JSONException {
        mMParticleAPI.setUserAttribute("testKeyToClear", "testValue");
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserAttribute("testKey" + i, "testValue" + i);
        }
        assertEquals(Constants.LIMIT_ATTR_COUNT, mMParticleAPI.mUserAttributes.length());
        assertTrue(mMParticleAPI.mUserAttributes.has("testKeyToClear"));
        mMParticleAPI.setUserAttribute("testKeyToClear", null);
        assertFalse(mMParticleAPI.mUserAttributes.has("testKeyToClear"));
    }

    public void testUpdateUserAttribute() throws JSONException {
        mMParticleAPI.setUserAttribute("testKeyToUpdate", "testValue1");
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            mMParticleAPI.setUserAttribute("testKey" + i, "testValue" + i);
        }
        assertEquals(Constants.LIMIT_ATTR_COUNT, mMParticleAPI.mUserAttributes.length());
        assertTrue(mMParticleAPI.mUserAttributes.has("testKeyToUpdate"));
        mMParticleAPI.setUserAttribute("testKeyToUpdate", "testValueUpdated");
        assertEquals("testValueUpdated", mMParticleAPI.mUserAttributes.get("testKeyToUpdate"));
    }

    public void testSessionAttributesCleared() throws JSONException {
        mMParticleAPI.startActivity();
        mMParticleAPI.setSessionAttribute("testKey1", "testValue1");
        assertTrue(mMParticleAPI.mSessionAttributes.has("testKey1"));
        mMParticleAPI.newSession();
        assertFalse(mMParticleAPI.mSessionAttributes.has("testKey1"));
    }

}
