package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseAbstractTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ConfigManagerInstrumentedTest extends BaseAbstractTest {

    @Test
    public void testSetMpidCurrentUserState() throws InterruptedException {
        final Long mpid1 = ran.nextLong();
        final Long mpid2 = ran.nextLong();
        final Long mpid3 = ran.nextLong();

        startMParticle();

        ConfigManager configManager = MParticle.getInstance().Internal().getConfigManager();

        assertEquals(mStartingMpid.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        assertEquals(mStartingMpid.longValue(), configManager.getMpid());

        configManager.setMpid(mpid1, ran.nextBoolean());
        assertEquals(mpid1.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());

        boolean newIsLoggedIn = !MParticle.getInstance().Identity().getCurrentUser().isLoggedIn();

        configManager.setMpid(mpid1, newIsLoggedIn);
        assertEquals(mpid1.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        assertEquals(newIsLoggedIn, MParticle.getInstance().Identity().getCurrentUser().isLoggedIn());

        configManager.setMpid(mpid2, false);
        assertEquals(mpid2.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        assertFalse(MParticle.getInstance().Identity().getCurrentUser().isLoggedIn());

        configManager.setMpid(mpid2, true);
        assertEquals(mpid2.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        assertTrue(MParticle.getInstance().Identity().getCurrentUser().isLoggedIn());

        configManager.setMpid(mpid3, true);
        assertEquals(mpid3.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        assertTrue(MParticle.getInstance().Identity().getCurrentUser().isLoggedIn());
    }

    @Test
    public void testConfigParsing() throws JSONException, InterruptedException {
        String token = mRandomUtils.getAlphaNumericString(20);
        int aliasMaxWindow = ran.nextInt();

        JSONObject config = new JSONObject()
                .put("wst", token)
                .put(ConfigManager.ALIAS_MAX_WINDOW, aliasMaxWindow);

        mServer.setupConfigResponse(config.toString());

        startMParticle();
        assertEquals(token, MParticle.getInstance().Internal().getConfigManager().getWorkspaceToken());
        assertEquals(aliasMaxWindow, MParticle.getInstance().Internal().getConfigManager().getAliasMaxWindow());

        //test set defaults when fields are not present
        MParticle.setInstance(null);
        mServer.setupConfigResponse(new JSONObject().toString());
        startMParticle();
        assertEquals("", MParticle.getInstance().Internal().getConfigManager().getWorkspaceToken());
        assertEquals(90, MParticle.getInstance().Internal().getConfigManager().getAliasMaxWindow());

    }
}
