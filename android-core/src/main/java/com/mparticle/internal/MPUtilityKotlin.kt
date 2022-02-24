package com.mparticle.internal

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object MPUtilityKotlin {

    @JvmStatic
    fun readFile(context: Context, path: String): String? {
        synchronized(path) {
            return try {
                getFile(context, path).run {
                    if (exists()) {
                        readText(Charsets.UTF_8)
                    } else {
                        null
                    }
                }
            } catch (ex: Exception) {
                Logger.warning("Unable to read file $path")
                null
            }
        }
    }

    @JvmStatic
    fun writeToFile(context: Context, path: String, contents: String?): Boolean {
        synchronized(path) {
            return try {
                getFile(context, path).apply {
                    if (contents.isNullOrEmpty()) {
                        clearFile(context, path)
                    } else {
                        if (!exists()) {
                            parentFile.mkdirs()
                            createNewFile()
                        }
                        writeText(contents ?: "", Charsets.UTF_8)
                    }
                }
                true
            } catch (ex: Exception) {
                Logger.warning("Unable to write to $path")
                Logger.debug(ex.stackTraceToString())
                false
            }
        }
    }

    @JvmStatic
    fun clearFile(context: Context, path: String) {
        synchronized(path) {
            getFile(context, path).apply {
                if (exists()) {
                    delete()
                }
            }
        }
    }

    private fun getFile(context: Context,  path: String): File = File(context.cacheDir.path, path)
}