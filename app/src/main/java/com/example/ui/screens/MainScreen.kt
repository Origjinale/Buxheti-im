package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionItem
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.BudgetSettingsDialog
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

private val albanianMonths = listOf(
    "Janar", "Shkurt", "Mars", "Prill", "Maj", "Qershor",
    "Korrik", "Gusht", "Shtator", "Tetor", "Nëntor", "Dhjetor"
)

enum class NavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Pasqyra", Icons.Default.Dashboard),
    TRANSACTIONS("Lëvizjet", Icons.Default.ReceiptLong),
    ANALYTICS("Statistika", Icons.Default.BarChart),
    SETTINGS("Cilësimet", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    onSignOutClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    // State from ViewModel
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val monthSummary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val budgetGoal by viewModel.budgetGoal.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val monthTitle = "${albanianMonths.getOrElse(selectedMonth - 1) { "" }} $selectedYear"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Buxheti Im",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Month Selector with chevrons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.previousMonth() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Muaji i kaluar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = monthTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.nextMonth() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Muaji i ardhshëm",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.testTag("open_budget_settings_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ndrysho buxhetin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != NavTab.SETTINGS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        showAddSheet = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Regjistro", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_transaction_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> {
                    DashboardScreen(
                        summary = monthSummary,
                        recentTransactions = filteredTransactions,
                        onAddTransactionClick = {
                            editingTransaction = null
                            showAddSheet = true
                        },
                        onTransactionClick = { tx ->
                            editingTransaction = tx
                            showAddSheet = true
                        },
                        onViewAllTransactions = {
                            currentTab = NavTab.TRANSACTIONS
                        },
                        onOpenBudgetSettings = {
                            showBudgetDialog = true
                        }
                    )
                }

                NavTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = filteredTransactions,
                        currency = monthSummary.currency,
                        filterType = filterType,
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        selectedDay = selectedDay,
                        onSelectedDayChange = { viewModel.selectDay(it) },
                        onTransactionClick = { tx ->
                            editingTransaction = tx
                            showAddSheet = true
                        }
                    )
                }

                NavTab.ANALYTICS -> {
                    AnalyticsScreen(
                        summary = monthSummary,
                        selectedDay = selectedDay,
                        onDaySelected = { viewModel.selectDay(it) }
                    )
                }

                NavTab.SETTINGS -> {
                    SettingsScreen(
                        budgetGoal = budgetGoal,
                        onOpenBudgetSettings = { showBudgetDialog = true },
                        onSignOutClick = onSignOutClick
                    )
                }
            }
        }
    }

    // Add or Edit Transaction Bottom Sheet
    if (showAddSheet) {
        AddTransactionSheet(
            currency = monthSummary.currency,
            existingTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { amount, type, category, note, date, paymentMethod ->
                if (editingTransaction != null) {
                    val updated = editingTransaction!!.copy(
                        amount = amount,
                        type = type.name,
                        category = category,
                        note = note,
                        date = date,
                        paymentMethod = paymentMethod
                    )
                    viewModel.updateTransaction(
                        transaction = updated,
                        onSuccess = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Transaksioni u përditësua me sukses!")
                            }
                        },
                        onError = { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Gabim: $error")
                            }
                        }
                    )
                } else {
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        category = category,
                        note = note,
                        date = date,
                        paymentMethod = paymentMethod,
                        onSuccess = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Transaksioni u ruajt me sukses!")
                            }
                        },
                        onError = { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Gabim: $error")
                            }
                        }
                    )
                }
            },
            onDelete = { txId ->
                viewModel.deleteTransaction(
                    transactionId = txId,
                    onSuccess = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Transaksioni u fshi!")
                        }
                    },
                    onError = { error ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Gabim: $error")
                        }
                    }
                )
            }
        )
    }

    // Budget Settings Dialog
    if (showBudgetDialog) {
        BudgetSettingsDialog(
            initialMonthlyBudget = budgetGoal.monthlyBudget,
            initialDailyBudget = budgetGoal.dailyBudget,
            initialCurrency = budgetGoal.currency,
            onDismiss = { showBudgetDialog = false },
            onSave = { monthly, daily, currency ->
                viewModel.saveBudgetGoal(
                    monthlyBudget = monthly,
                    dailyBudget = daily,
                    currency = currency,
                    onSuccess = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Buxheti u përditësua me sukses!")
                        }
                    },
                    onError = { error ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Gabim: $error")
                        }
                    }
                )
            }
        )
    }
}
