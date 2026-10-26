package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetGoal
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CategorySummary(
    val categoryId: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class DaySpending(
    val dayNumber: Int,
    val dateString: String,
    val totalExpense: Double
)

data class MonthSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val monthlyBudget: Double,
    val dailyBudget: Double,
    val currency: String,
    val todayExpense: Double,
    val topCategory: String?,
    val topCategoryAmount: Double,
    val dailySpendingList: List<DaySpending>,
    val categoryBreakdown: List<CategorySummary>
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Month & Year Filter
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1) // 1-12
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    // Day Filter ("YYYY-MM-DD" or null for entire month)
    private val _selectedDay = MutableStateFlow<String?>(null)
    val selectedDay: StateFlow<String?> = _selectedDay.asStateFlow()

    // Type Filter (null = all, EXPENSE, INCOME)
    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    // Category Filter (null = all)
    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Observe all transactions
    val transactionsState: StateFlow<UiState<List<TransactionItem>>> = repository
        .observeTransactions(currentUserId)
        .map<List<TransactionItem>, UiState<List<TransactionItem>>> { UiState.Success(it) }
        .catch { error ->
            if (error !is kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "Gabim gjatë vëzhgimit të transaksioneve: ${error.message}")
            }
            emit(UiState.Success(emptyList()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Observe budget settings
    val budgetGoal: StateFlow<BudgetGoal> = repository
        .observeBudgetGoal(currentUserId)
        .map { it ?: BudgetGoal(userId = currentUserId) }
        .catch { error ->
            if (error !is kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "Gabim gjatë vëzhgimit të buxhetit: ${error.message}")
            }
            emit(BudgetGoal(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = BudgetGoal(userId = currentUserId)
        )

    // Filtered transaction list for display
    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        transactionsState,
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        _filterType,
        _filterCategory,
        _searchQuery
    ) { params ->
        val state = params[0] as UiState<List<TransactionItem>>
        val year = params[1] as Int
        val month = params[2] as Int
        val day = params[3] as String?
        val type = params[4] as TransactionType?
        val category = params[5] as String?
        val query = (params[6] as String).trim().lowercase()

        if (state !is UiState.Success) return@combine emptyList()

        val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month)

        state.data.filter { tx ->
            val matchesMonth = tx.date.startsWith(monthPrefix)
            val matchesDay = day == null || tx.date == day
            val matchesType = type == null || tx.type == type.name
            val matchesCategory = category == null || tx.category == category
            val matchesQuery = query.isEmpty() ||
                    (tx.note?.lowercase()?.contains(query) == true) ||
                    tx.category.lowercase().contains(query) ||
                    tx.amount.toString().contains(query)

            matchesMonth && matchesDay && matchesType && matchesCategory && matchesQuery
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Monthly summary calculation
    val monthSummary: StateFlow<MonthSummary> = combine(
        transactionsState,
        budgetGoal,
        _selectedYear,
        _selectedMonth
    ) { txState, budget, year, month ->
        val rawTx = (txState as? UiState.Success<List<TransactionItem>>)?.data ?: emptyList<TransactionItem>()
        val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month)
        val monthTx = rawTx.filter { it.date.startsWith(monthPrefix) }
        val bGoal = budget

        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayExpense = 0.0

        val categoryExpenseMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()
        val dayExpenseMap = mutableMapOf<Int, Double>()

        for (tx in monthTx) {
            if (tx.type == TransactionType.INCOME.name) {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
                categoryExpenseMap[tx.category] = (categoryExpenseMap[tx.category] ?: 0.0) + tx.amount
                categoryCountMap[tx.category] = (categoryCountMap[tx.category] ?: 0) + 1

                val dayPart = tx.date.substringAfterLast("-").toIntOrNull() ?: 1
                dayExpenseMap[dayPart] = (dayExpenseMap[dayPart] ?: 0.0) + tx.amount
            }

            if (tx.date == todayString && tx.type == TransactionType.EXPENSE.name) {
                todayExpense += tx.amount
            }
        }

        // Days in month calculation for the chart
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dailySpendingList = (1..daysInMonth).map { dayNum ->
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
            DaySpending(
                dayNumber = dayNum,
                dateString = dateStr,
                totalExpense = dayExpenseMap[dayNum] ?: 0.0
            )
        }

        val categoryBreakdown = categoryExpenseMap.map { (catId, amount) ->
            CategorySummary(
                categoryId = catId,
                totalAmount = amount,
                count = categoryCountMap[catId] ?: 0,
                percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
            )
        }.sortedByDescending { it.totalAmount }

        val topCategory = categoryBreakdown.firstOrNull()

        MonthSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            monthlyBudget = bGoal.monthlyBudget,
            dailyBudget = bGoal.dailyBudget,
            currency = bGoal.currency,
            todayExpense = todayExpense,
            topCategory = topCategory?.categoryId,
            topCategoryAmount = topCategory?.totalAmount ?: 0.0,
            dailySpendingList = dailySpendingList,
            categoryBreakdown = categoryBreakdown
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MonthSummary(
            totalIncome = 0.0,
            totalExpense = 0.0,
            netBalance = 0.0,
            monthlyBudget = 500.0,
            dailyBudget = 20.0,
            currency = "EUR",
            todayExpense = 0.0,
            topCategory = null,
            topCategoryAmount = 0.0,
            dailySpendingList = emptyList(),
            categoryBreakdown = emptyList()
        )
    )

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
        _selectedDay.value = null
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
        _selectedDay.value = null
    }

    fun selectDay(day: String?) {
        _selectedDay.value = day
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        date: String,
        paymentMethod: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val item = TransactionItem(
                userId = currentUserId,
                amount = amount,
                type = type.name,
                category = category,
                note = note?.takeIf { it.isNotBlank() },
                date = date,
                paymentMethod = paymentMethod
            )
            val result = repository.addTransaction(currentUserId, item)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Dështoi shtimi i transaksionit")
            }
        }
    }

    fun updateTransaction(
        transaction: TransactionItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateTransaction(currentUserId, transaction)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Dështoi modifikimi i transaksionit")
            }
        }
    }

    fun deleteTransaction(
        transactionId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(currentUserId, transactionId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Dështoi fshirja e transaksionit")
            }
        }
    }

    fun saveBudgetGoal(
        monthlyBudget: Double,
        dailyBudget: Double,
        currency: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val goal = BudgetGoal(
                userId = currentUserId,
                monthlyBudget = monthlyBudget,
                dailyBudget = dailyBudget,
                currency = currency
            )
            val result = repository.saveBudgetGoal(currentUserId, goal)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Dështoi ruajtja e buxhetit")
            }
        }
    }

    companion object {
        private const val TAG = "FinanceVM"
    }
}
