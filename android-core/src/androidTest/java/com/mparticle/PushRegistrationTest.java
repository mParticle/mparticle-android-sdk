package com.mparticle;

import android.content.Context;

import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.internal.PushRegistrationHelper.PushRegistration;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PushRegistrationTest extends BaseCleanStartedEachTest {

    //So other classes can use the test fields
    public void setContext(Context context) {
        mContext = context;
    }

    @Test
    public void testPushRegistrationSet() {
        assertEquals(mStartingMpid.longValue(), MParticle.getInstance().Identity().getCurrentUser().getId());
        for (SetPush setPush : setPushes) {
            PushRegistration pushRegistration = new PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(pushRegistration);

            for (GetPush getPush : getPushes) {
                PushRegistration fetchedPushValue = getPush.getPushRegistration();
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
            PushRegistration pushRegistration = new PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(pushRegistration);

            for (ClearPush clearPush : clearPushes) {
                clearPush.clearPush();

                for (GetPush getPush : getPushes) {
                    PushRegistration fetchedPushRegistration = getPush.getPushRegistration();
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
            PushRegistration pushRegistration = new PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
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
                public void setPushRegistration(PushRegistration pushRegistration) {
                    MParticle.getInstance().logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId);
                }

                @Override
                public String getName() {
                    return "MParticle.getInstance().logPushRegistration(senderId, instanceId)";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistration pushRegistration) {
                    MParticle.getInstance().Internal().getConfigManager().setPushRegistration(pushRegistration);
                }

                @Override
                public String getName() {
                    return "ConfigManager.setPushRegistration(pushRegistration())";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistration pushRegistration) {
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
                public void setPushRegistration(PushRegistration pushRegistration) {
                    MParticle.getInstance().Messaging().enablePushNotifications(pushRegistration.senderId);

                    //This is mimicking us fetching an instance. Calling PushRegistrationHelper.setInstance() is what would really be called,
                    //but it would override the senderId write in the previous method call, which is what we are really testing
                    MParticle.getInstance().Internal().getConfigManager().setPushInstanceId(pushRegistration.instanceId);
                }

                @Override
                public String getName() {
                    return "MessagingApi.enablePushNotification(senderId)";
                }
            },
            new SetPush() {
                @Override
                public void setPushRegistration(PushRegistration pushRegistration) {
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
                    MParticle.getInstance().Internal().getConfigManager().setPushRegistration(new PushRegistration("instanceId", null));
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
                    return null;
                }
            }
    };

    public GetPush[] getPushes = new GetPush[] {
            new GetPush() {
                @Override
                public PushRegistration getPushRegistration() {
                    String senderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId();
                    String instanceId = MParticle.getInstance().Internal().getConfigManager().getPushInstanceId();
                    return new PushRegistration(instanceId, senderId);
                }

                @Override
                public String getName() {
                    return "ConfigManager.getPushSenderId() + ConfigManager.getPushInstanceId()";
                }
            },
            new GetPush() {
                @Override
                public PushRegistration getPushRegistration() {
                    return PushRegistrationHelper.getLatestPushRegistration(mContext);
                }

                @Override
                public String getName() {
                    return "PushRegistrationHelper.getLatestPushRegistration(context)";
                }
            },
            new GetPush() {
                @Override
                public PushRegistration getPushRegistration() {
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
        void setPushRegistration(PushRegistration pushRegistration);
    }

    public interface ClearPush extends SynonymousMethod {
        void clearPush();
    }

    public interface GetPush extends SynonymousMethod {
        PushRegistration getPushRegistration();
    }

    public interface PushEnabled extends SynonymousMethod {
        Boolean isPushEnabled();
    }

    PushRegistrationTest setServer(MockServer server) {
        mServer = server;
        return this;
    }
}
