package com.mparticle.internal;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;

public class BaseMPMessageTest {

    @Test
    public void testEventLength() throws Exception {
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).build();
        InternalSession session = new InternalSession();
        MessageManager.BaseMPMessage message = new MessageManager.BaseMPMessage.Builder(Constants.MessageType.EVENT, session, null, 1)
                .name(event.getEventName())
                .timestamp(1235)
                .length(event.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                .build();

        assertNull(message.opt("el"));
        assertNull(message.getAttributes());

        Map<String, String> info = new HashMap<String, String>(1);
        info.put("EventLength", "321");
        MPEvent event2 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).info(info).build();
        MessageManager.BaseMPMessage message2 = new MessageManager.BaseMPMessage.Builder(Constants.MessageType.EVENT, session, null, 1)
                .name(event2.getEventName())
                .timestamp(1235)
                .length(event2.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event2.getInfo()))
                .build();

        assertEquals(message2.getAttributes().getString("EventLength"), "321");

        MPEvent event3 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).build();
        MessageManager.BaseMPMessage message3 = new MessageManager.BaseMPMessage.Builder(Constants.MessageType.EVENT, session, null, 1)
                .name(event3.getEventName())
                .timestamp(1235)
                .length(event3.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                .build();

        assertEquals(message3.getAttributes().getString("EventLength"), "123");

    }

    @Test
    public void testNullCartOnCommerceEvent() throws Exception {
        MParticle.setInstance(new MockMParticle());
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("foo", "bar", 10).build()).build();
        Cart cart = null;
        MessageManager.BaseMPMessage message = new MessageManager.MPCommerceMessage.Builder(event, new InternalSession(), null, 0, cart)
                .timestamp(12345)
                .build();
        assertNotNull(message);
    }
}