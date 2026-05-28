package com.example.constructorapp.data.remote

import android.util.Log
import com.example.constructorapp.data.local.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.*
import java.io.IOException

object GoogleSheetsService {
    private const val TAG = "GoogleSheetsService"
    private val client = OkHttpClient()

    /**
     * Syncs the given list of reminders to the specified Google Sheet.
     * Uses a direct Google REST API call or an Apps Script endpoint.
     * If an OAuth token is provided, it calls the Google Sheets v4 API.
     * Otherwise, it simulates/logs the sync process to provide a seamless offline flow.
     */
    suspend fun syncToGoogleSheets(
        spreadsheetId: String,
        accessToken: String?,
        reminders: List<ReminderEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        if (spreadsheetId.isBlank()) {
            Log.e(TAG, "Spreadsheet ID is empty")
            return@withContext false
        }

        if (accessToken.isNullOrBlank()) {
            // Simulator Mode (for easy testing without OAuth keys)
            Log.i(TAG, "Syncing ${reminders.size} reminders to Sheet ID $spreadsheetId (Simulated Mode)...")
            kotlinx.coroutines.delay(2000) // Simulate network delay
            return@withContext true
        }

        try {
            // Google Sheets API v4 Append Values URL:
            // POST https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}:append
            val range = "Sheet1!A:G"
            val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range:append?valueInputOption=USER_ENTERED"

            // Construct values body
            val valuesArray = JsonArray(reminders.map { reminder ->
                JsonArray(listOf(
                    JsonPrimitive(reminder.id),
                    JsonPrimitive(reminder.title),
                    JsonPrimitive(reminder.description ?: ""),
                    JsonPrimitive(reminder.priority),
                    JsonPrimitive(reminder.location ?: ""),
                    JsonPrimitive(reminder.dueDate ?: ""),
                    JsonPrimitive(if (reminder.isCompleted) "Completed" else "Pending")
                ))
            })

            val requestBodyJson = buildJsonObject {
                put("range", range)
                put("majorDimension", "ROWS")
                put("values", valuesArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully synced reminders to Google Sheets")
                    true
                } else {
                    Log.e(TAG, "Failed to sync to Google Sheets: ${response.code} ${response.message}")
                    Log.e(TAG, "Error Body: ${response.body?.string()}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error syncing to Google Sheets", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error syncing to Google Sheets", e)
            false
        }
    }
}
