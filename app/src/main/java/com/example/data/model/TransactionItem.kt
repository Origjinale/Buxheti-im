package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class PaymentMethod {
    CASH,
    CARD,
    TRANSFER
}

@IgnoreExtraProperties
data class TransactionItem(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "food",
    val note: String? = null,
    val date: String = "", // Format: "YYYY-MM-DD"
    val paymentMethod: String? = PaymentMethod.CASH.name,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BudgetGoal(
    @DocumentId
    val id: String = "current",
    val userId: String = "",
    val monthlyBudget: Double = 500.0,
    val dailyBudget: Double = 20.0,
    val currency: String = "EUR",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

object FinanceCategories {
    data class CategoryInfo(
        val id: String,
        val titleSq: String,
        val type: TransactionType,
        val iconName: String
    )

    val expenseCategories = listOf(
        CategoryInfo("food", "Ushqim & Restorante", TransactionType.EXPENSE, "restaurant"),
        CategoryInfo("groceries", "Ushqimore & Treg", TransactionType.EXPENSE, "shopping_cart"),
        CategoryInfo("housing", "Banesë & Qira", TransactionType.EXPENSE, "home"),
        CategoryInfo("bills", "Fatura & Shërbime", TransactionType.EXPENSE, "receipt_long"),
        CategoryInfo("transport", "Transport & Karburant", TransactionType.EXPENSE, "directions_car"),
        CategoryInfo("shopping", "Blerje & Rroba", TransactionType.EXPENSE, "checkroom"),
        CategoryInfo("health", "Shëndet & Farmaci", TransactionType.EXPENSE, "medical_services"),
        CategoryInfo("entertainment", "Argëtim & Kafe", TransactionType.EXPENSE, "local_cafe"),
        CategoryInfo("education", "Arsim & Kurse", TransactionType.EXPENSE, "school"),
        CategoryInfo("family", "Familje & Fëmijë", TransactionType.EXPENSE, "family_restroom"),
        CategoryInfo("other_expense", "Të Tjera (Shpenzime)", TransactionType.EXPENSE, "more_horiz")
    )

    val incomeCategories = listOf(
        CategoryInfo("salary", "Pagë Mujore", TransactionType.INCOME, "payments"),
        CategoryInfo("freelance", "Biznes & Freelance", TransactionType.INCOME, "work"),
        CategoryInfo("investments", "Investime & Fitim", TransactionType.INCOME, "trending_up"),
        CategoryInfo("rent_income", "Qira e Pronës", TransactionType.INCOME, "real_estate_agent"),
        CategoryInfo("gifts", "Dhurata & Ndihma", TransactionType.INCOME, "card_giftcard"),
        CategoryInfo("other_income", "Të Tjera (Të ardhura)", TransactionType.INCOME, "savings")
    )

    fun getCategoryTitle(id: String): String {
        return (expenseCategories + incomeCategories).firstOrNull { it.id == id }?.titleSq ?: id
    }
}
