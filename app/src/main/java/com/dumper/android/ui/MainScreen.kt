package com.dumper.android.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dumper.android.R
import com.dumper.android.ui.console.ConsoleScreen
import com.dumper.android.ui.console.ConsoleViewModel
import com.dumper.android.ui.memory.MemoryScreen
import com.dumper.android.ui.memory.MemoryViewModel
import com.dumper.android.ui.navigation.BottomNavBar
import com.dumper.android.utils.openGithubPage

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MainScreen(memory: MemoryViewModel = viewModel(), console: ConsoleViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        fontWeight = FontWeight.Bold,
                        text = stringResource(R.string.app_name)
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = { context.openGithubPage() }) {
                        Icon(
                            painter = painterResource(R.drawable.github_mark),
                            contentDescription = stringResource(R.string.github_repository)
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) {
        Surface(modifier = Modifier.padding(it)) {
            NavHost(navController = navController, startDestination = "memory") {
                composable("memory") { MemoryScreen(navController, memory) }
                composable("console") { ConsoleScreen(navController, console) }
            }
        }
    }
}
