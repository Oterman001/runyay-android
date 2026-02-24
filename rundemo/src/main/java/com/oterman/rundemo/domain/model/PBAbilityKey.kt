package com.oterman.rundemo.domain.model

/**
 * PB Ability item type enum
 * Corresponds to iOS PBAbilityItemKey
 */
enum class PBAbilityKey(val description: String, val subType: String) {
    MAX_VDOT("最大跑力", "MVdot"),
    MAX_DISTANCE("最远距离", "MDistance"),
    MAX_DURATION("最长时间", "MTime")
}
