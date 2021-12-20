package com.mparticle.internal;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MPUtilityTest extends BaseCleanInstallEachTest {

    @Test
    public void testInstantAppDetectionTest() {
        assertFalse(MPUtility.isInstantApp(mContext));
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

    @Test
    public void testGetInstrumentedNetworkType() throws Exception {
        TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        Integer result = MPUtility.getNetworkType(mContext, manager);
        assertNull(result);
    }
}
