package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.PeriodPreset
import com.example.data.model.TransactionType
import com.example.domain.CategoryWithSubs
import com.example.domain.CurrencyFormatter
import com.example.domain.DateFormatters
import com.example.domain.RecurringRuleDetail
import com.example.domain.TransactionDetail
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PeriodFilterRow
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import com.example.ui.viewmodel.TransactionFilterState
import com.example.ui.viewmodel.TransactionSortField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionDetail>,
    recurringRules: List<RecurringRuleDetail>,
    filter: TransactionFilterState,
    accounts: List<AccountEntity>,
    categories: List<CategoryWithSubs>,
    currency: String,
    locale: String,
    weekStartsOn: Int,
    onFilterChange: ((TransactionFilterState) -> TransactionFilterState) -> Unit,
    onSelectPeriod: (PeriodPreset, Int) -> Unit,
    onResetFilter: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionDetail) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onEditRecurring: (RecurringRuleDetail) -> Unit,
    onToggleRecurring: (String) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onRunRecurringNow: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionDetail?>(null) }

    val activeFilterCount = listOfNotNull(
        filter.period.takeIf { it != PeriodPreset.MONTH }?.name,
        filter.type?.name,
        filter.accountId,
        filter.categoryId,
        filter.minAmount?.toString(),
        filter.maxAmount?.toString()
    ).size

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Semua Transaksi") },
                modifier = Modifier.testTag("tab_all_transactions")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Transaksi Berulang") },
                modifier = Modifier.testTag("tab_recurring_transactions")
            )
        }

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filter.searchQuery,
                        onValueChange = { q -> onFilterChange { it.copy(searchQuery = q) } },
                        placeholder = { Text("Cari transaksi, akun, tag...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("tx_search_input")
                    )
                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.height(52.dp).testTag("tx_filter_button")
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter transaksi")
                        if (activeFilterCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }
                }
                PeriodFilterRow(
                    selected = filter.period,
                    onSelect = { onSelectPeriod(it, weekStartsOn) }
                )
            }

            if (transactions.isEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyStateView(
                        title = "Belum ada transaksi",
                        description = "Tambahkan transaksi untuk mulai melacak keuangan Anda.",
                        actionLabel = "Tambah Transaksi",
                        onAction = onAddTransaction
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.transaction.id }) { detail ->
                        TransactionItemCard(
                            detail = detail,
                            currency = currency,
                            locale = locale,
                            onEdit = { onEditTransaction(detail) },
                            onDelete = { pendingDeleteTx = detail }
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${recurringRules.size} aturan berulang",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        onClick = onRunRecurringNow,
                        modifier = Modifier.testTag("run_recurring_button")
                    ) {
                        Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proses Jatuh Tempo")
                    }
                }

                if (recurringRules.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EmptyStateView(
                            title = "Belum ada transaksi berulang",
                            description = "Aktifkan opsi pengulangan saat menambah transaksi baru.",
                            icon = Icons.Filled.Repeat
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recurringRules, key = { it.rule.id }) { item ->
                            RecurringRuleCard(
                                item = item,
                                currency = currency,
                                locale = locale,
                                onEdit = { onEditRecurring(item) },
                                onToggle = { onToggleRecurring(item.rule.id) },
                                onDelete = { onDeleteRecurring(item.rule.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        TransactionFilterBottomSheet(
            filter = filter,
            accounts = accounts,
            categories = categories,
            onFilterChange = onFilterChange,
            onReset = onResetFilter,
            onDismiss = { showFilterSheet = false }
        )
    }

    pendingDeleteTx?.let { detail ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTx = null },
            title = { Text("Hapus transaksi?") },
            text = { Text("Saldo akun terkait akan dihitung ulang secara otomatis.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteTransaction(detail.transaction.id)
                    pendingDeleteTx = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTx = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionItemCard(
    detail: TransactionDetail,
    currency: String,
    locale: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tx = detail.transaction
    val fintech = LocalFintechColors.current
    val amountColor = when (tx.type) {
        TransactionType.INCOME -> fintech.income
        TransactionType.EXPENSE -> fintech.expense
        TransactionType.TRANSFER -> fintech.transfer
    }
    val prefix = when (tx.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "−"
        TransactionType.TRANSFER -> ""
    }

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("tx_card_${tx.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadgeIcon(
                    iconName = detail.category?.icon ?: "arrow-left-right",
                    hexColor = detail.category?.color ?: "#6366f1",
                    contentDescription = detail.category?.name
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.note ?: detail.category?.name ?: tx.type.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = buildString {
                        append(DateFormatters.formatShortDate(tx.date))
                        append(" · ")
                        append(detail.account.name)
                        if (detail.toAccount != null) {
                            append(" → ${detail.toAccount.name}")
                        }
                        if (detail.subCategory != null) {
                            append(" · ${detail.subCategory.name}")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix${CurrencyFormatter.format(tx.amount, currency, locale)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Ubah", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (detail.tags.isNotEmpty() || detail.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text("#${tag.name}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (detail.attachments.isNotEmpty()) {
                        AssistChip(
                            onClick = onEdit,
                            leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("${detail.attachments.size} lampiran", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringRuleCard(
    item: RecurringRuleDetail,
    currency: String,
    locale: String,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val rule = item.rule
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.note ?: rule.type.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${CurrencyFormatter.format(rule.amount, currency, locale)} · Berikutnya ${DateFormatters.formatShortDate(rule.nextRunDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${item.account?.name ?: "-"} · ${rule.frequency.label} tiap ${rule.interval} periode" +
                            if (!rule.isActive) " · Dijeda" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Ubah aturan")
                    }
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (rule.isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (rule.isActive) "Jeda" else "Lanjutkan"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus aturan", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionFilterBottomSheet(
    filter: TransactionFilterState,
    accounts: List<AccountEntity>,
    categories: List<CategoryWithSubs>,
    onFilterChange: ((TransactionFilterState) -> TransactionFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var minText by remember { mutableStateOf(filter.minAmount?.toLong()?.toString() ?: "") }
    var maxText by remember { mutableStateOf(filter.maxAmount?.toLong()?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Filter & Urutkan", style = MaterialTheme.typography.headlineSmall)

            Text("Jenis transaksi", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.type == null,
                    onClick = { onFilterChange { it.copy(type = null) } },
                    label = { Text("Semua") }
                )
                TransactionType.entries.forEach { txType ->
                    FilterChip(
                        selected = filter.type == txType,
                        onClick = { onFilterChange { it.copy(type = txType) } },
                        label = { Text(txType.label) }
                    )
                }
            }

            Text("Akun", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.accountId == null,
                    onClick = { onFilterChange { it.copy(accountId = null) } },
                    label = { Text("Semua akun") }
                )
                accounts.forEach { acc ->
                    FilterChip(
                        selected = filter.accountId == acc.id,
                        onClick = { onFilterChange { it.copy(accountId = acc.id) } },
                        label = { Text(acc.name) }
                    )
                }
            }

            Text("Kategori", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.categoryId == null,
                    onClick = { onFilterChange { it.copy(categoryId = null) } },
                    label = { Text("Semua kategori") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = filter.categoryId == cat.category.id,
                        onClick = { onFilterChange { it.copy(categoryId = cat.category.id) } },
                        label = { Text(cat.category.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { next ->
                        val clean = next.filter(Char::isDigit)
                        minText = clean
                        onFilterChange { it.copy(minAmount = clean.toDoubleOrNull()) }
                    },
                    label = { Text("Jumlah min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { next ->
                        val clean = next.filter(Char::isDigit)
                        maxText = clean
                        onFilterChange { it.copy(maxAmount = clean.toDoubleOrNull()) }
                    },
                    label = { Text("Jumlah maks") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Urutkan", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.sortField == TransactionSortField.DATE && filter.sortDesc,
                    onClick = { onFilterChange { it.copy(sortField = TransactionSortField.DATE, sortDesc = true) } },
                    label = { Text("Terbaru") }
                )
                FilterChip(
                    selected = filter.sortField == TransactionSortField.DATE && !filter.sortDesc,
                    onClick = { onFilterChange { it.copy(sortField = TransactionSortField.DATE, sortDesc = false) } },
                    label = { Text("Terlama") }
                )
                FilterChip(
                    selected = filter.sortField == TransactionSortField.AMOUNT && filter.sortDesc,
                    onClick = { onFilterChange { it.copy(sortField = TransactionSortField.AMOUNT, sortDesc = true) } },
                    label = { Text("Nominal tertinggi") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        minText = ""
                        maxText = ""
                        onReset()
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Reset filter")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Terapkan")
                }
            }
        }
    }
}
