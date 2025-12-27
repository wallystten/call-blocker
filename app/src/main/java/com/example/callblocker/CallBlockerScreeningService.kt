package com.example.callblocker

import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallBlockerScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart
        
        if (incomingNumber == null) {
            respondToCall(callDetails, allowCall())
            return
        }

        Log.d("CallBlocker", "Chamada de: $incomingNumber")

        if (isNumberInContacts(incomingNumber)) {
            Log.d("CallBlocker", "Número nos contatos - permitindo")
            respondToCall(callDetails, allowCall())
        } else {
            Log.d("CallBlocker", "Número desconhecido - bloqueando")
            respondToCall(callDetails, blockCall())
        }
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(cleanNumber)
            .build()

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        val exists = cursor?.use { it.count > 0 } ?: false
        return exists
    }

    private fun allowCall(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()
    }

    private fun blockCall(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }
}
