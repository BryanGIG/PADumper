package com.dumper.android.ui.console

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dumper.android.R

@Composable
fun ConsoleScreen(navController: NavController, viewModel: ConsoleViewModel = viewModel()) {
    val console by viewModel.console.observeAsState("")
    val eventCode by viewModel.finishCode.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(eventCode) {
        eventCode?.getContentIfNotHandled()?.let {
            if (it == 0) {
                viewModel.appendSuccess("Dump success!")
            } else {
                viewModel.appendError("Dump error!")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .horizontalScroll(scrollState)
                .padding(10.dp)
        ) {
            Text(
                text = console,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(8.dp)
            )
        }

        FloatingActionButton(
            onClick = { viewModel.copyConsole(context) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            elevation = FloatingActionButtonDefaults.elevation(5.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_console))
        }
    }
}
@Preview
@Composable
private fun ConsoleScreenPreview() {
    ConsoleScreen(navController = NavController(LocalContext.current))
}