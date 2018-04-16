package com.mparticle.kits;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;
import com.mparticle.utils.Server;
import com.mparticle.utils.TestingUtils;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class FilteredIdentityCallbacksTest extends BaseKitManagerStarted {

    TestingUtils.OnFailureCallback onFailureCallback = new TestingUtils.OnFailureCallback() {
        @Override
        public void onPreFailure() {
            StringBuilder urls = new StringBuilder();
            urls.append("Starting MPID = " + mStartingMpid);
            for (ServeEvent serveEvent: mServer.getServer().getAllServeEvents()) {
                urls.append("\n"+serveEvent.getRequest().getUrl());
                urls.append(", response: " + serveEvent.getResponse().getBodyAsString());
            }
            fail(urls.toString());
        }
    };

    KitUserTestKit kitIntegration;

    @Override
    protected void before() throws Exception {
        super.before();
        KitUserTestKit.setOnKitCreate(new BaseTestKit.OnKitCreateListener() {
            @Override
            public void onKitCreate(BaseTestKit kitIntegration) {
                if (kitIntegration instanceof KitUserTestKit) {
                    FilteredIdentityCallbacksTest.this.kitIntegration = (KitUserTestKit)kitIntegration;
                }
            }
        });
        long endTime = System.currentTimeMillis() + (10 * 1000);
        while (kitIntegration == null && System.currentTimeMillis() < endTime) {}
        if (kitIntegration == null) {
            fail("Kit not started");
        }
    }

    @Override
    protected Map<String, JSONObject> registerCustomKits() {
        Map<String, JSONObject> customKits = new HashMap<>();
        customKits.put(KitUserTestKit.class.getName(), new JSONObject());
        return customKits;
    }


    @Test
    public void testOnIdentifyNoMpidChange() throws Exception {
        final boolean[] called = new boolean[1];

        mServer.addConditionalIdentityResponse(mStartingMpid, mStartingMpid);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mStartingMpid,(Long) mParticleUser.getId());
                called[0] = true;
            }
        });

        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(urlPathMatching("/v([0-9]*)/identify"), new Server.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                return jsonObject.optString("previous_mpid", "").equals(String.valueOf(mStartingMpid));
            }
        }, 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    @Test
    public void testOnIdentify() throws Exception {
        final boolean[] called = new boolean[1];
        final Long mpid1 = new Random().nextLong();
        final Long mpid2 = new Random().nextLong();
        MParticle.getInstance().getConfigManager().setMpid(mpid1);
        mServer.addConditionalIdentityResponse(mpid1, mpid2);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                if (mpid2 == mParticleUser.getId()) {
                    assertEquals(mpid2,(Long) mParticleUser.getId());
                    called[0] = true;
                }
            }
        });

        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(urlPathMatching("/v([0-9]*)/identify"), new Server.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                return jsonObject.optString("previous_mpid", "").equals(String.valueOf(mpid1));
            }
        }, 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    @Test
    public void testOnLogin() throws Exception {
        final boolean[] called = new boolean[1];
        final Long mpid2 = new Random().nextLong();
        mServer.addConditionalLoginResponse(mStartingMpid, mpid2);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mpid2,(Long) mParticleUser.getId());
                called[0] = true;
            }
                      });

        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/login")), 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    @Test
    public void testOnLoginNoMpidChange() throws Exception {
        final boolean[] called = new boolean[1];
        mServer.addConditionalLoginResponse(mStartingMpid, mStartingMpid);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mStartingMpid,(Long) mParticleUser.getId());
                called[0] = true;
            }
        });

        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/login")), 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    @Test
    public void testOnLogout() throws Exception {
        final boolean[] called = new boolean[1];
        final Long mpid2 = new Random().nextLong();
        mServer.addConditionalLogoutResponse(mStartingMpid, mpid2);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mpid2,(Long) mParticleUser.getId());
                called[0] = true;
            }
        });

        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/logout")), 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    @Test
    public void testOnLogoutNoMpidChange() throws Exception {
        final boolean[] called = new boolean[1];
        mServer.addConditionalLogoutResponse(mStartingMpid, mStartingMpid);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mStartingMpid,(Long) mParticleUser.getId());
                called[0] = true;
            }
                      });

        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/logout")), 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }


    @Test
    public void testOnUserModified() throws Exception {
        final boolean[] called = new boolean[1];
        mServer.setupHappyModify();

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
                assertEquals(mStartingMpid, (Long) mParticleUser.getId());
                called[0] = true;
            }
                      });

        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build());

        Logger.error("Waiting for Modify request");
        mServer.waitForVerify(postRequestedFor(urlPathMatching(String.format("/v([0-9]*)/%s/modify", mStartingMpid))), 5000);
        TestingUtils.checkAllBool(called, 1, 100, new TestingUtils.OnFailureCallback() {
            @Override
            public void onPreFailure() {
                StringBuilder urls = new StringBuilder();
                urls.append("Passed ? = " + called[0]);
                urls.append("Starting MPID = " + mStartingMpid);
                for (ServeEvent serveEvent: mServer.getServer().getAllServeEvents()) {
                    urls.append("\n"+serveEvent.getRequest().getUrl());
                    urls.append(", response: " + serveEvent.getResponse().getBodyAsString());
                }
                fail(urls.toString());
            }
        });
    }

    @Test
    public void testOnUserIdentified() throws Exception {
        final boolean[] called = new boolean[1];
        final Long mpid1 = new Random().nextLong();
        final Long mpid2 = new Random().nextLong();
        MParticle.getInstance().getConfigManager().setMpid(mpid1);
        mServer.addConditionalIdentityResponse(mpid1, mpid2);

        kitIntegration.setMParticleUserListener(new AbstractMPUserListener() {
            @Override
            public void onUserIdentified(MParticleUser mParticleUser) {
                if (mpid2 == mParticleUser.getId()) {
                    assertEquals(mpid2,(Long) mParticleUser.getId());
                    called[0] = true;
                }
            }
        });

        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(urlPathMatching("/v([0-9]*)/identify"), new Server.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                return jsonObject.optString("previous_mpid", "").equals(String.valueOf(mpid1));
            }
        }, 5000);
        TestingUtils.checkAllBool(called, 1, 100, onFailureCallback);
    }

    class AbstractMPUserListener implements KitIntegration.IdentityListener {

        @Override
        public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {

        }

        @Override
        public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {

        }

        @Override
        public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {

        }

        @Override
        public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {

        }

        @Override
        public void onUserIdentified(MParticleUser mParticleUser) {

        }
    }

    public static class KitUserTestKit extends BaseTestKit implements KitIntegration.IdentityListener {
        IdentityListener listener;

        public KitUserTestKit() {
            super();
        }

        @Override
        public boolean isDisabled() {
            return false;
        }

        public void setMParticleUserListener(IdentityListener listener) {
            this.listener = listener;
        }

        @Override
        public String getName() {
            return "TestKit";
        }

        @Override
        public List<ReportingMessage> setOptOut(boolean optedOut) {
            return null;
        }


        @Override
        public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
            if (listener != null) {
                listener.onIdentifyCompleted(mParticleUser, identityApiRequest);
            }
        }

        @Override
        public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
            listener.onLoginCompleted(mParticleUser, identityApiRequest);
        }

        @Override
        public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
            listener.onLogoutCompleted(mParticleUser, identityApiRequest);
        }

        @Override
        public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
            listener.onModifyCompleted(mParticleUser, identityApiRequest);
        }

        @Override
        public void onUserIdentified(MParticleUser mParticleUser) {
            listener.onUserIdentified(mParticleUser);
        }
    }


}
