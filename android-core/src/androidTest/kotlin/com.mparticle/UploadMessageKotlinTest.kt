package com.mparticle

import com.mparticle.testutils.BaseCleanStartedEachTest

class UploadMessageKotlinTest : BaseCleanStartedEachTest() {
    val uploadInterval = 100

    override fun transformMParticleOptions(builder: MParticleOptions.Builder): MParticleOptions.Builder {
        // set a really long upload interval so the results don't get confused by multiple loops
        return builder.uploadInterval(uploadInterval)
    }

//    @Test
//    fun testUploadGreaterThan100() {
//        try {
//            // track these, since the way we have our HandlerThreads are setup, we get messages carried over from the last test
//            val preexistingUploadQueueMessages = AccessUtils.getUploadHandlerMessageQueue()
//            val numMessages = 10
//
//            Constants.setMaxMessagePerBatch(2)
//
//            (0..numMessages).forEach { MParticle.getInstance()?.logEvent(MPEvent.Builder("$it").build()) }
//
//            val messages = TreeSet<Int>()
//            MParticle.getInstance()?.upload()
//            var uploadsCount = 0
//            mServer.waitForVerify(
//                Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
//                    it.optJSONArray("msgs")?.let { messagesArray ->
//                        for (i in 0 until messagesArray.length()) {
//                            messagesArray.getJSONObject(i).optString("n").toIntOrNull()?.let { messageId -> messages.add(messageId) }
//                        }
//                        assertTrue(messagesArray.length() <= Constants.getMaxMessagePerBatch())
//                    }
//                    uploadsCount++
//                    messages.size >= numMessages
//                }
//            )
//            assertTrue(uploadsCount > 4)
//
//            // Check the UploadHandler Message queue,
//            val uploadQueueMessages = AccessUtils.getUploadHandlerMessageQueue()
//                .filter { it.what == UploadHandler.UPLOAD_MESSAGES }
//
//            // make sure there is just 1 upload message in the queue (for the upload loop)
//            assertEquals(1, uploadQueueMessages.size, "current: " + uploadQueueMessages.joinToString() + "\npre:" + preexistingUploadQueueMessages.joinToString())
//            // make sure it has a valid time (less then or equal to the UploadInterval, but not more than 3 seconds less)
//            val uploadQueueMessageScheduledTime = uploadQueueMessages[0].`when`
//            val uploadIntervalMillis = uploadInterval * 1000
//            // make sure this is the actual upload message
//            assertTrue(uploadQueueMessageScheduledTime < (SystemClock.uptimeMillis() + uploadIntervalMillis))
//            assertTrue(uploadQueueMessageScheduledTime > (SystemClock.uptimeMillis() + (uploadInterval - 100)))
//        } finally {
//            Constants.setMaxMessagePerBatch(100)
//        }
//    }
}
