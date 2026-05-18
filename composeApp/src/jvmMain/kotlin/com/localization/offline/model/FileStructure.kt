package com.localization.offline.model

import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.android_xml
import localization.composeapp.generated.resources.apple_strings
import localization.composeapp.generated.resources.json
import localization.composeapp.generated.resources.kmp_xml
import org.jetbrains.compose.resources.StringResource

enum class FileStructure(val stringResource: StringResource, val fileExtension: String) {
    KmpXml(Res.string.kmp_xml, ".xml"),
    AndroidXml(Res.string.android_xml, ".xml"),
    AppleStrings(Res.string.apple_strings, ".strings"),
    Json(Res.string.json, ".json")
}