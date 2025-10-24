package com.jonmechan.quicknote.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jonmechan.quicknote.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME   // ✅ dynamically pulled
    val buildDate = BuildConfig.BUILD_DATE
    val buildTime = BuildConfig.BUILD_TIME

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About QuickNote") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "QuickNote for Obsidian",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Version $versionName (built $buildDate ${buildTime.substring(11,16)})",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "A lightweight Android companion for your Obsidian vault. "
                        + "Quickly capture markdown notes directly into your vault folder with support for templates, subfolders, and front-matter.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/symph0nic/QuickNote"))
                context.startActivity(intent)
            }) {
                Text("🌐 View on GitHub")
            }

            Text(
                "Created by Jon Mechan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
