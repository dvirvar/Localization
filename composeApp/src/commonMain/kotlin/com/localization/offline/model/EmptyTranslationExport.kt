package com.localization.offline.model

import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.base_language
import localization.composeapp.generated.resources.dont_export
import org.jetbrains.compose.resources.StringResource

enum class EmptyTranslationExport(val stringResource: StringResource) {
    DontExport(Res.string.dont_export),
    BaseLanguage(Res.string.base_language)
}