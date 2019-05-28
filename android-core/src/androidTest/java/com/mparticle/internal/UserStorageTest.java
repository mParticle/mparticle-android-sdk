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
        long startTime = System.currentTimeMillis();
        UserStorage storage = UserStorage.create(mContext, ran.nextLong());
        long firstSeen = storage.getFirstSeenTime();

        assertTrue(firstSeen >= startTime && firstSeen <= System.currentTimeMillis());

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

    ConfigManagerMigrationTest.UserStorageFields fields1;
    ConfigManagerMigrationTest.UserStorageFields fields2;

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

        fields1 = new ConfigManagerMigrationTest.UserStorageFields();
        fields2 = new ConfigManagerMigrationTest.UserStorageFields();

        UserStorage subjectUserStorage = UserStorage.create(mContext, ran.nextLong());
        UserStorage targetUserStorage = UserStorage.create(mContext, ran.nextLong());

        ConfigManagerMigrationTest.setProfile(targetUserStorage, fields2);

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
                    userStorage.setBreadcrumbLimit(fields1.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionForeground(fields1.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setDeletedUserAttributes(fields1.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLastUseDate(fields1.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionId(fields1.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setPreviousSessionStart(fields1.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLtv(fields1.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setTotalRuns(fields1.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setCookies(fields1.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLaunchesSinceUpgrade(fields1.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setUserIdentities(fields1.userIdentities);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    userStorage.setLoggedInUser(fields1.knownIdentity);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet1 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), fields1.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), fields1.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), fields1.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), fields1.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), fields1.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), fields1.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), fields1.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), fields1.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), fields1.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), fields1.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), fields1.userIdentities);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.isLoggedIn(), fields1.knownIdentity);
                }
            }
    };

    UserConfigRunnable[] testFieldsForSet2 = new UserConfigRunnable[]{
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getBreadcrumbLimit(), fields2.breadcrumbLimit);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionForegound(), fields2.previousForeground);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getDeletedUserAttributes(), fields2.deletedUserAttributes);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLastUseDate(), fields2.lastUseDate);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionId(), fields2.previousSessionId);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getPreviousSessionStart(-1), fields2.previousStart);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLtv(), fields2.ltv);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getTotalRuns(-1), fields2.totalRuns);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getCookies(), fields2.cookies);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getLaunchesSinceUpgrade(), fields2.launchesSinceUpgrade);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.getUserIdentities(), fields2.userIdentities);
                }
            },
            new UserConfigRunnable() {
                @Override
                public void run(UserStorage userStorage) {
                    assertEquals(userStorage.isLoggedIn(), fields2.knownIdentity);
                }
            }
    };
}
