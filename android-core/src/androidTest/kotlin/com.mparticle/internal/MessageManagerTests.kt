package com.mparticle.internal

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.database.tables.SessionTable
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.junit.Assert
import org.junit.Test

class MessageManagerTests : BaseCleanInstallEachTest() {
    @Test
    @Throws(Exception::class)
    fun testInstallReferrerMigration() {
        //test previously installed
        setFirstRunLegacy(true, "key")
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        messageManager.isFirstRunForAST = true
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
        setFirstRunLegacy(false, "key")
        MParticle.setInstance(null)
        startMParticle()
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallReferrerFlags() {
        // test that both AST and First Run flags get properly flipped when their
        // corresponding setters get called
        setFirstRunLegacy(true, "key")
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        messageManager.isFirstRunForAST = true
        Assert.assertTrue(messageManager.isFirstRunForAST)
        messageManager.isFirstRunForAST = false
        Assert.assertFalse(messageManager.isFirstRunForAST)
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        messageManager.isFirstRunForMessage = false
        Assert.assertFalse(messageManager.isFirstRunForMessage)

        // same thing, but make sure nothing funky happens when the order gets
        // reversed
        MParticle.setInstance(null)
        setFirstRunLegacy(true, "key")
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        messageManager.isFirstRunForMessage = false
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
        messageManager.isFirstRunForAST = false
        Assert.assertFalse(messageManager.isFirstRunForAST)

        // test that the flags remain persisted when the application restarts
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(Exception::class)
    fun testDuplicateSessionEnds() {
        startMParticle()
        val session = InternalSession()
        session.start(mContext)
        messageManager.startSession(session)
        AccessUtils.awaitMessageHandler()
        var message = messageManager.mParticleDBManager.getSessionForSessionEndMessage(
            session.mSessionID,
            null,
            session.mpids
        )
        Assert.assertNotNull(message)
        messageManager.mParticleDBManager.updateSessionStatus(
            session.mSessionID,
            SessionTable.SessionTableColumns.STATUS
        )
        message = messageManager.mParticleDBManager.getSessionForSessionEndMessage(
            session.mSessionID,
            null,
            session.mpids
        )
        Assert.assertNull(message)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallReferrerFlagsEdgeCases() {
        setFirstRunLegacy(true, "key")

        //start SDK, and send only AST, but not First Run message
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        messageManager.isFirstRunForAST = true
        Assert.assertTrue(messageManager.isFirstRunForAST)
        messageManager.isFirstRunForAST = false
        Assert.assertFalse(messageManager.isFirstRunForAST)

        //restart SDK, and assert that AST message has been flagged, but First Run message has not
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
        setFirstRunLegacy(true, "key")

        //start SDK, and send only First Run message, but not AST
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        messageManager.isFirstRunForMessage = false
        Assert.assertFalse(messageManager.isFirstRunForMessage)

        //restart SDK, and assert that First Run message has been flagged as sent, but AST has not
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDelayedStartInvoked() {
        val options = MParticleOptions.builder(mContext).credentials("key", "secret").build()
        MParticle.start(options)
        val messageManager = com.mparticle.AccessUtils.getMessageManager()
        Assert.assertFalse(messageManager.hasDelayedStartOccurred())
        val handler = AccessUtils.getMessageHandler()
        handler.sendMessage(handler.obtainMessage())
        AccessUtils.awaitMessageHandler()
        Assert.assertTrue(messageManager.hasDelayedStartOccurred())
    }

    /**
     * simulates the install state to settings pre 5.0.9 || pre 4.17.1
     * @param firstRun
     */
    private fun setFirstRunLegacy(firstRun: Boolean, key: String) {
        val sharedPreferences =
            mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove(Constants.PrefKeys.FIRSTRUN_AST + key)
            .remove(Constants.PrefKeys.FIRSTRUN_MESSAGE + key)
            .apply()
        if (firstRun) {
            sharedPreferences.edit().remove(Constants.PrefKeys.FIRSTRUN_OBSELETE + key).apply()
        } else {
            sharedPreferences.edit().putBoolean(Constants.PrefKeys.FIRSTRUN_OBSELETE + key, false)
                .apply()
        }
    }

    private val messageManager: MessageManager
        get() = com.mparticle.AccessUtils.getMessageManager()
}