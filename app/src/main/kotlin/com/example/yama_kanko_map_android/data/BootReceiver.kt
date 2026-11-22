package com.example.yama_kanko_map_android.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      if (RecordingPreferences.isRecording(context)) {
        val serviceIntent = Intent(context, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
      }
    }
  }
}
