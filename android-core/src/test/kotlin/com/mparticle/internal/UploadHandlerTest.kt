package com.mparticle.internal

import android.content.Context
import android.database.Cursor
import android.os.Message
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.SdkListener
import com.mparticle.identity.AliasResponse
import com.mparticle.internal.MPUtility.AdIdInfo
import com.mparticle.internal.MParticleApiClient.AliasNetworkResponse
import com.mparticle.internal.MParticleApiClientImpl.MPRampException
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.database.services.MParticleDBManager.ReadyUpload
import com.mparticle.internal.messages.MPAliasMessage
import com.mparticle.mock.MockContext
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.RandomUtils
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(PowerMockRunner::class)
class UploadHandlerTest {
    private lateinit var handler: UploadHandler
    private lateinit var mConfigManager: ConfigManager
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MParticle.setInstance(MockMParticle())
        val stateManager = Mockito.mock(
            AppStateManager::class.java
        )
        mConfigManager = MParticle.getInstance()?.Internal()?.configManager!!
        handler = UploadHandler(
            MockContext(), mConfigManager, stateManager, Mockito.mock(
                MessageManager::class.java
            ), Mockito.mock(
                MParticleDBManager::class.java
            ), Mockito.mock(
                KitFrameworkWrapper::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetConnected() {
        handler.isNetworkConnected = true
        val latch = CountDownLatch(1)
        Thread {
            Assert.assertTrue(handler.isNetworkConnected)
            handler.setConnected(false)
            latch.countDown()
        }.start()
        latch.await(1000, TimeUnit.MILLISECONDS)
        Assert.assertFalse(handler.isNetworkConnected)
        handler.isNetworkConnected = true
        Thread { Assert.assertTrue(handler.isNetworkConnected) }.start()
    }

    @Test
    @Throws(Exception::class)
    fun testHandleMessage() {
        val message = Mockito.mock(Message::class.java)
        for (i in 0..29) {
            message.what = i
            handler.handleMessage(message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testShouldDelete() {
        for (i in 0..201) {
            if (i != 200) {
                Assert.assertFalse(handler.shouldDelete(i))
            }
        }
        Assert.assertTrue(handler.shouldDelete(200))
        Assert.assertTrue(handler.shouldDelete(202))
        for (i in 203..399) {
            Assert.assertFalse(handler.shouldDelete(i))
        }
        for (i in 400..428) {
            Assert.assertTrue(handler.shouldDelete(i))
        }
        Assert.assertFalse(handler.shouldDelete(429))
        for (i in 430..499) {
            Assert.assertTrue(handler.shouldDelete(i))
        }
        for (i in 500..599) {
            Assert.assertFalse(handler.shouldDelete(i))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testRampSampling() {
        handler.handleMessage(Message())
        val apiClient = Mockito.mock(
            MParticleApiClientImpl::class.java
        )
        val rampException = MPRampException()
        Mockito.`when`(apiClient.sendMessageBatch(Mockito.anyString())).thenThrow(rampException)
        Mockito.`when`(handler.mParticleDBManager.deleteUpload(Mockito.anyInt())).thenReturn(1)
        handler.setApiClient(apiClient)
        handler.uploadMessage(522, "")
    }

    @Test
    @Throws(Exception::class)
    fun testGetDeviceInfo() {
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(MockContext())
        Assert.assertNotNull(attributes)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAppInfo() {
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(MockContext())
        Assert.assertNotNull(attributes)
    }

    @Test
    @PrepareForTest(MPUtility::class)
    @Throws(Exception::class)
    fun testGetAAIDAllDefaults() {
        val AAID = UUID.randomUUID().toString()
        Mockito.`when`(mConfigManager.restrictAAIDBasedOnLAT).thenReturn(true)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(
            MPUtility.getAdIdInfo(
                Mockito.any(
                    Context::class.java
                )
            )
        ).thenReturn(AdIdInfo(AAID, false, AdIdInfo.Advertiser.AMAZON))
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(MockContext())
        Assert.assertFalse(attributes.getBoolean("lat"))
        Assert.assertEquals(AAID, attributes.getString("faid"))
    }

    @Test
    @PrepareForTest(MPUtility::class)
    @Throws(Exception::class)
    fun testGetAAIDLATTrueRestrictTrue() {
        val AAID = UUID.randomUUID().toString()
        Mockito.`when`(mConfigManager.restrictAAIDBasedOnLAT).thenReturn(true)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(
            MPUtility.getAdIdInfo(
                Mockito.any(
                    Context::class.java
                )
            )
        ).thenReturn(AdIdInfo(AAID, true, AdIdInfo.Advertiser.GOOGLE))
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(MockContext())
        Assert.assertTrue(attributes.getBoolean("lat"))
        Assert.assertFalse(attributes.has("gaid"))
    }

    @Test
    @PrepareForTest(MPUtility::class)
    @Throws(Exception::class)
    fun testGetAAIDLATTrueRestrictFalse() {
        val AAID = UUID.randomUUID().toString()
        Mockito.`when`(mConfigManager.restrictAAIDBasedOnLAT).thenReturn(false)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(
            MPUtility.getAdIdInfo(
                Mockito.any(
                    Context::class.java
                )
            )
        ).thenReturn(AdIdInfo(AAID, true, AdIdInfo.Advertiser.AMAZON))
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(MockContext())
        Assert.assertTrue(attributes.getBoolean("lat"))
        Assert.assertEquals(AAID, attributes.getString("faid"))
    }

    @Test
    @PrepareForTest(MPUtility::class)
    @Throws(Exception::class)
    fun testGetAAIDLATFalseRestrictLatFalse() {
        val AAID = UUID.randomUUID().toString()
        Mockito.`when`(mConfigManager.restrictAAIDBasedOnLAT).thenReturn(false)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(
            MPUtility.getAdIdInfo(
                Mockito.any(
                    Context::class.java
                )
            )
        ).thenReturn(AdIdInfo(AAID, false, AdIdInfo.Advertiser.GOOGLE))
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(MockContext())
        Assert.assertFalse(attributes.getBoolean("lat"))
        Assert.assertEquals(AAID, attributes.getString("gaid"))
    }

    @Test
    @Throws(Exception::class)
    fun testDontUploadSessionHistory() {
        handler.handleMessage(Message())
        Mockito.`when`(mConfigManager.includeSessionHistory).thenReturn(false)
        val mockCursor = Mockito.mock(
            Cursor::class.java
        )
        Mockito.`when`(mockCursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(mockCursor.getInt(Mockito.anyInt())).thenReturn(123)
        Mockito.`when`(mockCursor.getString(Mockito.anyInt())).thenReturn("cool message batch!")
        handler.upload(true)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadSessionHistory() {
        handler.handleMessage(Message())
        val mockCursor = Mockito.mock(
            Cursor::class.java
        )
        Mockito.`when`(handler.mParticleDBManager.readyUploads)
            .thenReturn(object : ArrayList<ReadyUpload?>() {
                init {
                    add(ReadyUpload(123, false, "a message batch"))
                }
            })
        val mockApiClient = Mockito.mock(
            MParticleApiClient::class.java
        )
        handler.setApiClient(mockApiClient)
        Mockito.`when`(mConfigManager.includeSessionHistory).thenReturn(true)
        Mockito.`when`(mockCursor.moveToNext()).thenReturn(true, false)
        handler.upload(true)
        Mockito.verify(mockApiClient).sendMessageBatch(Mockito.eq("a message batch"))
    }

    @Test
    @Throws(
        IOException::class,
        MPThrottleException::class,
        JSONException::class,
        MPRampException::class
    )
    fun testRetryLogic() {
        val deleteId = AndroidUtils.Mutable<Int?>(null)
        val database: MParticleDBManager = object : MParticleDBManager(MockContext()) {
            override fun deleteUpload(id: Int): Int {
                deleteId.value = id
                return id
            }
        }
        val uploadHandler: UploadHandler = object : UploadHandler(
            MockContext(),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            Mockito.mock(MessageManager::class.java),
            database, Mockito.mock(KitFrameworkWrapper::class.java)
        ) {
            override fun shouldDelete(statusCode: Int): Boolean {
                return false
            }
        }
        val mockApiClient = Mockito.mock(
            MParticleApiClient::class.java
        )
        Mockito.`when`(
            mockApiClient.sendAliasRequest(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(AliasNetworkResponse(0))
        uploadHandler.setApiClient(mockApiClient)
        TestCase.assertNull(deleteId.value)
        val aliasRequest = TestingUtils.getInstance().randomAliasRequest
        val request: JSONObject = MPAliasMessage(aliasRequest, "das", "apiKey")
        uploadHandler.uploadAliasRequest(1, request.toString())
        TestCase.assertNull(deleteId.value)
    }

    @Test
    @Throws(
        IOException::class,
        MPThrottleException::class,
        JSONException::class,
        MPRampException::class
    )
    fun testDeleteLogic() {
        val deletedUpload = AndroidUtils.Mutable<Int?>(null)
        val database: MParticleDBManager = object : MParticleDBManager(MockContext()) {
            override fun deleteUpload(id: Int): Int {
                deletedUpload.value = id
                return id
            }
        }
        val uploadHandler: UploadHandler = object : UploadHandler(
            MockContext(),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            Mockito.mock(MessageManager::class.java),
            database, Mockito.mock(KitFrameworkWrapper::class.java)
        ) {
            override fun shouldDelete(statusCode: Int): Boolean {
                return true
            }
        }
        val mockApiClient = Mockito.mock(
            MParticleApiClient::class.java
        )
        Mockito.`when`(
            mockApiClient.sendAliasRequest(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(AliasNetworkResponse(0))
        uploadHandler.setApiClient(mockApiClient)
        TestCase.assertNull(deletedUpload.value)
        val aliasRequest = TestingUtils.getInstance().randomAliasRequest
        uploadHandler.uploadAliasRequest(
            1,
            MPAliasMessage(aliasRequest, "das", "apiKey").toString()
        )
        Assert.assertNotNull(deletedUpload.value)
    }

    @PrepareForTest(MPUtility::class)
    @Throws(
        MPRampException::class,
        MPThrottleException::class,
        JSONException::class,
        IOException::class
    )
    fun testAliasCallback() {
        val ran = RandomUtils()
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(
            MPUtility.isAppDebuggable(
                Mockito.any(
                    Context::class.java
                )
            )
        ).thenReturn(true)
        val capturedResponse = AndroidUtils.Mutable<AliasResponse?>(null)
        val sdkListener: SdkListener = object : SdkListener() {
            override fun onAliasRequestFinished(aliasResponse: AliasResponse) {
                capturedResponse.value = aliasResponse
            }
        }
        MParticle.addListener(MockContext(), sdkListener)
        val mockApiClient = Mockito.mock(
            MParticleApiClient::class.java
        )
        handler.setApiClient(mockApiClient)

        //test successful request
        Mockito.`when`(
            mockApiClient.sendAliasRequest(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(AliasNetworkResponse(202))
        TestCase.assertNull(capturedResponse.value)
        var aliasRequest = TestingUtils.getInstance().randomAliasRequest
        var aliasRequestMessage = MPAliasMessage(aliasRequest, "das", "apiKey")
        handler.uploadAliasRequest(1, aliasRequestMessage.toString())
        capturedResponse.value?.isSuccessful?.let { Assert.assertTrue(it) }
        TestCase.assertNull(capturedResponse.value?.errorResponse)
        capturedResponse.value?.willRetry()?.let { Assert.assertFalse(it) }
        Assert.assertEquals(aliasRequest, capturedResponse.value?.request)
        Assert.assertEquals(202, capturedResponse.value?.responseCode)
        Assert.assertEquals(aliasRequestMessage.requestId, capturedResponse.value?.requestId)
        capturedResponse.value = null


        //test retry request
        Mockito.`when`(
            mockApiClient.sendAliasRequest(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(AliasNetworkResponse(429))
        TestCase.assertNull(capturedResponse.value)
        aliasRequest = TestingUtils.getInstance().randomAliasRequest
        aliasRequestMessage = MPAliasMessage(aliasRequest, "das", "apiKey")
        handler.uploadAliasRequest(2, aliasRequestMessage.toString())
        capturedResponse.value?.isSuccessful?.let { Assert.assertFalse(it) }
        TestCase.assertNull(capturedResponse.value?.errorResponse)
        capturedResponse.value?.willRetry()?.let { Assert.assertTrue(it) }
        Assert.assertEquals(aliasRequest, capturedResponse.value?.request)
        Assert.assertEquals(429, capturedResponse.value?.responseCode)
        Assert.assertEquals(aliasRequestMessage.requestId, capturedResponse.value?.requestId)
        capturedResponse.value = null

        //test error message present
        val error = ran.getAlphaNumericString(20)
        Mockito.`when`(
            mockApiClient.sendAliasRequest(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(AliasNetworkResponse(400, error))
        TestCase.assertNull(capturedResponse.value)
        aliasRequest = TestingUtils.getInstance().randomAliasRequest
        aliasRequestMessage = MPAliasMessage(aliasRequest, "das", "apiKey")
        handler.uploadAliasRequest(3, aliasRequestMessage.toString())
        capturedResponse.value?.isSuccessful?.let { Assert.assertFalse(it) }
        Assert.assertEquals(capturedResponse.value?.errorResponse, error)
        capturedResponse.value?.willRetry()?.let { Assert.assertFalse(it) }
        Assert.assertEquals(aliasRequest, capturedResponse.value?.request)
        Assert.assertEquals(aliasRequestMessage.requestId, capturedResponse.value?.requestId)
    }

    //we are uploading 100 messages at a time. Make sure when we have > 100 messages ready for upload, we perform
    //multiple uploads until they are done, then go to the next upload loop
    @Test
    @Throws(
        IOException::class,
        MPThrottleException::class,
        JSONException::class,
        MPRampException::class
    )
    fun testFullUploadLogic() {
        val database = MockMParticleDBManager()
        val mockConfigManager = Mockito.mock(
            ConfigManager::class.java
        )
        Mockito.`when`(mockConfigManager.uploadInterval).thenReturn(100)
        val mockAppStateManager = Mockito.mock(
            AppStateManager::class.java
        )
        val session: InternalSession = object : InternalSession() {
            override fun isActive(): Boolean {
                return true
            }
        }
        Mockito.`when`(mockAppStateManager.session).thenReturn(session)
        val uploadHandler = MockUploadHandler(database, mockConfigManager, mockAppStateManager)
        val mockApiClient = Mockito.mock(
            MParticleApiClient::class.java
        )
        Mockito.`when`(
            mockApiClient.sendMessageBatch(
                Mockito.any(
                    String::class.java
                )
            )
        ).thenReturn(200)
        uploadHandler.setApiClient(mockApiClient)


        //send a regular "upload loop" message
        val message = Mockito.mock(Message::class.java)
        message.what = UploadHandler.UPLOAD_MESSAGES
        message.arg1 = 0
        Mockito.`when`(message.toString()).thenReturn("asdvasd")
        database.hasMessagesTrueCount = 4
        uploadHandler.handleMessageImpl(message)

        //make sure `MParticleDatabaseManager#prepareMessageUploads()` was called 4 times (based on database.hasMessagesTrueCount)
        Assert.assertEquals(4, uploadHandler.prepareMessageUploadsCalledCount)
        //make sure `MParticleDatabaseManager#upload()` was still only called once. This will take care of all the prepared uploads
        Assert.assertEquals(1, uploadHandler.uploadCalledCount)

        //make sure we send the delayed message for the next "upload loop"
        Assert.assertNotSame(message, uploadHandler.message)
        Assert.assertEquals(100L, uploadHandler.messageDelay)
    }

    internal inner class MockMParticleDBManager : MParticleDBManager(MockContext()) {
        var hasMessagesTrueCount = 0
        override fun hasMessagesForUpload(): Boolean {
            return hasMessagesTrueCount-- > 0
        }

        override fun getDatabase(): MPDatabase? {
            return null
        }
    }

    internal inner class MockUploadHandler(
        database: MParticleDBManager?,
        configManager: ConfigManager?,
        appStateManager: AppStateManager?
    ) : UploadHandler(
        MockContext(),
        configManager,
        appStateManager,
        Mockito.mock(MessageManager::class.java),
        database,
        Mockito.mock(KitFrameworkWrapper::class.java)
    ) {
        var message: Message? = null
        var messageDelay: Long? = null
        var uploadCalledCount = 0
        var prepareMessageUploadsCalledCount = 0
        override fun upload(history: Boolean): Boolean {
            uploadCalledCount++
            return false
        }

        @Throws(Exception::class)
        override fun prepareMessageUploads(history: Boolean) {
            prepareMessageUploadsCalledCount++
        }

        override fun sendEmpty(what: Int) {
            message?.what = what
            messageDelay = null
        }

        public override fun sendEmptyDelayed(what: Int, uptimeMillis: Long) {
            message?.what = what
            messageDelay = uptimeMillis
        }
    }
}