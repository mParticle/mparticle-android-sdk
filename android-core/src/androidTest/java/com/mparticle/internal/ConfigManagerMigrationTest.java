package com.mparticle.internal;

import android.os.Looper;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.testutils.BaseAbstractTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ConfigManagerMigrationTest extends BaseAbstractTest {
    MParticle instance;

    @Before
    public void before() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MParticle.reset(mContext);
        ConfigManager.clearMpid(mContext);
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        instance = MParticle.getInstance();

        for(UserStorage userStorage : UserStorage.getAllUsers(mContext)) {
            assertTrue(userStorage.deleteUserConfig(mContext, userStorage.getMpid()));
        }
        assertEquals(UserStorage.getAllUsers(mContext).size(), 0);
    }

    /**
     * This tests that a UserConfig can be migrated to an existing UserConfig, and it will only
     * override fields that have been set by the User
     */
    @Test
    public void testUserConfigMigrateFullSubject() throws Exception {
        UserStorageFields fields1 = new UserStorageFields();
        UserStorageFields fields2 = new UserStorageFields();
        UserStorageFields fields3 = new UserStorageFields();

        ConfigManager configManager = instance.Internal().getConfigManager();
        assertTrue(configManager.getMpid() == Constants.TEMPORARY_MPID);

        long oldMpid = ran.nextLong();
        long newMpid = ran.nextLong();

        //test that every field was set in the subject UserConfig
        configManager.setMpid(newMpid, ran.nextBoolean());
        assertTrue(configManager.getUserStorage().getMpid() == newMpid);
        setProfile(configManager.getUserStorage(), fields1);
        fields1.assertMatchesProfile(configManager.getUserStorage());

        configManager.setMpid(oldMpid, ran.nextBoolean());
        assertTrue(configManager.getUserStorage().getMpid() == oldMpid);
        setProfile(configManager.getUserStorage(), fields2);
        fields2.assertMatchesProfile(configManager.getUserStorage());

        configManager.setMpid(newMpid, ran.nextBoolean());
        configManager.mergeUserConfigs(oldMpid, newMpid);
        assertTrue(configManager.getUserStorage().getMpid() == newMpid);
        fields2.assertMatchesProfile(configManager.getUserStorage());
    }

    public static void setProfile(UserStorage userStorage, UserStorageFields fields) {
        userStorage.setBreadcrumbLimit(fields.breadcrumbLimit);
        userStorage.setDeletedUserAttributes(fields.deletedUserAttributes);
        userStorage.setLastUseDate(fields.lastUseDate);
        userStorage.setPreviousSessionForeground(fields.previousForeground);
        userStorage.setPreviousSessionId(fields.previousSessionId);
        userStorage.setPreviousSessionStart(fields.previousStart);
        userStorage.setLtv(fields.ltv);
        userStorage.setTotalRuns(fields.totalRuns);
        userStorage.setCookies(fields.cookies);
        userStorage.setLaunchesSinceUpgrade(fields.launchesSinceUpgrade);
        userStorage.setUserIdentities(fields.userIdentities);
    }

    static class UserStorageFields {
        int breadcrumbLimit;
        long previousForeground;
        String deletedUserAttributes;
        long lastUseDate;
        String previousSessionId;
        long previousStart;
        String ltv;
        int totalRuns;
        String cookies;
        int launchesSinceUpgrade;
        String userIdentities;
        boolean knownIdentity;
        Random ran = new Random();

        UserStorageFields() {
            refreshProfile();
        }

        void refreshProfile() {
            breadcrumbLimit = ran.nextInt();
            previousForeground = ran.nextLong();
            deletedUserAttributes = UUID.randomUUID().toString();
            lastUseDate = ran.nextLong();
            previousSessionId = UUID.randomUUID().toString();
            previousStart = ran.nextLong();
            ltv = String.valueOf(ran.nextInt());
            totalRuns = ran.nextInt();
            cookies = UUID.randomUUID().toString();
            launchesSinceUpgrade = ran.nextInt();
            userIdentities = UUID.randomUUID().toString();
        }

        void assertMatchesProfile(UserStorage userStorage) {
            assertEquals(userStorage.getBreadcrumbLimit(), breadcrumbLimit);
            assertEquals(userStorage.getDeletedUserAttributes(), deletedUserAttributes);
            assertEquals(userStorage.getLastUseDate(), lastUseDate);
            assertEquals(userStorage.getPreviousSessionId(), previousSessionId);
            assertEquals(userStorage.getPreviousSessionStart(-1), previousStart);
            assertEquals(userStorage.getLtv(), ltv);
            assertEquals(userStorage.getTotalRuns(-1), totalRuns);
            assertEquals(userStorage.getCookies(), cookies);
            assertEquals(userStorage.getLaunchesSinceUpgrade(), launchesSinceUpgrade);
            assertEquals(userStorage.getUserIdentities(), userIdentities);
        }
    }
}
