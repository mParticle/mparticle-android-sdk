package com.mparticle.internal.database.services;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.DatabaseHelper;
import com.mparticle.internal.dto.UserAttributeRemoval;
import com.mparticle.internal.dto.UserAttributeResponse;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MParticleDBManagerTest {

    @Test
    public void testRemoveUserAttributes() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        MParticleDBManager manager = new MParticleDBManager(context);
        UserAttributeRemoval removal = new UserAttributeRemoval();
        removal.key = "foo";
        removal.mpId = 10L;
        manager.removeUserAttribute(removal, null);
        Map<String, Object> attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
        UserAttributeResponse newAttributes = new UserAttributeResponse();
        newAttributes.mpId = 10L;
        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        manager.removeUserAttribute(removal, null);
        attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
    }

    @Test
    public void testUserUserAttributeLists() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        MParticleDBManager manager = new MParticleDBManager(context);
        UserAttributeRemoval removal = new UserAttributeRemoval();
        removal.key = "foo";
        removal.mpId = 10L;
        manager.removeUserAttribute(removal, null);
        Map<String, Object> attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
        UserAttributeResponse newAttributes = new UserAttributeResponse();
        newAttributes.mpId = 10L;
        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        attributeList.add("bar-2");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar-2");
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));
    }
}