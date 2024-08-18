package com.localization.offline.ui.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel

class MainVM: ViewModel() {

}

@Composable
fun MainScreen(navController: NavController) {
    val vm = koinViewModel<MainVM>()
}