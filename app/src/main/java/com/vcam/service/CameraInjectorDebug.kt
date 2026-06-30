package com.vcam.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.Log
import com.vcam.utils.DebugLogger
import com.vcam.utils.CameraInjector
import com.vcam.utils.MediaSlotManager
import com.vcam.utils.RootManager
import com.vcam.utils.VcplaxEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Enhanced CameraInjector with detailed debug logging
 */
class CameraInjectorDebug(
    private val context: Context,
    private val mediaPath: String,
    private val isVideo: Boolean,
    private val targetPackage: String?,
    var rotation: Int = 0,
    var mirror: Boolean = false
) {
    companion object {
        private const val TAG = "CameraInjector"
        private const val VCAM_DIR = "/data/local/tmp/vcam"
        private const val INJECT_LIB = "/data/local/tmp/libvcam_inject.so"
        private const val FRAME_FILE = "$VCAM_DIR/frame.yuyv"
        private const val META_FILE = "$VCAM_DIR/frame_info"

        const val TARGET_W = 1280
        const val TARGET_H = 720

        init {
            try {
                System.loadLibrary("vcam_native")
                DebugLogger.success("تم تحميل مكتبة vcam_native")
            } catch (e: UnsatisfiedLinkError) {
                DebugLogger.warning("فشل تحميل vcam_native: ${e.message}")
            }
        }

        @JvmStatic external fun nativeStartFrameLoop(width: Int, height: Int, videoDevice: String): Boolean
        @JvmStatic external fun nativeUpdateYUYVFrame(yuyvData: ByteArray, width: Int, height: Int)
        @JvmStatic external fun nativeStopInjection()
        @JvmStatic external fun nativeCheckDevice(videoDevice: String): Boolean
        @JvmStatic external fun nativeInjectImage(imagePath: String, videoDevice: String): Int
        @JvmStatic external fun nativeInjectVideo(videoPath: String, videoDevice: String): Int
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    @Volatile private var running = false
    private var injectionJob: Job? = null
    @Volatile private var usingVcplax = false

    fun start() {
        DebugLogger.section("بدء الـ Injection")
        running = true
        injectionJob = scope.launch { performInjection() }
    }

    fun stop() {
        DebugLogger.section("إيقاف الـ Injection")
        running = false
        injectionJob?.cancel()

        if (usingVcplax) {
            DebugLogger.log("إيقاف VcplaxEngine...")
            VcplaxEngine.stopInjection()
            usingVcplax = false
            DebugLogger.success("تم إيقاف VcplaxEngine")
        } else {
            DebugLogger.log("تنظيف الـ Wrap Properties...")
            cleanupAllWrapProps()
            try {
                nativeStopInjection()
                DebugLogger.success("تم إيقاف الـ Native Injection")
            } catch (_: Exception) {}
            RootManager.runCommands(
                "pkill -f ffmpeg 2>/dev/null || true",
                "pkill -f v4l2  2>/dev/null || true"
            )
            RootManager.runCommand("setprop ctl.restart cameraserver")
            DebugLogger.success("تم إعادة تشغيل cameraserver")
        }
        DebugLogger.endSection()
    }

    private suspend fun performInjection() {
        DebugLogger.log("الملف: $mediaPath")
        DebugLogger.log("النوع: ${if (isVideo) "فيديو" else "صورة"}")
        DebugLogger.log("الجهاز المستهدف: ${targetPackage ?: "جميع التطبيقات"}")

        try {
            DebugLogger.section("محاولة VcplaxEngine (الطريقة الأولى)")
            val engineReady = VcplaxEngine.setup(context)
            DebugLogger.log("إعداد VcplaxEngine: ${if (engineReady) "نجح" else "فشل"}")
            
            if (engineReady) {
                DebugLogger.log("بدء الـ Injection عبر VcplaxEngine...")
                val started = VcplaxEngine.startInjection(mediaPath, loop = isVideo)
                
                if (started) {
                    usingVcplax = true
                    DebugLogger.success("VcplaxEngine بدأ بنجاح! ✓")

                    if (rotation != 0) {
                        DebugLogger.log("تطبيق التدوير: ${rotation}°")
                        VcplaxEngine.setRotation(rotation)
                    }
                    if (mirror) {
                        DebugLogger.log("تطبيق العكس: مفعّل")
                        VcplaxEngine.setMirror(true)
                    }

                    DebugLogger.endSection()
                    DebugLogger.section("مراقبة الـ Injection")
                    while (running) {
                        delay(500)
                        if (!VcplaxEngine.isRunning) {
                            DebugLogger.warning("VcplaxEngine توقف بشكل غير متوقع")
                            DebugLogger.log("محاولة إعادة التشغيل...")
                            VcplaxEngine.startInjection(mediaPath, loop = isVideo)
                        }
                    }
                    DebugLogger.endSection()
                    return
                } else {
                    DebugLogger.error("فشل بدء VcplaxEngine")
                }
            }
            DebugLogger.endSection()
        } catch (e: Exception) {
            DebugLogger.error("خطأ في VcplaxEngine", e)
        }

        DebugLogger.section("استخدام Injection القديم (LD_PRELOAD)")
        legacyInject()
        DebugLogger.endSection()
    }

    private suspend fun legacyInject() {
        try {
            DebugLogger.log("إعداد مكتبة الـ Injection...")
            setupInjectLib()
            DebugLogger.success("تم إعداد مكتبة الـ Injection")

            DebugLogger.log("تحميل وحدة v4l2loopback...")
            tryLoadV4L2Module()

            DebugLogger.log("البحث عن أجهزة الفيديو...")
            val devices = RootManager.getVideoDevices()
            DebugLogger.log("وجدت ${devices.size} جهاز فيديو")

            if (devices.isNotEmpty()) {
                devices.forEach { DebugLogger.log("  - $it") }
                val device = devices.last()
                DebugLogger.log("استخدام الجهاز: $device")
                
                DebugLogger.log("إعداد LD_PRELOAD...")
                setupLdPreload()
                DebugLogger.success("تم إعداد LD_PRELOAD")

                val started = tryStartV4L2(device)
                if (started) {
                    DebugLogger.success("بدء البث على v4l2")
                    streamFramesToV4L2(device)
                    return
                } else {
                    DebugLogger.warning("فشل بدء v4l2")
                }
            } else {
                DebugLogger.warning("لم يتم العثور على أجهزة v4l2")
            }

            DebugLogger.log("البث المباشر للملفات المشتركة...")
            streamFramesToSharedFile()
        } catch (e: Exception) {
            DebugLogger.error("خطأ في الـ Legacy Injection", e)
        }
    }

    private fun setupInjectLib() {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val srcLib = File(nativeDir, "libvcam_inject.so")
        DebugLogger.log("البحث عن مكتبة: ${srcLib.absolutePath}")

        if (srcLib.exists()) {
            DebugLogger.log("وجدت المكتبة، جاري النسخ...")
            RootManager.runCommands("mkdir -p $VCAM_DIR", "chmod 777 $VCAM_DIR")
            RootManager.runCommands(
                "cp '${srcLib.absolutePath}' $INJECT_LIB",
                "chmod 755 $INJECT_LIB"
            )
            DebugLogger.success("تم نسخ المكتبة")
        } else {
            DebugLogger.warning("لم يتم العثور على libvcam_inject.so")
        }
    }

    private fun setupLdPreload() {
        RootManager.runCommand("setenforce 0 2>/dev/null || true")
        
        val props = listOf(
            "wrap.cameraserver",
            "wrap.android.hardware.camera.provider@2.4-service",
            "wrap.android.hardware.camera.provider@2.5-service",
            "wrap.android.hardware.camera.provider@2.6-service"
        )
        
        props.forEach { prop ->
            DebugLogger.log("تعيين $prop")
            RootManager.runCommand("setprop '$prop' 'LD_PRELOAD=$INJECT_LIB'")
        }

        RootManager.runCommands(
            "setprop ctl.restart cameraserver",
            "sleep 2"
        )
        DebugLogger.success("تم تطبيق LD_PRELOAD وإعادة تشغيل cameraserver")
    }

    private fun cleanupAllWrapProps() {
        listOf(
            "wrap.cameraserver",
            "wrap.android.hardware.camera.provider@2.4-service",
            "wrap.android.hardware.camera.provider@2.5-service",
            "wrap.android.hardware.camera.provider@2.6-service"
        ).forEach { prop ->
            RootManager.runCommand("setprop '$prop' '' 2>/dev/null || true")
        }
    }

    private fun tryLoadV4L2Module() {
        RootManager.runCommands(
            "modprobe v4l2loopback devices=1 2>/dev/null || true",
            "insmod /vendor/lib/modules/v4l2loopback.ko 2>/dev/null || true"
        )
        DebugLogger.log("تم محاولة تحميل v4l2loopback")
    }

    private fun tryStartV4L2(device: String): Boolean {
        return try {
            nativeStartFrameLoop(TARGET_W, TARGET_H, device)
        } catch (e: Exception) {
            DebugLogger.error("خطأ في بدء v4l2", e)
            false
        }
    }

    private suspend fun streamFramesToV4L2(device: String) {
        if (isVideo) streamVideo(pushToV4L2 = true) else streamImage(pushToV4L2 = true)
    }

    private suspend fun streamFramesToSharedFile() {
        if (isVideo) streamVideo(pushToV4L2 = false) else streamImage(pushToV4L2 = false)
    }

    private suspend fun streamImage(pushToV4L2: Boolean) = withContext(Dispatchers.IO) {
        DebugLogger.log("تحميل الصورة...")
        val bitmap = loadAndTransformBitmap(mediaPath) ?: run {
            DebugLogger.error("فشل تحميل الصورة: $mediaPath")
            return@withContext
        }
        DebugLogger.success("تم تحميل الصورة بنجاح (${bitmap.width}x${bitmap.height})")
        
        DebugLogger.log("تحويل إلى YUYV...")
        val yuyv = bitmapToYUYV(bitmap, TARGET_W, TARGET_H)
        bitmap.recycle()
        nativeUpdateYUYVFrame(yuyv, TARGET_W, TARGET_H)
        DebugLogger.success("تم إرسال الإطار إلى الكاميرا")
        
        while (running) delay(500)
    }

    private suspend fun streamVideo(pushToV4L2: Boolean) = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(mediaPath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 5000L
            DebugLogger.log("مدة الفيديو: ${durationMs}ms")

            var posMs = 0L
            val frameIntervalMs = 33L
            var frameCount = 0

            while (running) {
                val frameBitmap = retriever.getFrameAtTime(
                    posMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (frameBitmap != null) {
                    val transformed = applyTransforms(frameBitmap)
                    val yuyv = bitmapToYUYV(transformed, TARGET_W, TARGET_H)
                    if (transformed !== frameBitmap) transformed.recycle()
                    frameBitmap.recycle()
                    nativeUpdateYUYVFrame(yuyv, TARGET_W, TARGET_H)
                    frameCount++
                }
                posMs += frameIntervalMs
                if (posMs >= durationMs) posMs = 0L
                delay(frameIntervalMs)
            }
            DebugLogger.success("تم بث $frameCount إطار")
        } finally {
            retriever.release()
        }
    }

    private fun loadAndTransformBitmap(path: String): Bitmap? {
        val raw = try { BitmapFactory.decodeFile(path) ?: return null }
                  catch (e: Exception) { return null }
        return applyTransforms(raw)
    }

    private fun applyTransforms(src: Bitmap): Bitmap {
        if (rotation == 0 && !mirror) return src
        val matrix = Matrix()
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        if (mirror) matrix.postScale(-1f, 1f, src.width / 2f, src.height / 2f)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun bitmapToYUYV(src: Bitmap, outW: Int, outH: Int): ByteArray {
        val bmp = if (src.width != outW || src.height != outH)
                      Bitmap.createScaledBitmap(src, outW, outH, true) else src
        val pixels = IntArray(outW * outH)
        bmp.getPixels(pixels, 0, outW, 0, 0, outW, outH)
        if (bmp !== src) bmp.recycle()

        val yuyv = ByteArray(outW * outH * 2)
        var idx = 0; var pi = 0
        while (pi < pixels.size - 1) {
            val p0 = pixels[pi]; val p1 = pixels[pi + 1]
            val r0 = (p0 shr 16) and 0xff; val g0 = (p0 shr 8) and 0xff; val b0 = p0 and 0xff
            val r1 = (p1 shr 16) and 0xff; val g1 = (p1 shr 8) and 0xff; val b1 = p1 and 0xff
            val y0 = ((66*r0+129*g0+25*b0+128) shr 8)+16
            val y1 = ((66*r1+129*g1+25*b1+128) shr 8)+16
            val u  = ((-38*r0-74*g0+112*b0+128) shr 8)+128
            val v  = ((112*r0-94*g0-18*b0+128) shr 8)+128
            yuyv[idx++] = y0.coerceIn(16,235).toByte()
            yuyv[idx++] = u.coerceIn(16,240).toByte()
            yuyv[idx++] = y1.coerceIn(16,235).toByte()
            yuyv[idx++] = v.coerceIn(16,240).toByte()
            pi += 2
        }
        return yuyv
    }
}
