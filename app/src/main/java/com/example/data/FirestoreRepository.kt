package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // -----------------------------
    // CREATE
    // -----------------------------
    suspend fun addCredit(credit: CreditEntity): Result<Unit> {
        return try {

            val cloudId =
                if (credit.cloudId.isBlank())
                    UUID.randomUUID().toString()
                else
                    credit.cloudId

            val newCredit = credit.copy(
                cloudId = cloudId
            )

            db.collection("credits")
                .document(cloudId)
                .set(newCredit)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------
    // READ
    // -----------------------------
    fun observeCredits(): Flow<List<CreditEntity>> = callbackFlow {

        val listener =
            db.collection("credits")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val list =
                        snapshot?.documents?.mapNotNull { doc ->

                            doc.toObject(CreditEntity::class.java)?.copy(
                                cloudId = doc.id
                            )

                        } ?: emptyList()

                    trySend(list)
                }

        awaitClose {
            listener.remove()
        }
    }

    // -----------------------------
// READ - Debit Account
// -----------------------------
fun observeDebitAccounts(): Flow<List<DebitAccountEntity>> = callbackFlow {

    val listener =
        db.collection("debitAccounts")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val list =
                    snapshot?.documents?.mapNotNull { doc ->

                        doc.toObject(DebitAccountEntity::class.java)?.copy(
                            cloudId = doc.id
                        )

                    } ?: emptyList()

                trySend(list)
            }

    awaitClose {
        listener.remove()
    }
}

    // -----------------------------
    // UPDATE
    // -----------------------------
    suspend fun updateCredit(
        credit: CreditEntity
    ): Result<Unit> {

        return try {

            db.collection("credits")
                .document(credit.cloudId)
                .set(credit)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------
    // DELETE
    // -----------------------------
    suspend fun deleteCredit(
        cloudId: String
    ): Result<Unit> {

        return try {

            db.collection("credits")
                .document(cloudId)
                .delete()
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------
    // Debit Account
    // -----------------------------
    suspend fun addDebitAccount(
    debit: DebitAccountEntity
): Result<Unit> {

    return try {

        val cloudId =
            if (debit.cloudId.isBlank())
                UUID.randomUUID().toString()
            else
                debit.cloudId

        val newDebit = debit.copy(
            cloudId = cloudId
        )

        db.collection("debitAccounts")
            .document(cloudId)
            .set(newDebit)
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun updateDebitAccount(
    debit: DebitAccountEntity
): Result<Unit> {

    return try {

        db.collection("debitAccounts")
            .document(debit.cloudId)
            .set(debit)
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun deleteDebitAccount(
    cloudId: String
): Result<Unit> {

    return try {

        db.collection("debitAccounts")
            .document(cloudId)
            .delete()
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }
}

    // -----------------------------
    // Debit Hand
    // -----------------------------
    suspend fun saveDebitHand(
        debit: DebitHandEntity
    ): Result<Unit> {

        return try {

            db.collection("debitHands")
                .add(debit)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}