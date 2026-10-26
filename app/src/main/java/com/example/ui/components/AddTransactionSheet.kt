package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceCategories
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    currency: String,
    existingTransaction: TransactionItem? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: TransactionType, category: String, note: String?, date: String, paymentMethod: String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString = dateFormat.format(Date())

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayString = dateFormat.format(cal.time)

    var type by remember {
        mutableStateOf(
            if (existingTransaction != null && existingTransaction.type == TransactionType.INCOME.name)
                TransactionType.INCOME
            else
                TransactionType.EXPENSE
        )
    }

    var amountText by remember {
        mutableStateOf(if (existingTransaction != null) existingTransaction.amount.toString() else "")
    }

    var category by remember {
        mutableStateOf(
            existingTransaction?.category ?: if (type == TransactionType.EXPENSE) "food" else "salary"
        )
    }

    var dateString by remember {
        mutableStateOf(existingTransaction?.date ?: todayString)
    }

    var note by remember {
        mutableStateOf(existingTransaction?.note ?: "")
    }

    var paymentMethod by remember {
        mutableStateOf(existingTransaction?.paymentMethod ?: PaymentMethod.CASH.name)
    }

    var amountError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header & Delete (if editing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction != null) "Ndrysho Transaksionin" else "Regjistro Transaksion të Ri",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (existingTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(existingTransaction.id)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Fshij transaksionin",
                            tint = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Toggle (Shpenzim / E Ardhur)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Shpenzim Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent
                            )
                            .clickable {
                                type = TransactionType.EXPENSE
                                if (FinanceCategories.incomeCategories.any { it.id == category }) {
                                    category = "food"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Shpenzim (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // E Ardhur Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent
                            )
                            .clickable {
                                type = TransactionType.INCOME
                                if (FinanceCategories.expenseCategories.any { it.id == category }) {
                                    category = "salary"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E Ardhur (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = false
                },
                label = { Text("Shuma") },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currency ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Ju lutem vendosni një shumë të vlefshme pozitive") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Zgjidh Kategorinë",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentCategoryList = if (type == TransactionType.EXPENSE) {
                FinanceCategories.expenseCategories
            } else {
                FinanceCategories.incomeCategories
            }

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentCategoryList) { cat ->
                    val isSelected = category == cat.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { category = cat.id }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CategoryIcon(
                                categoryId = cat.id,
                                type = type.name,
                                size = 34.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.titleSq,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Quick Selection
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dateString == todayString,
                    onClick = { dateString = todayString },
                    label = { Text("Sot") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = dateString == yesterdayString,
                    onClick = { dateString = yesterdayString },
                    label = { Text("Dje") }
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selection
            Text(
                text = "Mënyra e Pagesës",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(PaymentMethod.CASH.name, "Para në dorë", Icons.Default.Money),
                    Triple(PaymentMethod.CARD.name, "Kartë Bankare", Icons.Default.CreditCard),
                    Triple(PaymentMethod.TRANSFER.name, "Transfertë", Icons.Default.Payments)
                ).forEach { (methodId, label, icon) ->
                    val isSelected = paymentMethod == methodId
                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMethod = methodId },
                        label = { Text(label, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note (Optional)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Përshkrimi ose Shënimi (Opsional)") },
                placeholder = { Text("p.sh. Pazar në supermarket, Faturë interneti...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Notes, contentDescription = null)
                },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    val parsedAmount = amountText.replace(',', '.').toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        amountError = true
                        return@Button
                    }
                    onSave(
                        parsedAmount,
                        type,
                        category,
                        note.trim().takeIf { it.isNotEmpty() },
                        dateString,
                        paymentMethod
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingTransaction != null) "Përditëso Transaksionin" else "Ruaj Transaksionin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
