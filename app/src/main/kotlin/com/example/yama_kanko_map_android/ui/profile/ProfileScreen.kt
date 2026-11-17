package com.example.yama_kanko_map_android.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.yama_kanko_map_android.data.ProfileRepository
import com.example.yama_kanko_map_android.data.UserProfile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val profile = remember { ProfileRepository.loadProfile(context) }

  var heightText by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
  var weightText by remember { mutableStateOf(profile.weightKg.toInt().toString()) }
  var targetWeightText by remember { mutableStateOf(profile.targetWeightKg.toInt().toString()) }

  BackHandler {
    onBackClick()
  }

  val currentWeight = weightText.toFloatOrNull() ?: profile.weightKg
  val targetWeight = targetWeightText.toFloatOrNull() ?: profile.targetWeightKg
  val weightDiff = (currentWeight - targetWeight).coerceAtLeast(0f)
  val dailyTargetCal = (weightDiff * 240f).toInt()

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("👤 プロフィール設定") },
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Card(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "身体情報の設定",
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "設定された体重と身長は、歩行やランニング時の消費カロリー計算に使用されます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text("身長 (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("現在の体重 (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = targetWeightText,
            onValueChange = { targetWeightText = it },
            label = { Text("1か月後の目標体重 (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // 1日の目標消費カロリーカード
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "🎯 1か月の運動目標",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
          HorizontalDivider()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "1か月の減量目標:", style = MaterialTheme.typography.bodyMedium)
            Text(
              text = String.format(Locale.US, "-%.1f kg", weightDiff),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "1日の目標消費カロリー:", style = MaterialTheme.typography.bodyMedium)
            Text(
              text = "$dailyTargetCal kcal / 日",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Text(
            text = "※ 体脂肪1kg当たり約7,200kcal換算。1か月(30日)で達成するための1日の必要運動カロリーです。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        onClick = {
          val h = heightText.toFloatOrNull() ?: 170f
          val w = weightText.toFloatOrNull() ?: 65f
          val tw = targetWeightText.toFloatOrNull() ?: 63f
          ProfileRepository.saveProfile(context, UserProfile(heightCm = h, weightKg = w, targetWeightKg = tw))
          Toast.makeText(context, "プロフィールを保存しました。", Toast.LENGTH_SHORT).show()
          onBackClick()
        }
      ) {
        Text("保存する")
      }
    }
  }
}
