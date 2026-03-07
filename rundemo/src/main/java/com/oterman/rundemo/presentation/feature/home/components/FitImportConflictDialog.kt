package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FitImportConflictDialog(
    conflictingRecords: List<RunRecordEntity>,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onViewDetail: (workoutId: String) -> Unit
) {
    Dialog(onDismissRequest = onSkip) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "发现时间冲突",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "以下跑步记录与本次导入时间重叠，是否继续导入？",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(conflictingRecords) { record ->
                        ConflictRecordItem(
                            record = record,
                            onClick = { onViewDetail(record.workoutId) }
                        )
                        if (record != conflictingRecords.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSkip) {
                        Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("继续导入", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictRecordItem(
    record: RunRecordEntity,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
    val startTimeStr = timeFormat.format(Date(record.startTime))
    val datasourceLabel = record.datasource ?: ""

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
            Text(
                text = startTimeStr,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.2f", record.totalDistance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RunTheme.colorScheme.blue
                )
                Text(
                    text = " km",
                    fontSize = 12.sp,
                    color = SecondaryTextColor
                )
            }

            if (datasourceLabel.isNotEmpty()) {
                Text(
                    text = "  $datasourceLabel",
                    fontSize = 12.sp,
                    color = SecondaryTextColor
                )
            }
        }
    }
}
