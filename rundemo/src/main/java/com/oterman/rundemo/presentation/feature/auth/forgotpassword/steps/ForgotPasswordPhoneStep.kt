package com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.ForgotPasswordUiState

/**
 * 忘记密码 - 手机号输入步骤
 * 对应iOS的ForgotPasswordPhoneStepView
 */
@Composable
fun ForgotPasswordPhoneStep(
    uiState: ForgotPasswordUiState,
    onPhoneNumberChange: (String) -> Unit,
    onSendCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // 说明文字
        Text(
            text = "请输入您要重置密码的手机号",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // 手机号输入
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = { value ->
                // 限制只能输入数字且最多11位
                val filtered = value.filter { it.isDigit() }.take(11)
                onPhoneNumberChange(filtered)
            },
            label = { Text("手机号") },
            placeholder = { Text("请输入手机号") },
            singleLine = true,
            isError = uiState.phoneNumberError != null,
            supportingText = {
                uiState.phoneNumberError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.canSendCode) {
                        onSendCode()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 获取验证码按钮
        GradientButton(
            text = "获取验证码",
            onClick = {
                focusManager.clearFocus()
                onSendCode()
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = uiState.isLoading,
            enabled = uiState.canSendCode,
            loadingText = "发送中..."
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 错误信息显示
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

