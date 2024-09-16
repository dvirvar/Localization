package com.localization.offline.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexedNotNull
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.localization.offline.db.PlatformEntity
import com.localization.offline.extension.tryBrowse
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.model.FormatSpecifierFormatter
import com.localization.offline.service.ExportService
import com.localization.offline.service.ImportService
import com.localization.offline.service.LanguageService
import com.localization.offline.service.PlatformService
import com.localization.offline.ui.view.GenericDropdown
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.choose_path
import localization.composeapp.generated.resources.export
import localization.composeapp.generated.resources.file_structure
import localization.composeapp.generated.resources.format_specifier
import localization.composeapp.generated.resources.import
import localization.composeapp.generated.resources.path
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import java.io.File

class ExportImportVM: ViewModel() {
    private val exportService = ExportService()
    private val importService = ImportService()
    val platforms = PlatformService().getAllPlatforms()
    val languages = LanguageService().getAllLanguages()
    val showImportDialog = MutableStateFlow(false)

    fun editExportToPath(platformEntity: PlatformEntity) {
        viewModelScope.launch {
            val folder = FileKit.pickDirectory() ?: return@launch
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

    fun import(fileStructure: FileStructure, formatSpecifier: FormatSpecifier, paths: List<String>, selectedPlatforms: List<Boolean>) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportImportScreen() {
    val vm = koinViewModel<ExportImportVM>()
    val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
    val showImportDialog by vm.showImportDialog.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(36.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
        Export(platforms, vm::editExportToPath, vm::exportAsZip, vm::exportAndOverwrite)
        Import(
            {vm.showImportDialog.value = true},
            {}
        )
    }

    if (showImportDialog) {
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
            value = platforms.fastMap { true }
        }}
        val importButtonEnabled = remember(paths.value, platformsSelection.value) {
            paths.value.fastAny { it.isNotEmpty() } && platformsSelection.value.fastAny { it }
        }

        Dialog(onDismissRequest = {}) {
            Column(Modifier.wrapContentSize(unbounded = true).background(Color.White, RoundedCornerShape(6.dp)).padding(16.dp)) {
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
                        OutlinedTextField(paths.value[index].takeIf { it.isNotEmpty() } ?: stringResource(Res.string.choose_path), {}, readOnly = true, singleLine = true, label = { Text(language.name) })
                        IconButton({
                            scope.launch {
                                val file = FileKit.pickFile(PickerType.File(listOf(fileStructure.fileExtension.removePrefix("."))), language.name) ?: return@launch
                                paths.value = paths.value.toMutableList().apply {
                                    this[index] = file.file.absolutePath
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Folder, "path")
                        }
                    }
                }
                FlowRow(Modifier.fillMaxWidth()) {
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
                Row(Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
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
        }
    }
}

@Composable
private fun Export(
    platforms: List<PlatformEntity>,
    editExportToPath: (PlatformEntity) -> Unit,
    exportAsZip: (selectedPlatforms: List<Boolean>) -> Unit,
    exportAndOverwrite: (selectedPlatforms: List<Boolean>) -> Unit,
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
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188)).padding(10.dp)) {
        Text(stringResource(Res.string.export), style = MaterialTheme.typography.titleMedium)
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
                OutlinedTextField(platform.exportToPath.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.choose_path), {}, readOnly = true, singleLine = true, label = { Text(platform.name) })
                IconButton({
                    editExportToPath(platform)
                }) {
                    Icon(Icons.Outlined.Folder, "path")
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Button({
                exportAndOverwrite(selectedPlatforms.value)
            }, enabled = exportEnabled) {
                Text("Export & overwrite")
            }
            Button({
                exportAsZip(selectedPlatforms.value)
            }, enabled = exportEnabled) {
                Text("Export as ZIP")
            }
        }
    }
}

@Composable
private fun Import(import: () -> Unit, importFromTranslator: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188)).padding(10.dp)) {
        Text(stringResource(Res.string.import), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Button(import) {
                Text("Import")
            }
            Button(importFromTranslator) {
                Text("Import from translator")
            }
        }
    }
}