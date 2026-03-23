package com.oterman.rundemo.presentation.feature.runningshoes.detail

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.ui.res.painterResource
import com.oterman.rundemo.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.oterman.rundemo.domain.model.RunningShoe
import androidx.core.content.FileProvider
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.ImagePickerDialog
import com.oterman.rundemo.presentation.feature.runningshoes.batchlink.BatchLinkRunRecordsSheet
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningShoeDetailScreen(
    shoeId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToLinkedRecords: (String) -> Unit = {},
    onNavigateToBatchLink: (String) -> Unit = {},
    viewModel: RunningShoeDetailViewModel = viewModel(
        factory = RunningShoeDetailViewModelFactory(LocalContext.current, shoeId)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showBatchLinkSheet by remember { mutableStateOf(false) }

    // 裁剪后的目标Uri
    val croppedImageUri = remember {
        Uri.fromFile(File(context.cacheDir, "cropped_shoe_detail.jpg"))
    }

    // uCrop 裁剪结果处理
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.let { croppedUri ->
                    viewModel.uploadImage(croppedUri)
                }
            }
        }
    }

    // 从 Compose 主题获取颜色，用于 UCrop toolbar
    val toolbarColor = MaterialTheme.colorScheme.background.toArgb()
    val toolbarWidgetColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val statusBarColor = MaterialTheme.colorScheme.background.toArgb()

    // 启动裁剪的辅助函数
    fun launchCrop(sourceUri: Uri) {
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setToolbarTitle("裁剪跑鞋图片")
            setShowCropFrame(true)
            setShowCropGrid(true)
            setStatusBarColor(statusBarColor)
            setToolbarColor(toolbarColor)
            setToolbarWidgetColor(toolbarWidgetColor)
        }
        val intent = UCrop.of(sourceUri, croppedImageUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1024, 1024)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(intent)
    }

    // 图片选择器 - 选择后启动裁剪
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { launchCrop(it) }
    }

    // 相机拍照
    val tempImageUri = remember {
        val tempFile = File.createTempFile("shoe_detail_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            launchCrop(tempImageUri)
        }
    }

    var showImagePickerDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadShoe()
        }
    }

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) onNavigateBack()
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(uiState.shoe?.displayName ?: "跑鞋详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(shoeId)
                            }
                        )
                        if (uiState.shoe?.isDefault != true) {
                            DropdownMenuItem(
                                text = { Text("设为默认") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setAsDefault()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(if (uiState.shoe?.isActive == true) "退役" else "恢复使用")
                            },
                            onClick = {
                                showMenu = false
                                if (uiState.shoe?.isActive == true) viewModel.retireShoe()
                                else viewModel.reactivateShoe()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                viewModel.showDeleteDialog()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBatchLinkSheet = true }) {
                Icon(Icons.Default.Link, contentDescription = "批量关联")
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "detail-loading"
        ) { loading ->
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val shoe = uiState.shoe ?: return@AnimatedContent

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image section
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showImagePickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageSource = shoe.displayImageSource
                            if (imageSource != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageSource)
                                        .memoryCacheKey("shoe_${shoe.id}_${shoe.updatedAt}")
                                        .diskCacheKey("shoe_${shoe.id}_${shoe.updatedAt}")
                                        .build(),
                                    contentDescription = shoe.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    loading = {
                                        Box(
                                            modifier = Modifier.matchParentSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier.matchParentSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.svg_setting_shoes),
                                                contentDescription = null,
                                                modifier = Modifier.size(96.dp),
                                                contentScale = ContentScale.Fit,
                                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("点击添加图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Name + Status badges
                    item {
                        Column {
                            Row {
                                Text(
                                    text = shoe.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.alignByBaseline()
                                )
                                if (!shoe.displaySubtitle.isNullOrBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = shoe.displaySubtitle!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.alignByBaseline()
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                if (shoe.isDefault) {
                                    StatusBadge("默认", MaterialTheme.colorScheme.primary)
                                }
                                if (shoe.isRetired) {
                                    StatusBadge("已退役", MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Wear progress bar
                    item {
                        WearProgressSection(shoe)
                    }

                    // Statistics grid
                    item {
                        StatisticsCard(shoe)
                    }

                    // Detail info
                    item {
                        DetailInfoCard(shoe)
                    }

                    // Notes
                    if (!shoe.notes.isNullOrBlank()) {
                        item {
                            AppCard {
                                Column(Modifier.padding(16.dp)) {
                                    Text("备注", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(shoe.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Linked records
                    item {
                        AppCard(
                            modifier = Modifier.clickable { onNavigateToLinkedRecords(shoeId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("关联记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${uiState.linkedRecordsCount} 条",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }

    if (showBatchLinkSheet) {
        BatchLinkRunRecordsSheet(
            shoeId = shoeId,
            onDismiss = { showBatchLinkSheet = false },
            onLinkSuccess = {
                showBatchLinkSheet = false
                viewModel.loadShoe()
            }
        )
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("删除跑鞋") },
            text = { Text("确定要删除这双跑鞋吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog(); viewModel.deleteShoe() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            title = "选择跑鞋图片",
            onDismiss = { showImagePickerDialog = false },
            onTakePhoto = {
                showImagePickerDialog = false
                cameraLauncher.launch(tempImageUri)
            },
            onChooseFromGallery = {
                showImagePickerDialog = false
                galleryLauncher.launch("image/*")
            },
            galleryHint = "\uD83D\uDCA1 可在电商平台保存跑鞋图片后从相册选择，效果更好"
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun WearProgressSection(shoe: RunningShoe) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("磨损程度", style = MaterialTheme.typography.titleSmall)
                Text(
                    shoe.wearStatusText,
                    color = shoe.wearStatusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (shoe.wearPercentage / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = shoe.wearStatusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "%.1f / %.0f km".format(shoe.effectiveDistance, shoe.expectedLifespan),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsCard(shoe: RunningShoe) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Text("统计数据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                StatItem("总距离", "%.1f".format(shoe.effectiveDistance), "km")
                StatItem("总次数", "${shoe.totalRuns}", "次")
                StatItem("使用天数", "${shoe.usageDays}", "天")
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                StatItem("总时长", "%.0f".format(shoe.totalDuration), "min")
                StatItem("均距/次", "%.1f".format(shoe.averageDistancePerRun), "km")
                StatItem("每公里", shoe.costPerKm?.let { "%.2f".format(it) } ?: "-", shoe.costPerKm?.let { "元" })
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatItem(label: String, value: String, unit: String? = null) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!unit.isNullOrBlank()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailInfoCard(shoe: RunningShoe) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Text("详细信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            InfoRow("首次使用", shoe.firstUseDate?.let { dateFormat.format(Date(it)) } ?: "-")
            InfoRow("预期寿命", "%.0f km".format(shoe.expectedLifespan))
            InfoRow("剩余距离", "%.1f km".format(shoe.remainingDistance))
            InfoRow("初始距离", "%.1f km".format(shoe.initialDistance))
            shoe.retireDate?.let { InfoRow("退役日期", dateFormat.format(Date(it))) }
            shoe.price?.let { InfoRow("购买价格", "%.2f 元".format(it)) }
            shoe.shoeSize?.let { InfoRow("鞋码", it) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
