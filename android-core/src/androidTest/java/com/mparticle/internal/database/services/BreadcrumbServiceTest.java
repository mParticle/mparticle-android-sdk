package com.mparticle.internal.database.services;

import android.location.Location;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.messages.BaseMPMessage;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BreadcrumbServiceTest extends BaseMPServiceTest {
    private static BaseMPMessage message;
    private static int breadCrumbLimit;

    @Before
    public void before() throws Exception {
        message = new BaseMPMessage.Builder("test").build(new InternalSession(), new Location("New York City"), 1);
        breadCrumbLimit = ConfigManager.getBreadcrumbLimit(mContext);
    }

    /**
     * test throwing a bunch of trash, edge cases into the table and make sure that null values are
     * not stored in essential fields, and that it does not break the database
     * @throws Exception
     */
    @Test
    public void testNullValues() throws Exception {
        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "k", null);
        }
        assertEquals(BreadcrumbService.getBreadcrumbCount(database, null), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, null, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, null, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);
    }

    /**
     * test that the new DB schema of storing stuff dependent on MPID is working
     * @throws Exception
     */
    @Test
    public void testMpIdSpecific() throws Exception {

        /**
         * this test won't work if you can't store breadcrumbs
         */
        assertTrue(breadCrumbLimit >= 2);

        int expectedCount = breadCrumbLimit;
        for (int i = 0; i < expectedCount; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "apiKey", 1L);
        }
        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 2L).length(), 0);

        for (int i = 0; i < expectedCount -1; i++) {
            BreadcrumbService.insertBreadcrumb(database, mContext, message, "apiKey", 2L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 2L).length(), expectedCount - 1);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 3L).length(), 0);
    }

    @Test
    public void testBreadcrumbLimit() throws JSONException {
        List<Integer> deleted = new ArrayList<Integer>();

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            deleted.add(BreadcrumbService.insertBreadcrumb(database, mContext, message, "apiKey", 10L));
        }

        // make sure that 10 (number attempted to be inserted above the breadcrumb limit) entries have been deleted
        deleted.removeAll(Collections.singleton(new Integer(-1)));
        assertEquals(deleted.size(), 10);

        assertEquals(BreadcrumbService.getBreadcrumbs(database, mContext, 10L).length(), breadCrumbLimit);
    }
}