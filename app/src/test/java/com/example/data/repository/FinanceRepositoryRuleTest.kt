package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BudgetGoal
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTransaction_validPayload_createsDocument(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)

        val tx = TransactionItem(
            userId = aliceUid,
            amount = 25.50,
            type = TransactionType.EXPENSE.name,
            category = "food",
            note = "Kafe & Mengjes",
            date = "2026-10-25",
            paymentMethod = PaymentMethod.CASH.name
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(aliceUid, tx)
        }
        assertTrue(result.isSuccess)
        val txId = result.getOrThrow()
        assertTrue(txId.isNotEmpty())
    }

    @Test
    fun observeTransactions_authenticatedOwner_emitsCreatedTransaction(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)

        val tx = TransactionItem(
            userId = aliceUid,
            amount = 120.0,
            type = TransactionType.INCOME.name,
            category = "freelance",
            note = "Dizenjim Web",
            date = "2026-10-25",
            paymentMethod = PaymentMethod.CARD.name
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(aliceUid, tx)
        }
        val txId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(aliceUid).first { list ->
                list.any { it.id == txId }
            }
        }
        assertTrue(emitted.any { it.id == txId })
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceTransactions_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FinanceRepository(firestore)

        val tx = TransactionItem(
            userId = aliceUid,
            amount = 15.0,
            type = TransactionType.EXPENSE.name,
            category = "transport",
            date = "2026-10-25"
        )
        aliceRepo.addTransaction(aliceUid, tx).getOrThrow()

        // Switch to Bob
        signInTestUser(BOB_EMAIL)
        val bobRepo = FinanceRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeTransactions(aliceUid).first()
            }
            fail("Expected exception when Bob queries Alice's transactions")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun unauthenticatedUser_cannotObserveTransactions(): Unit = runBlocking {
        auth.signOut()
        val repository = FinanceRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions("unauth_user").first()
            }
            fail("Expected exception when unauthenticated user queries transactions")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun saveBudgetGoal_authenticatedOwner_persistsSettings(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)

        val budget = BudgetGoal(
            userId = aliceUid,
            monthlyBudget = 800.0,
            dailyBudget = 26.6,
            currency = "EUR"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBudgetGoal(aliceUid, budget)
        }
        assertTrue(result.isSuccess)

        val savedBudget = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBudgetGoal(aliceUid).first { it != null }
        }
        assertNotNull(savedBudget)
        assertEquals(800.0, savedBudget?.monthlyBudget ?: 0.0, 0.001)
        assertEquals("EUR", savedBudget?.currency)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
