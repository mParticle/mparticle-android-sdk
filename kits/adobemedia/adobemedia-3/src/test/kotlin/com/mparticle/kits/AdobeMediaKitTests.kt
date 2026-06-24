package com.mparticle.kits

import android.app.Application
import android.content.Context
import com.adobe.marketing.mobile.Media
import com.adobe.marketing.mobile.MediaTracker
import com.adobe.marketing.mobile.MobileCore
import com.mparticle.MParticleOptions
import com.mparticle.media.events.ContentType
import com.mparticle.media.events.MediaContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class AdobeMediaKitTests {
    private val kit: AdobeKit
        get() =
            object : AdobeKit() {
                val tracker: MediaTracker?
                    get() {
                        return super.defaultMediaTracker
                    }
            }

    @Test
    fun testGetName() {
        val name = kit.name
        assertTrue(name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit: KitIntegration = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
        } catch (ex: Exception) {
            e = ex
        }
        assertNotNull(e)
    }

    @Test
    fun testClassName() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = AdobeKit()::class.java.name
        assertEquals("$className not found as a known integration.", 1, integrations.filter { it.name == className }.count())
    }

    @Test
    fun testMediaConfig() {
        val testKit =
            object : AdobeKit() {
                val tracker: MediaTracker?
                    get() {
                        return super.defaultMediaTracker
                    }
            }
        val trackingServer = "launch app idea"
        val settings = mutableMapOf(AdobeKit.LAUNCH_APP_ID to trackingServer)
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(Mockito.mock(Application::class.java))

        testKit.kitManager = Mockito.mock(KitManagerImpl::class.java)
        Mockito.`when`(testKit.kitManager.getIntegrationAttributes(Mockito.any(KitIntegration::class.java))).thenReturn(
            mapOf(AdobeKit.MARKETING_CLOUD_ID_KEY to "not nothing"),
        )
        testKit.onKitCreate(settings, context)

        assertEquals(trackingServer, MobileCore.configKey)
        assertNotNull(testKit.tracker)
    }

    @Test
    fun toSecondsTest() {
        kit.apply {
            assertEquals(1.001, 1001L.toSeconds(), 0.0)
        }
    }

    @Test
    fun getMediaTypeTest() {
        var mediaContent =
            MediaContent().apply {
                contentType = ContentType.AUDIO
            }
        kit.apply {
            assertEquals(Media.MediaType.Audio, mediaContent.getMediaType())
        }
        mediaContent =
            MediaContent().apply {
                contentType = ContentType.VIDEO
            }
        kit.apply {
            assertEquals(Media.MediaType.Video, mediaContent.getMediaType())
        }
    }
}
