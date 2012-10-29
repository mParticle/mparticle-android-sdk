package com.mparticle;

import java.util.Random;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.test.AndroidTestCase;

import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.MessageKey;

public class MessageManagerTests extends AndroidTestCase {

    // commonly used test values - not used by every case
    String mSessionId;
    long mMsgTime;
    long mSessionStartTime;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSessionId = UUID.randomUUID().toString();
        mMsgTime = System.currentTimeMillis();
        mSessionStartTime = mMsgTime - 10*1000;
        MessageManager.setLocation(null);
    }

    // creates an 'ss' message - with no 'sid' property
    public void testCreateSessionStartMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.SESSION_START, mSessionId, mSessionStartTime, mSessionStartTime, null, null, true);
        assertNotNull(message.toString());
        assertSame(MessageType.SESSION_START, message.getString(MessageKey.TYPE));
        assertEquals(mSessionId, message.getString(MessageKey.ID));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
        assertFalse(message.has(MessageKey.SESSION_START_TIMESTAMP));
    }

    // creates an 'se' message without attributes
    public void testCreateSessionEndMessage() throws JSONException {
        long sessionLength= mMsgTime - mSessionStartTime;
        JSONObject message = MessageManager.createMessageSessionEnd(mSessionId, mSessionStartTime, mMsgTime, sessionLength);
        assertNotNull(message.toString());
        assertEquals(MessageType.SESSION_END, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.SESSION_START_TIMESTAMP));
        assertEquals(sessionLength, message.getLong(MessageKey.SESSION_LENGTH));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'e' message without attributes
    public void testCreateCustomEventMessage() throws JSONException {
        String eventName = "event1";
        JSONObject message = MessageManager.createMessage(MessageType.CUSTOM_EVENT, mSessionId, mSessionStartTime, mMsgTime, eventName, null, true);
        assertNotNull(message.toString());
        assertEquals(MessageType.CUSTOM_EVENT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.SESSION_START_TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(eventName, message.getString(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'e' message with attributes
    public void testCreateCustomEventWithAttributesMessage() throws JSONException {
        String eventName = "event2";
        JSONObject eventAttrs=new JSONObject("{key1:'value1'}");

        JSONObject message = MessageManager.createMessage(MessageType.CUSTOM_EVENT, mSessionId, mSessionStartTime, mMsgTime, eventName, eventAttrs, true);
        assertNotNull(message.toString());
        assertEquals(MessageType.CUSTOM_EVENT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.SESSION_START_TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(eventName, message.getString(MessageKey.NAME));
        assertTrue(message.has(MessageKey.ATTRIBUTES));
    }

    // creates a 'v' message with attributes
    public void testCreateScreenViewMessage() throws JSONException {
        String viewName = "view1";

        JSONObject message = MessageManager.createMessage(MessageType.SCREEN_VIEW, mSessionId, mSessionStartTime, mMsgTime, viewName, null, true);
        assertNotNull(message.toString());
        assertEquals(MessageType.SCREEN_VIEW, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.SESSION_START_TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(viewName, message.getString(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates a 'v' message with attributes
    public void testCreateScreenViewWithAttributesMessage() throws JSONException {
        String viewName = "view2";
        JSONObject eventAttrs=new JSONObject("{key2:'value2'}");

        JSONObject message = MessageManager.createMessage(MessageType.SCREEN_VIEW, mSessionId, mSessionStartTime, mMsgTime, viewName, eventAttrs, true);
        assertNotNull(message.toString());
        assertEquals(MessageType.SCREEN_VIEW, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionStartTime, message.getLong(MessageKey.SESSION_START_TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(viewName, message.getString(MessageKey.NAME));
        assertTrue(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'o' message
    public void testCreateOptOutMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.OPT_OUT, null, 0, mMsgTime, null, null, false);
        assertNotNull(message.toString());
        assertEquals(MessageType.OPT_OUT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_START_TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'x' message with attributes
    public void testCreateErrorMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.ERROR, null, 0, mMsgTime, null, null, false);
        assertNotNull(message.toString());
        assertEquals(MessageType.ERROR, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertFalse(message.has(MessageKey.SESSION_ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_START_TIMESTAMP));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
        // TODO: implement error message tests when ready
        // assertTrue(message.has(MessageKey.ERROR_TYPE));
        // assertTrue(message.has(MessageKey.ERROR_MESSAGE));
        // assertTrue(message.has(MessageKey.ERROR_STACK_TRACE));
    }

    // creates a message with location provided and requested
    public void testCreateWithLocationMessage() throws JSONException {
        Location testLocation=new Location("test");
        Random r = new Random();
        double testLatitude = ( 360.0 * r.nextDouble() - 180.0);
        double testLongitude = ( 360.0 * r.nextDouble() - 180.0);
        testLocation.setLatitude(testLatitude);
        testLocation.setLongitude(testLongitude);
        MessageManager.setLocation(testLocation);

        JSONObject message = MessageManager.createMessage(MessageType.SESSION_START, mSessionId, mSessionStartTime, mSessionStartTime, null, null, true);
        assertNotNull(message.toString());
        assertSame(MessageType.SESSION_START, message.getString(MessageKey.TYPE));
        assertEquals("test", message.getString(MessageKey.DATA_CONNECTION));
        assertTrue(message.has(MessageKey.LATITUDE));
        assertEquals(testLatitude, message.getDouble(MessageKey.LATITUDE));
        assertTrue(message.has(MessageKey.LONGITUDE));
        assertEquals(testLongitude, message.getDouble(MessageKey.LONGITUDE));
    }


    // creates a message with location provided but not requested
    public void testCreateWithLocationIgnoredMessage() throws JSONException {
        Location testLocation=new Location("test");
        Random r = new Random();
        double testLatitude = ( 360.0 * r.nextDouble() - 180.0);
        double testLongitude = ( 360.0 * r.nextDouble() - 180.0);
        testLocation.setLatitude(testLatitude);
        testLocation.setLongitude(testLongitude);
        MessageManager.setLocation(testLocation);

        JSONObject message = MessageManager.createMessage(MessageType.OPT_OUT, mSessionId, 0, mMsgTime, null, null, false);
        assertNotNull(message.toString());
        assertSame(MessageType.OPT_OUT, message.getString(MessageKey.TYPE));
        assertFalse(message.has(MessageKey.DATA_CONNECTION));
        assertFalse(message.has(MessageKey.LATITUDE));
        assertFalse(message.has(MessageKey.LONGITUDE));
    }

    // creates a message with location requested but unavailable
    public void testCreateWithLocationMissingMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.SESSION_START, mSessionId, mSessionStartTime, mSessionStartTime, null, null, true);
        assertNotNull(message.toString());
        assertSame(MessageType.SESSION_START, message.getString(MessageKey.TYPE));
        assertFalse(message.has(MessageKey.DATA_CONNECTION));
        assertFalse(message.has(MessageKey.LATITUDE));
        assertFalse(message.has(MessageKey.LONGITUDE));
    }

}
