package com.mparticle.tooling

object Logger {
    var verbose = false

    fun error(message: String) {
        println(message)
    }

    fun verbose(message: String) {
        if (verbose) {
            println(message)
        }
    }

    fun warning(message: String) {
        println(message)
    }
}
