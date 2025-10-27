package com.symph0nic.quicknote.utils

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.symph0nic.quicknote.data.VaultPreferences
import com.symph0nic.quicknote.ui.theme.ThemePreference
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID


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
    current: ThemePreference,
    onChange: (ThemePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(current.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemePreference.values().forEach { pref ->
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


// small default to populate first launch
fun defaultFrontmatter(): String = """
---
created: {{date}}
source: QuickNoteApp
---
""".trimIndent()

