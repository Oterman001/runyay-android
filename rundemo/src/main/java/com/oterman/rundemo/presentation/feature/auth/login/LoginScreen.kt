package com.oterman.rundemo.presentation.feature.auth.login

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.components.PasswordTextField
import com.oterman.rundemo.presentation.components.ShakeBox
import com.oterman.rundemo.presentation.components.SimpleTermsCheckbox
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * 登录界面
 * 对应iOS的PhoneLoginView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToContactUs: () -> Unit = {},
    onNavigateToUserTerms: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // 配置折叠工具栏的滚动行为
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    // 监听登录成功事件
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
            viewModel.resetLoginSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("账号登录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ContactUsPrompt(onNavigateToContactUs = onNavigateToContactUs)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 50.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 手机号输入框
            PhoneNumberField(
                phoneNumber = uiState.phoneNumber,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                errorMessage = uiState.phoneNumberError
            )

            // 密码输入框
            PasswordTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                errorMessage = uiState.passwordError
            )

            // Debug版本预置账号快速切换
            if (BuildConfig.DEBUG) {
                DebugAccountSelector(
                    accounts = listOf(
                        "17512081100" to "mrtian",
                        "17512099302" to "qwerty",
                        "19183959302" to "mrtian"
                    ),
                    onSelect = { phone, password ->
                        viewModel.fillDebugAccount(phone, password)
                    }
                )
            }

            // 协议勾选框（带抖动效果）
            ShakeBox(shouldShake = uiState.shouldShake) {
                SimpleTermsCheckbox(
                    checked = uiState.hasAgreedToTerms,
                    onCheckedChange = { viewModel.toggleTermsAgreement() },
                    onUserTermsClick = onNavigateToUserTerms,
                    onPrivacyPolicyClick = onNavigateToPrivacyPolicy
                )
            }

            // 登录按钮
            GradientButton(
                text = "登录",
                onClick = { viewModel.checkTermsAgreementAndLogin() },
                isLoading = uiState.isLoading,
                enabled = uiState.phoneNumber.isNotEmpty() &&
                        uiState.password.isNotEmpty() &&
                        uiState.phoneNumberError == null &&
                        uiState.passwordError == null
            )

            // 忘记密码链接
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text(
                        text = "忘记密码？",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 注册提示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "还没有账户？",
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "立即注册",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // 错误对话框
    if (uiState.errorMessage != null) {
        LoginErrorDialog(
            errorMessage = uiState.errorMessage!!,
            remainingAttempts = uiState.remainingAttempts,
            showForgotPasswordAlert = uiState.showForgotPasswordAlert,
            onDismiss = { viewModel.clearError() },
            onForgotPassword = {
                viewModel.clearError()
                onNavigateToForgotPassword()
            }
        )
    }
}

/**
 * 联系我们提示
 */
@Composable
private fun ContactUsPrompt(
    onNavigateToContactUs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "碰到问题？",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onNavigateToContactUs) {
            Text(
                text = "联系我们",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 手机号输入框
 */
@Composable
private fun PhoneNumberField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    errorMessage: String?
) {
    Column {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("手机号") },
            placeholder = { Text("请输入手机号") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "手机图标"
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = RunTheme.colorScheme.destructive
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = RunTheme.colorScheme.destructive,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 登录错误对话框
 */
@Composable
private fun LoginErrorDialog(
    errorMessage: String,
    remainingAttempts: Int?,
    showForgotPasswordAlert: Boolean,
    onDismiss: () -> Unit,
    onForgotPassword: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (showForgotPasswordAlert) "账户已被锁定" else "登录失败"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showForgotPasswordAlert) {
                    Text("输入错误次数过多，账户已被锁定24小时。您可以通过忘记密码功能重置密码。")
                } else {
                    Text(errorMessage)
                    
                    if (remainingAttempts != null && remainingAttempts <= 3 && remainingAttempts > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚠️ 剩余尝试次数：$remainingAttempts",
                                color = RunTheme.colorScheme.orange,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showForgotPasswordAlert) {
                TextButton(onClick = onForgotPassword) {
                    Text("忘记密码")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            }
        },
        dismissButton = if (showForgotPasswordAlert) {
            {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        } else null
    )
}

/**
 * Debug 预置账号快速选择面板（仅 Debug 构建可见）
 */
@Composable
private fun DebugAccountSelector(
    accounts: List<Pair<String, String>>,
    onSelect: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RunTheme.colorScheme.orange,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DEBUG 账号",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = RunTheme.colorScheme.orange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEachIndexed { index, (phone, password) ->
                OutlinedButton(
                    onClick = { onSelect(phone, password) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 4.dp,
                        vertical = 4.dp
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        RunTheme.colorScheme.orange.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "账号${index + 1}",
                        fontSize = 12.sp,
                        color = RunTheme.colorScheme.orange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
