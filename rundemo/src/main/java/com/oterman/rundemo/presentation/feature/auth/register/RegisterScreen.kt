package com.oterman.rundemo.presentation.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.components.AliyunCaptchaDialog
import com.oterman.rundemo.presentation.feature.auth.register.steps.PasswordStep
import com.oterman.rundemo.presentation.feature.auth.register.steps.PhoneInputStep
import com.oterman.rundemo.presentation.feature.auth.register.steps.VerificationStep

/**
 * 注册界面
 * 对应iOS的PhoneRegisterView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: (String?) -> Unit = {},
    onNavigateToUserTerms: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToContactUs: () -> Unit = {},
    viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // 配置折叠工具栏的滚动行为
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    // 监听注册成功事件
    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            onRegisterSuccess()
            viewModel.resetRegisterSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("账号注册") },
                navigationIcon = {
                    IconButton(onClick = {
                        handleBackAction(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateBack = onNavigateBack
                        )
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContactUs) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "帮助"
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
            if (uiState.currentStep == RegistrationStep.PHONE_NUMBER) {
                ContactUsPrompt(onNavigateToContactUs = onNavigateToContactUs)
            }
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
            // 根据当前步骤显示对应内容
            when (uiState.currentStep) {
                RegistrationStep.PHONE_NUMBER -> {
                    PhoneInputStep(
                        uiState = uiState,
                        onPhoneNumberChange = viewModel::onPhoneNumberChange,
                        onTermsToggle = viewModel::toggleTermsAgreement,
                        onSendCode = viewModel::checkTermsAgreementAndRequestCode,
                        onNavigateToUserTerms = onNavigateToUserTerms,
                        onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy
                    )
                    
                    // 登录提示
                    LoginPrompt(onNavigateToLogin = { onNavigateToLogin(null) })
                }
                
                RegistrationStep.VERIFICATION -> {
                    VerificationStep(
                        uiState = uiState,
                        onSmsCodeChange = viewModel::onSmsCodeChange,
                        onVerify = viewModel::verifyCodeAndRegister,
                        onResendCode = viewModel::requestVerificationCode
                    )
                }
                
                RegistrationStep.PASSWORD -> {
                    PasswordStep(
                        uiState = uiState,
                        onNicknameChange = viewModel::onNicknameChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                        onComplete = viewModel::completeRegistration
                    )
                }
            }
        }
    }
    
    // 图形验证码对话框
    if (uiState.showCaptcha) {
        AliyunCaptchaDialog(
            onSuccess = { param -> viewModel.handleCaptchaSuccess(param) },
            onFailure = { error -> viewModel.handleCaptchaFailure(error) },
            onCancel = { viewModel.handleCaptchaCancel() }
        )
    }

    // 错误对话框
    if (uiState.errorMessage != null) {
        RegisterErrorDialog(
            errorMessage = uiState.errorMessage!!,
            shouldNavigateToLogin = uiState.shouldNavigateToLogin,
            onDismiss = { viewModel.clearError() },
            onNavigateToLogin = {
                viewModel.clearError()
                onNavigateToLogin(uiState.phoneNumber)
            }
        )
    }
}

/**
 * 处理返回操作
 */
private fun handleBackAction(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    onNavigateBack: () -> Unit
) {
    if (uiState.canGoBack) {
        // 如果可以返回上一步，执行步骤返回
        viewModel.goToPreviousStep()
    } else {
        // 如果在第一步，返回到上级页面
        onNavigateBack()
    }
}

/**
 * 登录提示
 */
@Composable
private fun LoginPrompt(
    onNavigateToLogin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已有账户？",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onNavigateToLogin) {
            Text(
                text = "立即登录",
                color = MaterialTheme.colorScheme.primary
            )
        }
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
            .navigationBarsPadding()
            .padding(top = 8.dp),
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
 * 注册错误对话框
 */
@Composable
private fun RegisterErrorDialog(
    errorMessage: String,
    shouldNavigateToLogin: Boolean,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (shouldNavigateToLogin) "用户已存在" else "注册失败"
            )
        },
        text = {
            Text(errorMessage)
        },
        confirmButton = {
            if (shouldNavigateToLogin) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("前往登录")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            }
        },
        dismissButton = if (shouldNavigateToLogin) {
            {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        } else null
    )
}

