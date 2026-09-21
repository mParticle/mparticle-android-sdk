package com.mparticle.lints.dtos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowedTypesTest {
    // kotlin.collections.MapsKt is a multifile-class facade with no methods of its own - mapOf()
    // is actually declared on the part class kotlin.collections.MapsKt__MapsKt, which is what a
    // compiled-code reference (and therefore StaticFactory's reflection) actually resolves to.
    // declaredMethods (what StaticFactory reflects with) doesn't see inherited members, so the
    // bare facade name alone would never match a real mapOf call.
    @Test
    fun `mapOf lives on the part class, not the bare facade`() {
        val facade = Class.forName("kotlin.collections.MapsKt")
        assertTrue(facade.declaredMethods.none { it.name == "mapOf" })

        val partClass = Class.forName("kotlin.collections.MapsKt__MapsKt")
        assertTrue(partClass.declaredMethods.any { it.name == "mapOf" })
    }

    @Test
    fun `part class name is recognized once the multifile suffix is stripped`() {
        assertTrue(AllowedTypes.isAllowedStaticFactory("kotlin.collections.MapsKt__MapsKt", "mapOf"))
        assertTrue(AllowedTypes.isAllowedStaticFactory("kotlin.collections.MapsKt__MapsJVMKt", "mapOf"))
        assertTrue(AllowedTypes.isAllowedStaticFactory("kotlin.collections.CollectionsKt__CollectionsKt", "listOf"))
    }

    @Test
    fun `bare facade name is also recognized`() {
        assertTrue(AllowedTypes.isAllowedStaticFactory("kotlin.collections.MapsKt", "mapOf"))
    }

    @Test
    fun `only the specific allowed method names are accepted, not every method on the class`() {
        assertFalse(AllowedTypes.isAllowedStaticFactory("kotlin.collections.MapsKt__MapsKt", "getValue"))
    }

    @Test
    fun `unrelated classes are rejected regardless of method name`() {
        assertFalse(AllowedTypes.isAllowedStaticFactory("java.lang.Runtime", "getRuntime"))
        assertFalse(AllowedTypes.isAllowedStaticFactory(null, "mapOf"))
        assertFalse(AllowedTypes.isAllowedStaticFactory("kotlin.collections.MapsKt", null))
    }

    @Test
    fun `instance gate accepts primitives, strings and collections by type`() {
        assertTrue(AllowedTypes.isInstanceAllowed("a string"))
        assertTrue(AllowedTypes.isInstanceAllowed(1))
        assertTrue(AllowedTypes.isInstanceAllowed(1.0))
        assertTrue(AllowedTypes.isInstanceAllowed(true))
        assertTrue(AllowedTypes.isInstanceAllowed(mapOf("k" to "v")))
        assertTrue(AllowedTypes.isInstanceAllowed(listOf("v")))
    }

    @Test
    fun `instance gate rejects a java-lang-Class instance`() {
        // This is the shape of the getClass()-escape: an allowed instance's own inherited
        // getClass() returns a Class, which must not itself be treated as allowed.
        assertFalse(AllowedTypes.isInstanceAllowed(String::class.java))
        assertFalse(AllowedTypes.isInstanceAllowed(null))
    }
}
