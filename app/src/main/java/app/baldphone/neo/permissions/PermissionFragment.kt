package app.baldphone.neo.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import app.baldphone.neo.utils.AppDialog

import com.bald.uriah.baldphone.R

class PermissionFragment : Fragment() {

    private companion object {
        const val TAG = "PermissionFragment"
        const val SYSTEM_DIALOG_NOT_SHOWN_MS = 500L
        const val KEY_WAITING_FOR_RESULT = "waiting_for_result"
        const val KEY_CURRENT_PERMISSION = "current_permission"
    }

    private enum class UiState { NONE, RATIONALE, RECOVERY }

    private data class ActiveRequest(
        val permission: AppPermission, val startTimeMs: Long? = null
    )

    // Transient state
    private val queue = ArrayDeque<AppPermission>()
    private var current: ActiveRequest? = null
    private var uiState: UiState = UiState.NONE
    private var isWaitingForResult: Boolean = false

    private val callbackMap = mutableMapOf<AppPermission, PermissionManager.PermissionCallback>()
    private var currentCallback: PermissionManager.PermissionCallback? = null
    private var batchFinish: (() -> Unit)? = null

    private var dialog: AppDialog? = null

    private val runtimeLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            Log.d(TAG, "runtimeLauncher: $result")
            isWaitingForResult = false

            val req = current
            if (req == null) {
                // Edge case: Rotation happened between launch and callback
                Log.w(TAG, "runtimeLauncher: current is null (likely rotated)")

                // Try to match result to permission by checking which permissions were requested
                val resultPermissions = result.keys
                val matchingCallback = callbackMap.entries.firstOrNull { (permission, _) ->
                    permission is RuntimePermission && permission.permissions.any {
                        it in resultPermissions
                    }
                }?.value

                if (matchingCallback != null) {
                    Log.d(TAG, "runtimeLauncher: matched callback for rotated request")
                    val denied = result.filterValues { !it }.keys
                    val callbackResult = if (denied.isEmpty()) {
                        PermissionResult.Granted
                    } else {
                        PermissionResult.Denied
                    }
                    matchingCallback.onResult(callbackResult)
                } else {
                    Log.e(TAG, "runtimeLauncher: could not match callback, result lost")
                }
                return@registerForActivityResult
            }

            val denied = result.filterValues { !it }.keys
            if (denied.isEmpty()) {
                finishAndContinue(PermissionResult.Granted)
            } else if (wasSystemDialogNotShown(req.startTimeMs)) {
                // User previously chose "Don't ask again"
                uiState = UiState.RECOVERY
                showRecoveryDialog(req.permission)
            } else {
                finishAndContinue(PermissionResult.Denied)
            }
        }

    private val specialLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "specialLauncher result received")
            isWaitingForResult = false
            val ctx = context ?: return@registerForActivityResult
            val req = current ?: return@registerForActivityResult

            val result = when (val p = req.permission) {
                is SpecialPermission -> if (p.isGranted(ctx)) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.PermanentlyDenied
                }

                is RuntimePermission -> if (p.permissions.all {
                        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
                    }) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.Denied
                }
            }
            finishAndContinue(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore critical state if we were waiting for a result before rotation
        savedInstanceState?.let { bundle ->
            isWaitingForResult = bundle.getBoolean(KEY_WAITING_FOR_RESULT, false)
            val permissionName = bundle.getString(KEY_CURRENT_PERMISSION)

            if (isWaitingForResult && permissionName != null) {
                Log.d(TAG, "onCreate: restored waiting state for $permissionName")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save minimal state to detect if we're waiting for a launcher callback
        if (isWaitingForResult && current != null) {
            Log.d(TAG, "onSaveInstanceState: saving waiting state")
            outState.putBoolean(KEY_WAITING_FOR_RESULT, true)
            outState.putString(KEY_CURRENT_PERMISSION, current?.permission?.toString())
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        dialog?.dismiss()
        dialog = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        val changingConfigs = requireActivity().isChangingConfigurations
        Log.d(TAG, "onDestroy: changingConfig=$changingConfigs")

        // Clear callbacks only if not changing configurations (final destroy)
        if (!changingConfigs) {
            callbackMap.clear()
            currentCallback = null
            batchFinish = null
        }
    }

    /**
     * Process a batch of requests.
     */
    fun processRequests(
        requests: List<PermissionManager.RequestEntry>, onCompletion: () -> Unit
    ) {
        Log.d(
            TAG, "processRequests: current=$current, queue=${queue.size}, requests=${requests.size}"
        )

        // If flow already running, determine if we should reattach or restart
        if (current != null || queue.isNotEmpty()) {
            // Check if this is the same batch (same permissions in same order)
            val currentBatch = buildList {
                current?.permission?.let { add(it) }
                addAll(queue)
            }
            val newBatch = requests.map { it.permission }
            val isSameBatch = currentBatch == newBatch

            if (isSameBatch && isSystemDialogActive()) {
                // Same batch, system dialog active - reattach callbacks
                Log.d(
                    TAG, "processRequests: same batch, system dialog active, reattaching callbacks"
                )

                // Update callback map with new callback instances
                requests.forEach { entry ->
                    callbackMap[entry.permission] = entry.callback
                }

                // Update current callback for the active permission
                current?.permission?.let { activePermission ->
                    currentCallback = callbackMap[activePermission]
                }

                batchFinish = onCompletion
                return
            } else {
                Log.d(TAG, "processRequests: different batch or no active dialog, restarting flow")
                clearState()
            }
        }

        // Starting new batch - setup callback map and queue
        callbackMap.clear()
        requests.forEach { entry ->
            queue += entry.permission
            callbackMap[entry.permission] = entry.callback
        }

        Log.d(TAG, "processRequests: starting new batch, count=${queue.size}")
        batchFinish = onCompletion
        processNextPermission()
    }

    private fun processNextPermission() {
        if (activity?.isFinishing == true) {
            Log.d(TAG, "processNextPermission: activity is finishing, aborting batch")
            queue.clear()
        }

        val permission = queue.removeFirstOrNull()
        Log.d(TAG, "processNextPermission: $permission")
        if (permission == null) {
            batchFinish?.invoke()
            batchFinish = null
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            return
        }

        current = ActiveRequest(permission)
        currentCallback = callbackMap[permission]
        Log.d(TAG, "processNextPermission: currentCallback=${currentCallback?.hashCode()}")

        when (permission) {
            is RuntimePermission -> requestRuntime(permission)
            is SpecialPermission -> requestSpecial(permission)
        }
    }

    private fun requestRuntime(permission: RuntimePermission) {
        Log.d(TAG, "requestRuntime: $permission")
        val ctx = context ?: return

        val denied = permission.permissions.filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            finishAndContinue(PermissionResult.Granted)
            return
        }

        val act = activity ?: return
        val shouldShowRationale = denied.any {
            ActivityCompat.shouldShowRequestPermissionRationale(act, it)
        }
        if (shouldShowRationale) {
            uiState = UiState.RATIONALE
            Log.d(TAG, "requestRuntime: triggering rationale")
            showRationaleDialog(permission)
        } else {
            startRuntime(denied)
        }
    }

    private fun requestSpecial(permission: SpecialPermission) {
        val ctx = context ?: return
        if (permission.isGranted(ctx)) {
            finishAndContinue(PermissionResult.Granted)
            return
        }

        permission.settingsIntent(ctx)?.let {
            isWaitingForResult = true
            specialLauncher.launch(it)
        } ?: run {
            // If we are here, 'isGranted' returned false, but we have no Intent to ask for it.
            Log.e(TAG, "requestSpecial: Failed to get Settings Intent for $permission")
            finishAndContinue(PermissionResult.Error)
        }
    }

    private fun startRuntime(denied: List<String>) {
        Log.d(TAG, "startRuntime: $denied")
        current = current?.copy(startTimeMs = SystemClock.elapsedRealtime())
        isWaitingForResult = true
        runtimeLauncher.launch(denied.toTypedArray())
    }

    private fun finishAndContinue(result: PermissionResult) {
        Log.d(TAG, "finishAndContinue: result=$result, callback=$currentCallback")
        currentCallback?.onResult(result)
        current = null
        uiState = UiState.NONE
        processNextPermission()
    }

    private fun showRationaleDialog(permission: AppPermission) {
        Log.d(TAG, "showRationaleDialog: start")
        showPermissionDialog(
            permission = permission,
            messageRes = permission.messageRes,
            onPositive = {
                uiState = UiState.NONE
                startRuntime((permission as RuntimePermission).permissions.toList())
            })
    }

    private fun showRecoveryDialog(permission: AppPermission) {
        Log.d(TAG, "showRecoveryDialog: start")
        showPermissionDialog(
            permission = permission,
            messageRes = R.string.dialog_message_permission_settings,
            onPositive = {
                uiState = UiState.NONE
                dialog = null
                openSettings()
            })
    }

    /**
     * Helper method to show permission dialogs with common configuration
     */
    private fun showPermissionDialog(
        permission: AppPermission, @StringRes messageRes: Int, onPositive: () -> Unit
    ) {
        if (!isAdded) {
            Log.w(TAG, "showPermissionDialog: not added to fragment manager")
            finishAndContinue(PermissionResult.Error)
            return
        }
        dialog?.dismiss()
        dialog = AppDialog.Builder(requireContext()).setTitle(permission.titleRes)
            .setMessage(messageRes)
            .setPositiveButton(R.string.allow) { onPositive() }
            .setNegativeButton(android.R.string.cancel) {
                finishAndContinue(PermissionResult.Denied)
            }.setCancelable(false).show()
    }

    private fun openSettings() {
        val ctx = context ?: return
        specialLauncher.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)
            )
        )
    }

    // Utilities
    private fun isSystemDialogActive(): Boolean {
        // System dialog is considered active if:
        // 1. We're waiting for a launcher result
        // 2. We have a current request
        // 3. No custom dialog is showing (RATIONALE/RECOVERY)
        return isWaitingForResult && current != null && uiState == UiState.NONE && dialog == null
    }

    private fun wasSystemDialogNotShown(startTimeMs: Long?): Boolean {
        if (startTimeMs == null) {
            // State restored → rotation happened → dialog shown
            return false
        }
        val elapsed = SystemClock.elapsedRealtime() - startTimeMs
        Log.d(
            TAG,
            "wasSystemDialogNotShown: elapsed=$elapsed ms (threshold=$SYSTEM_DIALOG_NOT_SHOWN_MS)"
        )
        return elapsed < SYSTEM_DIALOG_NOT_SHOWN_MS
    }

    private fun clearState() {
        queue.clear()
        current = null
        uiState = UiState.NONE
        isWaitingForResult = false
        callbackMap.clear()
        currentCallback = null
        dialog?.dismiss()
        dialog = null
    }
}
