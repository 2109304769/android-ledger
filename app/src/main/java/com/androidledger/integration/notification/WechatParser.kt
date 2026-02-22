package com.androidledger.integration.notification

object WechatParser {

    private val AMOUNT_PATTERN = Regex("""[¥￥](\d+(?:\.\d{1,2})?)""")

    /**
     * Parse WeChat payment notification text.
     *
     * Known patterns:
     * - "微信支付\n支付成功\n¥35.00"        -> OUT, 3500
     * - "微信支付\n你收到了¥100.00"          -> IN, 10000
     * - notifications containing "付款"      -> OUT
     * - notifications containing "收款"/"转账" -> IN
     */
    fun parse(text: String): NotificationParseResult? {
        if (!text.contains("微信支付") && !text.contains("微信")) return null

        val amountMatch = AMOUNT_PATTERN.find(text) ?: return null
        val amountStr = amountMatch.groupValues[1]
        val amountMinor = parseAmountToMinor(amountStr) ?: return null

        val direction = inferDirection(text)

        // Try to extract merchant name: look for text between known keywords
        val merchant = extractMerchant(text)

        return NotificationParseResult(
            amountMinor = amountMinor,
            direction = direction,
            currency = "CNY",
            merchant = merchant,
            isConfirmed = true
        )
    }

    private fun parseAmountToMinor(amountStr: String): Long? {
        return try {
            val parts = amountStr.split(".")
            val intPart = parts[0].toLong()
            val decPart = if (parts.size > 1) {
                val d = parts[1]
                when (d.length) {
                    1 -> d.toLong() * 10
                    2 -> d.toLong()
                    else -> return null
                }
            } else {
                0L
            }
            intPart * 100 + decPart
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun inferDirection(text: String): String {
        // Explicit receive keywords
        val inKeywords = listOf("你收到了", "收款", "转账给你", "收到转账", "收到红包")
        for (kw in inKeywords) {
            if (text.contains(kw)) return "IN"
        }

        // Explicit payment keywords
        val outKeywords = listOf("支付成功", "付款", "已支付", "消费", "扣款")
        for (kw in outKeywords) {
            if (text.contains(kw)) return "OUT"
        }

        // Default to OUT for WeChat pay notifications
        return "OUT"
    }

    private fun extractMerchant(text: String): String? {
        // Try common patterns like "向XXX付款" or "在XXX消费"
        val merchantPatterns = listOf(
            Regex("""向(.+?)付款"""),
            Regex("""在(.+?)消费"""),
            Regex("""收到(.+?)的"""),
            Regex("""付款给(.+?)[\s\n]""")
        )
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty() && name.length <= 50) return name
            }
        }
        return null
    }
}
