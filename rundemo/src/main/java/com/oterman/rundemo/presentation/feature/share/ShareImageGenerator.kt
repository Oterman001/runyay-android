package com.oterman.rundemo.presentation.feature.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Compose 离屏渲染为 Bitmap + 系统分享调用
 */
object ShareImageGenerator {

    /**
     * 将 Composable 内容离屏渲染为 Bitmap
     *
     * @param context Activity context
     * @param widthPx 渲染宽度（像素）
     * @param content 要渲染的 Composable
     * @return 渲染后的 Bitmap
     */
    suspend fun renderToBitmap(
        context: Context,
        widthPx: Int,
        content: @Composable () -> Unit
    ): Bitmap = suspendCancellableCoroutine { cont ->
        val composeView = ComposeView(context).apply {
            setContent {
                ComopseDemoHubTheme(darkTheme = false, dynamicColor = false) {
                    content()
                }
            }
        }

        // 使用固定宽度、wrap_content 高度进行 measure
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        // 等待 Compose 绘制完成
        composeView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                composeView.viewTreeObserver.removeOnPreDrawListener(this)

                val bitmap = Bitmap.createBitmap(
                    composeView.measuredWidth,
                    composeView.measuredHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                composeView.draw(canvas)

                if (cont.isActive) {
                    cont.resume(bitmap)
                }
                return true
            }
        })

        // 触发一次绘制
        composeView.invalidate()
    }

    /**
     * 保存 Bitmap 到缓存目录并返回 FileProvider URI
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String = "share_run.jpg"): android.net.Uri {
        val shareDir = File(context.cacheDir, "share_images")
        if (!shareDir.exists()) shareDir.mkdirs()

        val file = File(shareDir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 创建系统分享 Intent
     */
    fun createShareIntent(context: Context, bitmap: Bitmap): Intent {
        val uri = saveBitmapToCache(context, bitmap)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
