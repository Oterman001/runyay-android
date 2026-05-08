package com.oterman.rundemo.presentation.feature.trainplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.components.SingleGoalEditor
import com.oterman.rundemo.presentation.feature.trainplan.components.StepEditSheet
import com.oterman.rundemo.presentation.feature.trainplan.components.TrainBlockCard
import com.oterman.rundemo.presentation.feature.trainplan.components.TrainWholeTypeSelector

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

    // Step editor sheet
    uiState.editingStep?.let { editing ->
        StepEditSheet(
            step = editing.step,
            onSave = { viewModel.saveStepEdit(it) },
            onDismiss = { viewModel.dismissStepEditor() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNewPlan) "新增训练" else "编辑训练",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.savePlan() }) {
                            Icon(Icons.Default.Check, "保存")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Basic info card
            item {
                Spacer(Modifier.height(4.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.onNameChange(it) },
                            label = { Text("训练名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("计划日期", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.toggleDatePicker(true) }) {
                                Text(uiState.scheduledDate ?: "选择日期")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Location type
                        Text("训练地点", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        val locationOptions = listOf(LocationType.OUTDOOR to "室外", LocationType.INDOOR to "室内")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            locationOptions.forEachIndexed { index, (type, label) ->
                                SegmentedButton(
                                    selected = uiState.locationType == type,
                                    onClick = { viewModel.onLocationTypeChange(type) },
                                    shape = SegmentedButtonDefaults.itemShape(index, locationOptions.size)
                                ) { Text(label) }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Train type
                        Text("训练类型", style = MaterialTheme.typography.bodyMedium)
                        TrainWholeTypeSelector(
                            selected = uiState.trainWholeType,
                            onSelected = { viewModel.onTrainWholeTypeChange(it) }
                        )
                    }
                }
            }

            // SELF_DEFINE: structured blocks
            if (uiState.trainWholeType == TrainWholeType.SELF_DEFINE) {
                // Warmup block
                uiState.warmupBlock?.let { block ->
                    item {
                        TrainBlockCard(
                            block = block,
                            blockIndex = 0,
                            onRemoveBlock = { viewModel.removeBlock(BlockType.WARMUP, 0) },
                            onAddStep = { viewModel.addStepToBlock(BlockType.WARMUP, 0) },
                            onStepClick = { stepIdx -> viewModel.openStepEditor(BlockType.WARMUP, 0, stepIdx) },
                            onRemoveStep = { stepIdx -> viewModel.removeStep(BlockType.WARMUP, 0, stepIdx) }
                        )
                    }
                }

                // Main blocks
                uiState.mainBlocks.forEachIndexed { index, block ->
                    item(key = "main_$index") {
                        TrainBlockCard(
                            block = block,
                            blockIndex = index,
                            onRemoveBlock = { viewModel.removeBlock(BlockType.MAIN, index) },
                            onAddStep = { viewModel.addStepToBlock(BlockType.MAIN, index) },
                            onStepClick = { stepIdx -> viewModel.openStepEditor(BlockType.MAIN, index, stepIdx) },
                            onRemoveStep = { stepIdx -> viewModel.removeStep(BlockType.MAIN, index, stepIdx) },
                            onLoopCountChange = { delta -> viewModel.updateLoopCount(index, delta) }
                        )
                    }
                }

                // Cooldown block
                uiState.cooldownBlock?.let { block ->
                    item {
                        TrainBlockCard(
                            block = block,
                            blockIndex = 0,
                            onRemoveBlock = { viewModel.removeBlock(BlockType.COOLDOWN, 0) },
                            onAddStep = { viewModel.addStepToBlock(BlockType.COOLDOWN, 0) },
                            onStepClick = { stepIdx -> viewModel.openStepEditor(BlockType.COOLDOWN, 0, stepIdx) },
                            onRemoveStep = { stepIdx -> viewModel.removeStep(BlockType.COOLDOWN, 0, stepIdx) }
                        )
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.warmupBlock == null) {
                            FilledTonalButton(onClick = { viewModel.addWarmupBlock() }) {
                                Text("+热身")
                            }
                        }
                        FilledTonalButton(onClick = { viewModel.addMainBlock() }) {
                            Text("+训练")
                        }
                        if (uiState.cooldownBlock == null) {
                            FilledTonalButton(onClick = { viewModel.addCooldownBlock() }) {
                                Text("+冷却")
                            }
                        }
                    }
                }
            } else {
                // Single goal editor
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
                        onPacerChange = { min, max -> viewModel.updatePacerGoal(min, max) }
                    )
                }
            }

            // Description card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("备注", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            placeholder = { Text("添加训练备注...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
