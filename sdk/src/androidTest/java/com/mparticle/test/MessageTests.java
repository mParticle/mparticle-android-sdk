package com.mparticle.test;

import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.MPMessage;

import org.json.JSONException;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Created by sdozor on 12/30/14.
 */
public class MessageTests extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MParticle.start(getContext());
    }

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

    public void testLogProductEvent(){
        try{
            MParticle.getInstance().logProductEvent(MPProduct.Event.ADD_TO_CART, new MPProduct.Builder("name", "sku").build());
            MParticle.getInstance().logProductEvent(null, new MPProduct.Builder("name", "sku").build());
            MParticle.getInstance().logProductEvent(MPProduct.Event.ADD_TO_CART, null);
        }catch (Exception e){
            fail(e.toString());
        }
    }

    public void testBreadcrumb(){
        try{
            MParticle.getInstance().leaveBreadcrumb(null);
            MParticle.getInstance().leaveBreadcrumb("asdasd");
        }catch (Exception e){
            fail(e.toString());
        }
    }

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
