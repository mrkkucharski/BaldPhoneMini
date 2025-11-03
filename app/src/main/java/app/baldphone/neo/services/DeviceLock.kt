package app.baldphone.neo.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi

enum class DeviceLockResult {
    SUCCESS,
    FAILURE,
    ACCESS_DENIED
}

fun interface DeviceLockCallback {
    fun onResult(result: DeviceLockResult)
}

@RequiresApi(Build.VERSION_CODES.P)
object DeviceLock {

    private const val TAG = "DeviceLock"

    private const val ACTION_LOCK = "device_lock"
    private const val EXTRA_RECEIVER = "receiver"
    private const val TIMEOUT_MS = 1_500L

    @JvmStatic
    fun requestLock(context: Context, callback: DeviceLockCallback) {

        if (!isServiceEnabled(context)) {
            callback.onResult(DeviceLockResult.ACCESS_DENIED)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var completed = false

        val receiver = object : ResultReceiver(handler) {
            override fun onReceiveResult(code: Int, data: Bundle?) {
                if (completed) return
                completed = true
                callback.onResult(
                    if (code == Activity.RESULT_OK)
                        DeviceLockResult.SUCCESS
                    else
                        DeviceLockResult.FAILURE
                )
            }
        }

        // Timeout safety
        handler.postDelayed({
            if (!completed) {
                completed = true
                callback.onResult(DeviceLockResult.FAILURE)
            }
        }, TIMEOUT_MS)

        try {
            context.startService(
                Intent(context, LockService::class.java).apply {
                    action = ACTION_LOCK
                    putExtra(EXTRA_RECEIVER, receiver)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LockService", e)
            if (!completed) {
                callback.onResult(DeviceLockResult.FAILURE)
            }
        }
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)

        val expected = ComponentName(context, LockService::class.java)

        return enabledServices.any { info ->
            ComponentName(
                info.resolveInfo.serviceInfo.packageName,
                info.resolveInfo.serviceInfo.name
            ) == expected
        }
    }

    class LockService : AccessibilityService() {

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            if (intent?.action == ACTION_LOCK) {

                val receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RECEIVER, ResultReceiver::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RECEIVER)
                }

                val success = try {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

                receiver?.send(
                    if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                    null
                )
            }
            return START_NOT_STICKY
        }

        override fun onAccessibilityEvent(event: AccessibilityEvent?) {
            // No-op
        }

        override fun onInterrupt() {
            // No-op
        }
    }
}
