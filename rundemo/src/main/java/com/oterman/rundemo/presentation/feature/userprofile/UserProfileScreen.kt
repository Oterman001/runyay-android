package com.oterman.rundemo.presentation.feature.userprofile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import com.yalantis.ucrop.UCrop
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import java.io.File

/**
 * 用户信息页面
 * 对应iOS的UserProfileView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 裁剪后的目标Uri
    val croppedImageUri = remember {
        Uri.fromFile(File(context.cacheDir, "cropped_avatar.jpg"))
    }

    // uCrop 裁剪结果处理
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.let { croppedUri ->
                    viewModel.uploadCroppedAvatar(croppedUri)
                }
            }
        }
    }

    // 启动裁剪的辅助函数
    fun launchCrop(sourceUri: Uri) {
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setToolbarTitle("裁剪头像")
            setCircleDimmedLayer(true)
            setShowCropFrame(true)
            setShowCropGrid(true)
            // 配置状态栏和工具栏颜色，避免与状态栏重叠
            setStatusBarColor(Color.BLACK)
            setToolbarColor(Color.parseColor("#FF6200EE"))
            setToolbarWidgetColor(Color.WHITE)
            setActiveControlsWidgetColor(Color.parseColor("#FF6200EE"))
        }
        val intent = UCrop.of(sourceUri, croppedImageUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(intent)
    }

    // 图片选择器 - 选择后启动裁剪
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { launchCrop(it) }
    }

    // 相机拍照
    val tempImageUri = remember {
        val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    // 拍照后启动裁剪
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            launchCrop(tempImageUri)
        }
    }

    // 处理导航
    LaunchedEffect(uiState.navigateToLogin) {
        if (uiState.navigateToLogin) {
            onNavigateToLogin()
            viewModel.resetNavigateToLogin()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 头像区域
            AvatarSection(
                avatarUrl = uiState.avatarUrl,
                isLoading = uiState.isLoadingAvatar || uiState.isUploadingAvatar,
                onClick = viewModel::showAvatarPicker
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 用户信息卡片
            UserInfoCard(
                userName = uiState.userName,
                phoneNumber = uiState.maskedPhoneNumber,
                isMale = uiState.isMale,
                birthdayMillis = uiState.birthdayMillis,
                onNicknameClick = viewModel::showNicknameEditor,
                onGenderClick = viewModel::showGenderPicker,
                onBirthdayClick = viewModel::showBirthdayPicker
            )

            Spacer(modifier = Modifier.weight(1f))

            // 底部按钮区域
            BottomButtonsSection(
                isLoggingOut = uiState.isLoggingOut,
                onLogoutClick = viewModel::showLogoutConfirm,
                onDeactivateClick = viewModel::showDeactivateConfirm
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 头像选择对话框
    if (uiState.showAvatarPickerDialog) {
        AvatarPickerDialog(
            onDismiss = viewModel::dismissAvatarPicker,
            onTakePhoto = {
                viewModel.dismissAvatarPicker()
                cameraLauncher.launch(tempImageUri)
            },
            onChooseFromGallery = {
                viewModel.dismissAvatarPicker()
                galleryLauncher.launch("image/*")
            }
        )
    }

    // 昵称编辑对话框
    if (uiState.showNicknameEditor) {
        NicknameEditDialog(
            nickname = uiState.editingNickname,
            error = uiState.nicknameError,
            isLoading = uiState.isUpdatingNickname,
            onNicknameChange = viewModel::updateEditingNickname,
            onDismiss = viewModel::dismissNicknameEditor,
            onConfirm = viewModel::saveNickname
        )
    }

    // 退出登录确认对话框
    if (uiState.showLogoutConfirmDialog) {
        LogoutConfirmDialog(
            onDismiss = viewModel::dismissLogoutConfirm,
            onConfirm = viewModel::logout
        )
    }

    // 注销账号确认对话框
    if (uiState.showDeactivateConfirmDialog) {
        DeactivateConfirmDialog(
            onDismiss = viewModel::dismissDeactivateConfirm,
            onConfirm = viewModel::proceedToPasswordConfirm
        )
    }

    // 性别选择对话框
    if (uiState.showGenderPicker) {
        GenderPickerDialog(onSelect = viewModel::saveGender, onDismiss = viewModel::dismissGenderPicker)
    }

    // 出生年月选择对话框
    if (uiState.showBirthdayPicker) {
        BirthdayPickerDialog(
            currentMillis = uiState.birthdayMillis,
            onSelect = viewModel::saveBirthday,
            onDismiss = viewModel::dismissBirthdayPicker
        )
    }

    // 密码确认对话框
    if (uiState.showPasswordConfirmDialog) {
        PasswordConfirmDialog(
            password = uiState.confirmPassword,
            error = uiState.passwordError,
            isLoading = uiState.isDeactivating,
            onPasswordChange = viewModel::updateConfirmPassword,
            onDismiss = viewModel::dismissPasswordConfirm,
            onConfirm = viewModel::deactivateAccount
        )
    }
}

/**
 * 头像区域组件
 */
@Composable
private fun AvatarSection(
    avatarUrl: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                }
                avatarUrl.isNullOrBlank() -> {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "默认头像",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击更换头像",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

/**
 * 用户信息卡片
 */
@Composable
private fun UserInfoCard(
    userName: String,
    phoneNumber: String,
    isMale: Boolean,
    birthdayMillis: Long,
    onNicknameClick: () -> Unit,
    onGenderClick: () -> Unit,
    onBirthdayClick: () -> Unit
) {
    val birthdayText = if (birthdayMillis > 0L)
        SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Date(birthdayMillis))
    else "未设置"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        // 昵称行
        InfoRow(
            label = "昵称",
            value = userName.ifEmpty { "未设置" },
            showArrow = true,
            onClick = onNicknameClick
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 性别行
        InfoRow(
            label = "性别",
            value = if (isMale) "男" else "女",
            showArrow = true,
            onClick = onGenderClick
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 出生年月行
        InfoRow(
            label = "出生年月",
            value = birthdayText,
            showArrow = true,
            onClick = onBirthdayClick
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 手机号行
        InfoRow(
            label = "手机号",
            value = phoneNumber,
            showArrow = false,
            onClick = null
        )
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    showArrow: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 底部按钮区域
 */
@Composable
private fun BottomButtonsSection(
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 退出登录按钮
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            enabled = !isLoggingOut
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // 注销账号按钮
        TextButton(
            onClick = onDeactivateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "注销账号",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 头像选择对话框
 */
@Composable
private fun AvatarPickerDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换头像") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarPickerOption(
                    icon = Icons.Filled.CameraAlt,
                    text = "拍照",
                    onClick = onTakePhoto
                )
                AvatarPickerOption(
                    icon = Icons.Filled.Photo,
                    text = "从相册选择",
                    onClick = onChooseFromGallery
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 头像选择选项
 */
@Composable
private fun AvatarPickerOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 性别选择对话框
 */
@Composable
private fun GenderPickerDialog(onSelect: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择性别") },
        text = {
            Column {
                TextButton(onClick = { onSelect(true) }, modifier = Modifier.fillMaxWidth()) { Text("男") }
                TextButton(onClick = { onSelect(false) }, modifier = Modifier.fillMaxWidth()) { Text("女") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 出生年月选择对话框
 */
@Composable
private fun BirthdayPickerDialog(currentMillis: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val cal = Calendar.getInstance().apply {
            if (currentMillis > 0L) timeInMillis = currentMillis
        }
        val dialog = DatePickerDialog(context, { _, year, month, _ ->
            val result = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelect(result.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

/**
 * 昵称编辑对话框
 */
@Composable
private fun NicknameEditDialog(
    nickname: String,
    error: String?,
    isLoading: Boolean,
    onNicknameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("修改昵称") },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    label = { Text("昵称") },
                    placeholder = { Text("请输入昵称") },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("1-20个字符，支持中文、英文、数字和下划线")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = nickname.isNotEmpty() && error == null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 退出登录确认对话框
 */
@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("退出登录") },
        text = { Text("确定要退出登录吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "确定",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 注销账号确认对话框
 */
@Composable
private fun DeactivateConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注销账号") },
        text = {
            Text(
                text = "注销账号后，您的所有数据将被永久删除，且无法恢复。确定要继续吗？",
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "继续注销",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 密码确认对话框
 */
@Composable
private fun PasswordConfirmDialog(
    password: String,
    error: String?,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("请输入密码") },
        text = {
            Column {
                Text(
                    text = "为确保是本人操作，请输入您的账号密码",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("确认注销")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}
