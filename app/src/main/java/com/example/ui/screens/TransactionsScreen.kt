package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceCategories
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun TransactionsScreen(
    transactions: List<TransactionItem>,
    currency: String,
    filterType: TransactionType?,
    onFilterTypeChange: (TransactionType?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedDay: String?,
    onSelectedDayChange: (String?) -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Kërko sipas shënimit ose kategorisë...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Pastro kërkimin")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transactions_search_bar")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills: All / Expenses / Income
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == null,
                onClick = { onFilterTypeChange(null) },
                label = { Text("Të Gjitha (${transactions.size})") }
            )

            FilterChip(
                selected = filterType == TransactionType.EXPENSE,
                onClick = {
                    onFilterTypeChange(
                        if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                    )
                },
                label = { Text("Vetëm Shpenzime") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                    selectedLabelColor = ExpenseRed
                )
            )

            FilterChip(
                selected = filterType == TransactionType.INCOME,
                onClick = {
                    onFilterTypeChange(
                        if (filterType == TransactionType.INCOME) null else TransactionType.INCOME
                    )
                },
                label = { Text("Vetëm Të Ardhura") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                    selectedLabelColor = IncomeGreen
                )
            )

            if (selectedDay != null) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectedDayChange(null) },
                    label = { Text("Dita: $selectedDay ✕") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nuk u gjet asnjë transaksion.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Provo të heqësh filtrat ose shto një transaksion të ri me butonin (+).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }
        } else {
            // Group transactions by Date
            val groupedByDate = transactions.groupBy { it.date }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (date, itemsForDate) ->
                    item(key = "header_$date") {
                        val dayExpenses = itemsForDate.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                        val dayIncome = itemsForDate.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayExpenses > 0) {
                                    Text(
                                        text = "-${String.format(Locale.US, "%.0f", dayExpenses)} $currency",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ExpenseRed
                                    )
                                }
                                if (dayIncome > 0) {
                                    Text(
                                        text = "+${String.format(Locale.US, "%.0f", dayIncome)} $currency",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }
                    }

                    items(itemsForDate, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            currency = currency,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }
}
