package app.baldphone.neo.calls

import android.Manifest.permission.READ_CALL_LOG
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils

import androidx.core.content.ContextCompat.checkSelfPermission

import app.baldphone.neo.contacts.Contact
import app.baldphone.neo.contacts.SimpleContact

import com.bald.uriah.baldphone.databases.calls.Call

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallsRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    /**
     * Returns up to [maxCount] contacts ordered by call frequency (incoming + outgoing calls),
     * using the median call count as the qualification threshold — only contacts called more
     * than the median number of times are returned. Returns empty if fewer than 2 contacts
     * qualify (caller handles the empty-section case).
     */
    suspend fun getFrequentContacts(
        allContacts: List<SimpleContact>,
        maxCount: Int = 8
    ): List<SimpleContact> = withContext(Dispatchers.IO) {
        if (checkSelfPermission(context, READ_CALL_LOG) != PERMISSION_GRANTED) return@withContext emptyList()
        if (allContacts.isEmpty()) return@withContext emptyList()

        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE)
        val callCounts = mutableMapOf<String, Int>()

        resolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null, null
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            while (cursor.moveToNext()) {
                val type = cursor.getInt(typeIdx)
                if (type != CallLog.Calls.INCOMING_TYPE && type != CallLog.Calls.OUTGOING_TYPE) continue
                val normalized = PhoneNumberUtils.normalizeNumber(cursor.getString(numberIdx) ?: continue)
                    ?.takeIf { it.isNotEmpty() } ?: continue
                callCounts[normalized] = (callCounts[normalized] ?: 0) + 1
            }
        }

        if (callCounts.isEmpty()) return@withContext emptyList()

        val scored = allContacts.mapNotNull { contact ->
            val normalized = PhoneNumberUtils.normalizeNumber(contact.phoneNumber)
                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val count = callCounts[normalized] ?: 0
            if (count > 0) contact to count else null
        }

        if (scored.isEmpty()) return@withContext emptyList()

        val median = scored.map { it.second }.sorted().let { counts ->
            counts[(counts.size - 1) / 2]
        }

        scored
            .filter { it.second > median }
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
    }

    /**
     * Retrieves the call history for a specific contact.
     */
    suspend fun getCallHistory(contact: Contact): List<Call> = withContext(Dispatchers.IO) {
        if (checkSelfPermission(context, READ_CALL_LOG) != PERMISSION_GRANTED) {
            return@withContext emptyList()
        }

        val contactUri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
            ?: return@withContext emptyList()

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.CACHED_LOOKUP_URI
        )

        val calls = mutableListOf<Call>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "${CallLog.Calls.CACHED_LOOKUP_URI} = ?",
            arrayOf(contactUri.toString()),
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calls.add(Call(cursor))
            }
        }
        calls
    }
}
