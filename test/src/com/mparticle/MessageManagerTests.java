package com.mparticle;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.AndroidTestCase;

import com.mparticle.MessageManager.MessageKey;
import com.mparticle.MessageManager.MessageType;

public class MessageManagerTests extends AndroidTestCase {

    // commonly used test values - not used by every case
    String mSessionId;
    long mMsgTime;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSessionId = UUID.randomUUID().toString();
        mMsgTime = System.currentTimeMillis();
    }

    // creates an 'ss' message - with no 'sid' property
    public void testCreateSessionStartMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.SESSION_START, mSessionId, mMsgTime, null, null);
        assertNotNull(message.toString());
        assertSame(MessageType.SESSION_START, message.getString(MessageKey.TYPE));
        assertEquals(mSessionId, message.getString(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'se' message without attributes
    // NOTE: duration is added to the message at insert time and not tested here
    public void testCreateSessionEndMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.SESSION_END, mSessionId, mMsgTime, null, null);
        assertNotNull(message.toString());
        assertEquals(MessageType.SESSION_END, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'e' message without attributes
    public void testCreateCustomEventMessage() throws JSONException {
        String eventName = "event1";
        JSONObject message = MessageManager.createMessage(MessageType.CUSTOM_EVENT, mSessionId, mMsgTime, eventName, null);
        assertNotNull(message.toString());
        assertEquals(MessageType.CUSTOM_EVENT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(eventName, message.getString(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'e' message with attributes
    public void testCreateCustomEventWithAttributesMessage() throws JSONException {
        String eventName = "event2";
        JSONObject eventAttrs=new JSONObject("{key1:'value1'}");

        JSONObject message = MessageManager.createMessage(MessageType.CUSTOM_EVENT, mSessionId, mMsgTime, eventName, eventAttrs);
        assertNotNull(message.toString());
        assertEquals(MessageType.CUSTOM_EVENT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(eventName, message.getString(MessageKey.NAME));
        assertTrue(message.has(MessageKey.ATTRIBUTES));
    }

    // creates a 'v' message with attributes
    public void testCreateScreenViewMessage() throws JSONException {
        String viewName = "view1";

        JSONObject message = MessageManager.createMessage(MessageType.SCREEN_VIEW, mSessionId, mMsgTime, viewName, null);
        assertNotNull(message.toString());
        assertEquals(MessageType.SCREEN_VIEW, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(viewName, message.getString(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

    // creates a 'v' message with attributes
    public void testCreateScreenViewWithAttributesMessage() throws JSONException {
        String viewName = "view2";
        JSONObject eventAttrs=new JSONObject("{key2:'value2'}");

        JSONObject message = MessageManager.createMessage(MessageType.SCREEN_VIEW, mSessionId, mMsgTime, viewName, eventAttrs);
        assertNotNull(message.toString());
        assertEquals(MessageType.SCREEN_VIEW, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertEquals(mSessionId, message.getString(MessageKey.SESSION_ID));
        assertNotSame(mSessionId, message.getString(MessageKey.ID));
        assertEquals(viewName, message.getString(MessageKey.NAME));
        assertTrue(message.has(MessageKey.ATTRIBUTES));
    }

    // creates an 'o' message
    public void testCreateOptOutMessage() throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.OPT_OUT, null, mMsgTime, null, null);
        assertNotNull(message.toString());
        assertEquals(MessageType.OPT_OUT, message.getString(MessageKey.TYPE));
        assertTrue(message.has(MessageKey.ID));
        assertEquals(mMsgTime, message.getLong(MessageKey.TIMESTAMP));
        assertFalse(message.has(MessageKey.SESSION_ID));
        assertFalse(message.has(MessageKey.NAME));
        assertFalse(message.has(MessageKey.ATTRIBUTES));
    }

}
