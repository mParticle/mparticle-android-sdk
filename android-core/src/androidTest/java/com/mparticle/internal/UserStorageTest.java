package com.mparticle.internal;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.RandomUtils;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class UserStorageTest extends BaseCleanStartedEachTest {

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

        UserStorage subjectUserStorage = UserStorage.create(mContext, new Random().nextLong());
        UserStorage targetUserStorage = UserStorage.create(mContext, new Random().nextLong());

        ConfigManagerTest.setProfile2(targetUserStorage);

        //set the random number of fields in the subjectUserCondif
        for (Integer index: indexsToSetProfile1Field) {
            setFieldsProfile1[index].run(subjectUserStorage);
        }

        targetUserStorage.merge(subjectUserStorage);


        // if the field was set in subjectUserConfig, check if the value was merged into the
        // targetUserConfig, otherwise, check if it retained the value that was set there previously
        for (int i = 0; i <setFieldsProfile1.length; i++) {
            if (indexsToSetProfile1Field.contains(i)) {
                testFieldsForSet1[i].run(targetUserStorage);
            } else {
                testFieldsForSet2[i].run(targetUserStorage);
            }
        }

    }


    interface UserConfigRunnable {
        void run(UserStorage userStorage);
    }

    UserConfigRunnable[] setFieldsProfile1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setBreadcrumbLimit(ConfigManagerTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionForeground(ConfigManagerTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setDeletedUserAttributes(ConfigManagerTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLastUseDate(ConfigManagerTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionId(ConfigManagerTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionStart(ConfigManagerTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLtv(ConfigManagerTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setTotalRuns(ConfigManagerTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setCookies(ConfigManagerTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLaunchesSinceUpgrade(ConfigManagerTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setUserIdentities(ConfigManagerTest.userIdentities);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), ConfigManagerTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), ConfigManagerTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), ConfigManagerTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), ConfigManagerTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), ConfigManagerTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), ConfigManagerTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), ConfigManagerTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), ConfigManagerTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), ConfigManagerTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), ConfigManagerTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), ConfigManagerTest.userIdentities);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet2 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), ConfigManagerTest.breadcrumbLimit2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), ConfigManagerTest.previousForeground2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), ConfigManagerTest.deletedUserAttributes2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), ConfigManagerTest.lastUseDate2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), ConfigManagerTest.previousSessionId2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), ConfigManagerTest.previousStart2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), ConfigManagerTest.ltv2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), ConfigManagerTest.totalRuns2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), ConfigManagerTest.cookies2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), ConfigManagerTest.launchesSinceUpgrade2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), ConfigManagerTest.userIdentities2);
                }
            }
    };
}
