package com.oterman.rundemo.presentation.feature.share

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Compose 离屏渲染为 Bitmap + 系统分享调用
 */
object ShareImageGenerator {

    private const val TAG = "ShareImageGenerator"

    private fun unwrapActivity(context: Context): Activity {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        throw IllegalArgumentException("Context must be backed by an Activity")
    }

    /**
     * 将 Composable 内容离屏渲染为 Bitmap
     *
     * 通过将 ComposeView 临时附加到 Activity 窗口获取 WindowRecomposer，
     * 完成 Composition、measure、layout、draw 后移除并返回 Bitmap。
     *
     * @param context Activity context（或其 ContextWrapper）
     * @param widthPx 渲染宽度（像素）
     * @param content 要渲染的 Composable
     * @return 渲染后的 Bitmap
     */
    suspend fun renderToBitmap(
        context: Context,
        widthPx: Int,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ): Bitmap = suspendCancellableCoroutine { cont ->
        try {
            val activity = unwrapActivity(context)
            val rootView = activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)

            val composeView = ComposeView(activity).apply {
                setContent {
                    ComopseDemoHubTheme(darkTheme = darkTheme, dynamicColor = false) {
                        content()
                    }
                }
            }

            // 临时添加到窗口（零尺寸，不影响现有布局），让 Compose 获取 Recomposer
            // 向左平移移出屏幕，防止后续 layout() 时内容短暂显示在屏幕上造成闪烁
            composeView.translationX = -(widthPx + 100).toFloat()
            rootView.addView(
                composeView,
                ViewGroup.LayoutParams(0, 0)
            )

            cont.invokeOnCancellation {
                rootView.removeView(composeView)
            }

            composeView.post {
                try {
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    composeView.measure(widthSpec, heightSpec)
                    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

                    // 延迟等待 Composition 完成渲染（长图复杂，600ms）
                    composeView.postDelayed({
                        try {
                            // 重新测量，确保拿到 composition 完成后的真实尺寸
                            composeView.measure(widthSpec, heightSpec)
                            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

                            val w = composeView.measuredWidth
                            val h = composeView.measuredHeight
                            if (w <= 0 || h <= 0) {
                                rootView.removeView(composeView)
                                if (cont.isActive) {
                                    cont.resumeWithException(
                                        IllegalStateException("ComposeView measured to ${w}x${h}")
                                    )
                                }
                                return@postDelayed
                            }

                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            composeView.draw(canvas)

                            rootView.removeView(composeView)

                            if (cont.isActive) {
                                cont.resume(bitmap)
                            }
                        } catch (e: Exception) {
                            rootView.removeView(composeView)
                            if (cont.isActive) cont.resumeWithException(e)
                        }
                    }, 600)
                } catch (e: Exception) {
                    rootView.removeView(composeView)
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            RLog.e(TAG, "renderToBitmap failed", e)
            if (cont.isActive) cont.resumeWithException(e)
        }
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

    /**
     * 将 Bitmap 保存到系统相册（Pictures/RunDemo/）。
     * - Android Q+：通过 [MediaStore]，无需存储权限。
     * - Android P 及以下：写入公共 Pictures 目录并触发媒体扫描（需 Manifest 中 WRITE_EXTERNAL_STORAGE，maxSdk 28）。
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Result<Uri> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/RunDemo"
                    )
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("无法创建相册条目"))
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                        resolver.delete(uri, null, null)
                        return Result.failure(IllegalStateException("图片写入失败"))
                    }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return Result.failure(IllegalStateException("无法打开输出流"))
                }
                // 通过构造预期路径触发媒体扫描，确保相册等应用立即感知到新图片
                @Suppress("DEPRECATION")
                val expectedPath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "RunDemo/$fileName"
                ).absolutePath
                MediaScannerConnection.scanFile(context, arrayOf(expectedPath), arrayOf("image/jpeg"), null)
                Result.success(uri)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val dir = File(picturesDir, "RunDemo")
                if (!dir.exists() && !dir.mkdirs()) {
                    return Result.failure(IllegalStateException("无法创建目录"))
                }
                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)) {
                        return Result.failure(IllegalStateException("图片写入失败"))
                    }
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
                Result.success(Uri.fromFile(file))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "saveToGallery failed", e)
            Result.failure(e)
        }
    }
}
