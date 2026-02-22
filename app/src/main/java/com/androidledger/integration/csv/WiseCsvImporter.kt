package com.androidledger.integration.csv

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

class WiseCsvImporter @Inject constructor() {

    companion object {
        private const val FORMAT_NAME = "Wise"
        // Header: TransferWise ID,Date,Amount,Currency,Description,...
        private val DATE_FORMAT_ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val DATE_FORMAT_EU = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parse(csvContent: String): CsvImportResult {
        val lines = csvContent.lines()
        if (lines.isEmpty()) {
            return CsvImportResult(emptyList(), 0, 0, FORMAT_NAME)
        }

        // Parse header to find column indices
        val header = parseCsvLine(lines[0])
        val colMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        val idCol = colMap["TransferWise ID"] ?: -1
        val dateCol = colMap["Date"] ?: -1
        val amountCol = colMap["Amount"] ?: -1
        val currencyCol = colMap["Currency"] ?: -1
        val descriptionCol = colMap["Description"] ?: -1
        val merchantCol = colMap["Merchant"] ?: -1

        if (idCol == -1 || dateCol == -1 || amountCol == -1) {
            return CsvImportResult(emptyList(), 0, 1, FORMAT_NAME)
        }

        val transactions = mutableListOf<ParsedTransaction>()
        var skippedCount = 0
        var errorCount = 0

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            try {
                val fields = parseCsvLine(line)

                val transferWiseId = fields.getOrNull(idCol)?.trim() ?: ""
                val dateStr = fields.getOrNull(dateCol)?.trim() ?: ""
                val amountStr = fields.getOrNull(amountCol)?.trim() ?: ""
                val currency = fields.getOrNull(currencyCol)?.trim() ?: ""
                val description = fields.getOrNull(descriptionCol)?.trim() ?: ""
                val merchant = fields.getOrNull(merchantCol)?.trim() ?: ""

                if (transferWiseId.isEmpty() || dateStr.isEmpty()) {
                    skippedCount++
                    continue
                }

                // Parse date - try ISO format first, then European format
                val occurredAt = parseDate(dateStr)
                if (occurredAt == 0L) {
                    errorCount++
                    continue
                }

                // Parse amount
                val amountDouble = amountStr.toDoubleOrNull()
                if (amountDouble == null) {
                    errorCount++
                    continue
                }

                val amountMinor = (abs(amountDouble) * 100).roundToLong()
                val direction = if (amountDouble < 0) "OUT" else "IN"

                // Merchant: use Merchant field if available, else use Description
                val resolvedMerchant = merchant.ifEmpty { description }

                transactions.add(
                    ParsedTransaction(
                        occurredAt = occurredAt,
                        amountMinor = amountMinor,
                        currency = currency,
                        direction = direction,
                        merchant = resolvedMerchant,
                        description = description,
                        externalId = transferWiseId,
                        entrySource = "CSV"
                    )
                )
            } catch (e: Exception) {
                errorCount++
            }
        }

        return CsvImportResult(
            transactions = transactions,
            skippedCount = skippedCount,
            errorCount = errorCount,
            formatName = FORMAT_NAME
        )
    }

    private fun parseDate(dateStr: String): Long {
        // Try ISO format: yyyy-MM-dd
        try {
            val date = DATE_FORMAT_ISO.parse(dateStr)
            if (date != null) return date.time
        } catch (_: Exception) {}

        // Try European format: dd-MM-yyyy
        try {
            val date = DATE_FORMAT_EU.parse(dateStr)
            if (date != null) return date.time
        } catch (_: Exception) {}

        return 0L
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        fields.add(current.toString())
        return fields
    }
}
