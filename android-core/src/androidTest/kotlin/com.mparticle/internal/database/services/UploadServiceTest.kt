package com.mparticle.internal.database.services

import com.mparticle.internal.Constants
import com.mparticle.internal.database.UploadSettings
import com.mparticle.networking.NetworkOptions
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class UploadServiceTest : BaseMPServiceTest() {

    @Test
    @Throws(JSONException::class)
    fun testDeleteUploadsOlderThan() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60L * 60L * 1000L
        val uploadSettings = UploadSettings(
            "apiKey",
            "secret",
            NetworkOptions.builder().build(),
            "",
            "",
        )

        // Insert 5 uploads dated 10 days ago and 5 uploads dated 1 hour ago.
        // insertUpload reads CREATED_AT from the message's TIMESTAMP ("ct") key.
        for (i in 0 until 5) {
            UploadService.insertUpload(database, uploadJson(now - 10L * oneDayMillis), uploadSettings)
        }
        for (i in 0 until 5) {
            UploadService.insertUpload(database, uploadJson(now - 60L * 60L * 1000L), uploadSettings)
        }
        assertEquals(10, UploadService.getReadyUploads(database).size)

        // Cut off at 7 days ago - the 5 old uploads should be removed and the 5 recent kept.
        val cutoffMillis = now - 7L * oneDayMillis
        val deleted = UploadService.deleteUploadsOlderThan(database, cutoffMillis)
        assertEquals(5, deleted)
        assertEquals(5, UploadService.getReadyUploads(database).size)

        // Rows whose CREATED_AT is exactly at the cutoff must not be removed (strict `<` predicate).
        UploadService.insertUpload(database, uploadJson(cutoffMillis), uploadSettings)
        assertEquals(0, UploadService.deleteUploadsOlderThan(database, cutoffMillis))
        assertEquals(6, UploadService.getReadyUploads(database).size)
    }

    @Throws(JSONException::class)
    private fun uploadJson(timestampMillis: Long): JSONObject = JSONObject()
        .put(Constants.MessageKey.TIMESTAMP, timestampMillis)
        .put("payload", "test")
}
