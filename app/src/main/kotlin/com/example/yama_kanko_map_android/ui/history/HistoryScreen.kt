package com.example.yama_kanko_map_android.ui.history

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.yama_kanko_map_android.data.RecordingResult
import com.example.yama_kanko_map_android.data.ResultRepository
import com.example.yama_kanko_map_android.ui.main.ResultDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var pastResults by remember { mutableStateOf(ResultRepository.loadResults(context)) }
  var itemToDelete by remember { mutableStateOf<RecordingResult?>(null) }
  var activeResultDialog by remember { mutableStateOf<RecordingResult?>(null) }

  BackHandler {
    onBackClick()
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("📜 アクティビティ履歴") },
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
        .padding(16.dp)
    ) {
      if (pastResults.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "まだ保存されたアクティビティ履歴がありません。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(pastResults, key = { it.id }) { item ->
            Card(
              onClick = { activeResultDialog = item },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${item.sportType.emoji} ${item.title}",
                    style = MaterialTheme.typography.titleMedium
                  )
                  Text(
                    text = item.dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                  ) {
                    Text(text = "距離: ${item.formattedDistance()}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "時間: ${item.formattedDuration()}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = item.formattedCalories(), style = MaterialTheme.typography.bodyMedium)
                  }
                }

                TextButton(
                  onClick = { itemToDelete = item }
                ) {
                  Text("削除", color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }

  // Result Dialog
  activeResultDialog?.let { result ->
    ResultDialog(
      result = result,
      onDismiss = { activeResultDialog = null },
      onExportGpx = { exportGpxFile(context, result) }
    )
  }

  // Delete Confirmation Dialog
  itemToDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = { Text("リザルトの削除") },
      text = { Text("「${target.title} (${target.dateStr})」を削除しますか？\n（保存されたGPXファイルも削除されます）") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          onClick = {
            ResultRepository.deleteResult(context, target.id)
            pastResults = ResultRepository.loadResults(context)
            itemToDelete = null
            Toast.makeText(context, "リザルト履歴を削除しました。", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("削除")
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToDelete = null }) {
          Text("キャンセル")
        }
      }
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
