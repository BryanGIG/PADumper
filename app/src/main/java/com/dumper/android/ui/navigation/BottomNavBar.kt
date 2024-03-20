package com.dumper.android.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dumper.android.ui.Screen


@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Screen.Memory,
        Screen.Console,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    NavigationBar {
        items.forEach { screen ->
            val selected =
                navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = { Text(stringResource(screen.resourceId)) },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.iconDefault else screen.iconOutline,
                        contentDescription = stringResource(screen.resourceId)
                    )
                }
            )
        }
    }
}