package app.baldphone.neo.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock

import com.bald.uriah.baldphone.BuildConfig

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import java.util.Locale

fun Context.getDeviceInfoFull(): String {
    val dm = resources.displayMetrics
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

    val maxMemInKb = Runtime.getRuntime().maxMemory() / 1024
    val freeMemInKb = Runtime.getRuntime().freeMemory() / 1024
    val usedMemInKb = (Runtime.getRuntime().totalMemory() / 1024) - freeMemInKb

    return """
        Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        Manufacturer: ${Build.MANUFACTURER}
        Brand: ${Build.BRAND}
        Device: ${Build.DEVICE}
        Model: ${Build.MODEL}
        Product: ${Build.PRODUCT}
        Hardware: ${Build.HARDWARE}
        Board: ${Build.BOARD}
        Type: ${Build.TYPE}
        Tags: ${Build.TAGS}
        Screen: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi} dpi)
        Locale: ${Locale.getDefault()}
        Timezone: ${gmtOffsetString()}
        App Heap Max: $maxMemInKb kB
        App Heap Used: $usedMemInKb kB
        App Heap Free: $freeMemInKb kB
        Memory Class: ${am.memoryClass} MB
        Large Mem Class: ${am.largeMemoryClass} MB
        Low Memory: ${memInfo.lowMemory}
        Threads: ${Thread.activeCount()}
        Uptime: ${SystemClock.uptimeMillis()} ms
    """.trimIndent()
}

private fun gmtOffsetString(): String {
    val zoneId = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("OOOO", Locale.getDefault())
    return formatter.format(zoneId.rules.getOffset(java.time.Instant.now()))
}
