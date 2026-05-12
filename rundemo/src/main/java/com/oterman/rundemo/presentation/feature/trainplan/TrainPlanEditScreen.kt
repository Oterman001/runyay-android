package com.oterman.rundemo.presentation.feature.trainplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.components.SingleGoalEditor
import com.oterman.rundemo.presentation.feature.trainplan.components.StepEditSheet
import com.oterman.rundemo.presentation.feature.trainplan.components.TrainBlockCard
import com.oterman.rundemo.ui.theme.RunTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPlanEditScreen(
    planId: String? = null,
    date: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: TrainPlanEditViewModel = viewModel(
        factory = TrainPlanEditViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val mainBlockStartIndex = remember(
        uiState.trainWholeType,
        uiState.warmupBlock,
        uiState.mainBlocks.size
    ) {
        if (uiState.trainWholeType == TrainWholeType.SELF_DEFINE) {
            6 + if (uiState.warmupBlock != null) 1 else 0
        } else {
            -1
        }
    }
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = from.index - mainBlockStartIndex
        val toIndex = to.index - mainBlockStartIndex
        if (uiState.isEditMode && fromIndex in uiState.mainBlocks.indices && toIndex in uiState.mainBlocks.indices) {
            viewModel.moveMainBlock(fromIndex, toIndex)
        }
    }

    LaunchedEffect(planId, date) {
        viewModel.init(planId, date)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    uiState.editingStep?.let { editing ->
        StepEditSheet(
            step = editing.step,
            onSave = { viewModel.saveStepEdit(it) },
            onDismiss = { viewModel.dismissStepEditor() }
        )
    }

    if (uiState.showDatePicker) {
        PlanDatePickerDialog(
            selectedDate = uiState.scheduledDate,
            onDismiss = { viewModel.toggleDatePicker(false) },
            onDateSelected = { viewModel.onDateChange(it) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        if (uiState.isNewPlan) "新增课程" else if (uiState.isEditMode) "编辑课程" else "课程详情",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!uiState.isEditMode) {
                        // Details mode has only the floating edit action.
                    } else if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(onClick = { viewModel.savePlan() }) {
                            Text("保存", color = RunTheme.colorScheme.blue)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading && uiState.isEditMode && uiState.trainWholeType == TrainWholeType.SELF_DEFINE) {
                TrainBlockActionBar(
                    onAddTrain = { viewModel.addMainBlock() },
                    onAddRecovery = { viewModel.addRecoveryBlock() },
                    onAddInterval = { viewModel.addIntervalBlock() }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isLoading && !uiState.isEditMode && viewModel.canEditCurrentPlan()) {
                FloatingActionButton(
                    onClick = { viewModel.enterEditMode() },
                    containerColor = RunTheme.colorScheme.blue
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle("基础信息") }
            item {
                BasicInfoCard(
                    name = uiState.name,
                    onNameChange = viewModel::onNameChange,
                    scheduledDate = uiState.scheduledDate,
                    onDateClick = { viewModel.toggleDatePicker(true) },
                    locationType = uiState.locationType,
                    onLocationTypeChange = viewModel::onLocationTypeChange,
                    trainWholeType = uiState.trainWholeType,
                    onTrainWholeTypeChange = viewModel::onTrainWholeTypeChange,
                    isEditMode = uiState.isEditMode
                )
            }

            if (uiState.trainWholeType == TrainWholeType.PACER) {
                item { SectionTitle("配速员") }
                item { PacerSummaryCard(uiState.pacerGoalStep?.minPace, uiState.pacerGoalStep?.maxPace) }
            } else if (uiState.trainWholeType != TrainWholeType.CALORIES) {
                item { SectionTitle("整体预估") }
                item {
                    TotalSummaryCard(
                        blocks = listOfNotNull(uiState.warmupBlock) + uiState.mainBlocks + listOfNotNull(uiState.cooldownBlock),
                        trainWholeType = uiState.trainWholeType,
                        distanceGoal = uiState.distanceGoalStep?.distanceMeters() ?: 0.0,
                        timeGoalSeconds = uiState.timeGoalStep?.timeGoalSeconds ?: 0
                    )
                }
            }

            if (uiState.trainWholeType == TrainWholeType.SELF_DEFINE) {
                item { SectionTitle("分段") }
                uiState.warmupBlock?.let { block ->
                    item("warmup") {
                        TrainBlockCard(
                            block = block,
                            blockIndex = 0,
                            onRemoveBlock = { viewModel.removeBlock(BlockType.WARMUP, 0) },
                            onAddStep = { viewModel.addStepToBlock(BlockType.WARMUP, 0) },
                            onStepClick = { viewModel.openStepEditor(BlockType.WARMUP, 0, it) },
                            onRemoveStep = { viewModel.removeStep(BlockType.WARMUP, 0, it) },
                            isEditMode = uiState.isEditMode
                        )
                    }
                }
                itemsIndexed(
                    items = uiState.mainBlocks,
                    key = { _, block -> block.blockId ?: "main_${block.seq}" }
                ) { index, block ->
                    if (uiState.isEditMode) {
                        ReorderableItem(reorderState, key = block.blockId ?: "main_${block.seq}") { _ ->
                            TrainBlockCard(
                                block = block,
                                blockIndex = index,
                                onRemoveBlock = { viewModel.removeBlock(BlockType.MAIN, index) },
                                onAddStep = { viewModel.addStepToBlock(BlockType.MAIN, index) },
                                onStepClick = { viewModel.openStepEditor(BlockType.MAIN, index, it) },
                                onRemoveStep = { viewModel.removeStep(BlockType.MAIN, index, it) },
                                isEditMode = true,
                                dragHandleModifier = Modifier.draggableHandle(),
                                onLoopCountChange = { viewModel.updateLoopCount(index, it) }
                            )
                        }
                    } else {
                        TrainBlockCard(
                            block = block,
                            blockIndex = index,
                            onRemoveBlock = { },
                            onAddStep = { },
                            onStepClick = { },
                            onRemoveStep = { },
                            isEditMode = false
                        )
                    }
                }
                uiState.cooldownBlock?.let { block ->
                    item("cooldown") {
                        TrainBlockCard(
                            block = block,
                            blockIndex = 0,
                            onRemoveBlock = { viewModel.removeBlock(BlockType.COOLDOWN, 0) },
                            onAddStep = { viewModel.addStepToBlock(BlockType.COOLDOWN, 0) },
                            onStepClick = { viewModel.openStepEditor(BlockType.COOLDOWN, 0, it) },
                            onRemoveStep = { viewModel.removeStep(BlockType.COOLDOWN, 0, it) },
                            isEditMode = uiState.isEditMode
                        )
                    }
                }
            } else {
                item { SectionTitle(uiState.trainWholeType.displayName()) }
                item {
                    SingleGoalEditor(
                        trainWholeType = uiState.trainWholeType,
                        distanceGoalStep = uiState.distanceGoalStep,
                        timeGoalStep = uiState.timeGoalStep,
                        calGoalStep = uiState.calGoalStep,
                        pacerGoalStep = uiState.pacerGoalStep,
                        onDistanceChange = { viewModel.updateDistanceGoal(it) },
                        onTimeChange = { viewModel.updateTimeGoal(it) },
                        onCaloriesChange = { viewModel.updateCaloriesGoal(it) },
                        onPacerChange = { min, max -> viewModel.updatePacerGoal(min, max) },
                        isEditMode = uiState.isEditMode
                    )
                }
            }

            item { SectionTitle("备注") }
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = { Text("添加训练备注...") },
                    minLines = 4,
                    maxLines = 6,
                    enabled = uiState.isEditMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = RunTheme.colorScheme.blue.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 2.dp)
    )
}

@Composable
private fun BasicInfoCard(
    name: String,
    onNameChange: (String) -> Unit,
    scheduledDate: String?,
    onDateClick: () -> Unit,
    locationType: LocationType,
    onLocationTypeChange: (LocationType) -> Unit,
    trainWholeType: TrainWholeType,
    onTrainWholeTypeChange: (TrainWholeType) -> Unit,
    isEditMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("名称", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(18.dp))
            if (isEditMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("起个名字") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = RunTheme.colorScheme.blue.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )
            } else {
                Text(
                    text = name.ifBlank { "未命名课程" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("时间", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                TextButton(
                    onClick = onDateClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(scheduledDate ?: "选择日期")
                }
            } else {
                Text(
                    text = scheduledDate ?: "未选择日期",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("地点", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                InlineChoiceGroup(
                    options = listOf(LocationType.OUTDOOR to "室外", LocationType.INDOOR to "室内"),
                    selected = locationType,
                    onSelected = onLocationTypeChange
                )
            } else {
                Text(
                    text = locationType.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("类型", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                TypeDropdown(
                    options = listOf(
                        TrainWholeType.SELF_DEFINE to "自定义",
                        TrainWholeType.DISTANCE to "距离",
                        TrainWholeType.TIME to "时间",
                        TrainWholeType.CALORIES to "卡路里",
                        TrainWholeType.PACER to "配速员"
                    ),
                    selected = trainWholeType,
                    onSelected = onTrainWholeTypeChange,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = trainWholeType.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TypeDropdown(
    options: List<Pair<TrainWholeType, String>>,
    selected: TrainWholeType,
    onSelected: (TrainWholeType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected.displayName()
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Text(
            text = selectedLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = RunTheme.colorScheme.blue,
            modifier = Modifier
                .background(RunTheme.colorScheme.blue.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = RunTheme.colorScheme.cardBg
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            color = if (value == selected) RunTheme.colorScheme.blue else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> InlineChoiceGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) RunTheme.colorScheme.blue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = if (isSelected) RunTheme.colorScheme.blue.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled) { onSelected(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TotalSummaryCard(
    blocks: List<TrainBlock>,
    trainWholeType: TrainWholeType,
    distanceGoal: Double,
    timeGoalSeconds: Int
) {
    val distance = if (trainWholeType == TrainWholeType.DISTANCE) distanceGoal else blocks.totalDistanceMeters()
    val duration = if (trainWholeType == TrainWholeType.TIME) timeGoalSeconds else blocks.totalDurationSeconds()
    SummaryCard {
        SummaryMetric(Icons.Outlined.DirectionsRun, formatDistance(distance), RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(Icons.Outlined.Timer, formatDuration(duration), RunTheme.colorScheme.orange, Modifier.weight(1f))
    }
}

@Composable
private fun PacerSummaryCard(minPace: Int?, maxPace: Int?) {
    SummaryCard {
        SummaryMetric(Icons.Outlined.Speed, minPace?.let(::formatPace) ?: "--", RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(Icons.Outlined.Timer, maxPace?.let(::formatPace) ?: "--", RunTheme.colorScheme.orange, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SummaryMetric(icon: ImageVector, value: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(RunTheme.colorScheme.divider)
    )
}

@Composable
private fun TrainBlockActionBar(
    onAddTrain: () -> Unit,
    onAddRecovery: () -> Unit,
    onAddInterval: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("+训练", Icons.Outlined.DirectionsRun, onAddTrain, Modifier.weight(1f))
        ActionButton("+恢复", Icons.Outlined.KeyboardDoubleArrowDown, onAddRecovery, Modifier.weight(1f))
        ActionButton("+循环", Icons.Outlined.Cached, onAddInterval, Modifier.weight(1f))
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .background(RunTheme.colorScheme.blue.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = RunTheme.colorScheme.blue)
        Spacer(Modifier.width(4.dp))
        Text(label, color = RunTheme.colorScheme.blue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDatePickerDialog(
    selectedDate: String?,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val initialMillis = remember(selectedDate) {
        runCatching {
            LocalDate.parse(selectedDate, formatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrElse {
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: initialMillis
                    val localDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(localDate.format(formatter))
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
