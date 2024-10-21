package com.localization.offline.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.model.AppLocale
import com.localization.offline.model.AppScreen
import com.localization.offline.model.KnownProject
import com.localization.offline.service.LocaleService
import com.localization.offline.service.ProjectService
import com.localization.offline.ui.view.AppLocaleDropdown
import com.localization.offline.ui.view.AppTooltip
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.export_import
import localization.composeapp.generated.resources.languages
import localization.composeapp.generated.resources.localization
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.platforms
import localization.composeapp.generated.resources.project_not_found
import localization.composeapp.generated.resources.projects
import localization.composeapp.generated.resources.select_project
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class MainVM: ViewModel() {
    val project = ProjectService().getCurrentProject()!!
    val knownProjects = ProjectService().getKnownProjects().filterNot { project.id == it.id }
    val appLocales = AppLocale.entries
    val currentAppLocale = LocaleService.current
    val showProjectNotFound = MutableStateFlow(false)
    val screen = MutableSharedFlow<AppScreen?>()

    fun goToProjects() {
        ProjectService().closeCurrentProject()
        viewModelScope.launch {
            screen.emit(AppScreen.Projects)
        }
    }

    fun switchProject(knownProject: KnownProject) {
        if (ProjectService().switchProject(knownProject)) {
            viewModelScope.launch {
                screen.emit(AppScreen.Main)
            }
        } else {
            showProjectNotFound.value = true
        }
    }

    fun changeLanguage(appLocale: AppLocale) {
        LocaleService.changeLocale(appLocale)
    }
}

@Composable
fun MainScreen(navController: NavController) {
    val vm = koinViewModel<MainVM>()
    val currentAppLocale by vm.currentAppLocale.collectAsStateWithLifecycle()
    val showProjectNotFound by vm.showProjectNotFound.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle(null)
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(screen) {
        if (screen != null) {
            navController.navigate(screen!!) {
                popUpTo<AppScreen.Main> {
                    inclusive = true
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer), verticalAlignment = Alignment.CenterVertically) {
            TextButton(vm::goToProjects) {
                Text(stringResource(Res.string.projects), maxLines = 1)
            }
            HorizontalDivider(Modifier.width(10.dp), thickness = 2.dp)
            KnownProjectsDropdown(vm.knownProjects, vm.project.name, vm::switchProject)
            Spacer(Modifier.weight(1f))
            AppLocaleDropdown(vm.appLocales, currentAppLocale, vm::changeLanguage)
        }
        TabRow(selectedTabIndex, Modifier.fillMaxWidth(), MaterialTheme.colorScheme.secondaryContainer) {
            Tab.entries.fastForEachIndexed { index, tab ->
                Tab(selectedTabIndex == index,
                    { selectedTabIndex = index },
                    text = { Text(stringResource(tab.stringResource), maxLines = 2) }
                )
            }
        }
        when(Tab.entries[selectedTabIndex]) {
            Tab.Localization -> LocalizationScreen()
            Tab.Languages -> LanguagesScreen()
            Tab.Platforms -> PlatformsScreen()
            Tab.ExportImport -> ExportImportScreen(navController)
        }
    }

    if (showProjectNotFound) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.project_not_found))
            },
            confirmButton = {
                Button({ vm.showProjectNotFound.value = false }) {
                    Text(stringResource(Res.string.ok))
                }
            })
    }
}

private enum class Tab(val stringResource: StringResource) {
    Localization(Res.string.localization),
    Languages(Res.string.languages),
    Platforms(Res.string.platforms),
    ExportImport(Res.string.export_import)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnownProjectsDropdown(
    knownProjects: List<KnownProject>,
    currentProjectName: String,
    onProjectSelected: (KnownProject) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {expanded = !expanded}) {
        TextButton({}, Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)) {
            AppTooltip(stringResource(Res.string.select_project), enableUserInput = knownProjects.isNotEmpty()) {
                Text(currentProjectName, maxLines = 1)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, matchTextFieldWidth = false) {
            knownProjects.fastForEach { knownProject ->
                AppTooltip(knownProject.path) {
                    DropdownMenuItem(text = { Text(knownProject.name, maxLines = 1) }, onClick = {
                        onProjectSelected(knownProject)
                        expanded = false
                    })
                }
            }
        }
    }
}