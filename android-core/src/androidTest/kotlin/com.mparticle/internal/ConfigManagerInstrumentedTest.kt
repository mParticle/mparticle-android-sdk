package com.mparticle.internal;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

import com.mparticle.Configuration;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseAbstractTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    public void testConfigResponseParsing() throws JSONException, InterruptedException {
        String token = mRandomUtils.getAlphaNumericString(20);
        int aliasMaxWindow = ran.nextInt();

        JSONObject config = new JSONObject()
                .put("wst", token)
                .put(ConfigManager.ALIAS_MAX_WINDOW, aliasMaxWindow);

        mServer.setupConfigResponse(config.toString());
        BothConfigsLoadedListener configLoadedListener = new BothConfigsLoadedListener();
        MPLatch latch = configLoadedListener.latch;

        startMParticle(MParticleOptions.builder(mContext).configuration(new AddConfigListener(configLoadedListener)));
        latch.await();

        assertEquals(token, MParticle.getInstance().Internal().getConfigManager().getWorkspaceToken());
        assertEquals(aliasMaxWindow, MParticle.getInstance().Internal().getConfigManager().getAliasMaxWindow());

        //test set defaults when fields are not present
        MParticle.setInstance(null);
        mServer.setupConfigResponse(new JSONObject().toString());
        configLoadedListener = new BothConfigsLoadedListener();
        latch = configLoadedListener.latch;

        startMParticle(MParticleOptions.builder(mContext).configuration(new AddConfigListener(configLoadedListener)));
        latch.await();

        assertEquals("", MParticle.getInstance().Internal().getConfigManager().getWorkspaceToken());
        assertEquals(90, MParticle.getInstance().Internal().getConfigManager().getAliasMaxWindow());
    }

    @Test
    public void cachedConfigLoadedExactlyOnce() throws InterruptedException, JSONException {
        MPLatch latch = new MPLatch(1);
        AndroidUtils.Mutable<Boolean> loadedCoreLocal = new AndroidUtils.Mutable<>(false);
        AndroidUtils.Mutable<Boolean> loadedKitLocal = new AndroidUtils.Mutable<>(false);

        setCachedConfig(getSimpleConfigWithKits());
        mServer.setupConfigDeferred();
        ConfigManager.ConfigLoadedListener configLoadedListener = new ConfigManager.ConfigLoadedListener() {
            @Override
            public void onConfigUpdated(ConfigManager.ConfigType configType, boolean isNew) {
                if (!isNew) {
                    switch (configType) {
                        case CORE:
                            if (loadedCoreLocal.value) {
                                fail("core config already loaded");
                            } else {
                                Logger.error("LOADED CACHED Core");
                                loadedCoreLocal.value = true;
                            }
                        case KIT:
                            if (loadedKitLocal.value) {
                                fail("kit config already loaded");
                            } else {
                                Logger.error("LOADED CACHED Kit");
                                loadedKitLocal.value = true;
                            }
                    }
                }
                if (loadedCoreLocal.value && loadedKitLocal.value) {
                    latch.countDown();

                }
                Logger.error("KIT = " + loadedKitLocal.value + " Core: " + loadedCoreLocal.value);
            }
        };
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .configuration(new AddConfigListener(configLoadedListener))
                .build();
        MParticle.start(options);

        //wait until both local configs are loaded
        latch.await();

        //try to coerce another load...
        new ConfigManager(mContext);
        MParticle instance = MParticle.getInstance();
        instance.logEvent(TestingUtils.getInstance().getRandomMPEventSimple());
        instance.setOptOut(true);
        instance.setOptOut(false);

        //and finally, load remote config
        mServer.setupConfigResponse(getSimpleConfigWithKits().toString());
        fetchConfig();
        BothConfigsLoadedListener bothConfigsLoadedListener = new BothConfigsLoadedListener();
        MPLatch reloadLatch = bothConfigsLoadedListener.latch;
        MParticle.getInstance().Internal().getConfigManager().addConfigUpdatedListener(bothConfigsLoadedListener);
        reloadLatch.await();
    }

    class BothConfigsLoadedListener implements ConfigManager.ConfigLoadedListener {
        Set<ConfigManager.ConfigType> types;
        MPLatch latch = new MPLatch(1);

        public BothConfigsLoadedListener(ConfigManager.ConfigType... configTypes) {
            if (configTypes == null || configTypes.length == 0) {
                configTypes = new ConfigManager.ConfigType[]{ConfigManager.ConfigType.CORE};
            }
            types = this.types = new HashSet<ConfigManager.ConfigType>(Arrays.asList(configTypes));
        }
        @Override
        public void onConfigUpdated(ConfigManager.ConfigType configType, boolean isNew) {
            if (isNew) {
                types.remove(configType);
            }
            if (types.size() == 0) {
                latch.countDown();
            }
        }
    }

    class AddConfigListener implements Configuration<ConfigManager> {
        ConfigManager.ConfigLoadedListener configLoadedListener;

        public AddConfigListener(ConfigManager.ConfigLoadedListener configLoadedListener) {
            this.configLoadedListener = configLoadedListener;
        }

        @Override
        public Class<ConfigManager> configures() {
            return ConfigManager.class;
        }

        @Override
        public void apply(ConfigManager configManager) {
            configManager.addConfigUpdatedListener(configLoadedListener);
        }
    }
}
