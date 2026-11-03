package com.mparticle.networking

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

internal class MPUrlImpl(private val url: URL?) : MPUrl() {
    @Throws(MalformedURLException::class)
    constructor(url: String?) : this(URL(url))

    @Throws(IOException::class)
    override fun openConnection(): MPConnection = MPConnectionImpl(url!!.openConnection() as HttpURLConnection?, this)

    override fun getFile(): String? = url!!.file

    override fun getAuthority(): String? = url!!.authority

    override fun getPath(): String? = url!!.path

    override fun toString(): String = url!!.toString()

    override fun equals(other: Any?): Boolean {
        if (other is MPUrlImpl) {
            return url!! == other.url
        }
        return url!! == other
    }
}
