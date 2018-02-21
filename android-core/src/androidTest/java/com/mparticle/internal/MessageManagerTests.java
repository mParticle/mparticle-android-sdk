package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.AccessUtils;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.database.tables.mp.SessionTable;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.testutils.MParticleUtils;
import com.mparticle.testutils.mock.MockContext;

import junit.framework.Assert;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class MessageManagerTests extends BaseCleanInstallEachTest {

    @Test
    public void testInstallReferrerMigration() throws Exception {
        //test previously installed
        setFirstRunLegacy(true, "key");
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        assertTrue(getMessageManager().isFirstRunForMessage());
        assertTrue(getMessageManager().isFirstRunForAST());

        setFirstRunLegacy(false, "key");
        MParticle.setInstance(null);
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        assertFalse(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());
    }

    @Test
    public void testInstallReferrerFlags() throws Exception {
        // test that both AST and First Run flags get properly flipped when their
        // corresponding setters get called
        setFirstRunLegacy(true, "key");
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        assertTrue(getMessageManager().isFirstRunForAST());
        getMessageManager().setFirstRunForAST(false);
        assertFalse(getMessageManager().isFirstRunForAST());

        assertTrue(getMessageManager().isFirstRunForMessage());
        getMessageManager().setFirstRunForMessage(false);
        assertFalse(getMessageManager().isFirstRunForMessage());

        // same thing, but make sure nothing funky happens when the order gets
        // reversed
        MParticle.setInstance(null);
        setFirstRunLegacy(true, "key");
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());

        assertTrue(getMessageManager().isFirstRunForMessage());
        getMessageManager().setFirstRunForMessage(false);
        assertFalse(getMessageManager().isFirstRunForMessage());

        assertTrue(getMessageManager().isFirstRunForAST());
        getMessageManager().setFirstRunForAST(false);
        assertFalse(getMessageManager().isFirstRunForAST());

        // test that the flags remain persisted when the application restarts
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());

        assertFalse(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());

    }

    @Test
    public void testDuplicateSessionEnds() throws Exception {
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        Session session = new Session();
        session.start(new MockContext());
        getMessageManager().startSession(session);
        MParticleUtils.awaitStoreMessage();
        BaseMPMessage message = getMessageManager().getMParticleDBManager().getSessionForSessionEndMessage(session.mSessionID, null, session.getMpids());
        Assert.assertNotNull(message);
        getMessageManager().getMParticleDBManager().updateSessionStatus(session.mSessionID, SessionTable.SessionTableColumns.STATUS);
        message = getMessageManager().getMParticleDBManager().getSessionForSessionEndMessage(session.mSessionID, null , session.getMpids());
        Assert.assertNull(message);
    }

    @Test
    public void testInstallReferrerFlagsEdgeCases() throws Exception {
        setFirstRunLegacy(true, "key");

        //start SDK, and send only AST, but not First Run message
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        assertTrue(getMessageManager().isFirstRunForAST());
        getMessageManager().setFirstRunForAST(false);
        assertFalse(getMessageManager().isFirstRunForAST());

        //restart SDK, and assert that AST message has been flagged, but First Run message has not
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());

        assertTrue(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());


        setFirstRunLegacy(true, "key");

        //start SDK, and send only First Run message, but not AST
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());
        assertTrue(getMessageManager().isFirstRunForMessage());
        getMessageManager().setFirstRunForMessage(false);
        assertFalse(getMessageManager().isFirstRunForMessage());

        //restart SDK, and assert that First Run message has been flagged as sent, but AST has not
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build());

        assertFalse(getMessageManager().isFirstRunForMessage());
        assertTrue(getMessageManager().isFirstRunForAST());

    }

    /**
     * simulates the install state to settings pre 5.0.9 || pre 4.17.1
     * @param firstRun
     */
    private void setFirstRunLegacy(boolean firstRun, String key) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .remove(Constants.PrefKeys.FIRSTRUN_AST + key)
                .remove(Constants.PrefKeys.FIRSTRUN_MESSAGE + key)
                .apply();
        if (firstRun) {
            sharedPreferences.edit().remove(Constants.PrefKeys.FIRSTRUN_OBSELETE + key).apply();
        } else {
            sharedPreferences.edit().putBoolean(Constants.PrefKeys.FIRSTRUN_OBSELETE + key, false).apply();
        }
    }

    private MessageManager getMessageManager() {
        return AccessUtils.getMessageManager();
    }
}
