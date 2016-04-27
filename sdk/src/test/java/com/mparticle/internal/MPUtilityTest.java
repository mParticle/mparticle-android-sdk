package com.mparticle.internal;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class MPUtilityTest {

    @Test
    public void testSetCheckedAttribute() throws Exception {
        JSONObject attributes = new JSONObject();

        MPUtility.setCheckedAttribute(attributes, "some key", "some value", false, true);
        assertEquals("some value", attributes.getString("some key"));


        MPUtility.setCheckedAttribute(attributes, "some key 2", "some value 2", false, false);
        assertEquals("some value 2", attributes.getString("some key 2"));

    }

    @Test
    public void testSetKeyThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            builder.append("a");
        }
        String keyThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, keyThatsTooLong, "some value 2", false, true);

        assertFalse(attributes.has(keyThatsTooLong));

    }


    @Test
    public void testSetValueThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            builder.append("a");
        }
        String valueThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, false);

        assertFalse(attributes.has("mykey"));

    }

    @Test
    public void testSetUserValueThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4097; i++) {
            builder.append("a");
        }
        String valueThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, true);

        assertFalse(attributes.has("mykey"));

    }
}