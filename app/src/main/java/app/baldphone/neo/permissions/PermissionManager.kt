package app.baldphone.neo.permissions

import android.util.Log
import androidx.fragment.app.FragmentActivity

/**
 * PermissionManager handles permission requests scoped to the specific Activity.
 */
object PermissionManager {

    private const val TAG = "PermissionManager"
    private const val FRAGMENT_TAG = "PermissionManagerFragment"

    fun interface PermissionCallback {
        fun onResult(result: PermissionResult)
    }

    data class RequestEntry(
        val permission: AppPermission,
        val callback: PermissionCallback
    )

    fun request(activity: FragmentActivity, builder: RequestBatch.() -> Unit) {
        RequestBatch(activity).apply(builder).request()
    }

    fun with(activity: FragmentActivity): RequestBatch = RequestBatch(activity)

    // @JvmStatic
    fun requestPermission(
        activity: FragmentActivity,
        permission: AppPermission,
        callback: PermissionCallback? = null
    ) {
        with(activity).add(permission, callback).request()
    }

    fun requestPermission(
        activity: FragmentActivity,
        permission: AppPermission,
        block: PermissionResultHandler.() -> Unit
    ) {
        val handler = PermissionResultHandler().apply(block)
        requestPermission(activity, permission) { result ->
            handler.handle(result)
        }
    }

    /**
     * Check if a permission is already granted without requesting it
     */
    @JvmStatic
    fun isGranted(activity: FragmentActivity, permission: AppPermission): Boolean = 
        permission.isGranted(activity)

    class RequestBatch internal constructor(
        private val activity: FragmentActivity
    ) {
        private val pending = ArrayDeque<RequestEntry>()

        fun add(
            permission: AppPermission,
            callback: PermissionCallback? = null
        ): RequestBatch {
            Log.d(TAG, "add: $permission")
            pending += RequestEntry(
                permission,
                callback ?: PermissionCallback { }
            )
            return this
        }

        fun add(
            permission: AppPermission,
            block: PermissionResultHandler.() -> Unit
        ): RequestBatch {
            val handler = PermissionResultHandler().apply(block)
            return add(permission) { result ->
                handler.handle(result)
            }
        }

        fun request() {
            if (pending.isEmpty()) {
                Log.w(TAG, "request: no pending requests")
                return
            }

            val fm = activity.supportFragmentManager
            val existing = fm.findFragmentByTag(FRAGMENT_TAG) as? PermissionFragment

            val fragment = if (existing != null) {
                Log.d(TAG, "request: reusing existing fragment")
                existing
            } else {
                Log.d(TAG, "request: creating new fragment")
                PermissionFragment().also {
                    fm.beginTransaction()
                        .add(it, FRAGMENT_TAG)
                        .commitNow()
                }
            }

            fragment.processRequests(
                pending.toList()
            ) {
                Log.d(TAG, "request: batch complete, removing fragment")
                if (!fm.isStateSaved) {
                    fm.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    class PermissionResultHandler {
        private var onGranted: (() -> Unit)? = null
        private var onDenied: (() -> Unit)? = null
        private var onPermanentlyDenied: (() -> Unit)? = null
        private var onError: (() -> Unit)? = null
        private var onAny: ((PermissionResult) -> Unit)? = null

        fun onGranted(block: () -> Unit) {
            onGranted = block
        }

        fun onDenied(block: () -> Unit) {
            onDenied = block
        }

        fun onPermanentlyDenied(block: () -> Unit) {
            onPermanentlyDenied = block
        }

        fun onError(block: () -> Unit) {
            onError = block
        }

        fun onAny(block: (PermissionResult) -> Unit) {
            onAny = block
        }

        fun handle(result: PermissionResult) {
            onAny?.invoke(result)
            when (result) {
                PermissionResult.Granted -> onGranted?.invoke()
                PermissionResult.Denied -> onDenied?.invoke()
                PermissionResult.PermanentlyDenied -> onPermanentlyDenied?.invoke()
                PermissionResult.Error -> onError?.invoke()
            }
        }
    }
}
