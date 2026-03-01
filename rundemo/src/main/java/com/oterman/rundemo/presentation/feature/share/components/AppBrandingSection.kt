package com.oterman.rundemo.presentation.feature.share.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.R

/**
 * 品牌区域：App图标 + 品牌文案
 * 显示在分享图底部
 */
@Composable
fun AppBrandingSection(
    brandText: String,
    modifier: Modifier = Modifier
) {
    val displayText = brandText.ifBlank { getRandomBrandText() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App 图标
        Image(
            painter = painterResource(id = R.drawable.run_demo),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 品牌文案
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "呀呀跑",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

private val brandTexts = listOf(
    "每一步都算数",
    "跑出自己的节奏",
    "用脚步丈量世界",
    "坚持跑步，遇见更好的自己",
    "奔跑是最好的解压方式",
    "今天的汗水，明天的勋章",
    "跑步让生活更美好",
    "一步一脚印，一路向前",
    "用跑步记录生活"
)

private fun getRandomBrandText(): String {
    return brandTexts.random()
}
