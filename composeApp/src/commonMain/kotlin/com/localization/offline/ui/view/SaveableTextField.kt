package com.localization.offline.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@Composable
fun SaveableButtonsTextField(
    onSave: (String) -> Unit,
    originalValue: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false
) {
    var value by remember(originalValue) { mutableStateOf(originalValue) }
    var showButtons by remember(originalValue) { mutableStateOf(false) }

    Column(modifier) {
        OutlinedTextField(value, {
            value = it
            showButtons = true
        }, textFieldModifier, label = label, singleLine = singleLine)
        if (showButtons) {
            ButtonsRow({
                onSave(value)
            }, {
                value = originalValue
                showButtons = false
            })
        }
    }
}

@Composable
private fun ButtonsRow(onSave: () -> Unit, onCancel: () -> Unit) {
    Row {
        Button(onSave) {
            Text(stringResource(Res.string.save))
        }
        Spacer(Modifier.width(4.dp))
        Button(onCancel) {
            Text(stringResource(Res.string.cancel))
        }
    }
}

@Composable
fun SaveableIconsTextField(
    onSave: (String) -> Unit,
    originalValue: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false
) {
    var value by remember(originalValue) { mutableStateOf(originalValue) }
    var showButtons by remember(originalValue) { mutableStateOf(false) }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, {
            value = it
            showButtons = true
        }, textFieldModifier, label = label, singleLine = singleLine)
        if (showButtons) {
            IconButton({
                onSave(value)
            }) {
                Icon(Icons.Filled.Save, "save")
            }
            IconButton({
                value = originalValue
                showButtons = false
            }) {
                Icon(Icons.Filled.Cancel, "cancel")
            }
        }
    }
}