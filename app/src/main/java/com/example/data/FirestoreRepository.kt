package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

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

    fun observeCredits(): Flow<List<CreditEntity>> = callbackFlow {

        val listener: ListenerRegistration =
            db.collection("credits")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val credits = snapshot?.documents?.mapNotNull {
                        it.toObject(CreditEntity::class.java)
                    } ?: emptyList()

                    trySend(credits)
                }

        awaitClose {
            listener.remove()
        }
    }
}