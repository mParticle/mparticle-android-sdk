package com.mparticle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BaseEventTest {

    @Test
    public void testEventType() {
        BaseEvent baseEvent = new BaseEvent(BaseEvent.Type.COMMERCE_EVENT);
        assertEquals(BaseEvent.Type.COMMERCE_EVENT, baseEvent.getType());
    }

    @Test
    public void testCustomFlags() {
        BaseEvent baseEvent = new BaseEvent(BaseEvent.Type.BREADCRUMB);
        assertNull(baseEvent.getCustomFlags());

        List<String> values1 = new ArrayList<String>();
        values1.add("val1");
        values1.add("val2");
        values1.add("val3");

        List<String> values2 = new ArrayList<String>();
        values2.add("val2");

        Map<String, List<String>> customFlags = new HashMap<String, List<String>>();
        customFlags.put("key1", values1);
        customFlags.put("key2", values2);
        customFlags.put("key3", new ArrayList<String>());
        baseEvent.setCustomFlags(customFlags);

        //should not be able to add null key
        customFlags.put(null, new ArrayList<String>());
        baseEvent.setCustomFlags(customFlags);
        assertEquals(3, baseEvent.getCustomFlags().size());

        baseEvent.setCustomFlags(null);
        assertNull(baseEvent.getCustomFlags());
    }

}
