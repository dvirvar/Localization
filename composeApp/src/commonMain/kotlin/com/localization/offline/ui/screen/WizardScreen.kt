@file:OptIn(ExperimentalMaterial3Api::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.db.CustomFormatSpecifierEntity
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.db.PlatformEntity
import com.localization.offline.extension.hasDuplicateBy
import com.localization.offline.model.AppScreen
import com.localization.offline.model.FileStructure
import com.localization.offline.model.FormatSpecifier
import com.localization.offline.service.ProjectService
import com.localization.offline.ui.view.GenericDropdown
import com.localization.offline.ui.view.Step
import com.localization.offline.ui.view.Stepper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.add
import localization.composeapp.generated.resources.back
import localization.composeapp.generated.resources.create
import localization.composeapp.generated.resources.duplicate_folder_and_file_detected
import localization.composeapp.generated.resources.duplicate_language_detected
import localization.composeapp.generated.resources.duplicate_platform_detected
import localization.composeapp.generated.resources.export
import localization.composeapp.generated.resources.export_description
import localization.composeapp.generated.resources.failed_to_create_project
import localization.composeapp.generated.resources.file_structure
import localization.composeapp.generated.resources.format_specifier
import localization.composeapp.generated.resources.format_specifiers
import localization.composeapp.generated.resources.format_specifiers_description
import localization.composeapp.generated.resources.languages
import localization.composeapp.generated.resources.next
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.platforms
import localization.composeapp.generated.resources.prefix
import localization.composeapp.generated.resources.regex
import localization.composeapp.generated.resources.remove
import localization.composeapp.generated.resources.with
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

class WizardVM(val name: String, val path: String): ViewModel() {
    val fileStructures = FileStructure.entries
    val formatSpecifiers = FormatSpecifier.entries
    val steps = listOf(Step("1", Res.string.languages), Step("2", Res.string.platforms))
    val currentStepIndex = MutableStateFlow(0)
    val languages = mutableStateListOf(LanguageEntity(Random.nextInt(), "", 0))
    val platforms = mutableStateListOf(PlatformEntity(Random.nextInt(), "", fileStructures.first(), FormatSpecifier.None,""))
    val customFormatSpecifiers = mutableStateListOf(mutableStateListOf<CustomFormatSpecifierEntity>())
    val languageExportSettings = mutableStateListOf(mutableStateListOf(LanguageExportSettingsEntity(languages.first().id, platforms.first().id, "", "")))
    val showDuplicateLanguagesDialog = MutableStateFlow(false)
    val showDuplicatePlatformsDialog = MutableStateFlow(false)
    val showDuplicateFolderAndFileDialog = MutableStateFlow(false)
    val showCreateProjectFailureDialog = MutableStateFlow(false)

    val nextButtonText = currentStepIndex.map {
        if (it == steps.size - 1) {
            Res.string.create
        } else {
            Res.string.next
        }
    }
    val enableNextButton = MutableStateFlow(false)
    val goBack = MutableSharedFlow<Unit?>()
    val screen = MutableSharedFlow<AppScreen?>()

    fun addLanguage() {
        val languageEntity = LanguageEntity(Random.nextInt(), "", languages.size)
        languages.add(languageEntity)
        languageExportSettings.fastForEachIndexed { index, codes ->
            codes.add(LanguageExportSettingsEntity(languageEntity.id, platforms[index].id, "", ""))
        }
        updateNextButtonEnableState()
    }

    fun editLanguage(index: Int, language: String) {
        languages[index] = languages[index].copy(name = language)
        updateNextButtonEnableState()
    }

    fun removeLanguage() {
        languages.removeLast()
        languageExportSettings.fastForEach {
            it.removeLast()
        }
        updateNextButtonEnableState()
    }

    fun addPlatform() {
        customFormatSpecifiers.add(mutableStateListOf())
        val platform = PlatformEntity(Random.nextInt(), "", fileStructures.first(), FormatSpecifier.None,"")
        platforms.add(platform)
        languageExportSettings.add(mutableStateListOf<LanguageExportSettingsEntity>().apply {
            languages.fastForEach {
                add(LanguageExportSettingsEntity(it.id, platform.id, "", ""))
            }
        })
        updateNextButtonEnableState()
    }

    fun editPlatform(index: Int, platform: String) {
        platforms[index] = platforms[index].copy(name = platform)
        updateNextButtonEnableState()
    }

    fun removePlatform() {
        platforms.removeLast()
        customFormatSpecifiers.removeLast()
        languageExportSettings.removeLast()
        updateNextButtonEnableState()
    }

    fun editFormatSpecifier(platformIndex: Int, formatSpecifier: FormatSpecifier) {
        if (platforms[platformIndex].formatSpecifier == FormatSpecifier.Custom && formatSpecifier != FormatSpecifier.Custom) {
            customFormatSpecifiers[platformIndex].clear()
        }
        platforms[platformIndex] = platforms[platformIndex].copy(formatSpecifier = formatSpecifier)
        updateNextButtonEnableState()
    }

    fun addCustomFormatSpecifier(platformIndex: Int) {
        customFormatSpecifiers[platformIndex].add(CustomFormatSpecifierEntity(Random.nextInt(), platforms[platformIndex].id, "", ""))
        updateNextButtonEnableState()
    }

    fun editCustomFormatSpecifier(platformIndex: Int, index: Int, from: String?, to: String?) {
        from?.let {
            customFormatSpecifiers[platformIndex][index] = customFormatSpecifiers[platformIndex][index].copy(from = it)
        }
        to?.let {
            customFormatSpecifiers[platformIndex][index] = customFormatSpecifiers[platformIndex][index].copy(to = it)
        }
        updateNextButtonEnableState()
    }

    fun removeCustomFormatSpecifier(platformIndex: Int) {
        customFormatSpecifiers[platformIndex].removeLast()
        updateNextButtonEnableState()
    }

    fun editFileStructure(platformIndex: Int, fileStructure: FileStructure) {
        platforms[platformIndex] = platforms[platformIndex].copy(fileStructure = fileStructure)
    }

    fun editExportPrefix(platformIndex: Int, prefix: String) {
        platforms[platformIndex] = platforms[platformIndex].copy(exportPrefix = prefix)
        updateNextButtonEnableState()
    }

    fun editLanguageFolderSuffix(platformIndex: Int, languageIndex: Int, folderSuffix: String) {
        languageExportSettings[platformIndex][languageIndex] = languageExportSettings[platformIndex][languageIndex].copy(folderSuffix = folderSuffix)
        updateNextButtonEnableState()
    }

    fun editLanguageFileName(platformIndex: Int, languageIndex: Int, fileName: String) {
        languageExportSettings[platformIndex][languageIndex] = languageExportSettings[platformIndex][languageIndex].copy(fileName = fileName)
        updateNextButtonEnableState()
    }

    private fun updateNextButtonEnableState() {
        if (currentStepIndex.value == 0) {
            enableNextButton.value = languages.all { lang -> lang.name.isNotBlank() }
        } else {
            var platformsAreValid = true
            for (platformIndex in platforms.indices) {
                val platform = platforms[platformIndex]
                if (platform.name.isBlank() || (platform.exportPrefix.isEmpty() && languageExportSettings[platformIndex].fastAny { it.folderSuffix.isEmpty() })) {
                    platformsAreValid = false
                    break
                }
            }
            enableNextButton.value = platformsAreValid &&
                    customFormatSpecifiers.all { it.all { it.from.isNotEmpty() && it.to.isNotEmpty() } } &&
                    languageExportSettings.all { it.all { it.fileName.isNotBlank() } }
        }
    }

    fun back() {
        if (currentStepIndex.value == 0) {
            viewModelScope.launch {
                goBack.emit(Unit)
            }
            return
        }
        currentStepIndex.value -= 1
        updateNextButtonEnableState()
    }

    fun next() {
        if (currentStepIndex.value < steps.size - 1) {
            if (!validateLanguages()) {
                return
            }
            currentStepIndex.value += 1
            updateNextButtonEnableState()
            return
        }
        if (!validatePlatforms()) {
            return
        }
        viewModelScope.launch {
            if (ProjectService().createProject(name, path, platforms, languages, languageExportSettings.flatten(), customFormatSpecifiers.flatten())) {
                screen.emit(AppScreen.Main)
            } else {
                showCreateProjectFailureDialog.value = true
            }
        }
    }

    private fun validateLanguages(): Boolean {
        if (languages.hasDuplicateBy { it.name }) {
            showDuplicateLanguagesDialog.value = true
            return false
        }
        return true
    }

    private fun validatePlatforms(): Boolean {
        if (platforms.hasDuplicateBy { it.name }) {
            showDuplicatePlatformsDialog.value = true
            return false
        }
        for (index in languageExportSettings.indices) {
            if (languageExportSettings[index].hasDuplicateBy { it.folderSuffix + it.fileName }) {
                showDuplicateFolderAndFileDialog.value = true
                return false
            }
        }
        return true
    }
}

@Composable
fun WizardScreen(navController: NavController, name: String, path: String) {
    val vm = koinViewModel<WizardVM>(parameters = {parametersOf(name, path)})
    val currentStepIndex by vm.currentStepIndex.collectAsStateWithLifecycle()
    val nextButtonText by vm.nextButtonText.collectAsStateWithLifecycle(Res.string.next)
    val enableNextButton by vm.enableNextButton.collectAsStateWithLifecycle()
    val goBack by vm.goBack.collectAsStateWithLifecycle(null)
    val screen by vm.screen.collectAsStateWithLifecycle(null)
    val showDuplicateLanguagesDialog by vm.showDuplicateLanguagesDialog.collectAsStateWithLifecycle()
    val showDuplicatePlatformsDialog by vm.showDuplicatePlatformsDialog.collectAsStateWithLifecycle()
    val showDuplicateFolderAndFileDialog by vm.showDuplicateFolderAndFileDialog.collectAsStateWithLifecycle()
    val showCreateProjectFailureDialog by vm.showCreateProjectFailureDialog.collectAsStateWithLifecycle()
    val bodyScrollState = rememberScrollState()

    LaunchedEffect(goBack) {
        if (goBack != null) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(screen) {
        if (screen != null) {
            navController.navigate(screen!!) {
                popUpTo<AppScreen.Projects> {
                    inclusive = true
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Stepper(Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.primaryContainer), vm.steps, currentStepIndex)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(bodyScrollState).padding(vertical = 36.dp)) {
            if (currentStepIndex == 0) {
                Languages(vm::addLanguage, vm::editLanguage, vm::removeLanguage, vm.languages)
            } else {
                Platforms(vm::addPlatform, vm::editPlatform, vm::removePlatform, vm.platforms)
                Spacer(Modifier.height(36.dp))
                FormatSpecifiers(vm::editFormatSpecifier, vm::addCustomFormatSpecifier, vm::editCustomFormatSpecifier, vm::removeCustomFormatSpecifier, vm.platforms, vm.formatSpecifiers, vm.customFormatSpecifiers)
                Spacer(Modifier.height(36.dp))
                Export(vm::editFileStructure, vm::editExportPrefix, vm::editLanguageFolderSuffix, vm::editLanguageFileName, vm.platforms, vm.fileStructures, vm.languages, vm.languageExportSettings)
            }
        }
        Row(Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp), Arrangement.End, Alignment.Bottom) {
            Button(vm::back) {
                Text(stringResource(Res.string.back))
            }
            Spacer(Modifier.width(6.dp))
            Button(vm::next, enabled = enableNextButton) {
                Text(stringResource(nextButtonText))
            }
        }
    }

    if (showDuplicateLanguagesDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.duplicate_language_detected))
            },
            confirmButton = {
                Button({ vm.showDuplicateLanguagesDialog.value = false }) {
                    Text(stringResource(Res.string.ok))
                }
            })
    } else if (showDuplicatePlatformsDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.duplicate_platform_detected))
            },
            confirmButton = {
                Button({ vm.showDuplicatePlatformsDialog.value = false }) {
                    Text(stringResource(Res.string.ok))
                }
            })
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
    } else if (showCreateProjectFailureDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.failed_to_create_project))
            },
            confirmButton = {
                Button({ vm.showCreateProjectFailureDialog.value = false }) {
                    Text(stringResource(Res.string.ok))
                }
            })
    }
}

@Composable
private fun Languages(addLanguage: () -> Unit, editLanguage: (Int, String) -> Unit, removeLanguage: () -> Unit, languages: List<LanguageEntity>) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp)
        .clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188))
        .padding(10.dp)) {
        Text(stringResource(Res.string.languages), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        languages.fastForEachIndexed { index, language ->
            OutlinedTextField(language.name, {
                editLanguage(index, it)
            }, Modifier.fillMaxWidth().padding(bottom = 10.dp), singleLine = true)
        }
        Row {
            Button({
                addLanguage()
                scope.launch {
                    delay(100)
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            }) {
                Text(stringResource(Res.string.add))
            }
            if (languages.size > 1) {
                Spacer(Modifier.width(10.dp))
                Button(removeLanguage) {
                    Text(stringResource(Res.string.remove))
                }
            }
        }
    }
}

@Composable
private fun Platforms(addPlatform: () -> Unit,
              editPlatform: (Int, String) -> Unit,
              removePlatform: () -> Unit,
              platforms: List<PlatformEntity>,) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp)
        .clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188))
        .padding(10.dp)) {
        Text(stringResource(Res.string.platforms), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        platforms.fastForEachIndexed { index, platform ->
            OutlinedTextField(platform.name, {
                editPlatform(index, it)
            }, Modifier.fillMaxWidth().padding(bottom = 10.dp), singleLine = true)
        }
        Row {
            Button({
                addPlatform()
                scope.launch {
                    delay(100)
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            }) {
                Text(stringResource(Res.string.add))
            }
            if (platforms.size > 1) {
                Spacer(Modifier.width(10.dp))
                Button(removePlatform) {
                    Text(stringResource(Res.string.remove))
                }
            }
        }
    }
}

@Composable
private fun FormatSpecifiers(
    editFormatSpecifier: (platformIndex: Int, FormatSpecifier) -> Unit,
    addCustomFormatSpecifier: (platformIndex: Int) -> Unit,
    editCustomFormatSpecifier: (platformIndex: Int, index: Int, from: String?, to: String?) -> Unit,
    removeCustomFormatSpecifier: (platformIndex: Int) -> Unit,
    platforms: List<PlatformEntity>,
    formatSpecifiers: List<FormatSpecifier>,
    customFormatSpecifiers: List<List<CustomFormatSpecifierEntity>>
) {
    var openFormatSpecifier by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp)
        .clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188))
        .padding(10.dp)) {
        Row(Modifier.fillMaxWidth()
            .clickable(MutableInteractionSource(), null) { openFormatSpecifier = !openFormatSpecifier },
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.format_specifiers), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(if (openFormatSpecifier) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                if (openFormatSpecifier) "close" else "open")
        }
        if (openFormatSpecifier) {
            Text(stringResource(Res.string.format_specifiers_description), style = MaterialTheme.typography.bodyMedium)
            platforms.fastForEachIndexed { platformIndex, platform ->
                Spacer(Modifier.height(10.dp))
                Text(platform.name, style = MaterialTheme.typography.titleSmall)
                GenericDropdown(formatSpecifiers.fastMap { stringResource(it.stringResource) },
                    formatSpecifiers.indexOf(platform.formatSpecifier), { editFormatSpecifier(platformIndex, formatSpecifiers[it]) },
                    { Text(stringResource(Res.string.format_specifier)) }
                )
                if (platform.formatSpecifier == FormatSpecifier.Custom) {
                    customFormatSpecifiers[platformIndex].fastForEachIndexed { index, cfs ->
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(cfs.from, {
                                editCustomFormatSpecifier(platformIndex, index, it, null)
                            }, Modifier.width(100.dp), placeholder = { Text(stringResource(Res.string.regex), Modifier.horizontalScroll(rememberScrollState()), maxLines = 1) }, singleLine = true)
                            Text(stringResource(Res.string.with), Modifier.padding(horizontal = 10.dp))
                            OutlinedTextField(cfs.to, {
                                editCustomFormatSpecifier(platformIndex, index, null, it)
                            }, Modifier.width(100.dp), singleLine = true)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button({addCustomFormatSpecifier(platformIndex)}) {
                            Text(stringResource(Res.string.add))
                        }
                        if (customFormatSpecifiers[platformIndex].isNotEmpty()) {
                            Spacer(Modifier.width(10.dp))
                            Button({removeCustomFormatSpecifier(platformIndex)}) {
                                Text(stringResource(Res.string.remove))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Export(
    editFileStructure: (platformIndex: Int, FileStructure) -> Unit,
    editExportPrefix: (Int, String) -> Unit,
    editLanguageFolderSuffix: (platformIndex: Int, languageIndex: Int, folderSuffix: String) -> Unit,
    editLanguageFileName: (platformIndex: Int, languageIndex: Int, fileName: String) -> Unit,
    platforms: List<PlatformEntity>,
    fileStructures: List<FileStructure>,
    languages: List<LanguageEntity>,
    languageExportSettings: List<List<LanguageExportSettingsEntity>>,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp)
        .clip(RoundedCornerShape(10.dp)).background(Color(249, 228, 188))
        .padding(10.dp)) {
        Text(stringResource(Res.string.export), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.export_description), style = MaterialTheme.typography.bodyMedium)
        platforms.fastForEachIndexed { platformIndex, platform ->
            Spacer(Modifier.height(10.dp))
            Text(platform.name, style = MaterialTheme.typography.titleSmall)
            GenericDropdown(fileStructures.fastMap { stringResource(it.stringResource) },
                fileStructures.indexOf(platform.fileStructure), { editFileStructure(platformIndex, fileStructures[it]) },
                { Text(stringResource(Res.string.file_structure)) }
            )
            OutlinedTextField(platform.exportPrefix, {
                editExportPrefix(platformIndex, it)
            }, singleLine = true, label = { Text(stringResource(Res.string.prefix)) })
            languageExportSettings[platformIndex].fastForEachIndexed { index, languageExportSettings ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(languageExportSettings.folderSuffix, {
                        editLanguageFolderSuffix(platformIndex, index, it)
                    }, Modifier.width(120.dp), singleLine = true, label = { Text(languages[index].name) })
                    Text("/", Modifier.align(Alignment.CenterVertically), fontSize = 36.sp)
                    OutlinedTextField(languageExportSettings.fileName, {
                        editLanguageFileName(platformIndex, index, it)
                    }, Modifier.width(120.dp), singleLine = true)
                    Spacer(Modifier.width(10.dp))
                    if (platform.exportPrefix.isNotEmpty() || languageExportSettings.folderSuffix.isNotEmpty() || languageExportSettings.fileName.isNotEmpty()) {
                        Text("${platform.exportPrefix}${languageExportSettings.folderSuffix}/${languageExportSettings.fileName}${platform.fileStructure.fileExtension}")
                    }
                }
            }
        }
    }
}