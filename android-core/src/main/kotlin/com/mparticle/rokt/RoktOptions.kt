package com.mparticle.rokt

class RoktOptions @JvmOverloads constructor(
    fontFilePathMap: Map<String, String> = emptyMap(),
    fontPostScriptNames: Set<String> = emptySet()
) {
    val fontFilePathMap: Map<String, String> = fontFilePathMap.toMap()
    val fontPostScriptNames: Set<String> = fontPostScriptNames.toSet()
}