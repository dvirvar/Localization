package com.localization.offline.model

import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.grid
import localization.composeapp.generated.resources.list
import org.jetbrains.compose.resources.StringResource

enum class LanguageViewStyle(val stringResource: StringResource) {
    List(Res.string.list),
    Grid(Res.string.grid)
}