@file:OptIn(ExperimentalLayoutApi::class, ExperimentalSerializationApi::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMapIndexedNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.extension.tryBrowse
import com.localization.offline.extension.tryBrowseAndHighlight
import com.localization.offline.model.AppScreen
import com.localization.offline.model.EmptyException
import com.localization.offline.model.ExportToTranslator
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.model.FormatSpecifierFormatter
import com.localization.offline.model.Navigation
import com.localization.offline.service.ExportService
import com.localization.offline.service.ImportService
import com.localization.offline.service.LanguageService
import com.localization.offline.service.PlatformService
import com.localization.offline.store.ProcessingStore
import com.localization.offline.ui.view.AppCard
import com.localization.offline.ui.view.AppDialog
import com.localization.offline.ui.view.AppTooltip
import com.localization.offline.ui.view.ButtonWithLoader
import com.localization.offline.ui.view.GenericDropdown
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.all_keys_are_translated
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.choose_path
import localization.composeapp.generated.resources.dont_export_to_translator
import localization.composeapp.generated.resources.editable_for_translator
import localization.composeapp.generated.resources.export
import localization.composeapp.generated.resources.export_and_overwrite
import localization.composeapp.generated.resources.export_as_zip
import localization.composeapp.generated.resources.export_description
import localization.composeapp.generated.resources.export_only_untranslated_keys
import localization.composeapp.generated.resources.export_to_translator
import localization.composeapp.generated.resources.file_format_is_not_correct
import localization.composeapp.generated.resources.file_structure
import localization.composeapp.generated.resources.format_specifier
import localization.composeapp.generated.resources.import
import localization.composeapp.generated.resources.import_description
import localization.composeapp.generated.resources.import_from_translator
import localization.composeapp.generated.resources.not_editable_for_translator
import localization.composeapp.generated.resources.nothing_to_export
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.path
import localization.composeapp.generated.resources.select_file
import localization.composeapp.generated.resources.select_folder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import java.io.File
import java.io.IOException

class ExportImportVM: ViewModel() {
    data class ExportToTranslatorState(
        val language: LanguageEntity,
        val selected: Boolean,
        val readOnly: Boolean
    )
    private val exportService = ExportService()
    private val importService = ImportService()
    val platforms = PlatformService().getAllPlatformsAsFlow()
    val languages = LanguageService().getAllLanguagesAsFlow()
    val showImportDialog = MutableStateFlow(false)
    val showExportToTranslatorDialog = MutableStateFlow(false)
    val showExportToTranslatorEmptyErrorDialog = MutableStateFlow(false)
    val showImportForTranslatorFormatErrorDialog = MutableStateFlow(false)
    val showExportAndOverwriteLoader = ProcessingStore.exportAndOverwriteTranslations.combine(ProcessingStore.importTranslations) { eao, i ->
        eao || i
    }
    val showExportAsZipLoader = ProcessingStore.exportTranslationsAsZip.combine(ProcessingStore.importTranslations) { eaz, i ->
        eaz || i
    }
    val showImportLoader = combine(ProcessingStore.importTranslations, ProcessingStore.exportAndOverwriteTranslations, ProcessingStore.exportTranslationsAsZip) { i, eao, eaz ->
        i || eao || eaz
    }
    val navigation = MutableSharedFlow<Navigation?>()

    fun editExportToPath(platformEntity: PlatformEntity) {
        viewModelScope.launch {
            val folder = FileKit.openDirectoryPicker() ?: return@launch
            PlatformService().updatePlatformExportToPath(platformEntity.id, folder.file.absolutePath)
        }
    }

    fun exportAsZip(selectedPlatformsBooleans: List<Boolean>) {
        viewModelScope.launch {
            val selectedPlatforms = platforms.first().filterIndexed { index, _ ->
                selectedPlatformsBooleans[index]
            }
            exportService.exportAsZip(selectedPlatforms)
            try {
                val desktop = Desktop.getDesktop()
                selectedPlatforms.fastForEach {
                    desktop.tryBrowse(File(it.exportToPath))
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    fun exportAndOverwrite(selectedPlatformsBooleans: List<Boolean>) {
        viewModelScope.launch {
            val selectedPlatforms = platforms.first().filterIndexed { index, _ ->
                selectedPlatformsBooleans[index]
            }
            exportService.exportAndOverwrite(selectedPlatforms)
        }
    }

    fun exportToTranslator(exportToTranslatorState: List<ExportToTranslatorState>, exportOnlyUntranslatedKeys: Boolean, exportFolder: File) {
        viewModelScope.launch {
            val exportToTranslatorLanguages = exportToTranslatorState.filter { it.selected }.fastMap { ExportToTranslator.Language(it.language.id, it.language.name, it.readOnly) }
            try {
                val exportedFile = exportService.exportToTranslator(exportToTranslatorLanguages, exportOnlyUntranslatedKeys, exportFolder)
                Desktop.getDesktop().tryBrowseAndHighlight(exportedFile)
            } catch (e: EmptyException) {
                showExportToTranslatorEmptyErrorDialog.value = true
            } finally {
                showExportToTranslatorDialog.value = false
            }
        }
    }

    fun import(fileStructure: FileStructure, formatSpecifier: FormatSpecifier, paths: List<String>, selectedPlatforms: List<Boolean>) {
        showImportDialog.value = false
        viewModelScope.launch {
            val languagePaths = languages.first().fastMapIndexedNotNull { index, language ->
                val path = paths[index]
                if (path.isNotEmpty()) {
                    Pair(language, path)
                } else {
                    null
                }
            }
            val platforms = platforms.first().filterIndexed { index, _ ->
                selectedPlatforms[index]
            }
            importService.import(fileStructure, formatSpecifier, languagePaths, platforms)
        }
    }

    fun importFromTranslator() {
        viewModelScope.launch {
            val exportToTranslatorFile = FileKit.openFilePicker(FileKitType.File("json")) ?: return@launch
            try {
                exportToTranslatorFile.file.inputStream().use {
                    Json.decodeFromStream<ExportToTranslator>(it)
                }
                navigation.emit(Navigation(AppScreen.Translator(exportToTranslatorFile.file.absolutePath, TranslatorVM.Type.Import.name), null))
            } catch (i: IllegalArgumentException) {
                showImportForTranslatorFormatErrorDialog.value = true
            } catch (_: IOException) {}
        }
    }
}

@Composable
fun ExportImportScreen(navController: NavController) {
    val vm = koinViewModel<ExportImportVM>()
    val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
    val showImportDialog by vm.showImportDialog.collectAsStateWithLifecycle()
    val showExportToTranslatorDialog by vm.showExportToTranslatorDialog.collectAsStateWithLifecycle()
    val showExportToTranslatorEmptyErrorDialog by vm.showExportToTranslatorEmptyErrorDialog.collectAsStateWithLifecycle()
    val showImportForTranslatorFormatErrorDialog by vm.showImportForTranslatorFormatErrorDialog.collectAsStateWithLifecycle()
    val showExportAndOverwriteLoader by vm.showExportAndOverwriteLoader.collectAsStateWithLifecycle(false)
    val showExportAsZipLoader by vm.showExportAsZipLoader.collectAsStateWithLifecycle(false)
    val showImportLoader by vm.showImportLoader.collectAsStateWithLifecycle(false)
    val navigation by vm.navigation.collectAsStateWithLifecycle(null)

    LaunchedEffect(navigation) {
        if (navigation != null) {
            navController.navigate(navigation!!.screen, navigation!!.navOptions)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(36.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
        ExportSettings(platforms, showExportAndOverwriteLoader, showExportAsZipLoader, vm::editExportToPath, vm::exportAsZip, vm::exportAndOverwrite) {vm.showExportToTranslatorDialog.value = true}
        Import(
            showImportLoader,
            {vm.showImportDialog.value = true},
            vm::importFromTranslator
        )
    }

    if (showExportToTranslatorDialog) {
        val scope = rememberCoroutineScope()
        val languages by vm.languages.collectAsStateWithLifecycle(listOf())
        val exportToTranslatorState = remember(languages) {
            mutableStateOf<List<ExportImportVM.ExportToTranslatorState>>(listOf()).apply {
                value = languages.fastMapIndexed { index, language ->
                    ExportImportVM.ExportToTranslatorState(language,index == 0, index == 0)
                }
            }
        }
        var exportOnlyUntranslatedKeys by remember { mutableStateOf(true) }
        var exportFolder by remember { mutableStateOf<File?>(null) }
        val exportButtonEnabled = remember(exportToTranslatorState.value, exportFolder) {
            val state = exportToTranslatorState.value
            state.fastAny { it.selected } && state.filter { it.selected }.fastAny { !it.readOnly } && exportFolder != null
        }

        AppDialog {
            languages.fastForEachIndexed { index, language ->
                val state = exportToTranslatorState.value[index]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTooltip(stringResource(if (state.selected) Res.string.export_to_translator else Res.string.dont_export_to_translator)) {
                        Checkbox(
                            state.selected,
                            { selected ->
                                exportToTranslatorState.value = exportToTranslatorState.value.toMutableList().apply { this[index] = this[index].copy(selected = selected) }
                            }
                        )
                    }
                    Text(language.name)
                    IconButton({exportToTranslatorState.value = exportToTranslatorState.value.toMutableList().apply { this[index] = this[index].copy(readOnly = !this[index].readOnly) }}) {
                        AppTooltip(stringResource(if (state.readOnly) Res.string.not_editable_for_translator else Res.string.editable_for_translator)) {
                            Icon(if (state.readOnly) Icons.Outlined.Lock else Icons.Outlined.LockOpen, "read only")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(exportOnlyUntranslatedKeys, {
                    exportOnlyUntranslatedKeys = it
                })
                Text(stringResource(Res.string.export_only_untranslated_keys))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(exportFolder?.absolutePath ?: stringResource(Res.string.choose_path), {}, Modifier.width(OutlinedTextFieldDefaults.MinWidth), readOnly = true, singleLine = true, label = { Text(stringResource(Res.string.path)) })
                IconButton({
                    scope.launch {
                        val folder = FileKit.openDirectoryPicker() ?: return@launch
                        exportFolder = folder.file
                    }
                }) {
                    AppTooltip(stringResource(Res.string.select_folder)) {
                        Icon(Icons.Outlined.Folder, "path")
                    }
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({ vm.showExportToTranslatorDialog.value = false }) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({
                    vm.exportToTranslator(exportToTranslatorState.value, exportOnlyUntranslatedKeys, exportFolder!!)
                }, enabled = exportButtonEnabled) {
                    Text(stringResource(Res.string.export))
                }
            }
        }
    } else if (showImportDialog) {
        val scope = rememberCoroutineScope()
        val fileStructures = FileStructure.entries
        var fileStructure by remember { mutableStateOf(fileStructures.first()) }
        val formatSpecifiers = FormatSpecifierFormatter.supportedToAppFormatSpecifiers
        var formatSpecifier by remember { mutableStateOf(formatSpecifiers.first()) }
        val languages by vm.languages.collectAsStateWithLifecycle(listOf())
        val paths = remember(languages) {
            mutableStateOf<List<String>>(listOf()).apply {
                value = languages.fastMap { "" }
            }
        }
        val platformsSelection = remember(platforms) { mutableStateOf<List<Boolean>>(listOf()).apply {
            value = platforms.fastMap { false }
        }}
        val importButtonEnabled = remember(paths.value, platformsSelection.value) {
            paths.value.fastAny { it.isNotEmpty() } && platformsSelection.value.fastAny { it }
        }

        AppDialog {
            GenericDropdown(fileStructures.fastMap { stringResource(it.stringResource) },
                fileStructures.indexOf(fileStructure), { fileStructure = fileStructures[it] },
                { Text(stringResource(Res.string.file_structure)) }
            )
            GenericDropdown(formatSpecifiers.fastMap { stringResource(it.stringResource) },
                formatSpecifiers.indexOf(formatSpecifier), { formatSpecifier = formatSpecifiers[it] },
                { Text(stringResource(Res.string.format_specifier)) }
            )
            languages.fastForEachIndexed { index, language ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(paths.value[index].takeIf { it.isNotEmpty() } ?: stringResource(Res.string.choose_path), {}, Modifier.width(TextFieldDefaults.MinWidth), readOnly = true, singleLine = true, label = { Text(language.name) })
                    IconButton({
                        scope.launch {
                            val file = FileKit.openFilePicker(FileKitType.File(fileStructure.fileExtension.removePrefix(".")), language.name) ?: return@launch
                            paths.value = paths.value.toMutableList().apply {
                                this[index] = file.file.absolutePath
                            }
                        }
                    }) {
                        AppTooltip(stringResource(Res.string.select_file)) {
                            Icon(Icons.Outlined.Folder, "path")
                        }
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                platforms.fastForEachIndexed { index, platform ->
                    FilterChip(platformsSelection.value[index],
                        {
                            platformsSelection.value = platformsSelection.value.toMutableList().apply {
                                this[index] = !platformsSelection.value[index]
                            }
                        },
                        { Text(platform.name) },
                        leadingIcon = if (platformsSelection.value[index]) {{ Icon(Icons.Filled.Done, "remove platform") }} else null)
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({ vm.showImportDialog.value = false }) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({
                    vm.import(fileStructure, formatSpecifier, paths.value, platformsSelection.value)
                }, enabled = importButtonEnabled) {
                    Text(stringResource(Res.string.import))
                }
            }
        }
    } else if (showExportToTranslatorEmptyErrorDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.nothing_to_export))
            },
            text = {
                Text(stringResource(Res.string.all_keys_are_translated))
            },
            confirmButton = {
                Button({vm.showExportToTranslatorEmptyErrorDialog.value = false}) {
                    Text(stringResource(Res.string.ok))
                }
            })
    } else if (showImportForTranslatorFormatErrorDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.file_format_is_not_correct))
            },
            confirmButton = {
                Button({vm.showImportForTranslatorFormatErrorDialog.value = false}) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}

@Composable
private fun ExportSettings(
    platforms: List<PlatformEntity>,
    showExportAndOverwriteLoader: Boolean,
    showExportAsZipLoader: Boolean,
    editExportToPath: (PlatformEntity) -> Unit,
    exportAsZip: (selectedPlatforms: List<Boolean>) -> Unit,
    exportAndOverwrite: (selectedPlatforms: List<Boolean>) -> Unit,
    exportToTranslator: () -> Unit
) {
    val selectedPlatforms = remember(platforms) {
        mutableStateOf<List<Boolean>>(listOf()).apply {
            value = platforms.map { true }
        }
    }
    val exportEnabled = remember(selectedPlatforms.value, platforms) {
        var enabled = false
        for (index in platforms.indices) {
            if (selectedPlatforms.value[index]) {
                val platform = platforms[index]
                if (platform.exportToPath.isEmpty()) {
                    enabled = false
                    break
                }
                enabled = true
            }
        }
        enabled
    }
    AppCard {
        Text(stringResource(Res.string.export), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.export_description, stringResource(Res.string.export_and_overwrite), stringResource(Res.string.export_as_zip), stringResource(Res.string.export_to_translator)), style = MaterialTheme.typography.bodyMedium)
        platforms.fastForEachIndexed { index, platform ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    selectedPlatforms.value[index],
                    {
                        selectedPlatforms.value = selectedPlatforms.value.toMutableList().apply {
                            this[index] = it
                        }
                    }
                )
                Text(platform.name)
            }
        }
        Text(stringResource(Res.string.path), style = MaterialTheme.typography.titleSmall)
        platforms.fastForEach { platform ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(platform.exportToPath.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.choose_path), {}, Modifier.width(OutlinedTextFieldDefaults.MinWidth), readOnly = true, singleLine = true, label = { Text(platform.name, maxLines = 1) })
                IconButton({
                    editExportToPath(platform)
                }) {
                    AppTooltip(stringResource(Res.string.select_folder)) {
                        Icon(Icons.Outlined.Folder, "path")
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ButtonWithLoader({
                exportAndOverwrite(selectedPlatforms.value)
            }, exportEnabled, showExportAndOverwriteLoader, stringResource(Res.string.export_and_overwrite), 2)
            ButtonWithLoader({
                exportAsZip(selectedPlatforms.value)
            }, exportEnabled, showExportAsZipLoader, stringResource(Res.string.export_as_zip), 2)
            Button(exportToTranslator) {
                Text(stringResource(Res.string.export_to_translator), maxLines = 2)
            }
        }
    }
}

@Composable
private fun Import(
    showImportLoader: Boolean,
    import: () -> Unit,
    importFromTranslator: () -> Unit) {
    AppCard {
        Text(stringResource(Res.string.import), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.import_description), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ButtonWithLoader(import, true, showImportLoader, stringResource(Res.string.import), 2)
            Button(importFromTranslator) {
                Text(stringResource(Res.string.import_from_translator), maxLines = 2)
            }
        }
    }
}