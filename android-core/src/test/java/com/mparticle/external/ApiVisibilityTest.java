package com.mparticle.external;

import com.mparticle.MParticle;
import com.mparticle.Session;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;

public class ApiVisibilityTest {

    @Test
    public void testMParticleApiVisibility() throws Exception {
        Method[] mpMethods = MParticle.class.getDeclaredMethods();
        int publicMethodCount = 0;
        for (Method m : mpMethods) {
            if (Modifier.isPublic(m.getModifiers())) {
                publicMethodCount++;
            }
        }
        assertEquals(62, publicMethodCount);
    }

    @Test
    public void testSessionApiVisibility() throws Exception {
        Method[] mpMethods = Session.class.getDeclaredMethods();
        int publicMethodCount = 0;
        for (Method m : mpMethods) {
            if (Modifier.isPublic(m.getModifiers())) {
                publicMethodCount++;
            }
        }
        assertEquals(4, publicMethodCount);
    }
}
