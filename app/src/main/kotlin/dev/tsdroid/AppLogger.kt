package dev.tsdroid

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val TAG = "AppLogger"
    private const val MAX_LOG_BYTES = 512 * 1024 // 512 KB before rotation

    private var logDir: File? = null
    private var appLogFile: File? = null
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    fun install(app: Application) {
        val dir = File(app.filesDir, "logs")
        dir.mkdirs()
        logDir = dir
        appLogFile = File(dir, "app.log")
        Log.i(TAG, "AppLogger installed at ${dir.absolutePath}")

        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrash(throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    // --- Runtime logging ---

    @JvmStatic fun d(tag: String, msg: String) { log("D", tag, msg) }
    @JvmStatic fun i(tag: String, msg: String) { log("I", tag, msg) }
    @JvmStatic fun w(tag: String, msg: String, tr: Throwable? = null) {
        log("W", tag, msg)
        tr?.let { logStackTrace("W", tag, it) }
    }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) {
        log("E", tag, msg)
        tr?.let { logStackTrace("E", tag, it) }
    }

    private fun log(level: String, tag: String, msg: String) {
        try {
            val file = appLogFile ?: return
            val ts = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val line = "$ts $level/$tag: $msg\n"
            // Rotate if too large
            if (file.length() > MAX_LOG_BYTES) {
                file.renameTo(File(file.parentFile, "app.1.log"))
            }
            file.appendText(line)
        } catch (_: Exception) {}
    }

    private fun logStackTrace(level: String, tag: String, tr: Throwable) {
        val sw = StringWriter()
        tr.printStackTrace(PrintWriter(sw))
        log(level, tag, sw.toString())
    }

    // --- Crash capture ---

    private fun writeCrash(throwable: Throwable) {
        try {
            val dir = logDir ?: return
            val crashDir = File(dir, "crashes")
            crashDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val file = File(crashDir, "crash_$timestamp.txt")

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))

            file.writeText("""
                Time: $timestamp
                Exception: ${throwable.javaClass.name}
                Message: ${throwable.message ?: "(none)"}

                Stacktrace:
                ${sw}

                Last app logs:
                ${appLogFile?.readText()?.takeLast(8192) ?: "(none)"}
            """.trimIndent())
        } catch (_: Exception) {}
    }

    // --- File access for viewer ---

    fun getCrashFiles(): List<File> {
        val crashDir = logDir?.let { File(it, "crashes") } ?: return emptyList()
        return crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getRuntimeLogFile(): File? = appLogFile?.takeIf { it.length() > 0 }
}
