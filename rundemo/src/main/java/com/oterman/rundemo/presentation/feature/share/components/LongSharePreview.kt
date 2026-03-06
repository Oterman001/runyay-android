package com.oterman.rundemo.presentation.feature.share.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem
import com.oterman.rundemo.presentation.feature.rundetail.components.AltitudeChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.CadenceChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.ContactTimeChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.HeartRateChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.PaceChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.PowerChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailHeaderDataCard
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailSegmentTable
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailTrainingSegmentTable
import com.oterman.rundemo.presentation.feature.rundetail.components.StrideLengthChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.TrainingEffectCard
import com.oterman.rundemo.presentation.feature.rundetail.components.VO2MaxCard
import com.oterman.rundemo.presentation.feature.rundetail.components.VerticalOscillationChartCard
import com.oterman.rundemo.presentation.feature.share.ShareCardType

/**
 * 长图预览：完整详情页所有卡片
 * 使用 Column（非LazyColumn），以支持离屏渲染为 Bitmap
 */
@Composable
fun LongSharePreview(
    record: RunRecordEntity,
    mapSnapshot: Bitmap?,
    metrics: List<RunMetricItem>,
    enabledCards: Map<ShareCardType, Boolean>,
    segments: List<RunSegment>,
    trainingSegments: List<RunSegment>,
    mergedTrainingSegments: List<MergedRunSegment>,
    heartRateSeries: List<ChartDataPoint>,
    speedSeries: List<ChartDataPoint>,
    cadenceSeries: List<ChartDataPoint>,
    powerSeries: List<ChartDataPoint>,
    strideLengthSeries: List<ChartDataPoint>,
    verticalOscillationSeries: List<ChartDataPoint>,
    contactTimeSeries: List<ChartDataPoint>,
    altitudeSeries: List<ChartDataPoint>,
    heartRate7Zones: List<AbilityZone>,
    heartRate5Zones: List<AbilityZone>,
    speedZones: List<AbilityZone>,
    vo2Max: Double?,
    previousVo2Max: Double?,
    showDate: Boolean,
    deviceName: String?,
    brandText: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    fun isCardEnabled(type: ShareCardType): Boolean = enabledCards[type] != false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. 地图截图
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Text("室内跑步", color = Color.Gray, fontSize = 16.sp)
            }
        }

        // 2. Header + DataGrid（始终显示）
        if (isCardEnabled(ShareCardType.HEADER)) {
            RunDetailHeaderDataCard(
                distance = record.totalDistance,
                startTime = record.startTime,
                endTime = record.endTime,
                duration = record.activeDuration,
                deviceName = deviceName ?: record.deviceVersion,
                isOutdoor = mapSnapshot != null,
                metrics = metrics,
                avatarUrl = avatarUrl,
                inclusiveLevel = record.inclusiveLevel
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 3. VO2Max
        if (isCardEnabled(ShareCardType.VO2MAX) && vo2Max != null && vo2Max > 0) {
            VO2MaxCard(vo2Max = vo2Max, previousVo2Max = previousVo2Max)
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 4. 训练效果
        if (isCardEnabled(ShareCardType.TRAINING_EFFECT) &&
            (record.trainingEffect > 0 || record.anaerobicTrainingEffect > 0)) {
            TrainingEffectCard(
                aerobicEffect = record.trainingEffect,
                anaerobicEffect = record.anaerobicTrainingEffect
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 5. 公里分段
        if (isCardEnabled(ShareCardType.KM_SEGMENTS) && segments.isNotEmpty()) {
            RunDetailSegmentTable(segments = segments)
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 6. 训练分段
        if (isCardEnabled(ShareCardType.TRAINING_SEGMENTS) && trainingSegments.isNotEmpty()) {
            RunDetailTrainingSegmentTable(
                segments = trainingSegments,
                mergedSegments = mergedTrainingSegments,
                expandedSegmentIds = emptySet(),
                onToggleExpansion = {}
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 7. 心率图表
        if (isCardEnabled(ShareCardType.HEART_RATE) && heartRateSeries.isNotEmpty()) {
            HeartRateChartCard(
                heartRateSeries = heartRateSeries,
                heartRate7Zones = heartRate7Zones,
                heartRate5Zones = heartRate5Zones,
                avgHeartRate = record.averageHeartRate,
                maxHeartRate = record.maxHeartRate,
                minHeartRate = record.minHeartRate
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 8. 配速图表
        if (isCardEnabled(ShareCardType.PACE) && speedSeries.isNotEmpty()) {
            PaceChartCard(
                speedSeries = speedSeries,
                speedZones = speedZones,
                avgSpeed = record.averageSpeed,
                maxSpeed = record.maxSpeed
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 9. 海拔图表
        if (isCardEnabled(ShareCardType.ALTITUDE) && altitudeSeries.isNotEmpty()) {
            AltitudeChartCard(
                altitudeSeries = altitudeSeries,
                elevationAscended = record.elevationAscended
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 10. 步幅图表
        if (isCardEnabled(ShareCardType.STRIDE_LENGTH) && strideLengthSeries.isNotEmpty()) {
            StrideLengthChartCard(
                strideLengthSeries = strideLengthSeries,
                avgStrideLength = record.averageStrideLength
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 11. 步频图表
        if (isCardEnabled(ShareCardType.CADENCE) && cadenceSeries.isNotEmpty()) {
            CadenceChartCard(
                cadenceSeries = cadenceSeries,
                avgCadence = record.averageCadence
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 12. 触地时间图表
        if (isCardEnabled(ShareCardType.CONTACT_TIME) && contactTimeSeries.isNotEmpty()) {
            ContactTimeChartCard(
                contactTimeSeries = contactTimeSeries,
                avgContactTime = record.averageContactTime
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 13. 垂直振幅图表
        if (isCardEnabled(ShareCardType.VERTICAL_OSCILLATION) && verticalOscillationSeries.isNotEmpty()) {
            VerticalOscillationChartCard(
                verticalOscillationSeries = verticalOscillationSeries,
                avgVerticalOscillation = record.averageVerticalOscillation
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 14. 功率图表
        if (isCardEnabled(ShareCardType.POWER) && powerSeries.isNotEmpty()) {
            PowerChartCard(
                powerSeries = powerSeries,
                avgPower = record.averagePower
            )
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 底部分隔线 + 品牌区
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AppBrandingSection(brandText = brandText)
    }
}
