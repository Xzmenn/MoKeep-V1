package com.example.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.TransactionType
import com.example.domain.CsvAndBackupEngine
import com.example.domain.RecurringRuleDetail
import com.example.domain.TransactionDetail
import com.example.ui.screens.AccountsCategoriesScreen
import com.example.ui.screens.AuthFlowScreen
import com.example.ui.screens.BudgetsGoalsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RecurringEditDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionFormSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SessionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data object RouteDashboard

@Serializable
data object RouteTransactions

@Serializable
data class RouteAccountsCategories(val initialTab: Int = 0)

@Serializable
data class RouteBudgetsGoals(val initialTab: Int = 0)

@Serializable
data object RouteReports

@Serializable
data object RouteSettings

private data class NavTabItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val isSelected: (androidx.navigation.NavDestination?) -> Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFintechRoot(
    mainViewModel: MainViewModel,
    financeViewModel: FinanceViewModel
) {
    val session by mainViewModel.sessionState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        merge(mainViewModel.messages, financeViewModel.messages).collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    when (val current = session) {
        SessionUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SessionUiState.Unauthenticated -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AuthFlowScreen(
                        onAuthSuccess = {
                            mainViewModel.notifyMessage("Berhasil masuk dengan Akun Google")
                        }
                    )
                }
            }
        }
        is SessionUiState.Authenticated -> {
            key(current.user.id) {
                AuthenticatedShell(
                    state = current,
                    mainViewModel = mainViewModel,
                    financeViewModel = financeViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    state: SessionUiState.Authenticated,
    mainViewModel: MainViewModel,
    financeViewModel: FinanceViewModel,
    snackbarHostState: SnackbarHostState
) {
    val user = state.user
    val setting = state.setting
    val currency = setting.currency
    val locale = setting.locale

    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val dashboardState by mainViewModel.dashboardState.collectAsStateWithLifecycle()
    val reportsState by mainViewModel.reportsState.collectAsStateWithLifecycle()
    val importPreview by mainViewModel.importPreview.collectAsStateWithLifecycle()

    val accounts by financeViewModel.accounts.collectAsStateWithLifecycle()
    val categories by financeViewModel.categories.collectAsStateWithLifecycle()
    val tags by financeViewModel.tags.collectAsStateWithLifecycle()
    val txFilter by financeViewModel.txFilter.collectAsStateWithLifecycle()
    val transactions by financeViewModel.filteredTransactions.collectAsStateWithLifecycle()
    val recurringRules by financeViewModel.recurringRules.collectAsStateWithLifecycle()
    val budgetSummary by financeViewModel.budgetSummary.collectAsStateWithLifecycle()
    val budgetHistory by financeViewModel.budgetHistory.collectAsStateWithLifecycle()
    val goals by financeViewModel.goals.collectAsStateWithLifecycle()

    var showTxSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionDetail?>(null) }
    var presetTxType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var presetTxAccountId by remember { mutableStateOf<String?>(null) }
    var editingRecurring by remember { mutableStateOf<RecurringRuleDetail?>(null) }

    fun navigateSingleTop(route: Any) {
        navController.navigate(route) {
            popUpTo(RouteDashboard) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val primaryTabs = listOf(
        NavTabItem(
            label = "Beranda",
            icon = Icons.Filled.Dashboard,
            testTag = "nav_dashboard",
            isSelected = { it?.hasRoute(RouteDashboard::class) == true },
            onClick = { navigateSingleTop(RouteDashboard) }
        ),
        NavTabItem(
            label = "Transaksi",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            testTag = "nav_transactions",
            isSelected = { it?.hasRoute(RouteTransactions::class) == true },
            onClick = { navigateSingleTop(RouteTransactions) }
        ),
        NavTabItem(
            label = "Akun",
            icon = Icons.Filled.AccountBalanceWallet,
            testTag = "nav_accounts",
            isSelected = { it?.hasRoute(RouteAccountsCategories::class) == true },
            onClick = { navigateSingleTop(RouteAccountsCategories(0)) }
        ),
        NavTabItem(
            label = "Rencana",
            icon = Icons.Filled.Savings,
            testTag = "nav_budgets_goals",
            isSelected = { it?.hasRoute(RouteBudgetsGoals::class) == true },
            onClick = { navigateSingleTop(RouteBudgetsGoals(0)) }
        ),
        NavTabItem(
            label = "Laporan",
            icon = Icons.Filled.BarChart,
            testTag = "nav_reports",
            isSelected = { it?.hasRoute(RouteReports::class) == true },
            onClick = { navigateSingleTop(RouteReports) }
        )
    )

    val topBarTitle = when {
        currentDestination?.hasRoute(RouteTransactions::class) == true -> "Transaksi"
        currentDestination?.hasRoute(RouteAccountsCategories::class) == true -> "Akun & Kategori"
        currentDestination?.hasRoute(RouteBudgetsGoals::class) == true -> "Anggaran & Target"
        currentDestination?.hasRoute(RouteReports::class) == true -> "Laporan"
        currentDestination?.hasRoute(RouteSettings::class) == true -> "Pengaturan"
        else -> "MoKeep"
    }

    fun openNewTransaction(type: TransactionType = TransactionType.EXPENSE, accountId: String? = null) {
        editingTx = null
        presetTxType = type
        presetTxAccountId = accountId
        showTxSheet = true
    }

    fun writeTextToUri(uri: Uri, content: String, successMsg: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    } ?: error("Gagal membuka file tujuan")
                }
                snackbarHostState.showSnackbar(successMsg)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Gagal menyimpan file")
            }
        }
    }

    fun writeBytesToUri(uri: Uri, bytes: ByteArray, successMsg: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                    } ?: error("Gagal membuka file tujuan")
                }
                snackbarHostState.showSnackbar(successMsg)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Gagal menyimpan file")
            }
        }
    }

    fun readTextFromUri(uri: Uri, onLoaded: (String) -> Unit) {
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("File tidak dapat dibaca")
                }
                onLoaded(text)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Gagal membaca file")
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWide,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "MoKeep",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Text(
                        user.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    primaryTabs.forEach { tab ->
                        NavigationDrawerItem(
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            selected = tab.isSelected(currentDestination),
                            onClick = {
                                scope.launch { drawerState.close() }
                                tab.onClick()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                        label = { Text("Kategori") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(RouteAccountsCategories(initialTab = 1)) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Pengaturan") },
                        selected = currentDestination?.hasRoute(RouteSettings::class) == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateSingleTop(RouteSettings)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(
                        header = {
                            FloatingActionButton(
                                onClick = { openNewTransaction() },
                                modifier = Modifier.padding(top = 8.dp).testTag("rail_fab_add_tx")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Tambah transaksi")
                            }
                        }
                    ) {
                        primaryTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = tab.isSelected(currentDestination),
                                onClick = tab.onClick,
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = currentDestination?.hasRoute(RouteSettings::class) == true,
                            onClick = { navigateSingleTop(RouteSettings) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Pengaturan") },
                            label = { Text("Setelan") }
                        )
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                if (!isWide) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu navigasi")
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { navigateSingleTop(RouteSettings) },
                                    modifier = Modifier.testTag("topbar_settings_button")
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isWide) {
                            NavigationBar {
                                primaryTabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = tab.isSelected(currentDestination),
                                        onClick = tab.onClick,
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (!isWide) {
                            FloatingActionButton(
                                onClick = { openNewTransaction() },
                                modifier = Modifier.testTag("fab_add_transaction")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Tambah transaksi")
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = RouteDashboard,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        composable<RouteDashboard> {
                            DashboardScreen(
                                state = dashboardState,
                                accounts = accounts,
                                goals = goals,
                                currency = currency,
                                locale = locale,
                                onNavigateAccounts = { navigateSingleTop(RouteAccountsCategories(0)) },
                                onNavigateTransactions = { navigateSingleTop(RouteTransactions) },
                                onNavigateBudgets = { navigateSingleTop(RouteBudgetsGoals(0)) },
                                onNavigateGoals = {
                                    navController.navigate(RouteBudgetsGoals(initialTab = 1)) {
                                        launchSingleTop = true
                                    }
                                },
                                onSeedSampleData = { mainViewModel.seedSampleData(user.id) }
                            )
                        }
                        composable<RouteTransactions> {
                            TransactionsScreen(
                                transactions = transactions,
                                recurringRules = recurringRules,
                                filter = txFilter,
                                accounts = accounts,
                                categories = categories,
                                currency = currency,
                                locale = locale,
                                weekStartsOn = setting.weekStartsOn,
                                onFilterChange = financeViewModel::updateFilter,
                                onSelectPeriod = financeViewModel::selectFilterPeriod,
                                onResetFilter = financeViewModel::resetFilters,
                                onAddTransaction = { openNewTransaction() },
                                onEditTransaction = { detail ->
                                    editingTx = detail
                                    showTxSheet = true
                                },
                                onDeleteTransaction = { txId ->
                                    financeViewModel.deleteTransaction(user.id, txId) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onEditRecurring = { editingRecurring = it },
                                onToggleRecurring = { financeViewModel.toggleRecurring(user.id, it) },
                                onDeleteRecurring = { financeViewModel.deleteRecurring(user.id, it) },
                                onRunRecurringNow = {
                                    financeViewModel.runRecurringNow(user.id) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                }
                            )
                        }
                        composable<RouteAccountsCategories> { entry ->
                            val args = entry.toRoute<RouteAccountsCategories>()
                            AccountsCategoriesScreen(
                                initialTab = args.initialTab,
                                accounts = accounts,
                                categories = categories,
                                currency = currency,
                                locale = locale,
                                onSaveAccount = { editId, name, type, initBal, limit, color, icon, note ->
                                    financeViewModel.saveAccount(
                                        user.id, editId, name, type, initBal, limit, color, icon, note
                                    ) { mainViewModel.refreshDashboardAndReports(user.id) }
                                },
                                onArchiveAccount = { id, archived ->
                                    financeViewModel.archiveAccount(user.id, id, archived) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onDeleteAccount = { id ->
                                    financeViewModel.deleteAccount(user.id, id) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onQuickTransfer = { accId ->
                                    openNewTransaction(TransactionType.TRANSFER, accId)
                                },
                                onSaveCategory = { editId, name, type, icon, color ->
                                    financeViewModel.saveCategory(user.id, editId, name, type, icon, color) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onDeleteCategory = { id ->
                                    financeViewModel.deleteCategory(user.id, id) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onSaveSubCategory = { catId, editId, name ->
                                    financeViewModel.saveSubCategory(user.id, catId, editId, name) {}
                                },
                                onDeleteSubCategory = financeViewModel::deleteSubCategory
                            )
                        }
                        composable<RouteBudgetsGoals> { entry ->
                            val args = entry.toRoute<RouteBudgetsGoals>()
                            BudgetsGoalsScreen(
                                initialTab = args.initialTab,
                                budgetSummary = budgetSummary,
                                budgetHistory = budgetHistory,
                                goals = goals,
                                accounts = accounts,
                                categories = categories,
                                defaultBudgetThreshold = setting.budgetAlertThreshold,
                                currency = currency,
                                locale = locale,
                                onMoveBudgetMonth = financeViewModel::moveBudgetMonth,
                                onCopyPreviousMonth = { financeViewModel.copyBudgetsFromPreviousMonth(user.id) },
                                onLoadBudgetHistory = { catId -> financeViewModel.loadBudgetHistory(user.id, catId) },
                                onSaveBudget = { editId, catId, amount, threshold, note ->
                                    financeViewModel.saveBudget(user.id, editId, catId, amount, threshold, note) {
                                        mainViewModel.refreshDashboardAndReports(user.id)
                                    }
                                },
                                onDeleteBudget = { id -> financeViewModel.deleteBudget(user.id, id) },
                                onSaveGoal = { editId, name, target, deadline, accId, color, icon, note ->
                                    financeViewModel.saveGoal(user.id, editId, name, target, deadline, accId, color, icon, note) {}
                                },
                                onDeleteGoal = { id -> financeViewModel.deleteGoal(user.id, id) },
                                onAddContribution = { goalId, isWithdrawal, amount, dateMillis, note ->
                                    financeViewModel.addGoalContribution(user.id, goalId, isWithdrawal, amount, dateMillis, note) {}
                                },
                                onDeleteContribution = { goalId, contribId ->
                                    financeViewModel.deleteGoalContribution(user.id, goalId, contribId)
                                }
                            )
                        }
                        composable<RouteReports> {
                            ReportsScreen(
                                state = reportsState,
                                currency = currency,
                                locale = locale,
                                weekStartsOn = setting.weekStartsOn,
                                onSelectPeriod = { preset, weekStart ->
                                    mainViewModel.selectReportPeriod(user.id, preset, weekStart)
                                },
                                onSetCustomRange = { from, to ->
                                    mainViewModel.setCustomReportRange(user.id, from, to)
                                },
                                onSelectCategoryType = { type ->
                                    mainViewModel.selectReportCategoryType(user.id, type)
                                },
                                onExportCsv = { uri ->
                                    scope.launch {
                                        val csv = mainViewModel.buildCsvExport(user.id, reportsState.fromMillis, reportsState.toMillis)
                                        writeTextToUri(uri, csv, "Laporan CSV tersimpan")
                                    }
                                },
                                onExportPdf = { uri ->
                                    scope.launch {
                                        val pdf = withContext(Dispatchers.Default) {
                                            mainViewModel.buildPdfExport(
                                                user.id, reportsState.fromMillis, reportsState.toMillis, currency, locale
                                            )
                                        }
                                        writeBytesToUri(uri, pdf, "Laporan PDF tersimpan")
                                    }
                                }
                            )
                        }
                        composable<RouteSettings> {
                            SettingsScreen(
                                user = user,
                                setting = setting,
                                importPreview = importPreview,
                                onUpdateProfile = { name -> mainViewModel.updateProfile(user.id, name) },
                                onUploadAvatar = { uri -> mainViewModel.uploadAvatar(user.id, uri) },
                                onLogout = { mainViewModel.signOut(credentialManager) },
                                onUpdateSettings = { cur, loc, thm, wk, th ->
                                    mainViewModel.updateSettings(user.id, cur, loc, thm, wk, th)
                                },
                                onExportCsv = { uri ->
                                    scope.launch {
                                        val csv = mainViewModel.buildCsvExport(user.id)
                                        writeTextToUri(uri, csv, "Ekspor CSV berhasil")
                                    }
                                },
                                onExportPdf = { uri ->
                                    scope.launch {
                                        val pdf = withContext(Dispatchers.Default) {
                                            mainViewModel.buildPdfExport(user.id, null, null, currency, locale)
                                        }
                                        writeBytesToUri(uri, pdf, "Ekspor PDF berhasil")
                                    }
                                },
                                onExportTemplateCsv = { uri ->
                                    writeTextToUri(uri, CsvAndBackupEngine.templateCsv(), "Templat CSV tersimpan")
                                },
                                onExportBackupJson = { uri ->
                                    scope.launch {
                                        val json = mainViewModel.buildBackupJson(user.id)
                                        writeTextToUri(uri, json, "Cadangan JSON tersimpan")
                                    }
                                },
                                onPreviewCsvUri = { uri ->
                                    readTextFromUri(uri) { content -> mainViewModel.previewCsv(user.id, content) }
                                },
                                onCommitCsvImport = { mainViewModel.commitCsv(user.id) },
                                onClearImportPreview = mainViewModel::clearImportPreview,
                                onRestoreBackupUri = { uri, done ->
                                    readTextFromUri(uri) { raw ->
                                        mainViewModel.restoreBackupJson(user.id, raw, done)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTxSheet) {
        TransactionFormSheet(
            editing = editingTx,
            presetType = presetTxType,
            presetAccountId = presetTxAccountId,
            accounts = accounts,
            categories = categories,
            tagSuggestions = tags,
            currency = currency,
            locale = locale,
            onDismiss = { showTxSheet = false },
            onDeleteAttachment = { attId -> financeViewModel.deleteAttachment(user.id, attId) },
            onSave = { type, amount, dateMillis, accountId, toAccountId, catId, subCatId, note, txTags, attUri, repeat, freq, interval, endDate ->
                financeViewModel.saveTransaction(
                    userId = user.id,
                    editingId = editingTx?.transaction?.id,
                    type = type,
                    amount = amount,
                    dateMillis = dateMillis,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = catId,
                    subCategoryId = subCatId,
                    note = note,
                    tags = txTags,
                    attachmentUri = attUri,
                    repeat = repeat,
                    frequency = freq,
                    interval = interval,
                    endDateMillis = endDate
                ) {
                    showTxSheet = false
                    mainViewModel.refreshDashboardAndReports(user.id)
                }
            }
        )
    }

    editingRecurring?.let { item ->
        RecurringEditDialog(
            rule = item,
            currency = currency,
            onDismiss = { editingRecurring = null },
            onSave = { amount, freq, interval, note, isActive ->
                financeViewModel.updateRecurringRule(
                    userId = user.id,
                    id = item.rule.id,
                    amount = amount,
                    frequency = freq,
                    interval = interval,
                    endDateMillis = item.rule.endDate,
                    note = note,
                    isActive = isActive
                ) {
                    editingRecurring = null
                    mainViewModel.refreshDashboardAndReports(user.id)
                }
            }
        )
    }
}
