package com.rokt.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mparticle.MParticle
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.Runnable
import java.lang.ref.WeakReference

class XmlExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_example)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val attributes = mapOf(
            Pair("lastname", "Smith"),
            Pair("mobile", "1112223333"),
            Pair("country", "USA"),
            Pair("noFunctional", "true"),
            Pair("email", "testEmail_${System.currentTimeMillis()}@example.com"),
            Pair("sandbox", "true"),
        )
        findViewById<Button>(R.id.executeButton).setOnClickListener {
            Log.d("ExampleActivity", "Button clicked")
            MParticle.getInstance()?.Rokt()?.selectPlacements(
                "mauitest",
                attributes,
                Runnable {  },
                Runnable {  },
                Runnable {  },
                Runnable {  },
                mapOf(
                    /*Pair(
                        "Location1", WeakReference(findViewById<Widget>(R.id.widget1))
                    ),*/
                    Pair(
                        "Location1", WeakReference(findViewById<RoktEmbeddedView>(R.id.widget1))
                    ),
                ),
                mapOf()
            )
            /*Rokt.execute(
                "mauitest", attributes, placeholders = mapOf(
                    *//*Pair(
                        "Location1", WeakReference(findViewById<Widget>(R.id.widget1))
                    ),*//*
                    Pair(
                        "Location1", WeakReference(findViewById<RoktEmbeddedView>(R.id.widget1))
                    ),
                )
            )*/
        }
    }
}