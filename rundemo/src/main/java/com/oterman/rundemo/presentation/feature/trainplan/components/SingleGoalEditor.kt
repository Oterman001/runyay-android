package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
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
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (trainWholeType) {
                TrainWholeType.DISTANCE -> {
                    Text("距离目标", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    var text by remember(distanceGoalStep?.distanceValue) {
                        mutableStateOf(distanceGoalStep?.distanceValue?.toString() ?: "")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onDistanceChange(it.toDoubleOrNull())
                        },
                        label = { Text("距离") },
                        suffix = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TrainWholeType.TIME -> {
                    Text("时间目标", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val totalSeconds = timeGoalStep?.timeGoalSeconds ?: 0
                    var minutes by remember(totalSeconds) {
                        mutableStateOf(if (totalSeconds > 0) (totalSeconds / 60).toString() else "")
                    }
                    var seconds by remember(totalSeconds) {
                        mutableStateOf(if (totalSeconds > 0) (totalSeconds % 60).toString() else "")
                    }
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                TrainWholeType.CALORIES -> {
                    Text("卡路里目标", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    var text by remember(calGoalStep?.caloriesValue) {
                        mutableStateOf(calGoalStep?.caloriesValue?.toString() ?: "")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onCaloriesChange(it.toIntOrNull())
                        },
                        label = { Text("卡路里") },
                        suffix = { Text("kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TrainWholeType.PACER -> {
                    Text("配速目标", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    var minText by remember(pacerGoalStep?.minPace) {
                        mutableStateOf(pacerGoalStep?.minPace?.let { formatPace(it) } ?: "")
                    }
                    var maxText by remember(pacerGoalStep?.maxPace) {
                        mutableStateOf(pacerGoalStep?.maxPace?.let { formatPace(it) } ?: "")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = {
                                minText = it
                                onPacerChange(parsePace(it), parsePace(maxText))
                            },
                            label = { Text("最快配速") },
                            placeholder = { Text("如 4:30") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("~")
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = {
                                maxText = it
                                onPacerChange(parsePace(minText), parsePace(it))
                            },
                            label = { Text("最慢配速") },
                            placeholder = { Text("如 6:00") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "格式: 分:秒/公里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                else -> {}
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
