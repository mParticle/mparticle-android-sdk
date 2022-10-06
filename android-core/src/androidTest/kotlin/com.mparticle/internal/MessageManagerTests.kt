package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.AccessUtils;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.database.tables.SessionTable;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessageManagerTests extends BaseCleanInstallEachTest {


    @Test
    public void testInstallReferrerMigration() throws Exception {
        //test previously installed
        setFirstRunLegacy(true, "key");
        assertNull(MParticle.getInstance());
        startMParticle();
        getMessageManager().setFirstRunForAST(true);

        assertTrue(getMessageManager().isFirstRunForMessage());
        assertTrue(getMessageManager().isFirstRunForAST());

        setFirstRunLegacy(false, "key");
        MParticle.setInstance(null);
        startMParticle();
        assertFalse(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());
    }

    @Test
    public void testInstallReferrerFlags() throws Exception {
        // test that both AST and First Run flags get properly flipped when their
        // corresponding setters get called
        setFirstRunLegacy(true, "key");
        assertNull(MParticle.getInstance());
        startMParticle();
        getMessageManager().setFirstRunForAST(true);

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
        startMParticle();

        assertTrue(getMessageManager().isFirstRunForMessage());
        getMessageManager().setFirstRunForMessage(false);
        assertFalse(getMessageManager().isFirstRunForMessage());

        assertTrue(getMessageManager().isFirstRunForAST());
        getMessageManager().setFirstRunForAST(false);
        assertFalse(getMessageManager().isFirstRunForAST());

        // test that the flags remain persisted when the application restarts
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        startMParticle();

        assertFalse(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());

    }

    @Test
    public void testDuplicateSessionEnds() throws Exception {
        startMParticle();
        InternalSession session = new InternalSession();
        session.start(mContext);
        getMessageManager().startSession(session);
        com.mparticle.internal.AccessUtils.awaitMessageHandler();
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
        startMParticle();
        getMessageManager().setFirstRunForAST(true);

        assertTrue(getMessageManager().isFirstRunForAST());
        getMessageManager().setFirstRunForAST(false);
        assertFalse(getMessageManager().isFirstRunForAST());

        //restart SDK, and assert that AST message has been flagged, but First Run message has not
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        startMParticle();

        assertTrue(getMessageManager().isFirstRunForMessage());
        assertFalse(getMessageManager().isFirstRunForAST());


        setFirstRunLegacy(true, "key");

        //start SDK, and send only First Run message, but not AST
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        startMParticle();
        assertTrue(getMessageManager().isFirstRunForMessage());
        getMessageManager().setFirstRunForMessage(false);
        assertFalse(getMessageManager().isFirstRunForMessage());

        //restart SDK, and assert that First Run message has been flagged as sent, but AST has not
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
        startMParticle();

        assertFalse(getMessageManager().isFirstRunForMessage());
        assertTrue(getMessageManager().isFirstRunForAST());

    }

    @Test
    public void testDelayedStartInvoked() throws InterruptedException {
        MParticleOptions options = MParticleOptions.builder(mContext).credentials("key", "secret").build();
        MParticle.start(options);
        MessageManager messageManager = AccessUtils.getMessageManager();
        assertFalse(messageManager.hasDelayedStartOccurred());
        MessageHandler handler = com.mparticle.internal.AccessUtils.getMessageHandler();
        handler.sendMessage(handler.obtainMessage());
        com.mparticle.internal.AccessUtils.awaitMessageHandler();
        assertTrue(messageManager.hasDelayedStartOccurred());

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
