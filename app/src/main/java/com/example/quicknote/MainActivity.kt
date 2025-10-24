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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.quicknote.ui.theme.QuickNoteTheme
import com.example.quicknote.ui.theme.ThemePreference


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        setContent {
            // Collect the saved theme preference (SYSTEM / LIGHT / DARK)
            val themePref by VaultPreferences
                .getThemePreference(context)
                .collectAsState(initial = com.example.quicknote.ui.theme.ThemePreference.SYSTEM)

            QuickNoteTheme(themePreference = themePref) {
                QuickNoteNavApp()
            }
        }
    }
}


// ───────────────────────────────────────────────────────────────
// Helpers
// ───────────────────────────────────────────────────────────────

suspend fun createNoteInVault(
    context: Context,
    vaultUriString: String,
    body: String,
    subfolder: String?,
    title: String,
    filename: String
): Boolean {
    return try {
        val vaultUri = vaultUriString.toUri()
        var folder = DocumentFile.fromTreeUri(context, vaultUri)
        if (folder == null || !folder.canWrite()) return false

        // Handle subfolder + defaultSubfolder chaining
        val combinedPath = buildString {
            if (!subfolder.isNullOrBlank()) {
                if (subfolder.startsWith("\\")) append(subfolder.drop(1))
                else append(subfolder)
            }
        }

        if (combinedPath.isNotBlank()) {
            combinedPath.split("\\", "/", "›").filter { it.isNotBlank() }.forEach { part ->
                folder = folder?.findFile(part) ?: folder?.createDirectory(part)
            }
        }



        val newFile = folder?.createFile("text/markdown", filename)
        if (newFile != null) {
            context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->
                val template = VaultPreferences.getFrontmatterTemplate(context).first()
                val front = template?.takeIf { it.isNotBlank() } ?: ""
                val renderedFrontmatter = if (front.isNotEmpty()) {
                    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    front.replace("{{date}}", LocalDate.now().format(dateFmt))
                        .replace("{{time}}", LocalTime.now().format(timeFmt))
                        .replace("{{datetime}}", LocalDateTime.now().format(dateTimeFmt))
                        .replace("{{uuid}}", UUID.randomUUID().toString())
                        .replace("{{title}}", title)
                } else ""

                val text = buildString {
                    if (renderedFrontmatter.isNotEmpty()) {
                        appendLine(renderedFrontmatter)
                        appendLine()
                    }
                    appendLine(body.ifBlank { "Hello from QuickNote! ✨" })
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


fun renderFilename(template: String): String {
    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFmt = DateTimeFormatter.ofPattern("HHmmss")
    val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

    return template
        .replace("{{date}}", LocalDate.now().format(dateFmt))
        .replace("{{time}}", LocalTime.now().format(timeFmt))
        .replace("{{datetime}}", LocalDateTime.now().format(dateTimeFmt))
        .replace("{{uuid}}", UUID.randomUUID().toString())
        .replace("{{title}}", "")
        .replace(Regex("[^A-Za-z0-9_\\-]"), "_") // clean for filenames
}

private fun applyPlaceholders(
    template: String,
    title: String,
    forDisplay: Boolean
): String {
    val now = LocalDateTime.now()
    val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val timeDisplay = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    val timeFile    = now.format(DateTimeFormatter.ofPattern("HHmmss"))
    val dtDisplay   = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val dtFile      = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    return template
        .replace("{{date}}", date)
        .replace("{{time}}", if (forDisplay) timeDisplay else timeFile)
        .replace("{{datetime}}", if (forDisplay) dtDisplay else dtFile)
        .replace("{{uuid}}", UUID.randomUUID().toString())
        .replace("{{title}}", title)
}

private fun ensureMd(name: String) =
    if (name.lowercase().endsWith(".md")) name else "$name.md"

private fun sanitizeForFile(name: String): String {
    // Allow spaces; just strip disallowed or risky filesystem characters
    val cleaned = name.replace(Regex("""[\\/:*?"<>|]"""), "-").trim()

    // Avoid blank or dot-only filenames
    return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") "Note.md" else cleaned
}


fun displayFilename(template: String, title: String): String =
    ensureMd(applyPlaceholders(template, title, forDisplay = true))

fun fileSafeFilename(template: String, title: String): String =
    sanitizeForFile(ensureMd(applyPlaceholders(template, title, forDisplay = false)))

@Composable
fun DropdownMenuBox(
    current: com.example.quicknote.ui.theme.ThemePreference,
    onChange: (com.example.quicknote.ui.theme.ThemePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(current.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            com.example.quicknote.ui.theme.ThemePreference.values().forEach { pref ->
                DropdownMenuItem(
                    text = { Text(pref.label) },
                    onClick = {
                        onChange(pref)
                        expanded = false
                    }
                )
            }
        }
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
    var defaultSubfolder by remember { mutableStateOf("") }
    var filenameTemplate by remember { mutableStateOf("") }

    // ── Observe saved values ───────────────────────────────────────
    LaunchedEffect(Unit) {
        VaultPreferences.getVaultUri(context).collect { folderUri = it }
    }
    LaunchedEffect(Unit) {
        VaultPreferences.getFrontmatterTemplate(context).collect {
            frontmatter = it ?: defaultFrontmatter()
        }
    }
    LaunchedEffect(Unit) {
        VaultPreferences.getDefaultSubfolder(context).collect {
            defaultSubfolder = it ?: ""
        }
    }
    LaunchedEffect(Unit) {
        VaultPreferences.getDefaultFilenameTemplate(context).collect {
            filenameTemplate = it ?: "QuickNote-{{datetime}}"
        }
    }

    // ── Folder picker launcher ─────────────────────────────────────
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

    // ── UI ─────────────────────────────────────────────────────────
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
            // ─── Folder picker ───────────────────────────────────────
            Text("Select your Obsidian vault folder:")
            Button(onClick = { openFolderLauncher.launch(null) }) {
                Text("📂 Choose Folder")
            }
            folderUri?.let { Text("Current folder:\n$it") } ?: Text("No folder selected yet")

            Divider(modifier = Modifier.padding(vertical = 16.dp))

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

            // ─── Filename template ───────────────────────────────────
            Text("Default filename template:")
            OutlinedTextField(
                value = filenameTemplate,
                onValueChange = {
                    filenameTemplate = it
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, it) }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("QuickNote-{{datetime}} or {{title}}") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    filenameTemplate = ""
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, null) }
                }) { Text("🧹 Clear") }

                Button(onClick = {
                    val def = "QuickNote-{{datetime}}"
                    filenameTemplate = def
                    scope.launch { VaultPreferences.setDefaultFilenameTemplate(context, def) }
                }) { Text("🔄 Reset to Default") }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // ─── Behaviour ─────────────────────────────────────────────
            var autoClear by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                VaultPreferences.getAutoClear(context).collect { autoClear = it }
            }

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

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // ─── Theme Mode ─────────────────────────────────────────────


            var themePref by remember { mutableStateOf(ThemePreference.SYSTEM) }
            LaunchedEffect(Unit) {
                VaultPreferences.getThemePreference(context).collect { themePref = it }
            }

            Text("App theme:")
            DropdownMenuBox(
                current = themePref,
                onChange = { newPref ->
                    themePref = newPref
                    scope.launch { VaultPreferences.setThemePreference(context, newPref) }
                }
            )



            // ─── Front-matter editor (as before) ─────────────────────
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
                }) { Text("🧹 Clear") }

                Button(onClick = {
                    val def = defaultFrontmatter()
                    frontmatter = def
                    scope.launch { VaultPreferences.setFrontmatterTemplate(context, def) }
                }) { Text("🔄 Reset to Default") }
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
