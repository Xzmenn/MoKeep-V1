package com.example.data.local

import androidx.room.withTransaction
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionTagCrossRef
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import com.example.domain.BalanceCalculator
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

object DefaultCategoryCatalog {
    data class DefaultCat(
        val name: String,
        val type: CategoryType,
        val icon: String,
        val color: String,
        val subs: List<String> = emptyList()
    )

    val items = listOf(
        DefaultCat("Makan & Minum", CategoryType.EXPENSE, "utensils", "#f97316", listOf("Restoran", "Kopi", "Groceries")),
        DefaultCat("Transportasi", CategoryType.EXPENSE, "bus", "#3b82f6", listOf("Bensin", "Ojek Online", "Parkir", "Tol")),
        DefaultCat("Belanja", CategoryType.EXPENSE, "shopping-bag", "#a855f7", listOf("Pakaian", "Elektronik")),
        DefaultCat("Tagihan", CategoryType.EXPENSE, "receipt", "#eab308", listOf("Listrik", "Air", "Internet", "Pulsa")),
        DefaultCat("Hiburan", CategoryType.EXPENSE, "clapperboard", "#ec4899", listOf("Film", "Langganan")),
        DefaultCat("Kesehatan", CategoryType.EXPENSE, "heart-pulse", "#ef4444"),
        DefaultCat("Pendidikan", CategoryType.EXPENSE, "graduation-cap", "#14b8a6"),
        DefaultCat("Rumah Tangga", CategoryType.EXPENSE, "house", "#84cc16"),
        DefaultCat("Sosial & Donasi", CategoryType.EXPENSE, "hand-heart", "#f43f5e"),
        DefaultCat("Lainnya", CategoryType.EXPENSE, "circle-ellipsis", "#64748b"),
        DefaultCat("Gaji", CategoryType.INCOME, "briefcase-business", "#22c55e"),
        DefaultCat("Bonus", CategoryType.INCOME, "badge-percent", "#10b981"),
        DefaultCat("Freelance", CategoryType.INCOME, "laptop", "#06b6d4"),
        DefaultCat("Investasi", CategoryType.INCOME, "chart-no-axes-combined", "#0ea5e9"),
        DefaultCat("Hadiah", CategoryType.INCOME, "gift", "#d946ef"),
        DefaultCat("Lainnya", CategoryType.INCOME, "circle-ellipsis", "#64748b"),
        DefaultCat("Transfer", CategoryType.TRANSFER, "arrow-left-right", "#6366f1")
    )
}

class DatabaseSeeder(private val db: AppDatabase) {
    private val dao = db.dao()

    suspend fun seedSampleDataForUser(userId: String) {
        if (dao.getAccounts(userId).isNotEmpty()) return

        db.withTransaction {
            val existingCats = dao.getCategories(userId)
            val catIds = mutableMapOf<Pair<String, CategoryType>, String>()
            existingCats.forEach { cat -> catIds[cat.name to cat.type] = cat.id }

            for (cat in DefaultCategoryCatalog.items) {
                if (!catIds.containsKey(cat.name to cat.type)) {
                    val catId = UUID.randomUUID().toString()
                    dao.insertCategory(
                        CategoryEntity(
                            id = catId,
                            userId = userId,
                            name = cat.name,
                            type = cat.type,
                            icon = cat.icon,
                            color = cat.color,
                            isDefault = true
                        )
                    )
                    catIds[cat.name to cat.type] = catId
                    for (sub in cat.subs) {
                        dao.insertSubCategory(
                            SubCategoryEntity(
                                id = UUID.randomUUID().toString(),
                                categoryId = catId,
                                name = sub
                            )
                        )
                    }
                }
            }

            val cash = AccountEntity(
                id = "acc-cash-$userId",
                userId = userId,
                name = "Kas",
                type = AccountType.CASH,
                initialBalance = 500_000.0,
                balance = 500_000.0,
                color = "#22c55e",
                icon = "wallet"
            )
            val bank = AccountEntity(
                id = "acc-bank-$userId",
                userId = userId,
                name = "BCA",
                type = AccountType.BANK,
                initialBalance = 20_000_000.0,
                balance = 20_000_000.0,
                color = "#2563eb",
                icon = "landmark"
            )
            val wallet = AccountEntity(
                id = "acc-wallet-$userId",
                userId = userId,
                name = "GoPay",
                type = AccountType.EWALLET,
                initialBalance = 500_000.0,
                balance = 500_000.0,
                color = "#06b6d4",
                icon = "smartphone"
            )
            val credit = AccountEntity(
                id = "acc-credit-$userId",
                userId = userId,
                name = "Kartu Kredit BNI",
                type = AccountType.CREDIT_CARD,
                initialBalance = 0.0,
                balance = 0.0,
                creditLimit = 10_000_000.0,
                color = "#f97316",
                icon = "credit-card"
            )
            val investment = AccountEntity(
                id = "acc-invest-$userId",
                userId = userId,
                name = "Reksa Dana",
                type = AccountType.INVESTMENT,
                initialBalance = 5_000_000.0,
                balance = 5_000_000.0,
                color = "#8b5cf6",
                icon = "chart-pie"
            )
            val accounts = listOf(cash, bank, wallet, credit, investment)
            accounts.forEach { dao.insertAccount(it) }

            val salaryId = catIds["Gaji" to CategoryType.INCOME]
            val freelanceId = catIds["Freelance" to CategoryType.INCOME]
            val foodId = catIds["Makan & Minum" to CategoryType.EXPENSE]!!
            val transportId = catIds["Transportasi" to CategoryType.EXPENSE]!!
            val shoppingId = catIds["Belanja" to CategoryType.EXPENSE]
            val billsId = catIds["Tagihan" to CategoryType.EXPENSE]
            val transferId = catIds["Transfer" to CategoryType.TRANSFER]

            val tagRutin = TagEntity(id = "tag-rutin-$userId", userId = userId, name = "rutin", color = "#0ea5e9")
            val tagKeluarga = TagEntity(id = "tag-keluarga-$userId", userId = userId, name = "keluarga", color = "#8b5cf6")
            val tagKebutuhan = TagEntity(id = "tag-kebutuhan-$userId", userId = userId, name = "kebutuhan", color = "#10b981")
            listOf(tagRutin, tagKeluarga, tagKebutuhan).forEach { dao.insertTag(it) }
            val tagMap = mapOf("rutin" to tagRutin.id, "keluarga" to tagKeluarga.id, "kebutuhan" to tagKebutuhan.id)

            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val transactionsToSave = mutableListOf<TransactionEntity>()

            for (offset in 2 downTo 0) {
                val ym = YearMonth.from(today).minusMonths(offset.toLong())
                val latestDay = if (offset == 0) today.dayOfMonth else ym.lengthOfMonth()
                fun dateAt(day: Int): Long {
                    val safeDay = minOf(day, latestDay)
                    return ym.atDay(safeDay).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
                }
                val salaryDay = if (offset == 0) 5 else 25

                data class SeedEntry(
                    val type: TransactionType,
                    val amount: Double,
                    val date: Long,
                    val accountId: String,
                    val toAccountId: String? = null,
                    val categoryId: String?,
                    val note: String,
                    val tags: List<String> = emptyList()
                )

                val entries = listOf(
                    SeedEntry(TransactionType.INCOME, 12_500_000.0, dateAt(salaryDay), bank.id, null, salaryId, "Gaji bulanan", listOf("rutin")),
                    SeedEntry(TransactionType.INCOME, 1_200_000.0, dateAt(10), bank.id, null, freelanceId, "Proyek desain"),
                    SeedEntry(TransactionType.EXPENSE, 180_000.0, dateAt(7), wallet.id, null, foodId, "Makan siang dan kopi", listOf("kebutuhan")),
                    SeedEntry(TransactionType.EXPENSE, 120_000.0, dateAt(12), wallet.id, null, transportId, "Transportasi harian", listOf("rutin")),
                    SeedEntry(TransactionType.EXPENSE, 800_000.0, dateAt(14), bank.id, null, billsId, "Tagihan listrik dan internet", listOf("rutin")),
                    SeedEntry(TransactionType.EXPENSE, 650_000.0, dateAt(17), bank.id, null, foodId, "Belanja kebutuhan rumah", listOf("keluarga", "kebutuhan")),
                    SeedEntry(TransactionType.EXPENSE, 1_200_000.0, dateAt(19), credit.id, null, shoppingId, "Belanja bulanan dengan kartu kredit", listOf("keluarga")),
                    SeedEntry(TransactionType.TRANSFER, 500_000.0, dateAt(20), bank.id, wallet.id, transferId, "Isi saldo GoPay"),
                    SeedEntry(TransactionType.TRANSFER, 1_000_000.0, dateAt(22), bank.id, investment.id, transferId, "Top up investasi", listOf("rutin"))
                )

                for (entry in entries) {
                    val txId = UUID.randomUUID().toString()
                    val tx = TransactionEntity(
                        id = txId,
                        userId = userId,
                        type = entry.type,
                        amount = entry.amount,
                        date = entry.date,
                        accountId = entry.accountId,
                        toAccountId = entry.toAccountId,
                        categoryId = entry.categoryId,
                        note = entry.note
                    )
                    transactionsToSave.add(tx)
                    dao.insertTransaction(tx)
                    for (tagName in entry.tags) {
                        tagMap[tagName]?.let { tagId ->
                            dao.insertTransactionTag(TransactionTagCrossRef(txId, tagId))
                        }
                    }
                }
            }

            val computedBalances = BalanceCalculator.computeBalances(accounts, transactionsToSave)
            val nowMillis = System.currentTimeMillis()
            computedBalances.forEach { (accId, balance) ->
                dao.updateAccountBalance(accId, balance, nowMillis)
            }

            val curYm = YearMonth.from(today)
            val prevYm = curYm.minusMonths(1)
            dao.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    categoryId = foodId,
                    year = curYm.year,
                    month = curYm.monthValue,
                    amount = 2_500_000.0,
                    alertThreshold = 80
                )
            )
            dao.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    categoryId = transportId,
                    year = curYm.year,
                    month = curYm.monthValue,
                    amount = 900_000.0,
                    alertThreshold = 80
                )
            )
            dao.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    categoryId = foodId,
                    year = prevYm.year,
                    month = prevYm.monthValue,
                    amount = 2_200_000.0,
                    alertThreshold = 80
                )
            )

            val goal1Id = "goal-emergency-$userId"
            dao.insertGoal(
                GoalEntity(
                    id = goal1Id,
                    userId = userId,
                    name = "Dana darurat",
                    targetAmount = 30_000_000.0,
                    deadline = today.plusYears(1).withDayOfMonth(1).atTime(23, 59).atZone(zoneId).toInstant().toEpochMilli(),
                    accountId = bank.id,
                    color = "#14b887",
                    icon = "shield-check"
                )
            )
            dao.insertGoalContribution(
                GoalContributionEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goal1Id,
                    amount = 2_000_000.0,
                    date = today.minusMonths(2).withDayOfMonth(12).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    note = "Setoran awal"
                )
            )
            dao.insertGoalContribution(
                GoalContributionEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goal1Id,
                    amount = 1_500_000.0,
                    date = today.minusMonths(1).withDayOfMonth(12).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    note = "Setoran rutin"
                )
            )

            val goal2Id = "goal-vacation-$userId"
            dao.insertGoal(
                GoalEntity(
                    id = goal2Id,
                    userId = userId,
                    name = "Liburan keluarga",
                    targetAmount = 12_000_000.0,
                    deadline = today.plusMonths(6).withDayOfMonth(1).atTime(23, 59).atZone(zoneId).toInstant().toEpochMilli(),
                    accountId = cash.id,
                    color = "#f59e0b",
                    icon = "plane"
                )
            )
            dao.insertGoalContribution(
                GoalContributionEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goal2Id,
                    amount = 750_000.0,
                    date = today.minusMonths(1).withDayOfMonth(18).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    note = "Tabungan liburan"
                )
            )

            dao.insertRecurringTransaction(
                RecurringTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 120_000.0,
                    accountId = wallet.id,
                    categoryId = transportId,
                    frequency = RecurrenceFrequency.WEEKLY,
                    interval = 1,
                    startDate = nowMillis,
                    nextRunDate = today.plusDays(7).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    note = "Transportasi mingguan",
                    tagNames = "rutin"
                )
            )
            dao.insertRecurringTransaction(
                RecurringTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 800_000.0,
                    accountId = bank.id,
                    categoryId = billsId,
                    frequency = RecurrenceFrequency.MONTHLY,
                    interval = 1,
                    startDate = nowMillis,
                    nextRunDate = today.plusMonths(1).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    note = "Tagihan internet bulanan",
                    tagNames = "rutin"
                )
            )
        }
    }
}
