package com.example.yama_kanko_map_android.ui.insights

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.yama_kanko_map_android.data.ProfileRepository
import com.example.yama_kanko_map_android.data.RecordingResult
import com.example.yama_kanko_map_android.data.ResultRepository
import com.example.yama_kanko_map_android.ui.main.ResultDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SimpleYearMonth(val year: Int, val month: Int) {
  fun format(): String = String.format(Locale.US, "%04d/%02d", year, month)
  fun formatJapanese(): String = String.format(Locale.JAPAN, "%04d年 %02d月", year, month)

  fun minusMonths(): SimpleYearMonth {
    var y = year
    var m = month - 1
    if (m < 1) {
      m = 12
      y--
    }
    return SimpleYearMonth(y, m)
  }

  fun plusMonths(): SimpleYearMonth {
    var y = year
    var m = month + 1
    if (m > 12) {
      m = 1
      y++
    }
    return SimpleYearMonth(y, m)
  }

  fun daysInMonth(): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  fun firstDayOfWeek(): Int { // 0=Sun, 1=Mon...
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    return cal.get(Calendar.DAY_OF_WEEK) - 1
  }

  companion object {
    fun now(): SimpleYearMonth {
      val cal = Calendar.getInstance()
      return SimpleYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  BackHandler {
    onBackClick()
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("📊 インサイト & カレンダー") },
        navigationIcon = {
          TextButton(onClick = onBackClick) {
            Text("← 戻る")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("📊 インサイト") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("📅 カレンダー") }
        )
      }

      when (selectedTab) {
        0 -> InsightsTabContent()
        1 -> CalendarTabContent()
      }
    }
  }
}

@Composable
fun InsightsTabContent() {
  val context = LocalContext.current
  val userProfile = remember { ProfileRepository.loadProfile(context) }
  val targetCal = userProfile.dailyTargetCaloriesKcal()

  val todayStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date()) }
  val todayCal = remember { ResultRepository.getDailyTotalCalories(context, todayStr) }
  val progress = if (targetCal > 0) (todayCal / targetCal).coerceIn(0f, 1f) else 0f
  val isGoalAchieved = targetCal > 0 && todayCal >= targetCal

  var currentYearMonth by remember { mutableStateOf(SimpleYearMonth.now()) }
  val yearMonthStr = remember(currentYearMonth) { currentYearMonth.format() }
  val monthlySummary = remember(yearMonthStr) {
    ResultRepository.getMonthlySummary(context, yearMonthStr)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1日の目標消費カロリー達成度カード
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
      )
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "🔥 本日の目標カロリー達成度",
            style = MaterialTheme.typography.titleMedium
          )
          if (isGoalAchieved) {
            Surface(
              color = Color(0xFF4CAF50),
              shape = CircleShape
            ) {
              Text(
                text = "🎉 達成！",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(MaterialTheme.shapes.small),
          color = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "本日累計: ${todayCal.toInt()} kcal",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "目標: $targetCal kcal/日",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 月間サマリーカード
    Card(
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 月切り替えヘッダー
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = { currentYearMonth = currentYearMonth.minusMonths() }) {
            Text("◀ 前月")
          }
          Text(
            text = currentYearMonth.formatJapanese(),
            style = MaterialTheme.typography.titleMedium
          )
          TextButton(onClick = { currentYearMonth = currentYearMonth.plusMonths() }) {
            Text("翌月 ▶")
          }
        }

        HorizontalDivider()

        // 月間合計概要
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "月間総消費カロリー", style = MaterialTheme.typography.labelSmall)
            Text(
              text = "${monthlySummary.totalCaloriesKcal.toInt()} kcal",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Column {
            Text(text = "月間総距離", style = MaterialTheme.typography.labelSmall)
            Text(
              text = String.format(Locale.US, "%.1f km", monthlySummary.totalDistanceKm),
              style = MaterialTheme.typography.titleLarge
            )
          }
        }

        HorizontalDivider()

        Text(text = "種目別実施サマリー", style = MaterialTheme.typography.titleSmall)

        // 種目別内訳一覧
        monthlySummary.sportSummaries.forEach { summary ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(summary.sportType.emoji, style = MaterialTheme.typography.titleMedium)
              Column {
                Text(summary.sportType.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "${summary.count}回 実施",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${summary.totalCaloriesKcal.toInt()} kcal",
                style = MaterialTheme.typography.bodyLarge
              )
              Text(
                text = String.format(Locale.US, "%.1f km", summary.totalDistanceKm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CalendarTabContent() {
  val context = LocalContext.current
  val userProfile = remember { ProfileRepository.loadProfile(context) }
  val targetCal = userProfile.dailyTargetCaloriesKcal()

  var currentYearMonth by remember { mutableStateOf(SimpleYearMonth.now()) }
  var selectedDateResults by remember { mutableStateOf<List<RecordingResult>?>(null) }
  var selectedDateStr by remember { mutableStateOf("") }
  var activeResultDialog by remember { mutableStateOf<RecordingResult?>(null) }

  val daysInMonth = currentYearMonth.daysInMonth()
  val firstDayOfWeek = currentYearMonth.firstDayOfWeek()
  val todayStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date()) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 月切替ヘッダー
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = { currentYearMonth = currentYearMonth.minusMonths() }) {
        Text("◀ 前月")
      }
      Text(
        text = currentYearMonth.formatJapanese(),
        style = MaterialTheme.typography.titleMedium
      )
      TextButton(onClick = { currentYearMonth = currentYearMonth.plusMonths() }) {
        Text("翌月 ▶")
      }
    }

    // 曜日ヘッダー
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      listOf("日", "月", "火", "水", "木", "金", "土").forEachIndexed { idx, day ->
        Text(
          text = day,
          style = MaterialTheme.typography.labelMedium,
          color = when (idx) {
            0 -> Color.Red
            6 -> Color.Blue
            else -> MaterialTheme.colorScheme.onSurface
          }
        )
      }
    }

    // カレンダーグリッド
    val totalCells = firstDayOfWeek + daysInMonth
    val gridItems = (1..totalCells).toList()

    LazyVerticalGrid(
      columns = GridCells.Fixed(7),
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      items(gridItems) { index ->
        if (index <= firstDayOfWeek) {
          Box(modifier = Modifier.aspectRatio(1f))
        } else {
          val dayNum = index - firstDayOfWeek
          val dateStr = String.format(Locale.US, "%s/%02d", currentYearMonth.format(), dayNum)

          val resultsForDay = remember(dateStr) {
            ResultRepository.getResultsForDate(context, dateStr)
          }
          val totalCalForDay = resultsForDay.sumOf { it.caloriesKcal.toDouble() }.toInt()
          val isToday = dateStr == todayStr
          val isAchieved = targetCal > 0 && totalCalForDay >= targetCal

          Card(
            modifier = Modifier
              .aspectRatio(1f)
              .clickable {
                if (resultsForDay.isNotEmpty()) {
                  selectedDateStr = dateStr
                  selectedDateResults = resultsForDay
                }
              },
            colors = CardDefaults.cardColors(
              containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "$dayNum",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isAchieved) {
                  Text("✔", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
              }

              if (totalCalForDay > 0) {
                Text(
                  text = "${totalCalForDay}k",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary
                )
                val emojis = resultsForDay.map { it.sportType.emoji }.distinct().joinToString("")
                Text(
                  text = emojis,
                  style = MaterialTheme.typography.labelSmall
                )
              } else {
                Spacer(modifier = Modifier.height(1.dp))
              }
            }
          }
        }
      }
    }
  }

  // 日付選択時のアクティビティリストダイアログ
  selectedDateResults?.let { list ->
    AlertDialog(
      onDismissRequest = { selectedDateResults = null },
      title = { Text("📅 $selectedDateStr のアクティビティ") },
      text = {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
        ) {
          items(list) { item ->
            Card(
              onClick = {
                activeResultDialog = item
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("${item.sportType.emoji} ${item.title}", style = MaterialTheme.typography.titleSmall)
                  Text("時間: ${item.formattedDuration()} | 距離: ${item.formattedDistance()}", style = MaterialTheme.typography.bodySmall)
                }
                Text("${item.caloriesKcal.toInt()} kcal", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { selectedDateResults = null }) {
          Text("閉じる")
        }
      }
    )
  }

  // リザルト詳細ダイアログ
  activeResultDialog?.let { result ->
    ResultDialog(
      result = result,
      onDismiss = { activeResultDialog = null },
      onExportGpx = { exportGpxFile(context, result) }
    )
  }
}

private fun exportGpxFile(context: android.content.Context, result: RecordingResult) {
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
