package com.oterman.rundemo.presentation.feature.auth.register.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.feature.auth.register.RegisterUiState
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * 步骤2：验证码验证
 * 对应iOS的RegVaildateSmsCodeView
 */
@Composable
fun VerificationStep(
    uiState: RegisterUiState,
    onSmsCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResendCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 提示信息
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "验证码已发送至",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = uiState.phoneNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // 验证码输入框
        SmsCodeField(
            smsCode = uiState.smsCode,
            onSmsCodeChange = onSmsCodeChange,
            errorMessage = uiState.smsCodeError
        )
        
        // 重发验证码
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (uiState.resendCountdown > 0) {
                Text(
                    text = "${uiState.resendCountdown}秒后可重发",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = onResendCode) {
                    Text(
                        text = "重新发送验证码",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 验证按钮
        GradientButton(
            text = if (uiState.isLoading) "验证中..." else "验证",
            onClick = onVerify,
            isLoading = uiState.isLoading,
            enabled = uiState.canVerifyCode
        )
    }
}

/**
 * 验证码输入框
 */
@Composable
private fun SmsCodeField(
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    errorMessage: String?
) {
    Column {
        Text(
            text = "验证码",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = smsCode,
            onValueChange = onSmsCodeChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入6位验证码") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "验证码图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = RunTheme.colorScheme.destructive,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = RunTheme.colorScheme.destructive,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

