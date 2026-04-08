package com.oterman.rundemo.presentation.feature.share.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.oterman.rundemo.R
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.domain.model.RunningShoe
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.components.AppCard
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
    userName: String? = null,
    showNickname: Boolean = true,
    linkedShoe: RunningShoe? = null,
    isPrivacyMode: Boolean = false,
    trackPoints: List<TrackPoint> = emptyList(),
    heartRateZone7Selected: Boolean = true,
    onHeartRateZoneChanged: ((Boolean) -> Unit)? = null,
    isIndoor: Boolean = false,
    segmentBarChartMode: Boolean = false,
    segmentMetricIndex: Int = 0,
    onSegmentBarChartModeChanged: ((Boolean) -> Unit)? = null,
    onSegmentMetricIndexChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    fun isCardEnabled(type: ShareCardType): Boolean = enabledCards[type] != false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 顶部间距：无地图（室内跑步）时补充，与底部保持对称
        if (isIndoor) {
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 1. 地图截图 + 底部渐变遮罩（室内跑步时整体隐藏）
        if (!isIndoor) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (mapSnapshot != null) {
                    val bitmapAspectRatio = mapSnapshot.width.toFloat() / mapSnapshot.height.toFloat()
                    Image(
                        bitmap = mapSnapshot.asImageBitmap(),
                        contentDescription = "运动轨迹",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmapAspectRatio),
                        contentScale = ContentScale.Fit
                    )
                } else if (isPrivacyMode && trackPoints.isNotEmpty()) {
                    val configuration = LocalConfiguration.current
                    val placeholderRatio = configuration.screenWidthDp.toFloat() /
                        (configuration.screenHeightDp * RunDetailLayoutConstants.MapHeightRatio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(placeholderRatio)
                    ) {
                        com.oterman.rundemo.presentation.feature.rundetail.components.PrivacyTrackView(
                            trackPoints = trackPoints,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 底部渐变遮罩（与短图一致，但使用 surface 色）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(RunDetailLayoutConstants.MapGradientHeight.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
        }

        // 2. Header + DataGrid（始终显示）
        if (isCardEnabled(ShareCardType.HEADER)) {
            RunDetailHeaderDataCard(
                distance = record.totalDistance,
                startTime = record.startTime,
                endTime = record.endTime,
                duration = record.activeDuration,
                deviceName = deviceName ?: com.oterman.rundemo.util.DeviceNameUtils.resolveDisplayName(record),
                isOutdoor = !isIndoor && (mapSnapshot != null || isPrivacyMode),
                metrics = metrics,
                avatarUrl = avatarUrl,
                userName = if (showNickname) userName else null,
                inclusiveLevel = record.inclusiveLevel,
                showInclusiveIndicator = false,
                indoorLabel = if (isIndoor) "(室内跑)" else null,
                modifier = if (!isIndoor) Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val invasionPx = kotlin.math.abs(
                        RunDetailLayoutConstants.HeaderInvasionOffset.dp.roundToPx()
                    )
                    layout(placeable.width, (placeable.height - invasionPx).coerceAtLeast(0)) {
                        placeable.placeRelative(0, -invasionPx)
                    }
                } else Modifier
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
            RunDetailSegmentTable(
                segments = segments,
                initialBarChartMode = segmentBarChartMode,
                initialMetricIndex = segmentMetricIndex,
                onBarChartModeChange = onSegmentBarChartModeChanged,
                onMetricIndexChange = onSegmentMetricIndexChanged
            )
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
                minHeartRate = record.minHeartRate,
                initialShow7Zone = heartRateZone7Selected,
                onZoneChanged = onHeartRateZoneChanged
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

        // 关联跑鞋卡片（所有图表之后）
        if (linkedShoe != null && isCardEnabled(ShareCardType.LINKED_SHOE)) {
            ShareLinkedShoeCard(shoe = linkedShoe)
            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
        }

        // 品牌卡片
        AppCard(
            modifier = Modifier.padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
        ) {
            AppBrandingSection(brandText = brandText)
        }
        Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
    }
}

/**
 * 长图中的跑鞋展示卡片（只读，不含"更换"按钮）
 */
@Composable
private fun ShareLinkedShoeCard(shoe: RunningShoe) {
    AppCard(
        modifier = Modifier.padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 跑鞋图片
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageSource = shoe.displayImageSource
                if (imageSource != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageSource)
                            .allowHardware(false)
                            .memoryCacheKey("shoe_${shoe.id}_${shoe.updatedAt}")
                            .diskCacheKey("shoe_${shoe.id}_${shoe.updatedAt}")
                            .build(),
                        contentDescription = shoe.displayName,
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Image(
                                painter = painterResource(R.drawable.svg_setting_shoes),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.svg_setting_shoes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = shoe.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!shoe.displaySubtitle.isNullOrBlank()) {
                    Text(
                        text = shoe.displaySubtitle!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "%.1f km".format(shoe.effectiveDistance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${shoe.totalRuns} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${shoe.usageDays} 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
