package com.localization.offline.model

import androidx.navigation.NavOptions

data class Navigation(
    val screen: AppScreen,
    val navOptions: NavOptions?
)