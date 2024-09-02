package com.localization.offline.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.model.AppScreen
import com.localization.offline.service.ProjectService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.export
import localization.composeapp.generated.resources.languages
import localization.composeapp.generated.resources.localization
import localization.composeapp.generated.resources.platforms
import localization.composeapp.generated.resources.projects
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class MainVM: ViewModel() {
    val projectName = ProjectService().getCurrentProject()?.name ?: ""
    val screen = MutableSharedFlow<AppScreen?>()

    fun goToProjects() {
        ProjectService().closeCurrentProject()
        viewModelScope.launch {
            screen.emit(AppScreen.Projects)
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    val vm = koinViewModel<MainVM>()
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(vm::goToProjects) {
                Text(stringResource(Res.string.projects), maxLines = 1)
            }
            HorizontalDivider(Modifier.width(10.dp), thickness = 2.dp)
            TextButton({}) {
                Text(vm.projectName, maxLines = 1)
            }
        }
        TabRow(selectedTabIndex, Modifier.fillMaxWidth()) {
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
            Tab.Platforms -> TODO()
            Tab.Export -> TODO()
        }
    }
}

private enum class Tab(val stringResource: StringResource) {
    Localization(Res.string.localization),
    Languages(Res.string.languages),
    Platforms(Res.string.platforms),
    Export(Res.string.export)
}