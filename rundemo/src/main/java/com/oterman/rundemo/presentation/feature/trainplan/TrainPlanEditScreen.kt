package com.oterman.rundemo.presentation.feature.trainplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.oterman.rundemo.R
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.components.SingleGoalEditor
import com.oterman.rundemo.presentation.feature.trainplan.components.StepEditSheet
import com.oterman.rundemo.presentation.feature.trainplan.components.TrainBlockCard
import com.oterman.rundemo.ui.theme.RunTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.launch
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPushPlatformSelect by remember { mutableStateOf(false) }
    var showDeletePushPlatformSelect by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val showEditHint = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("点击右下角编辑按钮，进入编辑台后可以编辑")
        }
    }
    val pushPlatforms = remember {
        DataSourcePlatform.entries.filter { it.isEnabled && it.requiresOAuthBinding }
    }
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

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onNavigateBack()
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除课程") },
            text = { Text("确定要删除「${uiState.name}」吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePlan()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showPushPlatformSelect) {
        AlertDialog(
            onDismissRequest = { showPushPlatformSelect = false },
            title = { Text("选择推送平台") },
            text = {
                androidx.compose.foundation.layout.Column {
                    pushPlatforms.forEach { platform ->
                        TextButton(
                            onClick = {
                                showPushPlatformSelect = false
                                viewModel.pushPlan(platform.code)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(platform.displayName) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPushPlatformSelect = false }) { Text("取消") }
            }
        )
    }

    if (showDeletePushPlatformSelect) {
        AlertDialog(
            onDismissRequest = { showDeletePushPlatformSelect = false },
            title = { Text("从手表删除推送") },
            text = {
                androidx.compose.foundation.layout.Column {
                    pushPlatforms.forEach { platform ->
                        TextButton(
                            onClick = {
                                showDeletePushPlatformSelect = false
                                viewModel.deletePushedPlan(platform.code)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(platform.displayName) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeletePushPlatformSelect = false }) { Text("取消") }
            }
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
                        val isActioning = uiState.isDeleting || uiState.isPushing || uiState.isDeletingPush
                        if (isActioning) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp).size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("删除课程", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMoreMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("推送到手表") },
                                        onClick = {
                                            showMoreMenu = false
                                            showPushPlatformSelect = true
                                        }
                                    )
                                    if (uiState.workoutId != null) {
                                        DropdownMenuItem(
                                            text = { Text("从手表删除推送") },
                                            onClick = {
                                                showMoreMenu = false
                                                showDeletePushPlatformSelect = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
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

            when (uiState.trainWholeType) {
                TrainWholeType.PACER -> {
                    item { SectionTitle("配速员") }
                    item { PacerSummaryCard(uiState.pacerGoalStep) }
                }
                TrainWholeType.CALORIES -> { /* 卡路里类型不显示整体预估 */ }
                else -> {
                    item { SectionTitle("整体预估") }
                    item {
                        TotalSummaryCard(
                            trainWholeType = uiState.trainWholeType,
                            blocks = listOfNotNull(uiState.warmupBlock) + uiState.mainBlocks + listOfNotNull(uiState.cooldownBlock),
                            distanceGoalMeters = uiState.distanceGoalStep?.distanceMeters() ?: 0.0,
                            timeGoalSeconds = uiState.timeGoalStep?.timeGoalSeconds ?: 0,
                            userVdot = uiState.userVdot
                        )
                    }
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
                            onStepClick = {
                                if (uiState.isEditMode) {
                                    viewModel.openStepEditor(BlockType.WARMUP, 0, it)
                                } else {
                                    showEditHint()
                                }
                            },
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
                                onMoveStep = { from, to -> viewModel.moveStep(BlockType.MAIN, index, from, to) },
                                isEditMode = true,
                                blockDragHandleModifier = Modifier.longPressDraggableHandle(),
                                onLoopCountChange = { viewModel.updateLoopCount(index, it) }
                            )
                        }
                    } else {
                        TrainBlockCard(
                            block = block,
                            blockIndex = index,
                            onRemoveBlock = { },
                            onAddStep = { },
                            onStepClick = { showEditHint() },
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
                            onStepClick = {
                                if (uiState.isEditMode) {
                                    viewModel.openStepEditor(BlockType.COOLDOWN, 0, it)
                                } else {
                                    showEditHint()
                                }
                            },
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
                        onPacerFullChange = { dm, du, ts, ps -> viewModel.updatePacerFull(dm, du, ts, ps) },
                        isEditMode = uiState.isEditMode
                    )
                }
            }

            item { SectionTitle("备注") }
            item {
                OutlinedTextField(
                    value = if (uiState.isEditMode) uiState.description else uiState.description.ifBlank { "无备注" },
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
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
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
    val rowHeight = 44.dp
    val locationOptions = listOf(
        LocationType.OUTDOOR to "室外",
        LocationType.INDOOR to "室内",
        LocationType.PENDING to "待定"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("名称", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(18.dp))
            if (isEditMode) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (name.isEmpty()) {
                                Text(
                                    "起个名字",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("时间", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                Text(
                    text = scheduledDate ?: "选择日期",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RunTheme.colorScheme.blue,
                    modifier = Modifier
                        .clickable { onDateClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Text(
                    text = scheduledDate ?: "未选择日期",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("地点", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.height(32.dp)) {
                    locationOptions.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = locationType == type,
                            onClick = { onLocationTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = locationOptions.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.15f),
                                activeContentColor = RunTheme.colorScheme.blue,
                                activeBorderColor = RunTheme.colorScheme.blue
                            ),
                            icon = {},
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            } else {
                Text(
                    text = locationType.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    enabled = true
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
        Row(
            modifier = Modifier
                .background(RunTheme.colorScheme.blue.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = RunTheme.colorScheme.blue
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = RunTheme.colorScheme.blue,
                modifier = Modifier.size(18.dp)
            )
        }
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
private fun TotalSummaryCard(
    trainWholeType: TrainWholeType,
    blocks: List<TrainBlock>,
    distanceGoalMeters: Double,
    timeGoalSeconds: Int,
    userVdot: Double?
) {
    when (trainWholeType) {
        TrainWholeType.SELF_DEFINE -> SelfDefineSummaryCard(estimateSelfDefine(blocks, userVdot))
        TrainWholeType.DISTANCE -> DistanceSummaryCard(estimateDistance(distanceGoalMeters, userVdot))
        TrainWholeType.TIME -> TimeSummaryCard(estimateTime(timeGoalSeconds, userVdot))
        else -> {}
    }
}

@Composable
private fun SelfDefineSummaryCard(est: TrainEstimate) {
    val distText = est.distanceMeters?.let { formatDistance(it) } ?: "--"
    val durText = est.durationSeconds?.let { formatDurationColon(it) } ?: "--"
    SummaryCard {
        SummaryMetric(painterResource(R.drawable.ic_goal_distance), distText, RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(painterResource(R.drawable.ic_goal_time), durText, RunTheme.colorScheme.orange, Modifier.weight(1f))
    }
}

@Composable
private fun DistanceSummaryCard(est: TrainEstimate) {
    val distText = est.distanceMeters?.let { formatDistance(it) } ?: "--"
    val durText = est.durationSeconds?.let { formatDurationColon(it) } ?: "--"
    SummaryCard {
        SummaryMetric(painterResource(R.drawable.ic_goal_distance), distText, RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(painterResource(R.drawable.ic_goal_time), durText, RunTheme.colorScheme.orange, Modifier.weight(1f))
    }
}

@Composable
private fun TimeSummaryCard(est: TrainEstimate) {
    val distText = est.distanceMeters?.let { formatDistance(it) } ?: "--"
    val durText = est.durationSeconds?.let { formatDurationColon(it) } ?: "--"
    SummaryCard {
        SummaryMetric(painterResource(R.drawable.ic_goal_distance), distText, RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(painterResource(R.drawable.ic_goal_time), durText, RunTheme.colorScheme.orange, Modifier.weight(1f))
    }
}

@Composable
private fun PacerSummaryCard(step: TrainStep?) {
    val distMeters = step?.distanceMeters() ?: 0.0
    val distText = if (distMeters > 0) formatDistance(distMeters) else "--"
    val paceText = step?.minPace?.let { formatPace(it) + "/km" } ?: "--"
    val timeText = step?.timeGoalSeconds?.let { formatDurationColon(it) } ?: "--"
    SummaryCard {
        SummaryMetric(painterResource(R.drawable.ic_goal_distance), distText, RunTheme.colorScheme.blue, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(painterResource(R.drawable.ic_intensity_pace), paceText, RunTheme.colorScheme.orange, Modifier.weight(1f))
        VerticalDivider()
        SummaryMetric(painterResource(R.drawable.ic_goal_time), timeText, RunTheme.colorScheme.success, Modifier.weight(1f))
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
private fun SummaryMetric(icon: Painter, value: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
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
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("+训练", painterResource(R.drawable.ic_step_training), onAddTrain, iconAfterText = true, iconSize = 14.dp, modifier = Modifier.weight(1f))
        ActionButton("+恢复", painterResource(R.drawable.ic_step_recovery), onAddRecovery, iconAfterText = true, iconSize = 14.dp, modifier = Modifier.weight(1f))
        ActionButton("+循环", rememberVectorPainter(Icons.Outlined.Cached), onAddInterval, iconAfterText = true, iconSize = 18.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    iconAfterText: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .background(RunTheme.colorScheme.blue.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
    ) {
        if (!iconAfterText) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = RunTheme.colorScheme.blue)
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = RunTheme.colorScheme.blue)
        if (iconAfterText) {
            Spacer(Modifier.width(4.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = RunTheme.colorScheme.blue)
        }
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
