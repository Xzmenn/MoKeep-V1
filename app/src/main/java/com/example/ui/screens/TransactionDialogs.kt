package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TagEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import com.example.domain.CategoryWithSubs
import com.example.domain.CurrencyFormatter
import com.example.domain.DateFormatters
import com.example.domain.RecurringRuleDetail
import com.example.domain.TransactionDetail
import com.example.ui.components.MoneyInputField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    editing: TransactionDetail?,
    presetType: TransactionType = TransactionType.EXPENSE,
    presetAccountId: String? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryWithSubs>,
    tagSuggestions: List<TagEntity>,
    currency: String,
    locale: String,
    onDismiss: () -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        dateMillis: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        subCategoryId: String?,
        note: String?,
        tags: List<String>,
        attachmentUri: Uri?,
        repeat: Boolean,
        frequency: RecurrenceFrequency,
        interval: Int,
        endDateMillis: Long?
    ) -> Unit
) {
    val activeAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    var type by remember { mutableStateOf(editing?.transaction?.type ?: presetType) }
    var amountDigits by remember {
        mutableStateOf(editing?.transaction?.amount?.toLong()?.toString() ?: "")
    }
    var dateMillis by remember { mutableStateOf(editing?.transaction?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var accountId by remember {
        mutableStateOf(editing?.transaction?.accountId ?: presetAccountId ?: activeAccounts.firstOrNull()?.id ?: "")
    }
    var toAccountId by remember { mutableStateOf(editing?.transaction?.toAccountId ?: "") }
    var categoryId by remember { mutableStateOf(editing?.transaction?.categoryId ?: "") }
    var subCategoryId by remember { mutableStateOf(editing?.transaction?.subCategoryId ?: "") }
    var note by remember { mutableStateOf(editing?.transaction?.note ?: "") }
    var selectedTags by remember {
        mutableStateOf(editing?.tags?.map { it.name }.orEmpty())
    }
    var tagInput by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    var repeat by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var intervalStr by remember { mutableStateOf("1") }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val availableCategories = remember(categories, type) {
        categories.filter { it.category.type.name == type.name }
    }
    val selectedCategory = remember(availableCategories, categoryId) {
        availableCategories.firstOrNull { it.category.id == categoryId }
    }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) attachmentUri = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing != null) "Ubah Transaksi" else "Tambah Transaksi",
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { idx, txType ->
                    SegmentedButton(
                        selected = type == txType,
                        onClick = {
                            if (type != txType) {
                                type = txType
                                categoryId = ""
                                subCategoryId = ""
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size),
                        modifier = Modifier.testTag("tx_type_${txType.name.lowercase()}")
                    ) {
                        Text(txType.label, maxLines = 1)
                    }
                }
            }

            MoneyInputField(
                label = "Jumlah",
                rawDigits = amountDigits,
                onRawDigitsChange = { amountDigits = it; validationError = null },
                currency = currency,
                testTag = "tx_amount_input"
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tanggal: ${DateFormatters.formatDate(dateMillis)}")
            }

            // Source Account Dropdown
            AccountDropdownField(
                label = if (type == TransactionType.INCOME) "Akun tujuan" else "Akun asal",
                selectedId = accountId,
                accounts = activeAccounts,
                currency = currency,
                locale = locale,
                onSelect = { accountId = it }
            )

            if (type == TransactionType.TRANSFER) {
                AccountDropdownField(
                    label = "Akun tujuan transfer",
                    selectedId = toAccountId,
                    accounts = activeAccounts.filter { it.id != accountId },
                    currency = currency,
                    locale = locale,
                    onSelect = { toAccountId = it }
                )
            } else {
                // Category Dropdown
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.category?.name ?: "Pilih kategori",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("tx_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.category.name) },
                                onClick = {
                                    categoryId = cat.category.id
                                    subCategoryId = ""
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCategory != null && selectedCategory.subCategories.isNotEmpty()) {
                    var subExpanded by remember { mutableStateOf(false) }
                    val selectedSub = selectedCategory.subCategories.firstOrNull { it.id == subCategoryId }
                    ExposedDropdownMenuBox(
                        expanded = subExpanded,
                        onExpandedChange = { subExpanded = !subExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSub?.name ?: "Tanpa subkategori",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subkategori (opsional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = subExpanded,
                            onDismissRequest = { subExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tanpa subkategori") },
                                onClick = { subCategoryId = ""; subExpanded = false }
                            )
                            selectedCategory.subCategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = { subCategoryId = sub.id; subExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
            )

            // Tags Input & Suggestions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tambah tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val clean = tagInput.trim().lowercase()
                            if (clean.isNotEmpty() && !selectedTags.contains(clean)) {
                                selectedTags = selectedTags + clean
                            }
                            tagInput = ""
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Tambah")
                    }
                }
                if (selectedTags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { selectedTags = selectedTags - tag },
                                label = { Text("#$tag") },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Hapus tag") }
                            )
                        }
                    }
                }
                val unusedSuggestions = tagSuggestions.filter { !selectedTags.contains(it.name) }
                if (unusedSuggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        unusedSuggestions.take(6).forEach { s ->
                            FilterChip(
                                selected = false,
                                onClick = { selectedTags = selectedTags + s.name },
                                label = { Text("+ ${s.name}") }
                            )
                        }
                    }
                }
            }

            // Recurring toggle (only for new transactions)
            if (editing == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ulangi transaksi ini", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = repeat, onCheckedChange = { repeat = it })
                }
                if (repeat) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = intervalStr,
                            onValueChange = { intervalStr = it.filter(Char::isDigit) },
                            label = { Text("Setiap (interval)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(
                                endDateMillis?.let { DateFormatters.formatShortDate(it) } ?: "Tgl akhir (opsional)",
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Attachments
            OutlinedButton(
                onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf")) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (attachmentUri != null) "Lampiran baru siap disimpan" else "Lampirkan struk / PDF")
            }
            editing?.attachments?.forEach { att ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(att.fileName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteAttachment(att.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus lampiran", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (validationError != null) {
                Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amount = amountDigits.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        validationError = "Jumlah harus lebih dari nol"
                        return@Button
                    }
                    if (accountId.isBlank()) {
                        validationError = "Pilih akun terlebih dahulu"
                        return@Button
                    }
                    if (type == TransactionType.TRANSFER && (toAccountId.isBlank() || toAccountId == accountId)) {
                        validationError = "Akun tujuan transfer harus berbeda dari akun asal"
                        return@Button
                    }
                    if (type != TransactionType.TRANSFER && categoryId.isBlank()) {
                        validationError = "Kategori wajib dipilih"
                        return@Button
                    }
                    onSave(
                        type,
                        amount,
                        dateMillis,
                        accountId,
                        toAccountId.takeIf { it.isNotBlank() },
                        categoryId.takeIf { it.isNotBlank() },
                        subCategoryId.takeIf { it.isNotBlank() },
                        note,
                        selectedTags,
                        attachmentUri,
                        repeat,
                        frequency,
                        intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        endDateMillis
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_transaction_button")
            ) {
                Text("Simpan Transaksi")
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDateMillis = pickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { endDateMillis = null; showEndDatePicker = false }) { Text("Hapus") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownField(
    label: String,
    selectedId: String,
    accounts: List<AccountEntity>,
    currency: String,
    locale: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${CurrencyFormatter.format(it.balance, currency, locale)}" } ?: "Pilih akun",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.name} · ${CurrencyFormatter.format(acc.balance, currency, locale)}") },
                    onClick = {
                        onSelect(acc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringEditDialog(
    rule: RecurringRuleDetail,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, frequency: RecurrenceFrequency, interval: Int, note: String?, isActive: Boolean) -> Unit
) {
    var amountDigits by remember { mutableStateOf(rule.rule.amount.toLong().toString()) }
    var frequency by remember { mutableStateOf(rule.rule.frequency) }
    var intervalStr by remember { mutableStateOf(rule.rule.interval.toString()) }
    var note by remember { mutableStateOf(rule.rule.note.orEmpty()) }
    var isActive by remember { mutableStateOf(rule.rule.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Transaksi Berulang") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoneyInputField(
                    label = "Jumlah",
                    rawDigits = amountDigits,
                    onRawDigitsChange = { amountDigits = it },
                    currency = currency
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it.filter(Char::isDigit) },
                    label = { Text("Interval") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aturan aktif")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountDigits.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onSave(amt, frequency, intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 1, note, isActive)
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
