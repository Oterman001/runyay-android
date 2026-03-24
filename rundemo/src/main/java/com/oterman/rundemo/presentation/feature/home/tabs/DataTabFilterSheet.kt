package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
private fun FilterSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

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
    selectedOutdoorTypes: Set<Int>,
    onDismiss: () -> Unit,
    onApply: (inclusiveLevels: Set<Int>, datasources: Set<String>, hideEmptyMonths: Boolean, outdoorTypes: Set<Int>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempLevels by remember(selectedInclusiveLevels) { mutableStateOf(selectedInclusiveLevels) }
    var tempDatasources by remember(selectedDatasources) { mutableStateOf(selectedDatasources) }
    var tempHideEmpty by remember(hideEmptyMonths) { mutableStateOf(hideEmptyMonths) }
    var tempOutdoorTypes by remember(selectedOutdoorTypes) { mutableStateOf(selectedOutdoorTypes) }

    val activeFilterCount = listOf(
        tempLevels.isNotEmpty(),
        tempDatasources.isNotEmpty(),
        tempHideEmpty,
        tempOutdoorTypes.isNotEmpty()
    ).count { it }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Row: 重置 | 筛选+badge | ✕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        tempLevels = emptySet()
                        tempDatasources = emptySet()
                        tempHideEmpty = false
                        tempOutdoorTypes = emptySet()
                    }
                ) {
                    Text(
                        text = "重置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "筛选",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 滚动内容区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // 统计分析级别
                FilterSectionHeader(icon = Icons.Outlined.BarChart, title = "统计分析级别")
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(20.dp))

                // 数据来源
                if (availableDatasources.isNotEmpty()) {
                    FilterSectionHeader(icon = Icons.Outlined.Watch, title = "数据来源")
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 室内/室外
                FilterSectionHeader(icon = Icons.Outlined.DirectionsRun, title = "室内/室外")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val outdoorSelected = 0 in tempOutdoorTypes
                    FilterChip(
                        selected = outdoorSelected,
                        onClick = {
                            tempOutdoorTypes = if (outdoorSelected) tempOutdoorTypes - 0 else tempOutdoorTypes + 0
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsRun,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text("室外跑") }
                    )
                    val indoorSelected = 1 in tempOutdoorTypes
                    FilterChip(
                        selected = indoorSelected,
                        onClick = {
                            tempOutdoorTypes = if (indoorSelected) tempOutdoorTypes - 1 else tempOutdoorTypes + 1
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text("室内跑") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(20.dp))

                // 显示选项
                FilterSectionHeader(icon = Icons.Default.CalendarViewMonth, title = "显示选项")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "隐藏无数据月份",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "不显示没有跑步记录的月份",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tempHideEmpty,
                        onCheckedChange = { tempHideEmpty = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 底部固定按钮
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onApply(tempLevels, tempDatasources, tempHideEmpty, tempOutdoorTypes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("确认")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
