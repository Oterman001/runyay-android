package com.oterman.rundemo.presentation.feature.share.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.presentation.feature.share.ShareMetricType
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 短图预览：地图截图 + Header(距离/日期/设备) + 指标网格 + 品牌区
 */
@Composable
fun ShortSharePreview(
    record: RunRecordEntity,
    mapSnapshot: Bitmap?,
    selectedMetrics: List<ShareMetricType>,
    showDate: Boolean,
    deviceName: String?,
    brandText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. 地图截图区域
        if (mapSnapshot != null) {
            Image(
                bitmap = mapSnapshot.asImageBitmap(),
                contentDescription = "运动轨迹",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                contentScale = ContentScale.Crop
            )
        } else {
            // 无地图时的占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "室内跑步",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        // 2. Header: 距离 + 日期 + 设备
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 距离
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.2f", record.totalDistance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = RunYayFontFamily
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 日期 + 时间段
            if (showDate) {
                Text(
                    text = formatShareDateTime(record.startTime, record.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 设备信息
            val displayDevice = deviceName ?: record.deviceVersion
            if (!displayDevice.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayDevice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 4. 指标网格
        ShareMetricsGrid(
            record = record,
            selectedMetrics = selectedMetrics
        )

        // 5. 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 6. 品牌区
        AppBrandingSection(brandText = brandText)
    }
}

/**
 * 格式化分享日期时间: "2024.12.28(六) 14:30-15:45"
 */
private fun formatShareDateTime(startTime: Long, endTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    val dayOfWeek = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { time = startDate }
    val weekDay = dayOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "${dateFormat.format(startDate)}(周$weekDay) ${timeFormat.format(startDate)}-${timeFormat.format(endDate)}"
}
