package com.oterman.rundemo.domain.model

enum class ShoeType(val displayName: String, val defaultLifespan: Double) {
    training("训练鞋", 700.0),
    racing("竞速鞋", 350.0),
    carbon("碳板鞋", 300.0),
    trail("越野鞋", 600.0),
    daily("日常慢跑鞋", 800.0);

    companion object {
        fun fromString(value: String): ShoeType {
            return entries.find { it.name == value } ?: training
        }
    }
}

enum class ShoeSortType(val displayName: String) {
    updateTime("更新时间"),
    createTime("创建时间"),
    totalDistance("总距离"),
    totalRuns("总次数"),
    brand("品牌")
}

enum class ShoeColor(val displayName: String, val hexColor: Long) {
    red("红色", 0xFFE53935),
    orange("橙色", 0xFFFB8C00),
    yellow("黄色", 0xFFFDD835),
    green("绿色", 0xFF43A047),
    blue("蓝色", 0xFF1E88E5),
    purple("紫色", 0xFF8E24AA),
    pink("粉色", 0xFFD81B60),
    gray("灰色", 0xFF757575),
    black("黑色", 0xFF212121),
    white("白色", 0xFFEEEEEE);

    companion object {
        fun fromString(value: String?): ShoeColor? {
            return entries.find { it.name == value }
        }
    }
}
