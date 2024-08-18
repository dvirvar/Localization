package com.localization.offline.model

import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.printf
import localization.composeapp.generated.resources.apple_ecosystem
import localization.composeapp.generated.resources.i18n
import localization.composeapp.generated.resources.none
import localization.composeapp.generated.resources.custom
import org.jetbrains.compose.resources.StringResource

enum class FormatSpecifier(val stringResource: StringResource) {
    Printf(Res.string.printf),
    AppleEcosystem(Res.string.apple_ecosystem),
    I18n(Res.string.i18n),
    None(Res.string.none),
    Custom(Res.string.custom)
}