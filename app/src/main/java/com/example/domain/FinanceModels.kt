package com.example.domain

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AttachmentEntity
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
import com.example.data.model.BudgetStatus
import com.example.data.model.TransactionType
import kotlinx.serialization.Serializable

data class CategoryWithSubs(
    val category: CategoryEntity,
    val subCategories: List<SubCategoryEntity>
)

data class TransactionDetail(
    val transaction: TransactionEntity,
    val account: AccountEntity,
    val toAccount: AccountEntity?,
    val category: CategoryEntity?,
    val subCategory: SubCategoryEntity?,
    val tags: List<TagEntity>,
    val attachments: List<AttachmentEntity>
)

data class BudgetAlertWarning(
    val categoryName: String,
    val percent: Int,
    val status: BudgetStatus
)

data class BudgetWithStatus(
    val budget: BudgetEntity,
    val category: CategoryEntity,
    val spent: Double,
    val percent: Int,
    val remaining: Double,
    val status: BudgetStatus
)

data class BudgetSummary(
    val items: List<BudgetWithStatus>,
    val year: Int,
    val month: Int,
    val totalBudget: Double,
    val totalSpent: Double,
    val totalRemaining: Double
)

data class BudgetHistoryPoint(
    val year: Int,
    val month: Int,
    val label: String,
    val budget: Double,
    val spent: Double
)

data class GoalWithProgress(
    val goal: GoalEntity,
    val account: AccountEntity?,
    val contributions: List<GoalContributionEntity>,
    val saved: Double,
    val percent: Int,
    val remaining: Double,
    val monthlyAverage: Double,
    val estimatedDate: Long?,
    val requiredPerMonth: Double?,
    val onTrack: Boolean
)

data class RecurringRuleDetail(
    val rule: RecurringTransactionEntity,
    val account: AccountEntity?,
    val toAccount: AccountEntity?,
    val category: CategoryEntity?
)

data class ReportChanges(
    val income: Double,
    val expense: Double,
    val net: Double
)

data class ReportPreviousTotals(
    val income: Double,
    val expense: Double,
    val net: Double
)

data class ReportSummary(
    val from: Long,
    val to: Long,
    val netWorth: Double,
    val income: Double,
    val expense: Double,
    val net: Double,
    val changes: ReportChanges,
    val previous: ReportPreviousTotals,
    val budgetAlerts: List<BudgetWithStatus>,
    val recentTransactions: List<TransactionDetail>
)

data class CategoryBreakdownItem(
    val categoryId: String?,
    val name: String,
    val icon: String,
    val color: String,
    val amount: Double,
    val percent: Double
)

data class CategoryBreakdownReport(
    val type: TransactionType,
    val total: Double,
    val items: List<CategoryBreakdownItem>
)

enum class CashflowBucket {
    DAY, WEEK, MONTH
}

data class CashflowPoint(
    val key: String,
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val runningNet: Double
)

data class CashflowReport(
    val from: Long,
    val to: Long,
    val bucket: CashflowBucket,
    val items: List<CashflowPoint>
)

data class NetWorthPoint(
    val month: String,
    val label: String,
    val assets: Double,
    val liabilities: Double,
    val netWorth: Double
)

data class NetWorthReport(
    val currentNetWorth: Double,
    val items: List<NetWorthPoint>
)

data class ImportRowError(
    val row: Int,
    val message: String
)

data class ParsedImportRow(
    val row: Int,
    val type: TransactionType,
    val amount: Double,
    val date: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryName: String,
    val subCategoryName: String,
    val note: String,
    val tags: List<String>
)

data class ImportPreviewResult(
    val total: Int,
    val validRows: List<ParsedImportRow>,
    val importedCount: Int?,
    val failed: Int,
    val errors: List<ImportRowError>
)

@Serializable
data class BackupData(
    val setting: SettingEntity? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val subCategories: List<SubCategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val transactionTags: List<TransactionTagCrossRef> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val recurringTransactions: List<RecurringTransactionEntity> = emptyList()
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val data: BackupData
)
