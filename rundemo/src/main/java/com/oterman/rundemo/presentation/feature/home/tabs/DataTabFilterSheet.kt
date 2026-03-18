package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.InclusiveLevelIndicator

/**
 * 数据来源友好名称映射
 */
fun datasourceDisplayName(code: String?): String = when (code) {
    "GCN" -> "Garmin CN"
    "GGB" -> "Garmin Global"
    "COROS" -> "COROS"
    "HK" -> "Apple Health"
    else -> code ?: "未知"
}

private data class InclusiveLevelFilterOption(
    val level: Int,
    val label: String
)

private val INCLUSIVE_LEVEL_FILTER_OPTIONS = listOf(
    InclusiveLevelFilterOption(1, "统计+分析"),
    InclusiveLevelFilterOption(2, "仅统计"),
    InclusiveLevelFilterOption(0, "不统计"),
)

/**
 * 数据筛选 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DataTabFilterSheet(
    selectedInclusiveLevels: Set<Int>,
    selectedDatasources: Set<String>,
    availableDatasources: List<String>,
    hideEmptyMonths: Boolean,
    onDismiss: () -> Unit,
    onApply: (inclusiveLevels: Set<Int>, datasources: Set<String>, hideEmptyMonths: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempLevels by remember(selectedInclusiveLevels) { mutableStateOf(selectedInclusiveLevels) }
    var tempDatasources by remember(selectedDatasources) { mutableStateOf(selectedDatasources) }
    var tempHideEmpty by remember(hideEmptyMonths) { mutableStateOf(hideEmptyMonths) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 统计分析级别
            Text(
                text = "统计分析级别",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                INCLUSIVE_LEVEL_FILTER_OPTIONS.forEach { option ->
                    val selected = option.level in tempLevels
                    FilterChip(
                        selected = selected,
                        onClick = {
                            tempLevels = if (selected) tempLevels - option.level
                            else tempLevels + option.level
                        },
                        label = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                InclusiveLevelIndicator(
                                    inclusiveLevel = option.level,
                                    size = 8.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(option.label)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 数据来源
            if (availableDatasources.isNotEmpty()) {
                Text(
                    text = "数据来源",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableDatasources.forEach { ds ->
                        val selected = ds in tempDatasources
                        FilterChip(
                            selected = selected,
                            onClick = {
                                tempDatasources = if (selected) tempDatasources - ds
                                else tempDatasources + ds
                            },
                            label = { Text(datasourceDisplayName(ds)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 隐藏无数据月份
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐藏无数据月份",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = tempHideEmpty,
                    onCheckedChange = { tempHideEmpty = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempLevels = emptySet()
                        tempDatasources = emptySet()
                        tempHideEmpty = false
                        onApply(emptySet(), emptySet(), false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("重置")
                }
                Button(
                    onClick = { onApply(tempLevels, tempDatasources, tempHideEmpty) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
