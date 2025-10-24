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
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.filled.Info
import androidx.core.view.WindowCompat


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        setContent {
            // Create NavController once, outside the theme
            val navController = rememberNavController()

            // Observe theme preference
            val themePref by VaultPreferences
                .getThemePreference(context)
                .collectAsState(initial = com.example.quicknote.ui.theme.ThemePreference.SYSTEM)

            QuickNoteTheme(themePreference = themePref) {
                // Pass navController down
                QuickNoteNavApp(navController)
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

@Composable
fun TemplateInputSection(
    title: String,
    description: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { showHelp = true }) {
            Icon(Icons.Default.Info, contentDescription = "Help")
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        placeholder = { Text(placeholder) },
        maxLines = 8
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onReset) { Text("🔄 Reset") }
        Button(onClick = onClear) { Text("🧹 Clear") }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("OK") }
            },
            title = { Text(title) },
            text = { Text(description) }
        )
    }
}



// ───────────────────────────────────────────────────────────────
// Navigation shell
// ───────────────────────────────────────────────────────────────
@Composable
fun QuickNoteNavApp(navController: NavHostController) {
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
                        ThemePreference.values().forEach { pref ->
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
fun PreviewApp() {
    val navController = rememberNavController()
    QuickNoteNavApp(navController)
}
