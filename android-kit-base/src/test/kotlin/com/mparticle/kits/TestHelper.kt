package com.mparticle.kits

import org.mockito.Mockito

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T = null as T
}

fun <T> any(type: Class<T>): T = Mockito.any<T>(type)
