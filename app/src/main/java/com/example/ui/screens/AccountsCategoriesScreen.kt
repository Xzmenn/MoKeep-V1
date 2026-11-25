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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.domain.CategoryWithSubs
import com.example.domain.CurrencyFormatter
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.EmptyStateView
import com.example.ui.components.parseHexColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsCategoriesScreen(
    initialTab: Int = 0,
    accounts: List<AccountEntity>,
    categories: List<CategoryWithSubs>,
    currency: String,
    locale: String,
    onSaveAccount: (
        editingId: String?,
        name: String,
        type: AccountType,
        initialBalance: Double,
        creditLimit: Double?,
        color: String,
        icon: String,
        note: String?
    ) -> Unit,
    onArchiveAccount: (id: String, archived: Boolean) -> Unit,
    onDeleteAccount: (id: String) -> Unit,
    onQuickTransfer: (accountId: String) -> Unit,
    onSaveCategory: (editingId: String?, name: String, type: CategoryType, icon: String, color: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onSaveSubCategory: (categoryId: String, editingId: String?, name: String) -> Unit,
    onDeleteSubCategory: (id: String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var pendingDeleteAccount by remember { mutableStateOf<AccountEntity?>(null) }

    var categoryTypeFilter by remember { mutableStateOf(CategoryType.EXPENSE) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var subDialogTarget by remember { mutableStateOf<Pair<CategoryEntity, SubCategoryEntity?>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Akun (${accounts.count { !it.isArchived }})") },
                modifier = Modifier.testTag("tab_accounts")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Kategori (${categories.size})") },
                modifier = Modifier.testTag("tab_categories")
            )
        }

        if (selectedTab == 0) {
            AccountsTabContent(
                accounts = accounts,
                currency = currency,
                locale = locale,
                onAdd = { editingAccount = null; showAccountDialog = true },
                onEdit = { editingAccount = it; showAccountDialog = true },
                onArchiveToggle = onArchiveAccount,
                onDelete = { pendingDeleteAccount = it },
                onQuickTransfer = onQuickTransfer
            )
        } else {
            CategoriesTabContent(
                categories = categories,
                selectedType = categoryTypeFilter,
                onTypeChange = { categoryTypeFilter = it },
                onAddCategory = { editingCategory = null; showCategoryDialog = true },
                onEditCategory = { editingCategory = it; showCategoryDialog = true },
                onDeleteCategory = { pendingDeleteCategory = it },
                onAddSubCategory = { cat -> subDialogTarget = cat to null },
                onEditSubCategory = { cat, sub -> subDialogTarget = cat to sub },
                onDeleteSubCategory = onDeleteSubCategory
            )
        }
    }

    if (showAccountDialog) {
        AccountFormDialog(
            editing = editingAccount,
            currency = currency,
            onDismiss = { showAccountDialog = false },
            onSave = { name, type, initBal, limit, color, icon, note ->
                onSaveAccount(editingAccount?.id, name, type, initBal, limit, color, icon, note)
                showAccountDialog = false
            }
        )
    }

    pendingDeleteAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAccount = null },
            title = { Text("Hapus akun \"${acc.name}\"?") },
            text = { Text("Akun yang sudah memiliki transaksi tidak dapat dihapus dan disarankan untuk diarsipkan.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteAccount(acc.id)
                    pendingDeleteAccount = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAccount = null }) { Text("Batal") }
            }
        )
    }

    if (showCategoryDialog) {
        CategoryFormDialog(
            editing = editingCategory,
            initialType = categoryTypeFilter,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, type, icon, color ->
                onSaveCategory(editingCategory?.id, name, type, icon, color)
                showCategoryDialog = false
            }
        )
    }

    pendingDeleteCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Hapus kategori \"${cat.name}\"?") },
            text = { Text("Transaksi terkait tetap tersimpan namun label kategorinya akan dilepas.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCategory(cat.id)
                    pendingDeleteCategory = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) { Text("Batal") }
            }
        )
    }

    subDialogTarget?.let { (cat, sub) ->
        SubCategoryFormDialog(
            categoryName = cat.name,
            editing = sub,
            onDismiss = { subDialogTarget = null },
            onSave = { name ->
                onSaveSubCategory(cat.id, sub?.id, name)
                subDialogTarget = null
            }
        )
    }
}

@Composable
private fun AccountsTabContent(
    accounts: List<AccountEntity>,
    currency: String,
    locale: String,
    onAdd: () -> Unit,
    onEdit: (AccountEntity) -> Unit,
    onArchiveToggle: (String, Boolean) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onQuickTransfer: (String) -> Unit
) {
    val fintech = LocalFintechColors.current
    val activeAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val archivedAccounts = remember(accounts) { accounts.filter { it.isArchived } }
    var showArchived by remember { mutableStateOf(false) }

    val assets = activeAccounts.sumOf { max(0.0, it.balance) }
    val liabilities = activeAccounts.sumOf { abs(min(0.0, it.balance)) }
    val netWorth = assets - liabilities

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total kekayaan bersih",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                CurrencyFormatter.format(netWorth, currency, locale),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.height(48.dp).testTag("add_account_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Akun")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Aset ${CurrencyFormatter.format(assets, currency, locale)} · Utang ${CurrencyFormatter.format(liabilities, currency, locale)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        if (activeAccounts.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum ada akun aktif",
                    description = "Tambahkan dompet, rekening bank, e-wallet, atau kartu kredit Anda.",
                    actionLabel = "Tambah Akun",
                    onAction = onAdd
                )
            }
        } else {
            val grouped = activeAccounts.groupBy { it.type }
            AccountType.entries.forEach { accType ->
                val list = grouped[accType].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "hdr_${accType.name}") {
                        Text(
                            accType.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(list, key = { it.id }) { account ->
                        AccountItemCard(
                            account = account,
                            currency = currency,
                            locale = locale,
                            expenseColor = fintech.expense,
                            onEdit = { onEdit(account) },
                            onArchive = { onArchiveToggle(account.id, true) },
                            onDelete = { onDelete(account) },
                            onQuickTransfer = { onQuickTransfer(account.id) }
                        )
                    }
                }
            }
        }

        if (archivedAccounts.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { showArchived = !showArchived },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Akun diarsipkan (${archivedAccounts.size})")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        if (showArchived) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (showArchived) {
                items(archivedAccounts, key = { it.id }) { acc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadgeIcon(acc.icon, acc.color, acc.name)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    CurrencyFormatter.format(acc.balance, currency, locale),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { onArchiveToggle(acc.id, false) }) {
                                Icon(Icons.Filled.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pulihkan")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountItemCard(
    account: AccountEntity,
    currency: String,
    locale: String,
    expenseColor: androidx.compose.ui.graphics.Color,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onQuickTransfer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadgeIcon(account.icon, account.color, account.name)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!account.note.isNullOrBlank()) {
                        Text(
                            account.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    CurrencyFormatter.format(account.balance, currency, locale),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (account.balance < 0) expenseColor else MaterialTheme.colorScheme.onSurface
                )
            }

            if (account.type == AccountType.CREDIT_CARD && account.creditLimit != null && account.creditLimit > 0) {
                val used = abs(min(0.0, account.balance))
                val ratio = (used / account.creditLimit).toFloat().coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Tagihan: ${CurrencyFormatter.format(used, currency, locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Limit: ${CurrencyFormatter.format(account.creditLimit, currency, locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    color = if (ratio > 0.8f) expenseColor else parseHexColor(account.color),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onQuickTransfer,
                    modifier = Modifier.height(40.dp).testTag("quick_transfer_${account.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transfer")
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Ubah akun")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(Icons.Filled.Archive, contentDescription = "Arsipkan akun")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus akun", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesTabContent(
    categories: List<CategoryWithSubs>,
    selectedType: CategoryType,
    onTypeChange: (CategoryType) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onAddSubCategory: (CategoryEntity) -> Unit,
    onEditSubCategory: (CategoryEntity, SubCategoryEntity) -> Unit,
    onDeleteSubCategory: (String) -> Unit
) {
    val filtered = remember(categories, selectedType) {
        categories.filter { it.category.type == selectedType }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { onTypeChange(t) },
                            label = { Text(t.label) }
                        )
                    }
                }
                Button(
                    onClick = onAddCategory,
                    modifier = Modifier.height(48.dp).testTag("add_category_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kategori")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum ada kategori ${selectedType.label.lowercase()}",
                    description = "Tambahkan kategori untuk mengelompokkan transaksi Anda.",
                    actionLabel = "Tambah Kategori",
                    onAction = onAddCategory
                )
            }
        } else {
            items(filtered, key = { it.category.id }) { item ->
                val cat = item.category
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadgeIcon(cat.icon, cat.color, cat.name)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.subCategories.size} subkategori",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEditCategory(cat) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Ubah kategori")
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus kategori", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.subCategories.forEach { sub ->
                                InputChip(
                                    selected = false,
                                    onClick = { onEditSubCategory(cat, sub) },
                                    label = { Text(sub.name) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onDeleteSubCategory(sub.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Hapus subkategori", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                )
                            }
                            AssistChip(
                                onClick = { onAddSubCategory(cat) },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Subkategori") }
                            )
                        }
                    }
                }
            }
        }
    }
}
