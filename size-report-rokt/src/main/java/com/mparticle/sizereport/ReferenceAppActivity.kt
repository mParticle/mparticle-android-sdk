package com.mparticle.sizereport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The reference app every flavor shares: a small Compose + Material3 screen standing in for the
 * partner app the SDK is dropped into.
 *
 * It has to actually render -- theme, layout, list, state -- or R8 strips Compose out of the
 * baseline, the baseline collapses back to an empty app, and every delta silently reverts to
 * charging the SDK for Compose. `measure_size.sh` asserts the baseline stays large enough to
 * prove that has not happened.
 */
class ReferenceAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ReferenceScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceScreen() {
    var count by remember { mutableStateOf(0) }
    val items = remember { List(20) { "Item ${it + 1}" } }

    Scaffold(topBar = { TopAppBar(title = { Text("Reference app") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            Button(onClick = { count++ }) {
                Text("Tapped $count")
            }
            LazyColumn {
                items(items) { item ->
                    Card(Modifier.padding(8.dp)) {
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
