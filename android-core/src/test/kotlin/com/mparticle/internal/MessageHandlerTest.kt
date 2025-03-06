package com.mparticle.internal

import android.os.Message
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.messages.MPAliasMessage
import com.mparticle.mock.MockContext
import com.mparticle.networking.NetworkOptions
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class MessageHandlerTest {
    private lateinit var mConfigManager: ConfigManager
    private lateinit var handler: MessageHandler
    private lateinit var mMessageManager: MessageManager
    private lateinit var mParticleDatabaseManager: MParticleDBManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MParticle.setInstance(MockMParticle())
        val stateManager = Mockito.mock(
            AppStateManager::class.java
        )
        mConfigManager = MParticle.getInstance()?.Internal()?.configManager!!
        mMessageManager = Mockito.mock(MessageManager::class.java)
        Mockito.`when`(mMessageManager.apiKey).thenReturn("apiKey")
        Mockito.`when`(mMessageManager.uploadSettings).thenReturn(UploadSettings("apiKey", "secret", NetworkOptions.builder().build(), "", ""))
        mParticleDatabaseManager = Mockito.mock(MParticleDBManager::class.java)
        handler = object : MessageHandler(
            mMessageManager,
            MockContext(),
            mParticleDatabaseManager,
            "dataplan1",
            1
        ) {
            public override fun databaseAvailable(): Boolean {
                return true
            }
        }
    }

    @Test
    @Throws(JSONException::class)
    fun testInsertAliasRequest() {
        val insertedAliasRequest = AndroidUtils.Mutable<JSONObject?>(null)
        Mockito.`when`(mConfigManager.deviceApplicationStamp).thenReturn("das")
        val database: MParticleDBManager = object : MParticleDBManager(MockContext()) {
            override fun insertAliasRequest(request: JSONObject, uploadSettings: UploadSettings) {
                insertedAliasRequest.value = request
            }

            override fun getDatabase(): MPDatabase? {
                return null
            }
        }
        handler.mMParticleDBManager = database
        TestCase.assertNull(insertedAliasRequest.value)
        val aliasRequest = TestingUtils.getInstance().randomAliasRequest
        val aliasMessage = MPAliasMessage(aliasRequest, "das", "apiKey")
        val mockMessage = Mockito.mock(
            Message::class.java
        )
        mockMessage.what = MessageHandler.STORE_ALIAS_MESSAGE
        mockMessage.obj = aliasMessage
        handler.handleMessageImpl(mockMessage)
        Assert.assertNotNull(insertedAliasRequest.value)
        aliasMessage.remove(MessageKey.REQUEST_ID)
        insertedAliasRequest.value?.remove(MessageKey.REQUEST_ID)
        TestingUtils.assertJsonEqual(aliasMessage, insertedAliasRequest.value)
    }
}
