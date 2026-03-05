package com.oterman.rundemo

import android.app.Application
import android.content.Context
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.util.RLog
import com.umeng.commonsdk.UMConfigure

class MyRunApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        RLog.i("MyRunApplication","onCreate")

        /**
         * 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
         * 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
         * UMConfigure.init调用中appkey和channel参数请置为null）。
         */
        UMConfigure.preInit(this,"69a930fe6f259537c76ddab0", BuildConfig.UMENG_CHANNEL)


        //初始化组件化基础库, 所有友盟业务SDK都必须调用此初始化接口。
        UMConfigure.init(this, "69a930fe6f259537c76ddab0", BuildConfig.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "");
    }
}