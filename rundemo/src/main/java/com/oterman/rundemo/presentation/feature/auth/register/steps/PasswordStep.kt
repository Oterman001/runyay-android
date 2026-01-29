package com.oterman.rundemo.presentation.feature.auth.register.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.feature.auth.register.RegisterUiState

/**
 * 步骤3：设置密码和昵称
 * 对应iOS的RegSetPwdView
 */
@Composable
fun PasswordStep(
    uiState: RegisterUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 昵称输入
        NicknameField(
            nickname = uiState.nickname,
            onNicknameChange = onNicknameChange,
            errorMessage = uiState.nicknameError
        )
        
        // 密码输入
        PasswordField(
            label = "设置密码",
            value = uiState.password,
            onValueChange = onPasswordChange,
            isVisible = uiState.isPasswordVisible,
            onToggleVisibility = onTogglePasswordVisibility,
            placeholder = "请设置登录密码",
            errorMessage = uiState.passwordError
        )
        
        // 确认密码输入
        PasswordField(
            label = "确认密码",
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            isVisible = uiState.isConfirmPasswordVisible,
            onToggleVisibility = onToggleConfirmPasswordVisibility,
            placeholder = "请再次输入密码",
            errorMessage = uiState.confirmPasswordError
        )
        
        // 密码要求说明
        PasswordRequirements(uiState)
        
        // 完成注册按钮
        GradientButton(
            text = if (uiState.isLoading) "注册中..." else "完成注册",
            onClick = onComplete,
            isLoading = uiState.isLoading,
            enabled = uiState.canRegister
        )
    }
}

/**
 * 昵称输入框
 */
@Composable
private fun NicknameField(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    errorMessage: String?
) {
    Column {
        Text(
            text = "设置昵称",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入昵称") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "昵称图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = Color.Red,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 密码输入框
 */
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    placeholder: String,
    errorMessage: String?
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "密码图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "隐藏密码" else "显示密码",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = Color.Red,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 密码要求说明
 */
@Composable
private fun PasswordRequirements(uiState: RegisterUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "要求说明：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 昵称要求
        RequirementItem(
            text = "昵称不能为空且不超过20字符",
            isMet = uiState.nickname.isNotEmpty() && uiState.nicknameError == null
        )
        
        // 密码长度要求
        RequirementItem(
            text = "至少6位字符",
            isMet = uiState.password.length >= 6
        )
        
        // 密码一致性要求
        RequirementItem(
            text = "两次密码输入一致",
            isMet = uiState.password == uiState.confirmPassword && uiState.password.isNotEmpty()
        )
    }
}

/**
 * 要求项
 */
@Composable
private fun RequirementItem(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) {
                androidx.compose.material.icons.Icons.Default.CheckCircle
            } else {
                androidx.compose.material.icons.Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

