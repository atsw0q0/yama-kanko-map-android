package com.example.yama_kanko_map_android.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavKey
import com.example.yama_kanko_map_android.R
import com.example.yama_kanko_map_android.data.LocationForegroundService
import com.example.yama_kanko_map_android.data.Spot
import com.example.yama_kanko_map_android.ui.theme.YamaKankoMapAndroidTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var currentPositionText by remember { mutableStateOf("現在地を取得中...") }
  var lastUpdateTime by remember { mutableLongStateOf(0L) }
  var isBackgroundModeOn by remember { mutableStateOf(false) }
  var selectedSpot by remember { mutableStateOf<Spot?>(null) }

  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      zoom(13.0)
      center(Point.fromLngLat(140.1033, 35.6225))
      pitch(0.0)
      bearing(0.0)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      isBackgroundModeOn = true
      ContextCompat.startForegroundService(
        context,
        Intent(context, LocationForegroundService::class.java)
      )
      Toast.makeText(context, "バックグラウンドモードをONにしました。", Toast.LENGTH_SHORT).show()
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (!fineGranted && !coarseGranted) {
      currentPositionText = "位置情報パーミッションが拒否されました"
    }
  }

  LaunchedEffect(Unit) {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.app_name) + " - Mapbox現在地") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    },
    floatingActionButton = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
      ) {
        // Toggle Button for Background Mode
        Button(
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isBackgroundModeOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
          ),
          onClick = {
            if (isBackgroundModeOn) {
              isBackgroundModeOn = false
              context.stopService(Intent(context, LocationForegroundService::class.java))
              Toast.makeText(context, "バックグラウンドモードをOFFにしました。", Toast.LENGTH_SHORT).show()
            } else {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
              ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                isBackgroundModeOn = true
                ContextCompat.startForegroundService(
                  context,
                  Intent(context, LocationForegroundService::class.java)
                )
                Toast.makeText(context, "バックグラウンドモードをONにしました。", Toast.LENGTH_SHORT).show()
              }
            }
          }
        ) {
          Text(if (isBackgroundModeOn) "BGモード: OFF" else "BGモード: ON")
        }

        FloatingActionButton(
          onClick = {
            mapViewportState.transitionToFollowPuckState()
          }
        ) {
          Text("現在地", modifier = Modifier.padding(horizontal = 12.dp))
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      val redPinImage = rememberIconImage(
        key = "red-pin-icon",
        painter = painterResource(R.drawable.ic_red_pin)
      )

      MapboxMap(
        Modifier.fillMaxSize(),
        mapViewportState = mapViewportState
      ) {
        // Render Red Pinned Spots as annotations on map with onClick
        LocationForegroundService.DEFAULT_SPOTS.forEach { spot ->
          PointAnnotation(
            point = Point.fromLngLat(spot.lng, spot.lat),
            onClick = {
              selectedSpot = spot
              Toast.makeText(
                context,
                "【${spot.name}】\n緯度: ${spot.lat}\n経度: ${spot.lng}",
                Toast.LENGTH_SHORT
              ).show()
              true
            }
          ) {
            iconImage = redPinImage
            iconSize = 1.0
            textField = spot.name
            textSize = 12.0
            textOffset = listOf(0.0, 1.8)
          }
        }

        MapEffect(Unit) { mapView ->
          mapView.location.updateSettings {
            enabled = true
            locationPuck = createDefault2DPuck(withBearing = true)
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
          }

          mapView.location.addOnIndicatorPositionChangedListener { point ->
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= 10_000L) {
              lastUpdateTime = now
              val formattedLat = String.format(Locale.US, "%.3f", point.latitude())
              val formattedLng = String.format(Locale.US, "%.3f", point.longitude())
              currentPositionText = "現在地: $formattedLat, $formattedLng"
            }
          }

          mapViewportState.transitionToFollowPuckState()
        }
      }

      // Status overlay at top
      Surface(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.medium
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = currentPositionText,
            style = MaterialTheme.typography.bodySmall
          )
          Text(
            text = if (isBackgroundModeOn) "バックグラウンド接近監視中" else "バックグラウンドモードOFF",
            color = if (isBackgroundModeOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall
          )
        }
      }

      // Detail card at bottom when a marker is tapped
      selectedSpot?.let { spot ->
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 6.dp
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "📍 ${spot.name}",
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = "緯度: ${spot.lat}\n経度: ${spot.lng}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            TextButton(onClick = { selectedSpot = null }) {
              Text("閉じる")
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.padding(innerPadding),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      data.forEach { Greeting(it) }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name !",
    modifier = modifier.padding(16.dp, 16.dp),
    style = MaterialTheme.typography.bodyLarge
  )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
  YamaKankoMapAndroidTheme { MainScreen(listOf("Android")) }
}
