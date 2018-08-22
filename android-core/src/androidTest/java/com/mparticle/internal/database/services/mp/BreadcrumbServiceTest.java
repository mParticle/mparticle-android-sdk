package com.mparticle.internal.database.services.mp;

import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.internal.InternalSession;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class BreadcrumbServiceTest extends BaseMPServiceTest {
    private static BaseMPMessage message;
    private static int breadCrumbLimit;
    private static Context context;

    @Before
    public void before() throws Exception {
        message = new BaseMPMessage.Builder("test", new InternalSession(), new Location("New York City"), 1).build();
        context = InstrumentationRegistry.getContext();
        breadCrumbLimit = ConfigManager.getBreadcrumbLimit(context);
    }

    /**
     * test throwing a bunch of trash, edge cases into the table and make sure that null values are
     * not stored in essential fields, and that it does not break the database
     * @throws Exception
     */
    @Test
    public void testNullValues() throws Exception {
        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, message, "k", null);
        }
        assertEquals(BreadcrumbService.getBreadcrumbCount(database, null), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, null, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, message, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, null, "k", 10L);
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
            BreadcrumbService.insertBreadcrumb(database, context, message, "apiKey", 1L);
        }
        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 2L).length(), 0);

        for (int i = 0; i < expectedCount -1; i++) {
            BreadcrumbService.insertBreadcrumb(database, context, message, "apiKey", 2L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 2L).length(), expectedCount - 1);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 3L).length(), 0);
    }

    @Test
    public void testBreadcrumbLimit() throws JSONException {
        List<Integer> deleted = new ArrayList<Integer>();

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            deleted.add(BreadcrumbService.insertBreadcrumb(database, context, message, "apiKey", 10L));
        }

        // make sure that 10 (number attempted to be inserted above the breadcrumb limit) entries have been deleted
        deleted.removeAll(Collections.singleton(new Integer(-1)));
        assertEquals(deleted.size(), 10);

        assertEquals(BreadcrumbService.getBreadcrumbs(database, context, 10L).length(), breadCrumbLimit);
    }
}