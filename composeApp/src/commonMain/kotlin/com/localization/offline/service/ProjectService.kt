@file:OptIn(ExperimentalSerializationApi::class)

package com.localization.offline.service

import androidx.compose.ui.util.fastForEach
import androidx.room.Transaction
import com.localization.offline.db.CustomFormatSpecifierEntity
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.model.KnownProject
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
                removeProjectFromKnownProjectsIfDoesntExist(folder)
                return false
            }
            addProjectToKnownProjectsIfDoesntExist(folder)
            DatabaseAccess.init(folder)
            LocalDataService.projectPath = folder.absolutePath
            return true
        }
        removeProjectFromKnownProjectsIfDoesntExist(folder)
        return false
    }

    fun switchProject(knownProject: KnownProject): Boolean {
        val folder = File(knownProject.path)
        if (DatabaseAccess.exists(folder)) {
            if (getProjectFile(folder) == null) {
                removeProjectFromKnownProjectsIfDoesntExist(folder)
                return false
            }
            DatabaseAccess.deInit()
            DatabaseAccess.init(folder)
            LocalDataService.projectPath = folder.absolutePath
            return true
        }
        removeProjectFromKnownProjectsIfDoesntExist(folder)
        return false
    }

    fun getKnownProjects(): List<KnownProject> {
        val paths = LocalDataService.knownProjectsPaths ?: mutableListOf()
        val removedPaths = mutableListOf<String>()
        val knownProjects = mutableListOf<KnownProject>()
        paths.fastForEach {
            val project = getProject(File(it))
            if (project == null) {
                removedPaths.add(it)
            } else {
                knownProjects.add(KnownProject(project.id, project.name, it))
            }
        }
        if (removedPaths.isNotEmpty()) {
            LocalDataService.knownProjectsPaths = paths
        }
        return knownProjects
    }

    private fun getProject(folder: File): Project? {
        val projectFile = getProjectFile(folder)
        return if (projectFile == null) {
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

    fun getCurrentProject(): Project? {
        return LocalDataService.projectPath?.let {
            getProject(File(it))
        }
    }

    private fun getProjectFile(folder: File): File? {
        val files = folder.listFiles { _, s -> s == projectFileName }
        if (files == null || files.isEmpty()) {
            return null
        }
        return files[0]
    }

    private fun addProjectToKnownProjectsIfDoesntExist(projectFolder: File) {
        val paths = LocalDataService.knownProjectsPaths ?: mutableListOf()
        if (!paths.contains(projectFolder.absolutePath)) {
            paths.add(projectFolder.absolutePath)
            LocalDataService.knownProjectsPaths = paths
        }
    }

    private fun removeProjectFromKnownProjectsIfDoesntExist(projectFolder: File) {
        val paths = LocalDataService.knownProjectsPaths ?: mutableListOf()
        paths.remove(projectFolder.absolutePath)
        LocalDataService.knownProjectsPaths = paths
    }

    suspend fun createProject(name: String, path: String, platforms: List<PlatformEntity>, languages: List<LanguageEntity>, languageExportSettings: List<LanguageExportSettingsEntity>, customFormatSpecifiers: List<CustomFormatSpecifierEntity>): Boolean {
        val project = Project(UUID.randomUUID().toString(), name)
        val projectFile = File(path, projectFileName)
        try {
            projectFile.outputStream().use {
                Json.encodeToStream<Project>(project, it)
            }
            val folder = File(path)
            DatabaseAccess.init(folder)
            addProjectToDatabase(platforms, languages, languageExportSettings, customFormatSpecifiers)
            addProjectToKnownProjectsIfDoesntExist(folder)
            LocalDataService.projectPath = folder.absolutePath
            return true
        } catch (e: Exception) {
            println(e)
            projectFile.delete()
            DatabaseAccess.deInit()
            return false
        }
    }

    @Transaction
    private suspend fun addProjectToDatabase(platforms: List<PlatformEntity>, languages: List<LanguageEntity>, languageExportSettings: List<LanguageExportSettingsEntity>, customFormatSpecifiers: List<CustomFormatSpecifierEntity>) {
        DatabaseAccess.platformDao!!.insert(platforms)
        DatabaseAccess.languageDao!!.insert(languages)
        DatabaseAccess.languageExportSettingsDao!!.insert(languageExportSettings)
        if (customFormatSpecifiers.isNotEmpty()) {
            DatabaseAccess.customFormatSpecifierDao!!.insert(customFormatSpecifiers)
        }
    }

    fun closeCurrentProject() {
        DatabaseAccess.deInit()
        LocalDataService.projectPath = null
    }
}