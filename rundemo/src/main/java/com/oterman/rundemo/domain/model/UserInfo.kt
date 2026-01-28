package com.oterman.rundemo.domain.model

/**
 * 用户信息领域模型
 * 对应iOS的UserInfo
 */
data class UserInfo(
    val userId: String,
    val userName: String?,
    val phoneNumber: String?,
    val email: String?,
    val token: String,
    val imageUrl: String?,
    val tokenExpireDate: Long? = null
)

