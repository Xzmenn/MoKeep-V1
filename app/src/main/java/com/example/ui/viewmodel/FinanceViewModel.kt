package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TagEntity
import com.example.data.model.AccountType
import com.example.data.model.BudgetStatus
import com.example.data.model.CategoryType
import com.example.data.model.PeriodPreset
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import com.example.domain.BudgetHistoryPoint
import com.example.domain.BudgetSummary
import com.example.domain.CategoryWithSubs
import com.example.domain.GoalWithProgress
import com.example.domain.RecurringRuleDetail
import com.example.domain.ReportsCalculator
import com.example.domain.TransactionDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

enum class TransactionSortField { DATE, AMOUNT }

data class TransactionFilterState(
    val searchQuery: String = "",
    val period: PeriodPreset = PeriodPreset.MONTH,
    val fromMillis: Long? = ReportsCalculator.getPeriodRange(PeriodPreset.MONTH).first,
    val toMillis: Long? = ReportsCalculator.getPeriodRange(PeriodPreset.MONTH).second,
    val type: TransactionType? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortField: TransactionSortField = TransactionSortField.DATE,
    val sortDesc: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.financeRepository
    private val authRepo = container.authRepository

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    private val userIdFlow = authRepo.currentUserId

    val accounts: StateFlow<List<AccountEntity>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeAccounts(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryWithSubs>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeCategoriesWithSubs(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeTags(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionDetail>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeTransactionDetails(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _txFilter = MutableStateFlow(TransactionFilterState())
    val txFilter = _txFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionDetail>> = combine(
        allTransactions,
        _txFilter
    ) { items, filter ->
        items.filter { item ->
            val tx = item.transaction
            val matchesType = filter.type == null || tx.type == filter.type
            val matchesAccount = filter.accountId == null || tx.accountId == filter.accountId || tx.toAccountId == filter.accountId
            val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesFrom = filter.fromMillis == null || tx.date >= filter.fromMillis
            val matchesTo = filter.toMillis == null || tx.date <= filter.toMillis
            val matchesMin = filter.minAmount == null || tx.amount >= filter.minAmount
            val matchesMax = filter.maxAmount == null || tx.amount <= filter.maxAmount
            val q = filter.searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                (tx.note?.contains(q, ignoreCase = true) == true) ||
                (item.category?.name?.contains(q, ignoreCase = true) == true) ||
                item.account.name.contains(q, ignoreCase = true) ||
                (item.toAccount?.name?.contains(q, ignoreCase = true) == true) ||
                item.tags.any { it.name.contains(q, ignoreCase = true) }

            matchesType && matchesAccount && matchesCategory && matchesFrom && matchesTo && matchesMin && matchesMax && matchesQuery
        }.let { filtered ->
            when (filter.sortField) {
                TransactionSortField.DATE -> if (filter.sortDesc) filtered.sortedByDescending { it.transaction.date } else filtered.sortedBy { it.transaction.date }
                TransactionSortField.AMOUNT -> if (filter.sortDesc) filtered.sortedByDescending { it.transaction.amount } else filtered.sortedBy { it.transaction.amount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalWithProgress>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeGoalsWithProgress(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringRuleDetail>> = userIdFlow
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repo.observeRecurringDetails(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _budgetMonth = MutableStateFlow(YearMonth.now())
    val budgetMonth = _budgetMonth.asStateFlow()

    val budgetSummary: StateFlow<BudgetSummary> = combine(userIdFlow, _budgetMonth) { uid, ym -> uid to ym }
        .flatMapLatest { (uid, ym) ->
            if (uid == null) {
                flowOf(BudgetSummary(emptyList(), ym.year, ym.monthValue, 0.0, 0.0, 0.0))
            } else {
                repo.observeBudgetsSummary(uid, ym.year, ym.monthValue)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BudgetSummary(emptyList(), YearMonth.now().year, YearMonth.now().monthValue, 0.0, 0.0, 0.0)
        )

    private val _budgetHistory = MutableStateFlow<List<BudgetHistoryPoint>>(emptyList())
    val budgetHistory = _budgetHistory.asStateFlow()

    // Filters
    fun updateFilter(transform: (TransactionFilterState) -> TransactionFilterState) {
        _txFilter.value = transform(_txFilter.value)
    }

    fun selectFilterPeriod(preset: PeriodPreset, weekStartsOn: Int = 1) {
        val (from, to) = ReportsCalculator.getPeriodRange(preset, weekStartsOn = weekStartsOn)
        _txFilter.value = _txFilter.value.copy(period = preset, fromMillis = from, toMillis = to)
    }

    fun resetFilters() {
        _txFilter.value = TransactionFilterState()
    }

    // Account actions
    fun saveAccount(
        userId: String,
        editingId: String?,
        name: String,
        type: AccountType,
        initialBalance: Double,
        creditLimit: Double?,
        color: String,
        icon: String,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.saveAccount(userId, editingId, name, type, initialBalance, creditLimit, color, icon, note)
                _messages.tryEmit("Akun berhasil disimpan")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Akun gagal disimpan")
            }
        }
    }

    fun archiveAccount(userId: String, id: String, archived: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.setAccountArchived(userId, id, archived)
                _messages.tryEmit(if (archived) "Akun diarsipkan" else "Arsip akun dibuka")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Gagal memperbarui akun")
            }
        }
    }

    fun deleteAccount(userId: String, id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteAccount(userId, id)
                _messages.tryEmit("Akun dihapus")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Akun gagal dihapus")
            }
        }
    }

    // Category actions
    fun saveCategory(
        userId: String,
        editingId: String?,
        name: String,
        type: CategoryType,
        icon: String,
        color: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.saveCategory(userId, editingId, name, type, icon, color)
                _messages.tryEmit("Kategori berhasil disimpan")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Kategori gagal disimpan")
            }
        }
    }

    fun deleteCategory(userId: String, id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteCategory(userId, id)
                _messages.tryEmit("Kategori dihapus")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Kategori gagal dihapus")
            }
        }
    }

    fun saveSubCategory(userId: String, categoryId: String, editingId: String?, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.saveSubCategory(userId, categoryId, editingId, name)
                _messages.tryEmit("Subkategori disimpan")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Subkategori gagal disimpan")
            }
        }
    }

    fun deleteSubCategory(id: String) {
        viewModelScope.launch {
            repo.deleteSubCategory(id)
            _messages.tryEmit("Subkategori dihapus")
        }
    }

    // Transaction & Recurring actions
    fun saveTransaction(
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
        tags: List<String>,
        attachmentUri: Uri?,
        repeat: Boolean,
        frequency: RecurrenceFrequency,
        interval: Int,
        endDateMillis: Long?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val (_, warnings) = repo.saveTransaction(
                    userId = userId,
                    editingId = editingId,
                    type = type,
                    amount = amount,
                    dateMillis = dateMillis,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    note = note,
                    tagNames = tags,
                    attachmentUri = attachmentUri
                )
                if (repeat && editingId == null) {
                    repo.createRecurringRule(
                        userId = userId,
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        categoryId = categoryId,
                        subCategoryId = subCategoryId,
                        note = note,
                        tagNames = tags,
                        frequency = frequency,
                        interval = interval,
                        startDateMillis = dateMillis,
                        endDateMillis = endDateMillis
                    )
                }
                _messages.tryEmit("Transaksi berhasil disimpan")
                warnings.forEach { w ->
                    val label = if (w.status == BudgetStatus.EXCEEDED) "Melebihi anggaran" else "Peringatan"
                    _messages.tryEmit("${w.categoryName}: ${w.percent}% ($label)")
                }
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Transaksi gagal disimpan")
            }
        }
    }

    fun deleteTransaction(userId: String, id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteTransaction(userId, id)
                _messages.tryEmit("Transaksi dihapus")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Transaksi gagal dihapus")
            }
        }
    }

    fun deleteAttachment(userId: String, attachmentId: String) {
        viewModelScope.launch {
            repo.deleteAttachment(userId, attachmentId)
            _messages.tryEmit("Lampiran dihapus")
        }
    }

    fun updateRecurringRule(
        userId: String,
        id: String,
        amount: Double,
        frequency: RecurrenceFrequency,
        interval: Int,
        endDateMillis: Long?,
        note: String?,
        isActive: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.updateRecurringRule(userId, id, amount, frequency, interval, endDateMillis, note, isActive)
                _messages.tryEmit("Aturan transaksi berulang diperbarui")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Aturan gagal diperbarui")
            }
        }
    }

    fun toggleRecurring(userId: String, id: String) {
        viewModelScope.launch {
            repo.toggleRecurringActive(userId, id)
        }
    }

    fun deleteRecurring(userId: String, id: String) {
        viewModelScope.launch {
            repo.deleteRecurringRule(userId, id)
            _messages.tryEmit("Aturan transaksi berulang dihapus")
        }
    }

    fun runRecurringNow(userId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val count = repo.processDueRecurring(userId)
            _messages.tryEmit(
                if (count > 0) "$count transaksi berulang berhasil dijalankan"
                else "Tidak ada jadwal transaksi berulang yang jatuh tempo"
            )
            onDone()
        }
    }

    // Budget actions
    fun moveBudgetMonth(offset: Int) {
        _budgetMonth.value = _budgetMonth.value.plusMonths(offset.toLong())
    }

    fun loadBudgetHistory(userId: String, categoryId: String) {
        val ym = _budgetMonth.value
        viewModelScope.launch {
            _budgetHistory.value = repo.getBudgetHistory(userId, categoryId, ym.year, ym.monthValue, 6)
        }
    }

    fun saveBudget(
        userId: String,
        editingId: String?,
        categoryId: String,
        amount: Double,
        threshold: Int,
        note: String?,
        onDone: () -> Unit
    ) {
        val ym = _budgetMonth.value
        viewModelScope.launch {
            try {
                repo.saveBudget(userId, editingId, categoryId, ym.year, ym.monthValue, amount, threshold, note)
                _messages.tryEmit("Anggaran berhasil disimpan")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Anggaran gagal disimpan")
            }
        }
    }

    fun deleteBudget(userId: String, id: String) {
        viewModelScope.launch {
            repo.deleteBudget(userId, id)
            _messages.tryEmit("Anggaran dihapus")
        }
    }

    fun copyBudgetsFromPreviousMonth(userId: String) {
        val ym = _budgetMonth.value
        viewModelScope.launch {
            val (copied, skipped) = repo.copyBudgetsFromPreviousMonth(userId, ym.year, ym.monthValue)
            _messages.tryEmit(
                if (copied > 0) {
                    "$copied anggaran disalin" + if (skipped > 0) ", $skipped dilewati" else ""
                } else {
                    "Tidak ada anggaran baru untuk disalin"
                }
            )
        }
    }

    // Goal actions
    fun saveGoal(
        userId: String,
        editingId: String?,
        name: String,
        targetAmount: Double,
        deadlineMillis: Long?,
        accountId: String?,
        color: String,
        icon: String,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.saveGoal(userId, editingId, name, targetAmount, deadlineMillis, accountId, color, icon, note)
                _messages.tryEmit("Target berhasil disimpan")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Target gagal disimpan")
            }
        }
    }

    fun deleteGoal(userId: String, id: String) {
        viewModelScope.launch {
            repo.deleteGoal(userId, id)
            _messages.tryEmit("Target dihapus")
        }
    }

    fun addGoalContribution(
        userId: String,
        goalId: String,
        isWithdrawal: Boolean,
        amount: Double,
        dateMillis: Long,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.addGoalContribution(userId, goalId, isWithdrawal, amount, dateMillis, note)
                _messages.tryEmit(if (isWithdrawal) "Penarikan dicatat" else "Setoran dicatat")
                onDone()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Kontribusi gagal disimpan")
            }
        }
    }

    fun deleteGoalContribution(userId: String, goalId: String, contributionId: String) {
        viewModelScope.launch {
            repo.deleteGoalContribution(userId, goalId, contributionId)
            _messages.tryEmit("Kontribusi dihapus")
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceViewModel(container) as T
        }
    }
}
