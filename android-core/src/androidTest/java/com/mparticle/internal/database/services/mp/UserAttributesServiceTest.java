package com.mparticle.internal.database.services.mp;

import com.mparticle.MParticle;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;

public class UserAttributesServiceTest extends BaseMPServiceTest {
    @Test
    public void testStoreByMpid() {
        for (int i = 0; i < 20; i++) {
            UserAttributesService.insertAttribute(database, String.valueOf(new Random().nextInt()), String.valueOf(new Random().nextInt()), System.currentTimeMillis(), false, 3L);
        }
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 20);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 4L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 4L).size(), 0);
        for (int i = 0; i < 30; i++) {
            UserAttributesService.insertAttribute(database, String.valueOf(new Random().nextInt()), String.valueOf(new Random().nextInt()), System.currentTimeMillis(), false, 4L);
        }
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 20);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 4L).size(), 30);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 4L).size(), 0);
        for (int i = 0; i < 15; i++) {
            UserAttributesService.insertAttribute(database, String.valueOf(new Random().nextInt()), String.valueOf(new Random().nextInt()), System.currentTimeMillis(), true, 3L);
        }
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 20);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 4L).size(), 30);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 4L).size(), 0);
        for (int i = 0; i < 30; i++) {
            UserAttributesService.insertAttribute(database, String.valueOf(new Random().nextInt()), String.valueOf(new Random().nextInt()), System.currentTimeMillis(), true, 5L);
        }
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 20);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 4L).size(), 30);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 4L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 5L).size(), 30);
    }

    @Test
    public void testDeleteByMpid() {
        testDeleteByMpid(true);
    }

    private void testDeleteByMpid(boolean repeat) {
        for (int i = 0; i < 15; i++) {
            UserAttributesService.insertAttribute(database, "key" + i, String.valueOf(new Random().nextInt()), System.currentTimeMillis(), false, 2L);
        }
        for (int i = 0; i < 15; i++) {
            UserAttributesService.insertAttribute(database, "key" + i, String.valueOf(new Random().nextInt()), System.currentTimeMillis(), false, 3L);
        }
        for (int i = 15; i < 30; i++) {
            UserAttributesService.insertAttribute(database, "key" + i, String.valueOf(new Random().nextInt()), System.currentTimeMillis(), true, 2L);
        }
        for (int i = 15; i < 30; i++) {
            UserAttributesService.insertAttribute(database, "key" + i, String.valueOf(new Random().nextInt()), System.currentTimeMillis(), true, 3L);
        }
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 2L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 2L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 15);

        UserAttributesService.deleteAttributes(database, "key1", 2L);
        UserAttributesService.deleteAttributes(database, "key16", 3L);

        assertEquals(UserAttributesService.getUserAttributesSingles(database, 2L).size(), 14);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 2L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 14);

        for (int i = 0; i < 30; i++) {
            UserAttributesService.deleteAttributes(database, "key" + i, 2L);
        }

        assertEquals(UserAttributesService.getUserAttributesSingles(database, 2L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 15);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 2L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 14);

        for (int i = 0; i < 30; i++) {
            UserAttributesService.deleteAttributes(database, "key" + i, 3L);
        }

        assertEquals(UserAttributesService.getUserAttributesSingles(database, 2L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesSingles(database, 3L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 2L).size(), 0);
        assertEquals(UserAttributesService.getUserAttributesLists(database, 3L).size(), 0);


        //easy way to test to make sure that insert is working properly after delete, just run the same test again
        if (repeat) {
            testDeleteByMpid(false);
        }
    }
}
