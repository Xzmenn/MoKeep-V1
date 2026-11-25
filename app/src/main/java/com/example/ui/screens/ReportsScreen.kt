package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import com.example.data.model.PeriodPreset
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.domain.DateFormatters
import com.example.ui.components.CashflowTrendChart
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.NetWorthHistoryChart
import com.example.ui.components.PairedBarChart
import com.example.ui.components.PeriodFilterRow
import com.example.ui.components.StatMetricCard
import com.example.ui.components.parseHexColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import com.example.ui.viewmodel.ReportsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    currency: String,
    locale: String,
    weekStartsOn: Int,
    onSelectPeriod: (PeriodPreset, Int) -> Unit,
    onSetCustomRange: (fromMillis: Long, toMillis: Long) -> Unit,
    onSelectCategoryType: (TransactionType) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onExportPdf: (Uri) -> Unit
) {
    val fintech = LocalFintechColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportCsv(uri)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) onExportPdf(uri)
    }

    val tabs = listOf("Ringkasan", "Arus Kas", "Kategori", "Tren", "Net Worth")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodFilterRow(
                selected = state.preset,
                onSelect = { preset ->
                    if (preset == PeriodPreset.CUSTOM) {
                        pickingFrom = true
                    } else {
                        onSelectPeriod(preset, weekStartsOn)
                    }
                }
            )
            if (state.preset == PeriodPreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickingFrom = true },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateFormatters.formatShortDate(state.fromMillis))
                    }
                    OutlinedButton(
                        onClick = { pickingTo = true },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateFormatters.formatShortDate(state.toMillis))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { csvLauncher.launch("laporan-mokeep.csv") },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("export_report_csv")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor CSV")
                }
                OutlinedButton(
                    onClick = { pdfLauncher.launch("laporan-mokeep.pdf") },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("export_report_pdf")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor PDF")
                }
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) },
                    modifier = Modifier.testTag("report_tab_$idx")
                )
            }
        }

        if (state.isLoading || state.summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val summary = state.summary
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
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
                                title = "Arus kas bersih",
                                amount = summary.net,
                                currency = currency,
                                locale = locale,
                                icon = Icons.Filled.AccountBalance,
                                toneColor = if (summary.net >= 0) fintech.income else fintech.warning,
                                changePercent = summary.changes.net,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Perbandingan periode sebelumnya", style = MaterialTheme.typography.titleMedium)
                                    ComparisonRow(
                                        "Pemasukan lalu",
                                        CurrencyFormatter.format(summary.previous.income, currency, locale)
                                    )
                                    ComparisonRow(
                                        "Pengeluaran lalu",
                                        CurrencyFormatter.format(summary.previous.expense, currency, locale)
                                    )
                                    ComparisonRow(
                                        "Arus kas bersih lalu",
                                        CurrencyFormatter.format(summary.previous.net, currency, locale)
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Grafik arus kas & saldo berjalan", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CashflowTrendChart(
                                        points = state.cashflow?.items.orEmpty(),
                                        showRunningNet = true
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = state.categoryType == TransactionType.EXPENSE,
                                    onClick = { onSelectCategoryType(TransactionType.EXPENSE) },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                ) { Text("Pengeluaran") }
                                SegmentedButton(
                                    selected = state.categoryType == TransactionType.INCOME,
                                    onClick = { onSelectCategoryType(TransactionType.INCOME) },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("Pemasukan") }
                            }
                        }
                        item {
                            val items = state.categoryBreakdown?.items.orEmpty()
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        "Distribusi ${state.categoryType.label.lowercase()}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    CategoryDonutChart(
                                        items = items,
                                        currency = currency,
                                        locale = locale
                                    )
                                    items.forEach { cat ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CategoryBadgeIcon(cat.icon, cat.color, cat.name, size = 34.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(cat.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                                Text(
                                                    "${CurrencyFormatter.format(cat.amount, currency, locale)} (${cat.percent.toInt()}%)",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { (cat.percent / 100.0).toFloat().coerceIn(0f, 1f) },
                                                color = parseHexColor(cat.color),
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            val pts = state.cashflow?.items.orEmpty()
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Tren pemasukan vs pengeluaran", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (pts.isEmpty()) {
                                        Text(
                                            "Belum ada data tren pada periode ini.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        PairedBarChart(
                                            labels = pts.map { it.label },
                                            seriesA = pts.map { it.income },
                                            seriesB = pts.map { it.expense },
                                            labelA = "Pemasukan",
                                            labelB = "Pengeluaran",
                                            colorA = fintech.income,
                                            colorB = fintech.expense
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        item {
                            val nw = state.netWorth
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Kekayaan bersih saat ini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        CurrencyFormatter.format(nw?.currentNetWorth ?: summary.netWorth, currency, locale),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Perkembangan aset vs liabilitas 12 bulan", style = MaterialTheme.typography.titleMedium)
                                    NetWorthHistoryChart(points = nw?.items.orEmpty())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickingFrom) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.fromMillis)
        DatePickerDialog(
            onDismissRequest = { pickingFrom = false },
            confirmButton = {
                TextButton(onClick = {
                    val chosenFrom = picker.selectedDateMillis ?: state.fromMillis
                    pickingFrom = false
                    onSetCustomRange(chosenFrom, maxOf(chosenFrom, state.toMillis))
                    pickingTo = true
                }) { Text("Lanjut tgl akhir") }
            },
            dismissButton = {
                TextButton(onClick = { pickingFrom = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = picker)
        }
    }

    if (pickingTo) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.toMillis)
        DatePickerDialog(
            onDismissRequest = { pickingTo = false },
            confirmButton = {
                TextButton(onClick = {
                    val chosenTo = picker.selectedDateMillis ?: state.toMillis
                    pickingTo = false
                    onSetCustomRange(minOf(state.fromMillis, chosenTo), maxOf(state.fromMillis, chosenTo))
                }) { Text("Terapkan") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTo = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = picker)
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold))
    }
}
