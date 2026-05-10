package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
import com.oterman.rundemo.presentation.feature.trainplan.formatPaceInput
import com.oterman.rundemo.presentation.feature.trainplan.parsePaceInput
import com.oterman.rundemo.ui.theme.RunTheme

@Composable
fun SingleGoalEditor(
    trainWholeType: TrainWholeType,
    distanceGoalStep: TrainStep?,
    timeGoalStep: TrainStep?,
    calGoalStep: TrainStep?,
    pacerGoalStep: TrainStep?,
    onDistanceChange: (Double?) -> Unit,
    onTimeChange: (Int?) -> Unit,
    onCaloriesChange: (Int?) -> Unit,
    onPacerChange: (Int?, Int?) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (trainWholeType) {
            TrainWholeType.DISTANCE -> DistanceGoal(distanceGoalStep, onDistanceChange, isEditMode)
            TrainWholeType.TIME -> TimeGoal(timeGoalStep, onTimeChange, isEditMode)
            TrainWholeType.CALORIES -> CaloriesGoal(calGoalStep, onCaloriesChange, isEditMode)
            TrainWholeType.PACER -> PacerGoal(pacerGoalStep, onPacerChange, isEditMode)
            TrainWholeType.SELF_DEFINE -> Unit
        }
    }
}

@Composable
private fun DistanceGoal(step: TrainStep?, onDistanceChange: (Double?) -> Unit, isEditMode: Boolean) {
    var text by remember(step?.distanceValue) { mutableStateOf(step?.distanceValue?.toString() ?: "") }
    GoalHeader("距离", Icons.Outlined.DirectionsRun)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onDistanceChange(it.toDoubleOrNull())
        },
        label = { Text("距离") },
        suffix = { Text("km") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = isEditMode,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimeGoal(step: TrainStep?, onTimeChange: (Int?) -> Unit, isEditMode: Boolean) {
    val totalSeconds = step?.timeGoalSeconds ?: 0
    var minutes by remember(totalSeconds) { mutableStateOf(if (totalSeconds > 0) (totalSeconds / 60).toString() else "") }
    var seconds by remember(totalSeconds) { mutableStateOf(if (totalSeconds > 0) (totalSeconds % 60).toString() else "") }
    GoalHeader("时间", Icons.Outlined.Timer)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = minutes,
            onValueChange = {
                minutes = it
                val m = it.toIntOrNull() ?: 0
                val s = seconds.toIntOrNull() ?: 0
                onTimeChange((m * 60 + s).takeIf { total -> total > 0 })
            },
            label = { Text("分钟") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = isEditMode,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = seconds,
            onValueChange = {
                seconds = it
                val m = minutes.toIntOrNull() ?: 0
                val s = it.toIntOrNull() ?: 0
                onTimeChange((m * 60 + s).takeIf { total -> total > 0 })
            },
            label = { Text("秒") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = isEditMode,
            modifier = Modifier.weight(1f)
        )
    }
    if (totalSeconds > 0) {
        Text(formatDuration(totalSeconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CaloriesGoal(step: TrainStep?, onCaloriesChange: (Int?) -> Unit, isEditMode: Boolean) {
    var text by remember(step?.caloriesValue) { mutableStateOf(step?.caloriesValue?.toString() ?: "") }
    GoalHeader("卡路里", Icons.Outlined.FavoriteBorder)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onCaloriesChange(it.toIntOrNull())
        },
        label = { Text("卡路里") },
        suffix = { Text("kcal") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        enabled = isEditMode,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PacerGoal(step: TrainStep?, onPacerChange: (Int?, Int?) -> Unit, isEditMode: Boolean) {
    var minText by remember(step?.minPace) { mutableStateOf(step?.minPace?.let { formatPaceInput(it) } ?: "") }
    var maxText by remember(step?.maxPace) { mutableStateOf(step?.maxPace?.let { formatPaceInput(it) } ?: "") }
    GoalHeader("配速员", Icons.Outlined.Speed)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = minText,
            onValueChange = {
                minText = it
                onPacerChange(parsePaceInput(it), parsePaceInput(maxText))
            },
            label = { Text("最快") },
            placeholder = { Text("4:30") },
            singleLine = true,
            enabled = isEditMode,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = maxText,
            onValueChange = {
                maxText = it
                onPacerChange(parsePaceInput(minText), parsePaceInput(it))
            },
            label = { Text("最慢") },
            placeholder = { Text("6:00") },
            singleLine = true,
            enabled = isEditMode,
            modifier = Modifier.weight(1f)
        )
    }
    Text("格式: 分:秒/公里", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GoalHeader(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = RunTheme.colorScheme.blue)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(2.dp))
}
