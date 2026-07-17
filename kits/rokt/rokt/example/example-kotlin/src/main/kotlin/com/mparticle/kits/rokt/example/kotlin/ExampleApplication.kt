package com.mparticle.kits.rokt.example.kotlin

import android.app.Application
import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.IdentityApiRequest

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(PREF_API_KEY, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_API_KEY
        val apiSecret = prefs.getString(PREF_API_SECRET, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_API_SECRET
        MParticle.start(buildOptions(this, apiKey, apiSecret))
        registerAttributeListener()
    }

    companion object {
        const val TEST_EMAIL = "jenny.smith@example.com"
        const val TEST_CUSTOMER_ID = "rokt-kit-example-customer"

        const val PREFS_NAME = "example_config"
        const val PREF_API_KEY = "mparticle_api_key"
        const val PREF_API_SECRET = "mparticle_api_secret"
        const val DEFAULT_API_KEY = "REPLACE_WITH_MPARTICLE_API_KEY"
        const val DEFAULT_API_SECRET = "REPLACE_WITH_MPARTICLE_API_SECRET"

        fun buildOptions(context: Context, apiKey: String, apiSecret: String): MParticleOptions {
            val identifyRequest =
                IdentityApiRequest
                    .withEmptyUser()
                    .email(TEST_EMAIL)
                    .customerId(TEST_CUSTOMER_ID)
                    .build()
            return MParticleOptions
                .builder(context)
                .credentials(apiKey, apiSecret)
                .identify(identifyRequest)
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build()
        }

        fun registerAttributeListener() {
            MParticle.getInstance()?.Identity()?.addIdentityStateListener { user, _ ->
                user.setUserAttributes(
                    mapOf(
                        "firstname" to "Jenny",
                        "lastname" to "Smith",
                        "mobile" to "5551234567",
                    ),
                )
            }
        }
    }
}
