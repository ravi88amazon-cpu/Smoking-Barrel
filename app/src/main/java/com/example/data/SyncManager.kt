package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncManager(
    private val repository: LedgerRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startSync() {

        Log.d("SyncManager", "Realtime sync started")

        scope.launch {

            repository.allCreditsFirestore.collectLatest { firestoreCredits ->

                Log.d(
                    "SyncManager",
                    "Received ${firestoreCredits.size} credits from Firestore"
                )

                repository.replaceCreditsFromFirestore(firestoreCredits)
            }
        }
    }
}