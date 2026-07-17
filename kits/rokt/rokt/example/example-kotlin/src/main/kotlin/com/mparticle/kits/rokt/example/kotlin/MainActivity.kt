package com.mparticle.kits.rokt.example.kotlin

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mparticle.kits.MParticleRokt
import com.mparticle.kits.RoktEmbeddedView
import com.mparticle.kits.RoktLayoutDimensionCallBack
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private val observedIdentifiers = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        private const val OVERLAY_PLACEMENT_IDENTIFIER = "REPLACE_WITH_OVERLAY_VIEW_NAME"
        private const val EMBEDDED_PLACEMENT_IDENTIFIER = "REPLACE_WITH_EMBEDDED_VIEW_NAME"
        private const val EMBEDDED_PLACEHOLDER_NAME = "Location1"
    }
}
