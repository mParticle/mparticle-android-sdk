package com.rokt.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rokt.example.ui.theme.MparticleandroidsdkTheme
import com.rokt.roktsdk.RoktLayout

class ComposeExampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attributes = mapOf(
            Pair("lastname", "Smith"),
            Pair("mobile", "1112223333"),
            Pair("country", "USA"),
            Pair("noFunctional", "true"),
            Pair("email", "testEmail_${System.currentTimeMillis()}@example.com"),
            Pair("sandbox", "true"),
        )
        enableEdgeToEdge()
        setContent {
            MparticleandroidsdkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )

                    RoktLayout(
                        true,
                        "mauitest",
                        attributes = attributes,
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MparticleandroidsdkTheme {
        Greeting("Android")
    }
}