package com.ayush.netpulse.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.util.UsageBackup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun UsageBackupCard(hasPermission: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<List<DailyUsageEntity>?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try { status = "Saved ${UsageBackup.write(context, uri)} daily records." }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { status = "Backup failed." }
                finally { busy = false }
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try { preview = UsageBackup.read(context, uri) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { status = "Invalid, unsupported or oversized backup." }
                finally { busy = false }
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Daily usage backup", style = MaterialTheme.typography.titleMedium)
            Text("Back up all saved daily usage records. Restore merges records without deleting existing usage. Does not include settings, speed logs or permissions.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy && hasPermission, onClick = {
                save.launch("netpulse-backup-${LocalDate.now()}.json")
            }) { Text("Create backup") }
            OutlinedButton(enabled = !busy && hasPermission, onClick = {
                open.launch(arrayOf("application/json", "text/plain"))
            }) { Text("Restore backup") }
            if (!hasPermission) Text("Grant Usage Access to manage usage backups.")
            if (status.isNotBlank()) Text(status)
        }
    }
    preview?.let { rows ->
        AlertDialog(onDismissRequest = { preview = null }, title = { Text("Merge usage backup?") },
            text = { Text("${rows.size} daily records. Existing counters will not be reduced. Only restore archives from your own device to avoid mixing usage.") },
            confirmButton = { TextButton(onClick = {
                preview = null
                busy = true
                scope.launch {
                    try { status = "Merged ${UsageBackup.merge(context, rows)} records." }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { status = "Restore failed; transaction rolled back." }
                    finally { busy = false }
                }
            }) { Text("Merge") } }, dismissButton = { TextButton(onClick = { preview = null }) { Text("Cancel") } })
    }
}
