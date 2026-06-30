package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CreditEntity
import com.example.data.DebitAccountEntity
import com.example.data.DebitHandEntity
import com.example.data.LedgerDatabase
import com.example.data.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class LedgerViewModel(
    application: Application,
    private val repository: LedgerRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("smoking_barrel_prefs", Context.MODE_PRIVATE)
    private val geminiService = com.example.data.GeminiService()

    // Apps Script URL state
    private val _appsScriptUrl = MutableStateFlow(sharedPrefs.getString("apps_script_url", "") ?: "")
    val appsScriptUrl: StateFlow<String> = _appsScriptUrl.asStateFlow()

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // AI Report state
    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    // Raw flows from Database
    val credits: StateFlow<List<CreditEntity>> = repository.allCreditsFirestore
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val debitsAccount: StateFlow<List<DebitAccountEntity>> = repository.allDebitsAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debitsHand: StateFlow<List<DebitHandEntity>> = repository.allDebitsHand
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations State
    val dashboardState: StateFlow<DashboardSummary> = combine(
        credits, debitsAccount, debitsHand
    ) { creditsList, debitsAccList, debitsHandList ->
        computeSummary(creditsList, debitsAccList, debitsHandList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    init {
        // Only run sheets import automatically once on first boot if database is empty,
        // so the user starts with all their historical spreadsheet records imported.
        // After that, the app is 100% local and standalone!
        viewModelScope.launch {
            val hasImported = sharedPrefs.getBoolean("has_imported_initial_data", false)
            if (!hasImported) {
                _isSyncing.value = true
                _syncMessage.value = "Importing historical records from Google Sheets..."
                val result = repository.fetchAndSyncFromSheets()
                _isSyncing.value = false
                if (result.isSuccess) {
                    sharedPrefs.edit().putBoolean("has_imported_initial_data", true).apply()
                    _syncMessage.value = "Import successful! Ledger is now 100% local."
                } else {
                    _syncMessage.value = "Started local ledger. Import can be retried in settings."
                }
            }
        }
    }

    fun saveAppsScriptUrl(url: String) {
        _appsScriptUrl.value = url
        sharedPrefs.edit().putString("apps_script_url", url).apply()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun forceResetAndImportFromSheets() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Resetting and re-importing from Sheets..."
            val result = repository.fetchAndSyncFromSheets()
            _isSyncing.value = false
            if (result.isSuccess) {
                sharedPrefs.edit().putBoolean("has_imported_initial_data", true).apply()
                _syncMessage.value = "Database successfully reset and re-imported from Sheets!"
            } else {
                _syncMessage.value = "Failed to pull from Sheets. Local data preserved."
            }
        }
    }

    fun refreshFromSheets() {
        // Safe alias for manual refresh
        forceResetAndImportFromSheets()
    }

    fun generateAiReport() {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            _aiReport.value = "Generating smart AI ledger report..."
            val report = geminiService.generateLedgerInsights(
                credits = credits.value,
                debitsAccount = debitsAccount.value,
                debitsHand = debitsHand.value
            )
            _aiReport.value = report
            _isGeneratingReport.value = false
        }
    }

    fun clearAiReport() {
        _aiReport.value = null
    }

    fun addCredit(
        date: String,
        vendor: String,
        productName: String,
        productType: String,
        numberOfProduct: Int,
        costPerProduct: Double,
        salesPrice: Double,
        paymentStatus: String
    ) {
        viewModelScope.launch {
            val costOfProduct = numberOfProduct * costPerProduct
            val totalPrice = numberOfProduct * salesPrice
            val credit = CreditEntity(
                date = date,
                vendor = vendor,
                productName = productName,
                productType = productType,
                numberOfProduct = numberOfProduct,
                costPerProduct = costPerProduct,
                costOfProduct = costOfProduct,
                salesPrice = salesPrice,
                totalPrice = totalPrice,
                paymentStatus = paymentStatus,
                isSynced = false
            )
            repository.addCredit(credit)
            attemptRemoteSync("Credit", credit)
        }
    }

    fun addDebitAccount(
        date: String,
        source: String,
        productName: String,
        productType: String,
        numberOfProduct: Int,
        costPerProduct: Double,
        paymentStatus: String
    ) {
        viewModelScope.launch {
            val totalPrice = numberOfProduct * costPerProduct
            val debit = DebitAccountEntity(
                date = date,
                source = source,
                productName = productName,
                productType = productType,
                numberOfProduct = numberOfProduct,
                costPerProduct = costPerProduct,
                totalPrice = totalPrice,
                paymentStatus = paymentStatus,
                isSynced = false
            )
            repository.addDebitAccount(debit)
            attemptRemoteSync("DebitAccount", debit)
        }
    }

    fun addDebitHand(
        date: String,
        spentBy: String,
        vendor: String,
        description: String,
        qty: Double,
        rate: Double,
        amount: Double,
        status: String
    ) {
        viewModelScope.launch {
            val debit = DebitHandEntity(
                date = date,
                spentBy = spentBy,
                vendor = vendor,
                description = description,
                qty = qty,
                rate = rate,
                amount = amount,
                status = status,
                isSynced = false
            )
            repository.addDebitHand(debit)
            attemptRemoteSync("DebitHand", debit)
        }
    }

    fun deleteCreditItem(credit: CreditEntity) {
        viewModelScope.launch {
            repository.deleteCredit(credit.id)
        }
    }

    fun updateCreditItem(credit: CreditEntity) {
        viewModelScope.launch {
            val costOfProduct = credit.numberOfProduct * credit.costPerProduct
            val totalPrice = credit.numberOfProduct * credit.salesPrice
            val updated = credit.copy(
                costOfProduct = costOfProduct,
                totalPrice = totalPrice,
                isSynced = false // Ensure it's marked as locally entered/modified
            )
            repository.addCredit(updated)
        }
    }

    fun deleteDebitAccountItem(debit: DebitAccountEntity) {
        viewModelScope.launch {
            repository.deleteDebitAccount(debit.id)
        }
    }

    fun deleteDebitHandItem(debit: DebitHandEntity) {
        viewModelScope.launch {
            repository.deleteDebitHand(debit.id)
        }
    }

    private suspend fun attemptRemoteSync(type: String, entity: Any) {
        val url = appsScriptUrl.value
        if (url.isBlank()) {
            _syncMessage.value = "Transaction saved successfully!"
            return
        }

        _isSyncing.value = true
        _syncMessage.value = "Syncing record to remote Google Sheet..."

        val payload = JSONObject()
        when (type) {
            "Credit" -> {
                val e = entity as CreditEntity
                payload.apply {
                    put("Date", e.date)
                    put("Vendor", e.vendor)
                    put("ProductName", e.productName)
                    put("ProductType", e.productType)
                    put("NumberOfProduct", e.numberOfProduct)
                    put("CostPerProduct", e.costPerProduct)
                    put("CostOfProduct", e.costOfProduct)
                    put("SalesPrice", e.salesPrice)
                    put("TotalPrice", e.totalPrice)
                    put("PaymentStatus", e.paymentStatus)
                }
            }
            "DebitAccount" -> {
                val e = entity as DebitAccountEntity
                payload.apply {
                    put("Date", e.date)
                    put("Source", e.source)
                    put("ProductName", e.productName)
                    put("ProductType", e.productType)
                    put("NumberOfProduct", e.numberOfProduct)
                    put("CostPerProduct", e.costPerProduct)
                    put("TotalPrice", e.totalPrice)
                    put("PaymentStatus", e.paymentStatus)
                }
            }
            "DebitHand" -> {
                val e = entity as DebitHandEntity
                payload.apply {
                    put("Date", e.date)
                    put("SpentBy", e.spentBy)
                    put("Vendor", e.vendor)
                    put("Description", e.description)
                    put("Qty", e.qty)
                    put("Rate", e.rate)
                    put("Amount", e.amount)
                    put("Status", e.status)
                }
            }
        }

        val result = repository.syncItemToGoogleSheet(url, type, payload)
        _isSyncing.value = false
        if (result.isSuccess) {
            _syncMessage.value = "Successfully synced to Google Sheet!"
            // Mark as synced locally
            when (type) {
                "Credit" -> repository.addCredit((entity as CreditEntity).copy(isSynced = true))
                "DebitAccount" -> repository.addDebitAccount((entity as DebitAccountEntity).copy(isSynced = true))
                "DebitHand" -> repository.addDebitHand((entity as DebitHandEntity).copy(isSynced = true))
            }
        } else {
            _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}. Saved locally."
        }
    }

    private fun computeSummary(
        creditsList: List<CreditEntity>,
        debitsAccList: List<DebitAccountEntity>,
        debitsHandList: List<DebitHandEntity>
    ): DashboardSummary {
        val totalBankCredits = creditsList
            .filter { it.paymentStatus.contains("received", ignoreCase = true) }
            .sumOf { it.totalPrice }

        val pendingList = creditsList
            .filter { 
                it.paymentStatus.contains("yet to Receive", ignoreCase = true) || 
                it.paymentStatus.contains("pending", ignoreCase = true)
            }
        val pendingPayments = pendingList.sumOf { it.totalPrice }
        val pendingCount = pendingList.size
        val pendingQty = pendingList.sumOf { it.numberOfProduct }

        val totalBankDebits = debitsAccList.sumOf { it.totalPrice }
        val bankBalance = totalBankCredits - totalBankDebits

        val totalHandExpenses = debitsHandList.sumOf { it.amount }

        // Group Hand Expenses by Partner (Investment)
        val defaultPartners = listOf("Karthi", "Pradeeph", "Sankar", "Ravi Shankar")
        val partnerInvestments = defaultPartners.associateWith { 0.0 }.toMutableMap()
        for (item in debitsHandList) {
            val partner = item.spentBy.trim()
            if (partner.isNotEmpty()) {
                partnerInvestments[partner] = (partnerInvestments[partner] ?: 0.0) + item.amount
            }
        }

        return DashboardSummary(
            totalBankCredits = totalBankCredits,
            totalBankDebits = totalBankDebits,
            bankBalance = bankBalance,
            pendingPayments = pendingPayments,
            pendingCount = pendingCount,
            pendingQty = pendingQty,
            totalHandExpenses = totalHandExpenses,
            partnerInvestments = partnerInvestments
        )
    }

    // Factory
    class Factory(
        private val application: Application,
        private val repository: LedgerRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LedgerViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class DashboardSummary(
    val totalBankCredits: Double = 0.0,
    val totalBankDebits: Double = 0.0,
    val bankBalance: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val pendingCount: Int = 0,
    val pendingQty: Int = 0,
    val totalHandExpenses: Double = 0.0,
    val partnerInvestments: Map<String, Double> = emptyMap()
)
