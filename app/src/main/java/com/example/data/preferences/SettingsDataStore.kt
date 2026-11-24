package com.example.data.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.data.model.DualSimMode
import com.example.data.model.FavoriteGroup
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.ThemeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "shajamal_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        val SELECTED_DOODLE_ID = stringPreferencesKey("selected_doodle_id")
        val CUSTOM_THEME_IMAGE_PATH = stringPreferencesKey("custom_theme_image_path")
        val OVERLAY_DIM_ALPHA = floatPreferencesKey("overlay_dim_alpha")
        val DTMF_TONES_ENABLED = booleanPreferencesKey("dtmf_tones_enabled")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val DUAL_SIM_MODE = stringPreferencesKey("dual_sim_mode")
        val AUTO_CLEAN_PASTED_NUMBER = booleanPreferencesKey("auto_clean_pasted_number")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val BLOCKED_NUMBERS = stringSetPreferencesKey("blocked_numbers")
        val NUMBER_NOTES = stringSetPreferencesKey("number_notes")
        val NUMBER_GROUPS = stringSetPreferencesKey("number_groups")
        val SIM1_RINGTONE_URI = stringPreferencesKey("sim1_ringtone_uri")
        val SIM1_RINGTONE_NAME = stringPreferencesKey("sim1_ringtone_name")
        val SIM2_RINGTONE_URI = stringPreferencesKey("sim2_ringtone_uri")
        val SIM2_RINGTONE_NAME = stringPreferencesKey("sim2_ringtone_name")
    }

    val settingsFlow: Flow<DialerSettings> = context.settingsDataStore.data.map { prefs ->
        val modeName = prefs[Keys.DUAL_SIM_MODE] ?: DualSimMode.ASK_ON_CALL.name
        val dualSimMode = runCatching { DualSimMode.valueOf(modeName) }
            .getOrDefault(DualSimMode.ASK_ON_CALL)

        val langName = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.ENGLISH.name
        val language = runCatching { AppLanguage.valueOf(langName) }
            .getOrDefault(AppLanguage.ENGLISH)

        DialerSettings(
            selectedThemeId = prefs[Keys.SELECTED_THEME_ID] ?: ThemeCatalog.ID_PURE_WHITE,
            selectedDoodle = DoodleDesign.fromId(prefs[Keys.SELECTED_DOODLE_ID]),
            customThemeImagePath = prefs[Keys.CUSTOM_THEME_IMAGE_PATH],
            overlayDimAlpha = (prefs[Keys.OVERLAY_DIM_ALPHA] ?: 0.52f).coerceIn(0.15f, 0.88f),
            dtmfTonesEnabled = prefs[Keys.DTMF_TONES_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            dualSimMode = dualSimMode,
            autoCleanPastedNumber = prefs[Keys.AUTO_CLEAN_PASTED_NUMBER] ?: true,
            language = language,
            sim1RingtoneUri = prefs[Keys.SIM1_RINGTONE_URI],
            sim1RingtoneName = prefs[Keys.SIM1_RINGTONE_NAME],
            sim2RingtoneUri = prefs[Keys.SIM2_RINGTONE_URI],
            sim2RingtoneName = prefs[Keys.SIM2_RINGTONE_NAME]
        )
    }

    val blockedNumbersFlow: Flow<List<BlockedNumberEntry>> = context.settingsDataStore.data.map { prefs ->
        val rawSet = prefs[Keys.BLOCKED_NUMBERS].orEmpty()
        rawSet.mapNotNull { encoded ->
            val parts = encoded.split("|", limit = 3)
            val number = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val label = parts.getOrNull(1).orEmpty()
            val addedAt = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            BlockedNumberEntry(number = number, label = label, addedAtMillis = addedAt)
        }.sortedByDescending { it.addedAtMillis }
    }

    val numberNotesFlow: Flow<Map<String, String>> = context.settingsDataStore.data.map { prefs ->
        val rawSet = prefs[Keys.NUMBER_NOTES].orEmpty()
        buildMap {
            rawSet.forEach { encoded ->
                val idx = encoded.indexOf('|')
                if (idx > 0 && idx < encoded.length - 1) {
                    val key = encoded.substring(0, idx)
                    val note = encoded.substring(idx + 1)
                    if (note.isNotBlank()) put(key, note)
                }
            }
        }
    }

    val numberGroupsFlow: Flow<Map<String, FavoriteGroup>> = context.settingsDataStore.data.map { prefs ->
        val rawSet = prefs[Keys.NUMBER_GROUPS].orEmpty()
        buildMap {
            rawSet.forEach { encoded ->
                val idx = encoded.indexOf('|')
                if (idx > 0 && idx < encoded.length - 1) {
                    val key = encoded.substring(0, idx)
                    val group = FavoriteGroup.fromId(encoded.substring(idx + 1))
                    if (group != FavoriteGroup.NONE) put(key, group)
                }
            }
        }
    }

    suspend fun selectTheme(themeId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SELECTED_THEME_ID] = themeId
        }
    }

    suspend fun selectDoodle(doodle: DoodleDesign) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SELECTED_DOODLE_ID] = doodle.id
        }
    }

    suspend fun saveCustomPhotoTheme(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, "custom_theme_${System.currentTimeMillis()}.jpg")
        runCatching {
            context.filesDir.listFiles { file ->
                file.name.startsWith("custom_theme_") && file.name.endsWith(".jpg")
            }?.forEach { old -> old.delete() }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            context.settingsDataStore.edit { prefs ->
                prefs[Keys.CUSTOM_THEME_IMAGE_PATH] = targetFile.absolutePath
                prefs[Keys.SELECTED_THEME_ID] = ThemeCatalog.ID_CUSTOM_PHOTO
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    suspend fun setSimRingtone(slotIndex: Int, uri: Uri?): String? = withContext(Dispatchers.IO) {
        if (uri == null) {
            clearSimRingtone(slotIndex)
            return@withContext null
        }

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val displayName = resolveAudioDisplayName(uri)
        context.settingsDataStore.edit { prefs ->
            if (slotIndex == 0) {
                prefs[Keys.SIM1_RINGTONE_URI] = uri.toString()
                prefs[Keys.SIM1_RINGTONE_NAME] = displayName
            } else {
                prefs[Keys.SIM2_RINGTONE_URI] = uri.toString()
                prefs[Keys.SIM2_RINGTONE_NAME] = displayName
            }
        }
        displayName
    }

    suspend fun clearSimRingtone(slotIndex: Int) {
        context.settingsDataStore.edit { prefs ->
            if (slotIndex == 0) {
                prefs.remove(Keys.SIM1_RINGTONE_URI)
                prefs.remove(Keys.SIM1_RINGTONE_NAME)
            } else {
                prefs.remove(Keys.SIM2_RINGTONE_URI)
                prefs.remove(Keys.SIM2_RINGTONE_NAME)
            }
        }
    }

    private fun resolveAudioDisplayName(uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Custom Ringtone"
    }

    suspend fun setOverlayDimAlpha(alpha: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.OVERLAY_DIM_ALPHA] = alpha.coerceIn(0.15f, 0.88f)
        }
    }

    suspend fun setDtmfTonesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DTMF_TONES_ENABLED] = enabled
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setDualSimMode(mode: DualSimMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DUAL_SIM_MODE] = mode.name
        }
    }

    suspend fun setAutoCleanPastedNumber(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_CLEAN_PASTED_NUMBER] = enabled
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }
    }

    suspend fun blockNumber(rawNumber: String, label: String = "") {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        if (canonical.isBlank()) return
        val displayNum = rawNumber.trim()
        val safeLabel = label.replace("|", " ").trim()
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_NUMBERS].orEmpty().toMutableSet()
            current.removeAll { entry ->
                val existingNum = entry.substringBefore('|')
                PhoneNumberInfoHelper.canonicalKey(existingNum) == canonical
            }
            current.add("$displayNum|$safeLabel|${System.currentTimeMillis()}")
            prefs[Keys.BLOCKED_NUMBERS] = current
        }
    }

    suspend fun unblockNumber(rawNumber: String) {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        if (canonical.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_NUMBERS].orEmpty().toMutableSet()
            current.removeAll { entry ->
                val existingNum = entry.substringBefore('|')
                PhoneNumberInfoHelper.canonicalKey(existingNum) == canonical
            }
            prefs[Keys.BLOCKED_NUMBERS] = current
        }
    }

    suspend fun isNumberBlocked(rawNumber: String): Boolean {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        if (canonical.isBlank()) return false
        val current = context.settingsDataStore.data.first()[Keys.BLOCKED_NUMBERS].orEmpty()
        return current.any { entry ->
            PhoneNumberInfoHelper.canonicalKey(entry.substringBefore('|')) == canonical
        }
    }

    suspend fun saveNumberNote(rawNumber: String, note: String) {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        if (canonical.isBlank()) return
        val cleanNote = note.replace("\n", " ").trim()
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.NUMBER_NOTES].orEmpty().toMutableSet()
            current.removeAll { it.substringBefore('|') == canonical }
            if (cleanNote.isNotEmpty()) {
                current.add("$canonical|$cleanNote")
            }
            prefs[Keys.NUMBER_NOTES] = current
        }
    }

    suspend fun setNumberGroup(rawNumber: String, group: FavoriteGroup) {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        if (canonical.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.NUMBER_GROUPS].orEmpty().toMutableSet()
            current.removeAll { it.substringBefore('|') == canonical }
            if (group != FavoriteGroup.NONE) {
                current.add("$canonical|${group.id}")
            }
            prefs[Keys.NUMBER_GROUPS] = current
        }
    }
}
