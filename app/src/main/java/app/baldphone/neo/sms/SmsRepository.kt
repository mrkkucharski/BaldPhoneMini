package app.baldphone.neo.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager

import androidx.core.content.ContextCompat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SmsRepository(private val context: Context) {

    /** Returns one entry per thread, sorted newest-first. */
    suspend fun getThreads(): List<SmsThread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<SmsThread>()
        val seen = mutableSetOf<Long>()

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
            ),
            null, null,
            "${Telephony.Sms.DATE} DESC"
        ) ?: return@withContext emptyList()

        cursor.use {
            val colThread = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val colAddress = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val colBody = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val colDate = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val colRead = it.getColumnIndexOrThrow(Telephony.Sms.READ)

            while (it.moveToNext()) {
                val threadId = it.getLong(colThread)
                if (!seen.add(threadId)) continue

                val address = it.getString(colAddress) ?: continue
                threads.add(
                    SmsThread(
                        threadId = threadId,
                        address = address,
                        contactName = resolveContactName(address),
                        snippet = it.getString(colBody) ?: "",
                        date = it.getLong(colDate),
                        isRead = it.getInt(colRead) == 1,
                    )
                )
            }
        }
        threads
    }

    /** Returns all messages in a thread, sorted oldest-first. */
    suspend fun getMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
            ),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC"
        ) ?: return@withContext emptyList()

        cursor.use {
            val colId = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val colBody = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val colDate = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val colType = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                messages.add(
                    SmsMessage(
                        id = it.getLong(colId),
                        body = it.getString(colBody) ?: "",
                        date = it.getLong(colDate),
                        isSent = it.getInt(colType) == Telephony.Sms.MESSAGE_TYPE_SENT,
                    )
                )
            }
        }
        messages
    }

    /** Returns the thread ID for a given [address], or null if no thread exists yet. */
    suspend fun findThreadId(address: String): Long? = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.ADDRESS} = ?",
            arrayOf(address),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    /** Sends a plain-text SMS to [address] and returns only after Android reports the send result. */
    suspend fun sendMessage(address: String, body: String) = withContext(Dispatchers.IO) {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        awaitSendResult(smsManager, address, body)
    }

    private suspend fun awaitSendResult(
        smsManager: SmsManager,
        address: String,
        body: String,
    ) = suspendCancellableCoroutine { continuation ->
        val action = "${context.packageName}.SMS_SENT.${System.nanoTime()}"
        val intent = android.content.Intent(action)
        val requestCode = (System.nanoTime() and Int.MAX_VALUE.toLong()).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                runCatching { context.unregisterReceiver(this) }
                if (!continuation.isActive) return

                if (resultCode == Activity.RESULT_OK) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(SmsSendException(resultCode))
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        continuation.invokeOnCancellation {
            runCatching { context.unregisterReceiver(receiver) }
            pendingIntent.cancel()
        }

        runCatching {
            smsManager.sendTextMessage(address, null, body, pendingIntent, null)
        }.onFailure { error ->
            runCatching { context.unregisterReceiver(receiver) }
            pendingIntent.cancel()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString())
        )
    }

    private fun resolveContactName(phoneNumber: String): String? = runCatching {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}

class SmsSendException(val result: Int) : Exception("SMS send failed with result code $result")
