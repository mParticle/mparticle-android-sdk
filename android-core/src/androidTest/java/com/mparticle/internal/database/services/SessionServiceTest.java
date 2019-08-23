package com.mparticle.internal.database.services;

import android.database.Cursor;

import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.tables.SessionTable;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

public class SessionServiceTest extends BaseMPServiceTest {

    @Test
    public void testUpdateSessionInstallReferrer() throws Exception {
        JSONObject fooObject = new JSONObject();
        String sessionId = UUID.randomUUID().toString();
        fooObject.put("foo", "bar");
        MessageManager.BaseMPMessage mpMessage = getMpMessage(sessionId);
        SessionService.insertSession(database, mpMessage, "foo-app-key", fooObject.toString(), fooObject.toString(), 1);
        fooObject = new JSONObject();
        String randomId = UUID.randomUUID().toString();
        fooObject.put("foo", randomId);
        SessionService.updateSessionInstallReferrer(database, fooObject, sessionId);
        Cursor cursor = null;
        try {
            cursor = SessionService.getSessions(database);
            while (cursor.moveToNext()) {
                String currentSessionId = cursor.getString(cursor.getColumnIndex(SessionTable.SessionTableColumns.SESSION_ID));
                String appInfo = cursor.getString(cursor.getColumnIndex(SessionTable.SessionTableColumns.APP_INFO));
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
}
