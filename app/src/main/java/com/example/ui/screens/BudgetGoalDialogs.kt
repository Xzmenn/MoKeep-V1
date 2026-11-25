package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.CategoryType
import com.example.domain.BudgetWithStatus
import com.example.domain.CategoryWithSubs
import com.example.domain.DateFormatters
import com.example.domain.GoalWithProgress
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.IconPickerGrid
import com.example.ui.components.MoneyInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    editing: BudgetWithStatus?,
    categories: List<CategoryWithSubs>,
    defaultThreshold: Int,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, amount: Double, threshold: Int, note: String?) -> Unit
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.category.type == CategoryType.EXPENSE }
    }
    var categoryId by remember {
        mutableStateOf(editing?.budget?.categoryId ?: expenseCategories.firstOrNull()?.category?.id.orEmpty())
    }
    var amountDigits by remember { mutableStateOf(editing?.budget?.amount?.toLong()?.toString() ?: "") }
    var thresholdText by remember { mutableStateOf((editing?.budget?.alertThreshold ?: defaultThreshold).toString()) }
    var note by remember { mutableStateOf(editing?.budget?.note ?: "") }
    var catExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedCat = expenseCategories.firstOrNull { it.category.id == categoryId }?.category

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Ubah Anggaran" else "Tambah Anggaran") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = catExpanded && editing == null,
                    onExpandedChange = { if (editing == null) catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCat?.name ?: "Pilih kategori pengeluaran",
                        onValueChange = {},
                        readOnly = true,
                        enabled = editing == null,
                        label = { Text("Kategori") },
                        trailingIcon = {
                            if (editing == null) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded && editing == null,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        expenseCategories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.category.name) },
                                onClick = {
                                    categoryId = item.category.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                MoneyInputField(
                    label = "Batas anggaran",
                    rawDigits = amountDigits,
                    onRawDigitsChange = { amountDigits = it; errorMsg = null },
                    currency = currency,
                    testTag = "budget_amount_input"
                )

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it.filter(Char::isDigit) },
                    label = { Text("Ambang peringatan (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    val amt = amountDigits.toDoubleOrNull() ?: 0.0
                    if (categoryId.isBlank()) {
                        errorMsg = "Kategori wajib dipilih"
                        return@Button
                    }
                    if (amt <= 0) {
                        errorMsg = "Batas anggaran harus lebih dari nol"
                        return@Button
                    }
                    val th = (thresholdText.toIntOrNull() ?: defaultThreshold).coerceIn(10, 100)
                    onSave(categoryId, amt, th, note.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormDialog(
    editing: GoalWithProgress?,
    accounts: List<AccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        targetAmount: Double,
        deadlineMillis: Long?,
        accountId: String?,
        color: String,
        icon: String,
        note: String?
    ) -> Unit
) {
    val activeAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    var name by remember { mutableStateOf(editing?.goal?.name ?: "") }
    var targetDigits by remember { mutableStateOf(editing?.goal?.targetAmount?.toLong()?.toString() ?: "") }
    var deadlineMillis by remember { mutableStateOf(editing?.goal?.deadline) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf(editing?.goal?.accountId) }
    var accExpanded by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(editing?.goal?.color ?: "#22c55e") }
    var icon by remember { mutableStateOf(editing?.goal?.icon ?: "target") }
    var note by remember { mutableStateOf(editing?.goal?.note ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedAccount = activeAccounts.firstOrNull { it.id == accountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Ubah Target Keuangan" else "Tambah Target Keuangan") },
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
                    label = { Text("Nama target") },
                    placeholder = { Text("Misal: Dana darurat, Liburan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                )
                MoneyInputField(
                    label = "Target nominal",
                    rawDigits = targetDigits,
                    onRawDigitsChange = { targetDigits = it; errorMsg = null },
                    currency = currency,
                    testTag = "goal_target_input"
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(deadlineMillis?.let { "Tenggat: ${DateFormatters.formatDate(it)}" } ?: "Tenggat waktu (opsional)")
                }
                ExposedDropdownMenuBox(
                    expanded = accExpanded,
                    onExpandedChange = { accExpanded = !accExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Tanpa akun tertaut",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Akun terkait (opsional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accExpanded,
                        onDismissRequest = { accExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tanpa akun tertaut") },
                            onClick = { accountId = null; accExpanded = false }
                        )
                        activeAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = { accountId = acc.id; accExpanded = false }
                            )
                        }
                    }
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
                    val target = targetDigits.toDoubleOrNull() ?: 0.0
                    if (trimmed.isEmpty()) {
                        errorMsg = "Nama target wajib diisi"
                        return@Button
                    }
                    if (target <= 0) {
                        errorMsg = "Target nominal harus lebih dari nol"
                        return@Button
                    }
                    onSave(trimmed, target, deadlineMillis, accountId, color, icon, note.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMillis = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { deadlineMillis = null; showDatePicker = false }) { Text("Hapus") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalContributionDialog(
    goalName: String,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (isWithdrawal: Boolean, amount: Double, dateMillis: Long, note: String?) -> Unit
) {
    var isWithdrawal by remember { mutableStateOf(false) }
    var amountDigits by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Kontribusi · $goalName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isWithdrawal,
                        onClick = { isWithdrawal = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Setoran") }
                    SegmentedButton(
                        selected = isWithdrawal,
                        onClick = { isWithdrawal = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Penarikan") }
                }

                MoneyInputField(
                    label = "Jumlah",
                    rawDigits = amountDigits,
                    onRawDigitsChange = { amountDigits = it; errorMsg = null },
                    currency = currency,
                    testTag = "goal_contribution_amount"
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tanggal: ${DateFormatters.formatDate(dateMillis)}")
                }

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
                    val amt = amountDigits.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) {
                        errorMsg = "Jumlah harus lebih dari nol"
                        return@Button
                    }
                    onSave(isWithdrawal, amt, dateMillis, note.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.testTag("save_contribution_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
