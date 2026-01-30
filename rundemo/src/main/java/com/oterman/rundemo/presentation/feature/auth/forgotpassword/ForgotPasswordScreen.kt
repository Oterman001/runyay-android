package com.oterman.rundemo.presentation.feature.auth.forgotpassword

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps.ForgotPasswordNewPasswordStep
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps.ForgotPasswordPhoneStep
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps.ForgotPasswordVerificationStep

/**
 * 忘记密码主界面
 * 对应iOS的ForgotPasswordView
 * 整合三个步骤：手机号输入、验证码验证、新密码设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onResetSuccess: (phoneNumber: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = ForgotPasswordViewModelFactory(context.applicationContext)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // 处理返回按键
    BackHandler {
        if (uiState.canGoBack) {
            viewModel.goToPreviousStep()
        } else {
            onNavigateBack()
        }
    }
    
    // 监听重置成功状态
    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            onResetSuccess(uiState.phoneNumber)
            viewModel.resetSuccessState()
        }
    }
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentStep) {
                            ResetStep.PHONE_NUMBER -> "重置密码"
                            ResetStep.VERIFICATION -> "验证手机号"
                            ResetStep.NEW_PASSWORD -> "设置新密码"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.canGoBack) {
                                viewModel.goToPreviousStep()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 根据当前步骤显示对应界面
            when (uiState.currentStep) {
                ResetStep.PHONE_NUMBER -> {
                    ForgotPasswordPhoneStep(
                        uiState = uiState,
                        onPhoneNumberChange = viewModel::onPhoneNumberChange,
                        onSendCode = viewModel::requestVerificationCode
                    )
                }
                ResetStep.VERIFICATION -> {
                    ForgotPasswordVerificationStep(
                        uiState = uiState,
                        onVerificationCodeChange = viewModel::onVerificationCodeChange,
                        onVerifyCode = viewModel::verifyCode,
                        onResendCode = viewModel::requestVerificationCode
                    )
                }
                ResetStep.NEW_PASSWORD -> {
                    ForgotPasswordNewPasswordStep(
                        uiState = uiState,
                        onNewPasswordChange = viewModel::onNewPasswordChange,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onToggleNewPasswordVisibility = viewModel::toggleNewPasswordVisibility,
                        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                        onResetPassword = viewModel::resetPassword
                    )
                }
            }
        }
    }
    
    // 用户不存在弹窗
    if (uiState.showUserNotExistAlert) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUserNotExistAlert,
            title = { Text("提示") },
            text = { Text("该手机号未注册，是否前往注册？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissUserNotExistAlert()
                        onNavigateToRegister()
                    }
                ) {
                    Text("去注册")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUserNotExistAlert) {
                    Text("取消")
                }
            }
        )
    }
}

