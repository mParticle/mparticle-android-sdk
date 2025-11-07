package com.mparticle.rokt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoktOptionsTest {
    @Test
    fun testDefaultConstructor_shouldCreateEmptyOptions() {
        val roktOptions = RoktOptions()

        assertTrue("fontFilePathMap should be empty", roktOptions.fontFilePathMap.isEmpty())
        assertTrue("fontPostScriptNames should be empty", roktOptions.fontPostScriptNames.isEmpty())
    }

    @Test
    fun testConstructorWithFontFilePathMap_shouldSetCorrectValues() {
        val fontMap =
            mapOf(
                "font1" to "/path/to/font1.ttf",
                "font2" to "/path/to/font2.otf",
            )

        val roktOptions = RoktOptions(fontFilePathMap = fontMap)

        assertEquals("fontFilePathMap should match", fontMap, roktOptions.fontFilePathMap)
        assertTrue("fontPostScriptNames should be empty", roktOptions.fontPostScriptNames.isEmpty())
        assertEquals("Should have 2 font files", 2, roktOptions.fontFilePathMap.size)
        assertEquals("Should contain font1 path", "/path/to/font1.ttf", roktOptions.fontFilePathMap["font1"])
        assertEquals("Should contain font2 path", "/path/to/font2.otf", roktOptions.fontFilePathMap["font2"])
    }

    @Test
    fun testConstructorWithFontPostScriptNames_shouldSetCorrectValues() {
        val fontNames =
            setOf(
                "Arial-Bold",
                "Helvetica-Light",
                "CustomFont-Regular",
            )

        val roktOptions = RoktOptions(fontPostScriptNames = fontNames)

        assertTrue("fontFilePathMap should be empty", roktOptions.fontFilePathMap.isEmpty())
        assertEquals("fontPostScriptNames should match", fontNames, roktOptions.fontPostScriptNames)
        assertEquals("Should have 3 font names", 3, roktOptions.fontPostScriptNames.size)
        assertTrue("Should contain Arial-Bold", roktOptions.fontPostScriptNames.contains("Arial-Bold"))
        assertTrue("Should contain Helvetica-Light", roktOptions.fontPostScriptNames.contains("Helvetica-Light"))
        assertTrue("Should contain CustomFont-Regular", roktOptions.fontPostScriptNames.contains("CustomFont-Regular"))
    }

    @Test
    fun testConstructorWithBothParameters_shouldSetBothValues() {
        val fontMap = mapOf("font1" to "/path/to/font1.ttf")
        val fontNames = setOf("Arial-Bold")

        val roktOptions =
            RoktOptions(
                fontFilePathMap = fontMap,
                fontPostScriptNames = fontNames,
            )

        assertEquals("fontFilePathMap should match", fontMap, roktOptions.fontFilePathMap)
        assertEquals("fontPostScriptNames should match", fontNames, roktOptions.fontPostScriptNames)
        assertEquals("Should have 1 font file", 1, roktOptions.fontFilePathMap.size)
        assertEquals("Should have 1 font name", 1, roktOptions.fontPostScriptNames.size)
    }

    @Test
    fun testConstructorWithEmptyCollections_shouldCreateEmptyOptions() {
        val roktOptions =
            RoktOptions(
                fontFilePathMap = emptyMap(),
                fontPostScriptNames = emptySet(),
            )

        assertTrue("fontFilePathMap should be empty", roktOptions.fontFilePathMap.isEmpty())
        assertTrue("fontPostScriptNames should be empty", roktOptions.fontPostScriptNames.isEmpty())
    }

    @Test
    fun testFontFilePathMapImmutability_shouldNotAllowModification() {
        val mutableMap = mutableMapOf("font1" to "/path/to/font1.ttf")
        val roktOptions = RoktOptions(fontFilePathMap = mutableMap)

        // Modify the original map
        mutableMap["font2"] = "/path/to/font2.ttf"

        // The RoktOptions should not be affected if the implementation is correct
        assertEquals("RoktOptions should have 1 font file", 1, roktOptions.fontFilePathMap.size)
        assertFalse("RoktOptions should not contain font2", roktOptions.fontFilePathMap.containsKey("font2"))
    }

    @Test
    fun testFontPostScriptNamesImmutability_shouldNotAllowModification() {
        val mutableSet = mutableSetOf("Arial-Bold")
        val roktOptions = RoktOptions(fontPostScriptNames = mutableSet)

        // Modify the original set
        mutableSet.add("Helvetica-Light")

        // The RoktOptions should not be affected if the implementation is correct
        assertEquals("RoktOptions should have 1 font name", 1, roktOptions.fontPostScriptNames.size)
        assertFalse("RoktOptions should not contain Helvetica-Light", roktOptions.fontPostScriptNames.contains("Helvetica-Light"))
    }

    @Test
    fun testMultipleInstances_shouldBeIndependent() {
        val fontMap1 = mapOf("font1" to "/path1")
        val fontMap2 = mapOf("font2" to "/path2")

        val roktOptions1 = RoktOptions(fontFilePathMap = fontMap1)
        val roktOptions2 = RoktOptions(fontFilePathMap = fontMap2)

        assertNotEquals("Instances should be different", roktOptions1.fontFilePathMap, roktOptions2.fontFilePathMap)
        assertEquals("First instance should have font1", "/path1", roktOptions1.fontFilePathMap["font1"])
        assertEquals("Second instance should have font2", "/path2", roktOptions2.fontFilePathMap["font2"])
        assertNull("First instance should not have font2", roktOptions1.fontFilePathMap["font2"])
        assertNull("Second instance should not have font1", roktOptions2.fontFilePathMap["font1"])
    }

    @Test
    fun testLargeCollections_shouldHandleCorrectly() {
        val largeFontMap = (1..100).associate { "font$it" to "/path/to/font$it.ttf" }
        val largeFontNames = (1..100).map { "Font-$it" }.toSet()

        val roktOptions =
            RoktOptions(
                fontFilePathMap = largeFontMap,
                fontPostScriptNames = largeFontNames,
            )

        assertEquals("Should have 100 font files", 100, roktOptions.fontFilePathMap.size)
        assertEquals("Should have 100 font names", 100, roktOptions.fontPostScriptNames.size)
        assertEquals("Should contain first font file", "/path/to/font1.ttf", roktOptions.fontFilePathMap["font1"])
        assertEquals("Should contain last font file", "/path/to/font100.ttf", roktOptions.fontFilePathMap["font100"])
        assertTrue("Should contain first font name", roktOptions.fontPostScriptNames.contains("Font-1"))
        assertTrue("Should contain last font name", roktOptions.fontPostScriptNames.contains("Font-100"))
    }

    @Test
    fun testEquality_sameParameters_shouldBeEqual() {
        val fontMap = mapOf("font1" to "/path1")
        val fontNames = setOf("Arial-Bold")

        val roktOptions1 = RoktOptions(fontFilePathMap = fontMap, fontPostScriptNames = fontNames)
        val roktOptions2 = RoktOptions(fontFilePathMap = fontMap, fontPostScriptNames = fontNames)

        // Note: Kotlin data classes would provide equals() automatically, but since RoktOptions
        // is not a data class, we test the property values individually
        assertEquals("fontFilePathMap should be equal", roktOptions1.fontFilePathMap, roktOptions2.fontFilePathMap)
        assertEquals("fontPostScriptNames should be equal", roktOptions1.fontPostScriptNames, roktOptions2.fontPostScriptNames)
    }

    @Test
    fun testSpecialCharactersInPaths_shouldHandleCorrectly() {
        val fontMap =
            mapOf(
                "font with spaces" to "/path with spaces/font file.ttf",
                "font-with-dashes" to "/path-with-dashes/font-file.ttf",
                "font_with_underscores" to "/path_with_underscores/font_file.ttf",
                "fontWithUnicode" to "/path/with/unicode/字体.ttf",
            )

        val roktOptions = RoktOptions(fontFilePathMap = fontMap)

        assertEquals("Should handle spaces", "/path with spaces/font file.ttf", roktOptions.fontFilePathMap["font with spaces"])
        assertEquals("Should handle dashes", "/path-with-dashes/font-file.ttf", roktOptions.fontFilePathMap["font-with-dashes"])
        assertEquals(
            "Should handle underscores",
            "/path_with_underscores/font_file.ttf",
            roktOptions.fontFilePathMap["font_with_underscores"],
        )
        assertEquals("Should handle unicode", "/path/with/unicode/字体.ttf", roktOptions.fontFilePathMap["fontWithUnicode"])
    }

    @Test
    fun testSpecialCharactersInFontNames_shouldHandleCorrectly() {
        val fontNames =
            setOf(
                "Arial-Bold",
                "Helvetica_Light",
                "Font With Spaces",
                "Font.With.Dots",
                "字体名称", // Unicode font name
            )

        val roktOptions = RoktOptions(fontPostScriptNames = fontNames)

        assertTrue("Should contain Arial-Bold", roktOptions.fontPostScriptNames.contains("Arial-Bold"))
        assertTrue("Should contain Helvetica_Light", roktOptions.fontPostScriptNames.contains("Helvetica_Light"))
        assertTrue("Should contain Font With Spaces", roktOptions.fontPostScriptNames.contains("Font With Spaces"))
        assertTrue("Should contain Font.With.Dots", roktOptions.fontPostScriptNames.contains("Font.With.Dots"))
        assertTrue("Should contain unicode font name", roktOptions.fontPostScriptNames.contains("字体名称"))
    }
}
