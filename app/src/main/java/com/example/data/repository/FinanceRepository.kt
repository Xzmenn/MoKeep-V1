package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionTagCrossRef
import com.example.data.model.AccountType
import com.example.data.model.BudgetStatus
import com.example.data.model.CategoryType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import com.example.domain.BackupData
import com.example.domain.BackupPayload
import com.example.domain.BalanceCalculator
import com.example.domain.BudgetAlertWarning
import com.example.domain.BudgetGoalCalculator
import com.example.domain.BudgetHistoryPoint
import com.example.domain.BudgetSummary
import com.example.domain.BudgetWithStatus
import com.example.domain.CategoryBreakdownItem
import com.example.domain.CategoryBreakdownReport
import com.example.domain.CategoryWithSubs
import com.example.domain.CsvAndBackupEngine
import com.example.domain.GoalWithProgress
import com.example.domain.ImportPreviewResult
import com.example.domain.ImportRowError
import com.example.domain.NetWorthReport
import com.example.domain.ParsedImportRow
import com.example.domain.RecurringCalculator
import com.example.domain.RecurringRuleDetail
import com.example.domain.ReportChanges
import com.example.domain.ReportPreviousTotals
import com.example.domain.ReportSummary
import com.example.domain.ReportsCalculator
import com.example.domain.TransactionDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class FinanceRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val dao = db.dao()

    // Reactive Streams
    fun observeAccounts(userId: String): Flow<List<AccountEntity>> = dao.observeAccounts(userId)

    fun observeCategoriesWithSubs(userId: String): Flow<List<CategoryWithSubs>> =
        combine(dao.observeCategories(userId), dao.observeSubCategories(userId)) { cats, subs ->
            val subMap = subs.groupBy { it.categoryId }
            cats.map { cat -> CategoryWithSubs(cat, subMap[cat.id].orEmpty()) }
        }

    fun observeTags(userId: String): Flow<List<TagEntity>> = dao.observeTags(userId)

    fun observeTransactionDetails(userId: String): Flow<List<TransactionDetail>> =
        combine(
            dao.observeTransactions(userId),
            dao.observeAccounts(userId),
            dao.observeCategories(userId),
            dao.observeSubCategories(userId),
            dao.observeTags(userId)
        ) { txs, accs, cats, subs, tags ->
            RawTxBundle(txs, accs, cats, subs, tags)
        }.combine(
            combine(dao.observeTransactionTags(userId), dao.observeAttachments(userId)) { txTags, atts ->
                txTags to atts
            }
        ) { bundle, (txTags, atts) ->
            val accMap = bundle.accounts.associateBy { it.id }
            val catMap = bundle.categories.associateBy { it.id }
            val subMap = bundle.subCategories.associateBy { it.id }
            val tagMap = bundle.tags.associateBy { it.id }
            val txTagMap = txTags.groupBy({ it.transactionId }, { tagMap[it.tagId] })
            val attMap = atts.groupBy { it.transactionId }

            bundle.transactions.mapNotNull { tx ->
                val account = accMap[tx.accountId] ?: return@mapNotNull null
                TransactionDetail(
                    transaction = tx,
                    account = account,
                    toAccount = tx.toAccountId?.let { accMap[it] },
                    category = tx.categoryId?.let { catMap[it] },
                    subCategory = tx.subCategoryId?.let { subMap[it] },
                    tags = txTagMap[tx.id].orEmpty().filterNotNull(),
                    attachments = attMap[tx.id].orEmpty()
                )
            }
        }

    private data class RawTxBundle(
        val transactions: List<TransactionEntity>,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val subCategories: List<SubCategoryEntity>,
        val tags: List<TagEntity>
    )

    fun observeGoalsWithProgress(userId: String): Flow<List<GoalWithProgress>> =
        combine(
            dao.observeGoals(userId),
            dao.observeAccounts(userId),
            dao.observeGoalContributions(userId)
        ) { goals, accounts, contributions ->
            val accMap = accounts.associateBy { it.id }
            val contribMap = contributions.groupBy { it.goalId }
            goals.map { goal ->
                val list = contribMap[goal.id].orEmpty()
                val saved = list.sumOf { it.amount }
                val est = BudgetGoalCalculator.estimateGoalCompletion(
                    saved = saved,
                    target = goal.targetAmount,
                    contributions = list,
                    deadline = goal.deadline
                )
                GoalWithProgress(
                    goal = goal,
                    account = goal.accountId?.let { accMap[it] },
                    contributions = list,
                    saved = saved,
                    percent = est.percent,
                    remaining = est.remaining,
                    monthlyAverage = est.monthlyAverage,
                    estimatedDate = est.estimatedDate,
                    requiredPerMonth = est.requiredPerMonth,
                    onTrack = est.onTrack
                )
            }
        }

    fun observeRecurringDetails(userId: String): Flow<List<RecurringRuleDetail>> =
        combine(
            dao.observeRecurringTransactions(userId),
            dao.observeAccounts(userId),
            dao.observeCategories(userId)
        ) { rules, accounts, categories ->
            val accMap = accounts.associateBy { it.id }
            val catMap = categories.associateBy { it.id }
            rules.map { rule ->
                RecurringRuleDetail(
                    rule = rule,
                    account = accMap[rule.accountId],
                    toAccount = rule.toAccountId?.let { accMap[it] },
                    category = rule.categoryId?.let { catMap[it] }
                )
            }
        }

    fun observeBudgetsSummary(userId: String, year: Int, month: Int): Flow<BudgetSummary> =
        combine(
            dao.observeBudgets(userId),
            dao.observeCategories(userId),
            dao.observeTransactions(userId)
        ) { budgets, categories, transactions ->
            val catMap = categories.associateBy { it.id }
            val zoneId = ZoneId.systemDefault()
            val ym = YearMonth.of(year, month)
            val fromMillis = ReportsCalculator.startOfDay(ym.atDay(1))
            val toMillis = ReportsCalculator.endOfDay(ym.atEndOfMonth())

            val spentByCategory = transactions
                .filter { it.type == TransactionType.EXPENSE && it.date in fromMillis..toMillis && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val monthBudgets = budgets.filter { it.year == year && it.month == month }
            val items = monthBudgets.mapNotNull { b ->
                val cat = catMap[b.categoryId] ?: return@mapNotNull null
                val spent = spentByCategory[b.categoryId] ?: 0.0
                val comp = BudgetGoalCalculator.getBudgetStatus(spent, b.amount, b.alertThreshold)
                BudgetWithStatus(
                    budget = b,
                    category = cat,
                    spent = spent,
                    percent = comp.percent,
                    remaining = comp.remaining,
                    status = comp.status
                )
            }.sortedBy { it.category.name }

            BudgetSummary(
                items = items,
                year = year,
                month = month,
                totalBudget = items.sumOf { it.budget.amount },
                totalSpent = items.sumOf { it.spent },
                totalRemaining = items.sumOf { it.remaining }
            )
        }

    // Balance recalculation inside active transaction
    private suspend fun recalculateBalancesInTx(userId: String, accountIds: List<String>) {
        val ids = accountIds.distinct()
        if (ids.isEmpty()) return
        val accounts = ids.mapNotNull { dao.getAccount(userId, it) }
        if (accounts.size != ids.size) throw IllegalStateException("Akun tidak ditemukan")
        val transactions = dao.getTransactionsForAccounts(userId, ids)
        val computed = BalanceCalculator.computeBalances(accounts, transactions)
        val now = System.currentTimeMillis()
        for (account in accounts) {
            val newBal = computed[account.id] ?: account.initialBalance
            BalanceCalculator.validateMinimumBalance(account, newBal)
            dao.updateAccountBalance(account.id, newBal, now)
        }
    }

    // Accounts CRUD
    suspend fun saveAccount(
        userId: String,
        editingId: String?,
        name: String,
        type: AccountType,
        initialBalance: Double,
        creditLimit: Double?,
        color: String,
        icon: String,
        note: String?
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) throw IllegalArgumentException("Nama akun wajib diisi")
        if (type != AccountType.CREDIT_CARD && (initialBalance < 0 || (creditLimit != null && creditLimit > 0))) {
            throw IllegalArgumentException("Saldo awal negatif dan batas kredit hanya berlaku untuk kartu kredit")
        }
        if (type == AccountType.CREDIT_CARD && (creditLimit == null || creditLimit <= 0)) {
            throw IllegalArgumentException("Batas kredit wajib diisi untuk kartu kredit")
        }
        val existingAll = dao.getAccounts(userId)
        if (existingAll.any { it.name.equals(cleanName, ignoreCase = true) && it.id != editingId }) {
            throw IllegalStateException("Nama akun sudah digunakan")
        }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = editingId ?: UUID.randomUUID().toString()
            if (editingId == null) {
                dao.insertAccount(
                    AccountEntity(
                        id = id,
                        userId = userId,
                        name = cleanName,
                        type = type,
                        initialBalance = initialBalance,
                        balance = initialBalance,
                        creditLimit = if (type == AccountType.CREDIT_CARD) creditLimit else null,
                        color = color,
                        icon = icon,
                        note = note?.takeIf { it.isNotBlank() },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val cur = dao.getAccount(userId, editingId) ?: throw IllegalStateException("Akun tidak ditemukan")
                dao.updateAccount(
                    cur.copy(
                        name = cleanName,
                        type = type,
                        initialBalance = initialBalance,
                        creditLimit = if (type == AccountType.CREDIT_CARD) creditLimit else null,
                        color = color,
                        icon = icon,
                        note = note?.takeIf { it.isNotBlank() },
                        updatedAt = now
                    )
                )
            }
            recalculateBalancesInTx(userId, listOf(id))
        }
    }

    suspend fun setAccountArchived(userId: String, accountId: String, archived: Boolean) {
        val cur = dao.getAccount(userId, accountId) ?: throw IllegalStateException("Akun tidak ditemukan")
        dao.updateAccount(cur.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(userId: String, accountId: String) {
        val txCount = dao.countTransactionsForAccount(userId, accountId)
        if (txCount > 0) {
            throw IllegalStateException("Akun memiliki transaksi, arsipkan saja")
        }
        dao.deleteAccount(userId, accountId)
    }

    // Categories & SubCategories CRUD
    suspend fun saveCategory(
        userId: String,
        editingId: String?,
        name: String,
        type: CategoryType,
        icon: String,
        color: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) throw IllegalArgumentException("Nama kategori wajib diisi")
        val existing = dao.getCategories(userId)
        if (existing.any { it.name.equals(cleanName, ignoreCase = true) && it.type == type && it.id != editingId }) {
            throw IllegalStateException("Kategori dengan nama ini sudah ada")
        }
        val now = System.currentTimeMillis()
        if (editingId == null) {
            dao.insertCategory(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = cleanName,
                    type = type,
                    icon = icon,
                    color = color,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val cur = dao.getCategory(userId, editingId) ?: throw IllegalStateException("Kategori tidak ditemukan")
            dao.updateCategory(cur.copy(name = cleanName, type = type, icon = icon, color = color, updatedAt = now))
        }
    }

    suspend fun deleteCategory(userId: String, categoryId: String) {
        dao.deleteCategory(userId, categoryId)
    }

    suspend fun saveSubCategory(userId: String, categoryId: String, editingId: String?, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) throw IllegalArgumentException("Nama subkategori wajib diisi")
        dao.getCategory(userId, categoryId) ?: throw IllegalStateException("Kategori tidak ditemukan")
        val now = System.currentTimeMillis()
        if (editingId == null) {
            dao.insertSubCategory(
                SubCategoryEntity(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    name = clean,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val cur = dao.getSubCategory(editingId) ?: throw IllegalStateException("Subkategori tidak ditemukan")
            dao.updateSubCategory(cur.copy(name = clean, updatedAt = now))
        }
    }

    suspend fun deleteSubCategory(subCategoryId: String) {
        dao.deleteSubCategory(subCategoryId)
    }

    // Transactions CRUD
    suspend fun saveTransaction(
        userId: String,
        editingId: String?,
        type: TransactionType,
        amount: Double,
        dateMillis: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        subCategoryId: String?,
        note: String?,
        tagNames: List<String>,
        recurringId: String? = null,
        attachmentUri: Uri? = null
    ): Pair<String, List<BudgetAlertWarning>> {
        if (amount <= 0) throw IllegalArgumentException("Jumlah harus lebih besar dari nol")
        val affectedAccounts = mutableListOf(accountId)
        var resolvedCategoryId = categoryId

        val txId = db.withTransaction {
            val account = dao.getAccount(userId, accountId) ?: throw IllegalStateException("Akun tidak ditemukan")
            if (account.isArchived) throw IllegalStateException("Akun yang diarsipkan tidak dapat digunakan")

            if (type == TransactionType.TRANSFER) {
                if (toAccountId.isNullOrBlank() || toAccountId == accountId) {
                    throw IllegalArgumentException("Akun tujuan transfer harus berbeda dari akun asal")
                }
                val dest = dao.getAccount(userId, toAccountId) ?: throw IllegalStateException("Akun tujuan tidak ditemukan")
                if (dest.isArchived) throw IllegalStateException("Akun tujuan yang diarsipkan tidak dapat digunakan")
                affectedAccounts.add(toAccountId)
                if (resolvedCategoryId == null) {
                    resolvedCategoryId = dao.getCategories(userId)
                        .firstOrNull { it.type == CategoryType.TRANSFER && it.name == "Transfer" }?.id
                }
            } else {
                if (resolvedCategoryId.isNullOrBlank()) {
                    throw IllegalArgumentException("Kategori wajib dipilih")
                }
                val cat = dao.getCategory(userId, resolvedCategoryId!!) ?: throw IllegalStateException("Kategori tidak ditemukan")
                if (cat.type.name != type.name) {
                    throw IllegalArgumentException("Jenis kategori tidak sesuai dengan transaksi")
                }
            }

            val now = System.currentTimeMillis()
            val id = editingId ?: UUID.randomUUID().toString()
            if (editingId != null) {
                val existing = dao.getTransaction(userId, editingId) ?: throw IllegalStateException("Transaksi tidak ditemukan")
                affectedAccounts.add(existing.accountId)
                existing.toAccountId?.let { affectedAccounts.add(it) }
                dao.updateTransaction(
                    existing.copy(
                        type = type,
                        amount = amount,
                        date = dateMillis,
                        accountId = accountId,
                        toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                        categoryId = resolvedCategoryId,
                        subCategoryId = if (type == TransactionType.TRANSFER) null else subCategoryId,
                        note = note?.takeIf { it.isNotBlank() },
                        updatedAt = now
                    )
                )
            } else {
                dao.insertTransaction(
                    TransactionEntity(
                        id = id,
                        userId = userId,
                        type = type,
                        amount = amount,
                        date = dateMillis,
                        accountId = accountId,
                        toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                        categoryId = resolvedCategoryId,
                        subCategoryId = if (type == TransactionType.TRANSFER) null else subCategoryId,
                        note = note?.takeIf { it.isNotBlank() },
                        recurringId = recurringId,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            // Upsert tags
            dao.deleteTagsForTransaction(id)
            for (rawName in tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
                val existingTag = dao.getTagByName(userId, rawName)
                val tagId = if (existingTag != null) {
                    existingTag.id
                } else {
                    val newTagId = UUID.randomUUID().toString()
                    dao.insertTag(TagEntity(id = newTagId, userId = userId, name = rawName))
                    newTagId
                }
                dao.insertTransactionTag(TransactionTagCrossRef(id, tagId))
            }

            recalculateBalancesInTx(userId, affectedAccounts)
            id
        }

        if (attachmentUri != null) {
            saveAttachmentFromUri(userId, txId, attachmentUri)
        }

        val warnings = computeBudgetWarnings(userId, type, resolvedCategoryId, dateMillis)
        return txId to warnings
    }

    private suspend fun computeBudgetWarnings(
        userId: String,
        type: TransactionType,
        categoryId: String?,
        dateMillis: Long
    ): List<BudgetAlertWarning> {
        if (type != TransactionType.EXPENSE || categoryId == null) return emptyList()
        val localDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val budget = dao.getBudgetByCategoryMonth(userId, categoryId, localDate.year, localDate.monthValue)
            ?: return emptyList()
        val cat = dao.getCategory(userId, categoryId) ?: return emptyList()
        val ym = YearMonth.of(localDate.year, localDate.monthValue)
        val fromMillis = ReportsCalculator.startOfDay(ym.atDay(1))
        val toMillis = ReportsCalculator.endOfDay(ym.atEndOfMonth())
        val spent = dao.getTransactions(userId)
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == categoryId && it.date in fromMillis..toMillis }
            .sumOf { it.amount }
        val status = BudgetGoalCalculator.getBudgetStatus(spent, budget.amount, budget.alertThreshold)
        return if (status.status == BudgetStatus.SAFE) emptyList() else listOf(
            BudgetAlertWarning(cat.name, status.percent, status.status)
        )
    }

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        db.withTransaction {
            val current = dao.getTransaction(userId, transactionId)
                ?: throw IllegalStateException("Transaksi tidak ditemukan")
            val affected = listOfNotNull(current.accountId, current.toAccountId)
            dao.deleteTransaction(userId, transactionId)
            recalculateBalancesInTx(userId, affected)
        }
    }

    suspend fun saveAttachmentFromUri(userId: String, transactionId: String, uri: Uri) {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        var fileName = "lampiran_${System.currentTimeMillis()}"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val dest = File(dir, "${UUID.randomUUID()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Gagal membaca file lampiran")

        dao.insertAttachment(
            AttachmentEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                transactionId = transactionId,
                fileName = fileName,
                mimeType = mime,
                size = if (size > 0) size else dest.length(),
                localUri = Uri.fromFile(dest).toString()
            )
        )
    }

    suspend fun deleteAttachment(userId: String, attachmentId: String) {
        dao.deleteAttachment(userId, attachmentId)
    }

    // Recurring Rules
    suspend fun createRecurringRule(
        userId: String,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        subCategoryId: String?,
        note: String?,
        tagNames: List<String>,
        frequency: RecurrenceFrequency,
        interval: Int,
        startDateMillis: Long,
        endDateMillis: Long?
    ) {
        val nextRun = RecurringCalculator.getNextRunDate(startDateMillis, frequency, interval)
        dao.insertRecurringTransaction(
            RecurringTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                note = note?.takeIf { it.isNotBlank() },
                tagNames = tagNames.joinToString(",").takeIf { it.isNotBlank() },
                frequency = frequency,
                interval = interval.coerceAtLeast(1),
                startDate = startDateMillis,
                endDate = endDateMillis,
                nextRunDate = nextRun,
                isActive = true
            )
        )
    }

    suspend fun updateRecurringRule(
        userId: String,
        id: String,
        amount: Double,
        frequency: RecurrenceFrequency,
        interval: Int,
        endDateMillis: Long?,
        note: String?,
        isActive: Boolean
    ) {
        val current = dao.getRecurringTransaction(userId, id)
            ?: throw IllegalStateException("Aturan berulang tidak ditemukan")
        val scheduleChanged = current.frequency != frequency || current.interval != interval
        val nextRun = if (scheduleChanged) {
            RecurringCalculator.getNextRunDate(current.lastRunDate ?: current.startDate, frequency, interval)
        } else {
            current.nextRunDate
        }
        dao.updateRecurringTransaction(
            current.copy(
                amount = amount,
                frequency = frequency,
                interval = interval.coerceAtLeast(1),
                endDate = endDateMillis,
                note = note?.takeIf { it.isNotBlank() },
                isActive = isActive,
                nextRunDate = nextRun,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleRecurringActive(userId: String, id: String) {
        val current = dao.getRecurringTransaction(userId, id) ?: return
        dao.updateRecurringTransaction(
            current.copy(isActive = !current.isActive, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteRecurringRule(userId: String, id: String) {
        dao.deleteRecurringTransaction(userId, id)
    }

    suspend fun processDueRecurring(userId: String, nowMillis: Long = System.currentTimeMillis()): Int {
        val dueRules = dao.getDueRecurringTransactions(userId, nowMillis)
        var createdCount = 0
        for (rule in dueRules) {
            var nextRun = rule.nextRunDate
            var lastRun = rule.lastRunDate
            var steps = 0
            try {
                while (nextRun <= nowMillis && steps < 60) {
                    if (rule.endDate != null && nextRun > rule.endDate) {
                        dao.updateRecurringTransaction(rule.copy(isActive = false))
                        break
                    }
                    val tags = rule.tagNames?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                    saveTransaction(
                        userId = userId,
                        editingId = null,
                        type = rule.type,
                        amount = rule.amount,
                        dateMillis = nextRun,
                        accountId = rule.accountId,
                        toAccountId = rule.toAccountId,
                        categoryId = rule.categoryId,
                        subCategoryId = rule.subCategoryId,
                        note = rule.note,
                        tagNames = tags,
                        recurringId = rule.id
                    )
                    lastRun = nextRun
                    nextRun = RecurringCalculator.getNextRunDate(nextRun, rule.frequency, rule.interval)
                    steps++
                    createdCount++
                    val shouldDeactivate = rule.endDate != null && nextRun > rule.endDate
                    dao.updateRecurringTransaction(
                        rule.copy(
                            lastRunDate = lastRun,
                            nextRunDate = nextRun,
                            isActive = !shouldDeactivate,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (shouldDeactivate) break
                }
            } catch (_: Exception) {
                // Stop rule processing on balance or reference error without advancing nextRunDate
            }
        }
        return createdCount
    }

    // Budgets CRUD
    suspend fun saveBudget(
        userId: String,
        editingId: String?,
        categoryId: String,
        year: Int,
        month: Int,
        amount: Double,
        alertThreshold: Int,
        note: String?
    ) {
        if (amount <= 0) throw IllegalArgumentException("Jumlah anggaran harus lebih dari nol")
        val cat = dao.getCategory(userId, categoryId)
        if (cat == null || cat.type != CategoryType.EXPENSE) {
            throw IllegalArgumentException("Kategori pengeluaran tidak ditemukan")
        }
        val existing = dao.getBudgetByCategoryMonth(userId, categoryId, year, month)
        if (existing != null && existing.id != editingId) {
            throw IllegalStateException("Anggaran untuk kategori dan bulan ini sudah ada")
        }
        val now = System.currentTimeMillis()
        if (editingId == null) {
            dao.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    categoryId = categoryId,
                    year = year,
                    month = month,
                    amount = amount,
                    alertThreshold = alertThreshold.coerceIn(1, 100),
                    note = note?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val cur = dao.getBudget(userId, editingId) ?: throw IllegalStateException("Anggaran tidak ditemukan")
            dao.updateBudget(
                cur.copy(
                    categoryId = categoryId,
                    amount = amount,
                    alertThreshold = alertThreshold.coerceIn(1, 100),
                    note = note?.takeIf { it.isNotBlank() },
                    updatedAt = now
                )
            )
        }
    }

    suspend fun deleteBudget(userId: String, id: String) {
        dao.deleteBudget(userId, id)
    }

    suspend fun copyBudgetsFromPreviousMonth(userId: String, toYear: Int, toMonth: Int): Pair<Int, Int> {
        val targetYm = YearMonth.of(toYear, toMonth)
        val prevYm = targetYm.minusMonths(1)
        val source = dao.getBudgetsForMonth(userId, prevYm.year, prevYm.monthValue)
        if (source.isEmpty()) return 0 to 0
        val existingCats = dao.getBudgetsForMonth(userId, toYear, toMonth).map { it.categoryId }.toSet()
        var copied = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        for (item in source) {
            if (existingCats.contains(item.categoryId)) {
                skipped++
            } else {
                dao.insertBudget(
                    BudgetEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        categoryId = item.categoryId,
                        year = toYear,
                        month = toMonth,
                        amount = item.amount,
                        alertThreshold = item.alertThreshold,
                        note = item.note,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                copied++
            }
        }
        return copied to skipped
    }

    suspend fun getBudgetHistory(
        userId: String,
        categoryId: String,
        year: Int,
        month: Int,
        months: Int = 6
    ): List<BudgetHistoryPoint> {
        val endYm = YearMonth.of(year, month)
        val allBudgets = dao.getBudgets(userId).filter { it.categoryId == categoryId }
        val allTx = dao.getTransactions(userId)
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == categoryId }
        val monthFmt = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"))

        return (0 until months).map { idx ->
            val ym = endYm.minusMonths((months - 1 - idx).toLong())
            val start = ReportsCalculator.startOfDay(ym.atDay(1))
            val end = ReportsCalculator.endOfDay(ym.atEndOfMonth())
            val b = allBudgets.firstOrNull { it.year == ym.year && it.month == ym.monthValue }?.amount ?: 0.0
            val spent = allTx.filter { it.date in start..end }.sumOf { it.amount }
            BudgetHistoryPoint(
                year = ym.year,
                month = ym.monthValue,
                label = ym.atDay(1).format(monthFmt),
                budget = b,
                spent = spent
            )
        }
    }

    // Goals & Contributions
    suspend fun saveGoal(
        userId: String,
        editingId: String?,
        name: String,
        targetAmount: Double,
        deadlineMillis: Long?,
        accountId: String?,
        color: String,
        icon: String,
        note: String?
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) throw IllegalArgumentException("Nama target wajib diisi")
        if (targetAmount <= 0) throw IllegalArgumentException("Jumlah target harus lebih dari nol")
        val now = System.currentTimeMillis()
        if (editingId == null) {
            dao.insertGoal(
                GoalEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = cleanName,
                    targetAmount = targetAmount,
                    deadline = deadlineMillis,
                    accountId = accountId,
                    color = color,
                    icon = icon,
                    note = note?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val cur = dao.getGoal(userId, editingId) ?: throw IllegalStateException("Target tidak ditemukan")
            val saved = dao.getContributionsForGoal(editingId).sumOf { it.amount }
            dao.updateGoal(
                cur.copy(
                    name = cleanName,
                    targetAmount = targetAmount,
                    deadline = deadlineMillis,
                    accountId = accountId,
                    color = color,
                    icon = icon,
                    note = note?.takeIf { it.isNotBlank() },
                    isCompleted = saved >= targetAmount,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun deleteGoal(userId: String, id: String) {
        dao.deleteGoal(userId, id)
    }

    suspend fun addGoalContribution(
        userId: String,
        goalId: String,
        isWithdrawal: Boolean,
        amount: Double,
        dateMillis: Long,
        note: String?
    ) {
        if (amount <= 0) throw IllegalArgumentException("Jumlah kontribusi harus lebih dari nol")
        db.withTransaction {
            val goal = dao.getGoal(userId, goalId) ?: throw IllegalStateException("Target tidak ditemukan")
            val savedBefore = dao.getContributionsForGoal(goalId).sumOf { it.amount }
            val signedAmount = if (isWithdrawal) -amount else amount
            if (isWithdrawal && amount > savedBefore) {
                throw IllegalStateException("Penarikan melebihi jumlah tabungan yang tersedia")
            }
            dao.insertGoalContribution(
                GoalContributionEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    amount = signedAmount,
                    date = dateMillis,
                    note = note?.takeIf { it.isNotBlank() }
                )
            )
            val savedAfter = savedBefore + signedAmount
            dao.updateGoal(
                goal.copy(
                    isCompleted = savedAfter >= goal.targetAmount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteGoalContribution(userId: String, goalId: String, contributionId: String) {
        db.withTransaction {
            val goal = dao.getGoal(userId, goalId) ?: throw IllegalStateException("Target tidak ditemukan")
            dao.deleteGoalContribution(goalId, contributionId)
            val savedAfter = dao.getContributionsForGoal(goalId).sumOf { it.amount }
            dao.updateGoal(
                goal.copy(
                    isCompleted = savedAfter >= goal.targetAmount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Reports Computations
    suspend fun buildReportSummary(
        userId: String,
        fromMillis: Long,
        toMillis: Long
    ): ReportSummary {
        val accounts = dao.getAccounts(userId)
        val allTx = dao.getTransactions(userId)
        val cats = dao.getCategories(userId).associateBy { it.id }
        val subs = dao.getSubCategoriesForUser(userId).associateBy { it.id }
        val tags = dao.getTags(userId).associateBy { it.id }
        val txTags = dao.getTransactionTagsForUser(userId).groupBy({ it.transactionId }, { tags[it.tagId] })
        val atts = dao.getAttachments(userId).groupBy { it.transactionId }
        val accMap = accounts.associateBy { it.id }

        val duration = (toMillis - fromMillis) + 1L
        val prevTo = fromMillis - 1L
        val prevFrom = prevTo - duration + 1L

        val currentPeriodTx = allTx.filter { it.date in fromMillis..toMillis }
        val prevPeriodTx = allTx.filter { it.date in prevFrom..prevTo }

        val income = currentPeriodTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = currentPeriodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevIncome = prevPeriodTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExpense = prevPeriodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val startLocal = Instant.ofEpochMilli(fromMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val ym = YearMonth.of(startLocal.year, startLocal.monthValue)
        val monthStart = ReportsCalculator.startOfDay(ym.atDay(1))
        val monthEnd = ReportsCalculator.endOfDay(ym.atEndOfMonth())
        val budgets = dao.getBudgetsForMonth(userId, ym.year, ym.monthValue)
        val alerts = budgets.mapNotNull { b ->
            val cat = cats[b.categoryId] ?: return@mapNotNull null
            val spent = allTx.filter {
                it.type == TransactionType.EXPENSE && it.categoryId == b.categoryId && it.date in monthStart..monthEnd
            }.sumOf { it.amount }
            val comp = BudgetGoalCalculator.getBudgetStatus(spent, b.amount, b.alertThreshold)
            if (comp.status == BudgetStatus.SAFE) null else BudgetWithStatus(
                budget = b,
                category = cat,
                spent = spent,
                percent = comp.percent,
                remaining = comp.remaining,
                status = comp.status
            )
        }

        val recent = allTx.take(5).mapNotNull { tx ->
            val account = accMap[tx.accountId] ?: return@mapNotNull null
            TransactionDetail(
                transaction = tx,
                account = account,
                toAccount = tx.toAccountId?.let { accMap[it] },
                category = tx.categoryId?.let { cats[it] },
                subCategory = tx.subCategoryId?.let { subs[it] },
                tags = txTags[tx.id].orEmpty().filterNotNull(),
                attachments = atts[tx.id].orEmpty()
            )
        }

        return ReportSummary(
            from = fromMillis,
            to = toMillis,
            netWorth = accounts.sumOf { it.balance },
            income = income,
            expense = expense,
            net = income - expense,
            changes = ReportChanges(
                income = ReportsCalculator.percentageChange(income, prevIncome),
                expense = ReportsCalculator.percentageChange(expense, prevExpense),
                net = ReportsCalculator.percentageChange(income - expense, prevIncome - prevExpense)
            ),
            previous = ReportPreviousTotals(
                income = prevIncome,
                expense = prevExpense,
                net = prevIncome - prevExpense
            ),
            budgetAlerts = alerts,
            recentTransactions = recent
        )
    }

    suspend fun buildCategoryBreakdown(
        userId: String,
        fromMillis: Long,
        toMillis: Long,
        type: TransactionType
    ): CategoryBreakdownReport {
        val txs = dao.getTransactions(userId).filter { it.type == type && it.date in fromMillis..toMillis }
        val cats = dao.getCategories(userId).associateBy { it.id }
        val total = txs.sumOf { it.amount }
        val grouped = txs.groupBy { it.categoryId }
        val items = grouped.map { (catId, list) ->
            val cat = catId?.let { cats[it] }
            val amount = list.sumOf { it.amount }
            CategoryBreakdownItem(
                categoryId = catId,
                name = cat?.name ?: "Tanpa kategori",
                icon = cat?.icon ?: "wallet",
                color = cat?.color ?: "#64748b",
                amount = amount,
                percent = if (total > 0) (amount / total) * 100.0 else 0.0
            )
        }.sortedByDescending { it.amount }
        return CategoryBreakdownReport(type = type, total = total, items = items)
    }

    suspend fun buildNetWorthReport(userId: String, months: Int = 12): NetWorthReport {
        val accounts = dao.getAccounts(userId)
        val txs = dao.getTransactions(userId)
        return NetWorthReport(
            currentNetWorth = accounts.sumOf { it.balance },
            items = ReportsCalculator.reconstructNetWorth(accounts, txs, months)
        )
    }

    // CSV Import, Export & JSON Backup/Restore
    suspend fun previewCsvImport(userId: String, csvContent: String): ImportPreviewResult {
        val accounts = dao.getAccounts(userId)
        return CsvAndBackupEngine.parseCsv(csvContent, accounts)
    }

    suspend fun commitCsvImport(userId: String, rows: List<ParsedImportRow>): ImportPreviewResult {
        var imported = 0
        val errors = mutableListOf<ImportRowError>()
        for (row in rows) {
            try {
                var catId: String? = null
                var subId: String? = null
                if (row.categoryName.isNotBlank()) {
                    val catType = CategoryType.valueOf(row.type.name)
                    val existingCat = dao.getCategories(userId).firstOrNull {
                        it.type == catType && it.name.equals(row.categoryName, ignoreCase = true)
                    }
                    catId = if (existingCat != null) {
                        existingCat.id
                    } else {
                        val newCatId = UUID.randomUUID().toString()
                        dao.insertCategory(
                            CategoryEntity(
                                id = newCatId,
                                userId = userId,
                                name = row.categoryName,
                                type = catType,
                                icon = "wallet",
                                color = "#64748b"
                            )
                        )
                        newCatId
                    }
                    if (row.subCategoryName.isNotBlank()) {
                        val existingSub = dao.getSubCategoriesForUser(userId).firstOrNull {
                            it.categoryId == catId && it.name.equals(row.subCategoryName, ignoreCase = true)
                        }
                        subId = if (existingSub != null) {
                            existingSub.id
                        } else {
                            val newSubId = UUID.randomUUID().toString()
                            dao.insertSubCategory(
                                SubCategoryEntity(
                                    id = newSubId,
                                    categoryId = catId,
                                    name = row.subCategoryName
                                )
                            )
                            newSubId
                        }
                    }
                }
                saveTransaction(
                    userId = userId,
                    editingId = null,
                    type = row.type,
                    amount = row.amount,
                    dateMillis = row.date,
                    accountId = row.accountId,
                    toAccountId = row.toAccountId,
                    categoryId = catId,
                    subCategoryId = subId,
                    note = row.note,
                    tagNames = row.tags
                )
                imported++
            } catch (e: Exception) {
                errors.add(ImportRowError(row.row, e.message ?: "Transaksi tidak dapat diimpor"))
            }
        }
        return ImportPreviewResult(
            total = rows.size,
            validRows = emptyList(),
            importedCount = imported,
            failed = errors.size,
            errors = errors
        )
    }

    suspend fun createBackupPayload(userId: String): BackupPayload {
        return BackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            data = BackupData(
                setting = dao.getSetting(userId),
                accounts = dao.getAccounts(userId),
                categories = dao.getCategories(userId),
                subCategories = dao.getSubCategoriesForUser(userId),
                transactions = dao.getTransactions(userId),
                tags = dao.getTags(userId),
                transactionTags = dao.getTransactionTagsForUser(userId),
                attachments = dao.getAttachments(userId),
                budgets = dao.getBudgets(userId),
                goals = dao.getGoals(userId),
                goalContributions = dao.getGoalContributionsForUser(userId),
                recurringTransactions = dao.getRecurringTransactions(userId)
            )
        )
    }

    suspend fun restoreBackupPayload(userId: String, payload: BackupPayload) {
        val data = payload.data
        db.withTransaction {
            dao.clearBudgets(userId)
            dao.clearGoals(userId)
            dao.clearRecurring(userId)
            dao.clearAttachments(userId)
            dao.clearTransactions(userId)
            dao.clearTags(userId)
            dao.clearCategories(userId)
            dao.clearAccounts(userId)

            val accIds = mutableMapOf<String, String>()
            for (acc in data.accounts) {
                val newId = UUID.randomUUID().toString()
                dao.insertAccount(acc.copy(id = newId, userId = userId, balance = acc.initialBalance))
                accIds[acc.id] = newId
            }

            val catIds = mutableMapOf<String, String>()
            for (cat in data.categories) {
                val newId = UUID.randomUUID().toString()
                dao.insertCategory(cat.copy(id = newId, userId = userId))
                catIds[cat.id] = newId
            }

            val subIds = mutableMapOf<String, String>()
            for (sub in data.subCategories) {
                val catId = catIds[sub.categoryId] ?: continue
                val newId = UUID.randomUUID().toString()
                dao.insertSubCategory(sub.copy(id = newId, categoryId = catId))
                subIds[sub.id] = newId
            }

            val tagIds = mutableMapOf<String, String>()
            for (tag in data.tags) {
                val newId = UUID.randomUUID().toString()
                dao.insertTag(tag.copy(id = newId, userId = userId))
                tagIds[tag.id] = newId
            }

            val recIds = mutableMapOf<String, String>()
            for (rule in data.recurringTransactions) {
                val accId = accIds[rule.accountId] ?: continue
                val newId = UUID.randomUUID().toString()
                dao.insertRecurringTransaction(
                    rule.copy(
                        id = newId,
                        userId = userId,
                        accountId = accId,
                        toAccountId = rule.toAccountId?.let { accIds[it] },
                        categoryId = rule.categoryId?.let { catIds[it] },
                        subCategoryId = rule.subCategoryId?.let { subIds[it] }
                    )
                )
                recIds[rule.id] = newId
            }

            val txIds = mutableMapOf<String, String>()
            for (tx in data.transactions) {
                val accId = accIds[tx.accountId] ?: continue
                val newId = UUID.randomUUID().toString()
                dao.insertTransaction(
                    tx.copy(
                        id = newId,
                        userId = userId,
                        accountId = accId,
                        toAccountId = tx.toAccountId?.let { accIds[it] },
                        categoryId = tx.categoryId?.let { catIds[it] },
                        subCategoryId = tx.subCategoryId?.let { subIds[it] },
                        recurringId = tx.recurringId?.let { recIds[it] }
                    )
                )
                txIds[tx.id] = newId
            }

            for (rel in data.transactionTags) {
                val txId = txIds[rel.transactionId] ?: continue
                val tagId = tagIds[rel.tagId] ?: continue
                dao.insertTransactionTag(TransactionTagCrossRef(txId, tagId))
            }

            for (b in data.budgets) {
                val catId = catIds[b.categoryId] ?: continue
                dao.insertBudget(b.copy(id = UUID.randomUUID().toString(), userId = userId, categoryId = catId))
            }

            val goalIds = mutableMapOf<String, String>()
            for (g in data.goals) {
                val newId = UUID.randomUUID().toString()
                dao.insertGoal(
                    g.copy(
                        id = newId,
                        userId = userId,
                        accountId = g.accountId?.let { accIds[it] }
                    )
                )
                goalIds[g.id] = newId
            }

            for (c in data.goalContributions) {
                val goalId = goalIds[c.goalId] ?: continue
                dao.insertGoalContribution(c.copy(id = UUID.randomUUID().toString(), goalId = goalId))
            }

            data.setting?.let { s ->
                val cur = dao.getSetting(userId)
                dao.upsertSetting(s.copy(id = cur?.id ?: UUID.randomUUID().toString(), userId = userId))
            }

            recalculateBalancesInTx(userId, accIds.values.toList())
        }
    }
}
