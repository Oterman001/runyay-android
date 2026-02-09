package com.oterman.rundemo.domain.model

/**
 * PB Ability item type enum
 * Corresponds to iOS PBAbilityItemKey
 */
enum class PBAbilityKey(val description: String) {
    MAX_VDOT("最大跑力"),
    MAX_DISTANCE("最远距离"),
    MAX_DURATION("最长时间")
}
