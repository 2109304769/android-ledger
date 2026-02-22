package com.androidledger.integration.csv

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

class RevolutCsvImporter @Inject constructor() {

    companion object {
        private const val FORMAT_NAME = "Revolut"
        // Header: Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parse(csvContent: String): CsvImportResult {
        val lines = csvContent.lines()
        if (lines.isEmpty()) {
            return CsvImportResult(emptyList(), 0, 0, FORMAT_NAME)
        }

        val transactions = mutableListOf<ParsedTransaction>()
        var skippedCount = 0
        var errorCount = 0

        // Skip header line (index 0)
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            try {
                val fields = parseCsvLine(line)
                if (fields.size < 10) {
                    errorCount++
                    continue
                }

                val state = fields[8].trim()
                if (state != "COMPLETED") {
                    skippedCount++
                    continue
                }

                val startedDate = fields[2].trim()
                val amountStr = fields[5].trim()
                val description = fields[4].trim()
                val currency = fields[7].trim()

                // Parse date
                val occurredAt = try {
                    DATE_FORMAT.parse(startedDate)?.time ?: 0L
                } catch (e: Exception) {
                    errorCount++
                    continue
                }

                // Parse amount (negative = OUT, positive = IN)
                val amountDouble = amountStr.toDoubleOrNull()
                if (amountDouble == null) {
                    errorCount++
                    continue
                }

                val amountMinor = (abs(amountDouble) * 100).roundToLong()
                val direction = if (amountDouble < 0) "OUT" else "IN"

                // Generate externalId = SHA-256 hash of (Started Date + Amount + Description + Currency)
                val hashInput = "$startedDate$amountStr$description$currency"
                val externalId = sha256(hashInput)

                transactions.add(
                    ParsedTransaction(
                        occurredAt = occurredAt,
                        amountMinor = amountMinor,
                        currency = currency,
                        direction = direction,
                        merchant = description,
                        description = description,
                        externalId = externalId,
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

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
