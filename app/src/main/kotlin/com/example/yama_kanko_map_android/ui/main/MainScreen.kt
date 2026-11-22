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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation3.runtime.NavKey
import com.example.yama_kanko_map_android.History
import com.example.yama_kanko_map_android.Insights
import com.example.yama_kanko_map_android.Profile
import com.example.yama_kanko_map_android.R
import com.example.yama_kanko_map_android.data.ActivitySportType
import com.example.yama_kanko_map_android.data.BatteryOptimizationUtil
import com.example.yama_kanko_map_android.data.LocationForegroundService
import com.example.yama_kanko_map_android.data.ProfileRepository
import com.example.yama_kanko_map_android.data.RecordingPreferences
import com.example.yama_kanko_map_android.data.RecordingResult
import com.example.yama_kanko_map_android.data.ResultCalculator
import com.example.yama_kanko_map_android.data.ResultRepository
import com.example.yama_kanko_map_android.data.Spot
import com.example.yama_kanko_map_android.data.WalkedPathManager
import com.example.yama_kanko_map_android.ui.theme.YamaKankoMapAndroidTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

  var currentPositionText by remember { mutableStateOf("現在地を取得中...") }
  var lastUpdateTime by remember { mutableLongStateOf(0L) }
  var isBackgroundModeOn by remember { mutableStateOf(false) }
  var currentSportType by remember { mutableStateOf(ActivitySportType.WALKING) }

  var userProfile by remember { mutableStateOf(ProfileRepository.loadProfile(context)) }
  var showSportSelectionDialog by remember { mutableStateOf(false) }
  var showBatteryOptDialog by remember { mutableStateOf(false) }

  var selectedSpot by remember { mutableStateOf<Spot?>(null) }
  var activeResultDialog by remember { mutableStateOf<RecordingResult?>(null) }

  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      zoom(13.0)
      center(Point.fromLngLat(140.1033, 35.6225))
      pitch(0.0)
      bearing(0.0)
    }
  }

  val startBgMode: (ActivitySportType) -> Unit = { selectedSportType ->
    currentSportType = selectedSportType
    isBackgroundModeOn = true
    RecordingPreferences.setRecordingState(context, isRecording = true, sportType = selectedSportType)
    ContextCompat.startForegroundService(
      context,
      Intent(context, LocationForegroundService::class.java)
    )
    Toast.makeText(context, "${selectedSportType.displayName}モードでBG監視を開始しました。", Toast.LENGTH_SHORT).show()
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      if (!BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)) {
        showBatteryOptDialog = true
      } else {
        showSportSelectionDialog = true
      }
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
    userProfile = ProfileRepository.loadProfile(context)

    // Restore previous active session after crash/reboot if any
    val restoredPoints = WalkedPathManager.restoreSessionIfAny(context)
    if (RecordingPreferences.isRecording(context)) {
      isBackgroundModeOn = true
      currentSportType = RecordingPreferences.getSportType(context)
      if (restoredPoints > 0) {
        Toast.makeText(context, "前回の記録セッションを自動復旧しました (軌跡:${restoredPoints}pt)", Toast.LENGTH_LONG).show()
      }
    }

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

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = drawerState.isOpen,
    drawerContent = {
      ModalDrawerSheet {
        Column(modifier = Modifier.padding(20.dp)) {
          Text("⛰️ ヤマ観光マップ", style = MaterialTheme.typography.titleLarge)
          Text("山岳観光 & アクティビティ記録", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()

        NavigationDrawerItem(
          label = { Text("🗺️ マップホーム") },
          selected = true,
          onClick = {
            scope.launch { drawerState.close() }
            mapViewportState.transitionToFollowPuckState()
          }
        )
        NavigationDrawerItem(
          label = { Text("📊 インサイト & カレンダー") },
          selected = false,
          onClick = {
            scope.launch { drawerState.close() }
            onItemClick(Insights)
          }
        )
        NavigationDrawerItem(
          label = { Text("📜 アクティビティ履歴") },
          selected = false,
          onClick = {
            scope.launch { drawerState.close() }
            onItemClick(History)
          }
        )
        NavigationDrawerItem(
          label = { Text("👤 プロフィール設定") },
          selected = false,
          onClick = {
            scope.launch { drawerState.close() }
            onItemClick(Profile)
          }
        )
      }
    }
  ) {
    Scaffold(
      modifier = modifier,
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text("⛰️ ヤマ観光マップ") },
          navigationIcon = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
              Text("☰", style = MaterialTheme.typography.titleLarge)
            }
          },
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

                val gpxFile = WalkedPathManager.exportToGpxFile(context)
                val result = ResultCalculator.calculateResult(
                  title = "${currentSportType.displayName}アクティビティ",
                  sportType = currentSportType,
                  points = WalkedPathManager.trackPoints.toList(),
                  userProfile = userProfile,
                  gpxFile = gpxFile
                )

                ResultRepository.saveResult(context, result)
                WalkedPathManager.clearSession(context)
                activeResultDialog = result
              } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                  ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  if (!BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)) {
                    showBatteryOptDialog = true
                  } else {
                    showSportSelectionDialog = true
                  }
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

        val pathPoints = remember(WalkedPathManager.trackPoints.size) {
          WalkedPathManager.trackPoints.map { Point.fromLngLat(it.lng, it.lat) }
        }

        MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = mapViewportState
        ) {
          // Draw Walked Trail in Pink (#FF1493)
          if (pathPoints.size >= 2) {
            PolylineAnnotation(
              points = pathPoints
            ) {
              lineColor = Color(0xFFFF1493) // Deep Pink
              lineWidth = 6.0
            }
          }

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

        // Status Overlay at top center
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
              text = if (isBackgroundModeOn) "BGモード監視中 (${currentSportType.displayName} 軌跡:${WalkedPathManager.trackPoints.size}pt)" else "バックグラウンドモードOFF",
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
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
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

  // Battery Optimization Guidance Dialog
  if (showBatteryOptDialog) {
    AlertDialog(
      onDismissRequest = {
        showBatteryOptDialog = false
        showSportSelectionDialog = true
      },
      title = { Text("⚡ バッテリー制限の案内", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("長時間の登山やアクティビティ中にバックグラウンド位置記録を途切れさせないため、バッテリー最適化の解除をおすすめします。", style = MaterialTheme.typography.bodyMedium)
          Text("※設定画面で「無制限」または「最適化しない」を選択してください。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showBatteryOptDialog = false
            BatteryOptimizationUtil.openBatteryOptimizationSettings(context)
            showSportSelectionDialog = true
          }
        ) {
          Text("設定画面を開く")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showBatteryOptDialog = false
            showSportSelectionDialog = true
          }
        ) {
          Text("スキップして開始")
        }
      }
    )
  }

  // Sport Selection Dialog
  if (showSportSelectionDialog) {
    SportSelectionDialog(
      onDismiss = { showSportSelectionDialog = false },
      onSportSelected = { sport ->
        showSportSelectionDialog = false
        startBgMode(sport)
      }
    )
  }

  // Result Dialog
  activeResultDialog?.let { result ->
    ResultDialog(
      result = result,
      onDismiss = {
        activeResultDialog = null
        WalkedPathManager.clearPath()
      },
      onExportGpx = {
        exportGpxFile(context, result)
      }
    )
  }
}

@Composable
fun SportSelectionDialog(
  onDismiss: () -> Unit,
  onSportSelected: (ActivitySportType) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("🏃 種目の選択", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("記録するアクティビティ種目を選択してください:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ActivitySportType.entries.forEach { sport ->
          Card(
            onClick = { onSportSelected(sport) },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(sport.emoji, style = MaterialTheme.typography.headlineMedium)
              Column {
                Text(sport.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                  when (sport) {
                    ActivitySportType.WALKING -> "距離・活動時間・消費カロリー"
                    ActivitySportType.RUNNING -> "距離・時間・カロリー・ペース・ピッチ・スプリット"
                    ActivitySportType.TRAIL_RUNNING, ActivitySportType.MOUNTAINEERING -> "距離・時間・カロリー・獲得標高・VAM・勾配"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = { onDismiss() }) {
        Text("閉じる")
      }
    }
  )
}

@Composable
fun ResultDialog(
  result: RecordingResult,
  onDismiss: () -> Unit,
  onExportGpx: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = "📊 ${result.sportType.emoji} アクティビティリザルト", style = MaterialTheme.typography.titleLarge)
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = result.title, style = MaterialTheme.typography.titleMedium)
        Text(text = "日時: ${result.dateStr}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()

        // 共通項目: 移動距離, 活動時間, 消費カロリー
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "移動距離", style = MaterialTheme.typography.labelSmall)
            Text(text = result.formattedDistance(), style = MaterialTheme.typography.bodyLarge)
          }
          Column {
            Text(text = "活動時間", style = MaterialTheme.typography.labelSmall)
            Text(text = result.formattedDuration(), style = MaterialTheme.typography.bodyLarge)
          }
          Column {
            Text(text = "消費カロリー", style = MaterialTheme.typography.labelSmall)
            Text(text = result.formattedCalories(), style = MaterialTheme.typography.bodyLarge)
          }
        }

        // ランニング専用項目
        if (result.sportType == ActivitySportType.RUNNING) {
          HorizontalDivider()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "1km平均ペース", style = MaterialTheme.typography.labelSmall)
              Text(text = result.formattedAveragePace(), style = MaterialTheme.typography.bodyLarge)
            }
            Column {
              Text(text = "平均ピッチ", style = MaterialTheme.typography.labelSmall)
              Text(text = result.formattedPitch(), style = MaterialTheme.typography.bodyLarge)
            }
          }

          val splits = result.getKmSplitsList()
          if (splits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "1km区間スプリット", style = MaterialTheme.typography.labelSmall)
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 100.dp)
            ) {
              items(splits) { split ->
                Text(text = split, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }

        // トレイルランニング＆登山専用項目
        if (result.sportType == ActivitySportType.TRAIL_RUNNING || result.sportType == ActivitySportType.MOUNTAINEERING) {
          HorizontalDivider()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "獲得標高 (上昇 / 下降)", style = MaterialTheme.typography.labelSmall)
              Text(
                text = "${result.formattedElevationGain()} / ${result.formattedElevationLoss()}",
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "上昇速度 (VAM)", style = MaterialTheme.typography.labelSmall)
              Text(text = result.formattedVam(), style = MaterialTheme.typography.bodyLarge)
            }
            Column {
              Text(text = "平均勾配", style = MaterialTheme.typography.labelSmall)
              Text(text = result.formattedSlope(), style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onExportGpx) {
        Text("GPXエクスポート")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("閉じる")
      }
    }
  )
}

fun exportGpxFile(context: android.content.Context, result: RecordingResult) {
  val path = result.gpxFilePath
  val file = if (path != null) File(path) else null

  if (file != null && file.exists()) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "GPXファイルを共有・保存"))
    } catch (_: Exception) {
      Toast.makeText(context, "ファイル共有に失敗しました", Toast.LENGTH_SHORT).show()
    }
  } else {
    Toast.makeText(context, "GPXファイルが存在しません", Toast.LENGTH_SHORT).show()
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
