package com.example.domain

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.BudgetStatus
import com.example.data.model.PeriodPreset
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class AccountDelta(val accountId: String, val delta: Double)

object BalanceCalculator {
    fun transactionEffects(transaction: TransactionEntity): List<AccountDelta> {
        return when (transaction.type) {
            TransactionType.INCOME -> listOf(AccountDelta(transaction.accountId, transaction.amount))
            TransactionType.EXPENSE -> listOf(AccountDelta(transaction.accountId, -transaction.amount))
            TransactionType.TRANSFER -> {
                if (transaction.toAccountId.isNullOrBlank()) {
                    listOf(AccountDelta(transaction.accountId, -transaction.amount))
                } else {
                    listOf(
                        AccountDelta(transaction.accountId, -transaction.amount),
                        AccountDelta(transaction.toAccountId, transaction.amount)
                    )
                }
            }
        }
    }

    fun computeBalances(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Map<String, Double> {
        val balances = accounts.associate { it.id to it.initialBalance }.toMutableMap()
        for (tx in transactions) {
            for (effect in transactionEffects(tx)) {
                if (balances.containsKey(effect.accountId)) {
                    balances[effect.accountId] = (balances[effect.accountId] ?: 0.0) + effect.delta
                }
            }
        }
        return balances
    }

    fun validateMinimumBalance(account: AccountEntity, computedBalance: Double) {
        val minimum = if (account.type == AccountType.CREDIT_CARD && account.creditLimit != null) {
            -account.creditLimit
        } else {
            0.0
        }
        if (computedBalance < minimum - 0.0001) {
            throw IllegalStateException("Saldo akun \"${account.name}\" tidak mencukupi")
        }
    }
}

data class BudgetComputation(
    val percent: Int,
    val remaining: Double,
    val status: BudgetStatus
)

object BudgetGoalCalculator {
    fun getBudgetStatus(spent: Double, amount: Double, threshold: Int): BudgetComputation {
        val percent = if (amount > 0) ((spent / amount) * 100.0).roundToInt() else 0
        val remaining = amount - spent
        val status = when {
            percent >= 100 -> BudgetStatus.EXCEEDED
            percent >= threshold -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
        return BudgetComputation(percent = percent, remaining = remaining, status = status)
    }

    data class GoalEstimation(
        val percent: Int,
        val remaining: Double,
        val monthlyAverage: Double,
        val estimatedDate: Long?,
        val requiredPerMonth: Double?,
        val onTrack: Boolean
    )

    fun estimateGoalCompletion(
        saved: Double,
        target: Double,
        contributions: List<GoalContributionEntity>,
        deadline: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): GoalEstimation {
        val percent = if (target > 0) min(100, max(0, ((saved / target) * 100.0).roundToInt())) else 0
        val remaining = max(0.0, target - saved)
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val firstRecentMonthMillis = nowDate.minusMonths(2).withDayOfMonth(1)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()

        val recentTotal = contributions.sumOf { contribution ->
            if (contribution.date in firstRecentMonthMillis..nowMillis) contribution.amount else 0.0
        }
        val monthlyAverage = recentTotal / 3.0

        val monthsRemaining = deadline?.let { dl ->
            max(1.0, (dl - nowMillis) / (30.4375 * 86_400_000.0))
        }
        val requiredPerMonth = when {
            remaining == 0.0 && deadline != null -> 0.0
            remaining == 0.0 || monthsRemaining == null || (deadline != null && deadline <= nowMillis) -> null
            else -> remaining / monthsRemaining
        }
        val estimatedDate = when {
            remaining == 0.0 -> nowMillis
            monthlyAverage > 0 -> {
                val monthsNeeded = ceil(remaining / monthlyAverage).toLong()
                Instant.ofEpochMilli(nowMillis).atZone(zoneId).plusMonths(monthsNeeded)
                    .toInstant().toEpochMilli()
            }
            else -> null
        }
        val onTrack = remaining == 0.0 ||
            (requiredPerMonth != null && monthlyAverage >= requiredPerMonth) ||
            (deadline == null && monthlyAverage > 0)

        return GoalEstimation(
            percent = percent,
            remaining = remaining,
            monthlyAverage = monthlyAverage,
            estimatedDate = estimatedDate,
            requiredPerMonth = requiredPerMonth,
            onTrack = onTrack
        )
    }
}

object RecurringCalculator {
    fun getNextRunDate(
        epochMillis: Long,
        frequency: RecurrenceFrequency,
        interval: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val steps = max(1, interval).toLong()
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val next = when (frequency) {
            RecurrenceFrequency.DAILY -> zoned.plusDays(steps)
            RecurrenceFrequency.WEEKLY -> zoned.plusWeeks(steps)
            RecurrenceFrequency.MONTHLY -> zoned.plusMonths(steps)
            RecurrenceFrequency.YEARLY -> zoned.plusYears(steps)
        }
        return next.toInstant().toEpochMilli()
    }
}

object ReportsCalculator {
    private val zoneId: ZoneId get() = ZoneId.systemDefault()
    private val idLocale = Locale.forLanguageTag("id-ID")

    fun getPeriodRange(
        preset: PeriodPreset,
        nowMillis: Long = System.currentTimeMillis(),
        weekStartsOn: Int = 1,
        customFrom: Long? = null,
        customTo: Long? = null
    ): Pair<Long, Long> {
        if (preset == PeriodPreset.CUSTOM && customFrom != null && customTo != null) {
            val fromDate = Instant.ofEpochMilli(customFrom).atZone(zoneId).toLocalDate()
            val toDate = Instant.ofEpochMilli(customTo).atZone(zoneId).toLocalDate()
            return startOfDay(fromDate) to endOfDay(toDate)
        }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when (preset) {
            PeriodPreset.TODAY -> startOfDay(today) to endOfDay(today)
            PeriodPreset.WEEK -> {
                val dow = today.dayOfWeek.value // 1=Mon..7=Sun
                val offset = if (weekStartsOn == 0) dow % 7 else dow - 1
                val start = today.minusDays(offset.toLong())
                val end = start.plusDays(6)
                startOfDay(start) to endOfDay(end)
            }
            PeriodPreset.YEAR -> {
                val start = today.withDayOfYear(1)
                val end = today.withMonth(12).withDayOfMonth(31)
                startOfDay(start) to endOfDay(end)
            }
            PeriodPreset.MONTH, PeriodPreset.CUSTOM -> {
                val ym = YearMonth.from(today)
                startOfDay(ym.atDay(1)) to endOfDay(ym.atEndOfMonth())
            }
        }
    }

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate): Long =
        date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

    fun percentageChange(current: Double, previous: Double): Double {
        if (previous == 0.0) return if (current == 0.0) 0.0 else 100.0
        return ((current - previous) / abs(previous)) * 100.0
    }

    fun bucketCashflow(
        transactions: List<TransactionEntity>,
        fromMillis: Long,
        toMillis: Long
    ): Pair<CashflowBucket, List<CashflowPoint>> {
        val startDate = Instant.ofEpochMilli(fromMillis).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(toMillis).atZone(zoneId).toLocalDate()
        val dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1
        if (dayCount <= 0) return CashflowBucket.DAY to emptyList()

        val bucket = when {
            dayCount <= 31 -> CashflowBucket.DAY
            dayCount <= 180 -> CashflowBucket.WEEK
            else -> CashflowBucket.MONTH
        }

        fun bucketStart(date: LocalDate): LocalDate = when (bucket) {
            CashflowBucket.DAY -> date
            CashflowBucket.WEEK -> date.minusDays((date.dayOfWeek.value - 1).toLong())
            CashflowBucket.MONTH -> date.withDayOfMonth(1)
        }

        fun bucketKey(date: LocalDate): String {
            val s = bucketStart(date)
            return if (bucket == CashflowBucket.MONTH) {
                "${s.year}-${s.monthValue.toString().padStart(2, '0')}"
            } else {
                s.toString()
            }
        }

        val dayFmt = DateTimeFormatter.ofPattern("d MMM", idLocale)
        val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", idLocale)

        fun bucketLabel(date: LocalDate): String = when (bucket) {
            CashflowBucket.MONTH -> date.format(monthFmt)
            else -> date.format(dayFmt)
        }

        val starts = mutableListOf<LocalDate>()
        var cursor = bucketStart(startDate)
        while (!cursor.isAfter(endDate)) {
            starts.add(cursor)
            cursor = when (bucket) {
                CashflowBucket.DAY -> cursor.plusDays(1)
                CashflowBucket.WEEK -> cursor.plusWeeks(1)
                CashflowBucket.MONTH -> cursor.plusMonths(1)
            }
        }

        val grouped = starts.associate { bucketKey(it) to doubleArrayOf(0.0, 0.0) }
        val startEpoch = startOfDay(startDate)
        val endEpochExclusive = startOfDay(endDate.plusDays(1))

        for (tx in transactions) {
            if (tx.date < startEpoch || tx.date >= endEpochExclusive) continue
            if (tx.type == TransactionType.TRANSFER) continue
            val txDate = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
            val pair = grouped[bucketKey(txDate)] ?: continue
            if (tx.type == TransactionType.INCOME) pair[0] += tx.amount else pair[1] += tx.amount
        }

        var runningNet = 0.0
        val points = starts.map { date ->
            val key = bucketKey(date)
            val pair = grouped[key] ?: doubleArrayOf(0.0, 0.0)
            val net = pair[0] - pair[1]
            runningNet += net
            CashflowPoint(
                key = key,
                label = bucketLabel(date),
                income = pair[0],
                expense = pair[1],
                net = net,
                runningNet = runningNet
            )
        }
        return bucket to points
    }

    fun reconstructNetWorth(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        months: Int = 12,
        nowMillis: Long = System.currentTimeMillis()
    ): List<NetWorthPoint> {
        val count = months.coerceIn(1, 60)
        val ordered = transactions.sortedBy { it.date }
        val balances = accounts.associate { it.id to it.initialBalance }.toMutableMap()
        val nowYm = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        val firstMonth = nowYm.minusMonths((count - 1).toLong())
        val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", idLocale)
        var txIndex = 0
        val points = mutableListOf<NetWorthPoint>()

        for (i in 0 until count) {
            val ym = firstMonth.plusMonths(i.toLong())
            val cutoff = endOfDay(ym.atEndOfMonth())
            while (txIndex < ordered.size && ordered[txIndex].date <= cutoff) {
                val tx = ordered[txIndex]
                for (effect in BalanceCalculator.transactionEffects(tx)) {
                    if (balances.containsKey(effect.accountId)) {
                        balances[effect.accountId] = (balances[effect.accountId] ?: 0.0) + effect.delta
                    }
                }
                txIndex++
            }
            val snapshotValues = if (i == count - 1) {
                accounts.map { it.balance }
            } else {
                balances.values.toList()
            }
            val assets = snapshotValues.sumOf { max(0.0, it) }
            val liabilities = snapshotValues.sumOf { abs(min(0.0, it)) }
            points.add(
                NetWorthPoint(
                    month = ym.toString(),
                    label = ym.atDay(1).format(monthFmt),
                    assets = assets,
                    liabilities = liabilities,
                    netWorth = assets - liabilities
                )
            )
        }
        return points
    }
}
