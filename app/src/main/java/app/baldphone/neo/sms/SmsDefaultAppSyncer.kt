package app.baldphone.neo.sms

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                activity.startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS),
                    ROLE_REQUEST_CODE
                )
            }
        }
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
