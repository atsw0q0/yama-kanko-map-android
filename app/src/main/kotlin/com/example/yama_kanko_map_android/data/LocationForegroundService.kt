package com.example.yama_kanko_map_android.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

data class Spot(val name: String, val lat: Double, val lng: Double)

class LocationForegroundService : Service() {

  companion object {
    const val CHANNEL_ID = "location_foreground_channel"
    const val ARRIVAL_CHANNEL_ID = "arrival_alert_channel"
    const val NOTIF_ID = 1001

    // 総武線沿線（千葉駅〜秋葉原駅）の各駅データ
    val DEFAULT_SPOTS = listOf(
      Spot("千葉駅", 35.613271, 140.113247),
      Spot("西千葉駅", 35.6225867, 140.1033586),
      Spot("稲毛駅", 35.6373207, 140.092457),
      Spot("新検見川駅", 35.651667, 140.072222),
      Spot("幕張駅", 35.659167, 140.057778),
      Spot("幕張本郷駅", 35.672611, 140.042222),
      Spot("津田沼駅", 35.691389, 140.020278),
      Spot("船橋駅", 35.701667, 139.984167),
      Spot("西船橋駅", 35.7075, 139.959167),
      Spot("下総中山駅", 35.714167, 139.943056),
      Spot("本八幡駅", 35.720833, 139.927222),
      Spot("市川駅", 35.728889, 139.907778),
      Spot("小岩駅", 35.733333, 139.881667),
      Spot("新小岩駅", 35.716944, 139.857778),
      Spot("平井駅", 35.706389, 139.8425),
      Spot("亀戸駅", 35.697222, 139.826111),
      Spot("錦糸町駅", 35.696389, 139.814444),
      Spot("両国駅", 35.695556, 139.793056),
      Spot("浅草橋駅", 35.6975, 139.786389),
      Spot("秋葉原駅", 35.698333, 139.773056)
    )
  }

  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var locationCallback: LocationCallback
  private val notifiedSpots = mutableSetOf<String>()

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    locationCallback = object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult) {
        val current = locationResult.lastLocation ?: return
        checkProximity(current)
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("バックグラウンド位置監視中")
      .setContentText("ピン留め地点への接近を監視しています...")
      .setSmallIcon(android.R.drawable.ic_menu_mylocation)
      .setOngoing(true)
      .build()

    startForeground(NOTIF_ID, notification)
    startLocationUpdates()

    return START_STICKY
  }

  private fun startLocationUpdates() {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
      .setMinUpdateIntervalMillis(3000L)
      .setMinUpdateDistanceMeters(5f)
      .build()

    try {
      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    } catch (_: SecurityException) {
      // Permission missing
    }
  }

  private fun checkProximity(currentLocation: Location) {
    DEFAULT_SPOTS.forEach { spot ->
      val results = FloatArray(1)
      Location.distanceBetween(
        currentLocation.latitude, currentLocation.longitude,
        spot.lat, spot.lng,
        results
      )

      val distanceMeters = results[0]

      // 条件 ②: 半径15m以内に近づいたらプッシュ通知
      if (distanceMeters <= 15.0) {
        // 条件 ③: 連打・連続通知を防ぐ（未通知の場合のみ通知）
        if (!notifiedSpots.contains(spot.name)) {
          notifiedSpots.add(spot.name)
          sendArrivalNotification(spot.name)
        }
      } else if (distanceMeters > 100.0) {
        // 100m以上離れた場合は通知済みフラグをリセット
        notifiedSpots.remove(spot.name)
      }
    }
  }

  private fun sendArrivalNotification(spotName: String) {
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(this, ARRIVAL_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("目的地に到着しました。 「$spotName」")
      .setContentText("目的地（$spotName）の15m以内に到着しました。")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(spotName.hashCode(), notification)
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)

      val serviceChannel = NotificationChannel(
        CHANNEL_ID,
        "バックグラウンド位置情報サービス",
        NotificationManager.IMPORTANCE_LOW
      )

      val alertChannel = NotificationChannel(
        ARRIVAL_CHANNEL_ID,
        "接近通知アラート",
        NotificationManager.IMPORTANCE_HIGH
      )

      manager?.createNotificationChannel(serviceChannel)
      manager?.createNotificationChannel(alertChannel)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    fusedLocationClient.removeLocationUpdates(locationCallback)
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
