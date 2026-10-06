package com.mparticle.integrationtests;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.MParticleUser;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class UserTest extends BaselineTest {

    private static MParticleUser currentUser(MParticle mParticle) {
        MParticleUser user = mParticle.Identity().getCurrentUser();
        assertNotNull(user);
        return user;
    }

    @Test
    public void userAttributes() throws Exception {
        MParticle mParticle = start();
        MParticleUser user = currentUser(mParticle);
        user.setUserAttribute("string", "value");
        user.setUserAttribute("number", 7);
        user.setUserAttribute("decimal", 1.5);
        user.setUserAttribute("boolean", true);
        user.setUserAttributeList("list", Arrays.asList("x", "y"));
        user.setUserTag("tagged");
        user.incrementUserAttribute("number", 3);
        user.removeUserAttribute("decimal");
        mParticle.logEvent(new MPEvent.Builder("After Attributes").build());
        uploadAndVerify();
    }

    @Test
    public void consentState() throws Exception {
        MParticle mParticle = start();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("marketing", GDPRConsent.builder(true)
                        .document("privacy_v1")
                        .location("app")
                        .hardwareId("hw-1")
                        .timestamp(1700000000000L)
                        .build())
                .addGDPRConsentState("analytics", GDPRConsent.builder(false)
                        .timestamp(1700000000000L)
                        .build())
                .setCCPAConsentState(CCPAConsent.builder(false)
                        .document("ccpa_v1")
                        .location("app")
                        .timestamp(1700000000000L)
                        .build())
                .build();
        currentUser(mParticle).setConsentState(state);
        mParticle.logEvent(new MPEvent.Builder("After Consent").build());
        uploadAndVerify();
    }
}
