package com.localization.offline.model

import com.localization.offline.ui.screen.TranslatorVM
import kotlinx.serialization.Serializable

interface AppScreen{
    @Serializable
    data object Splash : AppScreen
    @Serializable
    data object Projects : AppScreen
    @Serializable
    class Translator: AppScreen {
        val filePath: String
        private val typeName: String
        val type: TranslatorVM.Type get() = TranslatorVM.Type.valueOf(typeName)

        constructor(filePath: String, type: TranslatorVM.Type) {
            this.filePath = filePath
            typeName = type.name
        }
    }
    @Serializable
    data class Wizard(val name: String, val path: String): AppScreen
    @Serializable
    data object Main : AppScreen
}