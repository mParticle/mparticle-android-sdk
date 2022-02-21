package com.mparticle;

import android.content.Context;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PushRegistrationTest extends BaseCleanStartedEachTest {

    //So other classes can use the test fields
    public void setContext(Context context) {
        mContext = context;
    }

    @Test
    public void testPushEnabledOnStartup() throws InterruptedException {
        MParticle.reset(mContext);
        final String newToken = mRandomUtils.getAlphaNumericString(30);
        startMParticle();
        TestingUtils.setFirebasePresent(true, newToken);
        final CountDownLatch latch = new MPLatch(1);
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                List<JSONObject> identitChanges = request.asIdentityRequest().getBody().identity_changes;
                assertEquals(1, identitChanges.size());
                try {
                    assertEquals(newToken, identitChanges.get(0).getString("new_value"));
                    latch.countDown();
                } catch (JSONException e) {
                    new RuntimeException(e);
                }
            }
        });
        MParticle.getInstance().Messaging().enablePushNotifications("12345");
        latch.await();
        TestingUtils.setFirebasePresent(false, null);

    }

    @Test
    public void testPushRegistrationSet() {
        assertEquals(mStartingMpid.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        for (SetPush setPush : setPushes) {
            PushRegistrationHelper.PushRegistration pushRegistration = new PushRegistrationHelper.PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(pushRegistration);

            for (GetPush getPush : getPushes) {
                PushRegistrationHelper.PushRegistration fetchedPushValue = getPush.getPushRegistration();
                String fetchedSenderId = fetchedPushValue.senderId;
                String fetchedInstanceId = fetchedPushValue.instanceId;
                if (!pushRegistration.senderId.equals(fetchedSenderId)) {
                    fail("Mismatch! When push value of \"" + pushRegistration.senderId + "\" is set with: " + setPush.getName() + ". A different value \"" + fetchedSenderId + "\" is returned with:" + getPush.getName());
                }
                if (!pushRegistration.instanceId.equals(fetchedInstanceId)) {
                    fail("Mismatch! When push value of \"" + pushRegistration.instanceId + "\" is set with: " + setPush.getName() + ". A different value \"" + fetchedInstanceId + "\" is returned with:" + getPush.getName());
                }
            }
        }
    }

    @Test
    public void testPushRegistrationCleared() {
        for (SetPush setPush : setPushes) {
            PushRegistrationHelper.PushRegistration pushRegistration = new PushRegistrationHelper.PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(pushRegistration);

            for (ClearPush clearPush : clearPushes) {
                clearPush.clearPush();

                for (GetPush getPush : getPushes) {
                    PushRegistrationHelper.PushRegistration fetchedPushRegistration = getPush.getPushRegistration();
                    if (fetchedPushRegistration != null && fetchedPushRegistration.instanceId != null && fetchedPushRegistration.senderId != null) {
                        fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.getName() + ", and cleared with: " + clearPush.getName() + ", the value is not null when fetched with:" + getPush.getName());
                    }
                }
            }
        }
    }

    @Test
    public void testPushRegistrationEnabledDisabled() {

        for (SetPush setPush: setPushes) {
            PushRegistrationHelper.PushRegistration pushRegistration = new PushRegistrationHelper.PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(pushRegistration);

            for (PushEnabled pushEnabled: pushEnableds) {
                if (!pushEnabled.isPushEnabled()) {
                    fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.getName() + ", push IS NOT enabled with:" + pushEnabled.getName());
                }
            }

            for (ClearPush clearPush: clearPushes) {
                clearPush.clearPush();

                for (PushEnabled pushEnabled: pushEnableds) {
                    if (pushEnabled.isPushEnabled()) {
                        fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.getName() + ", and cleared with: " + clearPush.getName() + ", push IS enabled with:" + pushEnabled.getName());
                    }
                }
            }
        }


    }

    public SetPush[] setPushes = new SetPush[]{
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
                    MParticle.getInstance().logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId);
                }

                @Override
                public String getName() {
                    return "MParticle.getInstance().logPushRegistration(senderId, instanceId)";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
                    MParticle.getInstance().Internal().getConfigManager().setPushRegistration(pushRegistration);
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushRegistration(pushRegistration())";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
                    MParticle.getInstance().Internal().getConfigManager().setPushSenderId(pushRegistration.senderId);
                    MParticle.getInstance().Internal().getConfigManager().setPushInstanceId(pushRegistration.instanceId);
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushSenderId(senderId) + ConfigManager.setPushRegistration(instanceId)";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
                    //For enablePushNotifications() to set the push registration, we need to mimic
                    //the Firebase dependency, and clear the push-fetched flags
                    TestingUtils.setFirebasePresent(true, pushRegistration.instanceId);
                    MParticle.getInstance().Messaging().enablePushNotifications(pushRegistration.senderId);
                    //this method setting push is async, so wait for confirmation before continuing
                    ConfigManager configManager = ConfigManager.getInstance(mContext);
                    while (!configManager.isPushEnabled()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    TestingUtils.setFirebasePresent(false, null);
                }

                @Override
                public String getName() {
                    return "MessagingApi.enablePushNotification(senderId)";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
                    MParticle.setInstance(null);
                    try {
                        startMParticle(MParticleOptions.builder(mContext).pushRegistration(pushRegistration.instanceId, pushRegistration.senderId));
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public String getName() {
                    return "MParticleOptions.pushRegistration(instanceId, senderId)";
                }
            }
    };

    public ClearPush[] clearPushes = new ClearPush[] {
            new ClearPush() {
                @Override
                public void clearPush() {
                    MParticle.getInstance().Messaging().disablePushNotifications();
                }

                @Override
                public String getName() {
                    return "MessagingApi.disablePushNotifications";
                }
            },
            new ClearPush() {
                @Override
                public void clearPush() {
                    MParticle.getInstance().Internal().getConfigManager().setPushSenderId(null);
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushSenderId(null)";
                }
            },
            new ClearPush() {
                @Override
                public void clearPush() {
                    MParticle.getInstance().Internal().getConfigManager().setPushRegistration(null);
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushRegistration(null)";
                }
            },
            new ClearPush() {
                @Override
                public void clearPush() {
                    MParticle.getInstance().Internal().getConfigManager().setPushRegistration(new PushRegistrationHelper.PushRegistration("instanceId", null));
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushRegistration(PushRegistration(\"instanceId\", null))";
                }
            },
            new ClearPush() {
                @Override
                public void clearPush() {
                    MParticle.setInstance(null);
                    try {
                        startMParticle(MParticleOptions.builder(mContext).pushRegistration(null, null));
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public String getName() {
                    return "startMParticle(MParticleOptions.builder(mContext).pushRegistration(null, null))";
                }
            }
    };

    public GetPush[] getPushes = new GetPush[] {
            new GetPush() {
                @Override
                public PushRegistrationHelper.PushRegistration getPushRegistration() {
                    String senderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId();
                    String instanceId = MParticle.getInstance().Internal().getConfigManager().getPushInstanceId();
                    return new PushRegistrationHelper.PushRegistration(instanceId, senderId);
                }

                @Override
                public String getName() {
                    return "ConfigManager.getPushSenderId() + ConfigManager.getPushInstanceId()";
                }
            },
            new GetPush() {
                @Override
                public PushRegistrationHelper.PushRegistration getPushRegistration() {
                    return PushRegistrationHelper.getLatestPushRegistration(mContext);
                }

                @Override
                public String getName() {
                    return "PushRegistrationHelper.getLatestPushRegistration(context)";
                }
            },
            new GetPush() {
                @Override
                public PushRegistrationHelper.PushRegistration getPushRegistration() {
                    return MParticle.getInstance().Internal().getConfigManager().getPushRegistration();
                }

                @Override
                public String getName() {
                    return "ConfigManager.getPushRegistration()";
                }
            }
    };

    public PushEnabled[] pushEnableds = new PushEnabled[] {
            new PushEnabled() {
                @Override
                public Boolean isPushEnabled() {
                    return MParticle.getInstance().Internal().getConfigManager().isPushEnabled();
                }

                @Override
                public String getName() {
                    return "ConfigManager.isPushEnabled()";
                }
            }
    };


    public interface SynonymousMethod {
        String getName();
    }

    public interface SetPush extends SynonymousMethod {
        void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration);
    }

    public interface ClearPush extends SynonymousMethod {
        void clearPush();
    }

    public interface GetPush extends SynonymousMethod {
        PushRegistrationHelper.PushRegistration getPushRegistration();
    }

    public interface PushEnabled extends SynonymousMethod {
        Boolean isPushEnabled();
    }

    PushRegistrationTest setServer(MockServer server) {
        mServer = server;
        return this;
    }
}
