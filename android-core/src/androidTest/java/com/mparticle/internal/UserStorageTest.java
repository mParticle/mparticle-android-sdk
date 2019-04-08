package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class UserStorageTest extends BaseCleanStartedEachTest {

    @Before
    public void before() {
        MParticle.reset(mContext);
    }

    @Test
    public void testSetFirstSeenTime() throws InterruptedException {
        UserStorage storage = UserStorage.create(mContext, 1);
        long firstSeen = System.currentTimeMillis();
        storage.setFirstSeenTime(firstSeen);


        assertEquals(storage.getFirstSeenTime(), firstSeen);

        //make sure that the firstSeenTime does not update if it has already been set
        storage.setFirstSeenTime(10L);
        assertEquals(firstSeen, storage.getFirstSeenTime());
    }

    @Test
    public void testSetLastSeenTime() throws InterruptedException {
        UserStorage storage = UserStorage.create(mContext, 2);
        long time = System.currentTimeMillis();
        storage.setLastSeenTime(time);
        assertEquals(time, storage.getLastSeenTime());
    }

    @Test
    public void testMigrate() {
        //test this a number of times, since it will be different everytime
        for (int i = 0; i < 50; i++) {
            testRandomMigration();
        }
    }

    private void testRandomMigration() {
        int numberFieldsChanged = mRandomUtils.randomInt(0, 11);
        Set<Integer> indexsToSetProfile1Field = new HashSet<Integer>();

        //select between 0 and 11 fields that should be set in the subjectUserConfig
        for (int  i = 0; i < numberFieldsChanged; i++) {
            int random = mRandomUtils.randomInt(0,10);
            if (indexsToSetProfile1Field.contains(random)) {
                i--;
                continue;
            } else {
                indexsToSetProfile1Field.add(random);
            }
        }

        ConfigManagerMigrationTest.refreshProfile1();
        ConfigManagerMigrationTest.refreshProfile2();

        UserStorage subjectUserStorage = UserStorage.create(mContext, ran.nextLong());
        UserStorage targetUserStorage = UserStorage.create(mContext, ran.nextLong());

        ConfigManagerMigrationTest.setProfile2(targetUserStorage);

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
                    userStorage.setBreadcrumbLimit(ConfigManagerMigrationTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionForeground(ConfigManagerMigrationTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setDeletedUserAttributes(ConfigManagerMigrationTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLastUseDate(ConfigManagerMigrationTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionId(ConfigManagerMigrationTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionStart(ConfigManagerMigrationTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLtv(ConfigManagerMigrationTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setTotalRuns(ConfigManagerMigrationTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setCookies(ConfigManagerMigrationTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLaunchesSinceUpgrade(ConfigManagerMigrationTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setUserIdentities(ConfigManagerMigrationTest.userIdentities);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLoggedInUser(ConfigManagerMigrationTest.knownIdentity);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), ConfigManagerMigrationTest.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), ConfigManagerMigrationTest.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), ConfigManagerMigrationTest.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), ConfigManagerMigrationTest.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), ConfigManagerMigrationTest.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), ConfigManagerMigrationTest.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), ConfigManagerMigrationTest.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), ConfigManagerMigrationTest.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), ConfigManagerMigrationTest.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), ConfigManagerMigrationTest.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), ConfigManagerMigrationTest.userIdentities);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.isLoggedIn(), ConfigManagerMigrationTest.knownIdentity);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet2 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), ConfigManagerMigrationTest.breadcrumbLimit2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), ConfigManagerMigrationTest.previousForeground2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), ConfigManagerMigrationTest.deletedUserAttributes2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), ConfigManagerMigrationTest.lastUseDate2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), ConfigManagerMigrationTest.previousSessionId2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), ConfigManagerMigrationTest.previousStart2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), ConfigManagerMigrationTest.ltv2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), ConfigManagerMigrationTest.totalRuns2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), ConfigManagerMigrationTest.cookies2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), ConfigManagerMigrationTest.launchesSinceUpgrade2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), ConfigManagerMigrationTest.userIdentities2);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.isLoggedIn(), ConfigManagerMigrationTest.knownIdentity);
                }
            }
    };
}
