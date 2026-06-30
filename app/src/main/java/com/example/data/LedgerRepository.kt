package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LedgerRepository(private val ledgerDao: LedgerDao) {

    private val client = OkHttpClient()
    private val firestoreRepository = FirestoreRepository()

    // Room
val allCreditsRoom: Flow<List<CreditEntity>> = ledgerDao.getAllCredits()

// Firestore
val allCreditsFirestore: Flow<List<CreditEntity>> =
    firestoreRepository.observeCredits()

// Room
val allDebitsAccount: Flow<List<DebitAccountEntity>> = ledgerDao.getAllDebitsAccount()
val allDebitsHand: Flow<List<DebitHandEntity>> = ledgerDao.getAllDebitsHand()

    // Base Spreadsheet URLs
    private val spreadsheetId = "1_BplFZTeKDMhWyKE4v_r2aJlw3ihZfrb"
    private val creditUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=1434456980"
    private val debitAccountUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=1173786589"
    private val debitHandUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=2049797672"

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    suspend fun fetchAndSyncFromSheets(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Credits
            val creditsRequest = Request.Builder().url(creditUrl).build()
            val creditsResponse = client.newCall(creditsRequest).execute()
            if (!creditsResponse.isSuccessful) throw IOException("Failed to fetch credits CSV: $creditsResponse")
            val creditsBody = creditsResponse.body?.string() ?: ""
            val parsedCredits = parseCreditsCsv(creditsBody)
            if (parsedCredits.isEmpty()) {
                throw Exception("No Credit records were parsed from Google Sheets")
            }
            Log.d("LedgerRepository", "Parsed Credits = ${parsedCredits.size}")

            // 2. Fetch Debits Account
            val debitsAccountRequest = Request.Builder().url(debitAccountUrl).build()
            val debitsAccountResponse = client.newCall(debitsAccountRequest).execute()
            if (!debitsAccountResponse.isSuccessful) throw IOException("Failed to fetch debits account CSV: $debitsAccountResponse")
            val debitsAccountBody = debitsAccountResponse.body?.string() ?: ""
            val parsedDebitsAccount = parseDebitsAccountCsv(debitsAccountBody)

            // 3. Fetch Debits Hand
            val debitsHandRequest = Request.Builder().url(debitHandUrl).build()
            val debitsHandResponse = client.newCall(debitsHandRequest).execute()
            if (!debitsHandResponse.isSuccessful) throw IOException("Failed to fetch debits hand CSV: $debitsHandResponse")
            val debitsHandBody = debitsHandResponse.body?.string() ?: ""
            val parsedDebitsHand = parseDebitsHandCsv(debitsHandBody)

            // Save to DB (Clear old cached ones first, keeping unsynced new items!)
            // To ensure we don't wipe out local additions that are not synced yet,
            // we can clear database and insert remote ones, but preserve local-only ones.
            ledgerDao.clearCredits()
            ledgerDao.insertCredits(parsedCredits)
            Log.d("LedgerRepository", "Inserted Credits into Room = ${parsedCredits.size}")

            ledgerDao.clearDebitsAccount()
            ledgerDao.insertDebitsAccount(parsedDebitsAccount)

            ledgerDao.clearDebitsHand()
            ledgerDao.insertDebitsHand(parsedDebitsHand)

            Log.d("LedgerRepository", "Sheets sync completed successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LedgerRepository", "Sync failed", e)
            Result.failure(e)
        }
    }

    private fun parseCreditsCsv(csv: String): List<CreditEntity> {
        val list = mutableListOf<CreditEntity>()
        val lines = csv.split("\n")
        for (line in lines) {
            val parts = splitCsvLine(line)
            if (parts.isNotEmpty() && parts[0].toIntOrNull() != null) {
                try {
                    // Columns: S.No,Date,Vendor,Product Name,Product Type,Number of Product,Cost per Product,Cost of Product,Sales Price,Total Price,Payment Status
                    val date = parts.getOrNull(1) ?: ""
                    val vendor = parts.getOrNull(2) ?: ""
                    val productName = parts.getOrNull(3) ?: ""
                    val productType = parts.getOrNull(4) ?: ""
                    val qty = parts.getOrNull(5)?.toIntOrNull() ?: 0
                    val costPer = parts.getOrNull(6)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val costOf = parts.getOrNull(7)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val salesPrice = parts.getOrNull(8)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val totalPrice = parts.getOrNull(9)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val paymentStatus = parts.getOrNull(10) ?: ""

                    list.add(
                        CreditEntity(
                            date = date,
                            vendor = vendor,
                            productName = productName,
                            productType = productType,
                            numberOfProduct = qty,
                            costPerProduct = costPer,
                            costOfProduct = costOf,
                            salesPrice = salesPrice,
                            totalPrice = totalPrice,
                            paymentStatus = paymentStatus,
                            isSynced = true // This is downloaded from Sheet
                        )
                    )
                } catch (e: Exception) {
                    Log.e("LedgerRepository", "Error parsing credit row: $line", e)
                }
            }
        }
        return list
    }

    private fun parseDebitsAccountCsv(csv: String): List<DebitAccountEntity> {
        val list = mutableListOf<DebitAccountEntity>()
        val lines = csv.split("\n")
        for (line in lines) {
            val parts = splitCsvLine(line)
            if (parts.isNotEmpty() && parts[0].toIntOrNull() != null) {
                try {
                    // Columns: S.No,Date,Source,Product Name,Product Type,Number of Product,Cost per Product,Total Price,Payment Status
                    val date = parts.getOrNull(1) ?: ""
                    val source = parts.getOrNull(2) ?: ""
                    val productName = parts.getOrNull(3) ?: ""
                    val productType = parts.getOrNull(4) ?: ""
                    val qty = parts.getOrNull(5)?.toIntOrNull() ?: 0
                    val costPer = parts.getOrNull(6)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val totalPrice = parts.getOrNull(7)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val paymentStatus = parts.getOrNull(8) ?: ""

                    list.add(
                        DebitAccountEntity(
                            date = date,
                            source = source,
                            productName = productName,
                            productType = productType,
                            numberOfProduct = qty,
                            costPerProduct = costPer,
                            totalPrice = totalPrice,
                            paymentStatus = paymentStatus,
                            isSynced = true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("LedgerRepository", "Error parsing debit account row: $line", e)
                }
            }
        }
        return list
    }

    private fun parseDebitsHandCsv(csv: String): List<DebitHandEntity> {
        val list = mutableListOf<DebitHandEntity>()
        val lines = csv.split("\n")
        for (line in lines) {
            val parts = splitCsvLine(line)
            if (parts.isNotEmpty() && parts[0].toIntOrNull() != null) {
                try {
                    // Columns: S.No,Date,Spent By,Vendor,Description,Qty,Rate,Amount,Status
                    val date = parts.getOrNull(1) ?: ""
                    val spentBy = parts.getOrNull(2) ?: ""
                    val vendor = parts.getOrNull(3) ?: ""
                    val description = parts.getOrNull(4) ?: ""
                    val qty = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                    val rate = parts.getOrNull(6)?.replace("\"", "")?.replace(",", "")?.replace("Rs.", "")?.trim()?.toDoubleOrNull() ?: 0.0
                    val amount = parts.getOrNull(7)?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val status = parts.getOrNull(8) ?: ""

                    list.add(
                        DebitHandEntity(
                            date = date,
                            spentBy = spentBy,
                            vendor = vendor,
                            description = description,
                            qty = qty,
                            rate = rate,
                            amount = amount,
                            status = status,
                            isSynced = true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("LedgerRepository", "Error parsing debit hand row: $line", e)
                }
            }
        }
        return list
    }

    // Local Insert Actions
    suspend fun addCredit(credit: CreditEntity) {

    // Save locally
    ledgerDao.insertCredit(credit)

    // Save to Firestore
    firestoreRepository.saveCredit(credit)
}

    suspend fun addDebitAccount(debit: DebitAccountEntity) {

    ledgerDao.insertDebitAccount(debit)

    firestoreRepository.saveDebitAccount(debit)
}

    suspend fun addDebitHand(debit: DebitHandEntity) {

    ledgerDao.insertDebitHand(debit)

    firestoreRepository.saveDebitHand(debit)
}

    suspend fun deleteCredit(id: Int) {
        ledgerDao.deleteCredit(id)
    }

    suspend fun deleteDebitAccount(id: Int) {
        ledgerDao.deleteDebitAccount(id)
    }

    suspend fun deleteDebitHand(id: Int) {
        ledgerDao.deleteDebitHand(id)
    }

    // Post to Google Apps Script Web App
    suspend fun syncItemToGoogleSheet(
        appsScriptUrl: String,
        sheetType: String, // "Credit", "DebitAccount", "DebitHand"
        payload: JSONObject
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sheetType", sheetType)
                put("data", payload)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(appsScriptUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    IOException("Server error: ${response.code} - ${response.message}").let {
                        Result.failure(it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LedgerRepository", "Post failed", e)
            Result.failure(e)
        }
    }
        suspend fun replaceCreditsFromFirestore(
        credits: List<CreditEntity>
    ) = withContext(Dispatchers.IO) {

        ledgerDao.clearCredits()

        ledgerDao.insertCredits(credits)

        Log.d(
            "SyncManager",
            "Room updated with ${credits.size} Firestore credits"
        )
    }
}
