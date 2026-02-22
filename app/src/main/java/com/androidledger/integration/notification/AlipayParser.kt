package com.androidledger.integration.notification

object AlipayParser {

    private val AMOUNT_PATTERN = Regex("""[¥￥](\d+(?:\.\d{1,2})?)""")

    /**
     * Parse Alipay payment notification text.
     *
     * Known patterns:
     * - "支付宝支付成功¥25.50"  -> OUT, 2550
     * - "成功收款¥100.00"       -> IN, 10000
     * - "付款成功¥XX.XX"        -> OUT
     * - "转账收款¥XX.XX"        -> IN
     */
    fun parse(text: String): NotificationParseResult? {
        if (!text.contains("支付宝") && !text.contains("Alipay")) return null

        val amountMatch = AMOUNT_PATTERN.find(text) ?: return null
        val amountStr = amountMatch.groupValues[1]
        val amountMinor = parseAmountToMinor(amountStr) ?: return null

        val direction = inferDirection(text)

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
        val inKeywords = listOf("成功收款", "收款成功", "转账收款", "收到转账", "收到红包", "收到付款")
        for (kw in inKeywords) {
            if (text.contains(kw)) return "IN"
        }

        val outKeywords = listOf("支付成功", "付款成功", "已支付", "消费", "扣款", "交易成功")
        for (kw in outKeywords) {
            if (text.contains(kw)) return "OUT"
        }

        return "OUT"
    }

    private fun extractMerchant(text: String): String? {
        val merchantPatterns = listOf(
            Regex("""向(.+?)付款"""),
            Regex("""在(.+?)消费"""),
            Regex("""收到(.+?)的"""),
            Regex("""付款给(.+?)[\s\n]"""),
            Regex("""(.+?)成功收款""")
        )
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                // Filter out common non-merchant prefixes
                if (name.isNotEmpty() && name.length <= 50
                    && !name.contains("支付宝") && !name.contains("Alipay")
                ) {
                    return name
                }
            }
        }
        return null
    }
}
