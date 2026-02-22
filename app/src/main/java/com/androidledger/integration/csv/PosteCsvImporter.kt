package com.androidledger.integration.csv

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

class PosteCsvImporter @Inject constructor() {

    companion object {
        private const val FORMAT_NAME = "Poste Italiane"
        // Header: Data Operazione,Data Valuta,Accrediti,Addebiti,Descrizione Operazioni
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).apply {
            timeZone = TimeZone.getTimeZone("Europe/Rome")
        }
    }

    /**
     * Parse CSV content. The content may be in ISO-8859-1 encoding;
     * the caller should handle encoding conversion before passing the string.
     */
    fun parse(csvContent: String): CsvImportResult {
        val lines = csvContent.lines()
        if (lines.isEmpty()) {
            return CsvImportResult(emptyList(), 0, 0, FORMAT_NAME)
        }

        // Parse header to find column indices
        val header = parseCsvLine(lines[0])
        val colMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        val dataOpCol = colMap["Data Operazione"] ?: -1
        val accreditiCol = colMap["Accrediti"] ?: -1
        val addebitiCol = colMap["Addebiti"] ?: -1
        val descrizioneCol = colMap["Descrizione Operazioni"] ?: -1

        if (dataOpCol == -1 || descrizioneCol == -1) {
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

                val dataOperazione = fields.getOrNull(dataOpCol)?.trim() ?: ""
                val accreditiStr = fields.getOrNull(accreditiCol)?.trim() ?: ""
                val addebitiStr = fields.getOrNull(addebitiCol)?.trim() ?: ""
                val descrizione = fields.getOrNull(descrizioneCol)?.trim() ?: ""

                if (dataOperazione.isEmpty()) {
                    skippedCount++
                    continue
                }

                // Parse date (European format: dd/MM/yyyy)
                val occurredAt = try {
                    DATE_FORMAT.parse(dataOperazione)?.time ?: 0L
                } catch (e: Exception) {
                    errorCount++
                    continue
                }
                if (occurredAt == 0L) {
                    errorCount++
                    continue
                }

                // Determine amount and direction
                // Accrediti (credits) = positive = IN
                // Addebiti (debits) = negative = OUT
                val accrediti = parseItalianAmount(accreditiStr)
                val addebiti = parseItalianAmount(addebitiStr)

                val amountMinor: Long
                val direction: String

                when {
                    accrediti != null && accrediti > 0 -> {
                        amountMinor = (accrediti * 100).roundToLong()
                        direction = "IN"
                    }
                    addebiti != null -> {
                        amountMinor = (abs(addebiti) * 100).roundToLong()
                        direction = "OUT"
                    }
                    else -> {
                        errorCount++
                        continue
                    }
                }

                // Normalize description
                val normalized = PosteItalianeNormalizer.normalize(descrizione)
                val resolvedDirection = normalized.overrideDirection ?: direction
                val merchant = normalized.merchant
                val description = normalized.description

                // Generate externalId = SHA-256 hash of (Data Operazione + amount + first 30 chars of description)
                val amountForHash = if (resolvedDirection == "OUT") "-$amountMinor" else "$amountMinor"
                val descFirst30 = descrizione.take(30)
                val hashInput = "$dataOperazione$amountForHash$descFirst30"
                val externalId = sha256(hashInput)

                transactions.add(
                    ParsedTransaction(
                        occurredAt = occurredAt,
                        amountMinor = amountMinor,
                        currency = "EUR",
                        direction = resolvedDirection,
                        merchant = merchant,
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

    /**
     * Parses an Italian-format amount string (e.g., "1.234,56" or "1234,56" or "1234.56").
     * Returns null if the string is blank or unparseable.
     */
    private fun parseItalianAmount(amountStr: String): Double? {
        if (amountStr.isBlank()) return null
        // Italian format: periods as thousands separator, comma as decimal separator
        // Convert to standard format
        val normalized = amountStr
            .replace(".", "")   // Remove thousands separators
            .replace(",", ".")  // Convert decimal comma to dot
        return normalized.toDoubleOrNull()
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        // Poste CSV may use semicolons as delimiters
        val delimiter = if (line.contains(';')) ';' else ','

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
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

/**
 * Normalizes Poste Italiane transaction descriptions to extract
 * merchant names and category hints.
 */
object PosteItalianeNormalizer {

    data class NormalizedResult(
        val merchant: String,
        val description: String,
        val categoryHint: String? = null,
        val overrideDirection: String? = null
    )

    fun normalize(rawDescription: String): NormalizedResult {
        val desc = rawDescription.trim()
        val descUpper = desc.uppercase(Locale.ITALIAN)

        return when {
            // "PAGAMENTO POS DEL xx/xx PRESSO " -> remove prefix, keep merchant name
            descUpper.contains("PAGAMENTO POS") && descUpper.contains("PRESSO") -> {
                val pressoIndex = descUpper.indexOf("PRESSO")
                val merchant = desc.substring(pressoIndex + "PRESSO".length).trim()
                NormalizedResult(
                    merchant = merchant.ifEmpty { desc },
                    description = desc
                )
            }

            // "BONIFICO A FAVORE DI " -> direction=OUT, merchant=name after prefix
            descUpper.startsWith("BONIFICO A FAVORE DI") -> {
                val merchant = desc.substring("BONIFICO A FAVORE DI".length).trim()
                NormalizedResult(
                    merchant = merchant.ifEmpty { desc },
                    description = desc,
                    overrideDirection = "OUT"
                )
            }

            // "BONIFICO DA " -> direction=IN, merchant=name after prefix
            descUpper.startsWith("BONIFICO DA") -> {
                val merchant = desc.substring("BONIFICO DA".length).trim()
                NormalizedResult(
                    merchant = merchant.ifEmpty { desc },
                    description = desc,
                    overrideDirection = "IN"
                )
            }

            // "RICARICA POSTEPAY " -> category hint: transfer
            descUpper.startsWith("RICARICA POSTEPAY") -> {
                val merchant = desc.substring("RICARICA POSTEPAY".length).trim()
                NormalizedResult(
                    merchant = merchant.ifEmpty { "Ricarica PostePay" },
                    description = desc,
                    categoryHint = "transfer"
                )
            }

            // "PAGAMENTO F24 " -> category hint: tax
            descUpper.startsWith("PAGAMENTO F24") -> {
                NormalizedResult(
                    merchant = "F24",
                    description = desc,
                    categoryHint = "tax"
                )
            }

            // "PRELIEVO BANCOMAT " -> category hint: cash withdrawal
            descUpper.startsWith("PRELIEVO BANCOMAT") -> {
                val location = desc.substring("PRELIEVO BANCOMAT".length).trim()
                NormalizedResult(
                    merchant = location.ifEmpty { "Prelievo Bancomat" },
                    description = desc,
                    categoryHint = "cash_withdrawal"
                )
            }

            // "CANONE " -> category hint: bank fee
            descUpper.startsWith("CANONE") -> {
                NormalizedResult(
                    merchant = desc,
                    description = desc,
                    categoryHint = "bank_fee"
                )
            }

            else -> NormalizedResult(
                merchant = desc,
                description = desc
            )
        }
    }
}
