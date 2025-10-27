package com.symph0nic.quicknote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.symph0nic.quicknote.utils.createNoteInVault
import com.symph0nic.quicknote.data.VaultPreferences
import com.symph0nic.quicknote.utils.displayFilename
import com.symph0nic.quicknote.utils.fileSafeFilename
import kotlinx.coroutines.launch

// ───────────────────────────────────────────────────────────────
// HOME  (your main Quick Note screen)
// ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var vaultUri by remember { mutableStateOf<String?>(null) }
    var defaultSubfolder by remember { mutableStateOf("") }
    var defaultFilenameTemplate by remember { mutableStateOf("QuickNote-{{datetime}}") }
    var autoClear by remember { mutableStateOf(true) }

    var subfolder by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Load preferences
    LaunchedEffect(Unit) { VaultPreferences.getVaultUri(context).collect { vaultUri = it } }
    LaunchedEffect(Unit) { VaultPreferences.getDefaultSubfolder(context).collect { defaultSubfolder = it ?: "" } }
    LaunchedEffect(Unit) { VaultPreferences.getDefaultFilenameTemplate(context).collect { defaultFilenameTemplate = it ?: "QuickNote-{{datetime}}" } }
    LaunchedEffect(Unit) { VaultPreferences.getAutoClear(context).collect { autoClear = it } }

    // Live filename display
    val displayFilename by remember {
        derivedStateOf { displayFilename(defaultFilenameTemplate, title.ifBlank { "" }) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Note", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Folder display
            Row(verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (defaultSubfolder.isBlank()) "(root)\\" else "\\${defaultSubfolder}\\"
                Text(prefix, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = subfolder,
                    onValueChange = { subfolder = it },
                    label = { Text("Subfolder") },
                    placeholder = { Text("Add nested folder (optional)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Filename + Title
            Column {
                Text(
                    "Filename: $displayFilename",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note title") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            // Note content
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Note content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 10
            )

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        if (vaultUri == null) {
                            snackbarHostState.showSnackbar("⚠️ No vault folder set. Go to Settings first.")
                            return@launch
                        }

                        val computedFilename = fileSafeFilename(defaultFilenameTemplate, title)
                        val titleForTemplate = title.ifBlank { computedFilename.substringBeforeLast(".") }

                        val success = createNoteInVault(
                            context = context,
                            vaultUriString = vaultUri!!,
                            body = body,
                            subfolder = subfolder.ifBlank { defaultSubfolder },
                            title = titleForTemplate,
                            filename = computedFilename
                        )

                        snackbarHostState.showSnackbar(
                            if (success) "✅ Note saved: $computedFilename"
                            else "❌ Failed to save note."
                        )

                        if (success && autoClear) {
                            body = ""
                            title = ""
                            subfolder = ""
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("📝 Create Quick Note", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

