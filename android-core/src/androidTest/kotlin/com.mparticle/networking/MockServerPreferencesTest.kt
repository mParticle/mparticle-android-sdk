package com.mparticle.networking

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.MParticle
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A request an earlier test left running reaches that test's MockServer. Handling it must not point ConfigManager's
 * shared preferences back at the earlier server's context, or the current test reads the earlier test's API key.
 */
@RunWith(AndroidJUnit4::class)
class MockServerPreferencesTest {
    private val baseContext: Context = InstrumentationRegistry.getInstrumentation().context
    private val olderContext = SeparatePreferencesContext(baseContext, "older")
    private val currentContext = SeparatePreferencesContext(baseContext, "current")

    @After
    fun after() {
        olderContext.preferences().edit().clear().commit()
        currentContext.preferences().edit().clear().commit()
        MockServer.stop()
        // Point ConfigManager back at the default store for the tests that follow.
        ConfigManager(baseContext)
    }

    @Test
    fun testOlderServerRequestKeepsCurrentPreferences() {
        MParticle.setInstance(null)
        olderContext.preferences().edit().putString(Constants.PrefKeys.API_KEY, "older-key").commit()
        currentContext.preferences().edit().putString(Constants.PrefKeys.API_KEY, "current-key").commit()

        // With no running MParticle instance, the earlier test's server builds its ConfigManager over the older store.
        val olderServer = MockServer.getNewInstance(olderContext)
        // The current test's ConfigManager points the shared preferences at the current store, as the next start does.
        val currentConfig = ConfigManager(currentContext)
        Assert.assertEquals("current-key", currentConfig.apiKey)

        // A request the earlier test left running reaches the earlier test's server.
        olderServer.onRequestMade(MPConnectionTestImpl(MPUrl.getUrl("https://config2.mparticle.com/v4/older-key/config", null)))

        Assert.assertEquals("current-key", currentConfig.apiKey)
    }

    // Each instance keeps its preferences in its own store, as two tests' contexts can.
    private class SeparatePreferencesContext(
        base: Context,
        private val prefix: String,
    ) : ContextWrapper(base) {
        override fun getSharedPreferences(
            name: String?,
            mode: Int,
        ): SharedPreferences = super.getSharedPreferences("$prefix-$name", mode)

        fun preferences(): SharedPreferences = getSharedPreferences(ConfigManager.PREFERENCES_FILE, Context.MODE_PRIVATE)
    }
}
