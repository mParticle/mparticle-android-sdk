package com.mparticle.internal

import android.content.Context
import com.mparticle.mock.MockContext
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ProviderPersistenceTest {
    @Test
    fun testResolvesPreferenceTypesAndCachesMpPrefixedValue() {
        val context = MockContext()
        val prefs = context.getSharedPreferences("external_sdk_prefs", Context.MODE_PRIVATE)
        prefs
            .edit()
            .putString("str_key", "hello")
            .putInt("int_key", 42)
            .putBoolean("bool_key", true)
            .putFloat("float_key", 3.5f)
            .putLong("long_key", 9876543210L)
            .apply()

        val config =
            JSONObject(
                """
                {
                  "cms": [
                    {
                      "id": 7,
                      "pr": [
                        {
                          "f": "external_sdk_prefs",
                          "ps": [
                            { "k": "str_key", "t": 1, "n": "out_str", "d": "def_str" },
                            { "k": "int_key", "t": 2, "n": "out_int", "d": "0" },
                            { "k": "bool_key", "t": 3, "n": "out_bool", "d": "false" },
                            { "k": "float_key", "t": 4, "n": "out_float", "d": "0.0" },
                            { "k": "long_key", "t": 5, "n": "out_long", "d": "0" }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )
        val allowed = listOf("str_key", "int_key", "bool_key", "float_key", "long_key")
        val persistence = ProviderPersistence(config, context, allowed)
        val moduleValues = persistence.getJSONObject("7")
        Assert.assertEquals("hello", moduleValues.getString("out_str"))
        Assert.assertEquals("42", moduleValues.getString("out_int"))
        Assert.assertEquals("true", moduleValues.getString("out_bool"))
        Assert.assertEquals("3.5", moduleValues.getString("out_float"))
        Assert.assertEquals("9876543210", moduleValues.getString("out_long"))

        // Subsequent reads use the cached mp::<mpvar> preference entry.
        prefs.edit().putString("str_key", "changed").apply()
        val cachedPersistence = ProviderPersistence(config, context, allowed)
        Assert.assertEquals("hello", cachedPersistence.getJSONObject("7").getString("out_str"))
    }

    @Test
    fun testAppliesDefaultMacrosWhenKeyAbsent() {
        val context = MockContext()
        val config =
            JSONObject(
                """
                {
                  "cms": [
                    {
                      "id": 9,
                      "pr": [
                        {
                          "f": "macro_prefs",
                          "ps": [
                            { "k": "k_gn", "t": 1, "n": "gn", "d": "%gn%" },
                            { "k": "k_oaid", "t": 1, "n": "oaid", "d": "%oaid%" },
                            { "k": "k_g", "t": 1, "n": "g", "d": "%g%" },
                            { "k": "k_ts", "t": 1, "n": "ts", "d": "%ts%" },
                            { "k": "k_glsb", "t": 1, "n": "glsb", "d": "%glsb%" },
                            { "k": "k_lit", "t": 1, "n": "lit", "d": "literal_default" }
                          ]
                        }
                      ]
                    },
                    {
                      "id": 10
                    }
                  ]
                }
                """.trimIndent(),
            )
        val allowed = listOf("k_gn", "k_oaid", "k_g", "k_ts", "k_glsb", "k_lit")
        val persistence = ProviderPersistence(config, context, allowed)
        val moduleValues = persistence.getJSONObject("9")

        val gn = moduleValues.getString("gn")
        Assert.assertEquals(32, gn.length)
        Assert.assertFalse(gn.contains("-"))

        val oaid = moduleValues.getString("oaid")
        Assert.assertTrue(oaid.matches(Regex("^[0-7][0-9A-F]{15}-[0-2][0-9A-F]{15}$")))

        val g = moduleValues.getString("g")
        Assert.assertEquals(36, g.length)
        Assert.assertTrue(g.contains("-"))

        val ts = moduleValues.getString("ts")
        Assert.assertTrue(ts.toLong() > 0L)

        val glsb = moduleValues.getString("glsb")
        Assert.assertNotNull(glsb.toLong())

        Assert.assertEquals("literal_default", moduleValues.getString("lit"))
        Assert.assertEquals(0, persistence.getJSONObject("10").length())
    }
}
