package com.mparticle.kits.rokt.example.kotlin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mparticle.MParticle
import com.mparticle.kits.MParticleRokt
import com.mparticle.kits.RoktEmbeddedView
import com.mparticle.kits.RoktLayoutDimensionCallBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private val observedIdentifiers = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpCredentialFields()

        val embeddedView = findViewById<RoktEmbeddedView>(R.id.rokt_embedded_view)
        embeddedView.dimensionCallBack =
            object : RoktLayoutDimensionCallBack {
                override fun onHeightChanged(height: Int) {
                    Log.d(TAG, "Embedded placement height changed: $height")
                }
            }

        findViewById<Button>(R.id.show_overlay_button).setOnClickListener {
            observeEvents(OVERLAY_PLACEMENT_IDENTIFIER)
            MParticleRokt.Rokt().selectPlacements(
                OVERLAY_PLACEMENT_IDENTIFIER,
                placementAttributes(),
            )
        }

        findViewById<Button>(R.id.show_bottom_sheet_button).setOnClickListener {
            observeEvents(BOTTOM_SHEET_PLACEMENT_IDENTIFIER)
            MParticleRokt.Rokt().selectPlacements(
                BOTTOM_SHEET_PLACEMENT_IDENTIFIER,
                placementAttributes(),
            )
        }

        findViewById<Button>(R.id.show_embedded_button).setOnClickListener {
            observeEvents(EMBEDDED_PLACEMENT_IDENTIFIER)
            MParticleRokt.Rokt().selectPlacements(
                EMBEDDED_PLACEMENT_IDENTIFIER,
                placementAttributes(),
                mapOf(EMBEDDED_PLACEHOLDER_NAME to WeakReference(embeddedView)),
            )
        }

        findViewById<Button>(R.id.close_button).setOnClickListener {
            MParticleRokt.Rokt().close()
        }
    }

    private fun setUpCredentialFields() {
        val prefs = getSharedPreferences(ExampleApplication.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val apiSecretInput = findViewById<EditText>(R.id.api_secret_input)
        apiKeyInput.setText(
            prefs.getString(ExampleApplication.PREF_API_KEY, null) ?: ExampleApplication.DEFAULT_API_KEY,
        )
        apiSecretInput.setText(
            prefs.getString(ExampleApplication.PREF_API_SECRET, null) ?: ExampleApplication.DEFAULT_API_SECRET,
        )

        findViewById<Button>(R.id.save_credentials_button).setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            val apiSecret = apiSecretInput.text.toString().trim()
            prefs
                .edit()
                .putString(ExampleApplication.PREF_API_KEY, apiKey)
                .putString(ExampleApplication.PREF_API_SECRET, apiSecret)
                .apply()
            observedIdentifiers.clear()
            MParticle.switchWorkspace(ExampleApplication.buildOptions(applicationContext, apiKey, apiSecret))
            Toast.makeText(this, R.string.workspace_switched, Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                // switchWorkspace re-creates the SDK instance asynchronously; re-attach
                // the attribute listener once the new instance is up.
                delay(WORKSPACE_SWITCH_LISTENER_DELAY_MS)
                ExampleApplication.registerAttributeListener()
            }
        }
    }

    private fun observeEvents(identifier: String) {
        if (!observedIdentifiers.add(identifier)) {
            return
        }
        lifecycleScope.launch {
            MParticleRokt.Rokt().events(identifier).collect { event ->
                Log.d(TAG, "RoktEvent[$identifier]: $event")
            }
        }
    }

    private fun placementAttributes(): Map<String, String> = mapOf(
        "email" to ExampleApplication.TEST_EMAIL,
        "firstname" to "Jenny",
        "lastname" to "Smith",
    )

    companion object {
        private const val TAG = "RoktKitExample"
        private const val WORKSPACE_SWITCH_LISTENER_DELAY_MS = 3000L
        private const val OVERLAY_PLACEMENT_IDENTIFIER = "REPLACE_WITH_OVERLAY_VIEW_NAME"
        private const val BOTTOM_SHEET_PLACEMENT_IDENTIFIER = "REPLACE_WITH_BOTTOM_SHEET_VIEW_NAME"
        private const val EMBEDDED_PLACEMENT_IDENTIFIER = "REPLACE_WITH_EMBEDDED_VIEW_NAME"
        private const val EMBEDDED_PLACEHOLDER_NAME = "Location1"
    }
}
