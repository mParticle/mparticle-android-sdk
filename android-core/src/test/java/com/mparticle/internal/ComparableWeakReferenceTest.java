package com.mparticle.internal;

import com.mparticle.internal.ComparableWeakReference;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComparableWeakReferenceTest {

    @Test
    public void testComparableWeakRef() throws Exception {
        Object obj = new Object();

        WeakReference regularRef1 = new WeakReference(obj);
        WeakReference regularRef2 = new WeakReference(obj);

        assertFalse(regularRef1.equals(regularRef2));

        ComparableWeakReference comparableRef1 = new ComparableWeakReference(obj);
        ComparableWeakReference comparableRef2 = new ComparableWeakReference(obj);

        assertTrue(comparableRef1.equals(comparableRef2));
    }

    @Test
    public void testComparableWeakRefSet() throws Exception {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        HashSet<ComparableWeakReference<Object>> set = new HashSet<ComparableWeakReference<Object>>();
        set.add(new ComparableWeakReference<Object>(obj1));
        set.add(new ComparableWeakReference<Object>(obj2));
        set.add(new ComparableWeakReference<Object>(obj3));

        assertTrue(set.size() == 3);
        set.remove(new ComparableWeakReference<Object>(obj1));
        assertTrue(set.size() == 2);
        set.remove(new ComparableWeakReference<Object>(obj2));
        assertTrue(set.size() == 1);
        set.remove(new ComparableWeakReference<Object>(obj3));
        assertTrue(set.size() == 0);
    }
}
