package com.example.call

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_ANSWER_CALL -> {
                CallRepository.answerCall()
                val activityIntent = Intent(context, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                        .toBundle()
                    runCatching { context.startActivity(activityIntent, options) }
                        .onFailure { runCatching { context.startActivity(activityIntent) } }
                } else {
                    runCatching { context.startActivity(activityIntent) }
                }
            }

            ACTION_DECLINE_CALL -> {
                CallRepository.rejectOrDisconnect()
            }
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.example.call.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.example.call.ACTION_DECLINE_CALL"
    }
}
