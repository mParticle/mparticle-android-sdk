package com.mparticle.networking

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal class MPConnectionImpl(private val httpURLConnection: HttpURLConnection?, private val url: MPUrl?) : MPConnection {
    private val responseCode: Int? = null

    override fun isHttps(): Boolean = httpURLConnection is HttpsURLConnection

    @Throws(ProtocolException::class)
    override fun setRequestMethod(method: String?) {
        httpURLConnection!!.requestMethod = method
    }

    override fun setDoOutput(doOutput: Boolean?) {
        httpURLConnection!!.doOutput = doOutput!!
    }

    override fun setConnectTimeout(timeout: Int?) {
        httpURLConnection!!.connectTimeout = timeout!!
    }

    override fun setReadTimeout(readTimeout: Int?) {
        httpURLConnection!!.readTimeout = readTimeout!!
    }

    override fun setRequestProperty(key: String?, value: String?) {
        httpURLConnection!!.setRequestProperty(key, value)
    }

    override fun getURL(): MPUrl? = url

    override fun getRequestMethod(): String? = httpURLConnection!!.requestMethod

    override fun getHeaderField(key: String?): String? = httpURLConnection!!.getHeaderField(key)

    override fun getHeaderFields(): Map<String?, List<String>>? = httpURLConnection!!.headerFields

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream? = httpURLConnection!!.outputStream

    @Throws(IOException::class)
    override fun getInputStream(): InputStream? = httpURLConnection!!.inputStream

    override fun getErrorStream(): InputStream? = httpURLConnection!!.errorStream

    @Throws(IOException::class)
    override fun getResponseCode(): Int = responseCode ?: httpURLConnection!!.responseCode

    @Throws(IOException::class)
    override fun getResponseMessage(): String? = httpURLConnection!!.responseMessage

    override fun setSSLSocketFactory(factory: SSLSocketFactory?) {
        (httpURLConnection as HttpsURLConnection).sslSocketFactory = factory
    }

    override fun getSSLSocketFactory(): SSLSocketFactory? = (httpURLConnection as HttpsURLConnection).sslSocketFactory
}
