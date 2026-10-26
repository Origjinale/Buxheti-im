package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DaySpending
import java.util.Locale
import kotlin.math.max

@Composable
fun SpendingBarChart(
    dailySpendingList: List<DaySpending>,
    dailyBudget: Double,
    currency: String,
    selectedDay: String?,
    onDaySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailySpendingList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nuk ka shpenzime për këtë muaj",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxSpending = dailySpendingList.maxOfOrNull { it.totalExpense } ?: 0.0
    val chartUpperLimit = max(maxSpending, dailyBudget * 1.3).coerceAtLeast(10.0)

    val normalBarColor = MaterialTheme.colorScheme.primary
    val overBudgetBarColor = ExpenseRed
    val selectedBarColor = MaterialTheme.colorScheme.tertiary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val budgetLineColor = WarningOrange.copy(alpha = 0.8f)

    var tappedDay by remember { mutableStateOf<DaySpending?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend & Threshold Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shpenzimet sipas ditëve",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .background(WarningOrange, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Buxheti ditor: ${String.format(Locale.US, "%.0f", dailyBudget)} $currency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal Scrollable Bar Chart
        val barWidth = 18.dp
        val barSpacing = 10.dp
        val totalWidth = (barWidth + barSpacing) * dailySpendingList.size + 24.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(180.dp)
                    .pointerInput(dailySpendingList) {
                        detectTapGestures { offset ->
                            val itemWidthPx = (barWidth + barSpacing).toPx()
                            val clickedIndex = ((offset.x - 12.dp.toPx()) / itemWidthPx).toInt()
                            if (clickedIndex in dailySpendingList.indices) {
                                val item = dailySpendingList[clickedIndex]
                                tappedDay = if (tappedDay?.dateString == item.dateString) null else item
                                onDaySelected(tappedDay?.dateString)
                            }
                        }
                    }
            ) {
                val availableHeight = size.height - 30.dp.toPx()
                val budgetLineY = (availableHeight - (dailyBudget / chartUpperLimit * availableHeight)).toFloat().coerceIn(0f, availableHeight)

                // Draw budget target line
                drawLine(
                    color = budgetLineColor,
                    start = Offset(0f, budgetLineY),
                    end = Offset(size.width, budgetLineY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                dailySpendingList.forEachIndexed { index, day ->
                    val x = 12.dp.toPx() + index * (barWidth + barSpacing).toPx()
                    val barHeight = ((day.totalExpense / chartUpperLimit) * availableHeight).toFloat()
                    val barTop = availableHeight - barHeight

                    val isSelected = selectedDay == day.dateString
                    val isOverBudget = dailyBudget > 0 && day.totalExpense > dailyBudget

                    val color = when {
                        isSelected -> selectedBarColor
                        day.totalExpense == 0.0 -> emptyBarColor
                        isOverBudget -> overBudgetBarColor
                        else -> normalBarColor
                    }

                    // Background track bar (subtle)
                    drawRoundRect(
                        color = emptyBarColor.copy(alpha = 0.4f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth.toPx(), availableHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Active spending bar
                    if (barHeight > 0) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, barTop),
                            size = Size(barWidth.toPx(), barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Labels row along bottom
            Row(
                modifier = Modifier
                    .width(totalWidth)
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 4.dp)
            ) {
                dailySpendingList.forEach { day ->
                    val isSelected = selectedDay == day.dateString
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .clickable {
                                tappedDay = if (tappedDay?.dateString == day.dateString) null else day
                                onDaySelected(tappedDay?.dateString)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${day.dayNumber}",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(barSpacing))
                }
            }
        }

        // Tapped Day Detail Tooltip Card
        val currentSelected = dailySpendingList.firstOrNull { it.dateString == selectedDay }
        if (currentSelected != null && currentSelected.totalExpense > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dita ${currentSelected.dayNumber}:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", currentSelected.totalExpense)} $currency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyBudget > 0 && currentSelected.totalExpense > dailyBudget) ExpenseRed else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
