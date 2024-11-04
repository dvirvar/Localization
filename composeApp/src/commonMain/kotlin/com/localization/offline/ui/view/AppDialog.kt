package com.localization.offline.ui.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AppDialog(onDismissRequest: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val columnScrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismissRequest) {
        Box(contentAlignment = Alignment.Center) {
            Column(Modifier.width(IntrinsicSize.Max).background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp)).verticalScroll(columnScrollState).padding(16.dp), content = content)
            VerticalScrollbar(rememberScrollbarAdapter(columnScrollState), Modifier.align(Alignment.CenterEnd))
        }
    }
}
