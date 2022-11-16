package com.mparticle

import org.mockito.Mockito

fun <T> anyObject(): T {
    Mockito.any<T>()
    return uninitialized()
}
@Suppress("UNCHECKED_CAST")
fun <T> uninitialized(): T = null as T
