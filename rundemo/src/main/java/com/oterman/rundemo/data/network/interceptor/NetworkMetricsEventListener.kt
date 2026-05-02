package com.oterman.rundemo.data.network.interceptor

import android.content.Context
import com.tencent.bugly.crashreport.CrashReport
import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException

class NetworkMetricsEventListener(private val context: Context) : EventListener() {

    private var callStartNs = 0L

    override fun callStart(call: Call) {
        callStartNs = System.nanoTime()
    }

    override fun callEnd(call: Call) {
        val durationMs = (System.nanoTime() - callStartNs) / 1_000_000
        val path = call.request().url.encodedPath
        CrashReport.putUserData(context, "net_last", "$path ${durationMs}ms")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        CrashReport.postCatchedException(ioe)
    }
}
