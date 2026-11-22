package com.example.yama_kanko_map_android.data

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationUtil {

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun openBatteryOptimizationSettings(context: Context) {
    try {
      val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (_: Exception) {
      val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
      fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(fallbackIntent)
    }
  }
}
