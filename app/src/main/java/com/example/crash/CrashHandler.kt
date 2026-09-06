package com.example.crash

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught exception handler that persists crash reports to app-private storage.
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler? = null,
    private val exitOnCrash: Boolean = true
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"

        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    CrashHandler(context.applicationContext, currentHandler)
                )
                Log.d(TAG, "Global CrashHandler installed successfully")
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashReport = generateCrashReport(thread, throwable)
            Log.e(TAG, "Uncaught Exception detected:\n$crashReport", throwable)
            CrashLogManager.saveCrashLog(context, crashReport)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash report to file", e)
        } finally {
            // Forward to default system handler or kill process
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else if (exitOnCrash) {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }

    internal fun generateCrashReport(thread: Thread, throwable: Throwable): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timestamp = dateFormat.format(Date())

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTraceString = sw.toString()

        val rootCause = getRootCause(throwable)

        return buildString {
            appendLine("==================================================")
            appendLine("              SWAR MUSIC CRASH REPORT             ")
            appendLine("==================================================")
            appendLine("Timestamp: $timestamp")
            appendLine("Package: ${context.packageName}")
            appendLine("Thread: ${thread.name} (id: ${thread.id})")
            appendLine()
            appendLine("DEVICE INFORMATION:")
            appendLine("  Brand / Manufacturer: ${Build.BRAND} / ${Build.MANUFACTURER}")
            appendLine("  Model / Device: ${Build.MODEL} (${Build.DEVICE})")
            appendLine("  Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("  Hardware / ABI: ${Build.HARDWARE} / ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine()
            appendLine("EXCEPTION DETAILS:")
            appendLine("  Type: ${throwable.javaClass.name}")
            appendLine("  Message: ${throwable.localizedMessage ?: throwable.message ?: "No message provided"}")
            appendLine()
            if (rootCause !== throwable) {
                appendLine("ROOT CAUSE:")
                appendLine("  Type: ${rootCause.javaClass.name}")
                appendLine("  Message: ${rootCause.localizedMessage ?: rootCause.message ?: "No message"}")
                appendLine()
            }
            appendLine("FULL STACK TRACE:")
            appendLine(stackTraceString)
            appendLine("==================================================")
        }
    }

    private fun getRootCause(throwable: Throwable): Throwable {
        var cause: Throwable? = throwable.cause
        var root = throwable
        while (cause != null && cause !== root) {
            root = cause
            cause = cause.cause
        }
        return root
    }
}

/**
 * Utility for reading, writing, and clearing crash log files.
 */
object CrashLogManager {
    private const val FILE_NAME = "crash_log.txt"

    fun getCrashLogFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun saveCrashLog(context: Context, logContent: String) {
        try {
            val file = getCrashLogFile(context)
            file.parentFile?.mkdirs()
            file.writeText(logContent, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("CrashLogManager", "Failed to save crash log", e)
        }
    }

    fun getCrashLog(context: Context): String? {
        return try {
            val file = getCrashLogFile(context)
            if (file.exists() && file.length() > 0) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CrashLogManager", "Failed to read crash log", e)
            null
        }
    }

    fun clearCrashLog(context: Context): Boolean {
        return try {
            val file = getCrashLogFile(context)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("CrashLogManager", "Failed to clear crash log", e)
            false
        }
    }
}
