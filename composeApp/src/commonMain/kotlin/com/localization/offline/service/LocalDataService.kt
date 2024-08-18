package com.localization.offline.service

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalDataService {
    private val settings = Settings()

    private const val projectPathKey = "projectPathKey"
    var projectPath: String?
        get() = settings.get<String>(projectPathKey)
        set(value) {
            settings[projectPathKey] = value
        }

    private const val knownProjectsPathsKey = "knownProjectsPathsKey"
    var knownProjectsPaths: MutableList<String>?
        get() = get(knownProjectsPathsKey)
        set(value) = save(value, knownProjectsPathsKey)

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