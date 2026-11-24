package id.kalen3r.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import id.kalen3r.alarm.AlarmScheduler
import id.kalen3r.alarm.ReminderOutcome
import id.kalen3r.ui.alarm.AlarmScreen
import id.kalen3r.ui.alarm.BatteryHelpScreen
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.kalen3r.R
import id.kalen3r.ui.cari.CariScreen
import id.kalen3r.ui.detail.DetailScreen
import id.kalen3r.ui.month.MonthScreen

data class TabSpec(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

/** Route constants and the bottom-bar tab list (plain Kotlin, JVM-testable). */
object AppRoutes {
    const val KALENDER = "kalender"
    const val DETAIL = "detail/{date}"
    const val CARI = "cari"
    const val ALARM = "alarm"
    const val ALARM_HELP = "alarm/bantuan"
    const val DONASI = "donasi"

    fun detail(date: String) = "detail/$date"

    val tabs: List<TabSpec> = listOf(
        TabSpec(KALENDER, R.string.tab_kalender, Icons.Filled.CalendarMonth),
        TabSpec(CARI, R.string.tab_cari, Icons.Filled.Search),
        TabSpec(ALARM, R.string.tab_alarm, Icons.Filled.Alarm),
        TabSpec(DONASI, R.string.tab_donasi, Icons.Filled.Favorite),
    )
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val onTab = AppRoutes.tabs.any { it.route == current }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    AppRoutes.tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = AppRoutes.KALENDER, modifier = Modifier.padding(padding)) {
            composable(AppRoutes.KALENDER) { MonthScreen { nav.navigate(AppRoutes.detail(it.toString())) } }
            composable(
                AppRoutes.DETAIL,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { entry ->
                DetailScreen(
                    rawDate = entry.arguments?.getString("date"),
                    onBack = { nav.popBackStack() },
                    onRemind = { date ->
                        scope.launch {
                            val outcome = AlarmScheduler.addReminder(context, date)
                            snackbar.showSnackbar(
                                context.getString(
                                    when (outcome) {
                                        ReminderOutcome.ADDED -> R.string.reminder_added
                                        ReminderOutcome.PAST -> R.string.reminder_past
                                        ReminderOutcome.FAILED -> R.string.reminder_failed
                                    },
                                ),
                            )
                        }
                    },
                )
            }
            composable(AppRoutes.CARI) { CariScreen { nav.navigate(AppRoutes.detail(it.toString())) } }
            composable(AppRoutes.ALARM) { AlarmScreen(onOpenHelp = { nav.navigate(AppRoutes.ALARM_HELP) }) }
            composable(AppRoutes.ALARM_HELP) { BatteryHelpScreen(onBack = { nav.popBackStack() }) }
            composable(AppRoutes.DONASI) { Placeholder(R.string.placeholder_donasi) }
        }
    }
}

@Composable
private fun Placeholder(@StringRes title: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(title))
    }
}
