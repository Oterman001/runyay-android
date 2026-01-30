package com.oterman.rundemo.presentation.feature.auth.forgotpassword.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.ForgotPasswordUiState

/**
 * 忘记密码 - 新密码设置步骤
 * 对应iOS的ForgotPasswordNewPasswordStepView
 */
@Composable
fun ForgotPasswordNewPasswordStep(
    uiState: ForgotPasswordUiState,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleNewPasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onResetPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // 说明文字
        Text(
            text = "请设置您的新密码",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // 新密码输入
        OutlinedTextField(
            value = uiState.newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("新密码") },
            placeholder = { Text("请输入6位以上密码") },
            singleLine = true,
            isError = uiState.newPasswordError != null,
            supportingText = {
                uiState.newPasswordError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            visualTransformation = if (uiState.isNewPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onToggleNewPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isNewPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (uiState.isNewPasswordVisible) {
                            "隐藏密码"
                        } else {
                            "显示密码"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 确认密码输入
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("确认密码") },
            placeholder = { Text("请再次输入密码") },
            singleLine = true,
            isError = uiState.confirmPasswordError != null,
            supportingText = {
                uiState.confirmPasswordError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            visualTransformation = if (uiState.isConfirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isConfirmPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (uiState.isConfirmPasswordVisible) {
                            "隐藏密码"
                        } else {
                            "显示密码"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.canResetPassword) {
                        onResetPassword()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 重置密码按钮
        GradientButton(
            text = "重置密码",
            onClick = {
                focusManager.clearFocus()
                onResetPassword()
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = uiState.isLoading,
            enabled = uiState.canResetPassword,
            loadingText = "重置中..."
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

