package com.oterman.rundemo.data.network

import android.content.Context
import com.oterman.rundemo.data.network.api.DataSourceApi
import com.oterman.rundemo.data.network.api.FitFileApi
import com.oterman.rundemo.data.network.api.RunDataApi
import com.oterman.rundemo.data.network.api.RunningShoeApi
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.api.VersionApi
import com.oterman.rundemo.data.network.interceptor.AuthInterceptor
import com.oterman.rundemo.data.network.interceptor.NetworkMetricsEventListener
import com.oterman.rundemo.data.network.interceptor.SmartLoggingInterceptor
import com.oterman.rundemo.data.network.interceptor.TokenRefreshRetryInterceptor
import com.oterman.rundemo.data.repository.TokenRefreshManager
import com.oterman.rundemo.util.RLog
import okhttp3.EventListener
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
    
    private const val BASE_URL = "https://yayarun.cn/sys/"
//    private const val BASE_URL = "http://192.168.31.130:8080"

    private var tokenProvider: (() -> String?)? = null
    private var appContext: Context? = null
    private var onNewTokenCallback: ((String) -> Unit)? = null

    /**
     * 设置token提供者
     */
    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    /**
     * 设置 Application Context（用于懒加载 TokenRefreshManager）
     */
    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 设置新 Token 回调（服务端通过响应头 x-new-token 下发新 Token 时触发）
     */
    fun setOnNewTokenCallback(callback: (String) -> Unit) {
        onNewTokenCallback = callback
    }

    private val tokenRefreshManager: TokenRefreshManager?
        get() = appContext?.let { TokenRefreshManager.getInstance(it) }
    
    /**
     * 日志拦截器
     * 自动识别二进制内容，只打印摘要，避免日志中出现二进制乱码
     */
    private val loggingInterceptor = SmartLoggingInterceptor()


    /**
     * 日志拦截器  会打印body完整内容
     * 使用自定义 Logger 将日志写入 RLog 文件，OkHttp 默认已打印到 logcat，此处不重复输出
     */
//    private val loggingInterceptor = HttpLoggingInterceptor(
//        HttpLoggingInterceptor.Logger { message ->
//            RLog.d("OkHttp", message)
//        }
//    ).apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }


    /**
     * OkHttp客户端配置
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(TokenRefreshRetryInterceptor { tokenRefreshManager })
            .addInterceptor(AuthInterceptor(
                tokenProvider = { tokenProvider?.invoke() },
                tokenRefreshManagerProvider = { tokenRefreshManager },
                onNewToken = { newToken ->
                    RLog.d("RetrofitClient", "[TokenRenewal] callback triggered, forwarding new token")
                    onNewTokenCallback?.invoke(newToken)
                }
            ))
            .addInterceptor(loggingInterceptor)
            .eventListenerFactory(EventListener.Factory { appContext?.let { NetworkMetricsEventListener(it) } ?: EventListener.NONE })
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

    /**
     * FIT文件上传API接口实例
     */
    val fitFileApi: FitFileApi by lazy {
        retrofit.create(FitFileApi::class.java)
    }

    /**
     * 跑鞋管理API接口实例
     */
    val runningShoeApi: RunningShoeApi by lazy {
        retrofit.create(RunningShoeApi::class.java)
    }

    /**
     * 版本检测API接口实例
     */
    val versionApi: VersionApi by lazy {
        retrofit.create(VersionApi::class.java)
    }
}

