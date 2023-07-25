package com.mparticle.identity;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitManager;
import com.mparticle.networking.BaseNetworkConnection;
import com.mparticle.networking.MPUrl;
import com.mparticle.networking.MParticleBaseClient;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccessUtils {

    public static String IDENTIFY_PATH = MParticleIdentityClientImpl.IDENTIFY_PATH;
    public static String LOGIN_PATH = MParticleIdentityClientImpl.LOGIN_PATH;
    public static String LOGOUT_PATH = MParticleIdentityClientImpl.LOGOUT_PATH;
    public static String MODIFY_PATH = MParticleIdentityClientImpl.MODIFY_PATH;

    public static MPUrl getUrl(String endpoint) {
        MParticleIdentityClientImpl identityClient = getIdentityApiClient();
        try {
            return identityClient.getUrl(endpoint);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static MPUrl getUrl(String endpoint, Long mpid) {
        MParticleIdentityClientImpl identityClient = getIdentityApiClient();
        try {
            return identityClient.getUrl(mpid, endpoint);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static MParticleIdentityClientImpl getIdentityApiClient() {
        MParticleBaseClient identityClient = null;
        if (MParticle.getInstance() != null) {
            identityClient = MParticle.getInstance().Identity().getApiClient();
        }
        if (identityClient == null) {
            Context context = InstrumentationRegistry.getInstrumentation().getContext();
            ConfigManager configManager = null;
            if (MParticle.getInstance() != null) {
                MParticle.getInstance().Internal().getConfigManager();
            }
            if (configManager == null) {
                configManager = new ConfigManager(context);
            }
            return getIdentityClient(context, configManager);
        }
        return (MParticleIdentityClientImpl) identityClient;
    }

    private static MParticleIdentityClientImpl getIdentityClient(Context context, ConfigManager configManager) {
        return new MParticleIdentityClientImpl(context, configManager, MParticle.OperatingSystem.FIRE_OS);
    }

    public static void setIdentityApiClient(MParticleIdentityClient identityClient) {
        MParticle.getInstance().Identity().setApiClient(identityClient);
    }

    public static void setDefaultIdentityApiClient(Context context) {
        MParticle.getInstance().Identity().setApiClient(null);
    }

    public static void setUserIdentity(String value, MParticle.IdentityType identityType, long mpid) {
        MParticle.getInstance().Identity().mUserDelegate.setUserIdentity(value, identityType, mpid);
    }

    public static void setUserIdentities(Map<MParticle.IdentityType, String> userIdentities, long mpid) {
        for (Map.Entry<MParticle.IdentityType, String> entry : userIdentities.entrySet()) {
            MParticle.getInstance().Identity().mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), mpid);
        }
    }

    public static class IdentityApiClient implements MParticleIdentityClient {

        @Override
        public IdentityHttpResponse login(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public IdentityHttpResponse logout(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public IdentityHttpResponse modify(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public BaseNetworkConnection getRequestHandler() {
            return null;
        }

        @Override
        public void setRequestHandler(BaseNetworkConnection handler) {
        }
    }

    public static void clearUserIdentities(MParticleUserImpl user) {
        Map<MParticle.IdentityType, String> nullUserIdentities = new HashMap<MParticle.IdentityType, String>();
        for (MParticle.IdentityType identityType : MParticle.IdentityType.values()) {
            nullUserIdentities.put(identityType, null);
        }
        user.setUserIdentities(nullUserIdentities);
    }

    public static String getIdentityTypeString(MParticle.IdentityType identityType) {
        return MParticleIdentityClientImpl.getStringValue(identityType);
    }

    public static void setKitManager(KitManager kitManager) {
        MParticle.getInstance().Identity().mKitManager = kitManager;
    }

    public static Set<IdentityStateListener> getIdentityStateListeners() {
        return MParticle.getInstance().Identity().identityStateListeners;
    }
}
