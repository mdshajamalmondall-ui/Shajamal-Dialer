package com.example.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.ContactItem
import com.example.data.model.SimSlotInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceDirectoryRepository(
    private val context: Context,
    private val dualSimCallManager: DualSimCallManager
) {

    suspend fun loadCallLogs(slots: List<SimSlotInfo>, limit: Int = 150): List<CallLogItem> =
        withContext(Dispatchers.IO) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return@withContext emptyList()

            val items = mutableListOf<CallLogItem>()
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )

            runCatching {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val accIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

                    while (cursor.moveToNext() && items.size < limit) {
                        val typeInt = cursor.getInt(typeIdx)
                        val direction = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                            CallLog.Calls.VOICEMAIL_TYPE -> CallDirection.VOICEMAIL
                            CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
                            else -> CallDirection.INCOMING
                        }
                        val accountId = cursor.getString(accIdx)
                        items.add(
                            CallLogItem(
                                id = cursor.getLong(idIdx),
                                number = cursor.getString(numIdx).orEmpty(),
                                cachedName = cursor.getString(nameIdx),
                                direction = direction,
                                dateMillis = cursor.getLong(dateIdx),
                                durationSeconds = cursor.getLong(durIdx),
                                phoneAccountId = accountId,
                                simSlotIndex = dualSimCallManager.resolveSimSlotForAccountId(accountId, slots)
                            )
                        )
                    }
                }
            }
            items
        }

    suspend fun deleteCallLogEntry(id: Long): Boolean = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext false

        runCatching {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext emptyList()

        val contactMap = linkedMapOf<Long, MutableContactBuilder>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx).orEmpty().ifBlank { "Unnamed Contact" }
                    val rawNumber = cursor.getString(numIdx).orEmpty().trim()
                    if (rawNumber.isBlank()) continue
                    val starred = cursor.getInt(starIdx) == 1
                    val photo = cursor.getString(photoIdx)

                    val builder = contactMap.getOrPut(contactId) {
                        MutableContactBuilder(contactId, name, starred, photo)
                    }
                    val normalized = rawNumber.replace("\\s+".toRegex(), "")
                    if (builder.numbers.none { it.replace("\\s+".toRegex(), "") == normalized }) {
                        builder.numbers.add(rawNumber)
                    }
                }
            }
        }

        contactMap.values.map {
            ContactItem(
                id = it.id,
                name = it.name,
                primaryNumber = it.numbers.first(),
                phoneNumbers = it.numbers.toList(),
                isStarred = it.isStarred,
                photoUri = it.photoUri
            )
        }
    }

    private data class MutableContactBuilder(
        val id: Long,
        val name: String,
        val isStarred: Boolean,
        val photoUri: String?,
        val numbers: MutableList<String> = mutableListOf()
    )

    companion object {
        fun nameToT9Digits(name: String): String {
            return buildString {
                for (ch in name.lowercase()) {
                    when (ch) {
                        in 'a'..'c' -> append('2')
                        in 'd'..'f' -> append('3')
                        in 'g'..'i' -> append('4')
                        in 'j'..'l' -> append('5')
                        in 'm'..'o' -> append('6')
                        in 'p'..'s' -> append('7')
                        in 't'..'v' -> append('8')
                        in 'w'..'z' -> append('9')
                        in '0'..'9' -> append(ch)
                    }
                }
            }
        }

        fun matchesDialQuery(contact: ContactItem, queryDigits: String): Boolean {
            if (queryDigits.isBlank()) return false
            val cleanQuery = queryDigits.filter { it.isDigit() || it == '+' }
            if (cleanQuery.isBlank()) return false

            val numberMatch = contact.phoneNumbers.any { num ->
                num.filter { it.isDigit() || it == '+' }.contains(cleanQuery)
            }
            if (numberMatch) return true

            val digitsOnly = cleanQuery.filter { it.isDigit() }
            if (digitsOnly.isEmpty()) return false
            return nameToT9Digits(contact.name).contains(digitsOnly)
        }
    }
}
