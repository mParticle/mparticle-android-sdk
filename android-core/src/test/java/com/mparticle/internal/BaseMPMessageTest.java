package com.mparticle.internal;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.internal.messages.MPCommerceMessage;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.*;

public class BaseMPMessageTest {

    @Test
    public void testEventLength() throws Exception {
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).build();
        InternalSession session = new InternalSession();
        BaseMPMessage message = new BaseMPMessage.Builder(Constants.MessageType.EVENT)
                .name(event.getEventName())
                .timestamp(1235)
                .length(event.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getCustomAttributeStrings()))
                .build(session, null, 1);

        assertNull(message.opt("el"));
        assertNull(message.getAttributes());

        Map<String, String> info = new HashMap<String, String>(1);
        info.put("EventLength", "321");
        MPEvent event2 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).customAttributes(info).build();
        BaseMPMessage message2 = new BaseMPMessage.Builder(Constants.MessageType.EVENT)
                .name(event2.getEventName())
                .timestamp(1235)
                .length(event2.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event2.getCustomAttributeStrings()))
                .build(session, null, 1);

        assertEquals(message2.getAttributes().getString("EventLength"), "321");

        MPEvent event3 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).build();
        BaseMPMessage message3 = new BaseMPMessage.Builder(Constants.MessageType.EVENT)
                .name(event3.getEventName())
                .timestamp(1235)
                .length(event3.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getCustomAttributeStrings()))
                .build(session, null, 1);

        assertEquals(message3.getAttributes().getString("EventLength"), "123");

    }

    @Test
    public void testNullCartOnCommerceEvent() throws Exception {
        MParticle.setInstance(new MockMParticle());
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("foo", "bar", 10).build()).build();
        MPCommerceMessage.Builder builder = (MPCommerceMessage.Builder) new MPCommerceMessage.Builder(event)
                .timestamp(12345);
        BaseMPMessage message = builder.build(new InternalSession(), null, 0);
        assertNotNull(message);
    }
}