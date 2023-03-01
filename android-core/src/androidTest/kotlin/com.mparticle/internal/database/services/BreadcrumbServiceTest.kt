package com.mparticle.internal.database.services

import android.location.Location
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.InternalSession
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BreadcrumbServiceTest : BaseMPServiceTest() {
    @Before
    @Throws(Exception::class)
    fun before() {
        message =
            BaseMPMessage.Builder("test").build(InternalSession(), Location("New York City"), 1)
        breadCrumbLimit = ConfigManager.getBreadcrumbLimit(mContext)
    }

    /**
     * test throwing a bunch of trash, edge cases into the table and make sure that null values are
     * not stored in essential fields, and that it does not break the database
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testNullValues() {
        for (i in 0 until breadCrumbLimit + 10) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "k", null)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, null).toLong(), 0)
        for (i in 0 until breadCrumbLimit + 10) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, null, 10L)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L).toLong(), 0)
        for (i in 0 until breadCrumbLimit + 10) {
            BreadcrumbService.insertBreadcrumb(database, mContext, null, "k", 10L)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L).toLong(), 0)
        for (i in 0..9) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "k", 10L)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L).toLong(), 10)
        for (i in 0 until breadCrumbLimit + 10) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, null, 10L)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L).toLong(), 10)
        for (i in 0 until breadCrumbLimit + 10) {
            BreadcrumbService.insertBreadcrumb(database, mContext, null, "k", 10L)
        }
        Assert.assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L).toLong(), 10)
    }

    /**
     * test that the new DB schema of storing stuff dependent on MPID is working
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMpIdSpecific() {
        /**
         * this test won't work if you can't store breadcrumbs
         */
        Assert.assertTrue(breadCrumbLimit >= 2)
        val expectedCount = breadCrumbLimit
        for (i in 0 until expectedCount) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "apiKey", 1L)
        }
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 1L).length().toLong(),
            expectedCount.toLong()
        )
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 2L).length().toLong(),
            0
        )
        for (i in 0 until expectedCount - 1) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "apiKey", 2L)
        }
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 1L).length().toLong(),
            expectedCount.toLong()
        )
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 2L).length().toLong(),
            (expectedCount - 1).toLong()
        )
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 3L).length().toLong(),
            0
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testBreadcrumbLimit() {
        val deleted: MutableList<Int> = ArrayList()
        for (i in 0 until breadCrumbLimit + 10) {
            deleted.add(
                BreadcrumbService.insertBreadcrumb(
                    database,
                    mContext,
                    message,
                    "apiKey",
                    10L
                )
            )
        }

        // make sure that 10 (number attempted to be inserted above the breadcrumb limit) entries have been deleted
        deleted.removeAll(setOf(-1))
        Assert.assertEquals(deleted.size, 10)
        Assert.assertEquals(
            BreadcrumbService.getBreadcrumbs(database, mContext, 10L).length(),
            breadCrumbLimit
        )
    }

    companion object {
        private var message: BaseMPMessage? = null
        private var breadCrumbLimit = 0
    }
}
