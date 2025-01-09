@file:OptIn(ExperimentalSerializationApi::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.extension.moveFocusOnTab
import com.localization.offline.model.AppLocale
import com.localization.offline.model.AppScreen
import com.localization.offline.model.ExportToTranslator
import com.localization.offline.model.KnownProject
import com.localization.offline.model.Navigation
import com.localization.offline.service.LocaleService
import com.localization.offline.service.ProjectService
import com.localization.offline.ui.view.AppDialog
import com.localization.offline.ui.view.AppLocaleDropdown
import com.localization.offline.ui.view.AppTooltip
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import io.github.vinceglb.filekit.core.pickFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.choose_path
import localization.composeapp.generated.resources.create
import localization.composeapp.generated.resources.create_project
import localization.composeapp.generated.resources.create_project_q
import localization.composeapp.generated.resources.file_format_is_not_correct
import localization.composeapp.generated.resources.folder_is_not_empty
import localization.composeapp.generated.resources.import_for_translator
import localization.composeapp.generated.resources.name
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.open
import localization.composeapp.generated.resources.open_project
import localization.composeapp.generated.resources.path
import localization.composeapp.generated.resources.project_already_exist_in_folder
import localization.composeapp.generated.resources.project_not_found
import localization.composeapp.generated.resources.select_folder
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
import java.io.IOException

class ProjectsVM: ViewModel() {
    private val projectService = ProjectService()
    val appLocales = AppLocale.entries
    val currentAppLocale = LocaleService.current
    val knownProjects = projectService.getKnownProjects().toMutableStateList()
    val showProjectNotFound = MutableStateFlow(false)
    val showCreateProjectDialog = MutableStateFlow(false)
    val createProjectName = MutableStateFlow("")
    val createProjectPath = MutableStateFlow("")
    val createProjectEnabled = createProjectName.combine(createProjectPath) { name, path ->
        name.isNotBlank() && path.isNotEmpty()
    }
    val showProjectFolderNotEmptyDialog = MutableStateFlow(false)
    val showProjectExistInFolderDialog = MutableStateFlow(false)
    val showImportForTranslatorFormatError = MutableStateFlow(false)
    val navigation = MutableSharedFlow<Navigation?>()

    fun changeAppLocale(appLocale: AppLocale) {
        LocaleService.changeLocale(appLocale)
    }

    fun openProject(path: String) {
        if (projectService.openProject(File(path))) {
            viewModelScope.launch {
                navigation.emit(Navigation(AppScreen.Main, NavOptions.Builder().setPopUpTo(AppScreen.Projects::class, true).build()))
            }
        } else {
            knownProjects.removeIf { it.path == path }
            showProjectNotFound.value = true
        }
    }

    fun setCreateProjectDirectory(directory: PlatformDirectory?) {
        if (directory?.path == null) {
            return
        }
        createProjectPath.value = directory.path!!
    }

    fun closeCreateProjectDialog() {
        showCreateProjectDialog.value = false
        createProjectName.value = ""
        createProjectPath.value = ""
    }

    fun createProject(skipCheck: Boolean) {
        val name = createProjectName.value
        val path = createProjectPath.value
        val folder = File(path)
        if (!skipCheck && folder.list()?.isNotEmpty() == true) {
            if (DatabaseAccess.exists(folder)) {
                showProjectExistInFolderDialog.value = true
                return
            }
            showProjectFolderNotEmptyDialog.value = true
            return
        }
        closeCreateProjectDialog()
        viewModelScope.launch {
            navigation.emit(Navigation(AppScreen.Wizard(name, path), null))
        }
    }

    fun importForTranslator() {
        viewModelScope.launch {
            val exportToTranslatorFile = FileKit.pickFile(PickerType.File(listOf("json"))) ?: return@launch
            try {
                exportToTranslatorFile.file.inputStream().use {
                    Json.decodeFromStream<ExportToTranslator>(it)
                }
                navigation.emit(Navigation(AppScreen.Translator(exportToTranslatorFile.file.absolutePath, TranslatorVM.Type.Export.name), null))
            } catch (i: IllegalArgumentException) {
                showImportForTranslatorFormatError.value = true
            } catch (_: IOException) {}
        }
    }
}

@Composable
fun ProjectsScreen(navController: NavController) {
    val vm = koinViewModel<ProjectsVM>()
    val currentAppLocale by vm.currentAppLocale.collectAsStateWithLifecycle()
    val showProjectNotFound by vm.showProjectNotFound.collectAsStateWithLifecycle()
    val showCreateProjectDialog by vm.showCreateProjectDialog.collectAsStateWithLifecycle()
    val createProjectName by vm.createProjectName.collectAsStateWithLifecycle()
    val createProjectPath by vm.createProjectPath.collectAsStateWithLifecycle()
    val createProjectEnabled by vm.createProjectEnabled.collectAsStateWithLifecycle(false)
    val showProjectFolderNotEmptyDialog by vm.showProjectFolderNotEmptyDialog.collectAsStateWithLifecycle()
    val showProjectExistInFolderDialog by vm.showProjectExistInFolderDialog.collectAsStateWithLifecycle()
    val showImportForTranslatorFormatError by vm.showImportForTranslatorFormatError.collectAsStateWithLifecycle()
    val navigation by vm.navigation.collectAsStateWithLifecycle(null)

    val openFilePicker = rememberDirectoryPickerLauncher(
        stringResource(Res.string.open_project),
    ) {
        it?.path?.let { path ->
            vm.openProject(path)
        }
    }

    val createProjectPicker = rememberDirectoryPickerLauncher(
        stringResource(Res.string.create_project)
    ) {
        vm.setCreateProjectDirectory(it)
    }

    LaunchedEffect(navigation) {
        if (navigation != null) {
            navController.navigate(navigation!!.screen, navigation!!.navOptions)
        }
    }

    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        LazyColumn(Modifier.heightIn(0.dp, 300.dp).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items(vm.knownProjects, key = {it.path}) { project ->
                KnownProjectButton({
                    vm.openProject(project.path)
                }, project)
            }
        }
        SystemButton(openFilePicker::launch,
            Icons.Filled.Search, "open",
            stringResource(Res.string.open)
        )
        Spacer(Modifier.height(3.dp))
        SystemButton({vm.showCreateProjectDialog.value = true},
            Icons.Filled.Add, "create",
            stringResource(Res.string.create)
        )
        Spacer(Modifier.height(3.dp))
        SystemButton(vm::importForTranslator,
            Icons.Filled.ImportExport, "import for translator",
            stringResource(Res.string.import_for_translator)
        )
        Spacer(Modifier.height(3.dp))
        AppLocaleDropdown(vm.appLocales, currentAppLocale, vm::changeAppLocale)
    }

    if (showProjectNotFound) {
        AlertDialog({vm.showCreateProjectDialog.value = true},
            title = {
                Text(stringResource(Res.string.project_not_found))
            },
            confirmButton = {
            Button({vm.showProjectNotFound.value = false}) {
                Text(stringResource(Res.string.ok))
            }
        })
    }

    if (showProjectExistInFolderDialog) {
        AlertDialog({},
            title = {
            Text(stringResource(Res.string.project_already_exist_in_folder))
        }, confirmButton = {
            Button({vm.showProjectExistInFolderDialog.value = false}) {
                Text(stringResource(Res.string.ok))
            }
        })
    } else if (showProjectFolderNotEmptyDialog) {
        AlertDialog({},
            title =  {
                Text(stringResource(Res.string.folder_is_not_empty))
        }, text = {
            Text(stringResource(Res.string.create_project_q))
        }, confirmButton = {
            Button({
                vm.showProjectFolderNotEmptyDialog.value = false
                vm.createProject(true)
            }) {
                Text(stringResource(Res.string.yes))
            }
        }, dismissButton = {
            Button({vm.showProjectFolderNotEmptyDialog.value = false}) {
                Text(stringResource(Res.string.no))
            }
        })
    } else if (showCreateProjectDialog) {
        AppDialog(onDismissRequest = {vm.closeCreateProjectDialog()}) {
            OutlinedTextField(createProjectName, { it: String ->
                vm.createProjectName.value = it
            }, Modifier.width(TextFieldDefaults.MinWidth).moveFocusOnTab(), label = {
                Text(stringResource(Res.string.name))
            })
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(createProjectPath.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.choose_path), {}, Modifier.width(
                    OutlinedTextFieldDefaults.MinWidth), readOnly = true, singleLine = true, label = { Text(stringResource(Res.string.path)) })
                IconButton(createProjectPicker::launch) {
                    AppTooltip(stringResource(Res.string.select_folder)) {
                        Icon(Icons.Outlined.Folder, "path")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({vm.showCreateProjectDialog.value = false}) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(6.dp))
                Button({vm.createProject(false)}, enabled = createProjectEnabled) {
                    Text(stringResource(Res.string.create))
                }
            }
        }
    } else if (showImportForTranslatorFormatError) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.file_format_is_not_correct))
            },
            confirmButton = {
                Button({vm.showImportForTranslatorFormatError.value = false}) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}

@Composable
private fun KnownProjectButton(onClick: () -> Unit, knownProject: KnownProject) {
    Column(Modifier
        .width(240.dp)
        .heightIn(55.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.primary)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .clickable(MutableInteractionSource(), null, onClick = onClick)
    ) {
        Text(knownProject.name, style = MaterialTheme.typography.titleSmall, color = ButtonDefaults.buttonColors().contentColor)
        Spacer(Modifier.weight(1f))
        Text(knownProject.path, style = MaterialTheme.typography.bodySmall, color = ButtonDefaults.buttonColors().contentColor)
    }
}

@Composable
private fun SystemButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    text: String
) {
    Button(onClick, Modifier
        .size(240.dp, 40.dp), shape = RoundedCornerShape(10.dp), elevation = ButtonDefaults.buttonElevation(hoveredElevation = 0.dp)
    ) {
        Icon(imageVector, contentDescription)
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}