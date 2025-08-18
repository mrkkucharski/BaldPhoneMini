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

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import app.baldphone.neo.data.Prefs

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.databases.contacts.Contact
import com.bald.uriah.baldphone.databases.contacts.MiniContact
import com.bald.uriah.baldphone.utils.BDB
import com.bald.uriah.baldphone.utils.BDialog
import com.bald.uriah.baldphone.utils.BaldToast

import java.util.Collections

object CallManager {

    fun call(context: Context, miniContact: MiniContact) {
        runCatching {
            Contact.fromLookupKey(
                miniContact.lookupKey, context.contentResolver
            )!!.phoneList.first().toString()
        }.onSuccess {
            call(context, it)
        }.onFailure {
            BaldToast.error(context, "Could not find a phone number for this contact")
        }
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

    fun callDirectly(context: Context, number: CharSequence) {
        startCall(context, number, null)
    }

    private fun startCall(
        context: Context, number: CharSequence, subscriptionInfo: SubscriptionInfo? = null
    ) {
        val uri = Uri.fromParts("tel", number.toString(), null)

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
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
