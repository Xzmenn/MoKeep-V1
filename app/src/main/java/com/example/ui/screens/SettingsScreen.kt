package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.ThemeMode
import com.example.domain.ImportPreviewResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserEntity,
    setting: SettingEntity,
    importPreview: ImportPreviewResult?,
    onUpdateProfile: (name: String) -> Unit,
    onUploadAvatar: (Uri) -> Unit,
    onLogout: () -> Unit,
    onUpdateSettings: (currency: String, locale: String, theme: String, weekStartsOn: Int, threshold: Int) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onExportPdf: (Uri) -> Unit,
    onExportTemplateCsv: (Uri) -> Unit,
    onExportBackupJson: (Uri) -> Unit,
    onPreviewCsvUri: (Uri) -> Unit,
    onCommitCsvImport: () -> Unit,
    onClearImportPreview: () -> Unit,
    onRestoreBackupUri: (Uri, onDone: () -> Unit) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            listOf("Profil", "Preferensi", "Data", "Tentang").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("settings_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> ProfileTabContent(
                user = user,
                onUpdateProfile = onUpdateProfile,
                onUploadAvatar = onUploadAvatar,
                onLogout = onLogout
            )
            1 -> PreferencesTabContent(
                setting = setting,
                onSave = onUpdateSettings
            )
            2 -> DataTabContent(
                importPreview = importPreview,
                onExportCsv = onExportCsv,
                onExportPdf = onExportPdf,
                onExportTemplateCsv = onExportTemplateCsv,
                onExportBackupJson = onExportBackupJson,
                onPreviewCsvUri = onPreviewCsvUri,
                onCommitCsvImport = onCommitCsvImport,
                onClearImportPreview = onClearImportPreview,
                onRestoreBackupUri = onRestoreBackupUri
            )
            3 -> AboutTabContent()
        }
    }
}

@Composable
private fun ProfileTabContent(
    user: UserEntity,
    onUpdateProfile: (String) -> Unit,
    onUploadAvatar: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(user.name) { mutableStateOf(user.name) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadAvatar(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasAvatar = !user.imageUri.isNullOrBlank() && (
                            user.imageUri.startsWith("http") ||
                                File(user.imageUri.removePrefix("file://")).exists()
                            )
                        if (hasAvatar) {
                            AsyncImage(
                                model = user.imageUri,
                                contentDescription = "Foto profil ${user.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_avatar_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Foto Profil")
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama tampilan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Email Akun Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateProfile(name) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                    ) {
                        Text("Simpan Profil")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesTabContent(
    setting: SettingEntity,
    onSave: (currency: String, locale: String, theme: String, weekStartsOn: Int, threshold: Int) -> Unit
) {
    var currency by remember(setting.currency) { mutableStateOf(setting.currency) }
    var locale by remember(setting.locale) { mutableStateOf(setting.locale) }
    var theme by remember(setting.theme) { mutableStateOf(setting.theme) }
    var weekStartsOn by remember(setting.weekStartsOn) { mutableIntStateOf(setting.weekStartsOn) }
    var thresholdStr by remember(setting.budgetAlertThreshold) { mutableStateOf(setting.budgetAlertThreshold.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mata uang", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IDR", "USD", "SGD", "EUR", "MYR").forEach { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code },
                                label = { Text(code) }
                            )
                        }
                    }

                    Text("Format bahasa & angka", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("id-ID" to "Indonesia", "en-US" to "English (US)").forEach { (tag, label) ->
                            FilterChip(
                                selected = locale == tag,
                                onClick = { locale = tag },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Awal pekan kalender", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Senin", 0 to "Minggu").forEach { (day, label) ->
                            FilterChip(
                                selected = weekStartsOn == day,
                                onClick = { weekStartsOn = day },
                                label = { Text(label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it.filter(Char::isDigit) },
                        label = { Text("Ambang peringatan anggaran (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tema tampilan", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = theme == mode.key,
                                onClick = { theme = mode.key },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_chip_${mode.key}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val th = (thresholdStr.toIntOrNull() ?: 80).coerceIn(10, 100)
                            onSave(currency, locale, theme, weekStartsOn, th)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_preferences_button")
                    ) {
                        Text("Simpan Preferensi")
                    }
                }
            }
        }
    }
}

@Composable
private fun DataTabContent(
    importPreview: ImportPreviewResult?,
    onExportCsv: (Uri) -> Unit,
    onExportPdf: (Uri) -> Unit,
    onExportTemplateCsv: (Uri) -> Unit,
    onExportBackupJson: (Uri) -> Unit,
    onPreviewCsvUri: (Uri) -> Unit,
    onCommitCsvImport: () -> Unit,
    onClearImportPreview: () -> Unit,
    onRestoreBackupUri: (Uri, () -> Unit) -> Unit
) {
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreConfirmText by remember { mutableStateOf("") }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) onExportCsv(it)
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        if (it != null) onExportPdf(it)
    }
    val templateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) onExportTemplateCsv(it)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) onExportBackupJson(it)
    }
    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) onPreviewCsvUri(it)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            restoreConfirmText = ""
            pendingRestoreUri = it
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ekspor & Cadangan", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { csvExportLauncher.launch("transaksi-mokeep.csv") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor Transaksi (CSV)")
                    }
                    OutlinedButton(
                        onClick = { pdfExportLauncher.launch("laporan-mokeep.pdf") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor Laporan (PDF)")
                    }
                    OutlinedButton(
                        onClick = { backupLauncher.launch("cadangan-mokeep.json") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unduh Cadangan Lengkap (JSON)")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Impor CSV Transaksi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Unduh templat CSV lalu pilih file untuk pratinjau validasi sebelum disimpan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { templateLauncher.launch("templat-impor-mokeep.csv") },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Templat CSV")
                        }
                        Button(
                            onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih CSV")
                        }
                    }

                    if (importPreview != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Total: ${importPreview.total} · Valid: ${importPreview.validRows.size} · Gagal: ${importPreview.failed}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                importPreview.errors.take(5).forEach { err ->
                                    Text(
                                        "Baris ${err.row}: ${err.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onClearImportPreview) { Text("Tutup") }
                                    if (importPreview.validRows.isNotEmpty() && importPreview.importedCount == null) {
                                        Button(onClick = onCommitCsvImport) {
                                            Text("Impor ${importPreview.validRows.size} Baris")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pulihkan Cadangan JSON", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pemulihan akan menggantikan seluruh data akun, transaksi, anggaran, dan target saat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih File Cadangan JSON")
                    }
                }
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Konfirmasi Pemulihan Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ketik RESTORE untuk menimpa seluruh data saat ini dengan file cadangan.")
                    OutlinedTextField(
                        value = restoreConfirmText,
                        onValueChange = { restoreConfirmText = it },
                        label = { Text("Ketik RESTORE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = restoreConfirmText.trim() == "RESTORE",
                    onClick = {
                        onRestoreBackupUri(uri) { pendingRestoreUri = null }
                    }
                ) {
                    Text("Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun AboutTabContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MoKeep Android · v1.0.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Keuangan lebih teratur, hidup lebih tenang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Seluruh pencatatan akun, transaksi, transaksi berulang, anggaran bulanan, target tabungan, laporan arus kas, dan cadangan JSON tersimpan secara lokal di perangkat Anda menggunakan Room Database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
