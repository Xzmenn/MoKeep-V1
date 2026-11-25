package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetHistoryPoint
import com.example.domain.CashflowPoint
import com.example.domain.CategoryBreakdownItem
import com.example.domain.CurrencyFormatter
import com.example.domain.NetWorthPoint
import com.example.ui.theme.LocalFintechColors
import kotlin.math.max

@Composable
fun ChartLegendRow(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CashflowTrendChart(
    points: List<CashflowPoint>,
    showRunningNet: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fintech = LocalFintechColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    if (points.isEmpty()) {
        Text(
            "Belum ada data arus kas pada periode ini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        return
    }

    val maxVal = max(
        1.0,
        points.maxOf { max(it.income, max(it.expense, if (showRunningNet) kotlin.math.abs(it.runningNet) else 0.0)) }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        val legends = mutableListOf(
            "Pemasukan" to fintech.income,
            "Pengeluaran" to fintech.expense
        )
        if (showRunningNet) legends.add("Saldo berjalan" to fintech.transfer)
        ChartLegendRow(legends)
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val w = size.width
            val h = size.height
            val padY = 12f
            val chartH = h - padY * 2f

            // Grid lines
            for (i in 0..3) {
                val y = padY + chartH * (i / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            fun yFor(value: Double): Float {
                val norm = (value / maxVal).toFloat().coerceIn(0f, 1f)
                return padY + chartH * (1f - norm)
            }

            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w

            fun buildPath(selector: (CashflowPoint) -> Double): Path {
                val p = Path()
                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val y = yFor(selector(pt))
                    if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                return p
            }

            val incomePath = buildPath { it.income }
            val expensePath = buildPath { it.expense }

            if (!showRunningNet && points.size > 1) {
                val incomeArea = Path().apply {
                    addPath(incomePath)
                    lineTo(w, padY + chartH)
                    lineTo(0f, padY + chartH)
                    close()
                }
                drawPath(
                    incomeArea,
                    brush = Brush.verticalGradient(
                        listOf(fintech.income.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
            }

            drawPath(
                incomePath,
                color = fintech.income,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                expensePath,
                color = fintech.expense,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            if (showRunningNet) {
                val netPath = buildPath { max(0.0, it.runningNet) }
                drawPath(
                    netPath,
                    color = fintech.transfer,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size > 2) {
                Text(
                    points[points.size / 2].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                points.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdownItem>,
    currency: String,
    locale: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            "Belum ada data kategori pada periode ini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        return
    }
    val total = items.sumOf { it.amount }.coerceAtLeast(1.0)
    val palette = listOf("#14b8a6", "#3b82f6", "#f59e0b", "#8b5cf6", "#ef4444", "#64748b")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(124.dp)) {
                val strokeWidth = 24f
                var startAngle = -90f
                items.forEachIndexed { idx, item ->
                    val sweep = ((item.amount / total) * 360.0).toFloat()
                    val color = parseHexColor(item.color.ifBlank { palette[idx % palette.size] })
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = max(2f, sweep - 2f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    CurrencyFormatter.formatCompact(total, locale),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(5).forEachIndexed { idx, item ->
                val color = parseHexColor(item.color.ifBlank { palette[idx % palette.size] })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "${item.percent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PairedBarChart(
    labels: List<String>,
    seriesA: List<Double>,
    seriesB: List<Double>,
    labelA: String,
    labelB: String,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    if (labels.isEmpty()) return
    val maxVal = max(1.0, max(seriesA.maxOrNull() ?: 0.0, seriesB.maxOrNull() ?: 0.0))

    Column(modifier = modifier.fillMaxWidth()) {
        ChartLegendRow(listOf(labelA to colorA, labelB to colorB))
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
        ) {
            val w = size.width
            val h = size.height
            val groupW = w / labels.size.toFloat()
            val barW = (groupW * 0.30f).coerceAtMost(28f)
            val gap = 4f

            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            labels.indices.forEach { i ->
                val cx = i * groupW + groupW / 2f
                val hA = ((seriesA.getOrElse(i) { 0.0 } / maxVal).toFloat().coerceIn(0f, 1f)) * (h - 8f)
                val hB = ((seriesB.getOrElse(i) { 0.0 } / maxVal).toFloat().coerceIn(0f, 1f)) * (h - 8f)

                drawRoundRect(
                    color = colorA,
                    topLeft = Offset(cx - barW - gap / 2f, h - hA),
                    size = Size(barW, max(2f, hA)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = colorB,
                    topLeft = Offset(cx + gap / 2f, h - hB),
                    size = Size(barW, max(2f, hB)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = max(1, labels.size / 5)
            labels.forEachIndexed { idx, lbl ->
                if (idx % step == 0 || idx == labels.lastIndex) {
                    Text(
                        text = lbl.take(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NetWorthHistoryChart(
    points: List<NetWorthPoint>,
    modifier: Modifier = Modifier
) {
    val fintech = LocalFintechColors.current
    PairedBarChart(
        labels = points.map { it.label },
        seriesA = points.map { it.assets },
        seriesB = points.map { it.liabilities },
        labelA = "Aset",
        labelB = "Liabilitas",
        colorA = fintech.income,
        colorB = fintech.expense,
        modifier = modifier
    )
}

@Composable
fun BudgetHistoryChart(
    points: List<BudgetHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val fintech = LocalFintechColors.current
    PairedBarChart(
        labels = points.map { it.label },
        seriesA = points.map { it.budget },
        seriesB = points.map { it.spent },
        labelA = "Anggaran",
        labelB = "Terpakai",
        colorA = fintech.transfer,
        colorB = fintech.warning,
        modifier = modifier
    )
}
