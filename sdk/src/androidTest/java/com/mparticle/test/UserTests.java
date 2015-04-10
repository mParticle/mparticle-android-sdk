package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Test;
import org.junit.runner.RunWith;

public class UserTests extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MParticle.start(getContext());
    }

    @Test
    public void testUserAttributesAndTags() {
        JSONObject userAttributes = MParticle.getInstance().internal().getUserAttributes();

        assertNotNull(userAttributes);

        MParticle.getInstance().setUserAttribute("hair", "brown");

        try {
            assertEquals(userAttributes.get("hair"), "brown");
            MParticle.getInstance().removeUserAttribute("hair");
            assertNull(userAttributes.opt("hair"));
            MParticle.getInstance().setUserTag("whatever");
            assertNotNull(userAttributes.get("whatever"));
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testUserIdentities() {
        try{
            JSONArray identities = MParticle.getInstance().internal().getUserIdentities();
            assertNotNull(identities);
            MParticle.getInstance().setUserIdentity("other id", MParticle.IdentityType.Other);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Other)) {
                    assertEquals("other id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }
            MParticle.getInstance().setUserIdentity("customer id", MParticle.IdentityType.CustomerId);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.CustomerId)) {
                    assertEquals("customer id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().setUserIdentity("facebook id", MParticle.IdentityType.Facebook);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Facebook)) {
                    assertEquals("facebook id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().setUserIdentity("google id", MParticle.IdentityType.Google);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Google)) {
                    assertEquals("google id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().setUserIdentity("micro id", MParticle.IdentityType.Microsoft);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Microsoft)) {
                    assertEquals("micro id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().setUserIdentity("yahoo id", MParticle.IdentityType.Yahoo);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Yahoo)) {
                    assertEquals("yahoo id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().setUserIdentity("alias id", MParticle.IdentityType.Alias);
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Alias)) {
                    assertEquals("alias id", identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_VALUE));
                    break;
                }
            }

            MParticle.getInstance().removeUserIdentity("alias id");

            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).get(Constants.MessageKey.IDENTITY_NAME).equals(MParticle.IdentityType.Alias)) {
                    fail("ID didn't get removed!");
                    break;
                }
            }


        }catch (JSONException jse){fail(jse.getMessage());}

        }

}
