package com.vcam.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcam.R

class TermsActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var cbAgree: CheckBox
    private lateinit var btnAccept: MaterialButton
    private lateinit var btnReject: MaterialButton
    private lateinit var tvTermsContent: TextView

    companion object {
        const val PREFS_TERMS = "vcam_terms"
        const val KEY_TERMS_ACCEPTED = "terms_accepted_v1"

        fun hasAcceptedTerms(context: android.content.Context): Boolean =
            context.getSharedPreferences(PREFS_TERMS, android.content.Context.MODE_PRIVATE)
                .getBoolean(KEY_TERMS_ACCEPTED, false)

        fun setTermsAccepted(context: android.content.Context) {
            context.getSharedPreferences(PREFS_TERMS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        scrollView = findViewById(R.id.scroll_terms)
        cbAgree = findViewById(R.id.cb_agree_terms)
        btnAccept = findViewById(R.id.btn_accept_terms)
        btnReject = findViewById(R.id.btn_reject_terms)
        tvTermsContent = findViewById(R.id.tv_terms_content)

        // Load terms content
        tvTermsContent.text = getTermsText()

        // Setup checkbox
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
            btnAccept.alpha = if (isChecked) 1f else 0.5f
        }

        // Accept button
        btnAccept.setOnClickListener {
            setTermsAccepted(this)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // Reject button
        btnReject.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("رفض الشروط")
                .setMessage("إذا رفضت الشروط، لن تتمكن من استخدام التطبيق.")
                .setPositiveButton("تأكيد الرفض") { _, _ ->
                    finishAffinity()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        // Auto-disable accept button initially
        btnAccept.isEnabled = false
        btnAccept.alpha = 0.5f
    }

    private fun getTermsText(): String {
        return """
╔════════════════════════════════════════════════════════════════╗
║                    إخلاء المسؤولية القانونية                    ║
║              Virtual Cam — للتطوير والاختبار فقط              ║
╚════════════════════════════════════════════════════════════════╝

⚠️ تحذير مهم
────────────────────────────────────────────────────────────────

باستخدامك تطبيق Virtual Cam، فإنك تقرّ وتوافق صراحة على جميع الشروط والسياسات المذكورة أدناه.

📋 الشروط الأساسية
────────────────────────────────────────────────────────────────

1️⃣ طبيعة التطبيق ونطاق الاستخدام

التطبيق مُصمم حصراً للتطوير والاختبار التقني في بيئات محكومة. 
الاستخدام المسموح:
  ✓ اختبار تطبيقات الكاميرا
  ✓ البحث الأمني الأخلاقي
  ✓ تطوير تطبيقات الواقع المعزز
  ✓ الاختبار التقني المشروع

الاستخدام غير المسموح:
  ✗ أي غرض تجاري بدون ترخيص
  ✗ استخدام شخصي خارج التطوير
  ✗ نسخ أو توزيع غير مصرح

أنت المسؤول الحصري عن استخدامك للتطبيق والامتثال لجميع القوانين المحلية والدولية.


2️⃣ الاستخدامات المحظورة صراحةً

يُحظر تحت أي ظرف استخدام التطبيق في:

  ❌ انتحال الهوية أو تزوير الصور أو البيانات
  ❌ الالتفاف حول أنظمة التحقق (KYC / اعرف عميلك)
  ❌ خداع أنظمة التعرف على الوجه أو التحقق البيومتري
  ❌ الاحتيال المالي أو البنكي أو التأميني
  ❌ تزوير الفيديوهات بقصد التضليل
  ❌ انتهاك خصوصية الأشخاص بدون موافقتهم
  ❌ الإضرار بالأفراد أو المؤسسات أو الحكومات
  ❌ أي مخالفة قانونية أو جريمة

أي استخدام لهذه الأغراض يقع على عاتقك أنت وحدك.


3️⃣ المسؤولية والالتزامات

أنت (المستخدم) مسؤول بشكل كامل وحصري عن:

  • التحقق من أن استخدامك يتوافق مع القوانين المحلية والدولية
  • الحصول على أي تراخيص ضرورية قبل الاستخدام
  • أي ضرر أو أذى قد ينجم عن استخدامك للتطبيق
  • التزاماتك القانونية تجاه أطراف ثالثة

مطوّر التطبيق لا يتحمل أي مسؤولية قانونية أو مدنية أو جنائية عن أي استخدام خاطئ أو ضار.


╔════════════════════════════════════════════════════════════════╗
║                      سياسة الخصوصية                           ║
╚════════════════════════════════════════════════════════════════╝

🔐 حماية بياناتك
────────────────────────────────────────────────────────────────

التطبيق لا يجمع أو يخزن أو ينقل أي بيانات شخصية:

  ✓ اسمك أو بريدك الإلكتروني
  ✓ رقم هاتفك أو عنوانك
  ✓ موقعك الجغرافي
  ✓ بيانات القياسات الحيوية (الوجه، البصمات)
  ✓ بيانات مالية أو بطاقات ائتمان
  ✓ جهات الاتصال أو الرسائل
  ✓ تسجيلات الفيديو أو الصور

كل البيانات التقنية تُخزن محلياً على جهازك فقط.


📱 الأذونات المطلوبة
────────────────────────────────────────────────────────────────

التطبيق يطلب فقط الأذونات الضرورية:

  📷 الكاميرا: لتفعيل وظائف الكاميرا الافتراضية
  💾 التخزين: لحفظ الإعدادات محليا على جهازك

لا تُستخدم هذه الأذونات لجمع أو نقل بيانات شخصية.


🚫 عدم المشاركة مع أطراف ثالثة
────────────────────────────────────────────────────────────────

التطبيق لا يبيع أو يتاجر أو يؤجر أو يشارك بياناتك مع أي طرف ثالث.

الاستثناء الوحيد: إذا أصدرت المحكمة أمراً قضائياً رسمياً.


╔════════════════════════════════════════════════════════════════╗
║                   إقرار المستخدم النهائي                       ║
╚════════════════════════════════════════════════════════════════╝

بالموافقة على هذه الشروط والسياسات، فإنك تؤكد:

  ✓ قرأت وفهمت جميع البنود والشروط
  ✓ توافق على استخدام التطبيق للأغراض المشروعة فقط
  ✓ تتحمل المسؤولية الكاملة عن استخدامك
  ✓ ستلتزم بجميع القوانين المعمول بها
  ✓ تفهم أن الاستخدام غير القانوني سيؤدي إلى تتبع قانوني

إذا كنت لا توافق، يجب عليك الامتناع عن استخدام التطبيق فوراً.

════════════════════════════════════════════════════════════════
آخر تحديث: 29 يونيو 2026
════════════════════════════════════════════════════════════════
        """.trimIndent()
    }
}
