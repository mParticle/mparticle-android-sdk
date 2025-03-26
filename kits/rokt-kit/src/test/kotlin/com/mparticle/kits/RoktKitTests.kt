package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import io.mockk.*
import org.json.JSONArray
import org.junit.Before
import org.mockito.Mockito.mock
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*


class RoktKitTests {
    private val context = mockk<Context>(relaxed = true)
    private val roktKit = RoktKit()
    private val settings = HashMap<String, String>()
    private lateinit var kitManager: TestKitManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        settings["application_id"] = TEST_APPLICATION_ID
        every { context.applicationContext } returns context
        MParticle.setInstance(TestMParticle())
        kitManager = TestKitManager()
    }



    private inner class TestKitManager internal constructor() :
        KitManagerImpl(context, null, TestCoreCallbacks(), mock(MParticleOptions::class.java)) {
        var attributes = HashMap<String, String>()
        var result: AttributionResult? = null
        private var error: AttributionError? = null
        public override fun getIntegrationAttributes(kitIntegration: KitIntegration): Map<String, String> {
            return attributes
        }

        public override fun setIntegrationAttributes(
            kitIntegration: KitIntegration,
            integrationAttributes: Map<String, String>
        ) {
            attributes = integrationAttributes as HashMap<String, String>
        }

        override fun onResult(result: AttributionResult) {
            this.result = result
        }

        override fun onError(error: AttributionError) {
            this.error = error
        }
    }

    private inner class TestKitConfiguration : KitConfiguration() {
        override fun getKitId(): Int = TEST_KIT_ID
    }

    private inner class TestMParticle : MParticle() {
        override fun Identity(): IdentityApi = mock(IdentityApi::class.java)

    }

    internal inner class TestCoreCallbacks : CoreCallbacks {
        override fun isBackgrounded(): Boolean = false
        override fun getUserBucket(): Int = 0
        override fun isEnabled(): Boolean = false
        override fun setIntegrationAttributes(i: Int, map: Map<String, String>) {}
        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null
        override fun getCurrentActivity(): WeakReference<Activity>? = null
        override fun getLatestKitConfiguration(): JSONArray? = null
        override fun getDataplanOptions(): DataplanOptions? = null
        override fun isPushEnabled(): Boolean = false
        override fun getPushSenderId(): String? = null
        override fun getPushInstanceId(): String? = null
        override fun getLaunchUri(): Uri? = null
        override fun getLaunchAction(): String? = null
        override fun getKitListener(): KitListener {
            return object : KitListener {
                override fun kitFound(kitId: Int) {}
                override fun kitConfigReceived(kitId: Int, configuration: String?) {}
                override fun kitExcluded(kitId: Int, reason: String?) {}
                override fun kitStarted(kitId: Int) {}
                override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {
                }

                override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {
                }
            }
        }
    }

    companion object {
        private const val TEST_APPLICATION_ID = "app-abcdef1234567890"
        private const val TEST_ATTRIBUTION_TOKEN = "srctok-abcdef1234567890"
        private const val TEST_DEEP_LINK = "https://www.example.com/product/abc123"
        private const val TEST_KIT_ID = 0x01

        /*
     * Test Helpers
     */
        @Throws(Exception::class)
        private fun setTestSdkVersion(sdkVersion: Int) {
            setFinalStatic(VERSION::class.java.getField("SDK_INT"), sdkVersion)
        }

        @Throws(Exception::class)
        private fun setFinalStatic(field: Field, newValue: Int) {
            field.isAccessible = true
            val getDeclaredFields0 =
                Class::class.java.getDeclaredMethod(
                    "getDeclaredFields0",
                    Boolean::class.javaPrimitiveType
                )
            getDeclaredFields0.isAccessible = true
            val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
            var modifiersField: Field? = null
            for (each in fields) {
                if ("modifiers" == each.name) {
                    modifiersField = each
                    break
                }
            }
            modifiersField!!.isAccessible = true
            modifiersField!!.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[null] = newValue

        }
    }
}