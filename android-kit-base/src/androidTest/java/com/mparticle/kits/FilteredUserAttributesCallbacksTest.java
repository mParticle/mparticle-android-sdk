package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.mock.utils.RandomUtils;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class FilteredUserAttributesCallbacksTest extends BaseKitManagerStarted {

    private TestKit kitInstance;

    @Override
    protected Map<String, JSONObject> registerCustomKits() {
        Map<String, JSONObject> customKits = new HashMap<>();
        JSONObject userAttributesFilters = new JSONObject();
        try {
            userAttributesFilters = new JSONObject()
                    .put("hs", new JSONObject()
                            .put("ua", new JSONObject()
                                    .put(String.valueOf(KitUtils.hashForFiltering("Filtered1")), 0)
                                    .put(String.valueOf(KitUtils.hashForFiltering("Filtered2")), 0)
                                    .put(String.valueOf(KitUtils.hashForFiltering("Filtered3")), 0)
                                    .put(String.valueOf(KitUtils.hashForFiltering("Don't filter this")), 1)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        customKits.put(TestKit.class.getName(), userAttributesFilters);
        return customKits;
    }

    @Override
    protected void before() throws Exception {
        super.before();
        TestKit.setOnKitCreate(new BaseTestKit.OnKitCreateListener() {
            @Override
            public void onKitCreate(BaseTestKit kitIntegration) {
                if (kitIntegration instanceof TestKit) {
                    kitInstance = (TestKit)kitIntegration;
                }
            }
        });
        long endTime = System.currentTimeMillis() + (20 * 1000);
        while (kitInstance == null && System.currentTimeMillis() < endTime) {}
        if (kitInstance == null) {
            fail("Kit not started");
        }
        kitInstance.setUserAttributeListener(null);
    }

    @Test
    public void testSetSingleAttributes() throws Exception {
        final boolean[] called = new boolean[1];
        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
                super.onSetUserAttribute(key, value, user);
                try {
                    MParticleUtils.awaitSetUserAttribute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Logger.error("Called = true");
                assertEquals(user.getUserAttributes().get("TestAttribute"), "attribute1");
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("TestAttribute", "attribute1");

        TestingUtils.checkAllBool(called, 1, 10);
    }

    @Test
    public void testRemoveSingleAttributes() throws Exception {
        final boolean[] called = new boolean[2];
        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
                super.onSetUserAttribute(key, value, user);
                assertEquals(key, "TestAttribute");
                assertEquals(value, "attribute1");
                called[0] = true;
            }

            @Override
            public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
                super.onRemoveUserAttribute(key, user);
                assertEquals(key, "TestAttribute");
                try {
                    MParticleUtils.awaitRemoveUserAttribute();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertFalse(user.getUserAttributes().containsKey("TestAttribute"));
                called[1] = true;
            }
        });
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("TestAttribute", "attribute1");
        MParticle.getInstance().Identity().getCurrentUser().removeUserAttribute("TestAttribute");

        TestingUtils.checkAllBool(called, 1 ,10);
    }

    @Test
    public void testIncrementAttribute() throws Exception {
        final boolean[] called = new boolean[1];

        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onIncrementUserAttribute(String key, String value, FilteredMParticleUser user) {
                super.onIncrementUserAttribute(key, value, user);
                assertEquals(user.getUserAttributes().get("TestAttribute"), "6");
                called[0] = true;
            }
        });

        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("TestAttribute", 2);
        MParticle.getInstance().Identity().getCurrentUser().incrementUserAttribute("TestAttribute", 4);

        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testSetSingleTag() throws Exception {
        final boolean[] called = new boolean[1];

        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetUserTag(String tag, FilteredMParticleUser user) {
                super.onSetUserTag(tag, user);
                assertEquals(tag, "TestAttribute");
                try {
                    MParticleUtils.awaitSetUserAttribute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertTrue(user.getUserAttributes().containsKey("TestAttribute"));
                assertNull(user.getUserAttributes().get("TestAttribute"));
                called[0] = true;
            }
        });

        MParticle.getInstance().Identity().getCurrentUser().setUserTag("TestAttribute");

        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testSetUserAttributeList() throws InterruptedException {
        final boolean[] called = new boolean[1];
        kitInstance.setSupportsAttributeLists(true);
        final List<String> userAttributes = new ArrayList<String>() {{add("one"); add("two"); add("three");}};
        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
                super.onSetUserAttributeList(attributeKey, attributeValueList, user);
                assertEquals(attributeKey, "key1");
                try {
                    MParticleUtils.awaitSetUserAttribute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertTrue(user.getUserAttributes().containsKey("key1"));
                assertEquals(user.getUserAttributes().get("key1"), userAttributes);
                called[0] = true;
            }

            @Override
            public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
                super.onSetUserAttribute(key, value, user);
                fail("unexpected call to onSetUserAttribute");
            }
        });
        MParticle.getInstance().Identity().getCurrentUser().setUserAttributeList("key1", userAttributes);

        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testSetUserAttributeListDisabled() throws InterruptedException {
        final boolean[] called = new boolean[1];
        kitInstance.setSupportsAttributeLists(false);
        final List<String> userAttributes = new ArrayList<String>() {{add("one"); add("two"); add("three");}};
        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
                super.onSetUserAttributeList(attributeKey, attributeValueList, user);
                fail("unexpected call to onSetUserAttributeList");
            }

            @Override
            public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
                super.onSetUserAttribute(key, value, user);
                assertEquals(key, "key1");
                assertEquals(value, KitUtils.join(userAttributes));
                try {
                    MParticleUtils.awaitSetUserAttribute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertEquals(user.getUserAttributes().get("key1"), userAttributes);
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().getCurrentUser().setUserAttributeList("key1", userAttributes);

        TestingUtils.checkAllBool(called);
    }


    @Test
    public void testSetAllUserAttributes() throws InterruptedException {
        final boolean[] called = new boolean[1];
        Long otherMpid = new Random().nextLong();
        ConfigManager configManager = new ConfigManager(mContext);
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        final List<String> sampleUserAttributes = new ArrayList<String>() {{add("one"); add("two"); add("three");}};
        kitInstance.setUserAttributeListener(null);

        user.setUserAttribute("attribute1", "value1");
        user.setUserAttribute("Filtered1", "value2");
        user.setUserAttribute("attribute2", sampleUserAttributes);
        user.setUserAttribute("Filtered2", new ArrayList<String>(){{add("four");}});
        MParticleUtils.awaitSetUserAttribute();

        kitInstance.setUserAttributeListener(new SingleCallListener(otherMpid) {
            @Override
            public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
                super.onSetAllUserAttributes(userAttributes, userAttributeLists, user);
                called[0] = true;
            }
        });
        configManager.setMpid(otherMpid);
        TestingUtils.checkAllBool(called);
        called[0] = false;

        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
                super.onSetAllUserAttributes(userAttributes, userAttributeLists, user);
                assertEquals(userAttributes.size(), 1);
                assertEquals(userAttributeLists.size(), 1);
                assertEquals(userAttributes.get("attribute1"), "value1");
                assertEquals(userAttributeLists.get("attribute2"), sampleUserAttributes);
                called[0] = true;
            }
        });

        configManager.setMpid(mStartingMpid);
        TestingUtils.checkAllBool(called);
        called[0] = false;

        kitInstance.setUserAttributeListener(new SingleCallListener(otherMpid){
            @Override
            public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
                super.onSetAllUserAttributes(userAttributes, userAttributeLists, user);
                called[0] = true;
            }
        });

        configManager.setMpid(otherMpid);
        TestingUtils.checkAllBool(called);

        kitInstance.setSupportsAttributeLists(false);
        called[0] = false;

        kitInstance.setUserAttributeListener(new SingleCallListener(mStartingMpid) {
            @Override
            public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
                super.onSetAllUserAttributes(userAttributes, userAttributeLists, user);
                assertEquals(userAttributes.size(), 2);
                assertEquals(userAttributeLists.size(), 0);
                assertEquals(userAttributes.get("attribute1"), "value1");
                assertEquals(userAttributes.get("attribute2"), KitUtils.join(sampleUserAttributes));
                called[0] = true;
            }
        });

        configManager.setMpid(mStartingMpid);
        TestingUtils.checkAllBool(called);
        kitInstance.setUserAttributeListener(null);
    }

    @Test
    public void testFilteredCalls() throws Exception {
        kitInstance.setUserAttributeListener(new FailureListener());
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        user.setUserAttribute("Filtered1", "1");
        user.setUserTag("Filtered2");
        user.setUserAttributeList("Filtered3", new ArrayList<String>(){{add("one");}});

        MParticleUtils.awaitSetUserAttribute();

        assertEquals(user.getUserAttributes().get("Filtered1"), "1");
        assertTrue(user.getUserAttributes().containsKey("Filtered2"));
        assertNull(user.getUserAttributes().get("Filtered2"));
        assertEquals(user.getUserAttributes().get("Filtered3"), new ArrayList<String>(){{add("one");}});

        user.incrementUserAttribute("Filtered1", 2);

        long endTime = System.currentTimeMillis() + (10 * 1000);
        while (user.getUserAttributes().get("Filtered1").equals("1") && System.currentTimeMillis() < endTime) {
            Thread.sleep(200);
        }
        assertEquals(user.getUserAttributes().get("Filtered1"), "3");

        user.removeUserAttribute("Filtered1");
        MParticleUtils.awaitRemoveUserAttribute();

        assertNull(user.getUserAttributes().get("Filtered1"));
    }

    @Test
    public void testKitDisabled() throws Exception {
        kitInstance.setUserAttributeListener(new FailureListener());
        kitInstance.setDisabled(true);
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        user.setUserAttribute("key1", "1");
        user.setUserTag("key2");

        MParticleUtils.awaitSetUserAttribute();

        assertEquals(user.getUserAttributes().get("key1"), "1");
        assertTrue(user.getUserAttributes().containsKey("key2"));
        assertNull(user.getUserAttributes().get("key2"));

        user.incrementUserAttribute("key1", 2);

        long endTime = System.currentTimeMillis() + (10 * 1000);
        while (user.getUserAttributes().get("key1").equals("1") && System.currentTimeMillis() < endTime) {
            Thread.sleep(200);
        }
        assertEquals(user.getUserAttributes().get("key1"), "3");

        user.removeUserAttribute("key1");
        MParticleUtils.awaitRemoveUserAttribute();

        assertNull(user.getUserAttributes().get("key1"));
    }

    public static class TestKit extends BaseTestKit implements KitIntegration.UserAttributeListener {

        UserAttributeListener listener = new FailureListener();
        private boolean disabled = false;
        private boolean supportsAttributeLists = true;

        void setUserAttributeListener(UserAttributeListener listener) {
            this.listener = listener;
        }

        void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        void setSupportsAttributeLists(boolean supports) {
            this.supportsAttributeLists = supports;
        }

        @Override
        public boolean isDisabled() {
            return disabled;
        }

        @Override
        public void onIncrementUserAttribute(String key, String value, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onIncrementUserAttribute(key, value, user);
            }
        }

        @Override
        public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onRemoveUserAttribute(key, user);
            }
        }

        @Override
        public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onSetUserAttribute(key, value, user);
            }
        }

        @Override
        public void onSetUserTag(String tag, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onSetUserTag(tag, user);
            }
        }

        @Override
        public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onSetUserAttributeList(attributeKey, attributeValueList, user);
            }
        }

        @Override
        public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
            if (listener != null) {
                listener.onSetAllUserAttributes(userAttributes, userAttributeLists, user);
            }
        }

        @Override
        public boolean supportsAttributeLists() {
            return supportsAttributeLists;
        }
    }

    private static class FailureListener implements KitIntegration.UserAttributeListener {

        @Override
        public void onIncrementUserAttribute(String key, String value, FilteredMParticleUser user) {
            fail("unexpected call to onIncrementUserAttribute");
        }

        @Override
        public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
            fail("unexpected call to onRemoveUserAttribute");
        }

        @Override
        public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
            fail("unexpected call to onSetUserAttribute");
        }

        @Override
        public void onSetUserTag(String tag, FilteredMParticleUser user) {
            fail("unexpected call to onSetUserTag");
        }

        @Override
        public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
            fail("unexpected call to onSetUserAttribueList");
        }

        @Override
        public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
            fail("unexprected call to onSetAllUserAttributes");
        }

        @Override
        public boolean supportsAttributeLists() {
            fail("unexpected call to supportsAttributeLists");
            return true;
        }
    }

    private static class SingleCallListener implements KitIntegration.UserAttributeListener {
        boolean[] internalCalled = new boolean[6];
        long expectedMpid;

        public SingleCallListener(long mpid) {
            expectedMpid = mpid;
        }

        @Override
        public void onIncrementUserAttribute(String key, String value, FilteredMParticleUser user) {
            if (internalCalled[0]) {
                fail("unexpected call to onIncrementUserAttribute");
            }
            internalCalled[0] = true;
            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
            if (internalCalled[1]) {
                fail("unexpected call to onRemoveUserAttribute");
            }
            internalCalled[1] = true;
            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
            if (internalCalled[2]) {
                fail("unexpected call to onSetUserAttribute");
            }
            internalCalled[2] = true;
            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public void onSetUserTag(String key, FilteredMParticleUser user) {
            if (internalCalled[3]) {
                fail("unexpected call to onSetUserTag");
            }
            internalCalled[3] = true;
            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
            if (internalCalled[4]) {
                fail("unexpected call to onSetUserAttributeList");
            }
            internalCalled[4] = true;
            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
//            if (internalCalled[5]) {
//                fail("unexpected call to onSetAllUserAttributes");
//            }
//            internalCalled[5] = true;
//            assertEquals(user.getId(), expectedMpid);
        }

        @Override
        public boolean supportsAttributeLists() {
            return false;
        }
    }
}
