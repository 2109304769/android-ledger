package com.androidledger.ui.transactions

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// ---------- Data models ----------

data class TransactionListItem(
    val id: String,
    val amount: String,
    val amountMinor: Long,
    val merchant: String,
    val categoryEmoji: String,
    val categoryName: String,
    val sourceName: String,
    val time: String,
    val direction: String,
    val currency: String,
    val isConfirmed: Boolean,
    val profileName: String
)

data class DateGroup(
    val dateLabel: String,
    val dailyExpenseTotal: Long,
    val dailyExpenseCurrency: String,
    val items: List<TransactionListItem>
)

data class TransactionsSummary(
    val totalIncome: Long,
    val totalExpense: Long,
    val netFlow: Long
)

enum class TimeRange {
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

data class TransactionsFilterState(
    val selectedTimeRange: TimeRange = TimeRange.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedProfileId: String? = null,
    val selectedWalletId: String? = null,
    val selectedSourceId: String? = null,
    val selectedCategoryId: String? = null,
    val searchQuery: String = ""
)

// ---------- ViewModel ----------

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Raw data from DB
    private val allTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categoriesList: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<Map<String, Category>> = MutableStateFlow(emptyMap<String, Category>())

    // Filter state
    private val _filterState = MutableStateFlow(TransactionsFilterState())
    val filterState: StateFlow<TransactionsFilterState> = _filterState.asStateFlow()

    // Derived state: date groups + summary
    private val _dateGroups = MutableStateFlow<List<DateGroup>>(emptyList())
    val dateGroups: StateFlow<List<DateGroup>> = _dateGroups.asStateFlow()

    private val _summary = MutableStateFlow(TransactionsSummary(0L, 0L, 0L))
    val summary: StateFlow<TransactionsSummary> = _summary.asStateFlow()

    init {
        // Observe all data sources together and recompute on change.
        // combine() supports up to 5 typed flows; for 6 we nest two combines.
        viewModelScope.launch {
            combine(
                combine(allTransactions, _filterState, categoriesList) { txns, filter, cats ->
                    Triple(txns, filter, cats)
                },
                combine(profiles, wallets, sources) { profs, wals, srcs ->
                    Triple(profs, wals, srcs)
                }
            ) { (txns, filter, cats), (profs, wals, srcs) ->
                DataBundle(txns, filter, cats, profs, wals, srcs)
            }.collect { bundle ->
                // Update categories map
                val catMap = bundle.categories.associateBy { it.id }
                (categories as MutableStateFlow).value = catMap

                applyFilters(
                    bundle.transactions,
                    bundle.filterState,
                    catMap,
                    bundle.profiles,
                    bundle.sources
                )
            }
        }
    }

    // --- Filter setters ---

    fun setTimeRange(range: TimeRange) {
        _filterState.value = _filterState.value.copy(
            selectedTimeRange = range,
            customStartDate = if (range != TimeRange.CUSTOM) null else _filterState.value.customStartDate,
            customEndDate = if (range != TimeRange.CUSTOM) null else _filterState.value.customEndDate
        )
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _filterState.value = _filterState.value.copy(
            selectedTimeRange = TimeRange.CUSTOM,
            customStartDate = start,
            customEndDate = end
        )
    }

    fun setProfileFilter(id: String?) {
        _filterState.value = _filterState.value.copy(selectedProfileId = id)
    }

    fun setWalletFilter(id: String?) {
        _filterState.value = _filterState.value.copy(selectedWalletId = id)
    }

    fun setSourceFilter(id: String?) {
        _filterState.value = _filterState.value.copy(selectedSourceId = id)
    }

    fun setCategoryFilter(id: String?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = id)
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun clearFilters() {
        _filterState.value = TransactionsFilterState()
    }

    // --- Internal filtering and grouping ---

    private fun applyFilters(
        transactions: List<Transaction>,
        filter: TransactionsFilterState,
        catMap: Map<String, Category>,
        profilesList: List<Profile>,
        sourcesList: List<Source>
    ) {
        val profileMap = profilesList.associateBy { it.id }
        val sourceMap = sourcesList.associateBy { it.id }

        // Compute date range
        val (rangeStart, rangeEnd) = getDateRange(filter)

        // Step 1: Filter
        val filtered = transactions.filter { txn ->
            // Date range
            val inDateRange = txn.occurredAt in rangeStart..rangeEnd

            // Profile
            val matchesProfile = filter.selectedProfileId == null ||
                    txn.profileId == filter.selectedProfileId

            // Wallet
            val matchesWallet = filter.selectedWalletId == null ||
                    txn.walletId == filter.selectedWalletId

            // Source
            val matchesSource = filter.selectedSourceId == null ||
                    txn.sourceId == filter.selectedSourceId

            // Category
            val matchesCategory = filter.selectedCategoryId == null ||
                    txn.categoryId == filter.selectedCategoryId

            // Search
            val matchesSearch = filter.searchQuery.isBlank() || run {
                val q = filter.searchQuery.lowercase(Locale.getDefault())
                val merchantMatch = txn.merchant?.lowercase(Locale.getDefault())?.contains(q) == true
                val descMatch = txn.description?.lowercase(Locale.getDefault())?.contains(q) == true
                val amountMatch = formatAmount(txn.amountMinor, txn.currency).contains(q)
                merchantMatch || descMatch || amountMatch
            }

            inDateRange && matchesProfile && matchesWallet && matchesSource && matchesCategory && matchesSearch
        }

        // Step 2: Compute summary
        var totalIncome = 0L
        var totalExpense = 0L
        for (txn in filtered) {
            when (txn.direction) {
                "IN" -> totalIncome += txn.amountMinor
                "OUT" -> totalExpense += txn.amountMinor
            }
        }
        _summary.value = TransactionsSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netFlow = totalIncome - totalExpense
        )

        // Step 3: Map to list items and group by date desc
        val sorted = filtered.sortedByDescending { it.occurredAt }

        val groups = sorted.groupBy { txn ->
            dayKey(txn.occurredAt)
        }.entries.sortedByDescending { it.key }.map { (dayMillis, txns) ->
            val dailyExpense = txns.filter { it.direction == "OUT" }.sumOf { it.amountMinor }
            // Use the currency of the first OUT transaction in that day, or first transaction's currency
            val expenseCurrency = txns.firstOrNull { it.direction == "OUT" }?.currency
                ?: txns.first().currency

            DateGroup(
                dateLabel = formatDateHeader(dayMillis),
                dailyExpenseTotal = dailyExpense,
                dailyExpenseCurrency = expenseCurrency,
                items = txns.map { txn ->
                    val cat = txn.categoryId?.let { catMap[it] }
                    val source = sourceMap[txn.sourceId]
                    val profile = profileMap[txn.profileId]

                    TransactionListItem(
                        id = txn.id,
                        amount = formatAmount(txn.amountMinor, txn.currency),
                        amountMinor = txn.amountMinor,
                        merchant = txn.merchant ?: txn.description ?: "",
                        categoryEmoji = cat?.icon ?: "",
                        categoryName = cat?.name ?: "",
                        sourceName = source?.name ?: "",
                        time = formatTime(txn.occurredAt),
                        direction = txn.direction,
                        currency = txn.currency,
                        isConfirmed = txn.isConfirmed == 1,
                        profileName = profile?.name ?: ""
                    )
                }
            )
        }

        _dateGroups.value = groups
    }

    private fun getDateRange(filter: TransactionsFilterState): Pair<Long, Long> {
        return when (filter.selectedTimeRange) {
            TimeRange.THIS_MONTH -> {
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
                start to end
            }
            TimeRange.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                val end = cal.timeInMillis
                start to end
            }
            TimeRange.CUSTOM -> {
                val start = filter.customStartDate ?: 0L
                val end = filter.customEndDate ?: Long.MAX_VALUE
                start to end
            }
        }
    }

    // --- Formatting helpers ---

    companion object {
        private val amountFormat = DecimalFormat("#,##0.00")

        private val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

        fun formatAmount(amountMinor: Long, currency: String): String {
            val value = amountMinor / 100.0
            val symbol = when (currency.uppercase(Locale.getDefault())) {
                "EUR" -> "\u20AC"
                "CNY" -> "\u00A5"
                "USD" -> "$"
                else -> currency
            }
            return "$symbol${amountFormat.format(value)}"
        }

        fun formatDateHeader(dayMillis: Long): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = dayMillis
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
            return "${month}\u6708${day}\u65E5 ${weekDays[dow]}"
        }

        fun formatTime(millis: Long): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
            )
        }

        /**
         * Returns the millis for the start of the day (00:00:00.000) of the given timestamp.
         */
        fun dayKey(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }

    private data class DataBundle(
        val transactions: List<Transaction>,
        val filterState: TransactionsFilterState,
        val categories: List<Category>,
        val profiles: List<Profile>,
        val wallets: List<Wallet>,
        val sources: List<Source>
    )
}
