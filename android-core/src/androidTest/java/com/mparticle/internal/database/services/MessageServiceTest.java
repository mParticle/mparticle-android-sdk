package com.mparticle.internal.database.services;

import android.location.Location;

import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.messages.BaseMPMessage;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessageServiceTest extends BaseMPServiceTest {
    Long mpid1, mpid2, mpid3;

    @Before
    public void before() throws Exception {
        mpid1 = ran.nextLong();
        mpid2 = ran.nextLong();
        mpid3 = ran.nextLong();
    }

    @Test
    public void testMessagesForUploadByMpid() throws Exception {
        for (int i = 0; i < 20; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), mpid1, null, null);
        }
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 20);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
        for (int i = 0; i < 30; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), Constants.TEMPORARY_MPID, null, null);
        }
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 20);
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid2).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 30);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
        for (int i = 0; i < 35; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), mpid2, null, null);
        }
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 20);
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid2).size(), 35);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 30);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 55);

        assertEquals(MessageService.markMessagesAsUploaded(database, Integer.MAX_VALUE), 55);
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid2).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 30);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 0);
    }

    @Test
    public void testSessionHistoryByMpid() throws Exception {
        String currentSession = UUID.randomUUID().toString();
        String previousSession = UUID.randomUUID().toString();
        for (int i = 0; i < 20; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), mpid1, null, null);
        }
        for (int i = 0; i < 30; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), Constants.TEMPORARY_MPID, null, null);
        }
        for (int i = 0; i < 35; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), mpid2, null, null);
        }
        assertEquals(MessageService.markMessagesAsUploaded(database, Integer.MAX_VALUE), 55);
        assertEquals(MessageService.getSessionHistory(database, previousSession).size(), 55);
        assertEquals(MessageService.getSessionHistory(database, previousSession, true, mpid1).size(), 20);
        assertEquals(MessageService.getSessionHistory(database, previousSession, true, mpid2).size(), 35);
        assertEquals(MessageService.getSessionHistory(database, previousSession, false, mpid1).size(), 35);
        assertEquals(MessageService.getSessionHistory(database, previousSession, false, Constants.TEMPORARY_MPID).size(), 55);
    }

    @Test
    public void testSessionHistoryAccuracy() throws Exception {
        String currentSession = UUID.randomUUID().toString();
        String previousSession = UUID.randomUUID().toString();
        BaseMPMessage testMessage;
        Long[] mpids = new Long[]{mpid1, mpid2, mpid3};
        Long testMpid;
        Map<String, BaseMPMessage> testMessages = new HashMap<String, BaseMPMessage>();
        for (int i = 0; i < 100; i++) {
            testMpid = mpids[mRandomUtils.randomInt(0, 3)];
            testMessage = getMpMessage(currentSession, testMpid);
            testMessages.put(testMessage.toString(), testMessage);
            MessageService.insertMessage(database, "apiKey",testMessage , testMpid, null, null);
        }
        assertEquals(MessageService.markMessagesAsUploaded(database, Integer.MAX_VALUE), 100);
        List<MessageService.ReadyMessage> readyMessages = MessageService.getSessionHistory(database, previousSession, false, Constants.TEMPORARY_MPID);
        assertEquals(readyMessages.size(), testMessages.size());
        for (MessageService.ReadyMessage readyMessage: readyMessages) {
            BaseMPMessage message = testMessages.get(readyMessage.getMessage());
            assertNotNull(message);
            assertEquals(readyMessage.getMpid(), message.getMpId());
            assertEquals(readyMessage.getMessage(), message.toString());
            assertEquals(readyMessage.getSessionId(), currentSession);
        }

    }

    @Test
    public void testMessageFlow() throws JSONException {
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1, "dataplan1", 1);
        }
        List<MessageService.ReadyMessage> messageList = MessageService.getMessagesForUpload(database);
        assertEquals(messageList.size(), 10);
        assertEquals(MessageService.getSessionHistory(database, "123").size(), 0);

        int max = getMaxId(messageList);
        int numUpldated = MessageService.markMessagesAsUploaded(database, max);
        assertEquals(numUpldated, 10);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 0);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 10);
    }

    @Test
    public void testMessageFlowMax() throws JSONException {
        for (int i = 0; i < 110; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1, null, null);
        }
        List<MessageService.ReadyMessage> messages = MessageService.getMessagesForUpload(database);
        assertEquals(messages.size(), 100);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 0);

        int max = getMaxId(messages);
        int numUpdated = MessageService.markMessagesAsUploaded(database, max);
        assertEquals(numUpdated, 100);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 100);

        messages = MessageService.getMessagesForUpload(database);
        max = getMaxId(messages);
        numUpdated = MessageService.markMessagesAsUploaded(database, max);
        assertEquals(numUpdated, 110);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 100);
    }

    @Test
    public void testDeleteOldMessages() throws JSONException {
        String currentSession = UUID.randomUUID().toString();
        String newSession = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), 1, null, null);
        }
        assertEquals(MessageService.markMessagesAsUploaded(database, 10), 10);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 0);

        MessageService.deleteOldMessages(database, currentSession);
        assertEquals(MessageService.getSessionHistory(database, newSession).size(), 10);

        MessageService.deleteOldMessages(database, newSession);
        assertEquals(MessageService.getSessionHistory(database, newSession).size(), 0);
    }

    @Test
    public void testMessagesMaxSize() throws JSONException {
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1, "a", 1);
        }
        assertEquals(MessageService.getMessagesForUpload(database).size(), 10);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Constants.LIMIT_MAX_MESSAGE_SIZE; i++) {
            builder.append("ab");
        }
        BaseMPMessage message = new BaseMPMessage.Builder(builder.toString()).build(new InternalSession(), new Location("New York City"), 1);
        MessageService.insertMessage(database, "apiKey", message, 1, "b", 2);

        assertEquals(MessageService.getMessagesForUpload(database).size(), 10);
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1, "c", 3);
        }
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
    }

    private int getMaxId(List<MessageService.ReadyMessage> messages) {
        int max = 0;
        for (MessageService.ReadyMessage message: messages) {
            if (message.getMessageId() > max) {
                max = message.getMessageId();
            }
        }
        return max;
    }
}
