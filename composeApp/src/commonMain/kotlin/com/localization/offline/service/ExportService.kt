@file:OptIn(ExperimentalSerializationApi::class)

package com.localization.offline.service

import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.model.EmptyException
import com.localization.offline.model.EmptyTranslationExport
import com.localization.offline.model.ExportToTranslator
import com.localization.offline.model.FileStructureBuilderFactory
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.model.FormatSpecifierFormatterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolutePathString

class ExportService {
    suspend fun exportAsZip(platforms: List<PlatformEntity>) {
        getFormattedFiles(platforms, {
            File(Files.createTempDirectory(it.name).absolutePathString())
        }, { platform, exportFolder, les, fileBody ->
            val tempLanguageFolder = File(exportFolder, "${platform.exportPrefix}${les.folderSuffix}")
            if (tempLanguageFolder.mkdirs()) {
                val file = File(tempLanguageFolder, "${les.fileName}${platform.fileStructure.fileExtension}")
                file.outputStream().use {
                    it.write(fileBody.encodeToByteArray())
                }
            }
        }, { platform, exportFolder ->
            val zipFile = File(platform.exportToPath, platform.name + ".zip")
            zipFile.outputStream().use { tpfos ->
                ZipOutputStream(tpfos).use { zos ->
                    Files.walkFileTree(exportFolder.toPath(), object: SimpleFileVisitor<Path>() {
                        override fun visitFile(
                            file: Path,
                            attrs: BasicFileAttributes
                        ): FileVisitResult {
                            val name = exportFolder.toPath().relativize(file).toString()
                            zos.putNextEntry(ZipEntry(name))
                            Files.copy(file, zos)
                            zos.closeEntry()
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
            }
            File(exportFolder.absolutePath).deleteRecursively()
        })
    }

    suspend fun exportAndOverwrite(platforms: List<PlatformEntity>) {
        getFormattedFiles(platforms, {
            File(it.exportToPath)
        }, { platform, exportFolder, les, fileBody ->
            val languageFolder = File(exportFolder, "${platform.exportPrefix}${les.folderSuffix}")
            if (!languageFolder.exists()) {
                languageFolder.mkdirs()
            }
            val file = File(languageFolder, "${les.fileName}${platform.fileStructure.fileExtension}")
            if (!file.exists()) {
                file.createNewFile()
            }
            FileWriter(file, false).use {
                it.write(fileBody)
            }
        }, {_, _ ->})
    }

    private suspend inline fun getFormattedFiles(
        platforms: List<PlatformEntity>,
        crossinline platformExportFolder: (PlatformEntity) -> File,
        crossinline onFormattedFile: (PlatformEntity, File, LanguageExportSettingsEntity, fileBody: String) -> Unit,
        crossinline onFinishedPlatform: (PlatformEntity, File) -> Unit,
    ) {
        val less = DatabaseAccess.languageExportSettingsDao!!.getAll()
        platforms.fastForEach { platform ->
            val builder = FileStructureBuilderFactory.getBy(platform.fileStructure)
            val cfss = platform.formatSpecifier.takeIf { it == FormatSpecifier.Custom }?.let {
                DatabaseAccess.customFormatSpecifierDao!!.getAll(platform.id)
            }
            val platformExportFolder = platformExportFolder(platform)
            less[platform.id]!!.fastForEach { les ->
                val languageId = les.languageId
                val fsf = FormatSpecifierFormatterFactory.getBy(when(platform.formatSpecifier) {
                    FormatSpecifier.Java, FormatSpecifier.AppleEcosystem,
                    FormatSpecifier.I18n, FormatSpecifier.None -> FormatSpecifierFormatterFactory.Argument.Empty(platform.formatSpecifier)
                    FormatSpecifier.Custom -> FormatSpecifierFormatterFactory.Argument.Custom(cfss!!)
                })
                val keyValues = DatabaseAccess.translationDao!!.getAllKeyValue(platform.id, languageId).fastMap {
                    Pair(it.key, fsf?.run { format(it.value) } ?: it.value)
                }.toMutableList()
                if (platform.emptyTranslationExport == EmptyTranslationExport.BaseLanguage) {
                    keyValues.addAll(DatabaseAccess.translationDao!!.getAllKeysWithNoValue(platform.id, languageId).fastMap {
                        val base = DatabaseAccess.translationDao!!.getBaseLanguage(it.id) ?: ""
                        Pair(it.key, fsf?.run { format(base) } ?: base)
                    })
                }
                onFormattedFile(platform, platformExportFolder, les, builder.build(keyValues.sortedBy { it.first }))
            }
            onFinishedPlatform(platform, platformExportFolder)
        }
    }

    /**
     * Returns the created file
     *
     * @throws [EmptyException] if the there are no keys to export.
     */
    suspend fun exportToTranslator(languages: List<ExportToTranslator.Language>, exportOnlyUntranslatedKeys: Boolean, exportFolder: File): File {
        val keyValues = DatabaseAccess.translationDao!!.getAllKeyWithValues(languages.fastMap { it.id })
            .mapNotNull {
                if (exportOnlyUntranslatedKeys) {
                    val values = it.value
                    val isTranslatedKey = languages.fastFilter { !it.readOnly }.fastAll { language ->
                        values.fastAny { it.languageId == language.id }
                    }
                    if (isTranslatedKey) {
                        null
                    } else {
                        ExportToTranslator.KeyValues(it.key.id, it.key.key, it.key.description, it.value.fastMap { ExportToTranslator.KeyValues.Value(it.languageId, it.value) })
                    }
                } else {
                    ExportToTranslator.KeyValues(it.key.id, it.key.key, it.key.description, it.value.fastMap { ExportToTranslator.KeyValues.Value(it.languageId, it.value) })
                }
            }
        if (keyValues.isEmpty()) {
            throw EmptyException()
        }
        val ett = ExportToTranslator(languages, keyValues)
        val languagesNames = languages.fastJoinToString(",", "[", "]") { it.name }
        val projectName = ProjectService().getCurrentProject()?.name ?: ""
        val jsonFile = File(exportFolder, "${projectName}_translations$languagesNames.json")
        jsonFile.outputStream().use {
            Json.encodeToStream(ett, it)
        }
        return jsonFile
    }
}