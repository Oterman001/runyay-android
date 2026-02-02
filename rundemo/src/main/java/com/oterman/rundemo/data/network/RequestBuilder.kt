package com.oterman.rundemo.data.network

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.RequestHead
import com.oterman.rundemo.util.Constants
import com.oterman.rundemo.util.SecurityUtils

/**
 * 请求构建器
 * 统一管理请求头构建、签名计算等逻辑
 */
object RequestBuilder {
    
    /**
     * 创建标准请求
     * 
     * @param dtoName DTO名称，用于body的key
     * @param data 请求数据
     * @param preferencesManager 用于获取token和userId
     * @return 构建好的BaseRequest
     */
    fun <T> createRequest(
        dtoName: String,
        data: T,
        preferencesManager: PreferencesManager
    ): BaseRequest<T> {
        val timestamp = SecurityUtils.getTimestamp()
        val sign = SecurityUtils.generateSign(
            params = emptyMap(),
            timestamp = timestamp,
            appKey = Constants.Network.APP_KEY
        )
        
        return BaseRequest(
            head = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = preferencesManager.getUserToken() ?: "",
                userId = preferencesManager.getUserId() ?: ""
            ),
            body = mapOf(dtoName to listOf(data))
        )
    }
    
    /**
     * 创建请求（自定义token和userId）
     * 用于登录等场景，此时可能还没有保存token/userId
     * 
     * @param dtoName DTO名称
     * @param data 请求数据
     * @param token 自定义token
     * @param userId 自定义userId
     * @return 构建好的BaseRequest
     */
    fun <T> createRequest(
        dtoName: String,
        data: T,
        token: String = "",
        userId: String = ""
    ): BaseRequest<T> {
        val timestamp = SecurityUtils.getTimestamp()
        val sign = SecurityUtils.generateSign(
            params = emptyMap(),
            timestamp = timestamp,
            appKey = Constants.Network.APP_KEY
        )
        
        return BaseRequest(
            head = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = token,
                userId = userId
            ),
            body = mapOf(dtoName to listOf(data))
        )
    }
}

