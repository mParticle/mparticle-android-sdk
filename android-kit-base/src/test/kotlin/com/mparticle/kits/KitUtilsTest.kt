package com.mparticle.kits

import org.junit.Assert
import org.junit.Test
import java.util.LinkedList

class KitUtilsTest {
    @Test
    @Throws(Exception::class)
    fun testSanitizeAttributeKey() {
        Assert.assertNull(KitUtils.sanitizeAttributeKey(null))
        Assert.assertEquals("", KitUtils.sanitizeAttributeKey(""))
        Assert.assertEquals("TestTest", KitUtils.sanitizeAttributeKey("\$TestTest"))
        Assert.assertEquals("$", KitUtils.sanitizeAttributeKey("$$"))
        Assert.assertEquals("", KitUtils.sanitizeAttributeKey("$"))
    }

    @Test
    @Throws(Exception::class)
    fun testJoin() {
        val testList: MutableList<String> = LinkedList()
        testList.add("1")
        testList.add("test")
        testList.add("test 2")
        Assert.assertEquals("1,test,test 2", KitUtils.join(testList))
        Assert.assertEquals("", KitUtils.join(LinkedList()))
        val singleElementList: MutableList<String> = LinkedList()
        singleElementList.add("tester")
        Assert.assertEquals("tester", KitUtils.join(singleElementList))
    }

    @Test
    @Throws(Exception::class)
    fun testJoinWithDelimiter() {
        val testList: MutableList<String> = LinkedList()
        testList.add("1")
        testList.add("test")
        testList.add("test 2")
        Assert.assertEquals("", KitUtils.join(LinkedList(), "whatever"))
        val singleElementList: MutableList<String> = LinkedList()
        singleElementList.add("tester")
        Assert.assertEquals("tester", KitUtils.join(singleElementList, "whatever"))
        Assert.assertEquals("1whatevertestwhatevertest 2", KitUtils.join(testList, "whatever"))
    }
}
