package com.oterman.rundemo

import com.oterman.rundemo.data.fit.VdotCalculator
import com.oterman.rundemo.data.fit.VdotResult
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * VDOT跑力算法优化单元测试
 */
class VdotCalculatorTest {

    // ==================== 体感温度测试 ====================

    @Test
    fun `getApparentTemperature - below 20C returns actual temperature`() {
        assertEquals(15.0, VdotCalculator.getApparentTemperature(15.0, 80.0), 0.01)
        assertEquals(10.0, VdotCalculator.getApparentTemperature(10.0, 90.0), 0.01)
    }

    @Test
    fun `getApparentTemperature - above 20C with humidity increases temperature`() {
        val apparent = VdotCalculator.getApparentTemperature(30.0, 80.0)
        assertTrue("体感温度应高于实际温度: $apparent", apparent > 30.0)
    }

    @Test
    fun `getApparentTemperature - null humidity returns actual temperature`() {
        assertEquals(35.0, VdotCalculator.getApparentTemperature(35.0, null), 0.01)
    }

    @Test
    fun `getApparentTemperature - zero humidity returns actual temperature`() {
        assertEquals(35.0, VdotCalculator.getApparentTemperature(35.0, 0.0), 0.01)
    }

    // ==================== 温度效应测试 ====================

    @Test
    fun `getTemperatureEffect - optimal zone returns zero`() {
        assertEquals(0.0, VdotCalculator.getTemperatureEffect(14.0, null, 60.0), 0.001)
        assertEquals(0.0, VdotCalculator.getTemperatureEffect(10.0, null, 60.0), 0.001)
        assertEquals(0.0, VdotCalculator.getTemperatureEffect(18.0, null, 60.0), 0.001)
    }

    @Test
    fun `getTemperatureEffect - high temperature increases effect`() {
        val effect30 = VdotCalculator.getTemperatureEffect(30.0, null, 60.0)
        val effect25 = VdotCalculator.getTemperatureEffect(25.0, null, 60.0)
        assertTrue("30°C效应应大于25°C: $effect30 > $effect25", effect30 > effect25)
        assertTrue("效应应为正值", effect30 > 0)
    }

    @Test
    fun `getTemperatureEffect - low temperature has lighter penalty than high`() {
        val highTempEffect = VdotCalculator.getTemperatureEffect(28.0, null, 60.0)
        val lowTempEffect = VdotCalculator.getTemperatureEffect(0.0, null, 60.0) // 10°C偏差
        // 高温28°C偏差10°C vs 低温0°C偏差10°C，高温惩罚更重
        assertTrue("高温惩罚应重于低温: $highTempEffect > $lowTempEffect", highTempEffect > lowTempEffect)
    }

    @Test
    fun `getTemperatureEffect - capped at 10 percent`() {
        val timeMinute = 60.0
        val effect = VdotCalculator.getTemperatureEffect(50.0, 90.0, timeMinute)
        val maxAllowed = timeMinute * 0.10
        assertTrue("效应不应超过10%: $effect <= $maxAllowed", effect <= maxAllowed + 0.001)
    }

    @Test
    fun `getTemperatureEffect - humidity amplifies high temp effect`() {
        val noHumidity = VdotCalculator.getTemperatureEffect(30.0, null, 60.0)
        val highHumidity = VdotCalculator.getTemperatureEffect(30.0, 90.0, 60.0)
        assertTrue("高湿度应放大高温效应: $highHumidity > $noHumidity", highHumidity > noHumidity)
    }

    @Test
    fun `getTemperatureEffect - null temperature returns zero`() {
        assertEquals(0.0, VdotCalculator.getTemperatureEffect(null, null, 60.0), 0.001)
    }

    @Test
    fun `getTemperatureEffect - zero temperature returns zero`() {
        assertEquals(0.0, VdotCalculator.getTemperatureEffect(0.0, null, 60.0), 0.001)
    }

    // ==================== VdotResult 测试 ====================

    @Test
    fun `calculateWithResult - returns structured result`() {
        val result = VdotCalculator.calculateWithResult(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRate = 160.0,
            temperature = 15.0,
            humidity = 50.0,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertNotNull(result)
        assertTrue("VDOT应为正值", result!!.vdot > 0)
        assertTrue("置信度应在0-1之间", result.confidence in 0.0..1.0)
        assertTrue("rawVdot应为正值", result.rawVdot > 0)
    }

    @Test
    fun `calculateWithResult - invalid input returns null`() {
        assertNull(VdotCalculator.calculateWithResult(0.0, 50.0, 160.0, maxHR = 190.0, restHR = 60.0))
        assertNull(VdotCalculator.calculateWithResult(10000.0, 0.0, 160.0, maxHR = 190.0, restHR = 60.0))
        assertNull(VdotCalculator.calculateWithResult(10000.0, 50.0, 0.0, maxHR = 190.0, restHR = 60.0))
    }

    @Test
    fun `calculateFromDistanceAndTime - backward compatible`() {
        // 无湿度参数应正常工作
        val vdot = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRate = 160.0,
            temperature = 15.0,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertTrue("VDOT应为正值", vdot > 0)
    }

    // ==================== 置信度测试 ====================

    @Test
    fun `calculateConfidence - 10K race has high confidence`() {
        val confidence = VdotCalculator.calculateConfidence(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRateZone = 4,
            hasTemperature = true
        )
        assertTrue("10K比赛置信度应高: $confidence", confidence >= 0.8)
    }

    @Test
    fun `calculateConfidence - short easy jog has low confidence`() {
        val confidence = VdotCalculator.calculateConfidence(
            distanceMeters = 1500.0,
            timeMinute = 8.0,
            heartRateZone = 1,
            hasTemperature = false
        )
        assertTrue("短距离慢跑置信度应低: $confidence", confidence < 0.3)
    }

    @Test
    fun `calculateConfidence - marathon has high confidence`() {
        val confidence = VdotCalculator.calculateConfidence(
            distanceMeters = 42195.0,
            timeMinute = 240.0, // 虽然超过120min但距离可靠性高
            heartRateZone = 3,
            hasTemperature = true
        )
        assertTrue("马拉松置信度应中高: $confidence", confidence >= 0.6)
    }

    // ==================== 异常值检测测试 ====================

    @Test
    fun `detectOutliers - identifies extreme value`() {
        val values = listOf(45.0, 46.0, 44.0, 47.0, 45.0, 85.0) // 85是异常值
        val flags = VdotCalculator.detectOutliers(values)
        assertTrue("85应被标记为异常值", flags[5])
        assertFalse("45不应被标记为异常值", flags[0])
    }

    @Test
    fun `detectOutliers - no outliers in normal data`() {
        val values = listOf(45.0, 46.0, 44.0, 47.0, 45.5)
        val flags = VdotCalculator.detectOutliers(values)
        assertTrue("正常数据不应有异常值", flags.none { it })
    }

    @Test
    fun `detectOutliers - less than 3 values returns all false`() {
        val flags = VdotCalculator.detectOutliers(listOf(45.0, 85.0))
        assertEquals(2, flags.size)
        assertTrue("数据太少不检测", flags.none { it })
    }

    @Test
    fun `detectOutliers - all same values returns all false`() {
        val values = listOf(45.0, 45.0, 45.0, 45.0, 45.0)
        val flags = VdotCalculator.detectOutliers(values)
        assertTrue("相同值不应有异常值", flags.none { it })
    }

    @Test
    fun `detectOutliers - all outliers fallback to none`() {
        // 如果检测器认为所有值都是异常值，应回退
        val values = listOf(10.0, 50.0, 90.0)
        val flags = VdotCalculator.detectOutliers(values)
        // 不一定全部标记，但不应全部标记
        assertFalse("不应所有值都被标记", flags.all { it })
    }

    // ==================== Overall VDOT 测试 ====================

    @Test
    fun `calculateOverallVdot - first run returns origin`() {
        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 45.0,
            currentConfidence = 0.8,
            totalDistance = 10.0,
            activeDuration = 50.0
        )
        assertNotNull(overall)
        assertEquals("首次跑步综合=单次", 45.0, overall!!, 0.01)
    }

    @Test
    fun `calculateOverallVdot - recent runs have more weight`() {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000

        val history = listOf(
            createVdotEntity("w1", now - 1 * msPerDay, 40.0), // 1天前
            createVdotEntity("w2", now - 30 * msPerDay, 50.0), // 30天前
        )

        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = history,
            originVdot = 45.0,
            currentConfidence = 0.8,
            currentDateMs = now,
            totalDistance = 10.0,
            activeDuration = 50.0
        )
        assertNotNull(overall)
        // 结果应更接近45和40（近期），而非50（远期）
        assertTrue("综合跑力应在合理范围", overall!! in 40.0..50.0)
    }

    @Test
    fun `calculateOverallVdot - excluded records are filtered`() {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000

        val historyWithExcluded = listOf(
            createVdotEntity("w1", now - 1 * msPerDay, 40.0, inclusiveLevel = 0), // 被排除
            createVdotEntity("w2", now - 2 * msPerDay, 44.0, inclusiveLevel = 1),
        )

        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = historyWithExcluded,
            originVdot = 45.0,
            currentConfidence = 0.8,
            currentDateMs = now,
            totalDistance = 10.0,
            activeDuration = 50.0
        )
        assertNotNull(overall)
        // w1被排除，结果应基于45+44
        assertTrue("排除后综合跑力合理", overall!! in 43.0..46.0)
    }

    @Test
    fun `calculateOverallVdot - clamping limits delta`() {
        val now = System.currentTimeMillis()
        val previousOverall = 45.0

        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 55.0, // 比上次高10
            currentConfidence = 0.8,
            currentDateMs = now,
            totalDistance = 10.0,
            activeDuration = 50.0,
            previousOverallVdot = previousOverall
        )
        assertNotNull(overall)
        assertTrue("钳制应限制变化在3以内", overall!! <= previousOverall + 3.0 + 0.01)
    }

    @Test
    fun `calculateOverallVdot - no clamping on first calculation`() {
        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 55.0,
            currentConfidence = 0.8,
            totalDistance = 10.0,
            activeDuration = 50.0,
            previousOverallVdot = null // 首次
        )
        assertNotNull(overall)
        assertEquals("首次无钳制", 55.0, overall!!, 0.01)
    }

    @Test
    fun `calculateOverallVdot - insufficient data returns null`() {
        assertNull(VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 0.0,
            totalDistance = 10.0,
            activeDuration = 50.0
        ))
        assertNull(VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 45.0,
            totalDistance = 1.0,
            activeDuration = 10.0
        ))
    }

    @Test
    fun `calculateOverallVdot - outlier GPS anomaly is filtered`() {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000

        val history = listOf(
            createVdotEntity("w1", now - 1 * msPerDay, 45.0),
            createVdotEntity("w2", now - 3 * msPerDay, 44.0),
            createVdotEntity("w3", now - 5 * msPerDay, 46.0),
            createVdotEntity("w4", now - 7 * msPerDay, 85.0), // GPS异常
            createVdotEntity("w5", now - 10 * msPerDay, 45.0),
        )

        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = history,
            originVdot = 45.0,
            currentConfidence = 0.8,
            currentDateMs = now,
            totalDistance = 10.0,
            activeDuration = 50.0
        )
        assertNotNull(overall)
        // 85应被过滤，结果应在44-46范围
        assertTrue("异常值被过滤后结果合理: $overall", overall!! in 43.0..47.0)
    }

    @Test
    fun `calculateOverallVdot - long rest results in new run dominating`() {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000

        val history = listOf(
            createVdotEntity("w1", now - 60 * msPerDay, 50.0), // 60天前
            createVdotEntity("w2", now - 65 * msPerDay, 51.0), // 65天前
        )

        val overall = VdotCalculator.calculateOverallVdot(
            hisVdotList = history,
            originVdot = 42.0,
            currentConfidence = 0.8,
            currentDateMs = now,
            totalDistance = 10.0,
            activeDuration = 50.0
        )
        assertNotNull(overall)
        // 60+天前的权重很低，新跑(42)应主导
        assertTrue("长期休息后新跑主导: $overall", overall!! < 45.0)
    }

    // ==================== 回归测试：无湿度数据 ====================

    @Test
    fun `regression - no humidity matches temperature-only behavior`() {
        val vdotNoHumidity = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRate = 160.0,
            temperature = 15.0,
            humidity = null,
            maxHR = 190.0,
            restHR = 60.0
        )
        val vdotZeroHumidity = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRate = 160.0,
            temperature = 15.0,
            humidity = 0.0,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertEquals("null和0湿度应等价", vdotNoHumidity, vdotZeroHumidity, 0.001)
    }

    @Test
    fun `regression - optimal temp no humidity no adjustment`() {
        // 15°C在最佳区间(10-18)，应无调整
        val result = VdotCalculator.calculateWithResult(
            distanceMeters = 10000.0,
            timeMinute = 50.0,
            heartRate = 160.0,
            temperature = 15.0,
            humidity = null,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertNotNull(result)
        assertEquals("最佳温度无环境调整", 0.0, result!!.environmentalAdjustmentMinutes, 0.001)
    }

    // ==================== 海拔调整预留测试 ====================

    @Test
    fun `getAltitudeAdjustmentFactor - below 1000m no adjustment`() {
        assertEquals(1.0, VdotCalculator.getAltitudeAdjustmentFactor(500.0), 0.001)
        assertEquals(1.0, VdotCalculator.getAltitudeAdjustmentFactor(999.0), 0.001)
    }

    @Test
    fun `getAltitudeAdjustmentFactor - high altitude increases factor`() {
        val factor2000 = VdotCalculator.getAltitudeAdjustmentFactor(2000.0)
        assertTrue("2000m应有正向修正", factor2000 > 1.0)
        val factor3000 = VdotCalculator.getAltitudeAdjustmentFactor(3000.0)
        assertTrue("3000m修正大于2000m", factor3000 > factor2000)
    }

    // ==================== Helper ====================

    private fun createVdotEntity(
        workoutId: String,
        date: Long,
        originValue: Double,
        confidence: Double = 0.5,
        inclusiveLevel: Int = 1
    ) = OverallVdotEntity(
        workoutId = workoutId,
        date = date,
        originValue = originValue,
        value = originValue,
        confidence = confidence,
        inclusiveLevel = inclusiveLevel
    )
}
