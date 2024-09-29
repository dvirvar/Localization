package com.localization.offline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.localization.offline.model.AppScreen
import com.localization.offline.ui.screen.MainScreen
import com.localization.offline.ui.screen.ProjectsScreen
import com.localization.offline.ui.screen.SplashScreen
import com.localization.offline.ui.screen.TranslatorScreen
import com.localization.offline.ui.screen.WizardScreen
import com.localization.offline.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        NavHost(
            navController,
            startDestination = AppScreen.Splash,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<AppScreen.Splash> {
                SplashScreen(navController)
            }
            composable<AppScreen.Projects> {
                ProjectsScreen(navController)
            }
            composable<AppScreen.Translator> {
                val translator = it.toRoute<AppScreen.Translator>()
                TranslatorScreen(navController, translator.filePath)
            }
            composable<AppScreen.Wizard> {
                val wizard = it.toRoute<AppScreen.Wizard>()
                WizardScreen(navController, wizard.name, wizard.path)
            }
            composable<AppScreen.Main> {
                MainScreen(navController)
            }
        }
    }
}