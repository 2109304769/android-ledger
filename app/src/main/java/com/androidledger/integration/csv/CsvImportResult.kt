package com.androidledger.integration.csv

data class CsvImportResult(
    val transactions: List<ParsedTransaction>,
    val skippedCount: Int,
    val errorCount: Int,
    val formatName: String
)

data class ParsedTransaction(
    val occurredAt: Long,
    val amountMinor: Long,
    val currency: String,
    val direction: String, // "IN" or "OUT"
    val merchant: String,
    val description: String,
    val externalId: String,
    val entrySource: String = "CSV"
)
