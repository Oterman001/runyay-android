package com.oterman.rundemo.domain.model

enum class DashboardCardId(val displayName: String) {
    NEXT_TRAIN_PLAN("训练安排"),
    TOTAL_VDOT("总跑量"),
    YEAR_MONTH("年月统计"),
    WEEK("本周"),
    LATEST_RUN("最近跑步"),
    STREAK("连续跑步"),
    PB_ABILITY("PB能力"),
    PB_SPEED("PB速度"),
    DAILY_SENTENCE("每日一句")
}

data class DashboardCardItem(
    val id: DashboardCardId,
    val visible: Boolean = true
)
