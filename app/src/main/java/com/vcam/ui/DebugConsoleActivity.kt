package com.vcam.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.vcam.R
import java.text.SimpleDateFormat
import java.util.*

class DebugConsoleActivity : AppCompatActivity() {

    companion object {
        private var instance: DebugConsoleActivity? = null
        private val logs = mutableListOf<String>()
        private const val MAX_LOGS = 500

        fun getInstance(): DebugConsoleActivity? = instance

        fun addLog(message: String) {
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] $message"
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) logs.removeAt(0)
            instance?.updateUI()
        }

        fun clearLogs() {
            logs.clear()
        }

        fun getLogs(): String = logs.joinToString("\n")
    }

    private lateinit var tvConsole: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnClear: MaterialButton
    private lateinit var btnClose: MaterialButton
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_console)
        instance = this

        tvConsole = findViewById(R.id.tv_console)
        scrollView = findViewById(R.id.scroll_console)
        btnClear = findViewById(R.id.btn_clear_logs)
        btnClose = findViewById(R.id.btn_close_console)
        btnCopy = findViewById(R.id.btn_copy_logs)

        updateUI()

        btnClear.setOnClickListener {
            clearLogs()
            updateUI()
        }

        btnClose.setOnClickListener { finish() }

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("VCam Logs", getLogs())
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(this, "تم النسخ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private fun updateUI() {
        tvConsole.text = getLogs()
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
