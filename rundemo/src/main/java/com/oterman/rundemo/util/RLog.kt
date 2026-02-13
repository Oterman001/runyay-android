package com.oterman.rundemo.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志工具类
 *
 * 功能：
 * 1. 自动在日志末尾添加文件名和行号，方便在 logcat 中点击跳转
 * 2. 默认输出到文件，支持日志文件自动轮转
 * 3. 日志文件使用日期命名，单文件最大 5MB
 * 4. 最多保留 5 个日志文件，自动删除最旧的
 * 5. 支持配置 tag 前缀，便于区分不同应用的日志
 *
 * 使用示例：
 * ```
 * // 初始化（在 MainActivity.onCreate 中）
 * Logger.init(this, prefix = "XRUN")
 *
 * // 定义 tag 变量
 * companion object {
 *     private const val TAG = "LoginViewModel"
 * }
 *
 * // 使用
 * Logger.d(TAG, "用户登录开始")
 * Logger.e(TAG, "登录失败", exception)
 * ```
 *
 * Logcat 输出示例：
 * ```
 * D/XRUN_LoginViewModel: 用户登录开始 (LoginViewModel.kt:108)
 * ```
 *
 * 文件输出示例：
 * ```
 * 2026-01-29 15:30:45.123 | D | XRUN_LoginViewModel | 用户登录开始 | (LoginViewModel.kt:108)
 * ```
 *
 * 日志文件位置：
 * ```
 * /data/data/com.oterman.rundemo/files/logs/rundemo_2026-01-29.log
 * /data/data/com.oterman.rundemo/files/logs/rundemo_2026-01-29_001.log
 * ```
 */
object RLog {

    private var enableFileLogging = false
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private var tagPrefix: String = ""

    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    private const val MAX_FILE_COUNT = 5
    private const val LOG_FILE_PREFIX = "rundemo_"
    private const val LOG_FILE_EXTENSION = ".log"

    /**
     * 初始化日志系统
     *
     * @param context 应用上下文
     * @param enableFile 是否启用文件日志（默认启用）
     * @param prefix tag 前缀（可选，用于区分不同应用的日志）
     */
    fun init(context: Context, enableFile: Boolean = true, prefix: String = "XRUN") {
        enableFileLogging = enableFile
        tagPrefix = prefix
        if (enableFile) {
            logDir = File(context.filesDir, "logs")
            logDir?.mkdirs()
            currentLogFile = getOrCreateLogFile()
            cleanupOldLogFiles()
        }
    }

    /**
     * Verbose 级别日志
     */
    fun v(tag: String, message: String) = log(Log.VERBOSE, tag, message)

    /**
     * Debug 级别日志
     */
    fun d(tag: String, message: String) = log(Log.DEBUG, tag, message)

    /**
     * Info 级别日志
     */
    fun i(tag: String, message: String) = log(Log.INFO, tag, message)

    /**
     * Warning 级别日志
     */
    fun w(tag: String, message: String) = log(Log.WARN, tag, message)

    /**
     * Error 级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 异常对象（可选）
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, message, throwable)
    }

    /**
     * 核心日志方法
     */
    private fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        val location = getLocation()
        val fullMessage = "$message $location"
        val fullTag = formatTag(tag)

        // 输出到 logcat
        when (priority) {
            Log.VERBOSE -> Log.v(fullTag, fullMessage, throwable)
            Log.DEBUG -> Log.d(fullTag, fullMessage, throwable)
            Log.INFO -> Log.i(fullTag, fullMessage, throwable)
            Log.WARN -> Log.w(fullTag, fullMessage, throwable)
            Log.ERROR -> Log.e(fullTag, fullMessage, throwable)
        }

        // 写入文件
        if (enableFileLogging) {
            writeToFile(priorityToString(priority), fullTag, message, location, throwable)
        }
    }

    /**
     * 格式化 tag，添加前缀
     * 例如：LoginViewModel -> XRUN_LoginViewModel
     */
    private fun formatTag(tag: String): String {
        return if (tagPrefix.isNotEmpty()) {
            "${tagPrefix}_$tag"
        } else {
            tag
        }
    }

    /**
     * 获取调用位置（文件名和行号）
     *
     * 返回格式：(FileName.kt:lineNumber)
     * 这种格式在 Android Studio 的 logcat 中可以点击跳转到代码位置
     */
    private fun getLocation(): String {
        val stackTrace = Thread.currentThread().stackTrace

        // 遍历堆栈，找到第一个不是 Logger 类的调用位置
        for (i in 4 until stackTrace.size) {
            val element = stackTrace[i]
            val className = element.className

            // 跳过 Logger 类自身的调用
            if (!className.contains("RLog")) {
                val fileName = element.fileName ?: "Unknown.kt"
                val lineNumber = element.lineNumber
                return "($fileName:$lineNumber)"
            }
        }

        return "(Unknown:0)"
    }

    /**
     * 写入日志到文件
     */
    private fun writeToFile(
        level: String,
        tag: String,
        message: String,
        location: String,
        throwable: Throwable?
    ) {
        try {
            // 检查当前文件是否需要轮转
            val file = currentLogFile
            if (file != null && file.exists() && file.length() > MAX_FILE_SIZE) {
                rotateLogFile()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date())
            var logEntry = "$timestamp | $level | $tag | $message | $location\n"

            // 如果有异常，追加异常堆栈信息
            if (throwable != null) {
                logEntry += "Exception: ${throwable.stackTraceToString()}\n"
            }

            currentLogFile?.appendText(logEntry)
        } catch (e: Exception) {
            // 文件写入失败时，输出到 logcat
            Log.e("Logger", "Failed to write log file", e)
        }
    }

    /**
     * 获取或创建日志文件
     * 文件命名格式：rundemo_2026-01-29.log 或 rundemo_2026-01-29_001.log
     */
    private fun getOrCreateLogFile(): File? {
        val dir = logDir ?: return null
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 先尝试使用当前日期的基础文件名
        var file = File(dir, "$LOG_FILE_PREFIX$dateStr$LOG_FILE_EXTENSION")

        // 如果文件已存在且超过大小限制，创建带序号的新文件
        if (file.exists() && file.length() > MAX_FILE_SIZE) {
            var counter = 1
            do {
                val sequenceStr = String.format("%03d", counter)
                file = File(dir, "${LOG_FILE_PREFIX}${dateStr}_$sequenceStr$LOG_FILE_EXTENSION")
                counter++
            } while (file.exists() && file.length() > MAX_FILE_SIZE)
        }

        // 如果文件不存在，创建新文件
        if (!file.exists()) {
            file.createNewFile()
        }

        return file
    }

    /**
     * 轮转日志文件（当前文件超过大小限制时）
     */
    private fun rotateLogFile() {
        currentLogFile = getOrCreateLogFile()
        cleanupOldLogFiles()
    }

    /**
     * 清理旧的日志文件，保留最新的 MAX_FILE_COUNT 个文件
     */
    private fun cleanupOldLogFiles() {
        val dir = logDir ?: return

        // 获取所有日志文件并按最后修改时间排序（最新的在前）
        val logFiles = dir.listFiles { file ->
            file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: return

        // 删除超过数量限制的旧文件
        if (logFiles.size > MAX_FILE_COUNT) {
            logFiles.drop(MAX_FILE_COUNT).forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.e("Logger", "Failed to delete old log file: ${file.name}", e)
                }
            }
        }
    }

    /**
     * 将 Log 优先级转换为字符串
     */
    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "?"
    }

    /**
     * 获取当前日志文件路径（用于调试或导出）
     */
    fun getLogFilePath(): String? = currentLogFile?.absolutePath

    /**
     * 获取所有日志文件列表（按最新到最旧排序）
     */
    fun getAllLogFiles(): List<File> {
        val dir = logDir ?: return emptyList()
        return dir.listFiles { file ->
            file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 清空所有日志文件
     */
    fun clearAllLogFiles() {
        try {
            getAllLogFiles().forEach { file ->
                file.delete()
            }
            // 重新创建当前日志文件
            currentLogFile = getOrCreateLogFile()
        } catch (e: Exception) {
            Log.e("Logger", "Failed to clear log files", e)
        }
    }

    /**
     * 获取日志目录路径
     */
    fun getLogDirPath(): String? = logDir?.absolutePath
}
