package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.TrainWholeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainWholeTypeSelector(
    selected: TrainWholeType,
    onSelected: (TrainWholeType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        TrainWholeType.SELF_DEFINE to "自定义",
        TrainWholeType.DISTANCE to "距离",
        TrainWholeType.TIME to "时间",
        TrainWholeType.CALORIES to "卡路里",
        TrainWholeType.PACER to "配速"
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = label, fontSize = 12.sp)
            }
        }
    }
}
