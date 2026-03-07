package com.oterman.rundemo.data.network

import com.oterman.rundemo.data.network.api.DataSourceApi
import com.oterman.rundemo.data.network.api.RunDataApi
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.interceptor.AuthInterceptor
import com.oterman.rundemo.util.RLog
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端配置
 * 单例模式，提供API接口实例
 */
object RetrofitClient {
    
//    private const val BASE_URL = "https://yayarun.cn/sys/"
    private const val BASE_URL = "http://192.168.31.5:8080"

    private var tokenProvider: (() -> String?)? = null
    
    /**
     * 设置token提供者
     */
    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }
    
    /**
     * 日志拦截器
     * 使用自定义 Logger 将日志写入 RLog 文件，OkHttp 默认已打印到 logcat，此处不重复输出
     */
    private val loggingInterceptor = HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger { message ->
            RLog.d("OkHttp", message)
        }
    ).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * OkHttp客户端配置
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenProvider?.invoke() })
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit实例
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * 用户API接口实例
     */
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }
    
    /**
     * 数据源API接口实例
     */
    val dataSourceApi: DataSourceApi by lazy {
        retrofit.create(DataSourceApi::class.java)
    }

    /**
     * 跑步数据API接口实例
     */
    val runDataApi: RunDataApi by lazy {
        retrofit.create(RunDataApi::class.java)
    }
}

