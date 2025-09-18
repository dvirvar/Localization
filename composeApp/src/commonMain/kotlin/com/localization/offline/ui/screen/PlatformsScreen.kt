@file:OptIn(ExperimentalLayoutApi::class)

package com.localization.offline.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.localization.offline.db.CustomFormatSpecifierEntity
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.extension.hasDuplicateBy
import com.localization.offline.model.EmptyTranslationExport
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.service.LanguageService
import com.localization.offline.service.PlatformService
import com.localization.offline.ui.view.AppCard
import com.localization.offline.ui.view.AppDialog
import com.localization.offline.ui.view.AppTextField
import com.localization.offline.ui.view.AppTooltip
import com.localization.offline.ui.view.GenericDropdown
import com.localization.offline.ui.view.SaveableButtonsTextField
import com.localization.offline.ui.view.SaveableIconsTextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.add
import localization.composeapp.generated.resources.add_all_keys_to_platform
import localization.composeapp.generated.resources.all_your_custom_format_specifiers_will_be_deleted
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.change_format_specifier_q
import localization.composeapp.generated.resources.delete
import localization.composeapp.generated.resources.delete_q
import localization.composeapp.generated.resources.duplicate_folder_and_file_detected
import localization.composeapp.generated.resources.empty_translation_export
import localization.composeapp.generated.resources.export_settings
import localization.composeapp.generated.resources.export_settings_description
import localization.composeapp.generated.resources.file_structure
import localization.composeapp.generated.resources.format_specifier
import localization.composeapp.generated.resources.format_specifiers
import localization.composeapp.generated.resources.format_specifiers_description
import localization.composeapp.generated.resources.language_already_exist
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.platform
import localization.composeapp.generated.resources.platform_already_exist
import localization.composeapp.generated.resources.platforms
import localization.composeapp.generated.resources.prefix
import localization.composeapp.generated.resources.regex
import localization.composeapp.generated.resources.remove
import localization.composeapp.generated.resources.save
import localization.composeapp.generated.resources.with
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

class PlatformsVM: ViewModel() {
    private val platformService = PlatformService()
    val platforms = platformService.getAllPlatformsAsFlow()
    val customFormatSpecifiers = platformService.getAllCustomFormatSpecifiersAsFlow()
    val showAddPlatformDialog = MutableStateFlow(false)
    val platformNameError = MutableStateFlow<StringResource?>(null)
    val showPlatformNameAlreadyExistDialog = MutableStateFlow(false)
    val platformToDeletion = MutableStateFlow<PlatformEntity?>(null)
    val showDeletePlatformDialog = platformToDeletion.map {
        it != null
    }
    val formatSpecifiers = FormatSpecifier.entries
    private val customFormatSpecifierChangeData = MutableStateFlow<CustomFormatSpecifierChangeData?>(null)
    val showCustomFormatSpecifierDeletionDialog = customFormatSpecifierChangeData.map {
        it != null
    }
    val addCustomFormatSpecifiersPlatform = MutableStateFlow<PlatformEntity?>(null)
    val showAddCustomFormatSpecifiersDialog = addCustomFormatSpecifiersPlatform.map {
        it != null
    }
    val emptyTranslationExports = EmptyTranslationExport.entries
    val fileStructures = FileStructure.entries
    private val languageService = LanguageService()
    val languageExportSettings = languageService.getAllLanguageExportSettingsAsFlow()
    val languages = languageService.getAllLanguagesAsFlow()
    var showDuplicateFolderAndFileDialog = MutableStateFlow(false)

    fun addPlatform(name: String, emptyTranslationExport: EmptyTranslationExport,
                    fileStructure: FileStructure, formatSpecifier: FormatSpecifier,
                    exportPrefix: String, customFormatSpecifiers: List<CustomFormatSpecifierEntity>,
                    languageExportSettings: List<LanguageExportSettingsEntity>, addAllKeysToPlatform: Boolean) {
        viewModelScope.launch {
            if (platformService.doesPlatformExist(name)) {
                platformNameError.value = Res.string.platform_already_exist
                return@launch
            }
            val platform = PlatformEntity(Random.nextInt(), name, emptyTranslationExport, fileStructure, formatSpecifier, exportPrefix)
            val customFormatSpecifiers = customFormatSpecifiers.fastMap { it.copy(platformId = platform.id) }
            val languageExportSettings = languageExportSettings.fastMap { it.copy(platformId = platform.id) }
            platformService.addPlatform(platform, customFormatSpecifiers, languageExportSettings, addAllKeysToPlatform)
            showAddPlatformDialog.value = false
        }
    }

    fun editPlatformName(id: Int, originalName: String, name: String) {
        viewModelScope.launch {
            if (name != originalName) {
                if (platformService.doesPlatformExist(name, id)) {
                    showPlatformNameAlreadyExistDialog.value = true
                    return@launch
                }
            }
            platformService.updatePlatformName(id, name)
        }
    }

    fun setPlatformToDeletion(platform: PlatformEntity?) {
        platformToDeletion.value = platform
    }

    fun deletePlatform() {
        viewModelScope.launch {
            platformService.deletePlatform(platformToDeletion.value!!)
            setPlatformToDeletion(null)
        }
    }

    fun editFormatSpecifier(platform: PlatformEntity, formatSpecifier: FormatSpecifier) {
        if (platform.formatSpecifier == formatSpecifier) {
            return
        }
        viewModelScope.launch {
            if (platform.formatSpecifier == FormatSpecifier.Custom && !customFormatSpecifiers.first()[platform.id].isNullOrEmpty()) {
                customFormatSpecifierChangeData.value = CustomFormatSpecifierChangeData(platform.id, formatSpecifier)
                return@launch
            }
            platformService.updatePlatformFormatSpecifier(platform.id, formatSpecifier)
        }
    }

    fun editCustomFormatSpecifier(customFormatSpecifier: CustomFormatSpecifierEntity, from: String, to: String) {
        viewModelScope.launch {
            platformService.updateCustomFormatSpecifier(customFormatSpecifier.copy(from = from, to = to))
        }
    }

    fun deleteCustomFormatSpecifier(id: Int) {
        viewModelScope.launch {
            platformService.deleteCustomFormatSpecifier(id)
        }
    }

    fun cancelCustomFormatSpecifierChangeData() {
        customFormatSpecifierChangeData.value = null
    }

    fun editFormatSpecifierByChangeData() {
        viewModelScope.launch {
            val platformId = customFormatSpecifierChangeData.value!!.platformId
            platformService.updatePlatformFormatSpecifier(platformId, customFormatSpecifierChangeData.value!!.formatSpecifier)
            platformService.deletePlatformCustomFormatSpecifiers(platformId)
            customFormatSpecifierChangeData.value = null
        }
    }

    fun addCustomFormatSpecifiers(customFormatSpecifiers: List<CustomFormatSpecifierEntity>) {
        viewModelScope.launch {
            platformService.addCustomFormatSpecifiers(customFormatSpecifiers)
            addCustomFormatSpecifiersPlatform.value = null
        }
    }

    fun editEmptyTranslationExport(platform: PlatformEntity, emptyTranslationExport: EmptyTranslationExport) {
        viewModelScope.launch {
            platformService.updatePlatformEmptyTranslationExport(platform.id, emptyTranslationExport)
        }
    }

    fun editFileStructure(platform: PlatformEntity, fileStructure: FileStructure) {
        viewModelScope.launch {
            platformService.updatePlatformFileStructure(platform.id, fileStructure)
        }
    }

    fun editExportPrefix(platform: PlatformEntity, exportPrefix: String) {
        viewModelScope.launch {
            if (!validateExportPrefix(platform, exportPrefix)) {
                return@launch
            }
            platformService.updatePlatformExportPrefix(platform.id, exportPrefix)
        }
    }

    private suspend fun validateExportPrefix(platform: PlatformEntity, exportPrefix: String): Boolean {
        if (exportPrefix.isBlank()) {
            if (languageExportSettings.first()[platform.id]!!.fastAny { it.folderSuffix.isEmpty() }) {
                return false
            }
        }
        return true
    }

    fun editLanguageExportSettings(les: LanguageExportSettingsEntity, folderSuffix: String, fileName: String) {
        viewModelScope.launch {
            val les = les.copy(folderSuffix = folderSuffix, fileName = fileName)
            if (!validateLanguageExportSettings(les)) {
                return@launch
            }
            languageService.updateLanguageExportSettings(les)
        }
    }

    private suspend fun validateLanguageExportSettings(les: LanguageExportSettingsEntity): Boolean {
        val hasDuplicate = languageExportSettings.first()[les.platformId]!!.toMutableList().apply {
            val ind = this.indexOfFirst { it.languageId == les.languageId }
            this[ind] = les
        }.hasDuplicateBy { it.folderSuffix + it.fileName }
        if (hasDuplicate) {
            showDuplicateFolderAndFileDialog.value = true
            return false
        }
        return true
    }
}

@Composable
fun PlatformsScreen() {
    val vm = koinViewModel<PlatformsVM>()
    val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
    val customFormatSpecifiers by vm.customFormatSpecifiers.collectAsStateWithLifecycle(mapOf())
    val showAddPlatformDialog by vm.showAddPlatformDialog.collectAsStateWithLifecycle()
    val showPlatformNameAlreadyExistDialog by vm.showPlatformNameAlreadyExistDialog.collectAsStateWithLifecycle()
    val showDeletePlatformDialog by vm.showDeletePlatformDialog.collectAsStateWithLifecycle(false)
    val showCustomFormatSpecifierDeletionDialog by vm.showCustomFormatSpecifierDeletionDialog.collectAsStateWithLifecycle(false)
    val showAddCustomFormatSpecifiersDialog by vm.showAddCustomFormatSpecifiersDialog.collectAsStateWithLifecycle(false)
    val languageExportSettings by vm.languageExportSettings.collectAsStateWithLifecycle(mapOf())
    val languages by vm.languages.collectAsStateWithLifecycle(listOf())
    val showDuplicateFolderAndFileDialog by vm.showDuplicateFolderAndFileDialog.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(36.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
        Platforms(
            vm::editPlatformName,
            vm::setPlatformToDeletion,
            {
                vm.showAddPlatformDialog.value = true
            },
            platforms
        )
        FormatSpecifiers(
            vm::editFormatSpecifier, vm::editCustomFormatSpecifier, vm::deleteCustomFormatSpecifier,
            {
              vm.addCustomFormatSpecifiersPlatform.value = it
            },
            platforms, vm.formatSpecifiers, customFormatSpecifiers
        )
        ExportSettings(vm::editEmptyTranslationExport, vm::editFileStructure, vm::editExportPrefix, vm::editLanguageExportSettings, platforms, vm.emptyTranslationExports, vm.fileStructures, languageExportSettings, languages)
    }

    if (showAddPlatformDialog) {
        val emptyTranslationExports = EmptyTranslationExport.entries
        val formatSpecifiers = FormatSpecifier.entries
        val fileStructures = FileStructure.entries
        var platform by remember { mutableStateOf("") }
        val platformError by vm.platformNameError.collectAsStateWithLifecycle()
        var emptyTranslationExport by remember { mutableStateOf(EmptyTranslationExport.DontExport) }
        var formatSpecifier by remember { mutableStateOf(FormatSpecifier.None) }
        val customFormatSpecifiers = remember { mutableStateOf<List<CustomFormatSpecifierEntity>>(listOf()) }
        var fileStructure by remember { mutableStateOf(fileStructures.first()) }
        var exportPrefix by remember { mutableStateOf("") }
        val languageExportSettings = remember(languages) { mutableStateOf(languages.fastMap { LanguageExportSettingsEntity(it.id, 0, "", "") }.toMutableList()) }
        var addAllKeysToPlatform by remember { mutableStateOf(true) }

        val addButtonEnabled = remember(platform, exportPrefix, customFormatSpecifiers.value, languageExportSettings.value) {
            val platformIsValid = platform.isNotBlank() && (exportPrefix.isNotEmpty() || !languageExportSettings.value.fastAny { it.folderSuffix.isEmpty() })
            for (cfs in customFormatSpecifiers.value) {
                if (cfs.from.isEmpty() || cfs.to.isEmpty()) {
                    return@remember false
                }
            }
            val languageExportSettingsIsValid = languageExportSettings.value.all { it.fileName.isNotBlank() }
            platformIsValid && languageExportSettingsIsValid
        }

        DisposableEffect(Unit) {
            onDispose {
                vm.platformNameError.value = null
            }
        }

        AppDialog {
            AppTextField(platform, {
                platform = it
                vm.platformNameError.value = null
            }, Modifier.width(TextFieldDefaults.MinWidth), label = { Text(stringResource(Res.string.platform)) }, error = platformError?.let { stringResource(it) }, singleLine = true)
            GenericDropdown(emptyTranslationExports.fastMap { stringResource(it.stringResource) },
                emptyTranslationExports.indexOf(emptyTranslationExport), { emptyTranslationExport = emptyTranslationExports[it] },
                { Text(stringResource(Res.string.empty_translation_export), maxLines = 2)}
            )
            GenericDropdown(formatSpecifiers.fastMap { stringResource(it.stringResource) },
                formatSpecifiers.indexOf(formatSpecifier), { formatSpecifier = formatSpecifiers[it] },
                { Text(stringResource(Res.string.format_specifier), maxLines = 2) }
            )
            if (formatSpecifier == FormatSpecifier.Custom) {
                customFormatSpecifiers.value.fastForEachIndexed { index, strategy ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(strategy.from, {
                            customFormatSpecifiers.value = customFormatSpecifiers.value.toMutableList().apply {
                                this[index] = this[index].copy(from = it)
                            }
                        }, Modifier.width(100.dp), placeholder = { Text(stringResource(Res.string.regex), Modifier.horizontalScroll(rememberScrollState()), maxLines = 1) }, singleLine = true)
                        Text(stringResource(Res.string.with), Modifier.padding(horizontal = 10.dp))
                        OutlinedTextField(strategy.to, {
                            customFormatSpecifiers.value = customFormatSpecifiers.value.toMutableList().apply {
                                this[index] = this[index].copy(to = it)
                            }
                        }, Modifier.width(100.dp), singleLine = true)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    Button({
                        customFormatSpecifiers.value = customFormatSpecifiers.value.toMutableList().apply {
                            add(CustomFormatSpecifierEntity(Random.nextInt(), 0, "", ""))
                        }
                    }) {
                        Text(stringResource(Res.string.add))
                    }
                    if (customFormatSpecifiers.value.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Button({
                            customFormatSpecifiers.value = customFormatSpecifiers.value.toMutableList().apply {
                                removeLast()
                            }
                        }) {
                            Text(stringResource(Res.string.remove))
                        }
                    }
                }
            }
            GenericDropdown(fileStructures.fastMap { stringResource(it.stringResource) },
                fileStructures.indexOf(fileStructure), { fileStructure = fileStructures[it] },
                { Text(stringResource(Res.string.file_structure), maxLines = 2) }
            )
            OutlinedTextField(exportPrefix, {
                exportPrefix = it
            }, Modifier.width(TextFieldDefaults.MinWidth), singleLine = true, label = { Text(stringResource(Res.string.prefix), maxLines = 2) })
            languageExportSettings.value.fastForEachIndexed { index, les ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(les.folderSuffix, {
                        languageExportSettings.value = languageExportSettings.value.toMutableList().apply {
                            this[index] = this[index].copy(folderSuffix = it)
                        }
                    }, Modifier.width(120.dp), singleLine = true, label = { Text(languages[index].name) })
                    Text("/", Modifier.align(Alignment.CenterVertically), fontSize = 36.sp)
                    OutlinedTextField(les.fileName, {
                        languageExportSettings.value = languageExportSettings.value.toMutableList().apply {
                            this[index] = this[index].copy(fileName = it)
                        }
                    }, Modifier.width(120.dp), singleLine = true)
                    Spacer(Modifier.width(10.dp))
                    if (exportPrefix.isNotEmpty() || les.folderSuffix.isNotEmpty() || les.fileName.isNotEmpty()) {
                        Text("${exportPrefix}${les.folderSuffix}/${les.fileName}${fileStructure.fileExtension}")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(addAllKeysToPlatform, {addAllKeysToPlatform = it})
                Text(stringResource(Res.string.add_all_keys_to_platform))
            }
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({vm.showAddPlatformDialog.value = false}) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({
                    vm.addPlatform(platform, emptyTranslationExport, fileStructure, formatSpecifier, exportPrefix, customFormatSpecifiers.value, languageExportSettings.value, addAllKeysToPlatform)
                }, enabled = addButtonEnabled) {
                    Text(stringResource(Res.string.add))
                }
            }
        }
    } else if (showPlatformNameAlreadyExistDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.language_already_exist))
            },
            confirmButton = {
                Button({vm.showPlatformNameAlreadyExistDialog.value = false}) {
                    Text(stringResource(Res.string.ok))
                }
            })
    } else if (showDeletePlatformDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.delete_q, vm.platformToDeletion.value!!.name))
            },
            dismissButton = {
                Button({vm.setPlatformToDeletion(null)}) {
                    Text(stringResource(Res.string.no))
                }
            },
            confirmButton = {
                Button({vm.deletePlatform()}) {
                    Text(stringResource(Res.string.yes))
                }
            })
    } else if (showCustomFormatSpecifierDeletionDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.change_format_specifier_q))
            },
            text = {
                Text(stringResource(Res.string.all_your_custom_format_specifiers_will_be_deleted))
            },
            dismissButton = {
                Button({vm.cancelCustomFormatSpecifierChangeData()}) {
                    Text(stringResource(Res.string.no))
                }
            },
            confirmButton = {
                Button({vm.editFormatSpecifierByChangeData()}) {
                    Text(stringResource(Res.string.yes))
                }
            })
    } else if (showAddCustomFormatSpecifiersDialog) {
        val customFormatSpecifiers = remember { mutableStateOf(listOf(
            CustomFormatSpecifierEntity(Random.nextInt(), vm.addCustomFormatSpecifiersPlatform.value!!.id, "", "")
        )) }

        val addButtonEnabled = remember(customFormatSpecifiers.value) {
            var cfsAreValid = true
            for (cfs in customFormatSpecifiers.value) {
                if (cfs.from.isEmpty() || cfs.to.isEmpty()) {
                    cfsAreValid = false
                    break
                }
            }
            cfsAreValid
        }

        AppDialog {
            Text(vm.addCustomFormatSpecifiersPlatform.value!!.name, style = MaterialTheme.typography.titleMedium)
            customFormatSpecifiers.value.fastForEachIndexed { index, strategy ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(strategy.from, {
                        customFormatSpecifiers.value =
                            customFormatSpecifiers.value.toMutableList().apply {
                                this[index] = this[index].copy(from = it)
                            }
                    }, Modifier.width(100.dp), placeholder = { Text(stringResource(Res.string.regex), Modifier.horizontalScroll(rememberScrollState()), maxLines = 1) }, singleLine = true)
                    Text(
                        stringResource(Res.string.with),
                        Modifier.padding(horizontal = 10.dp)
                    )
                    OutlinedTextField(strategy.to, {
                        customFormatSpecifiers.value =
                            customFormatSpecifiers.value.toMutableList().apply {
                                this[index] = this[index].copy(to = it)
                            }
                    }, Modifier.width(100.dp), singleLine = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Button({
                    customFormatSpecifiers.value =
                        customFormatSpecifiers.value.toMutableList().apply {
                            add(CustomFormatSpecifierEntity(Random.nextInt(), vm.addCustomFormatSpecifiersPlatform.value!!.id, "", ""))
                        }
                }) {
                    Text(stringResource(Res.string.add))
                }
                if (customFormatSpecifiers.value.size > 1) {
                    Spacer(Modifier.width(10.dp))
                    Button({
                        customFormatSpecifiers.value =
                            customFormatSpecifiers.value.toMutableList().apply {
                                removeLast()
                            }
                    }) {
                        Text(stringResource(Res.string.remove))
                    }
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({ vm.addCustomFormatSpecifiersPlatform.value = null }) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({
                    vm.addCustomFormatSpecifiers(
                        customFormatSpecifiers.value
                    )
                }, enabled = addButtonEnabled) {
                    Text(stringResource(Res.string.add))
                }
            }
        }
    } else if (showDuplicateFolderAndFileDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.duplicate_folder_and_file_detected))
            },
            confirmButton = {
                Button({ vm.showDuplicateFolderAndFileDialog.value = false }) {
                    Text(stringResource(Res.string.ok))
                }
            })
    }
}

@Composable
private fun Platforms(
    editPlatformName: (id: Int, originalName: String, name: String) -> Unit,
    setPlatformToDeletion: (PlatformEntity) -> Unit,
    addPlatform: () -> Unit,
    platforms: List<PlatformEntity>
) {
    AppCard {
        Text(stringResource(Res.string.platforms), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        platforms.fastForEach { platform ->
            PlatformRow(platform.name, platforms.size > 1,
                {
                    editPlatformName(platform.id, platform.name, it)
                },
                {
                    setPlatformToDeletion(platform)
                }
            )
        }
        Row {
            Button(addPlatform) {
                Text(stringResource(Res.string.add))
            }
        }
    }
}

@Composable
private fun PlatformRow(platform: String, showDelete: Boolean, onSave: (String) -> Unit, onDelete: () -> Unit) {
    SaveableButtonsTextField(
        onSave,
        platform,
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        if (showDelete) {
            {
                IconButton(onDelete) {
                    AppTooltip(stringResource(Res.string.delete)) {
                        Icon(Icons.Filled.DeleteForever, "delete platform")
                    }
                }
            }
        } else null,
        singleLine = true
    )
}

@Composable
private fun FormatSpecifiers(
    editFormatSpecifier: (PlatformEntity, FormatSpecifier) -> Unit,
    editCustomFormatSpecifier: (CustomFormatSpecifierEntity, from: String, to: String) -> Unit,
    deleteCustomFormatSpecifier: (id: Int) -> Unit,
    addCustomFormatSpecifiers: (PlatformEntity) -> Unit,
    platforms: List<PlatformEntity>,
    formatSpecifiers: List<FormatSpecifier>,
    customFormatSpecifiers: Map<Int, List<CustomFormatSpecifierEntity>>
) {
    var openSection by remember { mutableStateOf(false) }

    AppCard {
        Row(Modifier.fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null) { openSection = !openSection },
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.format_specifiers), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(if (openSection) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                if (openSection) "close" else "open")
        }
        AnimatedVisibility(openSection) {
            Column {
                Text(stringResource(Res.string.format_specifiers_description), style = MaterialTheme.typography.bodyMedium)
                platforms.fastForEach { platform ->
                    Spacer(Modifier.height(10.dp))
                    Text(platform.name, style = MaterialTheme.typography.titleSmall)
                    GenericDropdown(formatSpecifiers.fastMap { stringResource(it.stringResource) },
                        formatSpecifiers.indexOf(platform.formatSpecifier),
                        { editFormatSpecifier(platform, formatSpecifiers[it]) },
                        { Text(stringResource(Res.string.format_specifier)) }
                    )
                    if (platform.formatSpecifier == FormatSpecifier.Custom) {
                        customFormatSpecifiers[platform.id]?.fastForEach { csf ->
                            Spacer(Modifier.height(10.dp))
                            CustomFormatSpecifierRow(csf, editCustomFormatSpecifier, deleteCustomFormatSpecifier)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button({addCustomFormatSpecifiers(platform)}) {
                            Text(stringResource(Res.string.add))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomFormatSpecifierRow(customFormatSpecifier: CustomFormatSpecifierEntity, onSave: (csf: CustomFormatSpecifierEntity, from: String, to: String) -> Unit, onDelete: (Int) -> Unit) {
    var from by remember(customFormatSpecifier.from) { mutableStateOf(customFormatSpecifier.from) }
    var to by remember(customFormatSpecifier.to) { mutableStateOf(customFormatSpecifier.to) }
    val showSaveCancel = remember(from, to, customFormatSpecifier.from, customFormatSpecifier.to) {
        from != customFormatSpecifier.from || to != customFormatSpecifier.to
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(from, {
            from = it
        }, Modifier.width(100.dp), singleLine = true)
        Text(
            stringResource(Res.string.with),
            Modifier.padding(horizontal = 10.dp)
        )
        OutlinedTextField(to, {
            to = it
        }, Modifier.width(100.dp), singleLine = true)
        if (showSaveCancel) {
            val saveEnabled = remember(from, to) { from.isNotBlank() && to.isNotBlank() }
            IconButton({
                onSave(customFormatSpecifier, from, to)
            }, enabled = saveEnabled) {
                AppTooltip(stringResource(Res.string.save)) {
                    Icon(Icons.Filled.Save, "save custom format specifier")
                }
            }
            IconButton({
                from = customFormatSpecifier.from
                to = customFormatSpecifier.to
            }) {
                AppTooltip(stringResource(Res.string.cancel)) {
                    Icon(Icons.Filled.Cancel, "cancel custom format specifier changes")
                }
            }
        } else {
            IconButton({
                onDelete(customFormatSpecifier.id)
            }) {
                AppTooltip(stringResource(Res.string.delete)) {
                    Icon(Icons.Filled.DeleteForever, "delete custom format specifier")
                }
            }
        }
    }
}

private data class CustomFormatSpecifierChangeData(
    val platformId: Int,
    val formatSpecifier: FormatSpecifier
)

@Composable
private fun ExportSettings(
    editEmptyTranslationExports: (PlatformEntity, EmptyTranslationExport) -> Unit,
    editFileStructure: (PlatformEntity, FileStructure) -> Unit,
    editExportPrefix: (PlatformEntity, String) -> Unit,
    editLanguageExportSettings: (les: LanguageExportSettingsEntity, folderSuffix: String, fileName: String) -> Unit,
    platforms: List<PlatformEntity>,
    emptyTranslationExports: List<EmptyTranslationExport>,
    fileStructures: List<FileStructure>,
    languageExportSettings: Map<Int, List<LanguageExportSettingsEntity>>,
    languages: List<LanguageEntity>
) {
    var openSection by remember { mutableStateOf(false) }

    AppCard {
        Row(Modifier.fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null) { openSection = !openSection },
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.export_settings), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(if (openSection) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                if (openSection) "close" else "open")
        }
        AnimatedVisibility(openSection) {
            Column {
                Text(stringResource(Res.string.export_settings_description), style = MaterialTheme.typography.bodyMedium)
                FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    platforms.fastForEach { platform ->
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text(platform.name, style = MaterialTheme.typography.titleSmall)
                            GenericDropdown(
                                emptyTranslationExports.fastMap { stringResource(it.stringResource) },
                                emptyTranslationExports.indexOf(platform.emptyTranslationExport),
                                {
                                    editEmptyTranslationExports(
                                        platform,
                                        emptyTranslationExports[it]
                                    )
                                },
                                { Text(stringResource(Res.string.empty_translation_export)) }
                            )
                            GenericDropdown(
                                fileStructures.fastMap { stringResource(it.stringResource) },
                                fileStructures.indexOf(platform.fileStructure),
                                { editFileStructure(platform, fileStructures[it]) },
                                { Text(stringResource(Res.string.file_structure)) }
                            )
                            SaveableIconsTextField(
                                {
                                    editExportPrefix(platform, it)
                                },
                                platform.exportPrefix,
                                textFieldModifier = Modifier.width(TextFieldDefaults.MinWidth),
                                singleLine = true,
                                label = { Text(stringResource(Res.string.prefix)) })
                            languageExportSettings[platform.id]!!.fastForEachIndexed { index, languageExportSettings ->
                                Spacer(Modifier.height(6.dp))
                                LanguageExportSettingsRow(
                                    platform,
                                    languageExportSettings,
                                    languages[index].name,
                                    editLanguageExportSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageExportSettingsRow(platform: PlatformEntity, languageExportSettings: LanguageExportSettingsEntity, languageName: String, onSave: (les: LanguageExportSettingsEntity, folderSuffix: String, fileName: String) -> Unit) {
    var folderSuffix by remember(languageExportSettings.folderSuffix) { mutableStateOf(languageExportSettings.folderSuffix) }
    var fileName by remember(languageExportSettings.fileName) { mutableStateOf(languageExportSettings.fileName) }
    val showSaveCancel by remember {
        derivedStateOf {
            folderSuffix != languageExportSettings.folderSuffix || fileName != languageExportSettings.fileName
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(folderSuffix, {
            folderSuffix = it
        }, Modifier.width(120.dp), singleLine = true, label = { Text(languageName) })
        Text("/", Modifier.align(Alignment.CenterVertically), fontSize = 36.sp)
        OutlinedTextField(fileName, {
            fileName = it
        }, Modifier.width(120.dp), singleLine = true)
        AnimatedVisibility(showSaveCancel) {
            val saveEnabled by remember {
                derivedStateOf {
                    (platform.exportPrefix.isNotEmpty() || folderSuffix.isNotBlank()) && fileName.isNotBlank()
                }
            }
            Row {
                IconButton({
                    onSave(languageExportSettings, folderSuffix, fileName)
                }, enabled = saveEnabled) {
                    AppTooltip(stringResource(Res.string.save)) {
                        Icon(Icons.Filled.Save, "save language export settings")
                    }
                }
                IconButton({
                    folderSuffix = languageExportSettings.folderSuffix
                    fileName = languageExportSettings.fileName
                }) {
                    AppTooltip(stringResource(Res.string.cancel)) {
                        Icon(Icons.Filled.Cancel, "cancel language export settings changes")
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        if (platform.exportPrefix.isNotEmpty() || folderSuffix.isNotEmpty() || fileName.isNotEmpty()) {
            Text("${platform.exportPrefix}${folderSuffix}/${fileName}${platform.fileStructure.fileExtension}")
        }
    }
}