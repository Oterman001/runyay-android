package com.oterman.rundemo.presentation.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.avatar.UserAvatar

/**
 * User profile card component
 * Corresponds to iOS UserProfileCard
 */
@Composable
fun UserProfileCard(
    isLoggedIn: Boolean,
    userName: String?,
    phoneNumber: String?,
    userId: String? = null,
    avatarUrl: String? = null,
    isLoadingAvatar: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    AppCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = avatarUrl,
                isLoading = isLoadingAvatar,
                userId = userId,
                userName = userName,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                if (isLoggedIn) {
                    // Logged in state
                    Text(
                        text = userName ?: "用户",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    phoneNumber?.let { phone ->
                        Text(
                            text = maskPhoneNumber(phone),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Not logged in state
                    Text(
                        text = "未登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "点击登录，同步数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Mask phone number for privacy display
 * e.g., "13812345678" -> "138****5678"
 */
private fun maskPhoneNumber(phone: String): String {
    return if (phone.length >= 11) {
        "${phone.substring(0, 3)}****${phone.substring(7)}"
    } else {
        phone
    }
}
