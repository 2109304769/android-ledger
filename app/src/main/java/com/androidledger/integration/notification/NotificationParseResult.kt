package com.androidledger.integration.notification

data class NotificationParseResult(
    val amountMinor: Long,
    val direction: String, // "IN" or "OUT"
    val currency: String, // "CNY"
    val merchant: String?, // extracted merchant name if available
    val isConfirmed: Boolean // true if parse was confident
)
