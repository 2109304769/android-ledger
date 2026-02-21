package com.androidledger.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Transaction
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.CategoryRepository
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class TransactionDisplayItem(
    val transactionId: String,
    val amount: String,
    val merchant: String,
    val categoryEmoji: String,
    val categoryName: String,
    val sourceIcon: String,
    val sourceName: String,
    val time: String,
    val direction: String,
    val profileName: String,
    val currency: String
)

data class CategoryExpenseItem(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val amount: Long,
    val percentage: Float,
    val color: String
)

data class TransactionGroup(
    val dateLabel: String,
    val dailyTotal: Long,
    val currency: String,
    val transactions: List<TransactionDisplayItem>
)

data class WalletDisplayItem(
    val walletId: String,
    val walletName: String,
    val currency: String,
    val totalBalance: Long,
    val profileName: String,
    val sources: List<SourceDisplayItem>
)

data class SourceDisplayItem(
    val sourceId: String,
    val name: String,
    val icon: String,
    val balance: Long,
    val currency: String,
    val walletName: String,
    val profileName: String
)

data class DashboardUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: String? = null,
    val monthLabel: String = "",
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyNet: Long = 0L,
    val primaryCurrency: String = "EUR",
    val walletItems: List<WalletDisplayItem> = emptyList(),
    val sourceItems: List<SourceDisplayItem> = emptyList(),
    val categoryExpenses: List<CategoryExpenseItem> = emptyList(),
    val transactionGroups: List<TransactionGroup> = emptyList(),
    val unconfirmedCount: Int = 0,
    val balanceHidden: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    private val _balanceHidden = MutableStateFlow(false)
    val balanceHidden: StateFlow<Boolean> = _balanceHidden.asStateFlow()

    private val currentMonthRange: Pair<Long, Long>
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val end = cal.timeInMillis
            return start to end
        }

    private val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allWallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val unconfirmedCount: StateFlow<Int> = transactionRepository.getUnconfirmedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val monthlyExpenseFlow = _selectedProfileId.flatMapLatest { profileId ->
        val (start, end) = currentMonthRange
        if (profileId == null) {
            transactionRepository.getAllMonthlyExpense(start, end)
        } else {
            transactionRepository.getMonthlyExpense(profileId, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val monthlyIncomeFlow = _selectedProfileId.flatMapLatest { profileId ->
        val (start, end) = currentMonthRange
        if (profileId == null) {
            transactionRepository.getAllMonthlyIncome(start, end)
        } else {
            transactionRepository.getMonthlyIncome(profileId, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val categoryExpensesFlow = _selectedProfileId.flatMapLatest { profileId ->
        val (start, end) = currentMonthRange
        if (profileId == null) {
            transactionRepository.getAllCategoryExpenses(start, end)
        } else {
            transactionRepository.getCategoryExpenses(profileId, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recentTransactionsFlow = _selectedProfileId.flatMapLatest { profileId ->
        if (profileId == null) {
            transactionRepository.getAllRecentTransactions(50)
        } else {
            transactionRepository.getRecentTransactions(profileId, 50)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        profiles,
        _selectedProfileId,
        allWallets,
        allSources,
        categories,
        unconfirmedCount,
        monthlyIncomeFlow,
        monthlyExpenseFlow,
        categoryExpensesFlow,
        recentTransactionsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profilesList = values[0] as List<Profile>
        val selectedId = values[1] as String?
        val walletsList = values[2] as List<Wallet>
        val sourcesList = values[3] as List<Source>
        val categoriesList = values[4] as List<Category>
        val unconfirmed = values[5] as Int
        val income = values[6] as Long?
        val expense = values[7] as Long?
        val catExpenses = values[8] as List<com.androidledger.data.dao.CategoryExpense>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[9] as List<Transaction>

        val categoryMap = categoriesList.associateBy { it.id }
        val walletMap = walletsList.associateBy { it.id }
        val sourceMap = sourcesList.associateBy { it.id }
        val profileMap = profilesList.associateBy { it.id }

        val filteredWallets = if (selectedId == null) walletsList
            else walletsList.filter { it.profileId == selectedId }

        val filteredWalletIds = filteredWallets.map { it.id }.toSet()

        val filteredSources = sourcesList.filter { it.walletId in filteredWalletIds && it.isArchived == 0 }

        // Determine primary currency from first wallet
        val primaryCurrency = filteredWallets.firstOrNull()?.currency ?: "EUR"

        // Monthly amounts - expense from OUT direction is negative (amountMinor stores negative for OUT)
        val monthlyExpenseAbs = abs(expense ?: 0L)
        val monthlyIncomeVal = income ?: 0L
        val monthlyNet = monthlyIncomeVal - monthlyExpenseAbs

        // Wallet display items
        val walletItems = filteredWallets.map { wallet ->
            val walletSources = filteredSources.filter { it.walletId == wallet.id }
            val totalBalance = walletSources.sumOf { it.balanceSnapshot }
            val profile = profileMap[wallet.profileId]
            WalletDisplayItem(
                walletId = wallet.id,
                walletName = wallet.name,
                currency = wallet.currency,
                totalBalance = totalBalance,
                profileName = profile?.name ?: "",
                sources = walletSources.map { source ->
                    SourceDisplayItem(
                        sourceId = source.id,
                        name = source.name,
                        icon = source.icon ?: "\uD83C\uDFE6",
                        balance = source.balanceSnapshot,
                        currency = wallet.currency,
                        walletName = wallet.name,
                        profileName = profile?.name ?: ""
                    )
                }
            )
        }

        // All source display items flattened
        val sourceItems = walletItems.flatMap { it.sources }

        // Category expenses
        val totalExpenseForCategories = catExpenses.sumOf { abs(it.total) }
        val categoryExpenseItems = catExpenses
            .sortedByDescending { abs(it.total) }
            .map { ce ->
                val cat = categoryMap[ce.categoryId]
                CategoryExpenseItem(
                    categoryId = ce.categoryId,
                    name = cat?.name ?: "Unknown",
                    emoji = cat?.icon ?: "\u2753",
                    amount = abs(ce.total),
                    percentage = if (totalExpenseForCategories > 0)
                        abs(ce.total).toFloat() / totalExpenseForCategories.toFloat() * 100f
                    else 0f,
                    color = cat?.color ?: "#95A5A6"
                )
            }

        // Transaction groups by date
        val transactionGroups = buildTransactionGroups(
            transactions = transactions,
            categoryMap = categoryMap,
            sourceMap = sourceMap,
            walletMap = walletMap,
            profileMap = profileMap,
            showProfile = selectedId == null
        )

        DashboardUiState(
            profiles = profilesList,
            selectedProfileId = selectedId,
            monthLabel = getMonthLabel(),
            monthlyIncome = monthlyIncomeVal,
            monthlyExpense = monthlyExpenseAbs,
            monthlyNet = monthlyNet,
            primaryCurrency = primaryCurrency,
            walletItems = walletItems,
            sourceItems = sourceItems,
            categoryExpenses = categoryExpenseItems,
            transactionGroups = transactionGroups,
            unconfirmedCount = unconfirmed,
            balanceHidden = _balanceHidden.value,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )

    fun selectProfile(id: String?) {
        _selectedProfileId.value = id
    }

    fun toggleBalanceHidden() {
        _balanceHidden.value = !_balanceHidden.value
    }

    private fun getMonthLabel(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "${year}\u5E74${month}\u6708"
    }

    private fun buildTransactionGroups(
        transactions: List<Transaction>,
        categoryMap: Map<String, Category>,
        sourceMap: Map<String, Source>,
        walletMap: Map<String, Wallet>,
        profileMap: Map<String, Profile>,
        showProfile: Boolean
    ): List<TransactionGroup> {
        val dateFormat = SimpleDateFormat("M\u6708d\u65E5 E", Locale.CHINESE)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val grouped = transactions.groupBy { tx ->
            dateFormat.format(Date(tx.occurredAt))
        }

        return grouped.map { (dateLabel, txList) ->
            val dailyTotal = txList
                .filter { it.direction == "OUT" }
                .sumOf { abs(it.amountMinor) }

            val currency = txList.firstOrNull()?.currency ?: "EUR"

            val displayItems = txList.map { tx ->
                val cat = tx.categoryId?.let { categoryMap[it] }
                val source = sourceMap[tx.sourceId]
                val wallet = walletMap[tx.walletId]
                val profile = profileMap[tx.profileId]

                TransactionDisplayItem(
                    transactionId = tx.id,
                    amount = formatAmount(tx.amountMinor, tx.currency),
                    merchant = tx.merchant ?: tx.description ?: cat?.name ?: "Unknown",
                    categoryEmoji = cat?.icon ?: "\u2753",
                    categoryName = cat?.name ?: "Uncategorized",
                    sourceIcon = source?.icon ?: "\uD83C\uDFE6",
                    sourceName = source?.name ?: "Unknown",
                    time = timeFormat.format(Date(tx.occurredAt)),
                    direction = tx.direction,
                    profileName = if (showProfile) profile?.name ?: "" else "",
                    currency = tx.currency
                )
            }

            TransactionGroup(
                dateLabel = dateLabel,
                dailyTotal = dailyTotal,
                currency = currency,
                transactions = displayItems
            )
        }
    }

    companion object {
        fun formatAmount(amountMinor: Long, currency: String): String {
            val symbol = getCurrencySymbol(currency)
            val absValue = abs(amountMinor)
            val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).let { nf ->
                nf.minimumFractionDigits = 2
                nf.maximumFractionDigits = 2
                nf.format(absValue.toDouble() / 100.0)
            }
            val prefix = if (amountMinor < 0) "-" else ""
            return "$prefix$symbol$formatted"
        }

        fun formatAmountAbs(amountMinor: Long, currency: String): String {
            val symbol = getCurrencySymbol(currency)
            val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).let { nf ->
                nf.minimumFractionDigits = 2
                nf.maximumFractionDigits = 2
                nf.format(amountMinor.toDouble() / 100.0)
            }
            return "$symbol$formatted"
        }

        fun getCurrencySymbol(currency: String): String {
            return try {
                Currency.getInstance(currency).symbol
            } catch (_: Exception) {
                when (currency.uppercase()) {
                    "EUR" -> "\u20AC"
                    "CNY", "RMB" -> "\u00A5"
                    "USD" -> "$"
                    "GBP" -> "\u00A3"
                    else -> "$currency "
                }
            }
        }
    }
}
