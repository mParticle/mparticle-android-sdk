package com.mparticle.rokt

class RoktOptions @JvmOverloads constructor(
    val fontFilePathMap: Map<String, String> = emptyMap(),
    val fontPostScriptNames: Set<String> = emptySet()
)