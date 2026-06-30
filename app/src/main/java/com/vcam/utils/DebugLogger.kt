package com.vcam.utils

import android.util.Log
import com.vcam.ui.DebugConsoleActivity

object DebugLogger {
    private const val TAG = "VCam"

    fun log(message: String) {
        Log.d(TAG, message)
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog(message)
        }
    }

    fun error(message: String, e: Exception? = null) {
        val fullMsg = if (e != null) "$message: ${e.message}" else message
        Log.e(TAG, fullMsg, e)
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog("❌ خطأ: $fullMsg")
        }
    }

    fun success(message: String) {
        Log.i(TAG, message)
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog("✅ $message")
        }
    }

    fun warning(message: String) {
        Log.w(TAG, message)
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog("⚠️  $message")
        }
    }

    fun section(title: String) {
        Log.i(TAG, title)
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog("┌─ $title")
        }
    }

    fun endSection() {
        DebugConsoleActivity.getInstance()?.let {
            DebugConsoleActivity.addLog("└─")
        }
    }
}
