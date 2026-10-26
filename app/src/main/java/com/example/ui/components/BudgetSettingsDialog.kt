package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun BudgetSettingsDialog(
    initialMonthlyBudget: Double,
    initialDailyBudget: Double,
    initialCurrency: String,
    onDismiss: () -> Unit,
    onSave: (monthly: Double, daily: Double, currency: String) -> Unit
) {
    var monthlyText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialMonthlyBudget)) }
    var dailyText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialDailyBudget)) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currencies = listOf("EUR", "LEK", "USD", "CHF")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Cilësimet e Buxhetit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Përcaktoni limitet tuaja për të mbajtur shpenzimet nën kontroll dhe për të kursyer çdo muaj.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Currency selector
                Text(
                    text = "Monedha kryesore",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            label = { Text(curr) }
                        )
                    }
                }

                // Monthly Budget Field
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = {
                        monthlyText = it
                        errorMessage = null
                    },
                    label = { Text("Limiti i Buxhetit Mujor") },
                    prefix = { Text("$currency ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_budget_input")
                )

                // Daily Budget Field
                OutlinedTextField(
                    value = dailyText,
                    onValueChange = {
                        dailyText = it
                        errorMessage = null
                    },
                    label = { Text("Limiti i Buxhetit Ditor") },
                    prefix = { Text("$currency ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_budget_input")
                )

                // Auto calculate daily from monthly
                OutlinedButton(
                    onClick = {
                        val monthlyVal = monthlyText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (monthlyVal > 0) {
                            dailyText = String.format(Locale.US, "%.1f", monthlyVal / 30.0)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Llogarit ditorin automatik (Mujor / 30)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mVal = monthlyText.replace(',', '.').toDoubleOrNull()
                    val dVal = dailyText.replace(',', '.').toDoubleOrNull()
                    if (mVal == null || mVal < 0 || dVal == null || dVal < 0) {
                        errorMessage = "Ju lutem vendosni vlera numerike të sakta"
                        return@Button
                    }
                    onSave(mVal, dVal, currency)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Ruaj Ndryshimet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulo")
            }
        }
    )
}
