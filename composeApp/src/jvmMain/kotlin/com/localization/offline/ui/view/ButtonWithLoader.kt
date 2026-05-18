package com.localization.offline.ui.view

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ButtonWithLoader(onClick: () -> Unit, enabled: Boolean, showLoader: Boolean, text: String, maxLines: Int = 1) {
    Button({
        if (!showLoader) {
            onClick()
        }
    }, enabled = enabled) {
        if (showLoader) {
            CircularProgressIndicator(Modifier.size(14.dp), color = ButtonDefaults.buttonColors().contentColor, strokeWidth = 1.5.dp)
        } else {
            Text(text, maxLines = maxLines)
        }
    }
}