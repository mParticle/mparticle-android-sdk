package com.mparticle

class Test {

    fun test() {
        val output = listOf(1, 4, 3)
            .fold(StringBuilder()) { initial, next ->
                initial.append(next.toString())
                initial
            }.toString()

        listOf("1", 3, "a")
            .joinToString() {
                it.toString()
            }
    }
}
