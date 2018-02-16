package com.mparticle.internal;

import com.mparticle.BaseCleanInstallEachTest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MPUtilityTest extends BaseCleanInstallEachTest {

    @Test
    public void testInstantAppDetectionTest() {
        assertFalse(MPUtility.isInstantApp(mContext));
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {

    }

    @Test
    public void testNullMapKey() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val1");
        map.put("key2", "val2");
        assertFalse(MPUtility.containsNullKey(map));
        map.put(null, "val3");
        assertTrue(MPUtility.containsNullKey(map));

        map = new Hashtable();
        map.put("key1", "val1");
        map.put("key2", "val2");
        assertFalse(MPUtility.containsNullKey(map));

        map = new TreeMap(map);
        assertFalse(MPUtility.containsNullKey(map));

        map = new LinkedHashMap(map);
        assertFalse(MPUtility.containsNullKey(map));
    }
}
