package com.localization.offline.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@Composable
fun SaveableTextField(onSave: (String) -> Unit, originalValue: String, modifier: Modifier = Modifier, textFieldModifier: Modifier, label: @Composable (() -> Unit)? = null, singleLine: Boolean = false) {
    var value by remember(originalValue) { mutableStateOf(originalValue) }
    var showButtons by remember(originalValue) { mutableStateOf(false) }

    Column(modifier) {
        OutlinedTextField(value, {
            value = it
            showButtons = true
        }, textFieldModifier, label = label, textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr), singleLine = singleLine)
        if (showButtons) {
            Row {
                Button({
                    onSave(value)
                }) {
                    Text(stringResource(Res.string.save))
                }
                Spacer(Modifier.width(4.dp))
                Button({
                    value = originalValue
                    showButtons = false
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}