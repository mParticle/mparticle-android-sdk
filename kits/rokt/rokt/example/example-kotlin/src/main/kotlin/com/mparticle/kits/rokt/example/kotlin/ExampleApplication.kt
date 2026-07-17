package com.mparticle.kits.rokt.example.kotlin

import android.app.Application
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.IdentityApiRequest

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val identifyRequest =
            IdentityApiRequest
                .withEmptyUser()
                .email(TEST_EMAIL)
                .customerId(TEST_CUSTOMER_ID)
                .build()
        val options =
            MParticleOptions
                .builder(this)
                .credentials(
                    "REPLACE WITH YOUR MPARTICLE API KEY",
                    "REPLACE WITH YOUR MPARTICLE API SECRET",
                ).identify(identifyRequest)
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build()
        MParticle.start(options)
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

    companion object {
        const val TEST_EMAIL = "jenny.smith@example.com"
        const val TEST_CUSTOMER_ID = "rokt-kit-example-customer"
    }
}
