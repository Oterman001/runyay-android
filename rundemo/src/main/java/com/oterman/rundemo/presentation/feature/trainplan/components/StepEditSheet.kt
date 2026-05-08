package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditSheet(
    step: TrainStep,
    onSave: (TrainStep) -> Unit,
    onDismiss: () -> Unit
) {
    var goalType by remember { mutableStateOf(step.goalType) }
    var distanceValue by remember { mutableStateOf(step.distanceValue?.toString() ?: "") }
    var distanceUnit by remember { mutableStateOf(step.distanceUnit ?: "KM") }
    var timeMinutes by remember {
        mutableStateOf(step.timeGoalSeconds?.let { (it / 60).toString() } ?: "")
    }
    var timeSeconds by remember {
        mutableStateOf(step.timeGoalSeconds?.let { (it % 60).toString() } ?: "")
    }
    var intensityType by remember { mutableStateOf(step.intensityType) }
    var minHeartRate by remember { mutableStateOf(step.minHeartRate?.toString() ?: "") }
    var maxHeartRate by remember { mutableStateOf(step.maxHeartRate?.toString() ?: "") }
    var heartZoneType by remember { mutableStateOf(step.heartZoneType ?: "") }
    var minPace by remember { mutableStateOf(step.minPace?.let { formatPace(it) } ?: "") }
    var maxPace by remember { mutableStateOf(step.maxPace?.let { formatPace(it) } ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("编辑步骤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            // Goal type selector
            Text("目标类型", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            val goalOptions = listOf(TrainGoalType.DISTANCE to "距离", TrainGoalType.TIME to "时间")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                goalOptions.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = goalType == type,
                        onClick = { goalType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, goalOptions.size)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Goal value input
            when (goalType) {
                TrainGoalType.DISTANCE -> {
                    OutlinedTextField(
                        value = distanceValue,
                        onValueChange = { distanceValue = it },
                        label = { Text("距离") },
                        suffix = { Text(if (distanceUnit == "M") "m" else "km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TrainGoalType.TIME -> {
                    Row {
                        OutlinedTextField(
                            value = timeMinutes,
                            onValueChange = { timeMinutes = it },
                            label = { Text("分钟") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = timeSeconds,
                            onValueChange = { timeSeconds = it },
                            label = { Text("秒") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))

            // Intensity type
            Text("强度类型", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            val intensityOptions = listOf(null to "无", IntensityType.HEART_RATE to "心率", IntensityType.SPEED to "配速")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                intensityOptions.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = intensityType == type,
                        onClick = { intensityType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, intensityOptions.size)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Intensity value input
            when (intensityType) {
                IntensityType.HEART_RATE -> {
                    Row {
                        OutlinedTextField(
                            value = minHeartRate,
                            onValueChange = { minHeartRate = it },
                            label = { Text("最低心率") },
                            suffix = { Text("bpm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = maxHeartRate,
                            onValueChange = { maxHeartRate = it },
                            label = { Text("最高心率") },
                            suffix = { Text("bpm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                IntensityType.SPEED -> {
                    Row {
                        OutlinedTextField(
                            value = minPace,
                            onValueChange = { minPace = it },
                            label = { Text("最快配速") },
                            placeholder = { Text("4:30") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = maxPace,
                            onValueChange = { maxPace = it },
                            label = { Text("最慢配速") },
                            placeholder = { Text("6:00") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                null -> {}
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val updated = step.copy(
                            goalType = goalType,
                            distanceValue = if (goalType == TrainGoalType.DISTANCE) distanceValue.toDoubleOrNull() else step.distanceValue,
                            distanceUnit = if (goalType == TrainGoalType.DISTANCE) distanceUnit else step.distanceUnit,
                            timeGoalSeconds = if (goalType == TrainGoalType.TIME) {
                                val m = timeMinutes.toIntOrNull() ?: 0
                                val s = timeSeconds.toIntOrNull() ?: 0
                                (m * 60 + s).takeIf { it > 0 }
                            } else step.timeGoalSeconds,
                            intensityType = intensityType,
                            minHeartRate = if (intensityType == IntensityType.HEART_RATE) minHeartRate.toIntOrNull() else null,
                            maxHeartRate = if (intensityType == IntensityType.HEART_RATE) maxHeartRate.toIntOrNull() else null,
                            heartZoneType = if (intensityType == IntensityType.HEART_RATE) heartZoneType.takeIf { it.isNotBlank() } else null,
                            minPace = if (intensityType == IntensityType.SPEED) parsePace(minPace) else null,
                            maxPace = if (intensityType == IntensityType.SPEED) parsePace(maxPace) else null
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
            }
        }
    }
}

private fun formatPace(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

private fun parsePace(text: String): Int? {
    val parts = text.split(":")
    if (parts.size != 2) return null
    val min = parts[0].toIntOrNull() ?: return null
    val sec = parts[1].toIntOrNull() ?: return null
    return min * 60 + sec
}
