package com.oterman.rundemo.domain.map

/**
 * 轨迹配色集合（供应商无关）
 */
data class TrackColorSet(
    val track: String,
    val start: String,
    val end: String,
    val stroke: String,
    val kmBadgeBg: String,
    val kmBadgeText: String
)
