package com.mparticle.kits;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class KitUtilsTest {

    @Test
    public void testSanitizeAttributeKey() throws Exception {
        assertNull(KitUtils.sanitizeAttributeKey(null));
        assertEquals("", KitUtils.sanitizeAttributeKey(""));
        assertEquals("TestTest", KitUtils.sanitizeAttributeKey("$TestTest"));
        assertEquals("$", KitUtils.sanitizeAttributeKey("$$"));
        assertEquals("", KitUtils.sanitizeAttributeKey("$"));
    }

    @Test
    public void testJoin() throws Exception {
        List<String> testList = new LinkedList<>();
        testList.add("1");
        testList.add("test");
        testList.add("test 2");
        assertEquals("1,test,test 2", KitUtils.join(testList));
        assertEquals("", KitUtils.join(new LinkedList<String>()));
        List<String> singleElementList = new LinkedList<>();
        singleElementList.add("tester");
        assertEquals("tester", KitUtils.join(singleElementList));
    }

    @Test
    public void testJoinWithDelimiter() throws Exception {
        List<String> testList = new LinkedList<>();
        testList.add("1");
        testList.add("test");
        testList.add("test 2");
        assertEquals("", KitUtils.join(new LinkedList<String>(), "whatever"));
        List<String> singleElementList = new LinkedList<>();
        singleElementList.add("tester");
        assertEquals("tester", KitUtils.join(singleElementList, "whatever"));
        assertEquals("1whatevertestwhatevertest 2", KitUtils.join(testList, "whatever"));
    }
}