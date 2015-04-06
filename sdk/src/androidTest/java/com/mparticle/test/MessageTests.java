package com.mparticle.test;

import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


public class MessageTests  {

    @Before
    public void setUp()  {
       // MParticle.start(getContext());
    }

    @Test
    public void testFirstRunMessage(){
        long firstRunTime = System.currentTimeMillis();
        String sessionId = "coolsessionId";
        MPMessage mpMessage = null;
        try {
            mpMessage = MParticle.getInstance().internal().getMessageManager().createFirstRunMessage(firstRunTime, sessionId);

            assertTrue(mpMessage.getSessionId().equals(sessionId));
            assertTrue(mpMessage.getString("sid").equals(sessionId));
            assertTrue(mpMessage.getString("ct").equals(Long.toString(firstRunTime)));
            assertFalse(TextUtils.isEmpty(mpMessage.getString("dct")));
        }catch (JSONException jse){
            fail(jse.toString());
        }
    }

    @Test
    public void testLogEvent(){
        try {
            MParticle.getInstance().logEvent(null, null);
            MParticle.getInstance().logEvent("some event", null);
            MParticle.getInstance().logEvent(null, MParticle.EventType.Other);
            MParticle.getInstance().logEvent("some event", MParticle.EventType.Other);
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testLogError(){
        try {
            MParticle.getInstance().logError("message");
            MParticle.getInstance().logError(null);
            MParticle.getInstance().logError("message", null);
            MParticle.getInstance().logError("message", new HashMap<String, String>());
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testEventLength() {
        try {
            MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).build();
            MPMessage message = new MPMessage.Builder(Constants.MessageType.EVENT, "whatever", null)
                    .name(event.getEventName())
                    .sessionStartTime(1234)
                    .timestamp(1235)
                    .length(event.getLength())
                    .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                    .build();

            assertNull(message.opt("el"));
            assertNull(message.getAttributes());

            Map<String, String> info = new HashMap<String, String>(1);
            info.put("EventLength", "321");
            MPEvent event2 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).info(info).build();
            MPMessage message2 = new MPMessage.Builder(Constants.MessageType.EVENT, "whatever", null)
                    .name(event2.getEventName())
                    .sessionStartTime(1234)
                    .timestamp(1235)
                    .length(event2.getLength())
                    .attributes(MPUtility.enforceAttributeConstraints(event2.getInfo()))
                    .build();

            assertEquals(message2.getAttributes().getString("EventLength"), "321");

            MPEvent event3 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).build();
            MPMessage message3 = new MPMessage.Builder(Constants.MessageType.EVENT, "whatever", null)
                    .name(event3.getEventName())
                    .sessionStartTime(1234)
                    .timestamp(1235)
                    .length(event3.getLength())
                    .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                    .build();

            assertEquals(message3.getAttributes().getString("EventLength"), "123");


        }catch(Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testLogLtv(){
        try {
            MParticle.getInstance().logLtvIncrease(null, "event name", new HashMap<String, String>());
            MParticle.getInstance().logLtvIncrease(new BigDecimal("5"), null, new HashMap<String, String>());
            MParticle.getInstance().logLtvIncrease(new BigDecimal("5"), "event name", null);
            MParticle.getInstance().logLtvIncrease(new BigDecimal("5"), "event name", new HashMap<String, String>());
        }catch(Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testLogProductEvent(){
        try{
            MParticle.getInstance().logProductEvent(MPProduct.Event.ADD_TO_CART, new MPProduct.Builder("name", "sku").build());
            MParticle.getInstance().logProductEvent(null, new MPProduct.Builder("name", "sku").build());
            MParticle.getInstance().logProductEvent(MPProduct.Event.ADD_TO_CART, null);
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testBreadcrumb(){
        try{
            MParticle.getInstance().leaveBreadcrumb(null);
            MParticle.getInstance().leaveBreadcrumb("asdasd");
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testLogScreen(){
        try{
            MParticle.getInstance().logScreen(null);
            MParticle.getInstance().logScreen("screen name");
            MParticle.getInstance().logScreen("screen name", null);
            MParticle.getInstance().logScreen("screen name", new HashMap<String, String>());
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testLogException(){
        try{
            MParticle.getInstance().logException(new NullPointerException());
            MParticle.getInstance().logException(new NullPointerException(), new HashMap<String, String>(), "message");
            MParticle.getInstance().logException(new NullPointerException(), null, "message");
            MParticle.getInstance().logException(new NullPointerException(), new HashMap<String, String>(), null);
        }catch (Exception e){

        }
    }

}
