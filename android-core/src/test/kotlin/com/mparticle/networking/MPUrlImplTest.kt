package com.mparticle.networking

import org.junit.Assert
import org.junit.Test
import java.net.MalformedURLException
import java.net.URL

class MPUrlImplTest {
    @Test
    fun testStringConstructorReadsTheFileAuthorityAndPath() {
        val url: MPUrl = MPUrlImpl("https://nativesdks.mparticle.com:443/v2/key/events?b=1#top")

        Assert.assertEquals("/v2/key/events?b=1", url.file)
        Assert.assertEquals("nativesdks.mparticle.com:443", url.authority)
        Assert.assertEquals("/v2/key/events", url.path)
    }

    @Test
    fun testUrlConstructorReadsTheFileAuthorityAndPath() {
        val url: MPUrl = MPUrlImpl(URL("https://config2.mparticle.com/v4/key/config?av=1"))

        Assert.assertEquals("/v4/key/config?av=1", url.file)
        Assert.assertEquals("config2.mparticle.com", url.authority)
        Assert.assertEquals("/v4/key/config", url.path)
    }

    @Test
    fun testToStringIsTheWholeUrlNotTheFile() {
        val url: MPUrl = MPUrlImpl("https://nativesdks.mparticle.com/v2/key/events?b=1#top")

        Assert.assertEquals("https://nativesdks.mparticle.com/v2/key/events?b=1#top", url.toString())
    }

    @Test
    fun testMalformedOrNullStringThrowsMalformedUrlException() {
        val nullUrl: String? = null

        Assert.assertThrows(MalformedURLException::class.java) { MPUrlImpl("not a url") }
        Assert.assertThrows(MalformedURLException::class.java) { MPUrlImpl(nullUrl) }
    }

    @Test
    fun testEqualsComparesTheUnderlyingUrl() {
        val url = MPUrlImpl("https://127.0.0.1/v2/events")

        Assert.assertTrue(url.equals(MPUrlImpl("https://127.0.0.1/v2/events")))
        Assert.assertTrue(url.equals(URL("https://127.0.0.1/v2/events")))
        Assert.assertFalse(url.equals(MPUrlImpl("https://127.0.0.1/v2/identify")))
        Assert.assertFalse(url.equals("https://127.0.0.1/v2/events"))
        Assert.assertFalse(url.equals(null))
    }

    @Test
    fun testOpenConnectionWrapsAnHttpsConnectionWithThisUrl() {
        val url = MPUrlImpl("https://127.0.0.1/v2/events")

        val connection = url.openConnection()

        Assert.assertTrue(connection is MPConnectionImpl)
        Assert.assertTrue(connection.isHttps)
        Assert.assertSame(url, connection.url)
    }

    @Test
    fun testOpenConnectionOnPlainHttpIsNotHttps() {
        val connection = MPUrlImpl("http://127.0.0.1/v2/events").openConnection()

        Assert.assertFalse(connection.isHttps)
    }

    @Test
    fun testOpenConnectionOnANonHttpUrlThrowsClassCastException() {
        val url = MPUrlImpl("file:/mparticle/config.json")

        Assert.assertThrows(ClassCastException::class.java) { url.openConnection() }
    }
}
