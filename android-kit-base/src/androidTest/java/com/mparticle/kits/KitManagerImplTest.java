package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertTrue;

public class KitManagerImplTest {

    @Test
    public void tokenTest() {
        assertTrue(true);
    }

}