package com.example.ui.viewmodel

import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.PeriodPreset
import com.example.data.model.TransactionType
import com.example.domain.CashflowReport
import com.example.domain.CategoryBreakdownReport
import com.example.domain.CsvAndBackupEngine
import com.example.domain.ImportPreviewResult
import com.example.domain.NetWorthReport
import com.example.domain.ReportSummary
import com.example.domain.ReportsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Unauthenticated : SessionUiState
    data class Authenticated(
        val user: UserEntity,
        val setting: SettingEntity
    ) : SessionUiState
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: ReportSummary? = null,
    val monthCashflow: CashflowReport? = null,
    val expenseCategories: CategoryBreakdownReport? = null,
    val sixMonthTrends: CashflowReport? = null
)

data class ReportsUiState(
    val preset: PeriodPreset = PeriodPreset.MONTH,
    val fromMillis: Long = ReportsCalculator.getPeriodRange(PeriodPreset.MONTH).first,
    val toMillis: Long = ReportsCalculator.getPeriodRange(PeriodPreset.MONTH).second,
    val categoryType: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = true,
    val summary: ReportSummary? = null,
    val cashflow: CashflowReport? = null,
    val categoryBreakdown: CategoryBreakdownReport? = null,
    val netWorth: NetWorthReport? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val authRepo = container.authRepository
    private val financeRepo = container.financeRepository
    private val dao = container.database.dao()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    val sessionState: StateFlow<SessionUiState> = authRepo.firebaseUserFlow
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(SessionUiState.Unauthenticated)
            } else {
                flow {
                    val provisioned = authRepo.ensureGoogleUserProvisioned(firebaseUser)
                    emit(provisioned)
                }.flatMapLatest { provisioned ->
                    combine(
                        authRepo.observeUser(provisioned.id),
                        authRepo.observeSetting(provisioned.id)
                    ) { user, setting ->
                        SessionUiState.Authenticated(
                            user = user ?: provisioned,
                            setting = setting ?: SettingEntity(id = "default", userId = provisioned.id)
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Loading)

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState = _dashboardState.asStateFlow()

    private val _reportsState = MutableStateFlow(ReportsUiState())
    val reportsState = _reportsState.asStateFlow()

    private val _importPreview = MutableStateFlow<ImportPreviewResult?>(null)
    val importPreview = _importPreview.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.collect { state ->
                if (state is SessionUiState.Authenticated) {
                    val created = financeRepo.processDueRecurring(state.user.id)
                    if (created > 0) {
                        _messages.tryEmit("$created transaksi berulang otomatis dicatat")
                    }
                    refreshDashboardAndReports(state.user.id)
                }
            }
        }
    }

    fun refreshDashboardAndReports(userId: String) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val nowMillis = System.currentTimeMillis()
            val monthStart = ReportsCalculator.startOfDay(today.withDayOfMonth(1))
            val sixMonthStart = ReportsCalculator.startOfDay(today.minusMonths(5).withDayOfMonth(1))

            val summary = financeRepo.buildReportSummary(userId, monthStart, nowMillis)
            val allTx = dao.getTransactions(userId)
            val (mBucket, mPoints) = ReportsCalculator.bucketCashflow(allTx, monthStart, nowMillis)
            val (tBucket, tPoints) = ReportsCalculator.bucketCashflow(allTx, sixMonthStart, nowMillis)
            val expCats = financeRepo.buildCategoryBreakdown(userId, monthStart, nowMillis, TransactionType.EXPENSE)

            _dashboardState.value = DashboardUiState(
                isLoading = false,
                summary = summary,
                monthCashflow = CashflowReport(monthStart, nowMillis, mBucket, mPoints),
                expenseCategories = expCats,
                sixMonthTrends = CashflowReport(sixMonthStart, nowMillis, tBucket, tPoints)
            )

            val rep = _reportsState.value
            val rSummary = financeRepo.buildReportSummary(userId, rep.fromMillis, rep.toMillis)
            val (rBucket, rPoints) = ReportsCalculator.bucketCashflow(allTx, rep.fromMillis, rep.toMillis)
            val rCats = financeRepo.buildCategoryBreakdown(userId, rep.fromMillis, rep.toMillis, rep.categoryType)
            val rNetWorth = financeRepo.buildNetWorthReport(userId, 12)
            _reportsState.value = rep.copy(
                isLoading = false,
                summary = rSummary,
                cashflow = CashflowReport(rep.fromMillis, rep.toMillis, rBucket, rPoints),
                categoryBreakdown = rCats,
                netWorth = rNetWorth
            )
        }
    }

    fun seedSampleData(userId: String) {
        viewModelScope.launch {
            container.seeder.seedSampleDataForUser(userId)
            refreshDashboardAndReports(userId)
            _messages.tryEmit("Data contoh berhasil ditambahkan")
        }
    }

    fun selectReportPeriod(userId: String, preset: PeriodPreset, weekStartsOn: Int = 1) {
        val (from, to) = ReportsCalculator.getPeriodRange(preset, weekStartsOn = weekStartsOn)
        _reportsState.value = _reportsState.value.copy(
            preset = preset,
            fromMillis = from,
            toMillis = to,
            isLoading = true
        )
        refreshDashboardAndReports(userId)
    }

    fun setCustomReportRange(userId: String, fromMillis: Long, toMillis: Long) {
        val (from, to) = ReportsCalculator.getPeriodRange(
            PeriodPreset.CUSTOM,
            customFrom = fromMillis,
            customTo = toMillis
        )
        _reportsState.value = _reportsState.value.copy(
            preset = PeriodPreset.CUSTOM,
            fromMillis = from,
            toMillis = to,
            isLoading = true
        )
        refreshDashboardAndReports(userId)
    }

    fun selectReportCategoryType(userId: String, type: TransactionType) {
        _reportsState.value = _reportsState.value.copy(categoryType = type)
        refreshDashboardAndReports(userId)
    }

    fun signOut(credentialManager: CredentialManager) {
        viewModelScope.launch {
            authRepo.signOut(credentialManager)
        }
    }

    fun notifyMessage(message: String) {
        _messages.tryEmit(message)
    }

    // Profile & Settings
    fun updateProfile(userId: String, name: String) {
        viewModelScope.launch {
            try {
                authRepo.updateProfile(userId, name)
                _messages.tryEmit("Profil berhasil diperbarui")
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Profil gagal diperbarui")
            }
        }
    }

    fun uploadAvatar(userId: String, uri: Uri) {
        viewModelScope.launch {
            try {
                authRepo.saveAvatarFromUri(userId, uri)
                _messages.tryEmit("Foto profil berhasil diperbarui")
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Foto profil gagal diperbarui")
            }
        }
    }

    fun updateSettings(
        userId: String,
        currency: String,
        locale: String,
        theme: String,
        weekStartsOn: Int,
        budgetAlertThreshold: Int
    ) {
        viewModelScope.launch {
            authRepo.updateSettings(userId, currency, locale, theme, weekStartsOn, budgetAlertThreshold)
            _messages.tryEmit("Preferensi berhasil disimpan")
        }
    }

    // Export / Import / Backup
    suspend fun buildCsvExport(userId: String, fromMillis: Long? = null, toMillis: Long? = null): String {
        val accounts = dao.getAccounts(userId).associateBy { it.id }
        val cats = dao.getCategories(userId).associateBy { it.id }
        val subs = dao.getSubCategoriesForUser(userId).associateBy { it.id }
        val tags = dao.getTags(userId).associateBy { it.id }
        val txTags = dao.getTransactionTagsForUser(userId).groupBy({ it.transactionId }, { tags[it.tagId] })
        val txs = dao.getTransactions(userId).filter { tx ->
            (fromMillis == null || tx.date >= fromMillis) && (toMillis == null || tx.date <= toMillis)
        }.mapNotNull { tx ->
            val acc = accounts[tx.accountId] ?: return@mapNotNull null
            com.example.domain.TransactionDetail(
                transaction = tx,
                account = acc,
                toAccount = tx.toAccountId?.let { accounts[it] },
                category = tx.categoryId?.let { cats[it] },
                subCategory = tx.subCategoryId?.let { subs[it] },
                tags = txTags[tx.id].orEmpty().filterNotNull(),
                attachments = emptyList()
            )
        }
        return CsvAndBackupEngine.exportTransactionsCsv(txs)
    }

    suspend fun buildPdfExport(
        userId: String,
        fromMillis: Long?,
        toMillis: Long?,
        currency: String,
        locale: String
    ): ByteArray {
        val accounts = dao.getAccounts(userId).associateBy { it.id }
        val cats = dao.getCategories(userId).associateBy { it.id }
        val subs = dao.getSubCategoriesForUser(userId).associateBy { it.id }
        val txs = dao.getTransactions(userId).filter { tx ->
            (fromMillis == null || tx.date >= fromMillis) && (toMillis == null || tx.date <= toMillis)
        }.mapNotNull { tx ->
            val acc = accounts[tx.accountId] ?: return@mapNotNull null
            com.example.domain.TransactionDetail(
                transaction = tx,
                account = acc,
                toAccount = tx.toAccountId?.let { accounts[it] },
                category = tx.categoryId?.let { cats[it] },
                subCategory = tx.subCategoryId?.let { subs[it] },
                tags = emptyList(),
                attachments = emptyList()
            )
        }
        return CsvAndBackupEngine.generateReportPdfBytes(txs, fromMillis, toMillis, currency, locale)
    }

    fun previewCsv(userId: String, csvContent: String) {
        viewModelScope.launch {
            val result = financeRepo.previewCsvImport(userId, csvContent)
            _importPreview.value = result
            _messages.tryEmit("Pratinjau selesai: ${result.validRows.size} baris valid")
        }
    }

    fun commitCsv(userId: String) {
        val preview = _importPreview.value ?: return
        if (preview.validRows.isEmpty()) return
        viewModelScope.launch {
            val result = financeRepo.commitCsvImport(userId, preview.validRows)
            _importPreview.value = result
            refreshDashboardAndReports(userId)
            _messages.tryEmit("${result.importedCount ?: 0} transaksi berhasil diimpor")
        }
    }

    fun clearImportPreview() {
        _importPreview.value = null
    }

    suspend fun buildBackupJson(userId: String): String {
        val payload = financeRepo.createBackupPayload(userId)
        return CsvAndBackupEngine.encodeBackup(payload)
    }

    fun restoreBackupJson(userId: String, rawJson: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val payload = CsvAndBackupEngine.decodeBackup(rawJson)
                financeRepo.restoreBackupPayload(userId, payload)
                refreshDashboardAndReports(userId)
                _messages.tryEmit("Cadangan berhasil dipulihkan")
                onSuccess()
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Pemulihan cadangan gagal")
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
