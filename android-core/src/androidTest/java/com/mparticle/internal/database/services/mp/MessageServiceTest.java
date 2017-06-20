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

import static junit.framework.Assert.assertEquals;

public class MessageServiceTest extends BaseMPServiceTest {

    @Before
    public void cleardb() {
        clearDatabase();
    }

    @Test
    public void testMessageFlow() throws JSONException {
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        List<MessageService.ReadyMessage> messageList = MessageService.getMessagesForUpload(database, 1);
        assertEquals(messageList.size(), 10);
        assertEquals(MessageService.getSessionHistory(database, "123", 1).size(), 0);

        int max = getMaxId(messageList);
        int numUpldated = MessageService.markMessagesAsUploaded(database, max, 1);
        assertEquals(numUpldated, 10);
        assertEquals(MessageService.getMessagesForUpload(database, 1).size(), 0);
        assertEquals(MessageService.getSessionHistory(database, "", 1).size(), 10);
    }

    @Test
    public void testMessageFlowMax() throws JSONException {
        for (int i = 0; i < 210; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        List<MessageService.ReadyMessage> messages = MessageService.getMessagesForUpload(database, 1);
        assertEquals(messages.size(), 100);
        assertEquals(MessageService.getSessionHistory(database, "", 1).size(), 0);

        int max = getMaxId(messages);
        int numUpdated = MessageService.markMessagesAsUploaded(database, max, 1);
        assertEquals(numUpdated, 100);
        assertEquals(MessageService.getSessionHistory(database, "", 1).size(), 100);

        messages = MessageService.getMessagesForUpload(database, 1);
        max = getMaxId(messages);
        numUpdated = MessageService.markMessagesAsUploaded(database, max, 1);
        assertEquals(numUpdated, 200);
        assertEquals(MessageService.getSessionHistory(database, "", 1).size(), 100);

        messages = MessageService.getMessagesForUpload(database, 1);
        max = getMaxId(messages);
        numUpdated = MessageService.markMessagesAsUploaded(database, max, 1);
        assertEquals(numUpdated, 210);
        assertEquals(MessageService.getSessionHistory(database, "", 1).size(), 100);

    }

    @Test
    public void testDeleteOldMessages() throws JSONException {
        String currentSession = "123";
        String newSession = "234";
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(currentSession), 1);
        }
        assertEquals(MessageService.markMessagesAsUploaded(database, 10, 1), 10);
        assertEquals(MessageService.getMessagesForUpload(database, 1).size(), 0);

        MessageService.deleteOldMessages(database, currentSession, 1);
        assertEquals(MessageService.getSessionHistory(database, newSession, 1).size(), 10);

        MessageService.deleteOldMessages(database, newSession, 1);
        assertEquals(MessageService.getSessionHistory(database, newSession, 1).size(), 0);

    }

    @Test
    public void testMessagesMaxSize() throws JSONException {
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        assertEquals(MessageService.getMessagesForUpload(database, 1).size(), 10);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Constants.LIMIT_MAX_MESSAGE_SIZE; i++) {
            builder.append("ab");
        }
        MPMessage message = new MPMessage.Builder(builder.toString(), new Session(), new Location("New York City"), 1).build();
        MessageService.insertMessage(database, "apiKey", message, 1);

        assertEquals(MessageService.getMessagesForUpload(database, 1).size(), 10);
        for (int i = 0; i < 10; i++) {
            MessageService.insertMessage(database, "apiKey", getMpMessage(), 1);
        }
        assertEquals(MessageService.getMessagesForUpload(database, 1).size(), 20);
    }

    @Test
    public void testUpdateMessageStatus() {

    }

    private MPMessage getMpMessage() throws JSONException {
        return getMpMessage(String.valueOf(new Random().nextInt()));
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
