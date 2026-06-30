package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // Save Credit
    suspend fun saveCredit(credit: CreditEntity): Result<Unit> {
        return try {
            db.collection("credits")
                .add(credit)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Save Debit Account
    suspend fun saveDebitAccount(debit: DebitAccountEntity): Result<Unit> {
        return try {
            db.collection("debitAccounts")
                .add(debit)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Save Debit Hand
suspend fun saveDebitHand(debit: DebitHandEntity): Result<Unit> {
    return try {
        db.collection("debitHands")
            .add(debit)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

    // Observe Credits (Realtime)
fun observeCredits(): Flow<List<CreditEntity>> = callbackFlow {

    val listener = db.collection("credits")
        .addSnapshotListener { snapshot, error ->

            if (error != null) {
                error.printStackTrace()
                trySend(emptyList())
                return@addSnapshotListener
            }

            try {
                val credits = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(CreditEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()

                trySend(credits)

            } catch (e: Exception) {
                e.printStackTrace()
                trySend(emptyList())
            }
        }

    awaitClose {
        listener.remove()
    }
}