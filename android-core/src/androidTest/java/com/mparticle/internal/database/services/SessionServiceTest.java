package com.mparticle.internal.database.services;

import android.database.Cursor;
import androidx.annotation.Nullable;

import com.mparticle.internal.BatchId;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.database.tables.SessionTable;
import com.mparticle.internal.messages.BaseMPMessage;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SessionServiceTest extends BaseMPServiceTest {

    @Test
    public void testUpdateSessionInstallReferrer() throws Exception {
        JSONObject fooObject = new JSONObject();
        String sessionId = UUID.randomUUID().toString();
        fooObject.put("foo", "bar");
        BaseMPMessage mpMessage = getMpMessage(sessionId);
        SessionService.insertSession(database, mpMessage, "foo-app-key", fooObject.toString(), fooObject.toString(), 1);
        fooObject = new JSONObject();
        String randomId = UUID.randomUUID().toString();
        fooObject.put("foo", randomId);
        SessionService.updateSessionInstallReferrer(database, fooObject, sessionId);
        Cursor cursor = null;
        try {
            cursor = SessionService.getSessions(database);
            while (cursor.moveToNext()) {
                String currentSessionId = cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.SESSION_ID));
                String appInfo = cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.APP_INFO));
                if (sessionId.equals(currentSessionId)) {
                    JSONObject appInfoObject = new JSONObject(appInfo);
                    assertEquals(randomId, appInfoObject.getString("foo"));
                    return;
                }
            }
        }finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        Assert.fail("Failed to find updated app customAttributes object.");
    }

    @Test
    public void flattenMessagesByBatchIdTest() {
        Map<BatchId, MessageBatch> batchMap = new HashMap<BatchId, MessageBatch>();
        batchMap.put(new BatchId(ran.nextLong(), "1", "a", 1), new MockMessageBatch(1));
        batchMap.put(new BatchId(ran.nextLong(), "2", null, null), new MockMessageBatch(2));
        batchMap.put(new BatchId(ran.nextLong(), "1", "a", 2), new MockMessageBatch(3));
        batchMap.put(new BatchId(ran.nextLong(), "1", "ab", null), new MockMessageBatch(4));
        batchMap.put(new BatchId(ran.nextLong(), "2", null, 3), new MockMessageBatch(5));
        batchMap.put(new BatchId(ran.nextLong(), "3", null, 3), new MockMessageBatch(6));
        batchMap.put(new BatchId(ran.nextLong(), "1", null, 3), new MockMessageBatch(7));

        Map<String, List<MessageBatch>> batchBySessionId = SessionService.flattenBySessionId(batchMap);
        assertEquals(4, batchBySessionId.get("1").size());
        assertEquals(2, batchBySessionId.get("2").size());
        assertEquals(1, batchBySessionId.get("3").size());

        //make sure the elements in the list are unique..no inadvertent copies
        List<MessageBatch> session1Batches = new ArrayList(batchBySessionId.get("1"));
        int size = session1Batches.size();
        for (MessageBatch messageBatch: session1Batches) {
            batchBySessionId.get("1").remove(messageBatch);
            assertEquals(--size, batchBySessionId.get("1").size());
        }
    }

    class MockMessageBatch extends MessageBatch {
        int id;

        public MockMessageBatch(int id) {
            super();
            this.id = id;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof MockMessageBatch) {
                return ((Integer)id).equals(((MockMessageBatch)obj).id);
            } else {
                return super.equals(obj);
            }
        }
    }
}
