package com.localization.offline.service

import com.localization.offline.model.LanguageViewStyle
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json

object LocalDataService {
    private val settings = Settings()

    private const val projectPathKey = "projectPathKey"
    var projectPath: String?
        get() = settings.getStringOrNull(projectPathKey)
        set(value) {
            settings[projectPathKey] = value
        }

    private const val knownProjectsPathsKey = "knownProjectsPathsKey"
    var knownProjectsPaths: MutableList<String>?
        get() = get(knownProjectsPathsKey)
        set(value) = save(value, knownProjectsPathsKey)

    private const val keyColumnWidthKey = "keyColumnWidthKey"
    var keyColumnWidth: Float?
        get() = settings.getFloatOrNull(keyColumnWidthKey)
        set(value) { settings[keyColumnWidthKey] = value }

    private const val languageViewStyleKey = "languageViewStyleKey"
    var languageViewStyle: LanguageViewStyle
        get() = settings.getIntOrNull(languageViewStyleKey)?.let {
            try {
                LanguageViewStyle.entries[it]
            } catch (e: Exception) {
                null
            }
        } ?: LanguageViewStyle.List
        set(value) { settings[languageViewStyleKey] = value.ordinal }

    private inline fun<reified T> get(key: String): T? {
        return settings.get<String>(key)?.let {
            Json.decodeFromString<T>(it)
        }
    }

    private inline fun<reified T> save(value: T?, key: String) {
        settings[key] = value?.let {
            Json.encodeToString(value = it)
        }
    }
}