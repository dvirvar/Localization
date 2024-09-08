package com.localization.offline.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.model.AppScreen
import com.localization.offline.model.KnownProject
import com.localization.offline.service.ProjectService
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.create
import localization.composeapp.generated.resources.create_project
import localization.composeapp.generated.resources.create_project_q
import localization.composeapp.generated.resources.folder_is_not_empty
import localization.composeapp.generated.resources.name
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.open
import localization.composeapp.generated.resources.open_project
import localization.composeapp.generated.resources.path
import localization.composeapp.generated.resources.project_already_exist_in_folder
import localization.composeapp.generated.resources.project_not_found
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

class ProjectsVM: ViewModel() {
    private val projectService = ProjectService()
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
    val screen = MutableSharedFlow<AppScreen?>()

    fun openProject(path: String) {
        if (projectService.openProject(File(path))) {
            viewModelScope.launch {
                screen.emit(AppScreen.Main)
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
            screen.emit(AppScreen.Wizard(name, path))
        }
    }
}

@Composable
fun ProjectsScreen(navController: NavController) {
    val vm = koinViewModel<ProjectsVM>()
    val showProjectNotFound by vm.showProjectNotFound.collectAsStateWithLifecycle()
    val showCreateProjectDialog by vm.showCreateProjectDialog.collectAsStateWithLifecycle()
    val createProjectName by vm.createProjectName.collectAsStateWithLifecycle()
    val createProjectPath by vm.createProjectPath.collectAsStateWithLifecycle()
    val createProjectEnabled by vm.createProjectEnabled.collectAsStateWithLifecycle(false)
    val showProjectFolderNotEmptyDialog by vm.showProjectFolderNotEmptyDialog.collectAsStateWithLifecycle()
    val showProjectExistInFolderDialog by vm.showProjectExistInFolderDialog.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle(null)

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

    LaunchedEffect(screen) {
        if (screen != null) {
            navController.navigate(screen!!)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Arrangement.Center, Alignment.CenterHorizontally) {
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
        val localDensity = LocalDensity.current
        var textFieldWidth by remember { mutableStateOf(0.dp) }
        Dialog(onDismissRequest = {vm.closeCreateProjectDialog()}) {
            Box(Modifier.wrapContentSize(unbounded = true).background(Color.White).padding(16.dp)) {
                Column {
                    OutlinedTextField(createProjectName, { it: String ->
                        vm.createProjectName.value = it
                    }, Modifier.onSizeChanged {
                        with(localDensity) {
                            textFieldWidth = it.width.toDp()
                        }
                    }, placeholder = {
                        Text(stringResource(Res.string.name))
                    }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.path))
                    Row(Modifier.width(textFieldWidth), verticalAlignment = Alignment.CenterVertically) {
                        Text(createProjectPath, Modifier.weight(1f), maxLines = 1, fontSize = 14.sp, overflow = TextOverflow.Ellipsis, softWrap = false)
                        IconButton(createProjectPicker::launch) {
                            Icon(Icons.Outlined.Folder, "path")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button({vm.createProject(false)}, Modifier.align(Alignment.CenterHorizontally), enabled = createProjectEnabled) {
                        Text(stringResource(Res.string.create))
                    }
                }
            }
        }
    }
}

@Composable
private fun KnownProjectButton(onClick: () -> Unit, knownProject: KnownProject) {
    Column(Modifier
        .size(240.dp, 55.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.primary)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .clickable(MutableInteractionSource(), null, onClick = onClick)
    ) {
        Text(knownProject.name, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        Text(knownProject.path, style = MaterialTheme.typography.bodySmall)
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