package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.IconPickerGrid
import com.example.ui.components.MoneyInputField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFormDialog(
    editing: AccountEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        initialBalance: Double,
        creditLimit: Double?,
        color: String,
        icon: String,
        note: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: AccountType.BANK) }
    var initialDigits by remember { mutableStateOf(editing?.initialBalance?.toLong()?.toString() ?: "0") }
    var creditLimitDigits by remember { mutableStateOf(editing?.creditLimit?.toLong()?.toString() ?: "") }
    var color by remember { mutableStateOf(editing?.color ?: "#0f766e") }
    var icon by remember { mutableStateOf(editing?.icon ?: "landmark") }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Ubah Akun" else "Tambah Akun") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama akun") },
                    placeholder = { Text("Misal: BCA, GoPay, Dompet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )
                Text("Jenis akun", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.entries.forEach { accType ->
                        FilterChip(
                            selected = type == accType,
                            onClick = { type = accType },
                            label = { Text(accType.label) }
                        )
                    }
                }
                MoneyInputField(
                    label = "Saldo awal",
                    rawDigits = initialDigits,
                    onRawDigitsChange = { initialDigits = it },
                    currency = currency,
                    testTag = "account_initial_balance_input"
                )
                if (type == AccountType.CREDIT_CARD) {
                    MoneyInputField(
                        label = "Limit kredit",
                        rawDigits = creditLimitDigits,
                        onRawDigitsChange = { creditLimitDigits = it },
                        currency = currency,
                        testTag = "account_credit_limit_input"
                    )
                }
                ColorPickerRow(selectedColor = color, onSelect = { color = it })
                IconPickerGrid(selectedIcon = icon, accentColor = color, onSelect = { icon = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMsg = "Nama akun wajib diisi"
                        return@Button
                    }
                    val initBal = initialDigits.toDoubleOrNull() ?: 0.0
                    val limit = if (type == AccountType.CREDIT_CARD) creditLimitDigits.toDoubleOrNull() else null
                    onSave(trimmed, type, initBal, limit, color, icon, note.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    editing: CategoryEntity?,
    initialType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: CategoryType, icon: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: initialType) }
    var color by remember { mutableStateOf(editing?.color ?: "#0f766e") }
    var icon by remember { mutableStateOf(editing?.icon ?: "utensils") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Ubah Kategori" else "Tambah Kategori") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )
                Text("Jenis kategori", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryType.entries.forEach { catType ->
                        FilterChip(
                            selected = type == catType,
                            onClick = { type = catType },
                            label = { Text(catType.label) }
                        )
                    }
                }
                ColorPickerRow(selectedColor = color, onSelect = { color = it })
                IconPickerGrid(selectedIcon = icon, accentColor = color, onSelect = { icon = it })
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMsg = "Nama kategori wajib diisi"
                        return@Button
                    }
                    onSave(trimmed, type, icon, color)
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun SubCategoryFormDialog(
    categoryName: String,
    editing: SubCategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Ubah Subkategori" else "Tambah Subkategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Kategori: $categoryName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama subkategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subcategory_name_input")
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMsg = "Nama subkategori wajib diisi"
                        return@Button
                    }
                    onSave(trimmed)
                },
                modifier = Modifier.testTag("save_subcategory_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
