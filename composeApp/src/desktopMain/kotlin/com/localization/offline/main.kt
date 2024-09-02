package com.localization.offline

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.localization.offline.ui.screen.LanguagesVM
import com.localization.offline.ui.screen.LocalizationVM
import com.localization.offline.ui.screen.MainVM
import com.localization.offline.ui.screen.ProjectsVM
import com.localization.offline.ui.screen.SplashVM
import com.localization.offline.ui.screen.WizardVM
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val viewModels = module {
    viewModel { SplashVM() }
    viewModel { ProjectsVM() }
    viewModel { parameters -> WizardVM(parameters.get(), parameters.get()) }
    viewModel { MainVM() }
    viewModel { LocalizationVM() }
    viewModel { LanguagesVM() }
}

fun main() = application {
    startKoin {
        modules(viewModels)
    }
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(position = WindowPosition(Alignment.Center)),
        title = "Localization",
    ) {
        App()
    }
}