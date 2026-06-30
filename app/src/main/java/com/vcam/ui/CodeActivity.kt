package com.vcam.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.vcam.R
import com.vcam.utils.LicenseChecker
import kotlinx.coroutines.launch

class CodeActivity : AppCompatActivity() {

    private lateinit var etCode: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code)

        etCode = findViewById(R.id.et_code)
        btnSubmit = findViewById(R.id.btn_submit)
        tvStatus = findViewById(R.id.tv_status)
        pbLoading = findViewById(R.id.progress_bar)

        btnSubmit.setOnClickListener { handleVerify() }
    }

    private fun handleVerify() {
        val code = etCode.text.toString().trim()
        if (code.isEmpty()) {
            showError("أدخل الكود")
            return
        }

        btnSubmit.isEnabled = false
        pbLoading.visibility = android.view.View.VISIBLE
        tvStatus.text = "جاري التحقق..."
        tvStatus.setTextColor(0xFF4F8EF7.toInt())

        lifecycleScope.launch {
            val result = LicenseChecker.verifyCode(code)
            pbLoading.visibility = android.view.View.GONE

            when (result) {
                LicenseChecker.VerifyResult.VALID -> {
                    LicenseChecker.saveCode(this@CodeActivity, code)
                    tvStatus.text = "✓ تم التحقق بنجاح"
                    tvStatus.setTextColor(0xFF22C55E.toInt())
                    
                    // Navigate to Terms & Privacy
                    Thread.sleep(500)
                    startActivity(Intent(this@CodeActivity, TermsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                LicenseChecker.VerifyResult.INVALID -> {
                    showError("كود غير صحيح")
                    btnSubmit.isEnabled = true
                }
                LicenseChecker.VerifyResult.REVOKED -> {
                    showError("وصولك تم إلغاؤه")
                    btnSubmit.isEnabled = true
                }
                LicenseChecker.VerifyResult.SERVER_EMPTY -> {
                    showError("خطأ في الاتصال — جرب لاحقاً")
                    btnSubmit.isEnabled = true
                }
                else -> {
                    showError("خطأ غير متوقع")
                    btnSubmit.isEnabled = true
                }
            }
        }
    }

    private fun showError(msg: String) {
        tvStatus.text = msg
        tvStatus.setTextColor(0xFFEF4444.toInt())
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }
}
