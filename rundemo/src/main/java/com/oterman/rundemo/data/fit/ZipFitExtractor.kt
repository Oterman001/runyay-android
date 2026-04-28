package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import com.oterman.rundemo.util.RLog
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * 从 ZIP / GZ 压缩包中提取 FIT 文件到临时目录。
 *
 * 限制：
 *  - ZIP 包体积 ≤ MAX_ZIP_SIZE_BYTES（50 MB）
 *  - ZIP 内单个 FIT 条目 ≤ MAX_FIT_ENTRY_SIZE_BYTES（5 MB）
 *  - ZIP 内 FIT 文件数量 ≤ MAX_FIT_ENTRIES（200 个）
 */
object ZipFitExtractor {

    private const val TAG = "ZipFitExtractor"

    const val MAX_ZIP_SIZE_MB = 50
    const val MAX_FIT_ENTRY_SIZE_MB = 5
    const val MAX_FIT_ENTRIES = 200

    private val MAX_ZIP_SIZE_BYTES = MAX_ZIP_SIZE_MB * 1024L * 1024L
    private val MAX_FIT_ENTRY_SIZE_BYTES = MAX_FIT_ENTRY_SIZE_MB * 1024L * 1024L

    /** 临时解压目录名 */
    private const val TEMP_DIR = "fit_imports"

    /**
     * 判断 URI 对应的文件是否为 ZIP 格式。
     * 优先检查 MIME type，其次检查文件名后缀。
     */
    fun isZip(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)
        if (mime != null && (mime == "application/zip" || mime == "application/x-zip-compressed"
                    || mime == "application/x-zip" || mime == "application/octet-stream")) {
            // octet-stream 时还需要看文件名
            if (mime != "application/octet-stream") return true
        }
        val name = getFileName(context, uri)?.lowercase() ?: return false
        return name.endsWith(".zip")
    }

    /**
     * 判断 URI 对应的文件是否为 GZ 格式（.fit.gz 或 .gz）。
     */
    fun isGz(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)
        if (mime == "application/gzip" || mime == "application/x-gzip") return true
        val name = getFileName(context, uri)?.lowercase() ?: return false
        return name.endsWith(".gz")
    }

    /**
     * 从 ZIP 压缩包中提取指定扩展名的文件，返回临时文件列表。
     *
     * @param extension 目标文件扩展名，默认 ".fit"，可传 ".gpx" 等
     * @throws IOException 当压缩包超出大小限制、无匹配内容或解压失败时
     */
    @Throws(IOException::class)
    fun extractZip(context: Context, uri: Uri, extension: String = ".fit"): List<File> {
        val extLower = extension.lowercase()
        val extLabel = extension.uppercase().removePrefix(".")

        // 检查整包大小
        val zipSizeBytes = getFileSize(context, uri)
        if (zipSizeBytes > MAX_ZIP_SIZE_BYTES) {
            throw IOException("压缩包体积超过限制（${MAX_ZIP_SIZE_MB} MB），当前 ${zipSizeBytes / 1024 / 1024} MB")
        }

        val outDir = getTempDir(context)
        val extracted = mutableListOf<File>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zis ->
                var entry = zis.nextEntry
                var entryCount = 0

                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory && entryName.lowercase().endsWith(extLower)) {
                        if (entryCount >= MAX_FIT_ENTRIES) {
                            RLog.w(TAG, "ZIP 内 $extLabel 文件超过 $MAX_FIT_ENTRIES 个，已忽略剩余")
                            break
                        }

                        // 防止路径穿越攻击
                        val safeEntryName = entryName.replace('/', '_').replace('\\', '_')
                        val outFile = File(outDir, "zip_${System.currentTimeMillis()}_${entryCount}_$safeEntryName")

                        var written = 0L
                        outFile.outputStream().buffered().use { out ->
                            val buf = ByteArray(8192)
                            var len: Int
                            while (zis.read(buf).also { len = it } != -1) {
                                written += len
                                if (written > MAX_FIT_ENTRY_SIZE_BYTES) {
                                    outFile.delete()
                                    throw IOException("ZIP 内文件 \"$entryName\" 超过单文件限制（${MAX_FIT_ENTRY_SIZE_MB} MB）")
                                }
                                out.write(buf, 0, len)
                            }
                        }

                        if (outFile.exists() && outFile.length() > 0) {
                            extracted.add(outFile)
                            entryCount++
                            RLog.d(TAG, "解压 $extLabel: $entryName → ${outFile.name} (${written} bytes)")
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IOException("无法读取压缩包内容")

        if (extracted.isEmpty()) {
            throw IOException("压缩包中未找到 $extLabel 文件")
        }

        RLog.i(TAG, "ZIP 解压完成，共提取 ${extracted.size} 个 $extLabel 文件")
        return extracted
    }

    /**
     * 解压单个 GZ 压缩文件，返回临时文件。
     *
     * @param outputExtension 期望的输出扩展名，默认 ".fit"，可传 ".gpx" 等
     * @throws IOException 当文件超出大小限制或解压失败时
     */
    @Throws(IOException::class)
    fun extractGz(context: Context, uri: Uri, outputExtension: String = ".fit"): File {
        val outDir = getTempDir(context)
        val originalName = getFileName(context, uri) ?: "file${outputExtension}.gz"
        val gzSuffix = outputExtension.lowercase() + ".gz"
        val baseName = if (originalName.lowercase().endsWith(gzSuffix)) {
            originalName.dropLast(3) // 去掉 ".gz"，保留目标扩展名
        } else {
            originalName.substringBeforeLast('.') + outputExtension
        }
        val outFile = File(outDir, "gz_${System.currentTimeMillis()}_$baseName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            GZIPInputStream(input.buffered()).use { gzis ->
                var written = 0L
                outFile.outputStream().buffered().use { out ->
                    val buf = ByteArray(8192)
                    var len: Int
                    while (gzis.read(buf).also { len = it } != -1) {
                        written += len
                        if (written > MAX_FIT_ENTRY_SIZE_BYTES) {
                            outFile.delete()
                            throw IOException("GZ 解压后文件超过大小限制（${MAX_FIT_ENTRY_SIZE_MB} MB）")
                        }
                        out.write(buf, 0, len)
                    }
                }
                RLog.i(TAG, "GZ 解压完成: ${outFile.name} (${written} bytes)")
            }
        } ?: throw IOException("无法读取 GZ 文件内容")

        return outFile
    }

    /**
     * 清理临时解压目录中的所有文件。
     */
    fun cleanTempDir(context: Context) {
        try {
            val dir = getTempDir(context)
            dir.listFiles()?.forEach { it.delete() }
            RLog.d(TAG, "已清理临时目录: ${dir.absolutePath}")
        } catch (e: Exception) {
            RLog.w(TAG, "清理临时目录失败: ${e.message}")
        }
    }

    private fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
