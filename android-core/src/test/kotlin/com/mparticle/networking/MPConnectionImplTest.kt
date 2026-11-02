package com.mparticle.networking

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class MPConnectionImplTest {
    private val httpConnection: HttpURLConnection = Mockito.mock(HttpURLConnection::class.java)
    private val httpsConnection: HttpsURLConnection = Mockito.mock(HttpsURLConnection::class.java)
    private val url: MPUrl = Mockito.mock(MPUrl::class.java)

    private fun wrap(httpURLConnection: HttpURLConnection?): MPConnection = MPConnectionImpl(httpURLConnection, url)

    @Test
    fun testIsHttpsOnlyForAnHttpsConnection() {
        Assert.assertFalse(wrap(httpConnection).isHttps)
        Assert.assertTrue(wrap(httpsConnection).isHttps)
        Assert.assertFalse(wrap(null).isHttps)
    }

    @Test
    fun testGetUrlReturnsTheUrlGiven() {
        val noUrl: MPConnection = MPConnectionImpl(httpConnection, null)

        Assert.assertSame(url, wrap(httpConnection).url)
        Assert.assertNull(noUrl.url)
    }

    @Test
    fun testSettersPassEachValueToTheConnection() {
        val connection = wrap(httpConnection)

        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setConnectTimeout(1000)
        connection.setReadTimeout(2000)
        connection.setRequestProperty("Content-Type", "application/json")

        Mockito.verify(httpConnection).setRequestMethod("POST")
        Mockito.verify(httpConnection).setDoOutput(true)
        Mockito.verify(httpConnection).setConnectTimeout(1000)
        Mockito.verify(httpConnection).setReadTimeout(2000)
        Mockito.verify(httpConnection).setRequestProperty("Content-Type", "application/json")
    }

    @Test
    fun testNullBoxedValuesThrowNullPointerExceptionBeforeTheConnectionIsCalled() {
        val connection = wrap(httpConnection)

        Assert.assertThrows(NullPointerException::class.java) { connection.setDoOutput(null) }
        Assert.assertThrows(NullPointerException::class.java) { connection.setConnectTimeout(null) }
        Assert.assertThrows(NullPointerException::class.java) { connection.setReadTimeout(null) }
        Mockito.verifyNoInteractions(httpConnection)
    }

    @Test
    fun testGettersReturnTheConnectionsValues() {
        val headers = mapOf<String?, List<String>>(null to listOf("HTTP/1.1 200 OK"), "Content-Type" to listOf("application/json"))
        val output = Mockito.mock(OutputStream::class.java)
        val input = Mockito.mock(InputStream::class.java)
        val error = Mockito.mock(InputStream::class.java)
        Mockito.`when`(httpConnection.getRequestMethod()).thenReturn("POST")
        Mockito.`when`(httpConnection.getHeaderField("Content-Type")).thenReturn("application/json")
        Mockito.`when`(httpConnection.getHeaderFields()).thenReturn(headers)
        Mockito.`when`(httpConnection.getOutputStream()).thenReturn(output)
        Mockito.`when`(httpConnection.getInputStream()).thenReturn(input)
        Mockito.`when`(httpConnection.getErrorStream()).thenReturn(error)
        Mockito.`when`(httpConnection.getResponseMessage()).thenReturn("OK")
        val connection = wrap(httpConnection)

        Assert.assertEquals("POST", connection.getRequestMethod())
        Assert.assertEquals("application/json", connection.getHeaderField("Content-Type"))
        Assert.assertNull(connection.getHeaderField("X-Missing"))
        Assert.assertSame(headers, connection.getHeaderFields())
        Assert.assertSame(output, connection.getOutputStream())
        Assert.assertSame(input, connection.getInputStream())
        Assert.assertSame(error, connection.getErrorStream())
        Assert.assertEquals("OK", connection.getResponseMessage())
    }

    @Test
    fun testGetResponseCodeAsksTheConnectionEachTime() {
        Mockito.`when`(httpConnection.getResponseCode()).thenReturn(200, 429)
        val connection = wrap(httpConnection)

        Assert.assertEquals(200, connection.getResponseCode())
        Assert.assertEquals(429, connection.getResponseCode())
        Mockito.verify(httpConnection, Mockito.times(2)).getResponseCode()
    }

    @Test
    fun testIOExceptionsFromTheConnectionPassThrough() {
        Mockito.`when`(httpConnection.getInputStream()).thenThrow(IOException("refused"))
        Mockito.`when`(httpConnection.getResponseCode()).thenThrow(IOException("timeout"))
        val connection = wrap(httpConnection)

        Assert.assertThrows(IOException::class.java) { connection.getInputStream() }
        Assert.assertThrows(IOException::class.java) { connection.getResponseCode() }
    }

    @Test
    fun testSslSocketFactoryIsSetAndReadOnAnHttpsConnection() {
        val factory = Mockito.mock(SSLSocketFactory::class.java)
        Mockito.`when`(httpsConnection.getSSLSocketFactory()).thenReturn(factory)
        val connection = wrap(httpsConnection)

        connection.setSSLSocketFactory(factory)

        Mockito.verify(httpsConnection).setSSLSocketFactory(factory)
        Assert.assertSame(factory, connection.getSSLSocketFactory())
    }

    @Test
    fun testSslSocketFactoryOnAPlainHttpConnectionThrowsClassCastException() {
        val connection = wrap(httpConnection)

        Assert.assertThrows(ClassCastException::class.java) { connection.setSSLSocketFactory(Mockito.mock(SSLSocketFactory::class.java)) }
        Assert.assertThrows(ClassCastException::class.java) { connection.getSSLSocketFactory() }
    }

    @Test
    fun testANullConnectionThrowsNullPointerExceptionWhenUsed() {
        val connection = wrap(null)

        Assert.assertThrows(NullPointerException::class.java) { connection.setRequestMethod("GET") }
        Assert.assertThrows(NullPointerException::class.java) { connection.getResponseCode() }
        Assert.assertThrows(NullPointerException::class.java) { connection.setSSLSocketFactory(null) }
    }
}
