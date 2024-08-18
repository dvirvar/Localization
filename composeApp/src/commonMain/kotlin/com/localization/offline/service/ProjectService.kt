package com.localization.offline.service

import com.localization.offline.db.CustomFormatSpecifierEntity
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.model.Project
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.util.UUID

class ProjectService {
    companion object {
        private const val projectFileName = "project.json"
    }

    fun openProject(folder: File): Boolean {
        if (DatabaseAccess.exists(folder)) {
            if (getProjectFile(folder) == null) {
                return false
            }
            addProjectToKnownProject(folder)
            DatabaseAccess.init(folder)
            LocalDataService.projectPath = folder.absolutePath
            return true
        }
        return false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getKnownProjects(): List<Project> {
        val paths = LocalDataService.knownProjectsPaths ?: mutableListOf()
        return paths.mapNotNull {
            val projectFile = getProjectFile(File(it))
            if (projectFile == null) {
                null
            } else {
                try {
                    projectFile.inputStream().use { stream ->
                        Json.decodeFromStream<Project>(stream)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun getProjectFile(folder: File): File? {
        val files = folder.listFiles { _, s -> s == projectFileName }
        if (files == null || files.isEmpty()) {
            return null
        }
        return files[0]
    }

    private fun addProjectToKnownProject(projectFolder: File) {
        val paths = LocalDataService.knownProjectsPaths ?: mutableListOf()
        paths.add(projectFolder.absolutePath)
        LocalDataService.knownProjectsPaths = paths
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun createProject(name: String, path: String, platforms: List<PlatformEntity>, languages: List<LanguageEntity>, languageExportSettings: List<LanguageExportSettingsEntity>, customFormatSpecifiers: List<CustomFormatSpecifierEntity>): Boolean {
        val project = Project(UUID.randomUUID().toString(), name)
        val projectFile = File(path, projectFileName)
        try {
            projectFile.outputStream().use {
                Json.encodeToStream<Project>(project, it)
            }
            val folder = File(path)
            DatabaseAccess.init(folder)
            DatabaseAccess.platformDao.insert(platforms)
            DatabaseAccess.languageDao.insert(languages)
            DatabaseAccess.languageExportSettingsDao.insert(languageExportSettings)
            if (customFormatSpecifiers.isNotEmpty()) {
                DatabaseAccess.customFormatSpecifierDao.insert(customFormatSpecifiers)
            }
            addProjectToKnownProject(folder)
            LocalDataService.projectPath = folder.absolutePath
            return true
        } catch (e: Exception) {
            print(e)
            projectFile.delete()
            DatabaseAccess.deInit()
            return false
        }
    }
}