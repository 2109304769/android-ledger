package com.androidledger.integration.csv

enum class CsvFormat(val displayName: String) {
    REVOLUT("Revolut"),
    WISE("Wise"),
    POSTE("Poste Italiane"),
    UNKNOWN("未知格式")
}

object CsvFormatDetector {

    /**
     * Detects the CSV format by examining the first (header) line.
     */
    fun detect(headerLine: String): CsvFormat {
        val trimmed = headerLine.trim().removeSuffix("\r")

        return when {
            trimmed.startsWith("Type,Product,Started Date") -> CsvFormat.REVOLUT
            trimmed.startsWith("TransferWise ID,Date,Amount") -> CsvFormat.WISE
            trimmed.contains("Data Operazione") && trimmed.contains("Data Valuta") -> CsvFormat.POSTE
            else -> CsvFormat.UNKNOWN
        }
    }
}
