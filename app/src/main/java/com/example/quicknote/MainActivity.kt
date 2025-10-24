package com.example.quicknote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickNoteApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteApp() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Note") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            var message by remember { mutableStateOf("Tap the button!") }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = message)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { message = "Pretend we just created a note ✨" }
                ) {
                    Text("📝 Create Quick Note")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickNoteApp() {
    QuickNoteApp()
}
