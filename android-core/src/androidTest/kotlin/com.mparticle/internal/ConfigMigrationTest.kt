package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigMigrationTest : BaseCleanInstallEachTest() {
    private val oldConfigSharedprefsKey = "json"

    @Test
    fun testConfigContentsMigratedProperly() {
        // store config in the old place
        val oldConfig = JSONObject().put("foo", "bar")
        setOldConfigState(oldConfig)

        // double check that there is nothing stored in the new place
        assertOldConfigState(oldConfig)

        // initialize ConfigManager
        val configManager = ConfigManager(mContext)

        // make sure the config is in the new place and gone from the old
        assertNewConfigState(oldConfig)

        // make sure config is also available via ConfigManager api
        assertEquals(oldConfig.toString(), configManager.config)
    }

    @Test
    fun testConfigMetadataRemainsDuringMigration() {
        val oldConfig = JSONObject().put("foo", "bar")
        val testTimestamp = System.currentTimeMillis() - 1000L
        val testEtag = "some etag"
        val testIfModified = "foo bar"

        // set metadata + config in the old place
        ConfigManager(mContext).apply {
            saveConfigJson(JSONObject(), null, testEtag, testIfModified, testTimestamp)
            setOldConfigState(oldConfig)
        }

        assertOldConfigState(oldConfig)

        // trigger migration, check that config metadata remained the same
        ConfigManager(mContext).apply {
            assertNewConfigState(oldConfig)
            assertEquals(oldConfig.toString(), config)
            assertEquals(testTimestamp, configTimestamp)
            assertEquals(testEtag, etag)
            assertEquals(testIfModified, ifModified)
        }
    }

    @Test
    fun testMigratingStaleConfig() {
        val oldConfig = JSONObject().put("foo", "bar")
        val testTimestamp = System.currentTimeMillis() - 1000L
        val testEtag = "some etag"
        val testIfModified = "foo bar"

        // set metadata + config in the old place
        ConfigManager(mContext).apply {
            saveConfigJson(JSONObject(), null, testEtag, testIfModified, testTimestamp)
            setOldConfigState(oldConfig)
        }

        assertOldConfigState(oldConfig)

        MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configMaxAgeSeconds(0)
            .build()
            .let {
                // start it the simple way, we don't want to block or anything so we can test config state
                MParticle.start(it)
            }

        // make sure config is deleted
        MParticle.getInstance()!!.Internal().configManager.let {
            assertTrue(it.config?.isEmpty()!!)
            assertNull(it.configTimestamp)
            assertNull(it.etag)
            assertNull(it.ifModified)
        }
    }

    @Test
    fun testMigratingNotStaleConfig() {
        val oldConfig = JSONObject().put("foo", "bar")
        val testTimestamp = System.currentTimeMillis() - 1000L
        val testEtag = "some etag"
        val testIfModified = "foo bar"

        // set metadata + config in the old place
        ConfigManager(mContext).apply {
            saveConfigJson(JSONObject(), null, testEtag, testIfModified, testTimestamp)
            setOldConfigState(oldConfig)
        }

        assertOldConfigState(oldConfig)

        MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configMaxAgeSeconds(Integer.MAX_VALUE)
            .build()
            .let {
                // start it the simple way, we don't want to block or anything so we can test config state
                MParticle.start(it)
            }

        // make sure config is deleted
        MParticle.getInstance()!!.Internal().configManager.let {
            assertEquals(oldConfig.toString(), it.config)
            assertEquals(testTimestamp, it.configTimestamp)
            assertEquals(testEtag, it.etag)
            assertEquals(testIfModified, it.ifModified)
        }
    }

    private fun setOldConfigState(config: JSONObject) {
        ConfigManager.getInstance(mContext).getKitConfigPreferences().edit()
            .remove(ConfigManager.KIT_CONFIG_KEY)
        ConfigManager.getPreferences(mContext).edit()
            .putString(oldConfigSharedprefsKey, config.toString()).apply()
    }

    private fun assertOldConfigState(config: JSONObject) {
        assertEquals(
            config.toString(),
            ConfigManager.getPreferences(mContext)
                .getString(oldConfigSharedprefsKey, JSONArray().toString())
        )
    }

    private fun assertNewConfigState(config: JSONObject) {
        val configString = ConfigManager.getPreferences(mContext)
            .getString(ConfigManager.CONFIG_JSON, JSONArray().toString())
        assertNull(JSONObject(configString).optJSONArray(ConfigManager.KEY_EMBEDDED_KITS))
        assertEquals(
            config.optString(ConfigManager.KEY_EMBEDDED_KITS, JSONArray().toString()),
            ConfigManager.getInstance(mContext).kitConfigPreferences.getString(
                ConfigManager.KIT_CONFIG_KEY,
                JSONArray().toString()
            )
        )
    }
}
