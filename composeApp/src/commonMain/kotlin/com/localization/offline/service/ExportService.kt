package com.localization.offline.service

import androidx.compose.ui.util.fastForEach
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.model.FileStructureBuilderFactory
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.model.FormatSpecifierFormatterFactory
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
                val keyValues = DatabaseAccess.translationDao!!.getAllKeyValue(platform.id, languageId).map {
                    Pair(it.key, fsf?.run { format(it.value) } ?: it.value)
                }
                onFormattedFile(platform, platformExportFolder, les, builder.build(keyValues.sortedBy { it.first }))
            }
            onFinishedPlatform(platform, platformExportFolder)
        }
    }
}