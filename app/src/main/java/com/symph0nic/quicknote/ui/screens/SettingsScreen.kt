package com.symph0nic.quicknote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavHostController
import android.content.Intent
import android.widget.Toast
import com.symph0nic.quicknote.data.VaultPreferences
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import com.symph0nic.quicknote.ui.theme.ThemePreference
import com.symph0nic.quicknote.utils.TemplateInputSection
import com.symph0nic.quicknote.utils.defaultFrontmatter


// ───────────────────────────────────────────────────────────────
// SETTINGS
// ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── State holders ───────────────────────────────────────────────
    var folderUri by remember { mutableStateOf<String?>(null) }
    var frontmatter by remember { mutableStateOf("") }
    var defaultSubfolder by remember { mutableStateOf("") }
    var filenameTemplate by remember { mutableStateOf("") }
    var themePref by remember { mutableStateOf(ThemePreference.SYSTEM) }
    var autoClear by remember { mutableStateOf(true) }

    // ── Observe saved values ────────────────────────────────────────
    LaunchedEffect(Unit) { VaultPreferences.getVaultUri(context).collect { folderUri = it } }
    LaunchedEffect(Unit) { VaultPreferences.getFrontmatterTemplate(context).collect { frontmatter = it ?: defaultFrontmatter() } }
    LaunchedEffect(Unit) { VaultPreferences.getDefaultSubfolder(context).collect { defaultSubfolder = it ?: "" } }
    LaunchedEffect(Unit) { VaultPreferences.getDefaultFilenameTemplate(context).collect { filenameTemplate = it ?: "QuickNote-{{datetime}}" } }
    LaunchedEffect(Unit) { VaultPreferences.getThemePreference(context).collect { themePref = it } }
    LaunchedEffect(Unit) { VaultPreferences.getAutoClear(context).collect { autoClear = it } }

    // ── Folder picker launcher ──────────────────────────────────────
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val uriString = uri.toString()
                scope.launch { VaultPreferences.setVaultUri(context, uriString) }
                folderUri = uriString
                Toast.makeText(context, "Vault folder saved ✅", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // ── UI ──────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ─── App theme toggle ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App theme", style = MaterialTheme.typography.bodyLarge)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(themePref.label)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ThemePreference.entries.forEach { pref ->
                            DropdownMenuItem(
                                text = { Text(pref.label) },
                                onClick = {
                                    expanded = false
                                    themePref = pref
                                    scope.launch { VaultPreferences.setThemePreference(context, pref) }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Vault folder selector (tight layout) ────────────────
            Text("Vault folder", style = MaterialTheme.typography.bodyLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { openFolderLauncher.launch(null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📂 Choose Folder")
                }
                if (folderUri != null) {
                    OutlinedButton(onClick = {
                        folderUri = null
                        scope.launch { VaultPreferences.setVaultUri(context, null) }
                    }) {
                        Text("🧹 Clear")
                    }
                }
            }
            folderUri?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } ?: Text(
                "No folder selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Default subfolder ───────────────────────────────────
            Text("Default subfolder (optional):")
            OutlinedTextField(
                value = defaultSubfolder,
                onValueChange = {
                    defaultSubfolder = it
                    scope.launch { VaultPreferences.setDefaultSubfolder(context, it) }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Daily or Inbox") }
            )

            // ─── Filename template (with helper popover) ─────────────
            TemplateInputSection(
                title = "Default filename template",
                description = "Supports {{title}}, {{date}}, {{time}}, {{datetime}}, {{uuid}}",
                value = filenameTemplate,
                placeholder = "QuickNote-{{datetime}} or {{title}}",
                onValueChange = {
                    filenameTemplate = it
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, it) }
                },
                onReset = {
                    val def = "QuickNote-{{datetime}}"
                    filenameTemplate = def
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, def) }
                },
                onClear = {
                    filenameTemplate = ""
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, null) }
                }
            )

            // ─── Front-matter template (with helper popover) ─────────
            TemplateInputSection(
                title = "Default front-matter template",
                description = "Front-matter supports placeholders like {{date}}, {{time}}, {{uuid}}. Example:\n---\ncreated: {{date}}\nsource: QuickNoteApp\n---",
                value = frontmatter,
                placeholder = "---\\ncreated: {{date}}\\nsource: QuickNoteApp\\n---",
                onValueChange = {
                    frontmatter = it
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, it) }
                },
                onReset = {
                    val def = defaultFrontmatter()
                    frontmatter = def
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, def) }
                },
                onClear = {
                    frontmatter = ""
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, null) }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Clear fields after saving (bottom) ──────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear fields after saving", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = autoClear,
                    onCheckedChange = {
                        autoClear = it
                        scope.launch { VaultPreferences.setAutoClear(context, it) }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Button(
                onClick = { navController.navigate("about") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("About QuickNote")
            }

        }
    }
}

