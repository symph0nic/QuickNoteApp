package com.example.quicknote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.vaultDataStore by preferencesDataStore(name = "vault_prefs")

object VaultPreferences {
    private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
    private val FRONTMATTER_KEY = stringPreferencesKey("frontmatter_template")

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
}
