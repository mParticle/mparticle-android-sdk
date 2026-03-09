package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticleOptions
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class AdjustKitTests {
    private val kit: KitIntegration
        get() = AdjustKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(!name.isNullOrEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            val appToken = AdjustKit::class.java.getDeclaredField("APP_TOKEN")
            appToken.isAccessible = true
            val token = appToken.get(AdjustKit.Companion) as String
            settings[token] = "test1"
            val fbAppId = AdjustKit::class.java.getDeclaredField("FB_APP_ID_KEY")
            fbAppId.isAccessible = true
            val fbId = fbAppId.get(AdjustKit.Companion) as String
            settings[fbId] = "test2"
            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNotNull(e)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.name == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    @get:Throws(JSONException::class)
    private val attributionJSON: JSONObject
        get() {
            val jsonObject = JSONObject()
            jsonObject.putOpt("tracker_token", "a1")
            jsonObject.putOpt("tracker_name", "b2")
            jsonObject.putOpt("network", "c3")
            jsonObject.putOpt("campaign", "d4")
            jsonObject.putOpt("adgroup", "e5")
            jsonObject.putOpt("creative", "f6")
            jsonObject.putOpt("click_label", "g7")
            jsonObject.putOpt("adid", "h8")
            return jsonObject
        }
}
