package com.localization.offline.ui.view

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    textFieldTrailing: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    readOnly: Boolean = false
) {
    var value by remember(originalValue) { mutableStateOf(originalValue) }
    val showButtons by remember(value, originalValue) {
        derivedStateOf {
            value != originalValue
        }
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value, {
                value = it
            }, Modifier.weight(1f), label = label, singleLine = singleLine, readOnly = readOnly)
            textFieldTrailing?.invoke()
        }
        AnimatedVisibility(showButtons) {
            ButtonsRow({
                onSave(value)
            }, {
                value = originalValue
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
    singleLine: Boolean = false,
    readOnly: Boolean = false
) {
    var value by remember(originalValue) { mutableStateOf(originalValue) }
    val showButtons by remember {
        derivedStateOf {
            value != originalValue
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, {
            value = it
        }, textFieldModifier, label = label, singleLine = singleLine, readOnly = readOnly)
        AnimatedVisibility(showButtons) {
            Row {
                IconButton({
                    onSave(value)
                }) {
                    AppTooltip(stringResource(Res.string.save)) {
                        Icon(Icons.Filled.Save, "save")
                    }
                }
                IconButton({
                    value = originalValue
                }) {
                    AppTooltip(stringResource(Res.string.cancel)) {
                        Icon(Icons.Filled.Cancel, "cancel")
                    }
                }
            }
        }
    }
}