package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.BudgetStatus
import com.example.domain.BudgetHistoryPoint
import com.example.domain.BudgetSummary
import com.example.domain.BudgetWithStatus
import com.example.domain.CategoryWithSubs
import com.example.domain.CurrencyFormatter
import com.example.domain.DateFormatters
import com.example.domain.GoalWithProgress
import com.example.ui.components.BudgetHistoryChart
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.EmptyStateView
import com.example.ui.components.parseHexColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsGoalsScreen(
    initialTab: Int = 0,
    budgetSummary: BudgetSummary,
    budgetHistory: List<BudgetHistoryPoint>,
    goals: List<GoalWithProgress>,
    accounts: List<AccountEntity>,
    categories: List<CategoryWithSubs>,
    defaultBudgetThreshold: Int,
    currency: String,
    locale: String,
    onMoveBudgetMonth: (Int) -> Unit,
    onCopyPreviousMonth: () -> Unit,
    onLoadBudgetHistory: (categoryId: String) -> Unit,
    onSaveBudget: (editingId: String?, categoryId: String, amount: Double, threshold: Int, note: String?) -> Unit,
    onDeleteBudget: (id: String) -> Unit,
    onSaveGoal: (
        editingId: String?,
        name: String,
        targetAmount: Double,
        deadlineMillis: Long?,
        accountId: String?,
        color: String,
        icon: String,
        note: String?
    ) -> Unit,
    onDeleteGoal: (id: String) -> Unit,
    onAddContribution: (goalId: String, isWithdrawal: Boolean, amount: Double, dateMillis: Long, note: String?) -> Unit,
    onDeleteContribution: (goalId: String, contributionId: String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var selectedBudgetCategoryId by remember { mutableStateOf<String?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetWithStatus?>(null) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalWithProgress?>(null) }
    var contributionTargetGoal by remember { mutableStateOf<GoalWithProgress?>(null) }
    var pendingDeleteGoal by remember { mutableStateOf<GoalWithProgress?>(null) }

    LaunchedEffect(budgetSummary.items, budgetSummary.year, budgetSummary.month) {
        val targetId = selectedBudgetCategoryId?.takeIf { id ->
            budgetSummary.items.any { it.category.id == id }
        } ?: budgetSummary.items.firstOrNull()?.category?.id
        if (targetId != null) {
            selectedBudgetCategoryId = targetId
            onLoadBudgetHistory(targetId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Anggaran") },
                modifier = Modifier.testTag("tab_budgets")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Target (${goals.size})") },
                modifier = Modifier.testTag("tab_goals")
            )
        }

        if (selectedTab == 0) {
            BudgetsTabContent(
                summary = budgetSummary,
                selectedCategoryId = selectedBudgetCategoryId,
                history = budgetHistory,
                currency = currency,
                locale = locale,
                onMoveMonth = onMoveBudgetMonth,
                onCopyPrev = onCopyPreviousMonth,
                onSelectCategory = { catId ->
                    selectedBudgetCategoryId = catId
                    onLoadBudgetHistory(catId)
                },
                onAddBudget = { editingBudget = null; showBudgetDialog = true },
                onEditBudget = { editingBudget = it; showBudgetDialog = true },
                onDeleteBudget = onDeleteBudget
            )
        } else {
            GoalsTabContent(
                goals = goals,
                currency = currency,
                locale = locale,
                onAddGoal = { editingGoal = null; showGoalDialog = true },
                onEditGoal = { editingGoal = it; showGoalDialog = true },
                onDeleteGoal = { pendingDeleteGoal = it },
                onContribute = { contributionTargetGoal = it },
                onDeleteContribution = onDeleteContribution
            )
        }
    }

    if (showBudgetDialog) {
        BudgetFormDialog(
            editing = editingBudget,
            categories = categories,
            defaultThreshold = defaultBudgetThreshold,
            currency = currency,
            onDismiss = { showBudgetDialog = false },
            onSave = { catId, amount, threshold, note ->
                onSaveBudget(editingBudget?.budget?.id, catId, amount, threshold, note)
                showBudgetDialog = false
            }
        )
    }

    if (showGoalDialog) {
        GoalFormDialog(
            editing = editingGoal,
            accounts = accounts,
            currency = currency,
            onDismiss = { showGoalDialog = false },
            onSave = { name, target, deadline, accId, color, icon, note ->
                onSaveGoal(editingGoal?.goal?.id, name, target, deadline, accId, color, icon, note)
                showGoalDialog = false
            }
        )
    }

    contributionTargetGoal?.let { goal ->
        GoalContributionDialog(
            goalName = goal.goal.name,
            currency = currency,
            onDismiss = { contributionTargetGoal = null },
            onSave = { isWithdrawal, amount, dateMillis, note ->
                onAddContribution(goal.goal.id, isWithdrawal, amount, dateMillis, note)
                contributionTargetGoal = null
            }
        )
    }

    pendingDeleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            title = { Text("Hapus target \"${goal.goal.name}\"?") },
            text = { Text("Riwayat setoran pada target ini juga akan dihapus.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteGoal(goal.goal.id)
                    pendingDeleteGoal = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGoal = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BudgetsTabContent(
    summary: BudgetSummary,
    selectedCategoryId: String?,
    history: List<BudgetHistoryPoint>,
    currency: String,
    locale: String,
    onMoveMonth: (Int) -> Unit,
    onCopyPrev: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (BudgetWithStatus) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    val fintech = LocalFintechColors.current
    val alerts = summary.items.filter { it.status != BudgetStatus.SAFE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Switcher & Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMoveMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                        }
                        Text(
                            DateFormatters.formatMonthYear(summary.year, summary.month),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onMoveMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopyPrev,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("copy_prev_budgets_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin bulan lalu", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = onAddBudget,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("add_budget_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anggaran")
                        }
                    }
                }
            }
        }

        // 3 Summary Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetMiniMetric("Anggaran", summary.totalBudget, currency, locale, Modifier.weight(1f))
                BudgetMiniMetric("Terpakai", summary.totalSpent, currency, locale, Modifier.weight(1f))
                BudgetMiniMetric("Sisa", summary.totalRemaining, currency, locale, Modifier.weight(1f))
            }
        }

        if (alerts.isNotEmpty()) {
            item {
                val hasExceeded = alerts.any { it.status == BudgetStatus.EXCEEDED }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (hasExceeded) fintech.expenseContainer else fintech.warningContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (hasExceeded) fintech.expense else fintech.warning
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "${alerts.size} kategori perlu perhatian (${alerts.joinToString { it.category.name }})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (summary.items.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum ada anggaran bulan ini",
                    description = "Tetapkan batas pengeluaran per kategori atau salin anggaran dari bulan lalu.",
                    actionLabel = "Tambah Anggaran",
                    onAction = onAddBudget
                )
            }
        } else {
            items(summary.items, key = { it.budget.id }) { item ->
                val isSelected = item.category.id == selectedCategoryId
                val statusColor = when (item.status) {
                    BudgetStatus.SAFE -> fintech.income
                    BudgetStatus.WARNING -> fintech.warning
                    BudgetStatus.EXCEEDED -> fintech.expense
                }
                Card(
                    onClick = { onSelectCategory(item.category.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadgeIcon(item.category.icon, item.category.color, item.category.name)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.category.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${CurrencyFormatter.format(item.spent, currency, locale)} dari ${CurrencyFormatter.format(item.budget.amount, currency, locale)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(containerColor = statusColor, contentColor = Color.White) {
                                Text("${item.percent}%", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (item.percent / 100f).coerceIn(0f, 1f) },
                            color = statusColor,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (item.remaining >= 0) "Sisa ${CurrencyFormatter.format(item.remaining, currency, locale)}"
                                else "Lebih ${CurrencyFormatter.format(-item.remaining, currency, locale)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                            Row {
                                IconButton(onClick = { onEditBudget(item) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Ubah anggaran", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDeleteBudget(item.budget.id) }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Hapus anggaran",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                val activeCatName = summary.items.firstOrNull { it.category.id == selectedCategoryId }?.category?.name ?: ""
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Riwayat anggaran 6 bulan · $activeCatName",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BudgetHistoryChart(points = history)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetMiniMetric(
    label: String,
    amount: Double,
    currency: String,
    locale: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                CurrencyFormatter.formatCompact(amount, locale),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GoalsTabContent(
    goals: List<GoalWithProgress>,
    currency: String,
    locale: String,
    onAddGoal: () -> Unit,
    onEditGoal: (GoalWithProgress) -> Unit,
    onDeleteGoal: (GoalWithProgress) -> Unit,
    onContribute: (GoalWithProgress) -> Unit,
    onDeleteContribution: (goalId: String, contributionId: String) -> Unit
) {
    val fintech = LocalFintechColors.current
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
                Text("${goals.size} target tabungan", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = onAddGoal,
                    modifier = Modifier.height(48.dp).testTag("add_goal_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target")
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum ada target keuangan",
                    description = "Buat target tabungan seperti dana darurat atau liburan dan pantau estimasi tercapainya.",
                    actionLabel = "Tambah Target",
                    onAction = onAddGoal
                )
            }
        } else {
            items(goals, key = { it.goal.id }) { goal ->
                var expandedHistory by remember { mutableStateOf(false) }
                val badgeText = when {
                    goal.remaining <= 0 -> "Tercapai"
                    goal.onTrack -> "Sesuai rencana"
                    else -> "Perlu perhatian"
                }
                val badgeColor = when {
                    goal.remaining <= 0 || goal.onTrack -> fintech.income
                    else -> fintech.warning
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadgeIcon(goal.goal.icon, goal.goal.color, goal.goal.name)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.goal.name, style = MaterialTheme.typography.titleMedium)
                                if (goal.account != null) {
                                    Text(
                                        "Akun: ${goal.account.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Badge(containerColor = badgeColor, contentColor = Color.White) {
                                Text(badgeText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                CurrencyFormatter.format(goal.saved, currency, locale),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "Target ${CurrencyFormatter.format(goal.goal.targetAmount, currency, locale)} (${goal.percent}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (goal.percent / 100f).coerceIn(0f, 1f) },
                            color = parseHexColor(goal.goal.color),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            goal.goal.deadline?.let { dl ->
                                val daysLeft = max(0L, (dl - System.currentTimeMillis()) / 86_400_000L)
                                Text(
                                    "Tenggat: ${DateFormatters.formatDate(dl)} ($daysLeft hari lagi)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Rata-rata bulanan: ${CurrencyFormatter.format(goal.monthlyAverage, currency, locale)}/bln",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            goal.requiredPerMonth?.let { req ->
                                if (goal.remaining > 0) {
                                    Text(
                                        "Kebutuhan per bulan: ${CurrencyFormatter.format(req, currency, locale)}/bln",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            goal.estimatedDate?.let { est ->
                                if (goal.remaining > 0) {
                                    Text(
                                        "Estimasi tercapai: ${DateFormatters.formatDate(est)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onContribute(goal) },
                                modifier = Modifier.height(44.dp).testTag("goal_contribute_${goal.goal.id}")
                            ) {
                                Text("Setor / Tarik")
                            }
                            Row {
                                IconButton(onClick = { expandedHistory = !expandedHistory }) {
                                    Icon(
                                        if (expandedHistory) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = "Riwayat kontribusi"
                                    )
                                }
                                IconButton(onClick = { onEditGoal(goal) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Ubah target")
                                }
                                IconButton(onClick = { onDeleteGoal(goal) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus target", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (expandedHistory) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            if (goal.contributions.isEmpty()) {
                                Text(
                                    "Belum ada riwayat setoran.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                goal.contributions.forEach { c ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                (if (c.amount >= 0) "+" else "") + CurrencyFormatter.format(c.amount, currency, locale),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (c.amount >= 0) fintech.income else fintech.expense
                                            )
                                            Text(
                                                "${DateFormatters.formatShortDate(c.date)} · ${c.note ?: if (c.amount >= 0) "Setoran" else "Penarikan"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteContribution(goal.goal.id, c.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Hapus kontribusi", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
