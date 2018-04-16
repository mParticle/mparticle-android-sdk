package com.mparticle.kits;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.mparticle.BaseCleanInstallEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.ReportingManager;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public abstract class BaseKitManagerStarted extends BaseCleanInstallEachTest {
    private static Map<Integer, String> mCustomTestKits;
    protected Long mStartingMpid;
    protected KitManagerImpl mKitManager;

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {
        mStartingMpid = new Random().nextLong();
        MParticleUtils.clear();
        setupConfigMessageForKits(registerCustomKits());
        new ConfigManager(mContext, null, null, null).setMpid(mStartingMpid);
        mServer.setupHappyIdentify(mStartingMpid);
        MParticle.setInstance(null);
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build());
        mKitManager = new CustomKitManagerImpl(mContext, com.mparticle.AccessUtils.getMessageManager(), MParticle.getInstance().getConfigManager(), MParticle.getInstance().getAppStateManager(), AccessUtils.getUploadHandler());
        mKitManager.setKitFactory(new CustomKitFactory());
        AccessUtils.setKitManager(mKitManager);
    }

    //implementing this method will both register your custom kit, and start it via modifying the
    //config response to contains an "eks" message with the kit's id
    protected abstract Map<String, JSONObject> registerCustomKits();

    private void setupConfigMessageForKits(Map<String, JSONObject> kitIds) {
        JSONArray eks = new JSONArray();
        int i = -1;
        mCustomTestKits = new HashMap<>();
        for (Map.Entry<String, JSONObject> kitConfig: kitIds.entrySet()) {
            try {
                mCustomTestKits.put(i, kitConfig.getKey());
                JSONObject configJson = new JSONObject();
                if (kitConfig.getValue() != null) {
                    configJson = kitConfig.getValue();
                }
                configJson.put("id", i);
                eks.put(configJson);
            } catch (JSONException e) {
                throw new RuntimeException(String.format("Kit class %s unable to be set", kitConfig.getKey()));
            }
            i--;
        }
        try {
            JSONObject configObject = new JSONObject().put("eks", eks);
            mServer.setupConfigResponse(configObject.toString(), 2000);
        } catch (JSONException e) {
            throw new RuntimeException("Error sending custom eks to config");
        }
    }

    //this is a non-anonymous class only for the purpose of debugging
    class CustomKitManagerImpl extends KitManagerImpl {

        public CustomKitManagerImpl(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager, BackgroundTaskHandler backgroundTaskHandler) {
            super(context, reportingManager, configManager, appStateManager, backgroundTaskHandler);
        }
    }

    class CustomKitFactory extends KitIntegrationFactory {

        @Override
        protected Map<Integer, String> getKnownIntegrations() {
            Map<Integer, String> kitIntegration = super.getKnownIntegrations();
            if (mCustomTestKits != null) {
                for (Integer key: mCustomTestKits.keySet()) {
                    if (kitIntegration.containsKey(key)) {
                        throw new RuntimeException(String.format("Key value %d is already an existing kit, use a unique kitId for tests, please", key));
                    }
                }
                kitIntegration.putAll(mCustomTestKits);
            }
            return kitIntegration;
        }
    }
}
