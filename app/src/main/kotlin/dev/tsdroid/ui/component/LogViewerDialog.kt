package dev.tsdroid.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.tsdroid.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    if (!show) return

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var deleteConfirm by remember { mutableStateOf<File?>(null) }
    // Force refresh when reopening
    val crashFiles = remember(show) { AppLogger.getCrashFiles() }
    val runtimeLog = remember(show) { AppLogger.getRuntimeLogFile() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column {
                TopAppBar(
                    title = {
                        Text(if (selectedFile != null) selectedFile!!.name else "Logs")
                    },
                    navigationIcon = {
                        if (selectedFile != null) {
                            IconButton(onClick = { selectedFile = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                )
                            }
                        }
                    },
                    actions = {
                        if (selectedFile != null) {
                            IconButton(onClick = { deleteConfirm = selectedFile }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                )

                if (selectedFile != null) {
                    // File content viewer
                    val content = remember(selectedFile) {
                        try { selectedFile!!.readText() } catch (_: Exception) { "(empty or unreadable)" }
                    }
                    Text(
                        text = content,
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    // File list
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (crashFiles.isEmpty() && runtimeLog == null) {
                            item {
                                Text(
                                    "No log files yet",
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Runtime log
                        if (runtimeLog != null) {
                            item {
                                LogFileRow(
                                    icon = Icons.Default.Description,
                                    name = "Runtime log (app.log)",
                                    detail = formatSize(runtimeLog!!.length()),
                                    file = runtimeLog,
                                    onClick = { selectedFile = runtimeLog },
                                )
                                HorizontalDivider()
                            }
                        }

                        // Crash files
                        itemsIndexed(crashFiles) { _, file ->
                            LogFileRow(
                                icon = Icons.Default.BugReport,
                                name = file.name,
                                detail = formatDate(file.lastModified()),
                                file = file,
                                onClick = { selectedFile = file },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    deleteConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete") },
            text = { Text("Delete ${file.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    try { file.delete() } catch (_: Exception) {}
                    deleteConfirm = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun LogFileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    detail: String,
    file: File,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDate(epoch: Long): String {
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epoch))
    } catch (_: Exception) {
        epoch.toString()
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
