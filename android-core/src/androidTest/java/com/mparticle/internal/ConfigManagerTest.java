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
        Looper.prepare();
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

        for(UserConfig userConfig: UserConfig.getAllUsers(mContext)) {
            UserConfig.deleteUserConfig(mContext, userConfig.getMpid());
        }
        assertEquals(UserConfig.getAllUsers(mContext).size(), 0);

        refreshProfile1();
        refreshProfile2();
        refreshProfile3();


    }

    /**
     * This tests that a UserConfig can be migrated from an existing MPID, to a new MPID
     */
    @Test
    public void testUserConfigMigrateEmptyTarget() throws Exception {
        long newMpid = new Random().nextInt();

        ConfigManager configManager = instance.getConfigManager();
        assertTrue(configManager.getMpid() == Constants.TEMPORARY_MPID);
        assertNotNull(configManager.getUserConfig());
        assertTrue((configManager.getUserConfig().getMpid()) == 0);

        setProfile1(instance.getConfigManager().getUserConfig());
        assertMatchesProfile1(instance.getConfigManager().getUserConfig());

        //make sure the new MPID doesn't have an existing UserConfig SharedPreferences file
        for (UserConfig existingUserConfig: UserConfig.getAllUsers(mContext)) {
            assertFalse(existingUserConfig.getMpid() == newMpid);
        }

        configManager.setMpid(newMpid);
        configManager.mergeUserConfigs(0, newMpid);
        assertTrue(configManager.getMpid() == newMpid);
        assertMatchesProfile1(configManager.getUserConfig());
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
        assertTrue(configManager.getUserConfig().getMpid() == newMpid);
        setProfile2(configManager.getUserConfig());
        assertMatchesProfile2(configManager.getUserConfig());

        configManager.setMpid(oldMpid);
        assertTrue(configManager.getUserConfig().getMpid() == oldMpid);
        setProfile3(configManager.getUserConfig());
        assertMatchesProfile3(configManager.getUserConfig());

        configManager.setMpid(newMpid);
        configManager.mergeUserConfigs(oldMpid, newMpid);
        assertTrue(configManager.getUserConfig().getMpid() == newMpid);
        assertMatchesProfile3(configManager.getUserConfig());
    }

    @Test
    public void testUserConfigMigrateEmptySubject() throws Exception {
        ConfigManager configManager = instance.getConfigManager();

        //test that no fields were set in the subject UserConfig
        long oldMpid = new Random().nextLong();
        long newMpid = new Random().nextLong();

        configManager.setMpid(newMpid);
        assertTrue(configManager.getUserConfig().getMpid() == newMpid);
        setProfile1(configManager.getUserConfig());

        //make sure this UserConfig does not exist
        for (UserConfig userConfig : UserConfig.getAllUsers(mContext)) {
            assertFalse(userConfig.getMpid() == oldMpid);
        }
        configManager.setMpid(oldMpid);
        assertTrue(configManager.getUserConfig().getMpid() == oldMpid);

        configManager.setMpid(newMpid);
        configManager.mergeUserConfigs(oldMpid, newMpid);
        assertTrue(configManager.getUserConfig().getMpid() == newMpid);
        assertMatchesProfile1(configManager.getUserConfig());
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
    
    void setProfile1(UserConfig userConfig) {
        userConfig.setBreadcrumbLimit(breadcrumbLimit);
        userConfig.setDeletedUserAttributes(deletedUserAttributes);
        userConfig.setLastUseDate(lastUseDate);
        userConfig.setPreviousSessionForeground(previousForeground);
        userConfig.setPreviousSessionId(previousSessionId);
        userConfig.setPreviousSessionStart(previousStart);
        userConfig.setLtv(ltv);
        userConfig.setTotalRuns(totalRuns);
        userConfig.setCookies(cookies);
        userConfig.setLaunchesSinceUpgrade(launchesSinceUpgrade);
        userConfig.setUserIdentities(userIdentities);
    }

    void assertMatchesProfile1(UserConfig userConfig) {
        assertEquals(userConfig.getBreadcrumbLimit(), breadcrumbLimit);
        assertEquals(userConfig.getDeletedUserAttributes(), deletedUserAttributes);
        assertEquals(userConfig.getLastUseDate(), lastUseDate);
        assertEquals(userConfig.getPreviousSessionId(), previousSessionId);
        assertEquals(userConfig.getPreviousSessionStart(-1), previousStart);
        assertEquals(userConfig.getLtv(), ltv);
        assertEquals(userConfig.getTotalRuns(-1), totalRuns);
        assertEquals(userConfig.getCookies(), cookies);
        assertEquals(userConfig.getLaunchesSinceUpgrade(), launchesSinceUpgrade);
        assertEquals(userConfig.getUserIdentities(), userIdentities);
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

    static void setProfile2(UserConfig userConfig) {
        userConfig.setBreadcrumbLimit(breadcrumbLimit2);
        userConfig.setDeletedUserAttributes(deletedUserAttributes2);
        userConfig.setLastUseDate(lastUseDate2);
        userConfig.setPreviousSessionForeground(previousForeground2);
        userConfig.setPreviousSessionId(previousSessionId2);
        userConfig.setPreviousSessionStart(previousStart2);
        userConfig.setLtv(ltv2);
        userConfig.setTotalRuns(totalRuns2);
        userConfig.setCookies(cookies2);
        userConfig.setLaunchesSinceUpgrade(launchesSinceUpgrade2);
        userConfig.setUserIdentities(userIdentities2);
    }

    static void assertMatchesProfile2(UserConfig userConfig) {
        assertEquals(userConfig.getBreadcrumbLimit(), breadcrumbLimit2);
        assertEquals(userConfig.getDeletedUserAttributes(), deletedUserAttributes2);
        assertEquals(userConfig.getLastUseDate(), lastUseDate2);
        assertEquals(userConfig.getPreviousSessionId(), previousSessionId2);
        assertEquals(userConfig.getPreviousSessionStart(-1), previousStart2);
        assertEquals(userConfig.getLtv(), ltv2);
        assertEquals(userConfig.getTotalRuns(-1), totalRuns2);
        assertEquals(userConfig.getCookies(), cookies2);
        assertEquals(userConfig.getLaunchesSinceUpgrade(), launchesSinceUpgrade2);
        assertEquals(userConfig.getUserIdentities(), userIdentities2);
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

    void setProfile3(UserConfig userConfig) {
        userConfig.setBreadcrumbLimit(breadcrumbLimit3);
        userConfig.setDeletedUserAttributes(deletedUserAttributes3);
        userConfig.setLastUseDate(lastUseDate3);
        userConfig.setPreviousSessionForeground(previousForeground3);
        userConfig.setPreviousSessionId(previousSessionId3);
        userConfig.setPreviousSessionStart(previousStart3);
        userConfig.setLtv(ltv3);
        userConfig.setTotalRuns(totalRuns3);
        userConfig.setCookies(cookies3);
        userConfig.setLaunchesSinceUpgrade(launchesSinceUpgrade3);
        userConfig.setUserIdentities(userIdentities3);
    }

    private void assertMatchesProfile3(UserConfig userConfig) {
        assertEquals(userConfig.getBreadcrumbLimit(), breadcrumbLimit3);
        assertEquals(userConfig.getDeletedUserAttributes(), deletedUserAttributes3);
        assertEquals(userConfig.getLastUseDate(), lastUseDate3);
        assertEquals(userConfig.getPreviousSessionId(), previousSessionId3);
        assertEquals(userConfig.getPreviousSessionStart(-1), previousStart3);
        assertEquals(userConfig.getLtv(), ltv3);
        assertEquals(userConfig.getTotalRuns(-1), totalRuns3);
        assertEquals(userConfig.getCookies(), cookies3);
        assertEquals(userConfig.getLaunchesSinceUpgrade(), launchesSinceUpgrade3);
        assertEquals(userConfig.getUserIdentities(), userIdentities3);
    }
}
