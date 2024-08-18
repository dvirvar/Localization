package com.localization.offline.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class Step(
    val id: String,
    val name: StringResource
)

@Composable
fun Stepper(modifier: Modifier, steps: List<Step>, currentStepIndex: Int) {
    Row(modifier, horizontalArrangement = Arrangement.Center, Alignment.CenterVertically){
        steps.fastForEachIndexed { index, step ->
            val isCurrentOrCompletedStep = index <= currentStepIndex
            val isCurrentStep = currentStepIndex == index
            Box(Modifier.size(24.dp).clip(CircleShape).background(if (isCurrentOrCompletedStep) MaterialTheme.colorScheme.primary else Color.LightGray), Alignment.Center) {
                Text(step.id, fontWeight = if(isCurrentStep) FontWeight.SemiBold else null)
            }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(step.name), fontWeight = if(isCurrentStep) FontWeight.SemiBold else null)
            if (index != steps.size - 1) {
                HorizontalDivider(Modifier.padding(horizontal = 10.dp).width(75.dp), 2.dp)
            }
        }
    }
}