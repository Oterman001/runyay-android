package com.oterman.rundemo.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 协议勾选框组件
 * 带有可点击的协议链接
 */
@Composable
fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onUserTermsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // 使用AnnotatedString来实现部分文本可点击
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("我已阅读并同意demorun的")
            }
            
            pushStringAnnotation(tag = "user_terms", annotation = "user_terms")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append("《用户协议》")
            }
            pop()
            
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("和")
            }
            
            pushStringAnnotation(tag = "privacy_policy", annotation = "privacy_policy")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append("《隐私政策》")
            }
            pop()
        }
        
        androidx.compose.foundation.text.ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall,
            onClick = { offset ->
                annotatedText.getStringAnnotations(
                    tag = "user_terms",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    onUserTermsClick()
                }
                
                annotatedText.getStringAnnotations(
                    tag = "privacy_policy",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    onPrivacyPolicyClick()
                }
            }
        )
    }
}

/**
 * 简化版协议勾选框（无链接跳转）
 */
@Composable
fun SimpleTermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "我已阅读并同意demorun的《用户协议》和《隐私政策》"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

