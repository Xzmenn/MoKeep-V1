package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.BudgetStatus
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.domain.DateFormatters
import com.example.domain.GoalWithProgress
import com.example.ui.components.CashflowTrendChart
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PairedBarChart
import com.example.ui.components.StatMetricCard
import com.example.ui.components.parseHexColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    accounts: List<AccountEntity>,
    goals: List<GoalWithProgress>,
    currency: String,
    locale: String,
    onNavigateAccounts: () -> Unit,
    onNavigateTransactions: () -> Unit,
    onNavigateBudgets: () -> Unit,
    onNavigateGoals: () -> Unit,
    onSeedSampleData: () -> Unit = {}
) {
    val fintech = LocalFintechColors.current

    if (state.isLoading || state.summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val summary = state.summary
    val isNewUser = summary.recentTransactions.isEmpty() && accounts.isEmpty() && goals.isEmpty()

    if (isNewUser) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmptyStateView(
                title = "Mulai kelola keuangan Anda",
                description = "Tambahkan akun pertama Anda atau muat data contoh untuk mencoba seluruh fitur MoKeep.",
                actionLabel = "Tambah akun",
                onAction = onNavigateAccounts
            )
            androidx.compose.material3.OutlinedButton(
                onClick = onSeedSampleData,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("seed_sample_data_button")
            ) {
                Text("Isi Data Contoh (Kas, BCA, GoPay & Transaksi)")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Net Worth Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(listOf(fintech.heroStart, fintech.heroEnd))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total kekayaan bersih",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = CurrencyFormatter.format(summary.netWorth, currency, locale),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${accounts.count { !it.isArchived }} akun aktif · Arus kas bersih bulan ini ${CurrencyFormatter.format(summary.net, currency, locale)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // KPI Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Pemasukan",
                    amount = summary.income,
                    currency = currency,
                    locale = locale,
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    toneColor = fintech.income,
                    changePercent = summary.changes.income,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Pengeluaran",
                    amount = summary.expense,
                    currency = currency,
                    locale = locale,
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    toneColor = fintech.expense,
                    changePercent = summary.changes.expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatMetricCard(
                title = "Arus kas bersih bulan ini",
                amount = summary.net,
                currency = currency,
                locale = locale,
                icon = Icons.Filled.AccountBalance,
                toneColor = if (summary.net >= 0) fintech.income else fintech.warning,
                changePercent = summary.changes.net,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Monthly Cashflow Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Arus kas bulan ini", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    CashflowTrendChart(
                        points = state.monthCashflow?.items.orEmpty(),
                        showRunningNet = false
                    )
                }
            }
        }

        // Expense by Category Donut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pengeluaran per kategori", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryDonutChart(
                        items = state.expenseCategories?.items.orEmpty(),
                        currency = currency,
                        locale = locale
                    )
                }
            }
        }

        // 6-Month Income vs Expense Bar Chart
        item {
            val sixItems = state.sixMonthTrends?.items.orEmpty()
            if (sixItems.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pemasukan vs pengeluaran · 6 bulan", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        PairedBarChart(
                            labels = sixItems.map { it.label },
                            seriesA = sixItems.map { it.income },
                            seriesB = sixItems.map { it.expense },
                            labelA = "Pemasukan",
                            labelB = "Pengeluaran",
                            colorA = fintech.income,
                            colorB = fintech.expense
                        )
                    }
                }
            }
        }

        // Budget Alerts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Peringatan anggaran", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = onNavigateBudgets,
                            modifier = Modifier.testTag("dashboard_view_budgets")
                        ) {
                            Text("Lihat semua")
                        }
                    }
                    if (summary.budgetAlerts.isEmpty()) {
                        Text(
                            "Tidak ada kategori yang mendekati batas anggaran.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            summary.budgetAlerts.forEach { alert ->
                                val isExceeded = alert.status == BudgetStatus.EXCEEDED
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isExceeded) fintech.expenseContainer.copy(alpha = 0.45f)
                                    else fintech.warningContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = if (isExceeded) fintech.expense else fintech.warning
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(alert.category.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "${CurrencyFormatter.format(alert.spent, currency, locale)} dari ${CurrencyFormatter.format(alert.budget.amount, currency, locale)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Badge(
                                            containerColor = if (isExceeded) fintech.expense else fintech.warning,
                                            contentColor = Color.White
                                        ) {
                                            Text("${alert.percent}%", modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Financial Goals
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target keuangan", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = onNavigateGoals,
                            modifier = Modifier.testTag("dashboard_view_goals")
                        ) {
                            Text("Lihat semua")
                        }
                    }
                    if (goals.isEmpty()) {
                        Text(
                            "Belum ada target yang dibuat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            goals.take(4).forEach { goal ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(goal.goal.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${goal.percent}%", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (goal.percent / 100f).coerceIn(0f, 1f) },
                                        color = parseHexColor(goal.goal.color),
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            CurrencyFormatter.format(goal.saved, currency, locale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            CurrencyFormatter.format(goal.goal.targetAmount, currency, locale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transaksi terbaru", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = onNavigateTransactions,
                            modifier = Modifier.testTag("dashboard_view_transactions")
                        ) {
                            Text("Lihat semua")
                        }
                    }
                    if (summary.recentTransactions.isEmpty()) {
                        Text(
                            "Belum ada transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        summary.recentTransactions.forEachIndexed { idx, detail ->
                            val tx = detail.transaction
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryBadgeIcon(
                                    iconName = detail.category?.icon ?: "arrow-left-right",
                                    hexColor = detail.category?.color ?: "#64748b",
                                    contentDescription = detail.category?.name
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.note ?: detail.category?.name ?: "Transaksi",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${DateFormatters.formatShortDate(tx.date)} · ${detail.account.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val amountColor = when (tx.type) {
                                    TransactionType.INCOME -> fintech.income
                                    TransactionType.EXPENSE -> fintech.expense
                                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val prefix = when (tx.type) {
                                    TransactionType.INCOME -> "+"
                                    TransactionType.EXPENSE -> "−"
                                    TransactionType.TRANSFER -> ""
                                }
                                Text(
                                    text = "$prefix${CurrencyFormatter.format(tx.amount, currency, locale)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = amountColor
                                )
                            }
                            if (idx < summary.recentTransactions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // Accounts strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Akun", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = onNavigateAccounts,
                    modifier = Modifier.testTag("dashboard_manage_accounts")
                ) {
                    Text("Kelola akun")
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts.filter { !it.isArchived }.take(8), key = { it.id }) { account ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.width(176.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadgeIcon(
                                    iconName = account.icon,
                                    hexColor = account.color,
                                    contentDescription = account.name,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                CurrencyFormatter.format(account.balance, currency, locale),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                account.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
