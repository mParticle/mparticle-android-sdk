package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticleOptions
import com.mparticle.kits.testkits.ListenerTestKit
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before

class KnownUserKitsLifecycleTest : BaseKitOptionsTest() {
    @Before
    @Throws(JSONException::class)
    fun before() {
        val builder = MParticleOptions.builder(mContext)
            .configuration(
                ConfiguredKitOptions()
                    .addKit(-1, TestKit1::class.java, JSONObject().put("eau", true))
                    .addKit(-2, TestKit2::class.java, JSONObject().put("eau", false))
                    .addKit(-3, TestKit3::class.java, JSONObject().put("eau", true))
            )
        startMParticle(builder)
    }

    class TestKit1 : TestKit()
    class TestKit2 : TestKit()
    class TestKit3 : TestKit()
    open class TestKit : ListenerTestKit() {
        override fun getName(): String {
            return "test kit" + i++
        }

        @Throws(IllegalArgumentException::class)
        override fun onKitCreate(
            settings: Map<String, String>?,
            context: Context
        ): List<ReportingMessage> {
            return emptyList()
        }

        override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
            return emptyList()
        }

        companion object {
            var i = 0
        }
    }
}
