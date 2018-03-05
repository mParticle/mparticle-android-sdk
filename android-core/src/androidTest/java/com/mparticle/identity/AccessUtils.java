package com.mparticle.identity;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.internal.networking.BaseNetworkConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AccessUtils {

    public static MParticleIdentityClient getIdentityApiClient() {
        return MParticle.getInstance().Identity().getApiClient();
    }

    public static void setIdentityApiClientProtocol(String https) {
        ((MParticleIdentityClientImpl)MParticle.getInstance().Identity().getApiClient()).overrideProtocol(https);
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
        for (Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
            MParticle.getInstance().Identity().mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), mpid);
        }
    }

    public static MParticleUser getUserInstance(Context context, long mpid) {
        return MParticleUser.getInstance(context, mpid, MParticle.getInstance().Identity().mUserDelegate);
    }

    public static class IdentityApiClient implements MParticleIdentityClient {

        @Override public IdentityHttpResponse login(IdentityApiRequest request) throws Exception { return null; }

        @Override public IdentityHttpResponse logout(IdentityApiRequest request) throws Exception { return null; }

        @Override public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception { return null;}

        @Override public IdentityHttpResponse modify(IdentityApiRequest request) throws Exception { return null;}

        @Override public BaseNetworkConnection getRequestHandler() { return null; }

        @Override public void setRequestHandler(BaseNetworkConnection handler) {}
    }

    public static void clearUserIdentities(MParticleUser user) {
        Map<MParticle.IdentityType, String> nullUserIdentities = new HashMap<MParticle.IdentityType, String>();
        for (MParticle.IdentityType identityType : MParticle.IdentityType.values()) {
            nullUserIdentities.put(identityType, null);
        }
        user.setUserIdentities(nullUserIdentities);
    }
}
