package com.oterman.rundemo.presentation.feature.auth.register.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.presentation.components.ShakeBox
import com.oterman.rundemo.presentation.components.SimpleTermsCheckbox
import com.oterman.rundemo.presentation.feature.auth.register.RegisterUiState

/**
 * 步骤1：手机号输入
 * 对应iOS的RegInputPhoneNumView
 */
@Composable
fun PhoneInputStep(
    uiState: RegisterUiState,
    onPhoneNumberChange: (String) -> Unit,
    onTermsToggle: () -> Unit,
    onSendCode: () -> Unit,
    onNavigateToUserTerms: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 手机号输入框
        PhoneNumberField(
            phoneNumber = uiState.phoneNumber,
            onPhoneNumberChange = onPhoneNumberChange,
            errorMessage = uiState.phoneNumberError
        )
        
        // 协议勾选框（带抖动效果）
        ShakeBox(shouldShake = uiState.shouldShake) {
            SimpleTermsCheckbox(
                checked = uiState.hasAgreedToTerms,
                onCheckedChange = { onTermsToggle() },
                onUserTermsClick = onNavigateToUserTerms,
                onPrivacyPolicyClick = onNavigateToPrivacyPolicy
            )
        }
        
        // 发送验证码按钮
        GradientButton(
            text = if (uiState.isLoading) "发送中..." else "发送验证码",
            onClick = onSendCode,
            isLoading = uiState.isLoading,
            enabled = uiState.phoneNumber.isNotEmpty() && uiState.phoneNumberError == null
        )
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
            placeholder = { Text("请输入手机号") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "手机图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

