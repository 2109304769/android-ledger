package com.androidledger.integration.notification

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.androidledger.data.dao.SourceDao
import com.androidledger.data.dao.WalletDao
import com.androidledger.data.entity.Transaction
import com.androidledger.data.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class LedgerNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "LedgerNotifListener"
        private const val PREF_NAME = "notification_listener_prefs"
        private const val KEY_ENABLED = "notification_listening_enabled"

        private const val PKG_WECHAT = "com.tencent.mm"
        private const val PKG_ALIPAY_DOMESTIC = "com.eg.android.AlipayGphone"
        private const val PKG_ALIPAY_INTERNATIONAL = "com.alipay.android.app"

        private val WATCHED_PACKAGES = setOf(
            PKG_WECHAT,
            PKG_ALIPAY_DOMESTIC,
            PKG_ALIPAY_INTERNATIONAL
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationListenerEntryPoint {
        fun transactionRepository(): TransactionRepository
        fun sourceDao(): SourceDao
        fun walletDao(): WalletDao
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val entryPoint: NotificationListenerEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationListenerEntryPoint::class.java
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName !in WATCHED_PACKAGES) return

        // Check if notification listening is enabled in SharedPreferences
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        if (!isEnabled) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract notification text
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        // Combine all text for parsing
        val fullText = buildString {
            if (title.isNotEmpty()) append(title)
            if (text.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(text)
            }
            if (bigText.isNotEmpty() && bigText != text) {
                if (isNotEmpty()) append("\n")
                append(bigText)
            }
        }

        if (fullText.isBlank()) return

        Log.d(TAG, "Notification from $packageName: $fullText")

        serviceScope.launch {
            processNotification(packageName, fullText, sbn.postTime)
        }
    }

    private suspend fun processNotification(packageName: String, text: String, postTime: Long) {
        try {
            val transactionRepo = entryPoint.transactionRepository()
            val sourceDao = entryPoint.sourceDao()
            val walletDao = entryPoint.walletDao()

            // Try to parse the notification
            val parseResult: NotificationParseResult? = when (packageName) {
                PKG_WECHAT -> WechatParser.parse(text)
                PKG_ALIPAY_DOMESTIC, PKG_ALIPAY_INTERNATIONAL -> AlipayParser.parse(text)
                else -> null
            }

            // Find a default source for CNY transactions
            val source = sourceDao.getFirstSourceByCurrency("CNY")
            if (source == null) {
                Log.w(TAG, "No CNY source found, cannot create transaction")
                return
            }

            // Get the wallet to find profileId
            val wallet = walletDao.getByIdSync(source.walletId)
            if (wallet == null) {
                Log.w(TAG, "Wallet not found for source ${source.id}")
                return
            }

            val now = System.currentTimeMillis()

            // Generate externalId for dedup: SHA-256 of (timestamp rounded to minute + amount + package + first 50 chars)
            val timestampMinute = (postTime / 60000) * 60000
            val amount = parseResult?.amountMinor ?: 0L
            val textPrefix = text.take(50)
            val dedupInput = "$timestampMinute|$amount|$packageName|$textPrefix"
            val externalId = sha256(dedupInput)

            val entrySource = when (packageName) {
                PKG_WECHAT -> "NOTIFICATION_WECHAT"
                PKG_ALIPAY_DOMESTIC, PKG_ALIPAY_INTERNATIONAL -> "NOTIFICATION_ALIPAY"
                else -> "NOTIFICATION_UNKNOWN"
            }

            if (parseResult != null) {
                // Parse succeeded: create confirmed transaction
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    profileId = wallet.profileId,
                    walletId = wallet.id,
                    sourceId = source.id,
                    occurredAt = postTime,
                    amountMinor = parseResult.amountMinor,
                    currency = parseResult.currency,
                    direction = parseResult.direction,
                    merchant = parseResult.merchant,
                    description = text.take(200),
                    source = entrySource,
                    externalId = externalId,
                    isConfirmed = 1,
                    createdAt = now
                )
                transactionRepo.addTransactionOrIgnore(transaction)
                Log.d(TAG, "Created confirmed transaction: ${parseResult.direction} ${parseResult.amountMinor}")
            } else {
                // Parse failed: create unconfirmed transaction with amount=0
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    profileId = wallet.profileId,
                    walletId = wallet.id,
                    sourceId = source.id,
                    occurredAt = postTime,
                    amountMinor = 0,
                    currency = "CNY",
                    direction = "OUT",
                    merchant = null,
                    description = text.take(200),
                    source = entrySource,
                    externalId = externalId,
                    isConfirmed = 0,
                    createdAt = now
                )
                transactionRepo.addTransactionOrIgnore(transaction)
                Log.d(TAG, "Created unconfirmed transaction from unparseable notification")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
