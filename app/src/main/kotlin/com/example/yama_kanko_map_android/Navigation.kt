package com.example.yama_kanko_map_android

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.yama_kanko_map_android.ui.history.HistoryScreen
import com.example.yama_kanko_map_android.ui.main.MainScreen
import com.example.yama_kanko_map_android.ui.profile.ProfileScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) })
        }
        entry<Profile> {
          ProfileScreen(onBackClick = { backStack.removeLastOrNull() })
        }
        entry<History> {
          HistoryScreen(
            onBackClick = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
