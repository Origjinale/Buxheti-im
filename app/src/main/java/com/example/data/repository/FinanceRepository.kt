package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BudgetGoal
import com.example.data.model.TransactionItem
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val firestore: FirebaseFirestore
) {
    // Secondary constructor to resolve database ID from resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun transactionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("transactions")

    private fun budgetCollection(userId: String) =
        firestore.collection("users").document(userId).collection("budget")

    /**
     * Real-time observation of all user transactions
     */
    fun observeTransactions(userId: String): Flow<List<TransactionItem>> = callbackFlow {
        val path = "users/$userId/transactions"
        val registration = transactionsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(TransactionItem::class.java)
                        .sortedByDescending { it.date }
                    trySend(items)
                }
            }
        awaitClose {
            registration.remove()
        }
    }

    /**
     * Add a new transaction
     */
    suspend fun addTransaction(userId: String, transaction: TransactionItem): Result<String> {
        val colRef = transactionsCollection(userId)
        val docRef = colRef.document()
        val data = mutableMapOf<String, Any>(
            "userId" to userId,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "category" to transaction.category,
            "date" to transaction.date,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!transaction.note.isNullOrBlank()) {
            data["note"] = transaction.note.trim()
        }
        if (!transaction.paymentMethod.isNullOrBlank()) {
            data["paymentMethod"] = transaction.paymentMethod
        }

        return try {
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(userId: String, transaction: TransactionItem): Result<Unit> {
        require(transaction.id.isNotBlank()) { "Transaction ID must not be blank" }
        val docRef = transactionsCollection(userId).document(transaction.id)

        val updates = mutableMapOf<String, Any>(
            "amount" to transaction.amount,
            "type" to transaction.type,
            "category" to transaction.category,
            "date" to transaction.date,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.note != null) {
            updates["note"] = transaction.note.trim()
        }
        if (transaction.paymentMethod != null) {
            updates["paymentMethod"] = transaction.paymentMethod
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Delete a transaction
     */
    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docRef = transactionsCollection(userId).document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Real-time observation of user budget settings
     */
    fun observeBudgetGoal(userId: String): Flow<BudgetGoal?> = callbackFlow {
        val path = "users/$userId/budget/current"
        val registration = budgetCollection(userId).document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(BudgetGoal::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose {
            registration.remove()
        }
    }

    /**
     * Save or update budget goal
     */
    suspend fun saveBudgetGoal(userId: String, budgetGoal: BudgetGoal): Result<Unit> {
        val docRef = budgetCollection(userId).document("current")
        val data = mapOf<String, Any>(
            "userId" to userId,
            "monthlyBudget" to budgetGoal.monthlyBudget,
            "dailyBudget" to budgetGoal.dailyBudget,
            "currency" to budgetGoal.currency,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            // Check if document exists first to determine whether to set or update
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateData = mapOf<String, Any>(
                    "monthlyBudget" to budgetGoal.monthlyBudget,
                    "dailyBudget" to budgetGoal.dailyBudget,
                    "currency" to budgetGoal.currency,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateData).await()
            } else {
                docRef.set(data).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
