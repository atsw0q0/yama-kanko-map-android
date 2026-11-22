package com.example.yama_kanko_map_android.data

import android.content.Context

object RecordingPreferences {
  private const val PREFS_NAME = "recording_session_prefs"
  private const val KEY_IS_RECORDING = "is_recording"
  private const val KEY_SPORT_TYPE = "sport_type"
  private const val KEY_START_TIME = "start_time_millis"

  fun setRecordingState(
    context: Context,
    isRecording: Boolean,
    sportType: ActivitySportType = ActivitySportType.WALKING,
    startTimeMillis: Long = System.currentTimeMillis()
  ) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_IS_RECORDING, isRecording)
      .putString(KEY_SPORT_TYPE, sportType.name)
      .putLong(KEY_START_TIME, startTimeMillis)
      .apply()
  }

  fun isRecording(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_IS_RECORDING, false)
  }

  fun getSportType(context: Context): ActivitySportType {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(KEY_SPORT_TYPE, ActivitySportType.WALKING.name)
    return try {
      ActivitySportType.valueOf(name ?: ActivitySportType.WALKING.name)
    } catch (_: Exception) {
      ActivitySportType.WALKING
    }
  }

  fun getStartTimeMillis(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_START_TIME, System.currentTimeMillis())
  }

  fun clear(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
  }
}
