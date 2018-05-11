package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MessageHandlerTest {

    @Test
    public void testRemoveUserAttributes() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        MParticleDatabase database = new MParticleDatabase(context);
        MessageHandler handler = new MessageHandler(Looper.myLooper(), Mockito.mock(MessageManager.class), database, context);
        handler.prepareDatabase();
        MessageManager.UserAttributeRemoval removal = new MessageManager.UserAttributeRemoval();
        removal.key = "foo";
        handler.removeUserAttribute(removal, null);
        TreeMap<String, List<String>> attributes = handler.getUserAttributeLists();
        Assert.assertNull(attributes.get("foo"));
        MessageManager.UserAttributeResponse newAttributes = new MessageManager.UserAttributeResponse();
        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List<String> attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        handler.setUserAttribute(newAttributes);
        attributes = handler.getUserAttributeLists();
        Assert.assertNotNull(attributes.get("foo"));
        handler.removeUserAttribute(removal, null);
        attributes = handler.getUserAttributeLists();
        Assert.assertNull(attributes.get("foo"));
    }

    @Test
    public void testUserUserAttributeLists() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        MParticleDatabase database = new MParticleDatabase(context);
        MessageHandler handler = new MessageHandler(Looper.myLooper(), Mockito.mock(MessageManager.class), database, context);
        handler.prepareDatabase();
        MessageManager.UserAttributeRemoval removal = new MessageManager.UserAttributeRemoval();
        removal.key = "foo";

        handler.removeUserAttribute(removal, null);
        TreeMap<String, List<String>> attributes = handler.getUserAttributeLists();
        Assert.assertNull(attributes.get("foo"));
        MessageManager.UserAttributeResponse newAttributes = new MessageManager.UserAttributeResponse();

        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List<String> attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        handler.setUserAttribute(newAttributes);
        attributes = handler.getUserAttributeLists();
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        attributeList.add("bar-2");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        handler.setUserAttribute(newAttributes);
        attributes = handler.getUserAttributeLists();
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar-2");
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        handler.setUserAttribute(newAttributes);
        attributes = handler.getUserAttributeLists();
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        handler.setUserAttribute(newAttributes);
        attributes = handler.getUserAttributeLists();
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));
    }
}