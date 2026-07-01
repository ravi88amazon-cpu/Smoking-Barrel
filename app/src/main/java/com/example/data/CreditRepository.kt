package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CreditRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Add a new Credit
     */
    suspend fun addCredit(credit: CreditEntity): Result<Unit> {

        return try {

            val id =
                if (credit.cloudId.isBlank())
                    UUID.randomUUID().toString()
                else
                    credit.cloudId

            val newCredit = credit.copy(
                cloudId = id
            )

            db.collection("credits")
                .document(id)
                .set(newCredit)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe Credits in realtime
     */
    fun observeCredits(): Flow<List<CreditEntity>> = callbackFlow {

        val listener =
            db.collection("credits")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val credits =
                        snapshot?.documents?.mapNotNull {
                            it.toObject(CreditEntity::class.java)
                        } ?: emptyList()

                    trySend(credits)
                }

        awaitClose {
            listener.remove()
        }
    }
}