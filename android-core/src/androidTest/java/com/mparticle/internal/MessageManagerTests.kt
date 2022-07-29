package com.mparticle.internal

import android.content.Context
import android.content.SharedPreferences
import com.mparticle.AccessUtils
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.database.tables.SessionTable
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.context
import com.mparticle.utils.startMParticle
import org.junit.Assert
import org.junit.Test

class MessageManagerTests : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testInstallReferrerMigration() {
        val apiKey = "apiKey"
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
        // test previously installed
        setFirstRunLegacy(true, apiKey)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        messageManager.isFirstRunForAST = true
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
        setFirstRunLegacy(false, apiKey)
        MParticle.setInstance(null)
        startMParticle(options)
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallReferrerFlags() {
        // test that both AST and First Run flags get properly flipped when their
        // corresponding setters get called
        val apiKey = "apiKey"
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
        setFirstRunLegacy(true, apiKey)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
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
        setFirstRunLegacy(true, "apiKey")
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        messageManager.isFirstRunForMessage = false
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
        messageManager.isFirstRunForAST = false
        Assert.assertFalse(messageManager.isFirstRunForAST)

        // test that the flags remain persisted when the application restarts
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(Exception::class)
    fun testDuplicateSessionEnds() {
        startMParticle()
        val session = InternalSession()
        session.start(context)
        messageManager.startSession(session)
        com.mparticle.internal.AccessUtils.awaitMessageHandler()
        var message: BaseMPMessage? =
            messageManager.mParticleDBManager.getSessionForSessionEndMessage(
                session.mSessionID,
                null,
                session.mpids
            )
        junit.framework.Assert.assertNotNull(message)
        messageManager.mParticleDBManager.updateSessionStatus(
            session.mSessionID,
            SessionTable.SessionTableColumns.STATUS
        )
        message = messageManager.mParticleDBManager.getSessionForSessionEndMessage(
            session.mSessionID,
            null,
            session.mpids
        )
        junit.framework.Assert.assertNull(message)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallReferrerFlagsEdgeCases() {
        val apiKey = "apiKey"
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
        setFirstRunLegacy(true, "key")

        // start SDK, and send only AST, but not First Run message
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        messageManager.isFirstRunForAST = true
        Assert.assertTrue(messageManager.isFirstRunForAST)
        messageManager.isFirstRunForAST = false
        Assert.assertFalse(messageManager.isFirstRunForAST)

        // restart SDK, and assert that AST message has been flagged, but First Run message has not
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        Assert.assertFalse(messageManager.isFirstRunForAST)
        setFirstRunLegacy(true, apiKey)

        // start SDK, and send only First Run message, but not AST
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        Assert.assertTrue(messageManager.isFirstRunForMessage)
        messageManager.isFirstRunForMessage = false
        Assert.assertFalse(messageManager.isFirstRunForMessage)

        // restart SDK, and assert that First Run message has been flagged as sent, but AST has not
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(options)
        Assert.assertFalse(messageManager.isFirstRunForMessage)
        Assert.assertTrue(messageManager.isFirstRunForAST)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDelayedStartInvoked() {
        val options = MParticleOptions.builder(context).credentials("key", "secret").build()
        MParticle.start(options)
        val messageManager = AccessUtils.messageManager
        Assert.assertFalse(messageManager.hasDelayedStartOccurred())
        val handler = com.mparticle.internal.AccessUtils.messageHandler
        handler.sendMessage(handler.obtainMessage())
        com.mparticle.internal.AccessUtils.awaitMessageHandler()
        Assert.assertTrue(messageManager.hasDelayedStartOccurred())
    }

    /**
     * simulates the install state to settings pre 5.0.9 || pre 4.17.1
     * @param firstRun
     */
    private fun setFirstRunLegacy(firstRun: Boolean, key: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
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
        private get() = AccessUtils.messageManager
}
