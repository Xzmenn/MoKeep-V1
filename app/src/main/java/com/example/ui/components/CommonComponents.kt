package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.PeriodPreset
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFintechColors
import kotlin.math.abs

val AvailableColors = listOf(
    "#0f766e", "#14b8a6", "#22c55e", "#3b82f6", "#6366f1",
    "#8b5cf6", "#ec4899", "#ef4444", "#f97316", "#eab308", "#64748b"
)

val AvailableIcons = listOf(
    "wallet", "landmark", "smartphone", "credit-card", "chart-pie",
    "utensils", "bus", "shopping-bag", "receipt", "clapperboard",
    "heart-pulse", "graduation-cap", "house", "hand-heart", "briefcase-business",
    "badge-percent", "laptop", "chart-no-axes-combined", "gift", "arrow-left-right",
    "shield-check", "plane", "target", "circle-ellipsis"
)

fun mapIconName(name: String): ImageVector = when (name.lowercase()) {
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "landmark" -> Icons.Filled.AccountBalance
    "smartphone" -> Icons.Filled.Smartphone
    "credit-card" -> Icons.Filled.CreditCard
    "chart-pie" -> Icons.Filled.PieChart
    "utensils" -> Icons.Filled.Restaurant
    "bus" -> Icons.Filled.DirectionsBus
    "shopping-bag" -> Icons.Filled.ShoppingBag
    "receipt" -> Icons.Filled.ReceiptLong
    "clapperboard" -> Icons.Filled.Movie
    "heart-pulse" -> Icons.Filled.Favorite
    "graduation-cap" -> Icons.Filled.School
    "house" -> Icons.Filled.Home
    "hand-heart" -> Icons.Filled.VolunteerActivism
    "briefcase-business" -> Icons.Filled.Work
    "badge-percent" -> Icons.Filled.Percent
    "laptop" -> Icons.Filled.Laptop
    "chart-no-axes-combined" -> Icons.AutoMirrored.Filled.TrendingUp
    "gift" -> Icons.Filled.CardGiftcard
    "arrow-left-right" -> Icons.AutoMirrored.Filled.CompareArrows
    "shield-check" -> Icons.Filled.Shield
    "plane" -> Icons.Filled.Flight
    "target" -> Icons.Filled.Savings
    "circle-ellipsis" -> Icons.Filled.MoreHoriz
    else -> Icons.Filled.Category
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF0F766E)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        if (clean.length == 6) Color(0xFF000000L or value) else Color(value)
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun CategoryBadgeIcon(
    iconName: String,
    hexColor: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val tint = parseHexColor(hexColor)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = mapIconName(iconName),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun StatMetricCard(
    title: String,
    amount: Double,
    currency: String,
    locale: String,
    icon: ImageVector,
    toneColor: Color,
    changePercent: Double? = null,
    modifier: Modifier = Modifier
) {
    val fintech = LocalFintechColors.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(toneColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = toneColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = CurrencyFormatter.format(amount, currency, locale),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (changePercent != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val isPositive = changePercent >= 0
                val changeColor = if (isPositive) fintech.income else fintech.expense
                val sign = if (isPositive) "+" else "-"
                Text(
                    text = "$sign${String.format("%.1f", abs(changePercent))}% vs periode lalu",
                    style = MaterialTheme.typography.labelSmall,
                    color = changeColor
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.Savings,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun MoneyInputField(
    label: String,
    rawDigits: String,
    onRawDigitsChange: (String) -> Unit,
    currency: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    testTag: String = "money_input"
) {
    val parsed = rawDigits.toDoubleOrNull() ?: 0.0
    OutlinedTextField(
        value = rawDigits,
        onValueChange = { next ->
            val filtered = next.filter { it.isDigit() }
            onRawDigitsChange(filtered)
        },
        label = { Text(label) },
        prefix = { Text("$currency ") },
        supportingText = {
            if (supportingText != null) {
                Text(supportingText)
            } else if (parsed > 0) {
                Text("Preview: ${CurrencyFormatter.format(parsed, currency)}")
            }
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun PeriodFilterRow(
    selected: PeriodPreset,
    onSelect: (PeriodPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodPreset.entries.forEach { preset ->
            FilterChip(
                selected = selected == preset,
                onClick = { onSelect(preset) },
                label = { Text(preset.label) },
                modifier = Modifier.testTag("period_chip_${preset.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selectedColor: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text("Warna", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvailableColors.forEach { hex ->
                val color = parseHexColor(hex)
                val isSelected = hex.equals(selectedColor, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Terpilih",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerGrid(
    selectedIcon: String,
    accentColor: String,
    onSelect: (String) -> Unit
) {
    val accent = parseHexColor(accentColor)
    Column {
        Text("Ikon", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvailableIcons.forEach { iconName ->
                val isSelected = iconName == selectedIcon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accent.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, accent, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { onSelect(iconName) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mapIconName(iconName),
                        contentDescription = iconName,
                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
