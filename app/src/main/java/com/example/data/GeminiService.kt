package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun generateLedgerInsights(
        credits: List<CreditEntity>,
        debitsAccount: List<DebitAccountEntity>,
        debitsHand: List<DebitHandEntity>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key not configured. Please add your GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        // Format a compact summary of the ledger to send as context
        val creditSummary = credits.take(15).joinToString("\n") { 
            "- Date: ${it.date}, Vendor: ${it.vendor}, Product: ${it.productName}, Qty: ${it.numberOfProduct}, Total Price: Rs.${it.totalPrice}, Status: ${it.paymentStatus}" 
        }
        val debitsAccSummary = debitsAccount.take(15).joinToString("\n") { 
            "- Date: ${it.date}, Source: ${it.source}, Product: ${it.productName}, Qty: ${it.numberOfProduct}, Total Price: Rs.${it.totalPrice}" 
        }
        val debitsHandSummary = debitsHand.take(15).joinToString("\n") { 
            "- Date: ${it.date}, Partner: ${it.spentBy}, Vendor: ${it.vendor}, Desc: ${it.description}, Qty: ${it.qty}, Amount: Rs.${it.amount}" 
        }

        val prompt = """
            You are a professional business financial analyst. Analyze this ledger data for the "Smoking Barrel" apparel brand:

            ### CREDIT (REVENUE & INFLOWS)
            $creditSummary

            ### BANK ACCOUNT DEBITS (OUTFLOWS)
            $debitsAccSummary

            ### HAND DEBITS (OUT-OF-POCKET EXPENSES BY PARTNERS)
            $debitsHandSummary

            Please write a short, high-level, highly professional analysis. Give 3 clear bullet points:
            1. Revenue Analysis: Sum up recent income and discuss any notable sales (e.g. Meesho vs Prabhakar / Infognana).
            2. Cash Burn & Spending: Analyze Bank Account Debits and Hand Debits, identifying key cost centers (e.g., garments, prints, yarn, heat press machine purchases).
            3. Operational Advice: Actionable business advice on pricing, partner investments, or collection of pending payments.
            
            Keep the tone clean, objective, motivating, and professional. Avoid lengthy greetings or conclusions.
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val part = JSONObject().apply {
                put("text", prompt)
            }
            val contentObj = JSONObject().apply {
                put("parts", JSONArray().apply { put(part) })
            }
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply { put(contentObj) })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Failed to generate report from Gemini (HTTP ${response.code})."
                }
                val responseString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val firstPart = parts.getJSONObject(0)
                firstPart.getString("text")
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to analyze ledger", e)
            "Error analyzing ledger data: ${e.localizedMessage ?: e.message}"
        }
    }
}
