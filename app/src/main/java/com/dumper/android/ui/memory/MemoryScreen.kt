package com.dumper.android.ui.memory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.dumper.android.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun MemoryScreen(navController: NavController, viewModel: MemoryViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val processList by viewModel.processList.observeAsState()

    LaunchedEffect(processList) {
        if (processList?.isNotEmpty() == true) {
            val displayNames = processList!!.map { it.getDisplayName() }.toTypedArray()

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.select_process))
                .setSingleChoiceItems(displayNames, -1) { dialog, idx ->
                    viewModel.changePackageName(processList!![idx].processName)
                    dialog.dismiss()
                }
                .create()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(6.dp)
    ) {
        val processText by viewModel.packageName.collectAsState()
        val libName by viewModel.libName.collectAsState()
        val autoFix by viewModel.isFixELF.collectAsState()
        val metadata by viewModel.isDumpMetadata.collectAsState()

        OutlinedTextField(
            value = processText,
            onValueChange = { viewModel.changePackageName(it) },
            label = { Text(stringResource(R.string.process_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.getProcessList(context) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.select_process))
        }

        OutlinedTextField(
            value = libName,
            onValueChange = { viewModel.changeLibName(it) },
            label = { Text(stringResource(R.string.elf_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = autoFix,
                onCheckedChange = { viewModel.changeFixELF(it) },
            )
            Text(stringResource(R.string.fix_elf_result))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = metadata,
                onCheckedChange = { viewModel.changeDumpMetadata(it) },
            )
            Text(stringResource(R.string.dump_global_metadata_dat))
        }

        Button(
            onClick = {
                navController.navigate("console") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                viewModel.beginDump(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Text(stringResource(R.string.dump))
        }
    }
}

@Preview
@Composable
private fun MemoryScreenPreview() {
    MemoryScreen(navController = NavController(LocalContext.current))
}