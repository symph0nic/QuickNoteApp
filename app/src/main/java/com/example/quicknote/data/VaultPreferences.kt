package com.example.quicknote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import com.example.quicknote.ui.theme.ThemePreference
import androidx.datastore.preferences.core.emptyPreferences



// One DataStore instance for the whole app
val Context.vaultDataStore by preferencesDataStore(name = "vault_prefs")

object VaultPreferences {

    // ─── Existing keys ────────────────────────────────────────────────
    private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
    private val FRONTMATTER_KEY = stringPreferencesKey("frontmatter_template")

    // ─── New keys ────────────────────────────────────────────────────
    private val DEFAULT_SUBFOLDER_KEY = stringPreferencesKey("default_subfolder")
    private val DEFAULT_FILENAME_KEY = stringPreferencesKey("default_filename_template")
    private val THEME_KEY = stringPreferencesKey("theme_mode") // "SYSTEM" | "LIGHT" | "DARK"


    // ─── Existing helpers ────────────────────────────────────────────
    fun getVaultUri(context: Context): Flow<String?> =
        context.vaultDataStore.data.map { it[VAULT_URI_KEY] }

    suspend fun setVaultUri(context: Context, uri: String?) {
        context.vaultDataStore.edit { prefs ->
            if (uri != null) prefs[VAULT_URI_KEY] = uri else prefs.remove(VAULT_URI_KEY)
        }
    }

    fun getFrontmatterTemplate(context: Context): Flow<String?> =
        context.vaultDataStore.data.map { it[FRONTMATTER_KEY] }

    suspend fun setFrontmatterTemplate(context: Context, template: String?) {
        context.vaultDataStore.edit { prefs ->
            if (template != null) prefs[FRONTMATTER_KEY] = template else prefs.remove(FRONTMATTER_KEY)
        }
    }

    // ─── New helpers ─────────────────────────────────────────────────
    fun getDefaultSubfolder(context: Context): Flow<String?> =
        context.vaultDataStore.data.map { it[DEFAULT_SUBFOLDER_KEY] }

    suspend fun setDefaultSubfolder(context: Context, subfolder: String?) {
        context.vaultDataStore.edit { prefs ->
            if (subfolder != null) prefs[DEFAULT_SUBFOLDER_KEY] = subfolder
            else prefs.remove(DEFAULT_SUBFOLDER_KEY)
        }
    }

    fun getDefaultFilenameTemplate(context: Context): Flow<String?> =
        context.vaultDataStore.data.map { it[DEFAULT_FILENAME_KEY] }

    suspend fun setDefaultFilenameTemplate(context: Context, template: String?) {
        context.vaultDataStore.edit { prefs ->
            if (template != null) prefs[DEFAULT_FILENAME_KEY] = template
            else prefs.remove(DEFAULT_FILENAME_KEY)
        }
    }

    private val AUTO_CLEAR_KEY = booleanPreferencesKey("auto_clear_after_save")

    fun getAutoClear(context: Context): Flow<Boolean> =
        context.vaultDataStore.data.map { it[AUTO_CLEAR_KEY] ?: true }

    suspend fun setAutoClear(context: Context, enabled: Boolean) {
        context.vaultDataStore.edit { prefs -> prefs[AUTO_CLEAR_KEY] = enabled }
    }

// ─── Theme Preference ────────────────────────────────────────────────


    fun getThemePreference(context: Context): Flow<ThemePreference> =
        context.vaultDataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences())
                else throw e
            }
            .map { prefs ->
                val raw = prefs[THEME_KEY] ?: ThemePreference.SYSTEM.name
                ThemePreference.fromString(raw)
            }

    suspend fun setThemePreference(context: Context, pref: ThemePreference) {
        context.vaultDataStore.edit { prefs ->
            prefs[THEME_KEY] = pref.name
        }
    }



}
