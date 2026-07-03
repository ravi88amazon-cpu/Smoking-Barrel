package com.example

import android.app.Application
import com.example.ui.CreditViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CreditEntity
import com.example.data.DebitAccountEntity
import com.example.data.DebitHandEntity
import com.example.data.LedgerDatabase
import com.example.data.LedgerRepository
import com.example.ui.LedgerColors
import com.example.ui.LedgerViewModel
import com.example.ui.MetricCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.PartnerProgressRow
import com.example.ui.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog
import java.util.Calendar
import android.os.Environment
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CalendarToday

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val context = LocalContext.current
                val database = remember { LedgerDatabase.getDatabase(context) }
                val repository = remember { LedgerRepository(database.ledgerDao()) }
                val viewModel: LedgerViewModel = viewModel(
                    factory = LedgerViewModel.Factory(
                        application = context.applicationContext as Application,
                        repository = repository
                    )
                )

 MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("Dashboard") }

    // Observers
    val credits by viewModel.credits.collectAsState()
    val debitsAccount by viewModel.debitsAccount.collectAsState()
    val debitsHand by viewModel.debitsHand.collectAsState()
    val summary by viewModel.dashboardState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val aiReport by viewModel.aiReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()

    // Dialog trigger states
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var showAddDebitAccDialog by remember { mutableStateOf(false) }
    var showAddDebitHandDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var activeMetricDetail by remember { mutableStateOf<String?>(null) }
    var activeEditCredit by remember { mutableStateOf<CreditEntity?>(null) }
    var activeEditDebitAccount by remember { mutableStateOf<DebitAccountEntity?>(null) }

    // Toast of sync state changes
    remember(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerColors.DarkSlateBg),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerColors.DeepInk)
                    .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SMOKING BARREL",
                            color = Color(0xFFF97316),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Financial Ledger",
                                color = LedgerColors.MetallicSilver,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF97316),
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LedgerColors.NeonGreen)
                                )
                            }
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data",
                                tint = LedgerColors.MetallicSilver
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Navigation tabs/chips (Non-scrollable, all visible on one page)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Dashboard, Revenues, Expenses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Dashboard", "Sales", "Expenses").forEach { tabName ->
                            val isSelected = activeTab == tabName
                            val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.CardBg
                            val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { activeTab = tabName }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_${tabName.lowercase().replace(" & ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabName,
                                    color = textCol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Row 2: Investment, Reports & Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Investment", "Reports").forEach { tabName ->
                            val isSelected = activeTab == tabName
                            val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.CardBg
                            val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { activeTab = tabName }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_${tabName.lowercase().replace(" & ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabName,
                                    color = textCol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == "Revenues") {
                FloatingActionButton(
                    onClick = { showAddCreditDialog = true },
                    containerColor = Color(0xFFF97316),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_credit_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Credit")
                }
            } else if (activeTab == "Expenses") {
                FloatingActionButton(
                    onClick = { showAddDebitAccDialog = true },
                    containerColor = Color(0xFFF97316),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_bank_debit_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bank Debit")
                }
            } else if (activeTab == "Investment") {
                FloatingActionButton(
                    onClick = { showAddDebitHandDialog = true },
                    containerColor = Color(0xFFF97316),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_hand_debit_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Investment")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LedgerColors.DarkSlateBg)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "Dashboard" -> DashboardView(
                    summary = summary,
                    debitsHand = debitsHand,
                    isGeneratingReport = isGeneratingReport,
                    aiReport = aiReport,
                    onGenerateReport = { viewModel.generateAiReport() },
                    onClearReport = { viewModel.clearAiReport() },
                    onBankBalanceClick = { activeMetricDetail = "balance" },
                    onCreditsClick = { activeMetricDetail = "credits" },
                    onDebitsClick = { activeMetricDetail = "debits" },
                    onPendingClick = { activeMetricDetail = "pending" },
                    onOutOfPocketClick = { activeMetricDetail = "out_of_pocket" }
                )
                "Sales" -> CreditsView(
                    credits = credits,
                    onDelete = { viewModel.deleteCreditItem(it) },
                    onUpdate = { viewModel.updateCreditItem(it) },
                    onEditClick = { activeEditCredit = it }
                )
                "Expenses" -> DebitsView(
    debitsAccount = debitsAccount,
    onDeleteAccount = {
        viewModel.deleteDebitAccountItem(it)
    },
    onEditAccount = {
        activeEditDebitAccount = it
    }
)
                "Investment" -> InvestmentView(
                    debitsHand = debitsHand,
                    onDeleteHand = { viewModel.deleteDebitHandItem(it) }
                )
                "Reports" -> SettingsView(
                    viewModel = viewModel,
                    credits = credits,
                    debitsAccount = debitsAccount,
                    debitsHand = debitsHand,
                    context = context,
                    onResetDatabase = { showResetConfirmDialog = true }
                )
            }
        }
    }

    // Modal dialogs
    activeEditCredit?.let { credit ->
        EditCreditDialog(
            credit = credit,
            onDismiss = { activeEditCredit = null },
            onSave = { updated ->
                viewModel.updateCreditItem(updated)
                activeEditCredit = null
            }
        )
    }

    activeEditDebitAccount?.let { debit ->
    EditDebitAccountDialog(
        debit = debit,
        onDismiss = {
            activeEditDebitAccount = null
        },
        onSave = { updated ->
            viewModel.updateDebitAccountItem(updated)
            activeEditDebitAccount = null
        }
    )
}

    if (showAddCreditDialog) {
        AddCreditDialog(
            onDismiss = { showAddCreditDialog = false },
            onSave = { date, vendor, pName, pType, qty, costPer, sales, status ->
                viewModel.addCredit(date, vendor, pName, pType, qty, costPer, sales, status)
                showAddCreditDialog = false
            }
        )
    }

    if (showAddDebitAccDialog) {
        AddDebitAccDialog(
            onDismiss = { showAddDebitAccDialog = false },
            onSave = { date, source, pName, pType, qty, costPer, status ->
                viewModel.addDebitAccount(date, source, pName, pType, qty, costPer, status)
                showAddDebitAccDialog = false
            }
        )
    }

    if (showAddDebitHandDialog) {
        AddDebitHandDialog(
            onDismiss = { showAddDebitHandDialog = false },
            onSave = { date, spentBy, vendor, desc, qty, rate, amount, status ->
                viewModel.addDebitHand(date, spentBy, vendor, desc, qty, rate, amount, status)
                showAddDebitHandDialog = false
            }
        )
    }

    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LedgerColors.CardBg
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = LedgerColors.SoftRed,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Text(
                        text = "Reset & Re-Import?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Text(
                        text = "This will erase all your local modifications and re-import original historical entries from the master Google Spreadsheet CSV files.\n\nAny local-only entries you added since making the app offline-first will be permanently lost. Are you sure you want to proceed?",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showResetConfirmDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = LedgerColors.SlateGrayText, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                viewModel.forceResetAndImportFromSheets()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LedgerColors.SoftRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Reset & Import", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    activeMetricDetail?.let { metricType ->
        MetricDetailsModal(
            metricType = metricType,
            credits = credits,
            debitsAccount = debitsAccount,
            debitsHand = debitsHand,
            onDismiss = { activeMetricDetail = null },
            onDeleteCredit = { viewModel.deleteCreditItem(it) },
            onDeleteDebitAccount = { viewModel.deleteDebitAccountItem(it) },
            onDeleteDebitHand = { viewModel.deleteDebitHandItem(it) },
            onUpdateCredit = { viewModel.updateCreditItem(it) },
            onEditCredit = { activeEditCredit = it }
        )
    }
}

// ==========================================
// VIEW: DASHBOARD
// ==========================================
@Composable
fun DashboardView(
    summary: com.example.ui.DashboardSummary,
    debitsHand: List<DebitHandEntity>,
    isGeneratingReport: Boolean,
    aiReport: String?,
    onGenerateReport: () -> Unit,
    onClearReport: () -> Unit,
    onBankBalanceClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onDebitsClick: () -> Unit,
    onPendingClick: () -> Unit,
    onOutOfPocketClick: () -> Unit
) {
    var expandedPartner by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1 Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Current Bank Balance",
                value = formatCurrency(summary.bankBalance)",
                icon = Icons.Default.MonetizationOn,
                gradient = LedgerColors.GreenGradient,
                subtitle = "Available Business Balance",
                modifier = Modifier.weight(1f),
                onClick = onBankBalanceClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Sales Received",
                value = formatCurrency(summary.totalBankCredits),
                icon = Icons.Default.TrendingUp,
                gradient = LedgerColors.TechGradient,
                subtitle = "Payments Received",
                modifier = Modifier.weight(1f),
                onClick = onCreditsClick
            )
            MetricCard(
                title = "Business Expenses",
                value = formatCurrency(summary.totalBankDebits),
                icon = Icons.Default.TrendingDown,
                gradient = LedgerColors.FireGradient,
                subtitle = "Paid from Bank Account",
                modifier = Modifier.weight(1f),
                onClick = onDebitsClick
            )
        }

        // Row 3 Metrics (Pending & Out of pocket)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Outstanding Payments",
                value = when (summary.pendingQty) {
                    0 -> "No Outstanding Payments"
                    1 -> "1 Product"
                    else -> "${summary.pendingQty} Products"
                },
                icon = Icons.Default.Info,
                gradient = Brush.horizontalGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                subtitle = "${formatCurrency(summary.pendingPayments)} yet to collect",
                modifier = Modifier.weight(1f),
                onClick = onPendingClick
            )
            MetricCard(
                title = "Investment Made",
                value = formatCurrency(summary.totalHandExpenses),
                icon = Icons.Default.Payments,
                gradient = Brush.horizontalGradient(colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
                subtitle = "Invested Amount Details",
                modifier = Modifier.weight(1f),
                onClick = onOutOfPocketClick
            )
        }

        // Partner Investments Card (Matches Sheet 4)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Partner Investment Summary",
                    color = Color(0xFFF97316),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a name to view contribution details",
                    color = LedgerColors.SlateGrayText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Render progress bars for Karthi, Pradeeph, Sankar, Ravi Shankar
                summary.partnerInvestments.forEach { (partner, amount) ->
                    val isExpanded = expandedPartner == partner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedPartner = if (isExpanded) null else partner
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        PartnerProgressRow(
                            partnerName = partner,
                            amount = amount,
                            total = summary.totalHandExpenses,
                            isExpanded = isExpanded
                        )

                        if (isExpanded) {
                            val partnerItems = debitsHand.filter { it.spentBy.trim().equals(partner, ignoreCase = true) }
                            if (partnerItems.isEmpty()) {
                                Text(
                                    text = "No direct cash investments/expenses recorded for $partner.",
                                    color = LedgerColors.SlateGrayText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 12.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Investment History:",
                                        color = LedgerColors.SlateGrayText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    partnerItems.forEach { item ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = LedgerColors.DarkSlateBg)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.description.ifEmpty { item.vendor.ifEmpty { "Direct Cash Contribution" } },
                                                            color = LedgerColors.MetallicSilver,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (item.vendor.isNotEmpty() && item.description.isNotEmpty()) {
                                                            Text(
                                                                text = "Vendor: ${item.vendor}",
                                                                color = LedgerColors.SlateGrayText,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = item.date,
                                                            color = LedgerColors.SlateGrayText,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = formatCurrency(item.amount),
                                                            color = Color(0xFFF97316),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (item.qty > 0 || item.rate > 0) {
                                                            Text(
                                                                text = "${if (item.qty % 1 == 0.0) item.qty.toInt().toString() else item.qty} x Rs. ${String.format("%,.2f", item.rate)}",
                                                                color = LedgerColors.SlateGrayText,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        StatusBadge(statusText = item.status)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gemini AI Financial Health Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI FINANCIAL REPORT",
                            color = LedgerColors.MetallicSilver,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (aiReport != null && !isGeneratingReport) {
                        IconButton(onClick = onClearReport) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear analysis",
                                tint = LedgerColors.SlateGrayText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (aiReport == null) {
                    Text(
                        text = "Get automatic AI analysis of cash burn, revenue performance, and smart operations advice from Gemini based on current spreadsheet data.",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onGenerateReport,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_report_button")
                    ) {
                        Text("Generate Live AI Insights", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (isGeneratingReport) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFF97316),
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Running AI analysis on Smoking Barrel logs...",
                                color = LedgerColors.SlateGrayText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = aiReport,
                            color = LedgerColors.MetallicSilver,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW: CREDITS (REVENUE LIST)
// ==========================================
private fun getCreditMonthYear(credit: CreditEntity): String {
    return try {
        val date = parseLedgerDate(credit.date)
        if (date.time == 0L) {
            "Unknown"
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

@Composable
fun CreditsView(
    credits: List<CreditEntity>,
    onDelete: (CreditEntity) -> Unit,
    onUpdate: (CreditEntity) -> Unit,
    onEditClick: (CreditEntity) -> Unit
) {
    var selectedMonthYear by remember { mutableStateOf("All Months") }

    val monthYears = remember(credits) {
        val list = mutableListOf<String>()
        list.add("All Months")
        
        // Sort credits by date descending to get chronological order
        val sortedCredits = credits.sortedWith { a, b ->
            parseLedgerDate(b.date).compareTo(parseLedgerDate(a.date))
        }
        
        for (c in sortedCredits) {
            val my = getCreditMonthYear(c)
            if (my != "Unknown" && !list.contains(my)) {
                list.add(my)
            }
        }
        list
    }

    val filteredCredits = remember(credits, selectedMonthYear) {
        if (selectedMonthYear == "All Months") {
            credits
        } else {
            credits.filter { getCreditMonthYear(it) == selectedMonthYear }
        }
    }

    val totalRevenue = remember(filteredCredits) {
        filteredCredits.sumOf { it.totalPrice }
    }

    val collectedRevenue = remember(filteredCredits) {
        filteredCredits.filter { it.paymentStatus.contains("received", ignoreCase = true) }.sumOf { it.totalPrice }
    }

    val pendingRevenue = remember(filteredCredits) {
        filteredCredits.filter { 
            it.paymentStatus.contains("yet to Receive", ignoreCase = true) || 
            it.paymentStatus.contains("pending", ignoreCase = true) 
        }.sumOf { it.totalPrice }
    }

    if (credits.isEmpty()) {
        EmptyState(message = "No revenues cached. Click the refresh button top-right to pull latest entries.")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // MONTH FILTER CHIPS BAR
                Text(
                    text = "FILTER BY MONTH",
                    color = LedgerColors.SlateGrayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(monthYears) { month ->
                        val isSelected = month == selectedMonthYear
                        val bgCol = if (isSelected) Color(0xFFF97316) else LedgerColors.CardBg
                        val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgCol)
                                .clickable { selectedMonthYear = month }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = month,
                                color = textCol,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // REVENUE SUMMARY CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${selectedMonthYear.uppercase()} REVENUE SUMMARY",
                            color = Color(0xFFF97316),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL REVENUE", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                                Text(formatCurrency(totalRevenue), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("COLLECTED", color = LedgerColors.NeonGreen, fontSize = 9.sp)
                                Text(formatCurrency(collectedRevenue), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PENDING", color = LedgerColors.AmberOrange, fontSize = 9.sp)
                                Text(formatCurrency(collectedRevenue), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REVENUE LOGS (${selectedMonthYear})",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${filteredCredits.size} records",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(filteredCredits) { credit ->
                CreditCardItem(
                    credit = credit,
                    onDelete = { onDelete(credit) },
                    onEdit = { onEditClick(credit) },
                    onUpdate = onUpdate
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun CreditCardItem(
    credit: CreditEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onUpdate: (CreditEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credit_item_card_${credit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (credit.isSynced) LedgerColors.NeonGreen else LedgerColors.AmberOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = credit.date,
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(statusText = credit.paymentStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit item",
                            tint = LedgerColors.SlateGrayText.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = LedgerColors.SoftRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = credit.productName,
                color = LedgerColors.MetallicSilver,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vendor: ${credit.vendor} • Type: ${credit.productType}",
                color = LedgerColors.SlateGrayText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Quantity & Pricing",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${credit.numberOfProduct} pcs @ Rs. ${credit.salesPrice} (cost: Rs. ${credit.costPerProduct}/pc)",
                        color = LedgerColors.MetallicSilver,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL REVENUE",
                        color = Color(0xFFF97316),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(credit.totalPrice),
                        color = LedgerColors.MetallicSilver,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Quick Toggle Button if Pending
            val isPending = credit.paymentStatus.contains("yet to Receive", ignoreCase = true) ||
                            credit.paymentStatus.contains("pending", ignoreCase = true)
            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val updatedCredit = credit.copy(paymentStatus = "Payment received")
                        onUpdate(updatedCredit)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark as Received",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark as Received", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// VIEW: EXPENSES (DEBITS)
// ==========================================
private fun getDebitMonthYear(dateStr: String): String {
    return try {
        val date = parseLedgerDate(dateStr)
        if (date.time == 0L) {
            "Unknown"
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

@Composable
fun DebitsView(
    debitsAccount: List<DebitAccountEntity>,
    onDeleteAccount: (DebitAccountEntity) -> Unit,
    onEditAccount: (DebitAccountEntity) -> Unit
) {
    var selectedMonthYear by remember { mutableStateOf("All Months") }

    val monthYears = remember(debitsAccount) {
        val list = mutableListOf<String>()
        list.add("All Months")
        val sorted = debitsAccount.sortedWith { a, b ->
            parseLedgerDate(b.date).compareTo(parseLedgerDate(a.date))
        }
        for (d in sorted) {
            val my = getDebitMonthYear(d.date)
            if (my != "Unknown" && !list.contains(my)) {
                list.add(my)
            }
        }
        list
    }

    LaunchedEffect(monthYears) {
        if (!monthYears.contains(selectedMonthYear)) {
            selectedMonthYear = "All Months"
        }
    }

    val filteredDebitsAccount = remember(debitsAccount, selectedMonthYear) {
        if (selectedMonthYear == "All Months") {
            debitsAccount
        } else {
            debitsAccount.filter { getDebitMonthYear(it.date) == selectedMonthYear }
        }
    }

    val totalExpense = remember(filteredDebitsAccount) {
        filteredDebitsAccount.sumOf { it.totalPrice }
    }

    val clearedExpense = remember(filteredDebitsAccount) {
        filteredDebitsAccount.filter {
            it.paymentStatus.contains("received", ignoreCase = true) ||
            it.paymentStatus.contains("paid", ignoreCase = true) ||
            it.paymentStatus.contains("completed", ignoreCase = true) ||
            it.paymentStatus.isBlank()
        }.sumOf { it.totalPrice }
    }

    val pendingExpense = remember(totalExpense, clearedExpense) {
        totalExpense - clearedExpense
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // MONTH FILTER CHIPS BAR
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "FILTER BY MONTH",
                color = LedgerColors.SlateGrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(monthYears) { month ->
                    val isSelected = month == selectedMonthYear
                    val bgCol = if (isSelected) Color(0xFFF97316) else LedgerColors.CardBg
                    val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .clickable { selectedMonthYear = month }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = month,
                            color = textCol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EXPENSE SUMMARY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
                border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${selectedMonthYear.uppercase()} BANK DIRECT DEBITS",
                        color = Color(0xFFF97316),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TOTAL EXPENSE", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                            Text(formatCurrency(totalExpense)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CLEARED", color = LedgerColors.NeonGreen, fontSize = 9.sp)
                            Text(formatCurrency(pendingExpense)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PENDING", color = LedgerColors.AmberOrange, fontSize = 9.sp)
                            Text(formatCurrency(pendingExpense)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredDebitsAccount.isEmpty()) {
            EmptyState(message = "No Bank Debits cached for $selectedMonthYear.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BANK DIRECT DEBITS",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${filteredDebitsAccount.size} records",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(filteredDebitsAccount) { debit ->
    DebitAccountCardItem(
        debit = debit,
        onEdit = {
            onEditAccount(debit)
        },
        onDelete = {
            onDeleteAccount(debit)
        }
    )
}

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun InvestmentView(
    debitsHand: List<DebitHandEntity>,
    onDeleteHand: (DebitHandEntity) -> Unit
) {
    var selectedMonthYear by remember { mutableStateOf("All Months") }

    val monthYears = remember(debitsHand) {
        val list = mutableListOf<String>()
        list.add("All Months")
        val sorted = debitsHand.sortedWith { a, b ->
            parseLedgerDate(b.date).compareTo(parseLedgerDate(a.date))
        }
        for (d in sorted) {
            val my = getDebitMonthYear(d.date)
            if (my != "Unknown" && !list.contains(my)) {
                list.add(my)
            }
        }
        list
    }

    LaunchedEffect(monthYears) {
        if (!monthYears.contains(selectedMonthYear)) {
            selectedMonthYear = "All Months"
        }
    }

    val filteredDebitsHand = remember(debitsHand, selectedMonthYear) {
        if (selectedMonthYear == "All Months") {
            debitsHand
        } else {
            debitsHand.filter { getDebitMonthYear(it.date) == selectedMonthYear }
        }
    }

    val totalInvestment = remember(filteredDebitsHand) {
        filteredDebitsHand.sumOf { it.amount }
    }

    val clearedInvestment = remember(filteredDebitsHand) {
        filteredDebitsHand.filter {
            it.status.contains("received", ignoreCase = true) ||
            it.status.contains("paid", ignoreCase = true) ||
            it.status.contains("completed", ignoreCase = true) ||
            it.status.isBlank()
        }.sumOf { it.amount }
    }

    val pendingInvestment = remember(totalInvestment, clearedInvestment) {
        totalInvestment - clearedInvestment
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // MONTH FILTER CHIPS BAR
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "FILTER BY MONTH",
                color = LedgerColors.SlateGrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(monthYears) { month ->
                    val isSelected = month == selectedMonthYear
                    val bgCol = if (isSelected) Color(0xFFF97316) else LedgerColors.CardBg
                    val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .clickable { selectedMonthYear = month }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = month,
                            color = textCol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INVESTMENT SUMMARY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
                border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${selectedMonthYear.uppercase()} INVESTMENT DETAILS",
                        color = Color(0xFFF97316),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TOTAL INVESTED", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                            Text(formatCurrency(totalInvestment)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CLEARED", color = LedgerColors.NeonGreen, fontSize = 9.sp)
                            Text(formatCurrency(clearedInvestment)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PENDING", color = LedgerColors.AmberOrange, fontSize = 9.sp)
                            Text(formatCurrency(pendingInvestment)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredDebitsHand.isEmpty()) {
            EmptyState(message = "No Investment records cached for $selectedMonthYear.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INVESTED AMOUNT DETAILS",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${filteredDebitsHand.size} records",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(filteredDebitsHand) { debit ->
                    DebitHandCardItem(debit = debit, onDelete = { onDeleteHand(debit) })
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun DebitAccountCardItem(debit: DebitAccountEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debit_acc_card_${debit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (debit.isSynced) LedgerColors.NeonGreen else LedgerColors.AmberOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = debit.date,
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(statusText = debit.paymentStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
    onClick = onEdit
) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit"
    )
}
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete account debit",
                            tint = LedgerColors.SoftRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = debit.productName,
                color = LedgerColors.MetallicSilver,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Source: ${debit.source} • Category: ${debit.productType}",
                color = LedgerColors.SlateGrayText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Quantity & Unit cost",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${debit.numberOfProduct} pcs @ Rs. ${debit.costPerProduct}/pc",
                        color = LedgerColors.MetallicSilver,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SPENT",
                        color = LedgerColors.SoftRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(debit.totalPrice),
                        color = LedgerColors.MetallicSilver,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DebitHandCardItem(debit: DebitHandEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debit_hand_card_${debit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (debit.isSynced) LedgerColors.NeonGreen else LedgerColors.AmberOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = debit.date,
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(statusText = debit.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete hand debit",
                            tint = LedgerColors.SoftRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = debit.description,
                color = LedgerColors.MetallicSilver,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Paid by ",
                    color = LedgerColors.SlateGrayText,
                    fontSize = 12.sp
                )
                Text(
                    text = debit.spentBy,
                    color = Color(0xFFF97316),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • Vendor: ${debit.vendor}",
                    color = LedgerColors.SlateGrayText,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (debit.qty > 0 || debit.rate > 0) {
                        Text(
                            text = "Specs",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${debit.qty} Qty @ Rs. ${debit.rate}",
                            color = LedgerColors.MetallicSilver,
                            fontSize = 12.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SPENT",
                        color = LedgerColors.AmberOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(debit.amount),
                        color = LedgerColors.MetallicSilver,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ==========================================
// VIEW: REPORTS & SETTINGS INTEGRATION
// ==========================================
@Composable
fun SettingsView(
    viewModel: LedgerViewModel,
    credits: List<CreditEntity>,
    debitsAccount: List<DebitAccountEntity>,
    debitsHand: List<DebitHandEntity>,
    context: Context,
    onResetDatabase: () -> Unit
) {
    // State variables for report export configuration
    var reportType by remember { mutableStateOf("Overall Consolidated") } // "Overall Consolidated", "Revenue Ledger", "Expense Ledger"
    var exportFormat by remember { mutableStateOf("PDF Report") } // "PDF Report", "Excel / CSV"
    var periodType by remember { mutableStateOf("Month-wise") } // "Day-wise", "Week-wise", "Month-wise", "Year-wise", "Custom Date"

    // Initializer values
    val currentCal = Calendar.getInstance()
    val sdfDay = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    val todayStr = sdfDay.format(currentCal.time)

    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedWeekStartDate by remember { mutableStateOf(todayStr) }
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }
    var customStartDate by remember { mutableStateOf(todayStr) }
    var customEndDate by remember { mutableStateOf(todayStr) }

    // Helper to trigger Native DatePickerDialog
    fun showDatePicker(currentVal: String, onSelected: (String) -> Unit) {
        try {
            val cal = Calendar.getInstance()
            val parsedDate = sdfDay.parse(currentVal)
            if (parsedDate != null) {
                cal.time = parsedDate
            }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val dStr = String.format("%02d", dayOfMonth)
                    val mStr = String.format("%02d", month + 1)
                    onSelected("$dStr.$mStr.$year")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        } catch (e: Exception) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val dStr = String.format("%02d", dayOfMonth)
                    val mStr = String.format("%02d", month + 1)
                    onSelected("$dStr.$mStr.$year")
                },
                currentCal.get(Calendar.YEAR),
                currentCal.get(Calendar.MONTH),
                currentCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // Helper to calculate description of period
    val periodDesc = remember(periodType, selectedDate, selectedWeekStartDate, selectedMonth, selectedYear, customStartDate, customEndDate) {
        when (periodType) {
            "Day-wise" -> "Day $selectedDate"
            "Week-wise" -> "Week starting $selectedWeekStartDate"
            "Month-wise" -> {
                val cal = Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth - 1) }
                val monthName = SimpleDateFormat("MMMM", Locale.US).format(cal.time)
                "$monthName $selectedYear"
            }
            "Year-wise" -> "Year $selectedYear"
            "Custom Date" -> "From $customStartDate to $customEndDate"
            else -> "All Time"
        }
    }

    // Filtering logic
    val filteredCredits = remember(credits, periodType, selectedDate, selectedWeekStartDate, selectedMonth, selectedYear, customStartDate, customEndDate) {
        credits.filter {
            isDateInPeriod(
                parseLedgerDate(it.date),
                periodType,
                selectedDate,
                selectedWeekStartDate,
                selectedMonth,
                selectedYear,
                customStartDate,
                customEndDate
            )
        }
    }

    val filteredDebitsAccount = remember(debitsAccount, periodType, selectedDate, selectedWeekStartDate, selectedMonth, selectedYear, customStartDate, customEndDate) {
        debitsAccount.filter {
            isDateInPeriod(
                parseLedgerDate(it.date),
                periodType,
                selectedDate,
                selectedWeekStartDate,
                selectedMonth,
                selectedYear,
                customStartDate,
                customEndDate
            )
        }
    }

    val filteredDebitsHand = remember(debitsHand, periodType, selectedDate, selectedWeekStartDate, selectedMonth, selectedYear, customStartDate, customEndDate) {
        debitsHand.filter {
            isDateInPeriod(
                parseLedgerDate(it.date),
                periodType,
                selectedDate,
                selectedWeekStartDate,
                selectedMonth,
                selectedYear,
                customStartDate,
                customEndDate
            )
        }
    }

    val totalMatchedRecords = remember(reportType, filteredCredits, filteredDebitsAccount, filteredDebitsHand) {
        when (reportType) {
            "Revenue Ledger" -> filteredCredits.size
            "Expense Ledger" -> filteredDebitsAccount.size
            "Investment Ledger" -> filteredDebitsHand.size
            else -> filteredCredits.size + filteredDebitsAccount.size + filteredDebitsHand.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Title Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
            border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export Report",
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "FINANCIAL REPORT GENERATOR",
                        color = Color(0xFFF97316),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure and download overall statements, revenue summaries, or expense ledgers dynamically.",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Card 1: Select Report Type & Format
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. REPORT TYPE & FORMAT",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Report Types Selection
                Text("Select Report Scope:", color = LedgerColors.SlateGrayText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Overall Consolidated", "Revenue Ledger").forEach { opt ->
                            val isSelected = reportType == opt
                            val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.DeepInk
                            val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { reportType = opt }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (opt == "Overall Consolidated") "Consolidated" else "Revenues",
                                    color = textCol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Expense Ledger", "Investment Ledger").forEach { opt ->
                            val isSelected = reportType == opt
                            val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.DeepInk
                            val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { reportType = opt }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (opt == "Expense Ledger") "Expenses" else "Investments",
                                    color = textCol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format Selection
                Text("Select Document Format:", color = LedgerColors.SlateGrayText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("PDF Report", "Excel / CSV").forEach { format ->
                        val isSelected = exportFormat == format
                        val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.DeepInk
                        val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { exportFormat = format }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format,
                                color = textCol,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Select Date Range / Period
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. DATE & PERIOD FILTERS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Time Filter Selection Chips
                Text("Time Frame Option:", color = LedgerColors.SlateGrayText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val periodOptions = listOf("Day-wise", "Week-wise", "Month-wise", "Year-wise", "Custom Date")
                    items(periodOptions) { opt ->
                        val isSelected = periodType == opt
                        val bg = if (isSelected) Color(0xFFF97316) else LedgerColors.DeepInk
                        val textCol = if (isSelected) Color.White else LedgerColors.SlateGrayText
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { periodType = opt }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = opt,
                                color = textCol,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker Fields depending on PeriodType
                Text("Select Filter Criteria:", color = LedgerColors.SlateGrayText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))

                when (periodType) {
                    "Day-wise" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LedgerColors.DeepInk)
                                .clickable { showDatePicker(selectedDate) { selectedDate = it } }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Target Day: $selectedDate", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    "Week-wise" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LedgerColors.DeepInk)
                                .clickable { showDatePicker(selectedWeekStartDate) { selectedWeekStartDate = it } }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Week Starts On: $selectedWeekStartDate", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: Matches 7 continuous days starting from selected date.",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    "Month-wise" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Month Dropdown Box / Spinner
                            var isMonthExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LedgerColors.DeepInk)
                                    .clickable { isMonthExpanded = !isMonthExpanded }
                                    .padding(14.dp)
                            ) {
                                val monthName = SimpleDateFormat("MMMM", Locale.US).format(
                                    Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth - 1) }.time
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(monthName.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                                }

                                if (isMonthExpanded) {
                                    Dialog(onDismissRequest = { isMonthExpanded = false }) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .width(200.dp)
                                                    .height(300.dp)
                                                    .padding(8.dp)
                                            ) {
                                                items((1..12).toList()) { mNum ->
                                                    val mName = SimpleDateFormat("MMMM", Locale.US).format(
                                                        Calendar.getInstance().apply { set(Calendar.MONTH, mNum - 1) }.time
                                                    )
                                                    Text(
                                                        text = mName,
                                                        color = if (mNum == selectedMonth) Color(0xFFF97316) else Color.White,
                                                        fontWeight = if (mNum == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedMonth = mNum
                                                                isMonthExpanded = false
                                                            }
                                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Year Dropdown Box
                            var isYearExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LedgerColors.DeepInk)
                                    .clickable { isYearExpanded = !isYearExpanded }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$selectedYear", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                                }

                                if (isYearExpanded) {
                                    Dialog(onDismissRequest = { isYearExpanded = false }) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .height(200.dp)
                                                    .padding(8.dp)
                                            ) {
                                                items((2024..2030).toList()) { yr ->
                                                    Text(
                                                        text = "$yr",
                                                        color = if (yr == selectedYear) Color(0xFFF97316) else Color.White,
                                                        fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedYear = yr
                                                                isYearExpanded = false
                                                            }
                                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Year-wise" -> {
                        // Year Select
                        var isYearExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LedgerColors.DeepInk)
                                .clickable { isYearExpanded = !isYearExpanded }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Target Year: $selectedYear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            }

                            if (isYearExpanded) {
                                Dialog(onDismissRequest = { isYearExpanded = false }) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .width(200.dp)
                                                .height(220.dp)
                                                .padding(8.dp)
                                        ) {
                                            items((2024..2030).toList()) { yr ->
                                                Text(
                                                    text = "$yr",
                                                    color = if (yr == selectedYear) Color(0xFFF97316) else Color.White,
                                                    fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedYear = yr
                                                            isYearExpanded = false
                                                        }
                                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Custom Date" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LedgerColors.DeepInk)
                                    .clickable { showDatePicker(customStartDate) { customStartDate = it } }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("From: $customStartDate", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(14.dp))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LedgerColors.DeepInk)
                                    .clickable { showDatePicker(customEndDate) { customEndDate = it } }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("To: $customEndDate", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Preview Output count & status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.DeepInk),
            border = BorderStroke(1.dp, LedgerColors.SlateGrayText.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "3. FILTER METRIC PREVIEW",
                    color = Color(0xFFF97316),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("REVENUES MATCHED", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                        Text("${filteredCredits.size} items", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("BANK OUTFLOWS", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                        Text("${filteredDebitsAccount.size} items", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("INVESTED AMOUNT", color = LedgerColors.SlateGrayText, fontSize = 9.sp)
                        Text("${filteredDebitsHand.size} items", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val matchMsg = remember(reportType, totalMatchedRecords, periodDesc) {
                    when (reportType) {
                        "Revenue Ledger" -> "Selected target records represent $totalMatchedRecords revenues matching '$periodDesc'."
                        "Expense Ledger" -> "Selected target records represent $totalMatchedRecords bank outflows matching '$periodDesc'."
                        "Investment Ledger" -> "Selected target records represent $totalMatchedRecords investment items matching '$periodDesc'."
                        else -> "Selected target records represent $totalMatchedRecords total transactions matching '$periodDesc'."
                    }
                }
                Text(
                    text = matchMsg,
                    color = LedgerColors.SlateGrayText,
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // Export Actions Row
        Button(
            onClick = {
                if (totalMatchedRecords == 0) {
                    Toast.makeText(context, "No records found matching target filters!", Toast.LENGTH_SHORT).show()
                } else {
                    exportReport(
                        context = context,
                        reportType = reportType,
                        format = exportFormat,
                        filteredCredits = filteredCredits,
                        filteredDebitsAccount = filteredDebitsAccount,
                        filteredDebitsHand = filteredDebitsHand,
                        periodDesc = periodDesc
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("export_report_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DOWNLOAD ${exportFormat.uppercase()} REPORT",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Collapsed standalone controls to keep them safe and secondary
        var isResetExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, LedgerColors.SoftRed.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isResetExpanded = !isResetExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATABASE MAINTENANCE CONTROLS",
                        color = LedgerColors.SoftRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = if (isResetExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand maintenance controls",
                        tint = LedgerColors.SlateGrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isResetExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Everything is saved locally on this device. Resetting the database is irreversible.",
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onResetDatabase,
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerColors.SoftRed.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset & Clear Offline Database", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EXPORT & DOCUMENT GENERATION UTILITIES
// -------------------------------------------------------------
private fun exportReport(
    context: Context,
    reportType: String,
    format: String,
    filteredCredits: List<CreditEntity>,
    filteredDebitsAccount: List<DebitAccountEntity>,
    filteredDebitsHand: List<DebitHandEntity>,
    periodDesc: String
) {
    if (format == "PDF Report") {
        generateAndSharePdf(context, reportType, filteredCredits, filteredDebitsAccount, filteredDebitsHand, periodDesc)
    } else {
        generateAndShareCsv(context, reportType, filteredCredits, filteredDebitsAccount, filteredDebitsHand, periodDesc)
    }
}

private fun generateAndShareCsv(
    context: Context,
    reportType: String,
    credits: List<CreditEntity>,
    debitsAccount: List<DebitAccountEntity>,
    debitsHand: List<DebitHandEntity>,
    periodDesc: String
) {
    val csvString = java.lang.StringBuilder()
    val filename: String

    val safePeriodDesc = periodDesc.replace(" ", "_").replace(":", "_").replace("/", "_")

    when (reportType) {
        "Revenue Ledger" -> {
            filename = "revenue_report_${safePeriodDesc}.csv"
            csvString.append("S.No,Date,Vendor,Product Name,Product Type,Quantity,Rate,Cost of Product,Sales Price,Total Revenue,Payment Status\n")
            var totalRev = 0.0
            credits.forEachIndexed { idx, c ->
                csvString.append("${idx + 1},")
                    .append("${escapeCsv(c.date)},")
                    .append("${escapeCsv(c.vendor)},")
                    .append("${escapeCsv(c.productName)},")
                    .append("${escapeCsv(c.productType)},")
                    .append("${c.numberOfProduct},")
                    .append("${c.costPerProduct},")
                    .append("${c.costOfProduct},")
                    .append("${c.salesPrice},")
                    .append("${c.totalPrice},")
                    .append("${escapeCsv(c.paymentStatus)}\n")
                totalRev += c.totalPrice
            }
            csvString.append("\n")
            csvString.append("TOTAL MATCHED REVENUE,,,,,,,,,${totalRev},\n")
        }
        "Expense Ledger" -> {
            filename = "expense_report_${safePeriodDesc}.csv"
            csvString.append("S.No,Date,Type,Source_Partner,Product_Description,Product Type,Quantity,Rate,Total Amount,Status\n")
            var idx = 1
            var totalBank = 0.0
            debitsAccount.forEach { d ->
                csvString.append("$idx,")
                    .append("${escapeCsv(d.date)},")
                    .append("Bank Outflow,")
                    .append("${escapeCsv(d.source)},")
                    .append("${escapeCsv(d.productName)},")
                    .append("${escapeCsv(d.productType)},")
                    .append("${d.numberOfProduct},")
                    .append("${d.costPerProduct},")
                    .append("${d.totalPrice},")
                    .append("${escapeCsv(d.paymentStatus)}\n")
                idx++
                totalBank += d.totalPrice
            }
            csvString.append("\n")
            csvString.append("TOTAL BANK OUTFLOWS,,,,,,,,${totalBank},\n")
        }
        "Investment Ledger" -> {
            filename = "investment_report_${safePeriodDesc}.csv"
            csvString.append("S.No,Date,Type,Investor,Description,Quantity,Rate,Total Amount,Status\n")
            var idx = 1
            var totalCash = 0.0
            debitsHand.forEach { d ->
                csvString.append("$idx,")
                    .append("${escapeCsv(d.date)},")
                    .append("InvestmentDetail,")
                    .append("${escapeCsv(d.spentBy)},")
                    .append("${escapeCsv(d.vendor)} - ${escapeCsv(d.description)},")
                    .append("${d.qty},")
                    .append("${d.rate},")
                    .append("${d.amount},")
                    .append("${escapeCsv(d.status)}\n")
                idx++
                totalCash += d.amount
            }
            csvString.append("\n")
            csvString.append("TOTAL CASH INVESTED / SPENT,,,,,,,${totalCash},\n")
        }
        else -> { // Overall Consolidated
            filename = "overall_consolidated_report_${safePeriodDesc}.csv"
            csvString.append("S.No,Date,Category,Source_Partner,Description,Total Amount,Status\n")
            var idx = 1
            var totalRev = 0.0
            var totalBank = 0.0
            var totalCash = 0.0
            credits.forEach { c ->
                csvString.append("$idx,")
                    .append("${escapeCsv(c.date)},")
                    .append("REVENUE,")
                    .append("${escapeCsv(c.vendor)},")
                    .append("${escapeCsv(c.productName)} (${c.numberOfProduct} pcs),")
                    .append("${c.totalPrice},")
                    .append("${escapeCsv(c.paymentStatus)}\n")
                idx++
                totalRev += c.totalPrice
            }
            debitsAccount.forEach { d ->
                csvString.append("$idx,")
                    .append("${escapeCsv(d.date)},")
                    .append("EXPENSE (BANK),")
                    .append("${escapeCsv(d.source)},")
                    .append("${escapeCsv(d.productName)} (${d.numberOfProduct} pcs),")
                    .append("${d.totalPrice},")
                    .append("${escapeCsv(d.paymentStatus)}\n")
                idx++
                totalBank += d.totalPrice
            }
            debitsHand.forEach { d ->
                csvString.append("$idx,")
                    .append("${escapeCsv(d.date)},")
                    .append("INVESTMENT (CASH),")
                    .append("${escapeCsv(d.spentBy)},")
                    .append("${escapeCsv(d.vendor)} - ${escapeCsv(d.description)},")
                    .append("${d.amount},")
                    .append("${escapeCsv(d.status)}\n")
                idx++
                totalCash += d.amount
            }
            csvString.append("\n")
            csvString.append("TOTAL REVENUES,,,,,${totalRev},\n")
            csvString.append("TOTAL BANK OUTFLOWS,,,,,${totalBank},\n")
            csvString.append("TOTAL CASH INVESTED / SPENT,,,,,${totalCash},\n")
            csvString.append("NET ACCUMULATED FLOW,,,,,${totalRev - (totalBank + totalCash)},\n")
        }
    }

    try {
        val file = File(context.cacheDir, filename)
        val fos = FileOutputStream(file)
        fos.write(csvString.toString().toByteArray())
        fos.close()

        shareGeneratedFile(context, file, "text/csv")
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving Excel CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun escapeCsv(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
        "\"$clean\""
    } else {
        clean
    }
}

private fun generateAndSharePdf(
    context: Context,
    reportType: String,
    credits: List<CreditEntity>,
    debitsAccount: List<DebitAccountEntity>,
    debitsHand: List<DebitHandEntity>,
    periodDesc: String
) {
    val pdfDocument = PdfDocument()
    val paint = Paint()

    class PdfRow(val sNo: Int, val date: String, val category: String, val desc: String, val amount: Double, val status: String)
    val rows = mutableListOf<PdfRow>()

    var totalRev = 0.0
    var totalBankExpense = 0.0
    var totalInvestmentCash = 0.0
    var totalExp = 0.0

    when (reportType) {
        "Revenue Ledger" -> {
            credits.forEachIndexed { index, c ->
                rows.add(PdfRow(index + 1, c.date, "REVENUE", "${c.productName} (${c.vendor})", c.totalPrice, c.paymentStatus))
                totalRev += c.totalPrice
            }
        }
        "Expense Ledger" -> {
            var i = 1
            debitsAccount.forEach { d ->
                rows.add(PdfRow(i++, d.date, "BANK EXPENSE", "${d.productName} (${d.source})", d.totalPrice, d.paymentStatus))
                totalBankExpense += d.totalPrice
            }
            totalExp = totalBankExpense
        }
        "Investment Ledger" -> {
            var i = 1
            debitsHand.forEach { d ->
                rows.add(PdfRow(i++, d.date, "INVESTMENT", "${d.vendor} - ${d.description} (By ${d.spentBy})", d.amount, d.status))
                totalInvestmentCash += d.amount
            }
        }
        else -> { // Overall Consolidated
            var i = 1
            credits.forEach { c ->
                rows.add(PdfRow(i++, c.date, "REVENUE", "${c.productName} (${c.vendor})", c.totalPrice, c.paymentStatus))
                totalRev += c.totalPrice
            }
            debitsAccount.forEach { d ->
                rows.add(PdfRow(i++, d.date, "EXPENSE (BANK)", "${d.productName} (${d.source})", d.totalPrice, d.paymentStatus))
                totalBankExpense += d.totalPrice
            }
            debitsHand.forEach { d ->
                rows.add(PdfRow(i++, d.date, "INVESTMENT", "${d.vendor} - ${d.description}", d.amount, d.status))
                totalInvestmentCash += d.amount
            }
            totalExp = totalBankExpense + totalInvestmentCash
        }
    }

    val sortedRows = rows.sortedWith { a, b ->
        parseLedgerDate(b.date).compareTo(parseLedgerDate(a.date)) // Sort chronologically descending for sheets
    }

    val itemsPerPage = 22
    var pageNumber = 1
    val totalPages = Math.max(1, (sortedRows.size + itemsPerPage - 1) / itemsPerPage)

    var rowCounter = 0
    while (rowCounter < sortedRows.size || rowCounter == 0) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Draw header accent
        paint.color = 0xFFF97316.toInt()
        canvas.drawRect(40f, 40f, 555f, 48f, paint)

        // Title
        paint.color = 0xFF1F2937.toInt()
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(reportType.uppercase(), 40f, 75f, paint)

        // Subtitle
        paint.textSize = 9f
        paint.isFakeBoldText = false
        paint.color = 0xFF6B7280.toInt()
        canvas.drawText("PERIOD: ${periodDesc.uppercase()} | GENERATION TIME: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())}", 40f, 92f, paint)

        var tableHeaderY = 165f
        if (pageNumber == 1) {
            paint.color = 0xFFF3F4F6.toInt()
            canvas.drawRect(40f, 105f, 555f, 150f, paint)

            paint.color = 0xFF1F2937.toInt()
            paint.textSize = 8f
            paint.isFakeBoldText = true

            if (reportType == "Revenue Ledger") {
                canvas.drawText("TOTAL MATCHED REVENUE", 50f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFF10B981.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalRev)}", 50f, 138f, paint)
            } else if (reportType == "Expense Ledger") {
                var clearedBankExpense = 0.0
                debitsAccount.forEach { d ->
                    val isCleared = d.paymentStatus.contains("received", ignoreCase = true) ||
                            d.paymentStatus.contains("paid", ignoreCase = true) ||
                            d.paymentStatus.contains("completed", ignoreCase = true) ||
                            d.paymentStatus.isBlank()
                    if (isCleared) {
                        clearedBankExpense += d.totalPrice
                    }
                }
                val pendingBankExpense = totalBankExpense - clearedBankExpense

                canvas.drawText("TOTAL BANK OUTFLOW", 50f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalBankExpense)}", 50f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("CLEARED", 220f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFF10B981.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", clearedBankExpense)}", 220f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("PENDING", 390f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", pendingBankExpense)}", 390f, 138f, paint)
            } else if (reportType == "Investment Ledger") {
                var clearedInvestment = 0.0
                debitsHand.forEach { d ->
                    val isCleared = d.status.contains("received", ignoreCase = true) ||
                            d.status.contains("paid", ignoreCase = true) ||
                            d.status.contains("completed", ignoreCase = true) ||
                            d.status.isBlank()
                    if (isCleared) {
                        clearedInvestment += d.amount
                    }
                }
                val pendingInvestment = totalInvestmentCash - clearedInvestment

                canvas.drawText("TOTAL INVESTED", 50f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFFF97316.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalInvestmentCash)}", 50f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("CLEARED", 220f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFF10B981.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", clearedInvestment)}", 220f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("PENDING", 390f, 122f, paint)
                paint.textSize = 12f
                paint.color = 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", pendingInvestment)}", 390f, 138f, paint)
            } else { // Overall Consolidated
                canvas.drawText("TOTAL REVENUE", 50f, 122f, paint)
                paint.textSize = 11f
                paint.color = 0xFF10B981.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalRev)}", 50f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("BANK EXPENSES", 175f, 122f, paint)
                paint.textSize = 11f
                paint.color = 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalBankExpense)}", 175f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("CASH SPENT", 300f, 122f, paint)
                paint.textSize = 11f
                paint.color = 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", totalInvestmentCash)}", 300f, 138f, paint)

                paint.color = 0xFF1F2937.toInt()
                paint.textSize = 8f
                canvas.drawText("NET FLOW", 425f, 122f, paint)
                paint.textSize = 11f
                val net = totalRev - totalExp
                paint.color = if (net >= 0) 0xFF10B981.toInt() else 0xFFEF4444.toInt()
                canvas.drawText("Rs. ${String.format("%,.2f", net)}", 425f, 138f, paint)
            }
            tableHeaderY = 175f
        } else {
            tableHeaderY = 120f
        }

        // Draw Table Headers
        paint.color = 0xFF374151.toInt()
        paint.textSize = 9f
        paint.isFakeBoldText = true

        canvas.drawText("S.No", 40f, tableHeaderY, paint)
        canvas.drawText("Date", 75f, tableHeaderY, paint)
        canvas.drawText("Particulars & Description", 145f, tableHeaderY, paint)
        canvas.drawText("Amount", 420f, tableHeaderY, paint)
        canvas.drawText("Status", 500f, tableHeaderY, paint)

        // Line below headers
        paint.strokeWidth = 1f
        paint.color = 0xFFD1D5DB.toInt()
        canvas.drawLine(40f, tableHeaderY + 5f, 555f, tableHeaderY + 5f, paint)

        var currentY = tableHeaderY + 22f
        paint.isFakeBoldText = false
        var drawnCount = 0

        while (rowCounter < sortedRows.size && drawnCount < itemsPerPage) {
            val r = sortedRows[rowCounter]
            paint.color = 0xFF1F2937.toInt()

            canvas.drawText("${rowCounter + 1}", 40f, currentY, paint)
            canvas.drawText(r.date, 75f, currentY, paint)

            var descStr = r.desc
            if (descStr.length > 34) descStr = descStr.take(31) + "..."
            canvas.drawText("$descStr (${r.category})", 145f, currentY, paint)

            if (r.category.contains("REVENUE", ignoreCase = true)) {
                paint.color = 0xFF059669.toInt()
                canvas.drawText("+${String.format("%,.2f", r.amount)}", 420f, currentY, paint)
            } else {
                paint.color = 0xFFDC2626.toInt()
                canvas.drawText("-${String.format("%,.2f", r.amount)}", 420f, currentY, paint)
            }

            paint.color = 0xFF4B5563.toInt()
            var stStr = r.status
            if (stStr.length > 10) stStr = stStr.take(8) + ".."
            canvas.drawText(stStr, 500f, currentY, paint)

            currentY += 24f
            rowCounter++
            drawnCount++
        }

        // Footer
        paint.color = 0xFF9CA3AF.toInt()
        paint.textSize = 8f
        canvas.drawText("Financial Statement • Page $pageNumber of $totalPages", 40f, 810f, paint)
        canvas.drawText("Generated Securely by Ledger Mobile Client Engine", 370f, 810f, paint)

        pdfDocument.finishPage(page)
        pageNumber++
        if (rowCounter >= sortedRows.size) break
    }

    val safePeriodDesc = periodDesc.replace(" ", "_").replace(":", "_").replace("/", "_")
    val filename = "ledger_report_${safePeriodDesc}.pdf"
    try {
        val file = File(context.cacheDir, filename)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()

        shareGeneratedFile(context, file, "application/pdf")
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun shareGeneratedFile(context: Context, file: File, mimeType: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ledger Statement - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is the exported ledger data report matching your search filter criteria.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Download / Share Statement:"))
    } catch (e: Exception) {
        Toast.makeText(context, "File share failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun isDateInPeriod(
    itemDate: Date,
    periodType: String,
    selectedDateStr: String,
    selectedWeekStartStr: String,
    selectedMonth: Int,
    selectedYear: Int,
    customStartStr: String,
    customEndStr: String
): Boolean {
    val itemCal = Calendar.getInstance().apply { time = itemDate }
    if (itemDate.time == 0L) return false

    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)

    return when (periodType) {
        "Day-wise" -> {
            val targetDate = try { sdf.parse(selectedDateStr) } catch(e: Exception) { null }
            if (targetDate == null) false
            else {
                val targetCal = Calendar.getInstance().apply { time = targetDate }
                itemCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                itemCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            }
        }
        "Week-wise" -> {
            val targetStartDate = try { sdf.parse(selectedWeekStartStr) } catch(e: Exception) { null }
            if (targetStartDate == null) false
            else {
                val targetEndCal = Calendar.getInstance().apply {
                    time = targetStartDate
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val itemTime = itemDate.time
                itemTime >= targetStartDate.time && itemTime <= targetEndCal.timeInMillis
            }
        }
        "Month-wise" -> {
            itemCal.get(Calendar.YEAR) == selectedYear &&
            (itemCal.get(Calendar.MONTH) + 1) == selectedMonth
        }
        "Year-wise" -> {
            itemCal.get(Calendar.YEAR) == selectedYear
        }
        "Custom Date" -> {
            val start = try { sdf.parse(customStartStr) } catch(e: Exception) { null }
            val end = try { sdf.parse(customEndStr) } catch(e: Exception) { null }
            if (start == null || end == null) false
            else {
                val endCal = Calendar.getInstance().apply {
                    time = end
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                itemDate.time >= start.time && itemDate.time <= endCal.timeInMillis
            }
        }
        else -> true
    }
}

// ==========================================
// SHARED EMPTY STATE COMPONENT
// ==========================================
@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ListAlt,
            contentDescription = null,
            tint = LedgerColors.SlateGrayText.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = LedgerColors.SlateGrayText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

// ==========================================
// MODAL DIALOGS: ADD LEDGER RECORDS
// ==========================================

@Composable
fun EditCreditDialog(
    credit: CreditEntity,
    onDismiss: () -> Unit,
    onSave: (CreditEntity) -> Unit
) {
    var date by remember { mutableStateOf(credit.date) }
    var vendor by remember { mutableStateOf(credit.vendor) }
    var pName by remember { mutableStateOf(credit.productName) }
    var pType by remember { mutableStateOf(credit.productType) }
    var qtyString by remember { mutableStateOf(credit.numberOfProduct.toString()) }
    var costPerString by remember { mutableStateOf(credit.costPerProduct.toString()) }
    var salesPriceString by remember { mutableStateOf(credit.salesPrice.toString()) }
    var paymentStatus by remember { mutableStateOf(credit.paymentStatus) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.CardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Edit Credit Entry (Revenue)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Render Fields
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 29-Jun-26)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_credit_date_field")
                )

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_credit_vendor_field")
                )

                OutlinedTextField(
                    value = pName,
                    onValueChange = { pName = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_credit_product_name_field")
                )

                OutlinedTextField(
                    value = pType,
                    onValueChange = { pType = it },
                    label = { Text("Product Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_credit_product_type_field")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_credit_qty_field")
                    )
                    OutlinedTextField(
                        value = costPerString,
                        onValueChange = { costPerString = it },
                        label = { Text("Cost/Product") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_credit_cost_field")
                    )
                }

                OutlinedTextField(
                    value = salesPriceString,
                    onValueChange = { salesPriceString = it },
                    label = { Text("Sales Price/Product") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_credit_sales_field")
                )

                // Payment Status Chips
                Text("Payment Status:", color = LedgerColors.SlateGrayText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Payment received", "Payment yet to Receive").forEach { status ->
                        val isSel = paymentStatus == status
                        val col = if (isSel) Color(0xFFF97316) else LedgerColors.DeepInk
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(col)
                                .clickable { paymentStatus = status }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (status == "Payment yet to Receive") "Pending" else "Received",
                                color = if (isSel) Color.White else LedgerColors.SlateGrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LedgerColors.SlateGrayText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyString.toIntOrNull() ?: 1
                            val cost = costPerString.toDoubleOrNull() ?: 0.0
                            val sales = salesPriceString.toDoubleOrNull() ?: 0.0
                            onSave(
                                credit.copy(
                                    date = date,
                                    vendor = vendor,
                                    productName = pName,
                                    productType = pType,
                                    numberOfProduct = qty,
                                    costPerProduct = cost,
                                    salesPrice = sales,
                                    paymentStatus = paymentStatus
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        modifier = Modifier.testTag("edit_credit_dialog_save_btn")
                    ) {
                        Text("Save Changes", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EditDebitAccountDialog(
    debit: DebitAccountEntity,
    onDismiss: () -> Unit,
    onSave: (DebitAccountEntity) -> Unit
) {

    var date by remember { mutableStateOf(debit.date) }
    var source by remember { mutableStateOf(debit.source) }
    var productName by remember { mutableStateOf(debit.productName) }
    var productType by remember { mutableStateOf(debit.productType) }
    var qtyString by remember { mutableStateOf(debit.numberOfProduct.toString()) }
    var costPerString by remember { mutableStateOf(debit.costPerProduct.toString()) }
    var paymentStatus by remember { mutableStateOf(debit.paymentStatus) }

    Dialog(onDismissRequest = onDismiss) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.CardBg
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Edit Expense Entry",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
    value = date,
    onValueChange = { date = it },
    label = { Text("Date (e.g. 29-Jun-26)") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

OutlinedTextField(
    value = source,
    onValueChange = { source = it },
    label = { Text("Source") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

OutlinedTextField(
    value = productName,
    onValueChange = { productName = it },
    label = { Text("Product Name") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

OutlinedTextField(
    value = productType,
    onValueChange = { productType = it },
    label = { Text("Product Type") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {

    OutlinedTextField(
        value = qtyString,
        onValueChange = { qtyString = it },
        label = { Text("Quantity") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f)
    )

    OutlinedTextField(
        value = costPerString,
        onValueChange = { costPerString = it },
        label = { Text("Cost/Product") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f)
    )
}

Text(
    text = "Payment Status",
    color = LedgerColors.SlateGrayText,
    fontSize = 12.sp
)

Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {

    listOf(
        "Payment received",
        "Payment yet to Receive"
    ).forEach { status ->

        val selected = paymentStatus == status

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected)
                        Color(0xFFF97316)
                    else
                        LedgerColors.DeepInk
                )
                .clickable {
                    paymentStatus = status
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {

            Text(
                text = if (status == "Payment yet to Receive")
                    "Pending"
                else
                    "Received",
                color = if (selected)
                    Color.White
                else
                    LedgerColors.SlateGrayText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

Spacer(modifier = Modifier.height(10.dp))
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
) {

    TextButton(
        onClick = onDismiss
    ) {
        Text(
            "Cancel",
            color = LedgerColors.SlateGrayText
        )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Button(
        onClick = {

            val qty = qtyString.toIntOrNull() ?: 0
            val cost = costPerString.toDoubleOrNull() ?: 0.0

            onSave(
                debit.copy(
                    date = date,
                    source = source,
                    productName = productName,
                    productType = productType,
                    numberOfProduct = qty,
                    costPerProduct = cost,
                    totalPrice = qty * cost,
                    paymentStatus = paymentStatus
                )
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF97316)
        )
    ) {
        Text(
            "Save Changes",
            color = Color.White
        )
    }
}

            }
        }
    }
}

@Composable
fun AddCreditDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, vendor: String, pName: String, pType: String, qty: Int, costPer: Double, sales: Double, status: String) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yy", Locale.US).format(Date())) }
    var vendor by remember { mutableStateOf("") }
    var pName by remember { mutableStateOf("") }
    var pType by remember { mutableStateOf("") }
    var qtyString by remember { mutableStateOf("") }
    var costPerString by remember { mutableStateOf("") }
    var salesPriceString by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("Payment received") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.CardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Credit Entry (Revenue)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Render Fields
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 29-Jun-26)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_date_field")
                )

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor Name (e.g. Meesho, Prabhakar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_vendor_field")
                )

                OutlinedTextField(
                    value = pName,
                    onValueChange = { pName = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_product_name_field")
                )

                OutlinedTextField(
                    value = pType,
                    onValueChange = { pType = it },
                    label = { Text("Product Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_product_type_field")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("credit_qty_field")
                    )
                    OutlinedTextField(
                        value = costPerString,
                        onValueChange = { costPerString = it },
                        label = { Text("Cost/Product") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("credit_cost_field")
                    )
                }

                OutlinedTextField(
                    value = salesPriceString,
                    onValueChange = { salesPriceString = it },
                    label = { Text("Sales Price/Product") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_sales_field")
                )

                // Payment Status Chips
                Text("Payment Status:", color = LedgerColors.SlateGrayText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Payment received", "Payment yet to Receive").forEach { status ->
                        val isSel = paymentStatus == status
                        val col = if (isSel) Color(0xFFF97316) else LedgerColors.DeepInk
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(col)
                                .clickable { paymentStatus = status }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (status == "Payment yet to Receive") "Pending" else "Received",
                                color = if (isSel) Color.White else LedgerColors.SlateGrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LedgerColors.SlateGrayText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyString.toIntOrNull() ?: 1
                            val cost = costPerString.toDoubleOrNull() ?: 0.0
                            val sales = salesPriceString.toDoubleOrNull() ?: 0.0
                            onSave(date, vendor, pName, pType, qty, cost, sales, paymentStatus)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        modifier = Modifier.testTag("credit_dialog_save_btn")
                    ) {
                        Text("Add Record", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebitAccDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, source: String, pName: String, pType: String, qty: Int, costPer: Double, status: String) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yy", Locale.US).format(Date())) }
    var source by remember { mutableStateOf("") }
    var pName by remember { mutableStateOf("") }
    var pType by remember { mutableStateOf("") }
    var qtyString by remember { mutableStateOf("") }
    var costPerString by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Paid from Account") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.CardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Bank Account Debit",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 29-Jun-26)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source (e.g. JJ Garment, Jio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pName,
                    onValueChange = { pName = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pType,
                    onValueChange = { pType = it },
                    label = { Text("Product Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costPerString,
                        onValueChange = { costPerString = it },
                        label = { Text("Cost per Product") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LedgerColors.SlateGrayText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyString.toIntOrNull() ?: 1
                            val cost = costPerString.toDoubleOrNull() ?: 0.0
                            onSave(date, source, pName, pType, qty, cost, status)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        modifier = Modifier.testTag("debit_acc_dialog_save_btn")
                    ) {
                        Text("Add Record", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebitHandDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, spentBy: String, vendor: String, desc: String, qty: Double, rate: Double, amount: Double, status: String) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())) }
    var spentBy by remember { mutableStateOf("Karthi") }
    var vendor by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var qtyString by remember { mutableStateOf("") }
    var rateString by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Paid") }

    val partners = listOf("Karthi", "Pradeeph", "Sankar", "Ravi Shankar")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.CardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Invested Amount Detail",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 29.12.2025)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Partner Selection Dropdown/Chips
                Text("Spent By Partner:", color = LedgerColors.SlateGrayText, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        partners.take(2).forEach { name ->
                            val isSel = spentBy == name
                            val col = if (isSel) Color(0xFFF97316) else LedgerColors.DeepInk
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(col)
                                    .clickable { spentBy = name }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) Color.White else LedgerColors.SlateGrayText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        partners.takeLast(2).forEach { name ->
                            val isSel = spentBy == name
                            val col = if (isSel) Color(0xFFF97316) else LedgerColors.DeepInk
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(col)
                                    .clickable { spentBy = name }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) Color.White else LedgerColors.SlateGrayText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor (e.g. Balaji Steam, Amazon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("Qty (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateString,
                        onValueChange = { rateString = it },
                        label = { Text("Rate (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Total Amount spent (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = {
                        val q = qtyString.toDoubleOrNull() ?: 0.0
                        val r = rateString.toDoubleOrNull() ?: 0.0
                        val auto = if (q > 0 && r > 0) "${q * r}" else ""
                        Text(auto)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LedgerColors.SlateGrayText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = qtyString.toDoubleOrNull() ?: 0.0
                            val r = rateString.toDoubleOrNull() ?: 0.0
                            val amt = amountString.toDoubleOrNull() ?: (q * r)
                            onSave(date, spentBy, vendor, desc, q, r, amt, status)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        modifier = Modifier.testTag("debit_hand_dialog_save_btn")
                    ) {
                        Text("Add Record", color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW: METRIC DETAILS MODAL
// ==========================================
sealed class UnifiedTransaction {
    data class Credit(val entity: CreditEntity) : UnifiedTransaction()
    data class DebitAccount(val entity: DebitAccountEntity) : UnifiedTransaction()
    data class DebitHand(val entity: DebitHandEntity) : UnifiedTransaction()
    
    val date: String
        get() = when (this) {
            is Credit -> entity.date
            is DebitAccount -> entity.date
            is DebitHand -> entity.date
        }
        
    val amount: Double
        get() = when (this) {
            is Credit -> entity.totalPrice
            is DebitAccount -> entity.totalPrice
            is DebitHand -> entity.amount
        }
}

private fun parseLedgerDate(dateStr: String): java.util.Date {
    val formats = listOf("dd-MMM-yy", "dd.MM.yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
    for (fmt in formats) {
        try {
            return SimpleDateFormat(fmt, Locale.US).parse(dateStr.trim()) ?: java.util.Date(0)
        } catch (e: Exception) {
            // continue
        }
    }
    return java.util.Date(0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailsModal(
    metricType: String,
    credits: List<CreditEntity>,
    debitsAccount: List<DebitAccountEntity>,
    debitsHand: List<DebitHandEntity>,
    onDismiss: () -> Unit,
    onDeleteCredit: (CreditEntity) -> Unit,
    onDeleteDebitAccount: (DebitAccountEntity) -> Unit,
    onDeleteDebitHand: (DebitHandEntity) -> Unit,
    onUpdateCredit: (CreditEntity) -> Unit,
    onEditCredit: (CreditEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val themeColor = when (metricType) {
        "balance" -> Color(0xFF10B981) // Green
        "credits" -> Color(0xFF3B82F6) // Tech Blue
        "debits" -> Color(0xFFEF4444) // Fire Red
        "pending" -> Color(0xFFF59E0B) // Amber Orange
        "out_of_pocket" -> Color(0xFFEC4899) // Hot Pink
        else -> Color(0xFFF97316)
    }

    val title = when (metricType) {
        "balance" -> "Bank Ledger Transactions"
        "credits" -> "Bank Credits Details"
        "debits" -> "Bank Debits Details"
        "pending" -> "Pending Payments Details"
        "out_of_pocket" -> "Invested Amount Details"
        else -> "Transaction Details"
    }

    val rawList = remember(metricType, credits, debitsAccount, debitsHand) {
        when (metricType) {
            "balance" -> {
                val bankCredits = credits.filter { it.paymentStatus.contains("received", ignoreCase = true) }.map { UnifiedTransaction.Credit(it) }
                val bankDebits = debitsAccount.map { UnifiedTransaction.DebitAccount(it) }
                (bankCredits + bankDebits)
            }
            "credits" -> {
                credits.filter { it.paymentStatus.contains("received", ignoreCase = true) }.map { UnifiedTransaction.Credit(it) }
            }
            "debits" -> {
                debitsAccount.map { UnifiedTransaction.DebitAccount(it) }
            }
            "pending" -> {
                credits.filter { 
                    it.paymentStatus.contains("yet to Receive", ignoreCase = true) || 
                    it.paymentStatus.contains("pending", ignoreCase = true) 
                }.map { UnifiedTransaction.Credit(it) }
            }
            "out_of_pocket" -> {
                debitsHand.map { UnifiedTransaction.DebitHand(it) }
            }
            else -> emptyList()
        }
    }

    val sortedList = remember(rawList) {
        rawList.sortedWith { a, b ->
            val dateA = parseLedgerDate(a.date)
            val dateB = parseLedgerDate(b.date)
            dateB.compareTo(dateA) // Descending (latest first)
        }
    }

    val filteredList = remember(sortedList, searchQuery) {
        if (searchQuery.isBlank()) sortedList else {
            val q = searchQuery.trim().lowercase()
            sortedList.filter { item ->
                when (item) {
                    is UnifiedTransaction.Credit -> {
                        item.entity.productName.lowercase().contains(q) ||
                        item.entity.vendor.lowercase().contains(q) ||
                        item.entity.productType.lowercase().contains(q)
                    }
                    is UnifiedTransaction.DebitAccount -> {
                        item.entity.productName.lowercase().contains(q) ||
                        item.entity.source.lowercase().contains(q) ||
                        item.entity.productType.lowercase().contains(q)
                    }
                    is UnifiedTransaction.DebitHand -> {
                        item.entity.description.lowercase().contains(q) ||
                        item.entity.vendor.lowercase().contains(q) ||
                        item.entity.spentBy.lowercase().contains(q)
                    }
                }
            }
        }
    }

    val filteredTotal = remember(filteredList) {
        filteredList.sumOf { item ->
            when (item) {
                is UnifiedTransaction.Credit -> item.entity.totalPrice
                is UnifiedTransaction.DebitAccount -> {
                    if (metricType == "balance") -item.entity.totalPrice else item.entity.totalPrice
                }
                is UnifiedTransaction.DebitHand -> item.entity.amount
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = LedgerColors.DarkSlateBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title.uppercase(),
                            color = themeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (metricType == "balance") {
                                "Net Balance: Rs. ${String.format("%,.2f", filteredTotal)}"
                            } else {
                                "Total: Rs. ${String.format("%,.2f", filteredTotal)}"
                            },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(LedgerColors.CardBg, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...", color = LedgerColors.SlateGrayText, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = LedgerColors.SlateGrayText.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = LedgerColors.DeepInk,
                        unfocusedContainerColor = LedgerColors.DeepInk
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = LedgerColors.SlateGrayText
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = LedgerColors.SlateGrayText
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No transactions found." else "No matching transactions.",
                            color = LedgerColors.SlateGrayText,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList) { item ->
                            when (item) {
                                is UnifiedTransaction.Credit -> {
                                    CreditCardItem(
                                        credit = item.entity,
                                        onDelete = { onDeleteCredit(item.entity) },
                                        onEdit = { onEditCredit(item.entity) },
                                        onUpdate = onUpdateCredit
                                    )
                                }
                                is UnifiedTransaction.DebitAccount -> {
                                    DebitAccountCardItem(debit = item.entity, onEdit = {  }, onDelete = { onDeleteDebitAccount(item.entity) })
                                }
                                is UnifiedTransaction.DebitHand -> {
                                    DebitHandCardItem(debit = item.entity, onDelete = { onDeleteDebitHand(item.entity) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    fun formatCurrency(amount: Double): String {
    return "₹%,.2f".format(amount)
}
}


