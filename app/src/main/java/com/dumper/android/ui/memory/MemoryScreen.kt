package com.dumper.android.ui.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.dumper.android.dumper.process.ProcessData

@Composable
fun MemoryScreen(navController: NavController, viewModel: MemoryViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDialogProcessList by viewModel.isDialogProcessList.collectAsState()
    val processList by viewModel.processList.observeAsState(emptyArray())

    if (isDialogProcessList && processList.isNotEmpty()) {
        var selectedProcessIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.closeProcessListDialog() },
            title = { Text(stringResource(R.string.select_process)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(items = processList) { idx, process ->
                        ItemApp(selectedProcessIndex, idx, process) {
                            selectedProcessIndex = idx
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePackageName(processList!![selectedProcessIndex].processName)
                        viewModel.closeProcessListDialog()
                    }
                ) {
                    Text(stringResource(R.string.select))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.closeProcessListDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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

@Composable
private fun ItemApp(
    selectedProcessIndex: Int,
    idx: Int,
    process: ProcessData,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = idx == selectedProcessIndex,
            onCheckedChange = {}
        )

        Text(text = process.getDisplayName())
    }
}

@Preview
@Composable
private fun MemoryScreenPreview() {
    MemoryScreen(NavController(LocalContext.current), viewModel())
}