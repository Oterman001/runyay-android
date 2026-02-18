package com.oterman.rundemo.presentation.feature.settings.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.GoalType
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Run Goal Setting Page
 * Matches iOS RunGoalSetFullPage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunGoalSetPage(
    viewModel: RunGoalSetViewModel = viewModel(
        factory = RunGoalSetViewModelFactory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Show success and navigate back
    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            snackbarHostState.showSnackbar("目标设置已保存")
            viewModel.clearSuccessFlag()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "目标设置",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Goal Enable Toggle
            GoalToggleSection(
                enabled = uiState.goalEnabled,
                onToggle = { viewModel.setGoalEnabled(it) }
            )

            if (uiState.goalEnabled) {
                Spacer(modifier = Modifier.height(24.dp))

                // Goal Type Picker
                GoalTypePickerSection(
                    selectedType = uiState.goalType,
                    onTypeSelected = { viewModel.setGoalType(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Goal Input Section
                if (uiState.goalType == GoalType.DISTANCE) {
                    DistanceGoalInputSection(
                        monthInput = uiState.monthDistanceInput,
                        yearInput = uiState.yearDistanceInput,
                        onMonthInputChange = { viewModel.setMonthDistanceInput(it) },
                        onYearInputChange = { viewModel.setYearDistanceInput(it) },
                        onAutoFillYear = { viewModel.autoFillYearFromMonth() },
                        onAutoFillMonth = { viewModel.autoFillMonthFromYear() }
                    )
                } else {
                    DurationGoalInputSection(
                        monthInput = uiState.monthDurationInput,
                        yearInput = uiState.yearDurationInput,
                        onMonthInputChange = { viewModel.setMonthDurationInput(it) },
                        onYearInputChange = { viewModel.setYearDurationInput(it) },
                        onAutoFillYear = { viewModel.autoFillYearFromMonth() },
                        onAutoFillMonth = { viewModel.autoFillMonthFromYear() }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveGoalSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RunTheme.colorScheme.blue
                )
            ) {
                Text(
                    text = "保存设置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoalToggleSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "启用目标追踪",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "开启后在首页显示目标进度",
                fontSize = 13.sp,
                color = SecondaryTextColor
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = RunTheme.colorScheme.blue
            )
        )
    }
}

@Composable
private fun GoalTypePickerSection(
    selectedType: GoalType,
    onTypeSelected: (GoalType) -> Unit
) {
    Column {
        Text(
            text = "目标类型",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SecondaryTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedType == GoalType.DISTANCE,
                onClick = { onTypeSelected(GoalType.DISTANCE) },
                label = {
                    Text(
                        text = "距离目标",
                        fontWeight = if (selectedType == GoalType.DISTANCE)
                            FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.15f),
                    selectedLabelColor = RunTheme.colorScheme.blue
                )
            )

            FilterChip(
                selected = selectedType == GoalType.DURATION,
                onClick = { onTypeSelected(GoalType.DURATION) },
                label = {
                    Text(
                        text = "时长目标",
                        fontWeight = if (selectedType == GoalType.DURATION)
                            FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.15f),
                    selectedLabelColor = RunTheme.colorScheme.blue
                )
            )
        }
    }
}

@Composable
private fun DistanceGoalInputSection(
    monthInput: String,
    yearInput: String,
    onMonthInputChange: (String) -> Unit,
    onYearInputChange: (String) -> Unit,
    onAutoFillYear: () -> Unit,
    onAutoFillMonth: () -> Unit
) {
    Column {
        Text(
            text = "距离目标设置",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SecondaryTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Month distance input
        GoalInputField(
            label = "月度目标",
            value = monthInput,
            onValueChange = onMonthInputChange,
            unit = "公里",
            placeholder = "10-1000",
            onAutoFill = onAutoFillYear,
            autoFillText = "自动计算年度"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Year distance input
        GoalInputField(
            label = "年度目标",
            value = yearInput,
            onValueChange = onYearInputChange,
            unit = "公里",
            placeholder = "100-12000",
            onAutoFill = onAutoFillMonth,
            autoFillText = "自动计算月度"
        )
    }
}

@Composable
private fun DurationGoalInputSection(
    monthInput: String,
    yearInput: String,
    onMonthInputChange: (String) -> Unit,
    onYearInputChange: (String) -> Unit,
    onAutoFillYear: () -> Unit,
    onAutoFillMonth: () -> Unit
) {
    Column {
        Text(
            text = "时长目标设置",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SecondaryTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Month duration input
        GoalInputField(
            label = "月度目标",
            value = monthInput,
            onValueChange = onMonthInputChange,
            unit = "小时",
            placeholder = "5-200",
            onAutoFill = onAutoFillYear,
            autoFillText = "自动计算年度"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Year duration input
        GoalInputField(
            label = "年度目标",
            value = yearInput,
            onValueChange = onYearInputChange,
            unit = "小时",
            placeholder = "50-2000",
            onAutoFill = onAutoFillMonth,
            autoFillText = "自动计算月度"
        )
    }
}

@Composable
private fun GoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    placeholder: String,
    onAutoFill: () -> Unit,
    autoFillText: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = autoFillText,
                fontSize = 13.sp,
                color = RunTheme.colorScheme.blue,
                modifier = Modifier
                    .clickable { onAutoFill() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Only allow numbers
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = placeholder,
                        color = SecondaryTextColor
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Text(
                text = unit,
                fontSize = 15.sp,
                color = SecondaryTextColor,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RunGoalSetPagePreview() {
    RunGoalSetPage()
}
