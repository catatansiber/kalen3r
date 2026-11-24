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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    const val DONASI = "donasi"

    fun detail(date: String) = "detail/$date"

    val tabs: List<TabSpec> = listOf(
        TabSpec(KALENDER, R.string.tab_kalender, Icons.Filled.CalendarMonth),
        TabSpec(CARI, R.string.tab_cari, Icons.Filled.Search),
        TabSpec(ALARM, R.string.tab_alarm, Icons.Filled.Alarm),
        TabSpec(DONASI, R.string.tab_donasi, Icons.Filled.Favorite),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val onTab = AppRoutes.tabs.any { it.route == current }

    Scaffold(
        topBar = {
            if (current == AppRoutes.DETAIL) {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_detail)) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            }
        },
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
            composable(AppRoutes.KALENDER) { Placeholder(R.string.placeholder_kalender) }
            composable(
                AppRoutes.DETAIL,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { Placeholder(R.string.title_detail) }
            composable(AppRoutes.CARI) { Placeholder(R.string.placeholder_cari) }
            composable(AppRoutes.ALARM) { Placeholder(R.string.placeholder_alarm) }
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
