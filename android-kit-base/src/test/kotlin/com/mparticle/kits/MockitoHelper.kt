package com.mparticle.kits

import org.mockito.Mockito

fun <T> anyObject(): T = Mockito.any<T>()
fun <T> anyObject(clazz: Class<T>): T = Mockito.any<T>()
fun <T> safeEq(obj: T): T = Mockito.eq<T>(obj)
