package com.localization.offline.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.db.DatabaseAccess
import com.localization.offline.model.AppScreen
import com.localization.offline.service.LocalDataService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.project_not_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

class SplashVM: ViewModel() {
    val screen = MutableSharedFlow<AppScreen?>(1)
    val showProjectNotFound = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            if (LocalDataService.projectPath == null) {
                screen.emit(AppScreen.Projects)
            } else {
                val folder = File(LocalDataService.projectPath!!)
                if (DatabaseAccess.exists(folder)) {
                    DatabaseAccess.init(folder)
                    screen.emit(AppScreen.Main)
                } else {
                    showProjectNotFound.emit(true)
                }
            }
        }
    }

    fun goTo(screen: AppScreen) {
        viewModelScope.launch {
            this@SplashVM.screen.emit(screen)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    val vm = koinViewModel<SplashVM>()
    val screen by vm.screen.collectAsStateWithLifecycle(null)
    val showProjectNotFound by vm.showProjectNotFound.collectAsStateWithLifecycle()

    LaunchedEffect(screen) {
        if (screen != null) {
            navController.navigate(screen!!) {
                popUpTo<AppScreen.Splash> {
                    inclusive = true
                }
            }
        }
    }
    if (showProjectNotFound) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.project_not_found))
            },
            confirmButton = {
                Button({
                    vm.goTo(AppScreen.Projects)
                }) {
                    Text(stringResource(Res.string.ok))
                }
            })
    }
}