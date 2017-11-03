package com.mparticle.internal.database.services.mp;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.Session;
import com.mparticle.internal.database.tables.mp.SessionTable;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SessionServiceTest extends BaseMPServiceTest {

    private static Context context;

    @Override
    public void before() throws Exception {
        super.before();
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testUpdateSessionInstallReferrer() throws Exception {
        JSONObject fooObject = new JSONObject();
        String sessionId = UUID.randomUUID().toString();
        fooObject.put("foo", "bar");
        MPMessage mpMessage = getMpMessage(sessionId);
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
        Assert.fail("Failed to find updated app info object.");
    }
}
