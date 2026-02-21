package com.androidledger.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Tag
import com.androidledger.data.entity.Transaction
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.CategoryRepository
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.SettingsRepository
import com.androidledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class AddTransactionUiState(
    val currentTab: String = "EXPENSE",
    val amountText: String = "",
    val calculatedAmount: Long = 0L,
    val selectedCurrency: String = "EUR",
    val selectedSource: Source? = null,
    val selectedTransferTarget: Source? = null,
    val selectedCategory: Category? = null,
    val selectedTags: List<Tag> = emptyList(),
    val merchant: String = "",
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val showOptionalFields: Boolean = false,
    val selectedProfile: Profile? = null,
    val lastUsedSourceId: String? = null,
    val lastUsedCategoryId: String? = null,
    val exchangeRate: Double = 7.85,
    val transferRate: String = "7.85"
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()

    val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    val tags: StateFlow<List<Tag>> = settingsRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCategories("EXPENSE")
        loadExchangeRate()
    }

    private fun loadCategories(type: String) {
        viewModelScope.launch {
            categoryRepository.getByType(type).collect { list ->
                _categories.value = list
            }
        }
    }

    private fun loadExchangeRate() {
        viewModelScope.launch {
            settingsRepository.getRate("EUR", "CNY").collect { rate ->
                if (rate != null) {
                    _uiState.update {
                        it.copy(
                            exchangeRate = rate.rate,
                            transferRate = rate.rate.toString()
                        )
                    }
                }
            }
        }
    }

    fun onTabChange(tab: String) {
        val categoryType = when (tab) {
            "EXPENSE" -> "EXPENSE"
            "INCOME" -> "INCOME"
            else -> null
        }
        _uiState.update { it.copy(currentTab = tab, selectedCategory = null) }
        if (categoryType != null) {
            loadCategories(categoryType)
        }
    }

    fun onDigitPress(digit: String) {
        val current = _uiState.value.amountText
        // Prevent multiple dots in the current number segment
        if (digit == ".") {
            val lastSegment = current.split(Regex("[+\\-*/]")).lastOrNull() ?: ""
            if (lastSegment.contains(".")) return
            // Prevent leading dot when expression is empty
            if (current.isEmpty()) {
                _uiState.update {
                    it.copy(amountText = "0.")
                }
                recalculate()
                return
            }
        }
        // Prevent leading zeros (except "0.")
        if (digit == "0" && current == "0") return

        _uiState.update { it.copy(amountText = current + digit) }
        recalculate()
    }

    fun onOperatorPress(op: String) {
        val current = _uiState.value.amountText
        if (current.isEmpty()) return
        val lastChar = current.last()
        // Replace operator if the last char is already an operator
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

    private fun recalculate() {
        val amount = calculateExpression(_uiState.value.amountText)
        _uiState.update { it.copy(calculatedAmount = amount) }
    }

    /**
     * Evaluate a simple math expression containing +, -, *, /
     * Returns the result in cents (minor units).
     */
    internal fun calculateExpression(expression: String): Long {
        if (expression.isBlank()) return 0L
        // Remove trailing operator
        val cleaned = expression.trimEnd('+', '-', '*', '/')
        if (cleaned.isBlank()) return 0L

        return try {
            val result = evalExpression(cleaned)
            // Convert to cents: multiply by 100, round to nearest
            Math.round(result * 100)
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Simple recursive descent parser for expressions with +, -, *, /
     */
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

    fun onSourceSelect(source: Source) {
        _uiState.update {
            it.copy(
                selectedSource = source,
                lastUsedSourceId = source.id
            )
        }
        // Auto-detect currency from the wallet
        val wallet = allWallets.value.find { it.id == source.walletId }
        if (wallet != null) {
            _uiState.update { it.copy(selectedCurrency = wallet.currency) }
        }
    }

    fun onTransferTargetSelect(source: Source) {
        _uiState.update { it.copy(selectedTransferTarget = source) }
    }

    fun onCategorySelect(category: Category) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                lastUsedCategoryId = category.id
            )
        }
    }

    fun onTagToggle(tag: Tag) {
        val current = _uiState.value.selectedTags
        val updated = if (current.any { it.id == tag.id }) {
            current.filter { it.id != tag.id }
        } else {
            current + tag
        }
        _uiState.update { it.copy(selectedTags = updated) }
    }

    fun onMerchantChange(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onOccurredAtChange(millis: Long) {
        _uiState.update { it.copy(occurredAt = millis) }
    }

    fun onToggleOptionalFields() {
        _uiState.update { it.copy(showOptionalFields = !it.showOptionalFields) }
    }

    fun onCurrencyToggle() {
        _uiState.update {
            it.copy(
                selectedCurrency = if (it.selectedCurrency == "EUR") "CNY" else "EUR"
            )
        }
    }

    fun onTransferRateChange(rate: String) {
        _uiState.update { it.copy(transferRate = rate) }
    }

    fun onProfileSelect(profile: Profile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    fun onSave() {
        val state = _uiState.value
        val amount = if (state.calculatedAmount > 0L) state.calculatedAmount
        else calculateExpression(state.amountText)

        if (amount <= 0L) return

        val source = state.selectedSource ?: return
        val wallet = allWallets.value.find { it.id == source.walletId } ?: return
        val profile = profiles.value.find { it.id == wallet.profileId } ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tagIdsStr = if (state.selectedTags.isNotEmpty()) {
                state.selectedTags.joinToString(",") { it.id }
            } else null

            if (state.currentTab == "TRANSFER") {
                saveTransfer(state, source, wallet, profile, amount, now, tagIdsStr)
            } else {
                val direction = if (state.currentTab == "EXPENSE") "OUT" else "IN"
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    profileId = profile.id,
                    walletId = wallet.id,
                    sourceId = source.id,
                    occurredAt = state.occurredAt,
                    amountMinor = amount,
                    currency = state.selectedCurrency,
                    direction = direction,
                    merchant = state.merchant.ifBlank { null },
                    description = state.description.ifBlank { null },
                    categoryId = state.selectedCategory?.id,
                    tagIds = tagIdsStr,
                    source = "MANUAL",
                    isConfirmed = 1,
                    createdAt = now
                )
                transactionRepository.addTransaction(transaction)
            }

            // Emit success
            _saveSuccess.emit(true)

            // Reset form but keep last used source and category
            _uiState.update {
                it.copy(
                    amountText = "",
                    calculatedAmount = 0L,
                    selectedTransferTarget = null,
                    selectedTags = emptyList(),
                    merchant = "",
                    description = "",
                    occurredAt = System.currentTimeMillis(),
                    showOptionalFields = false,
                    transferRate = it.exchangeRate.toString()
                )
            }
        }
    }

    private suspend fun saveTransfer(
        state: AddTransactionUiState,
        fromSource: Source,
        fromWallet: Wallet,
        profile: Profile,
        amount: Long,
        now: Long,
        tagIdsStr: String?
    ) {
        val toSource = state.selectedTransferTarget ?: return
        val toWallet = allWallets.value.find { it.id == toSource.walletId } ?: return

        val transferGroupId = UUID.randomUUID().toString()

        // Determine target amount: if cross-currency, apply transfer rate
        val fromCurrency = fromWallet.currency
        val toCurrency = toWallet.currency
        val targetAmount = if (fromCurrency != toCurrency) {
            val rate = state.transferRate.toDoubleOrNull() ?: state.exchangeRate
            if (fromCurrency == "EUR" && toCurrency == "CNY") {
                // amount is in EUR cents; multiply by rate to get CNY cents
                Math.round(amount * rate)
            } else if (fromCurrency == "CNY" && toCurrency == "EUR") {
                // amount is in CNY cents; divide by rate to get EUR cents
                if (rate != 0.0) Math.round(amount / rate) else amount
            } else {
                amount
            }
        } else {
            amount
        }

        // OUT record (from source)
        val outTransaction = Transaction(
            id = UUID.randomUUID().toString(),
            profileId = profile.id,
            walletId = fromWallet.id,
            sourceId = fromSource.id,
            occurredAt = state.occurredAt,
            amountMinor = amount,
            currency = fromCurrency,
            direction = "TRANSFER",
            merchant = state.merchant.ifBlank { null },
            description = state.description.ifBlank { null },
            categoryId = state.selectedCategory?.id,
            tagIds = tagIdsStr,
            source = "MANUAL",
            isConfirmed = 1,
            transferGroupId = transferGroupId,
            createdAt = now
        )

        // IN record (to source)
        // The target wallet may belong to a different profile
        val toProfileId = allWallets.value
            .find { it.id == toSource.walletId }?.profileId ?: profile.id
        val inTransaction = Transaction(
            id = UUID.randomUUID().toString(),
            profileId = toProfileId,
            walletId = toWallet.id,
            sourceId = toSource.id,
            occurredAt = state.occurredAt,
            amountMinor = targetAmount,
            currency = toCurrency,
            direction = "TRANSFER",
            merchant = state.merchant.ifBlank { null },
            description = state.description.ifBlank { null },
            categoryId = state.selectedCategory?.id,
            tagIds = tagIdsStr,
            source = "MANUAL",
            isConfirmed = 1,
            transferGroupId = transferGroupId,
            createdAt = now
        )

        transactionRepository.addTransaction(outTransaction)
        transactionRepository.addTransaction(inTransaction)
    }
}
