package com.mparticle.internal;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.utils.RandomUtils;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class UserConfigTest extends BaseCleanStartedEachTest {

    MParticle instance;
    Context mContext;

    @Override
    protected void beforeClass() throws Exception {
    }

    @Override
    protected void before() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        instance = MParticle.getInstance();
    }

    @Test
    public void testMigrate() {
        //test this a number of times, since it will be different everytime
        for (int i = 0; i < 50; i++) {
            testRandomMigration();
        }
    }

    private void testRandomMigration() {
        int numberFieldsChanged = RandomUtils.getInstance().randomInt(0, 11);
        Set<Integer> indexsToSetProfile1Field = new HashSet<Integer>();

        //select between 0 and 11 fields that should be set in the subjectUserConfig
        for (int  i = 0; i < numberFieldsChanged; i++) {
            int random = RandomUtils.getInstance().randomInt(0,10);
            if (indexsToSetProfile1Field.contains(random)) {
                i--;
                continue;
            } else {
                indexsToSetProfile1Field.add(random);
            }
        }

        ConfigManagerTest.refreshProfile1();
        ConfigManagerTest.refreshProfile2();

        UserConfig subjectUserConfig = UserConfig.getUserConfig(mContext, new Random().nextLong());
        UserConfig targetUserConfig = UserConfig.getUserConfig(mContext, new Random().nextLong());

        ConfigManagerTest.setProfile2(targetUserConfig);

        //set the random number of fields in the subjectUserCondif
        for (Integer index: indexsToSetProfile1Field) {
            setFieldsProfile1[index].run(subjectUserConfig);
        }

        targetUserConfig.merge(subjectUserConfig);


        // if the field was set in subjectUserConfig, check if the value was merged into the
        // targetUserConfig, otherwise, check if it retained the value that was set there previously
        for (int i = 0; i <setFieldsProfile1.length; i++) {
            if (indexsToSetProfile1Field.contains(i)) {
                testFieldsForSet1[i].run(targetUserConfig);
            } else {
                testFieldsForSet2[i].run(targetUserConfig);
            }
        }

    }


    interface UserConfigRunnable {
        void run(UserConfig userConfig);
    }

    UserConfigRunnable[] setFieldsProfile1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setBreadcrumbLimit(ConfigManagerTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setPreviousSessionForeground(ConfigManagerTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setDeletedUserAttributes(ConfigManagerTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setLastUseDate(ConfigManagerTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setPreviousSessionId(ConfigManagerTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setPreviousSessionStart(ConfigManagerTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setLtv(ConfigManagerTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setTotalRuns(ConfigManagerTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setCookies(ConfigManagerTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setLaunchesSinceUpgrade(ConfigManagerTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    userConfig.setUserIdentities(ConfigManagerTest.userIdentities);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getBreadcrumbLimit(), ConfigManagerTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionForegound(), ConfigManagerTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getDeletedUserAttributes(), ConfigManagerTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLastUseDate(), ConfigManagerTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionId(), ConfigManagerTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionStart(-1), ConfigManagerTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLtv(), ConfigManagerTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getTotalRuns(-1), ConfigManagerTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getCookies(), ConfigManagerTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLaunchesSinceUpgrade(), ConfigManagerTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getUserIdentities(), ConfigManagerTest.userIdentities);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet2 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getBreadcrumbLimit(), ConfigManagerTest.breadcrumbLimit2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionForegound(), ConfigManagerTest.previousForeground2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getDeletedUserAttributes(), ConfigManagerTest.deletedUserAttributes2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLastUseDate(), ConfigManagerTest.lastUseDate2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionId(), ConfigManagerTest.previousSessionId2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getPreviousSessionStart(-1), ConfigManagerTest.previousStart2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLtv(), ConfigManagerTest.ltv2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getTotalRuns(-1), ConfigManagerTest.totalRuns2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getCookies(), ConfigManagerTest.cookies2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getLaunchesSinceUpgrade(), ConfigManagerTest.launchesSinceUpgrade2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserConfig userConfig) {
                    assertEquals(userConfig.getUserIdentities(), ConfigManagerTest.userIdentities2);
                }
            }
    };
}
