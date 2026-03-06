package com.mparticle.kits.mocks

import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import kotlin.Throws

class MockResources : Resources(null, null, null) {
    override fun getIdentifier(
        name: String,
        defType: String,
        defPackage: String,
    ): Int {
        if (name == "mp_key") {
            return 1
        } else if (name == "mp_secret") {
            return 2
        }
        return 0
    }

    @Throws(NotFoundException::class)
    override fun getString(id: Int): String {
        when (id) {
            1 -> return testAppKey
            2 -> return testAppSecret
        }
        return ""
    }

    @Throws(NotFoundException::class)
    override fun getString(
        id: Int,
        vararg formatArgs: Any,
    ): String = super.getString(id, *formatArgs)

    companion object {
        var testAppKey = "the app key"
        var testAppSecret = "the app secret"
    }
}
