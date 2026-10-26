package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType

@Composable
fun CategoryIcon(
    categoryId: String,
    type: String,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val (icon, bgCol, tintCol) = getCategoryStyling(categoryId, type)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgCol),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryId,
            tint = tintCol,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun getCategoryStyling(categoryId: String, type: String): Triple<ImageVector, Color, Color> {
    return when (categoryId) {
        "food" -> Triple(Icons.Default.Restaurant, Color(0xFFFEF3C7), Color(0xFFD97706))
        "groceries" -> Triple(Icons.Default.ShoppingCart, Color(0xFFE0F2FE), Color(0xFF0284C7))
        "housing" -> Triple(Icons.Default.Home, Color(0xFFEDE9FE), Color(0xFF7C3AED))
        "bills" -> Triple(Icons.Default.ReceiptLong, Color(0xFFFFEDD5), Color(0xFFEA580C))
        "transport" -> Triple(Icons.Default.DirectionsCar, Color(0xFFFCE7F3), Color(0xFFDB2777))
        "shopping" -> Triple(Icons.Default.Checkroom, Color(0xFFF3E8FF), Color(0xFF9333EA))
        "health" -> Triple(Icons.Default.MedicalServices, Color(0xFFFEE2E2), Color(0xFFDC2626))
        "entertainment" -> Triple(Icons.Default.LocalCafe, Color(0xFFFEF9C3), Color(0xFFCA8A04))
        "education" -> Triple(Icons.Default.School, Color(0xFFCCFBF1), Color(0xFF0D9488))
        "family" -> Triple(Icons.Default.FamilyRestroom, Color(0xFFFDF4FF), Color(0xFFC026D3))

        // Income
        "salary" -> Triple(Icons.Default.Payments, Color(0xFFD1FAE5), Color(0xFF059669))
        "freelance" -> Triple(Icons.Default.Work, Color(0xFFCFFAFE), Color(0xFF0891B2))
        "investments" -> Triple(Icons.Default.TrendingUp, Color(0xFFE0E7FF), Color(0xFF4F46E5))
        "rent_income" -> Triple(Icons.Default.RealEstateAgent, Color(0xFFECFDF5), Color(0xFF10B981))
        "gifts" -> Triple(Icons.Default.CardGiftcard, Color(0xFFFCE7F3), Color(0xFFE11D48))
        "other_income" -> Triple(Icons.Default.Savings, Color(0xFFD1FAE5), Color(0xFF059669))

        else -> if (type == TransactionType.INCOME.name) {
            Triple(Icons.Default.Savings, Color(0xFFD1FAE5), Color(0xFF059669))
        } else {
            Triple(Icons.Default.MoreHoriz, Color(0xFFF1F5F9), Color(0xFF64748B))
        }
    }
}
