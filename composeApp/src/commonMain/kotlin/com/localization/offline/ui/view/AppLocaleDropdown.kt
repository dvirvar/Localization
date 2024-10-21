package com.localization.offline.ui.view

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.localization.offline.model.AppLocale
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.select_language
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLocaleDropdown(
    appLocales: List<AppLocale>,
    currentAppLocale: AppLocale,
    onAppLocaleSelected: (AppLocale) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {expanded = !expanded}) {
        TextButton({}, Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)) {
            AppTooltip(stringResource(Res.string.select_language)) {
                Text(currentAppLocale.languageName, maxLines = 1)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, matchTextFieldWidth = false) {
            appLocales.fastForEach { appLocale ->
                DropdownMenuItem(text = { Text(appLocale.languageName, maxLines = 1) }, onClick = {
                    onAppLocaleSelected(appLocale)
                    expanded = false
                })
            }
        }
    }
}