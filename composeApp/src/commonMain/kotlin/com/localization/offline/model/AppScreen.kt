package com.localization.offline.model

import kotlinx.serialization.Serializable

interface AppScreen{
    @Serializable
    data object Splash : AppScreen
    @Serializable
    data object Projects : AppScreen
    @Serializable
    data class Wizard(val name: String, val path: String): AppScreen
    @Serializable
    data object Main : AppScreen
}