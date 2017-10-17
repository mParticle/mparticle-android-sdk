package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class ConfigManagerTest {
    MParticle instance;
    Context mContext;

    @BeforeClass
    public static void setupClass() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
        ConfigManager.clearMpid(mContext);
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        instance = MParticle.getInstance();

        for(UserStorage userStorage : UserStorage.getAllUsers(mContext)) {
            assertTrue(UserStorage.deleteUserConfig(mContext, userStorage.getMpid()));
        }
        assertEquals(UserStorage.getAllUsers(mContext).size(), 0);

        refreshProfile1();
        refreshProfile2();
        refreshProfile3();


    }

    /**
     * This tests that a UserConfig can be migrated to an existing UserConfig, and it will only
     * override fields that have been set by the User
     */
    @Test
    public void testUserConfigMigrateFullSubject() throws Exception {
        ConfigManager configManager = instance.getConfigManager();
        assertTrue(configManager.getMpid() == Constants.TEMPORARY_MPID);

        long oldMpid = new Random().nextLong();
        long newMpid = new Random().nextLong();


        //test that every field was set in the subject UserConfig
        configManager.setMpid(newMpid);
        assertTrue(configManager.getUserStorage().getMpid() == newMpid);
        setProfile2(configManager.getUserStorage());
        assertMatchesProfile2(configManager.getUserStorage());

        configManager.setMpid(oldMpid);
        assertTrue(configManager.getUserStorage().getMpid() == oldMpid);
        setProfile3(configManager.getUserStorage());
        assertMatchesProfile3(configManager.getUserStorage());

        configManager.setMpid(newMpid);
        configManager.mergeUserConfigs(oldMpid, newMpid);
        assertTrue(configManager.getUserStorage().getMpid() == newMpid);
        assertMatchesProfile3(configManager.getUserStorage());
    }

    static int breadcrumbLimit;
    static long previousForeground;
    static String deletedUserAttributes;
    static long lastUseDate;
    static String previousSessionId;
    static long previousStart;
    static String ltv;
    static int totalRuns;
    static String cookies;
    static int launchesSinceUpgrade;
    static String userIdentities;

    static void refreshProfile1() {
        breadcrumbLimit = new Random().nextInt();
        previousForeground = new Random().nextLong();
        deletedUserAttributes = UUID.randomUUID().toString();
        lastUseDate = new Random().nextLong();
        previousSessionId = UUID.randomUUID().toString();
        previousStart = new Random().nextLong();
        ltv = String.valueOf(new Random().nextInt());
        totalRuns = new Random().nextInt();
        cookies = UUID.randomUUID().toString();
        launchesSinceUpgrade = new Random().nextInt();
        userIdentities = UUID.randomUUID().toString();
    }
    
    void setProfile1(UserStorage userStorage) {
        userStorage.setBreadcrumbLimit(breadcrumbLimit);
        userStorage.setDeletedUserAttributes(deletedUserAttributes);
        userStorage.setLastUseDate(lastUseDate);
        userStorage.setPreviousSessionForeground(previousForeground);
        userStorage.setPreviousSessionId(previousSessionId);
        userStorage.setPreviousSessionStart(previousStart);
        userStorage.setLtv(ltv);
        userStorage.setTotalRuns(totalRuns);
        userStorage.setCookies(cookies);
        userStorage.setLaunchesSinceUpgrade(launchesSinceUpgrade);
        userStorage.setUserIdentities(userIdentities);
    }

    void assertMatchesProfile1(UserStorage userStorage) {
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

    static int breadcrumbLimit2;
    static long previousForeground2;
    static String deletedUserAttributes2;
    static long lastUseDate2;
    static String previousSessionId2;
    static long previousStart2;
    static String ltv2;
    static int totalRuns2;
    static String cookies2;
    static int launchesSinceUpgrade2;
    static String userIdentities2;

    static void refreshProfile2() {
        breadcrumbLimit2 = new Random().nextInt();
        previousForeground2 = new Random().nextLong();
        deletedUserAttributes2 = UUID.randomUUID().toString();
        lastUseDate2 = new Random().nextLong();
        previousSessionId2 = UUID.randomUUID().toString();
        previousStart2 = new Random().nextLong();
        ltv2 = String.valueOf(new Random().nextInt());
        totalRuns2 = new Random().nextInt();
        cookies2 = UUID.randomUUID().toString();
        launchesSinceUpgrade2 = new Random().nextInt();
        userIdentities2 = UUID.randomUUID().toString();
    }

    static void setProfile2(UserStorage userStorage) {
        userStorage.setBreadcrumbLimit(breadcrumbLimit2);
        userStorage.setDeletedUserAttributes(deletedUserAttributes2);
        userStorage.setLastUseDate(lastUseDate2);
        userStorage.setPreviousSessionForeground(previousForeground2);
        userStorage.setPreviousSessionId(previousSessionId2);
        userStorage.setPreviousSessionStart(previousStart2);
        userStorage.setLtv(ltv2);
        userStorage.setTotalRuns(totalRuns2);
        userStorage.setCookies(cookies2);
        userStorage.setLaunchesSinceUpgrade(launchesSinceUpgrade2);
        userStorage.setUserIdentities(userIdentities2);
    }

    static void assertMatchesProfile2(UserStorage userStorage) {
        assertEquals(userStorage.getBreadcrumbLimit(), breadcrumbLimit2);
        assertEquals(userStorage.getDeletedUserAttributes(), deletedUserAttributes2);
        assertEquals(userStorage.getLastUseDate(), lastUseDate2);
        assertEquals(userStorage.getPreviousSessionId(), previousSessionId2);
        assertEquals(userStorage.getPreviousSessionStart(-1), previousStart2);
        assertEquals(userStorage.getLtv(), ltv2);
        assertEquals(userStorage.getTotalRuns(-1), totalRuns2);
        assertEquals(userStorage.getCookies(), cookies2);
        assertEquals(userStorage.getLaunchesSinceUpgrade(), launchesSinceUpgrade2);
        assertEquals(userStorage.getUserIdentities(), userIdentities2);
    }

    int breadcrumbLimit3;
    long previousForeground3;
    String deletedUserAttributes3;
    long lastUseDate3;
    String previousSessionId3;
    long previousStart3;
    String ltv3;
    int totalRuns3;
    String cookies3;
    int launchesSinceUpgrade3;
    String userIdentities3;

    void refreshProfile3() {
        breadcrumbLimit3 = new Random().nextInt();
        previousForeground3 = new Random().nextLong();
        deletedUserAttributes3 = UUID.randomUUID().toString();
        lastUseDate3 = new Random().nextLong();
        previousSessionId3 = UUID.randomUUID().toString();
        previousStart3 = new Random().nextLong();
        ltv3 = String.valueOf(new Random().nextInt());
        totalRuns3 = new Random().nextInt();
        cookies3 = UUID.randomUUID().toString();
        launchesSinceUpgrade3 = new Random().nextInt();
        userIdentities3 = UUID.randomUUID().toString();
    }

    void setProfile3(UserStorage userStorage) {
        userStorage.setBreadcrumbLimit(breadcrumbLimit3);
        userStorage.setDeletedUserAttributes(deletedUserAttributes3);
        userStorage.setLastUseDate(lastUseDate3);
        userStorage.setPreviousSessionForeground(previousForeground3);
        userStorage.setPreviousSessionId(previousSessionId3);
        userStorage.setPreviousSessionStart(previousStart3);
        userStorage.setLtv(ltv3);
        userStorage.setTotalRuns(totalRuns3);
        userStorage.setCookies(cookies3);
        userStorage.setLaunchesSinceUpgrade(launchesSinceUpgrade3);
        userStorage.setUserIdentities(userIdentities3);
    }

    private void assertMatchesProfile3(UserStorage userStorage) {
        assertEquals(userStorage.getBreadcrumbLimit(), breadcrumbLimit3);
        assertEquals(userStorage.getDeletedUserAttributes(), deletedUserAttributes3);
        assertEquals(userStorage.getLastUseDate(), lastUseDate3);
        assertEquals(userStorage.getPreviousSessionId(), previousSessionId3);
        assertEquals(userStorage.getPreviousSessionStart(-1), previousStart3);
        assertEquals(userStorage.getLtv(), ltv3);
        assertEquals(userStorage.getTotalRuns(-1), totalRuns3);
        assertEquals(userStorage.getCookies(), cookies3);
        assertEquals(userStorage.getLaunchesSinceUpgrade(), launchesSinceUpgrade3);
        assertEquals(userStorage.getUserIdentities(), userIdentities3);
    }
}
