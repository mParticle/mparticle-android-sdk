package com.mparticle.external

import com.mparticle.MParticle
import com.mparticle.Session
import org.junit.Assert
import org.junit.Test
import java.lang.reflect.Modifier

class ApiVisibilityTest {
    @Test
    @Throws(Exception::class)
    fun testMParticleApiVisibility() {
        val mpMethods = MParticle::class.java.declaredMethods
        var publicMethodCount = 0
        for (m in mpMethods) {
            if (Modifier.isPublic(m.modifiers)) {
                publicMethodCount++
            }
        }
        Assert.assertEquals(66, publicMethodCount)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionApiVisibility() {
        val mpMethods = Session::class.java.declaredMethods
        var publicMethodCount = 0
        for (m in mpMethods) {
            if (Modifier.isPublic(m.modifiers)) {
                publicMethodCount++
            }
        }
        Assert.assertEquals(4, publicMethodCount)
    }
}
