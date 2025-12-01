@file:OptIn(ExperimentalMaterial3Api::class)

package com.localization.offline.ui.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

@Composable
fun AppTooltip(
    text: String,
    state: TooltipState,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit
) {
    TooltipBox(
        rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        enableUserInput = enableUserInput,
        content = content
    )
}

@Composable
fun AppTooltip(
    text: String,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit
) {
    AppTooltip(text, rememberTooltipState(isPersistent = true), enableUserInput, content)
}