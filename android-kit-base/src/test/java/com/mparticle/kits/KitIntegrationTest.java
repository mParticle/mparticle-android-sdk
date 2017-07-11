package com.mparticle.kits;


import android.content.Context;
import android.util.SparseBooleanArray;

import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;
import com.mparticle.mock.MockMParticle;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KitIntegrationTest {

    @Test
    public void testGetAllUserAttributesWithoutLists() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitIntegration integration = new KitIntegration() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
                return null;
            }

            @Override
            public List<ReportingMessage> setOptOut(boolean optedOut) {
                return null;
            }
        };
        integration.setConfiguration(Mockito.mock(KitConfiguration.class));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key 1", "value 1");
        List<String> list = new LinkedList<>();
        list.add("value 2");
        list.add("value 3");
        attributes.put("key 2", list);

        Mockito.when(MParticle.getInstance().Identity().getCurrentUser().getUserAttributes()).thenReturn(attributes);
        Map<String, Object> filteredAttributes = integration.getAllUserAttributes();
        Assert.assertEquals("value 1", filteredAttributes.get("key 1"));
        Assert.assertEquals("value 2,value 3", filteredAttributes.get("key 2"));
    }

    class MockSparseBooleanArray extends SparseBooleanArray {
        @Override
        public boolean get(int key) {
            return get(key, false);
        }

        @Override
        public boolean get(int key, boolean valueIfKeyNotFound) {
            System.out.print("SparseArray getting: " + key);
            if (map.containsKey(key)) {
                return map.get(key);
            }else{
                return valueIfKeyNotFound;
            }
        }

        Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
        @Override
        public void put(int key, boolean value) {
            map.put(key, value);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    @Test
    public void testGetAllUserAttributesWithoutListsWithFilters() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitIntegration integration = new KitIntegration() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
                return null;
            }

            @Override
            public List<ReportingMessage> setOptOut(boolean optedOut) {
                return null;
            }
        };
        KitConfiguration configuration = Mockito.mock(KitConfiguration.class);
        MockSparseBooleanArray mockArray = new MockSparseBooleanArray();
        mockArray.put(MPUtility.mpHash("key 4"), false);
        mockArray.put(MPUtility.mpHash("key 3"), false);
        Mockito.when(configuration.getUserAttributeFilters()).thenReturn(mockArray);
        integration.setConfiguration(configuration);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key 1", "value 1");
        attributes.put("key 4", "value 4");
        List<String> list = new LinkedList<>();
        list.add("value 2");
        list.add("value 3");
        attributes.put("key 2", list);
        attributes.put("key 3", list);
        Mockito.when(MParticle.getInstance().Identity().getCurrentUser().getUserAttributes()).thenReturn(attributes);
        Map<String, Object> filteredAttributes = integration.getAllUserAttributes();
        Assert.assertEquals("value 1", filteredAttributes.get("key 1"));
        Assert.assertEquals("value 2,value 3", filteredAttributes.get("key 2"));
        Assert.assertNull(filteredAttributes.get("key 3"));
        Assert.assertNull(filteredAttributes.get("key 4"));
    }



    @Test
    public void testGetAllUserAttributesWithLists() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitIntegration integration = new AttributeListenerIntegration();
        integration.setConfiguration(Mockito.mock(KitConfiguration.class));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key 1", "value 1");
        List<String> list = new LinkedList<>();
        list.add("value 2");
        list.add("value 3");
        attributes.put("key 2", list);

        Mockito.when(MParticle.getInstance().Identity().getCurrentUser().getUserAttributes()).thenReturn(attributes);
        Map<String, Object> filteredAttributes = integration.getAllUserAttributes();
        Assert.assertEquals("value 1", filteredAttributes.get("key 1"));
        Assert.assertEquals(list, filteredAttributes.get("key 2"));
    }

    @Test
    public void testGetAllUserAttributesWithListsAndFilters() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitIntegration integration = new AttributeListenerIntegration();
        KitConfiguration configuration = Mockito.mock(KitConfiguration.class);
        MockSparseBooleanArray mockArray = new MockSparseBooleanArray();
        mockArray.put(MPUtility.mpHash("key 4"), false);
        mockArray.put(MPUtility.mpHash("key 3"), false);
        Mockito.when(configuration.getUserAttributeFilters()).thenReturn(mockArray);
        integration.setConfiguration(configuration);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key 1", "value 1");
        attributes.put("key 4", "value 4");
        List<String> list = new LinkedList<>();
        list.add("value 2");
        list.add("value 3");
        attributes.put("key 2", list);
        attributes.put("key 3", list);
        Mockito.when(MParticle.getInstance().Identity().getCurrentUser().getUserAttributes()).thenReturn(attributes);
        Map<String, Object> filteredAttributes = integration.getAllUserAttributes();
        Assert.assertEquals("value 1", filteredAttributes.get("key 1"));
        Assert.assertEquals(list, filteredAttributes.get("key 2"));
        Assert.assertNull(filteredAttributes.get("key 3"));
        Assert.assertNull(filteredAttributes.get("key 4"));
    }

    private class AttributeListenerIntegration extends KitIntegration implements KitIntegration.AttributeListener {

        @Override
        public void setUserAttribute(String attributeKey, String attributeValue) {

        }

        @Override
        public void setUserAttributeList(String attributeKey, List<String> attributeValueList) {

        }

        @Override
        public boolean supportsAttributeLists() {
            return true;
        }

        @Override
        public void setAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists) {

        }

        @Override
        public void removeUserAttribute(String key) {

        }

        @Override
        public void setUserIdentity(MParticle.IdentityType identityType, String identity) {

        }

        @Override
        public void removeUserIdentity(MParticle.IdentityType identityType) {

        }

        @Override
        public List<ReportingMessage> logout() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
            return null;
        }

        @Override
        public List<ReportingMessage> setOptOut(boolean optedOut) {
            return null;
        }
    }
}