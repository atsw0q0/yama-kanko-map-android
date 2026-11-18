package com.example.yama_kanko_map_android.data

import android.content.Context
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ActivitySportType(val displayName: String, val emoji: String) {
  WALKING("徒歩", "🚶"),
  RUNNING("ランニング", "🏃"),
  TRAIL_RUNNING("トレイルランニング", "🏔")
}

data class UserProfile(
  val heightCm: Float = 170f,
  val weightKg: Float = 65f,
  val targetWeightKg: Float = 63f
) {
  /**
   * 1か月(30日)で目標体重を達成するための1日の運動目標消費カロリー (kcal/日)
   * 脂肪1kg = 約7,200kcal
   * 1日の目標消費カロリー = (現在の体重 - 目標体重) * 7200 / 30 = 差分 * 240 kcal
   */
  fun dailyTargetCaloriesKcal(): Int {
    val weightDiffKg = (weightKg - targetWeightKg).coerceAtLeast(0f)
    return (weightDiffKg * 240f).toInt()
  }
}

object ProfileRepository {
  private const val PREFS_NAME = "user_profile_prefs"

  fun saveProfile(context: Context, profile: UserProfile) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putFloat("heightCm", profile.heightCm)
      .putFloat("weightKg", profile.weightKg)
      .putFloat("targetWeightKg", profile.targetWeightKg)
      .apply()
  }

  fun loadProfile(context: Context): UserProfile {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val height = prefs.getFloat("heightCm", 170f)
    val weight = prefs.getFloat("weightKg", 65f)
    val targetWeight = prefs.getFloat("targetWeightKg", 63f)
    return UserProfile(heightCm = height, weightKg = weight, targetWeightKg = targetWeight)
  }
}

data class TrackPoint(
  val lat: Double,
  val lng: Double,
  val alt: Double,
  val timeMillis: Long
)

data class RecordingResult(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val sportType: ActivitySportType = ActivitySportType.WALKING,
  val dateStr: String,
  val distanceMeters: Float,
  val durationMillis: Long,
  val caloriesKcal: Float,
  val elevationGainMeters: Float = 0f,
  val elevationLossMeters: Float = 0f,
  val vamMetersPerHour: Float = 0f,
  val averageSlopePercent: Float = 0f,
  val averagePaceSecondsPerKm: Float = 0f,
  val estimatedPitchSpm: Int = 0,
  val kmSplitsJson: String = "",
  val gpxFilePath: String? = null
) {
  fun formattedDistance(): String {
    return if (distanceMeters >= 1000f) {
      String.format(Locale.US, "%.2f km", distanceMeters / 1000f)
    } else {
      "${distanceMeters.toInt()} m"
    }
  }

  fun formattedDuration(): String {
    val seconds = (durationMillis / 1000) % 60
    val minutes = (durationMillis / (1000 * 60)) % 60
    val hours = durationMillis / (1000 * 60 * 60)
    return if (hours > 0) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun formattedCalories(): String = "${caloriesKcal.toInt()} kcal"
  fun formattedElevationGain(): String = "+${elevationGainMeters.toInt()} m"
  fun formattedElevationLoss(): String = "-${elevationLossMeters.toInt()} m"
  fun formattedVam(): String = "${vamMetersPerHour.toInt()} m/h"
  fun formattedSlope(): String = String.format(Locale.US, "%.1f %%", averageSlopePercent)

  fun formattedAveragePace(): String {
    if (averagePaceSecondsPerKm <= 0f) return "--'--\"/km"
    val totalSec = averagePaceSecondsPerKm.toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d'%02d\"/km", min, sec)
  }

  fun formattedPitch(): String = "$estimatedPitchSpm spm"

  fun getKmSplitsList(): List<String> {
    if (kmSplitsJson.isEmpty()) return emptyList()
    return try {
      val array = JSONArray(kmSplitsJson)
      val list = mutableListOf<String>()
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }
}

data class SportSummary(
  val sportType: ActivitySportType,
  val count: Int,
  val totalCaloriesKcal: Float,
  val totalDistanceKm: Float,
  val totalDurationMillis: Long
)

data class MonthlySummary(
  val yearMonthStr: String,
  val sportSummaries: List<SportSummary>,
  val totalCaloriesKcal: Float,
  val totalDistanceKm: Float,
  val totalDurationMillis: Long
)

object ResultCalculator {
  fun calculateResult(
    title: String,
    sportType: ActivitySportType,
    points: List<TrackPoint>,
    userProfile: UserProfile,
    gpxFile: File?
  ): RecordingResult {
    if (points.isEmpty()) {
      return RecordingResult(
        title = title,
        sportType = sportType,
        dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date()),
        distanceMeters = 0f,
        durationMillis = 0L,
        caloriesKcal = 0f,
        gpxFilePath = gpxFile?.absolutePath
      )
    }

    var totalDistance = 0f
    val durationMillis = (points.last().timeMillis - points.first().timeMillis).coerceAtLeast(0L)
    val durationHours = durationMillis / 3600000.0f

    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val curr = points[i]
      val results = FloatArray(1)
      Location.distanceBetween(prev.lat, prev.lng, curr.lat, curr.lng, results)
      totalDistance += results[0]
    }

    val speedKmh = if (durationHours > 0f) (totalDistance / 1000f) / durationHours else 0f
    val mets = when (sportType) {
      ActivitySportType.WALKING -> if (speedKmh > 5f) 4.3f else 3.5f
      ActivitySportType.RUNNING -> if (speedKmh > 10f) 10.0f else 8.0f
      ActivitySportType.TRAIL_RUNNING -> 9.0f
    }
    val calories = (mets * userProfile.weightKg * durationHours * 1.05f).coerceAtLeast(0f)

    // Moving average filter (3-point) for altitude noise reduction
    val rawAlts = points.map { it.alt }
    val smoothedAlts = FloatArray(rawAlts.size)
    for (i in rawAlts.indices) {
      when (i) {
        0 -> smoothedAlts[i] = rawAlts[0].toFloat()
        rawAlts.size - 1 -> smoothedAlts[i] = rawAlts.last().toFloat()
        else -> smoothedAlts[i] = ((rawAlts[i - 1] + rawAlts[i] + rawAlts[i + 1]) / 3.0).toFloat()
      }
    }

    // Accumulate gain and loss with 3.0m threshold
    var elevationGain = 0f
    var elevationLoss = 0f
    var lastValidAlt = smoothedAlts[0]

    for (i in 1 until smoothedAlts.size) {
      val diff = smoothedAlts[i] - lastValidAlt
      if (diff >= 3.0f) {
        elevationGain += diff
        lastValidAlt = smoothedAlts[i]
      } else if (diff <= -3.0f) {
        elevationLoss += -diff
        lastValidAlt = smoothedAlts[i]
      }
    }

    val vam = if (durationHours > 0f) elevationGain / durationHours else 0f
    val slope = if (totalDistance > 0f) (elevationGain / totalDistance) * 100f else 0f

    val totalDistanceKm = totalDistance / 1000f
    val avgPaceSecPerKm = if (totalDistanceKm > 0.05f) (durationMillis / 1000f) / totalDistanceKm else 0f
    val estimatedPitch = if (sportType == ActivitySportType.RUNNING && speedKmh > 1f) {
      (150 + (speedKmh * 2.5f)).toInt().coerceIn(140, 200)
    } else 0

    // Compute 1km splits
    val splitsList = mutableListOf<String>()
    var accumulatedDist = 0f
    var lastKmTime = points.first().timeMillis
    var kmCount = 1

    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val curr = points[i]
      val results = FloatArray(1)
      Location.distanceBetween(prev.lat, prev.lng, curr.lat, curr.lng, results)
      accumulatedDist += results[0]

      if (accumulatedDist >= kmCount * 1000f) {
        val splitDurationSec = (curr.timeMillis - lastKmTime) / 1000
        val min = splitDurationSec / 60
        val sec = splitDurationSec % 60
        splitsList.add("${kmCount}km: ${min}'${String.format(Locale.US, "%02d", sec)}\"")
        lastKmTime = curr.timeMillis
        kmCount++
      }
    }

    val splitsJson = JSONArray(splitsList).toString()
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(points.first().timeMillis))

    return RecordingResult(
      title = "${sportType.displayName}アクティビティ",
      sportType = sportType,
      dateStr = dateStr,
      distanceMeters = totalDistance,
      durationMillis = durationMillis,
      caloriesKcal = calories,
      elevationGainMeters = elevationGain,
      elevationLossMeters = elevationLoss,
      vamMetersPerHour = vam,
      averageSlopePercent = slope,
      averagePaceSecondsPerKm = avgPaceSecPerKm,
      estimatedPitchSpm = estimatedPitch,
      kmSplitsJson = splitsJson,
      gpxFilePath = gpxFile?.absolutePath
    )
  }
}

object ResultRepository {
  private const val FILE_NAME = "past_results_history.json"

  fun saveResult(context: Context, result: RecordingResult) {
    val list = loadResults(context).toMutableList()
    list.add(0, result)
    saveList(context, list)
  }

  fun deleteResult(context: Context, resultId: String) {
    val list = loadResults(context).filterNot { it.id == resultId }
    saveList(context, list)
    val itemToDelete = loadResults(context).find { it.id == resultId }
    itemToDelete?.gpxFilePath?.let { path ->
      val file = File(path)
      if (file.exists()) file.delete()
    }
  }

  fun clearAllResults(context: Context) {
    val currentList = loadResults(context)
    currentList.forEach { item ->
      item.gpxFilePath?.let { path ->
        val file = File(path)
        if (file.exists()) file.delete()
      }
    }
    saveList(context, emptyList())
  }

  fun getDailyTotalCalories(context: Context, targetDateStr: String): Float {
    // targetDateStr: "yyyy/MM/dd"
    return loadResults(context)
      .filter { it.dateStr.startsWith(targetDateStr) }
      .sumOf { it.caloriesKcal.toDouble() }
      .toFloat()
  }

  fun getResultsForDate(context: Context, targetDateStr: String): List<RecordingResult> {
    return loadResults(context).filter { it.dateStr.startsWith(targetDateStr) }
  }

  fun getMonthlySummary(context: Context, yearMonthStr: String): MonthlySummary {
    // yearMonthStr: "yyyy/MM"
    val monthResults = loadResults(context).filter { it.dateStr.startsWith(yearMonthStr) }

    val sportSummaries = ActivitySportType.entries.map { sport ->
      val items = monthResults.filter { it.sportType == sport }
      SportSummary(
        sportType = sport,
        count = items.size,
        totalCaloriesKcal = items.sumOf { it.caloriesKcal.toDouble() }.toFloat(),
        totalDistanceKm = items.sumOf { (it.distanceMeters / 1000f).toDouble() }.toFloat(),
        totalDurationMillis = items.sumOf { it.durationMillis }
      )
    }

    return MonthlySummary(
      yearMonthStr = yearMonthStr,
      sportSummaries = sportSummaries,
      totalCaloriesKcal = monthResults.sumOf { it.caloriesKcal.toDouble() }.toFloat(),
      totalDistanceKm = monthResults.sumOf { (it.distanceMeters / 1000f).toDouble() }.toFloat(),
      totalDurationMillis = monthResults.sumOf { it.durationMillis }
    )
  }

  fun loadResults(context: Context): List<RecordingResult> {
    val file = File(context.getExternalFilesDir(null), FILE_NAME)
    if (!file.exists()) return emptyList()

    return try {
      val jsonArray = JSONArray(file.readText())
      val list = mutableListOf<RecordingResult>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val sportTypeName = obj.optString("sportType", ActivitySportType.WALKING.name)
        val sportType = try {
          ActivitySportType.valueOf(sportTypeName)
        } catch (_: Exception) {
          ActivitySportType.WALKING
        }

        list.add(
          RecordingResult(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", "アクティビティ"),
            sportType = sportType,
            dateStr = obj.optString("dateStr", ""),
            distanceMeters = obj.optDouble("distanceMeters", 0.0).toFloat(),
            durationMillis = obj.optLong("durationMillis", 0L),
            caloriesKcal = obj.optDouble("caloriesKcal", 0.0).toFloat(),
            elevationGainMeters = obj.optDouble("elevationGainMeters", 0.0).toFloat(),
            elevationLossMeters = obj.optDouble("elevationLossMeters", 0.0).toFloat(),
            vamMetersPerHour = obj.optDouble("vamMetersPerHour", 0.0).toFloat(),
            averageSlopePercent = obj.optDouble("averageSlopePercent", 0.0).toFloat(),
            averagePaceSecondsPerKm = obj.optDouble("averagePaceSecondsPerKm", 0.0).toFloat(),
            estimatedPitchSpm = obj.optInt("estimatedPitchSpm", 0),
            kmSplitsJson = obj.optString("kmSplitsJson", ""),
            gpxFilePath = obj.optString("gpxFilePath").takeIf { it.isNotEmpty() }
          )
        )
      }
      list
    } catch (e: Exception) {
      emptyList()
    }
  }

  private fun saveList(context: Context, list: List<RecordingResult>) {
    val jsonArray = JSONArray()
    list.forEach { item ->
      val obj = JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("sportType", item.sportType.name)
        put("dateStr", item.dateStr)
        put("distanceMeters", item.distanceMeters.toDouble())
        put("durationMillis", item.durationMillis)
        put("caloriesKcal", item.caloriesKcal.toDouble())
        put("elevationGainMeters", item.elevationGainMeters.toDouble())
        put("elevationLossMeters", item.elevationLossMeters.toDouble())
        put("vamMetersPerHour", item.vamMetersPerHour.toDouble())
        put("averageSlopePercent", item.averageSlopePercent.toDouble())
        put("averagePaceSecondsPerKm", item.averagePaceSecondsPerKm.toDouble())
        put("estimatedPitchSpm", item.estimatedPitchSpm)
        put("kmSplitsJson", item.kmSplitsJson)
        put("gpxFilePath", item.gpxFilePath ?: "")
      }
      jsonArray.put(obj)
    }

    val file = File(context.getExternalFilesDir(null), FILE_NAME)
    file.writeText(jsonArray.toString())
  }
}
