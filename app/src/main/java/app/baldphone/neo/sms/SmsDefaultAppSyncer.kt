package app.baldphone.neo.sms

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony

import com.bald.uriah.baldphone.utils.BPrefs

object SmsDefaultAppSyncer {

    private val SMS_COMPONENTS = listOf(
        "app.baldphone.neo.sms.SmsReceiver",
        "app.baldphone.neo.sms.WapPushReceiver",
        "app.baldphone.neo.sms.RespondViaMessageService",
    )

    private const val ROLE_REQUEST_CODE = 0x534d5300 // "SMS\0"

    enum class DefaultSmsRequestAction {
        REQUEST_ROLE,
        REQUEST_LEGACY_CHANGE_DEFAULT,
        NONE
    }

    fun sync(activity: Activity) {
        val isNativePanel = BPrefs.get(activity)
            .getString(BPrefs.CUSTOM_MESSAGES_KEY, null) == null
        val isCurrentDefault = Telephony.Sms.getDefaultSmsPackage(activity) == activity.packageName

        when {
            isNativePanel && !isCurrentDefault -> {
                enableSmsComponents(activity)
                requestSmsRole(activity)
            }
            !isNativePanel && isCurrentDefault -> {
                disableSmsComponents(activity)
                // Android revokes the SMS role automatically once required components are disabled
            }
        }
    }

    private fun requestSmsRole(activity: Activity) {
        val isAtLeastAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val roleManager = if (isAtLeastAndroidQ) {
            activity.getSystemService(RoleManager::class.java)
        } else {
            null
        }

        when (getDefaultSmsRequestAction(
            isAtLeastAndroidQ = isAtLeastAndroidQ,
            isRoleManagerAvailable = roleManager != null,
            isSmsRoleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true,
            isSmsRoleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        )) {
            DefaultSmsRequestAction.REQUEST_ROLE -> {
                activity.startActivityForResult(
                    roleManager!!.createRequestRoleIntent(RoleManager.ROLE_SMS),
                    ROLE_REQUEST_CODE
                )
            }
            DefaultSmsRequestAction.REQUEST_LEGACY_CHANGE_DEFAULT -> {
                activity.startActivityForResult(
                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
                    },
                    ROLE_REQUEST_CODE
                )
            }
            DefaultSmsRequestAction.NONE -> Unit
        }
    }

    fun getDefaultSmsRequestAction(
        isAtLeastAndroidQ: Boolean,
        isRoleManagerAvailable: Boolean,
        isSmsRoleAvailable: Boolean,
        isSmsRoleHeld: Boolean
    ): DefaultSmsRequestAction {
        if (!isAtLeastAndroidQ) return DefaultSmsRequestAction.REQUEST_LEGACY_CHANGE_DEFAULT
        if (!isRoleManagerAvailable || !isSmsRoleAvailable || isSmsRoleHeld) {
            return DefaultSmsRequestAction.NONE
        }
        return DefaultSmsRequestAction.REQUEST_ROLE
    }

    private fun enableSmsComponents(activity: Activity) {
        setSmsComponentsState(activity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    private fun disableSmsComponents(activity: Activity) {
        setSmsComponentsState(activity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    private fun setSmsComponentsState(activity: Activity, state: Int) {
        val pm = activity.packageManager
        SMS_COMPONENTS.forEach { className ->
            runCatching {
                pm.setComponentEnabledSetting(
                    ComponentName(activity, Class.forName(className)),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
