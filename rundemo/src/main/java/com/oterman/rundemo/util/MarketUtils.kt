package com.oterman.rundemo.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object MarketUtils {

    data class MarketDef(val serverKey: String, val label: String, val pkgName: String)

    private val MARKETS = listOf(
        MarketDef("oppo",   "OPPO 软件商店", "com.heytap.market"),
        MarketDef("xiaomi", "小米应用商店",   "com.xiaomi.market"),
        MarketDef("vivo",   "Vivo 应用商店", "com.bbk.appstore"),
        MarketDef("huawei", "华为应用市场",   "com.huawei.appmarket"),
        MarketDef("google", "Google Play",   "com.android.vending"),
    )

    data class ResolvedMarket(val label: String, val url: String)

    /**
     * 遍历已知市场，返回第一个「设备已安装 + 服务端已上架」的市场和跳转 URL。
     * 未上架的市场（marketUrls 中无对应 key）直接跳过，不构造兜底链接。
     */
    fun resolve(context: Context, marketUrls: Map<String, String>?): ResolvedMarket? {
        if (marketUrls.isNullOrEmpty()) return null
        val pm = context.packageManager
        return MARKETS.firstNotNullOfOrNull { market ->
            val url = marketUrls[market.serverKey] ?: return@firstNotNullOfOrNull null
            val installed = runCatching { pm.getPackageInfo(market.pkgName, 0) }.isSuccess
            if (installed) ResolvedMarket(market.label, url) else null
        }
    }

    fun open(context: Context, resolved: ResolvedMarket): Boolean {
        return runCatching {
            val uri = Uri.parse(resolved.url)
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
