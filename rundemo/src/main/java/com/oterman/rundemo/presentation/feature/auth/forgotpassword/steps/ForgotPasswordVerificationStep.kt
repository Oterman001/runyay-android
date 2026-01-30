package com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.ForgotPasswordUiState

/**
 * 忘记密码 - 验证码验证步骤
 * 对应iOS的ForgotPasswordVerificationStepView
 */
@Composable
fun ForgotPasswordVerificationStep(
    uiState: ForgotPasswordUiState,
    onVerificationCodeChange: (String) -> Unit,
    onVerifyCode: () -> Unit,
    onResendCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // 说明文字
        Text(
            text = "验证码已发送至 ${uiState.phoneNumber}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // 验证码输入
        OutlinedTextField(
            value = uiState.verificationCode,
            onValueChange = { value ->
                // 限制只能输入数字且最多6位
                val filtered = value.filter { it.isDigit() }.take(6)
                onVerificationCodeChange(filtered)
            },
            label = { Text("验证码") },
            placeholder = { Text("请输入6位验证码") },
            singleLine = true,
            isError = uiState.verificationCodeError != null,
            supportingText = {
                uiState.verificationCodeError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.canVerifyCode) {
                        onVerifyCode()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 重发验证码
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.resendCountdown > 0) {
                Text(
                    text = "${uiState.resendCountdown}秒后可重发",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(
                    onClick = onResendCode,
                    enabled = !uiState.isLoading
                ) {
                    Text("重发验证码")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 验证按钮
        GradientButton(
            text = "下一步",
            onClick = {
                focusManager.clearFocus()
                onVerifyCode()
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = uiState.isLoading,
            enabled = uiState.canVerifyCode,
            loadingText = "验证中..."
        )
        
        // 错误信息显示
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

