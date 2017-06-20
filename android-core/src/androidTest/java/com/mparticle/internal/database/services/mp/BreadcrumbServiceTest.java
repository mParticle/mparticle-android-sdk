package com.mparticle.internal.database.services.mp;

import android.location.Location;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.Session;
import com.mparticle.internal.database.tables.mp.BreadcrumbTable;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class BreadcrumbServiceTest extends BaseMPServiceTest {
    private static MPMessage message;
    private static int breadCrumbLimit;

    @BeforeClass
    public static void setUp() throws JSONException {
        message = new MPMessage.Builder("test", new Session(), new Location("New York City"), 1).build();
        breadCrumbLimit = ConfigManager.getBreadcrumbLimit();
    }

    /**
     * test throwing a bunch of trash, edge cases into the table and make sure that null values are
     * not stored in essential fields, and that it does not break the database
     * @throws Exception
     */
    @Test
    public void testNullValues() throws Exception {
        clearDatabase();

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, "k", null);
        }
        assertEquals(BreadcrumbService.getBreadcrumbCount(database, null), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, null, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 0);

        for (int i = 0; i < 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, "k", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, null, 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), 10);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, null, "k", 10L);
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
        clearDatabase();

        int expectedCount = breadCrumbLimit;
        for (int i = 0; i < expectedCount; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, "apiKey", 1L);
        }
        assertEquals(BreadcrumbService.getBreadcrumbs(database, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, 2L).length(), 0);

        for (int i = 0; i < expectedCount -1; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, "apiKey", 2L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbs(database, 1L).length(), expectedCount);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, 2L).length(), expectedCount - 1);
        assertEquals(BreadcrumbService.getBreadcrumbs(database, 3L).length(), 0);
    }

    /**
     * test that the logic to migrate existing database data to the new MPID depended schema is working
     * @throws Exception
     */

    @Test
    public void testMigrationToMpIdSpecific() throws Exception {
        //TODO
        //still need to implement the logic in the actual code
    }

    /**
     * there will be some cases, when we get data coming in that is MPID dependent, but we have not
     * received a return on the Identify() call yet, so we will need to be able to store data in a sort
     * of "real MPID pending" state, then switch it all to the correct MPID when we get it
     * @throws Exception
     */
    @Test
    public void testUpdateMpIdOnIdentification() throws Exception {
        //TODO
        //still need to actually hash out this logic and use case, and implement in the actual code
    }

    @Test
    public void testBreadcrumbLimit() throws JSONException {
        int breadCrumbLimit = ConfigManager.getBreadcrumbLimit();
        List<Integer> deleted = new ArrayList<Integer>();

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            deleted.add(BreadcrumbService.insertBreadcrumb(database, message, "apiKey", 10L));
        }

        // make sure that 10 (number attempted to be inserted above the breadcrumb limit) entries have been deleted
        deleted.removeAll(Collections.singleton(new Integer(-1)));
        assertEquals(deleted.size(), 10);

        assertEquals(BreadcrumbService.getBreadcrumbs(database, 10L).length(), breadCrumbLimit);

        for (int i = 0; i < breadCrumbLimit + 10; i++) {
            BreadcrumbService.insertBreadcrumb(database, message, "apiKey", 20L);
            BreadcrumbService.insertBreadcrumb(database, message, "apiKey", 10L);
        }

        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 10L), breadCrumbLimit);
        assertEquals(BreadcrumbService.getBreadcrumbCount(database, 20L), breadCrumbLimit);
    }
}
