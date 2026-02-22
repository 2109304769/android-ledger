package com.androidledger.ui.quickentry

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Transaction
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.CategoryRepository
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuickEntryUiState(
    val amountText: String = "",
    val calculatedAmount: Long = 0L,
    val selectedCurrency: String = "EUR",
    val selectedSource: Source? = null,
    val topSources: List<Source> = emptyList(),
    val resultMessage: String? = null,
    val canUndo: Boolean = false,
    val isQuickEntryEnabled: Boolean = false
)

@HiltViewModel
class QuickEntryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "quick_entry_prefs"
        const val KEY_ENABLED = "quick_entry_enabled"
        const val KEY_DEFAULT_EXPENSE_SOURCE_ID = "default_expense_source_id"
        const val KEY_DEFAULT_INCOME_SOURCE_ID = "default_income_source_id"
        const val KEY_DEFAULT_EXPENSE_CATEGORY_ID = "default_expense_category_id"
        const val KEY_DEFAULT_INCOME_CATEGORY_ID = "default_income_category_id"
        const val KEY_DEFAULT_CURRENCY = "default_currency"
        const val KEY_DEFAULT_PROFILE_ID = "default_profile_id"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(QuickEntryUiState())
    val uiState: StateFlow<QuickEntryUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()

    val allSources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastInsertedTransaction: Transaction? = null
    private var undoTimerJob: Job? = null

    init {
        loadSettings()
        loadTopSources()
    }

    private fun loadSettings() {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val currency = prefs.getString(KEY_DEFAULT_CURRENCY, "EUR") ?: "EUR"
        _uiState.update {
            it.copy(
                selectedCurrency = currency,
                isQuickEntryEnabled = enabled
            )
        }
    }

    private fun loadTopSources() {
        viewModelScope.launch {
            profileRepository.getAllSources().collect { sources ->
                val activeSources = sources.filter { it.isArchived == 0 }.take(5)
                _uiState.update { it.copy(topSources = activeSources) }

                // Auto-select default expense source if configured
                val defaultSourceId = prefs.getString(KEY_DEFAULT_EXPENSE_SOURCE_ID, null)
                if (_uiState.value.selectedSource == null && defaultSourceId != null) {
                    val defaultSource = activeSources.find { it.id == defaultSourceId }
                    if (defaultSource != null) {
                        _uiState.update { it.copy(selectedSource = defaultSource) }
                    }
                }

                // If no default set, select first source
                if (_uiState.value.selectedSource == null && activeSources.isNotEmpty()) {
                    _uiState.update { it.copy(selectedSource = activeSources.first()) }
                }
            }
        }
    }

    fun onDigitPress(digit: String) {
        val current = _uiState.value.amountText
        if (digit == ".") {
            val lastSegment = current.split(Regex("[+\\-*/]")).lastOrNull() ?: ""
            if (lastSegment.contains(".")) return
            if (current.isEmpty()) {
                _uiState.update { it.copy(amountText = "0.") }
                recalculate()
                return
            }
        }
        if (digit == "0" && current == "0") return

        _uiState.update { it.copy(amountText = current + digit) }
        recalculate()
    }

    fun onOperatorPress(op: String) {
        val current = _uiState.value.amountText
        if (current.isEmpty()) return
        val lastChar = current.last()
        if (lastChar in "+-*/") {
            _uiState.update { it.copy(amountText = current.dropLast(1) + op) }
        } else {
            _uiState.update { it.copy(amountText = current + op) }
        }
    }

    fun onBackspace() {
        val current = _uiState.value.amountText
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(amountText = current.dropLast(1)) }
            recalculate()
        }
    }

    fun onClear() {
        _uiState.update { it.copy(amountText = "", calculatedAmount = 0L) }
    }

    fun onCurrencyToggle() {
        _uiState.update {
            it.copy(selectedCurrency = if (it.selectedCurrency == "EUR") "CNY" else "EUR")
        }
    }

    fun onSourceSelect(source: Source) {
        _uiState.update { it.copy(selectedSource = source) }
        // Auto-detect currency from the wallet
        val wallet = allWallets.value.find { it.id == source.walletId }
        if (wallet != null) {
            _uiState.update { it.copy(selectedCurrency = wallet.currency) }
        }
    }

    private fun recalculate() {
        val amount = calculateExpression(_uiState.value.amountText)
        _uiState.update { it.copy(calculatedAmount = amount) }
    }

    internal fun calculateExpression(expression: String): Long {
        if (expression.isBlank()) return 0L
        val cleaned = expression.trimEnd('+', '-', '*', '/')
        if (cleaned.isBlank()) return 0L

        return try {
            val result = evalExpression(cleaned)
            Math.round(result * 100)
        } catch (_: Exception) {
            0L
        }
    }

    private fun evalExpression(expr: String): Double {
        val tokens = tokenize(expr)
        val parser = ExpressionParser(tokens)
        return parser.parseExpression()
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (ch in expr) {
            if (ch in "+-*/") {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                tokens.add(ch.toString())
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    private class ExpressionParser(private val tokens: List<String>) {
        private var pos = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos++]
                val term = parseTerm()
                result = if (op == "+") result + term else result - term
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < tokens.size && (tokens[pos] == "*" || tokens[pos] == "/")) {
                val op = tokens[pos++]
                val factor = parseFactor()
                result = if (op == "*") result * factor else {
                    if (factor != 0.0) result / factor else 0.0
                }
            }
            return result
        }

        private fun parseFactor(): Double {
            if (pos >= tokens.size) return 0.0
            return tokens[pos++].toDoubleOrNull() ?: 0.0
        }
    }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = if (state.calculatedAmount > 0L) state.calculatedAmount
        else calculateExpression(state.amountText)

        if (amount <= 0L) return

        val source = state.selectedSource ?: return
        val wallet = allWallets.value.find { it.id == source.walletId } ?: return

        // Get default category
        val defaultCategoryId = prefs.getString(KEY_DEFAULT_EXPENSE_CATEGORY_ID, null)
        // Get default profile - use wallet's profile
        val profileId = wallet.profileId

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                walletId = wallet.id,
                sourceId = source.id,
                occurredAt = now,
                amountMinor = amount,
                currency = state.selectedCurrency,
                direction = "OUT",
                merchant = null,
                description = null,
                categoryId = defaultCategoryId,
                tagIds = null,
                source = "QUICK_ENTRY",
                isConfirmed = 1,
                createdAt = now
            )

            transactionRepository.addTransaction(transaction)
            lastInsertedTransaction = transaction

            // Format success message
            val currencySymbol = if (state.selectedCurrency == "EUR") "\u20AC" else "\u00A5"
            val amountStr = formatAmount(amount)
            val sourceName = source.name

            // Try to get category name
            var categoryName = "..."
            if (defaultCategoryId != null) {
                categoryRepository.getById(defaultCategoryId).collect { cat ->
                    if (cat != null) {
                        categoryName = cat.name
                    }
                }
            }

            val message = "\u2713 \u5DF2\u8BB0 $currencySymbol$amountStr \u00B7 $sourceName \u00B7 $categoryName"
            _uiState.update {
                it.copy(
                    resultMessage = message,
                    canUndo = true,
                    amountText = "",
                    calculatedAmount = 0L
                )
            }

            _saveSuccess.emit(true)

            // Start undo timer
            startUndoTimer()
        }
    }

    private fun startUndoTimer() {
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            delay(2000)
            _uiState.update {
                it.copy(canUndo = false, resultMessage = null)
            }
            lastInsertedTransaction = null
        }
    }

    fun undoLastTransaction() {
        val transaction = lastInsertedTransaction ?: return
        undoTimerJob?.cancel()

        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            lastInsertedTransaction = null
            _uiState.update {
                it.copy(
                    resultMessage = null,
                    canUndo = false
                )
            }
        }
    }

    private fun formatAmount(amountMinor: Long): String {
        val whole = amountMinor / 100
        val fraction = amountMinor % 100
        return if (fraction == 0L) {
            "$whole.00"
        } else {
            String.format("%d.%02d", whole, fraction)
        }
    }

    fun isQuickEntryEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false)
    }
}
