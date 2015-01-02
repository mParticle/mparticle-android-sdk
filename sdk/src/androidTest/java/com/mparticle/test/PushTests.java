package com.mparticle.test;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MPCloudBackgroundMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.ProviderCloudMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created by sdozor on 12/29/14.
 */
public class PushTests extends AndroidTestCase {

    static final String[] MESSAGE_KEYS = {"mp_message","com.urbanairship.push.ALERT","alert","a","message"};

    static final String MP_JSON = "{ \"m_cmd\":1, \"m_cid\":123, \"m_cntid\":123, \"m_expy\":1519987318000, \"m_ldt\":\"2014-10-23T17:45:00Z\", \"m_t\":\"Notification title\", \"m_m\":\"The longer notification message\", \"m_sm\":\"The text shown at the bottom of exp. notifications\", \"m_g\":23, \"m_dact\": \"com.mparticle.particlebox.MainActivity\", \"m_sia\": true, \"m_iamt\": \"mp.dark\", \"m_l_c\":2312121, \"m_l_off\":1000, \"m_l_on\":1000, \"m_n\":42, \"m_ao\":true, \"m_p\":3, \"m_s\":\"mysoundname\", \"m_bi\":\"http://bigimage.png\", \"m_bt\":\"some pretty long text, up to maybe 340 characters\", \"m_ib_1\":\"inbox line 1\", \"m_ib_2\":\"inbox line 2\", \"m_ib_3\":\"inbox line 3\", \"m_ib_4\":\"inbox line 4\", \"m_ib_5\":\"inbox line 5\", \"m_xt\":\"Expanded notification title\", \"m_v\":\"100, 300, 100, 300, 100, 300, 400, 300, 100, 300, 100, 300, 400, 300, 100, 300, 100, 500, 100, 200, 50, 500\", \"m_a1_aid\":\"someactionid\", \"m_a1_ai\":\"actioniconname\", \"m_a1_at\":\"actiontitle\", \"m_a1_act\":\"com.mparticle.particlebox.OtherActivity\", \"m_a2_aid\":\"someactionid\", \"m_a2_ai\":\"actioniconname\", \"m_a2_at\":\"actiontitle\", \"m_a2_act\":\"com.mparticle.particlebox.AnotherActivity\", \"m_a3_aid\":\"someactionid\", \"m_a3_ai\":\"actioniconname\", \"m_a3_at\":\"actiontitle\", \"m_a3_act\":\"com.mparticle.particlebox.YetAnotherActivity\", \"m_vi\":\"public\", \"ARBITRARY_KEY\": \"some value\" }";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void testProviderMessageCreation(){
        JSONArray messageKeys = getMockMessageKeys();
        String key = MESSAGE_KEYS[2];
        Intent pushIntent = new Intent();
        Bundle pushExtras = new Bundle();
        String notificationMessage = "some notification message...";
        pushExtras.putString(key, notificationMessage);
        pushIntent.putExtras(pushExtras);

        AbstractCloudMessage message = null;
        try {
            message = AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }

        assertTrue("Message was not parsed into a ProviderCloudMessage! " + message.getClass().toString(), (message instanceof ProviderCloudMessage));
        Notification notification = ((ProviderCloudMessage) message).buildNotification(getContext());
        assertEquals("Title differed!",notificationMessage, notification.extras.get(Notification.EXTRA_TEXT));
        assertNotNull(notification);
        assertNotNull(notification.contentIntent);
    }

    public void testBasicMpMessageCreation(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();

        pushIntent.putExtras(getMpExtras(MP_JSON));

        AbstractCloudMessage message = null;
        try {
            message = AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }
        assertTrue("Message was not parsed into an MPCloudNotification! " + message.getClass().toString(), message instanceof MPCloudNotificationMessage);
        Notification notification = ((MPCloudNotificationMessage) message).buildNotification(getContext());

        assertNotNull(notification);
        assertNotNull(notification.contentIntent);
    }

    public void testMpMessageRedacted(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();

        pushIntent.putExtras(getMpExtras(MP_JSON));

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }
        String text = message.getPrimaryText(getContext());
        assertFalse(TextUtils.isEmpty(text));
        assertFalse(message.getRedactedJsonPayload().toString().contains(text));
    }

    public void testPushActionCreation(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        String actionId = UUID.randomUUID().toString();
        extras.putString("m_a1_aid", actionId);
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }
        assertNotNull(message.getActions());
        assertTrue(message.getActions().length == 3);
        assertEquals(actionId, message.getActions()[0].getActionId());
    }

    public void testMpPushMessageIntent(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();

        pushIntent.putExtras(getMpExtras(MP_JSON));

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }
        assertNotNull(message.buildNotification(getContext()).contentIntent);
    }

    public void testProviderMessageRedacted(){
        JSONArray messageKeys = getMockMessageKeys();
        String key = MESSAGE_KEYS[2];
        Intent pushIntent = new Intent();
        Bundle pushExtras = new Bundle();
        String notificationMessage = "some notification message...";
        pushExtras.putString(key, notificationMessage);
        pushIntent.putExtras(pushExtras);

        AbstractCloudMessage message = null;
        try {
            message = AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            fail(e.getMessage());
        }
        assertFalse(message.getRedactedJsonPayload().toString().contains(notificationMessage));
    }

    public void testMpMessageExpiry(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        extras.putString("m_expy", Long.toString(System.currentTimeMillis()));
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            assertNotNull(e);
            return;
        }
        fail("Message should have been expired!");
    }

    public void testCampaignBatchExpiration(){
        JSONArray messageKeys = getMockMessageKeys();

        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        extras.putString("m_expy", Long.toString(System.currentTimeMillis()));
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, messageKeys);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            assertNotNull(e);
            return;
        }
    }

    public void testCommands(){
        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        extras.putString("m_cmd", Integer.toString(MPCloudNotificationMessage.COMMAND_ALERT_LOCALTIME));
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, null);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {

        }
        assertTrue("Message should have been delayed.", message.isDelayed());
        assertFalse("Message should have been delayed.", message.shouldDisplay());

        extras.putString("m_cmd", Integer.toString(MPCloudNotificationMessage.COMMAND_ALERT_NOW));
        pushIntent.putExtras(extras);

        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, null);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {

        }
        assertTrue("Message should have been displayed.", message.shouldDisplay());
    }

    public void testVariousDefaultFields(){
        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, null);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {

        }
        assertFalse(TextUtils.isEmpty(message.getPrimaryText(getContext())));
        assertNotNull(message.getLargeIcon(getContext()));
        assertFalse(TextUtils.isEmpty(message.getContentTitle(getContext())));
    }

    public void testDownloadedImages(){
        Intent pushIntent = new Intent();
        Bundle extras = getMpExtras(MP_JSON);
        extras.putString("m_li","https://static.mparticle.com/public/logo_highres.png" );
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, null);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {

        }
        assertNotNull(message.getLargeIcon(null));
    }

    public void testSilentPush(){
        Bundle extras = getMpExtras(MP_JSON);
        extras.putString("m_cmd", Integer.toString(MPCloudNotificationMessage.COMMAND_DONOTHING));
        assertTrue(MPCloudBackgroundMessage.processSilentPush(getContext(), extras));

        extras.putString("m_cmd", Integer.toString(MPCloudNotificationMessage.COMMAND_ALERT_CONFIG_REFRESH));
        assertTrue(MPCloudBackgroundMessage.processSilentPush(getContext(), extras));

        extras.putString("m_cmd", Integer.toString(MPCloudNotificationMessage.COMMAND_ALERT_NOW));
        assertFalse(MPCloudBackgroundMessage.processSilentPush(getContext(), extras));
    }

    private JSONArray getMockMessageKeys(){

        JSONArray jsonArray = new JSONArray();
        for (String key : MESSAGE_KEYS){
            jsonArray.put(key);
        }
        return jsonArray;
    }

    public static Bundle getMpExtras(String jsonString){
        try {
            Bundle extras = new Bundle();
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()){
                String key = keys.next();
                String value = json.getString(key);
                extras.putString(key, value);
            }
            return extras;
        } catch (JSONException e) {
            fail("Failed to generate MP extras." + e.toString());
            return null;
        }
    }
}
