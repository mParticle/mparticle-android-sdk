package com.mparticle.internal.database.services.mp;

import android.location.Location;
import android.os.Message;
import android.util.Log;

import com.mparticle.internal.Constants;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.Session;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

public class MessageServiceTest extends BaseMPServiceTest {
    Long mpid1, mpid2;

    @Before
    public void cleardb() {
        clearDatabase();
        mpid1 = new Random().nextLong();
        mpid2 = new Random().nextLong();
    }

    @Test
    public void testMessagesForUploadByMpid() throws Exception {
        for (int i = 0; i < 20; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), mpid1);
        }
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 20);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
        for (int i = 0; i < 30; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), Constants.TEMPORARY_MPID);
        }
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid1).size(), 20);
        assertEquals(MessageService.getMessagesForUpload(database, true, mpid2).size(), 0);
        assertEquals(MessageService.getMessagesForUpload(database, true, Constants.TEMPORARY_MPID).size(), 30);
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
        for (int i = 0; i < 35; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), mpid2);
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
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), mpid1);
        }
        for (int i = 0; i < 30; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), Constants.TEMPORARY_MPID);
        }
        for (int i = 0; i < 35; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), mpid2);
        }
        assertEquals(MessageService.markMessagesAsUploaded(database, Integer.MAX_VALUE), 55);
        assertEquals(MessageService.getSessionHistory(database, previousSession).size(), 55);
        assertEquals(MessageService.getSessionHistory(database, previousSession, true, mpid1).size(), 20);
        assertEquals(MessageService.getSessionHistory(database, previousSession, true, mpid2).size(), 35);
        assertEquals(MessageService.getSessionHistory(database, previousSession, false, mpid1).size(), 35);
        assertEquals(MessageService.getSessionHistory(database, previousSession, false, Constants.TEMPORARY_MPID).size(), 55);
    }

    @Test
    public void testMessageFlow() throws JSONException {
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
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
        for (int i = 0; i < 210; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
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
        assertEquals(numUpdated, 200);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 100);

        messages = MessageService.getMessagesForUpload(database);
        max = getMaxId(messages);
        numUpdated = MessageService.markMessagesAsUploaded(database, max);
        assertEquals(numUpdated, 210);
        assertEquals(MessageService.getSessionHistory(database, "").size(), 100);
    }

    @Test
    public void testDeleteOldMessages() throws JSONException {
        String currentSession = UUID.randomUUID().toString();
        String newSession = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), 1);
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
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        assertEquals(MessageService.getMessagesForUpload(database).size(), 10);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Constants.LIMIT_MAX_MESSAGE_SIZE; i++) {
            builder.append("ab");
        }
        MPMessage message = new MPMessage.Builder(builder.toString(), new Session(), new Location("New York City"), 1).build();
        MessageService.insertMessage(database, "apiKey", message, 1);

        assertEquals(MessageService.getMessagesForUpload(database).size(), 10);
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        assertEquals(MessageService.getMessagesForUpload(database).size(), 20);
    }

    @Test
    public void testUpdateMessageStatus() {

    }

    private MPMessage getMpMessage() throws JSONException {
        return getMpMessage(UUID.randomUUID().toString());
    }

    private MPMessage getMpMessage(String sessionId) throws JSONException {
        Session session = new Session();
        session.mSessionID = sessionId;
        return new MPMessage.Builder("test", session, new Location("New York City"), 1).build();
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
