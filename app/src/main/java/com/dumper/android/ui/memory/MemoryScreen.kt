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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.dumper.android.R

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
            shape = RoundedCornerShape(4),
            title = { Text(stringResource(R.string.select_process)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(items = processList) { idx, process ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape,
                            onClick = {
                                selectedProcessIndex = idx
                                viewModel.changePackageName(processList!![selectedProcessIndex].processName)
                                viewModel.closeProcessListDialog()
                            }) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = process.getDisplayName(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.closeProcessListDialog() }) {
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

        LabelledCheckBox(
            modifier = Modifier.fillMaxWidth(),
            checked = autoFix,
            onCheckedChange = { viewModel.changeFixELF(it) },
            label = stringResource(R.string.fix_elf_result))

        LabelledCheckBox(
            checked = metadata,
            onCheckedChange = { viewModel.changeDumpMetadata(it) },
            label =stringResource(R.string.dump_global_metadata_dat))


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
fun LabelledCheckBox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    label: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(!checked) }
        )

        Text(
            text = label,
        )
    }
}

@Preview
@Composable
private fun MemoryScreenPreview() {
    MemoryScreen(NavController(LocalContext.current), viewModel())
}