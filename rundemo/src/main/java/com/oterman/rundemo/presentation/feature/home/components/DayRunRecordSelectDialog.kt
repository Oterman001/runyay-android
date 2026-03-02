package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.DayRunRecordInfo
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dialog for selecting a run record when there are multiple records on the same day
 */
@Composable
fun DayRunRecordSelectDialog(
    dayData: DayRunData,
    onRecordSelected: (workoutId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
    val dateString = dateFormat.format(dayData.date)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Text(
                    text = "$dateString 跑步记录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "该日共有 ${dayData.runCount} 条记录，请选择查看",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Record list
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(dayData.recordInfos) { record ->
                        RecordSelectItem(
                            record = record,
                            onClick = { onRecordSelected(record.workoutId) }
                        )

                        if (record != dayData.recordInfos.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "取消",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single record item in the selection dialog
 */
@Composable
private fun RecordSelectItem(
    record: DayRunRecordInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start time
            Text(
                text = record.startTime,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Distance
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.2f", record.distance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RunTheme.colorScheme.blue
                )
                Text(
                    text = " km",
                    fontSize = 12.sp,
                    color = SecondaryTextColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

//            // Duration with icon
//            Icon(
//                imageVector = Icons.Outlined.Schedule,
//                contentDescription = null,
//                modifier = Modifier.size(14.dp),
//                tint = SecondaryTextColor
//            )
//            Spacer(modifier = Modifier.width(2.dp))
//            Text(
//                text = record.duration,
//                fontSize = 13.sp,
//                color = SecondaryTextColor
//            )

            Spacer(modifier = Modifier.weight(1f))

            // Device info with icon
            if (!record.deviceInfo.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
//                Icon(
//                    imageVector = Icons.Outlined.Watch,
//                    contentDescription = null,
//                    modifier = Modifier.size(14.dp),
//                    tint = SecondaryTextColor
//                )
//                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = record.deviceInfo,
                    fontSize = 12.sp,
                    color = SecondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
