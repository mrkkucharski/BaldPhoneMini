package app.baldphone.neo.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.utils.PhoneNumberUtils

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.utils.BDB
import com.bald.uriah.baldphone.utils.BDialog
import com.bald.uriah.baldphone.utils.BaldToast

import java.util.Collections

object CallManager {
    private const val TAG = "CallManager"

    /** Helper function for [com.bald.uriah.baldphone.activities.SOSActivity] and similar,
     * which bypasses all dialogs. */
    fun callDirectly(context: Context, number: CharSequence) {
        call(context, number, directly = true)
    }

    fun call(context: Context, number: CharSequence, directly: Boolean = false) {

        if (!directly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && Prefs.isDualSimActive) {
            val sm =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val list = sm.activeSubscriptionInfoList?.let {
                    Collections.unmodifiableList(it)
                } ?: emptyList()

                if (list.size > 1) {
                    val names = list.map { it.displayName ?: "" }.toTypedArray()
                    BDB.from(context).addFlag(BDialog.FLAG_OK or BDialog.FLAG_CANCEL)
                        .setTitle(R.string.choose_sim).setSubText(R.string.choose_sim_subtext)
                        .setOptions(*names).setPositiveButtonListener {
                            startCall(context, number, list[it[0] as Int])
                            true
                        }.show()
                    return
                }
            }
        }

        startCall(context, number, null)
    }

    /**
     * Starts a phone call for the given number.
     *
     * For an emergency numbers it uses [Intent.ACTION_DIAL] as third-party applications are
     * generally prohibited from calling emergency numbers directly via [Intent.ACTION_CALL].
     */
    private fun startCall(
        context: Context, number: CharSequence, subscriptionInfo: SubscriptionInfo? = null
    ) {
        val uri = Uri.fromParts("tel", number.toString(), null)

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val isEmergency = PhoneNumberUtils.isEmergency(context, number.toString())
        Log.v(TAG, "isEmergency: $isEmergency")

        val intent = if (hasCallPermission && !isEmergency) {
            Intent(Intent.ACTION_CALL).setData(uri)
        } else {
            Intent(Intent.ACTION_DIAL).setData(uri)
        }

        if (hasCallPermission && subscriptionInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

            telecomManager.callCapablePhoneAccounts.firstOrNull { it.id.contains(subscriptionInfo.iccId) }
                ?.let {
                    intent.putExtra(
                        TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it
                    )
                }
        }

        runCatching {
            context.startActivity(intent)
        }.recoverCatching {
            context.startActivity(
                Intent(Intent.ACTION_DIAL).setData(uri)
            )
        }.onFailure {
            BaldToast.error(context, "Could not find an app to place call")
        }
    }
}
