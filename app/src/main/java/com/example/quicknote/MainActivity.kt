package com.example.quicknote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import android.content.Intent
import android.widget.Toast
import com.example.quicknote.data.VaultPreferences
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.material3.HorizontalDivider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.time.format.DateTimeFormatter



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuickNoteNavApp() }
    }
}

// ───────────────────────────────────────────────────────────────
// Navigation shell
// ───────────────────────────────────────────────────────────────
@Composable
fun QuickNoteNavApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}

// ───────────────────────────────────────────────────────────────
// HOME  (your main Quick Note screen)
// ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("Tap the button!") }
    var vaultUri by remember { mutableStateOf<String?>(null) }

    // Observe saved vault URI
    LaunchedEffect(Unit) {
        VaultPreferences.getVaultUri(context).collect { saved ->
            vaultUri = saved
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Note") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = message)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch {
                        if (vaultUri == null) {
                            message = "⚠️ No vault folder set. Go to Settings first."
                        } else {
                            val success = createNoteInVault(context, vaultUri!!)
                            message = if (success) "✅ Note saved!" else "❌ Failed to save note."
                        }
                    }
                }) {
                    Text("📝 Create Quick Note")
                }
            }
        }
    }
}






suspend fun createNoteInVault(context: Context, vaultUriString: String): Boolean {
    return try {
        val vaultUri = vaultUriString.toUri()
        val folder = DocumentFile.fromTreeUri(context, vaultUri)
        if (folder == null || !folder.canWrite()) return false

        val filename = "QuickNote-${LocalDateTime.now()}".replace(":", "-") + ".md"

        val newFile = folder.createFile("text/markdown", filename)
        if (newFile != null) {
            context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->


// Fetch template
                val template = VaultPreferences.getFrontmatterTemplate(context).first()
                val front = template?.takeIf { it.isNotBlank() } ?: ""

                // Create reusable formatters
                val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
                val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                val renderedFrontmatter = if (front.isNotEmpty()) {
                    front.replace("{{date}}", LocalDate.now().format(dateFmt))
                        .replace("{{time}}", LocalTime.now().format(timeFmt))
                        .replace("{{datetime}}", LocalDateTime.now().format(dateTimeFmt))
                        .replace("{{uuid}}", UUID.randomUUID().toString())
                        .replace("{{title}}", "")
                } else ""



                // === Build final Markdown content ===
                val text = buildString {
                    if (renderedFrontmatter.isNotEmpty()) {
                        appendLine(renderedFrontmatter)
                        appendLine()
                    }
                    appendLine("Hello from QuickNote! ✨")
                }.trimIndent()

                stream.write(text.toByteArray())
                return true

            }
        }
        false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}




// ───────────────────────────────────────────────────────────────
// SETTINGS
// ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderUri by remember { mutableStateOf<String?>(null) }
    var frontmatter by remember { mutableStateOf("") }

    // Observe saved values
    LaunchedEffect(Unit) {
        VaultPreferences.getVaultUri(context).collect { folderUri = it }
    }
    LaunchedEffect(Unit) {
        VaultPreferences.getFrontmatterTemplate(context).collect { template ->
            frontmatter = template ?: defaultFrontmatter()
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select your Obsidian vault folder:")
            Button(onClick = { openFolderLauncher.launch(null) }) {
                Text("📂 Choose Folder")
            }
            folderUri?.let {
                Text("Current folder:\n$it")
            } ?: Text("No folder selected yet")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            Text("Default front-matter template:")
            OutlinedTextField(
                value = frontmatter,
                onValueChange = {
                    frontmatter = it
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("---\ncreated: {{date}}\nsource: QuickNoteApp\n---") },
                maxLines = 10
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    frontmatter = ""
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, null) }
                }) {
                    Text("🧹 Clear")
                }

                Button(onClick = {
                    val def = defaultFrontmatter()
                    frontmatter = def
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, def) }
                }) {
                    Text("🔄 Reset to Default")
                }
            }
        }
    }
}

// small default to populate first launch
fun defaultFrontmatter(): String = """
---
created: {{date}}
source: QuickNoteApp
---
""".trimIndent()



@Preview(showBackground = true)
@Composable
fun PreviewApp() = QuickNoteNavApp()
