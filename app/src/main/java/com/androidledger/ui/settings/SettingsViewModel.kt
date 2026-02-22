package com.androidledger.ui.settings

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.ExchangeRate
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.CategoryRepository
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.SettingsRepository
import com.androidledger.integration.notification.LedgerNotificationListener
import com.androidledger.ui.quickentry.QuickEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    companion object {
        private const val PREF_NAME = "notification_listener_prefs"
        private const val KEY_ENABLED = "notification_listening_enabled"
        private const val KEY_PRIVACY_ACCEPTED = "notification_privacy_accepted"
    }

    private val quickEntryPrefs: SharedPreferences =
        application.getSharedPreferences(QuickEntryViewModel.PREFS_NAME, Context.MODE_PRIVATE)

    val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exchangeRate = MutableStateFlow<ExchangeRate?>(null)
    val exchangeRate: StateFlow<ExchangeRate?> = _exchangeRate.asStateFlow()

    private val _notificationListeningEnabled = MutableStateFlow(false)
    val notificationListeningEnabled: StateFlow<Boolean> = _notificationListeningEnabled.asStateFlow()

    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()

    private val _privacyAccepted = MutableStateFlow(false)
    val privacyAccepted: StateFlow<Boolean> = _privacyAccepted.asStateFlow()

    private fun loadExchangeRate() {
        viewModelScope.launch {
            settingsRepository.getRate("EUR", "CNY").collect { rate ->
                _exchangeRate.value = rate
            }
        }
    }

    private fun loadNotificationSettings() {
        val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _notificationListeningEnabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _privacyAccepted.value = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        _notificationAccessGranted.value = isNotificationAccessGranted()
    }

    fun refreshNotificationAccessStatus() {
        _notificationAccessGranted.value = isNotificationAccessGranted()
    }

    fun setNotificationListeningEnabled(enabled: Boolean) {
        val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _notificationListeningEnabled.value = enabled
    }

    fun setPrivacyAccepted() {
        val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).apply()
        _privacyAccepted.value = true
    }

    fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(
            application.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val componentName = ComponentName(application, LedgerNotificationListener::class.java)
        return flat.contains(componentName.flattenToString())
    }

    fun createProfile(name: String, type: String, emoji: String) {
        viewModelScope.launch {
            profileRepository.createProfile(name = name, type = type, emoji = emoji)
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun createWallet(profileId: String, name: String, currency: String) {
        viewModelScope.launch {
            profileRepository.createWallet(
                profileId = profileId,
                name = name,
                currency = currency
            )
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            profileRepository.deleteWallet(wallet)
        }
    }

    fun createSource(walletId: String, name: String, type: String, icon: String?) {
        viewModelScope.launch {
            profileRepository.createSource(
                walletId = walletId,
                name = name,
                type = type,
                icon = icon
            )
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            profileRepository.deleteSource(source)
        }
    }

    fun updateExchangeRate(newRate: Double) {
        viewModelScope.launch {
            val current = _exchangeRate.value
            val updated = if (current != null) {
                current.copy(rate = newRate, updatedAt = System.currentTimeMillis())
            } else {
                ExchangeRate(
                    id = UUID.randomUUID().toString(),
                    fromCurrency = "EUR",
                    toCurrency = "CNY",
                    rate = newRate,
                    updatedAt = System.currentTimeMillis()
                )
            }
            settingsRepository.updateRate(updated)
        }
    }

    fun getWalletsForProfile(profileId: String): List<Wallet> {
        return allWallets.value.filter { it.profileId == profileId }
    }

    fun getSourcesForWallet(walletId: String): List<Source> {
        return allSources.value.filter { it.walletId == walletId }
    }

    // ========== Quick Entry Settings ==========

    val allCategories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickEntryEnabled = MutableStateFlow(false)
    val quickEntryEnabled: StateFlow<Boolean> = _quickEntryEnabled.asStateFlow()

    private val _quickEntryDefaultExpenseSourceId = MutableStateFlow<String?>(null)
    val quickEntryDefaultExpenseSourceId: StateFlow<String?> = _quickEntryDefaultExpenseSourceId.asStateFlow()

    private val _quickEntryDefaultIncomeSourceId = MutableStateFlow<String?>(null)
    val quickEntryDefaultIncomeSourceId: StateFlow<String?> = _quickEntryDefaultIncomeSourceId.asStateFlow()

    private val _quickEntryDefaultExpenseCategoryId = MutableStateFlow<String?>(null)
    val quickEntryDefaultExpenseCategoryId: StateFlow<String?> = _quickEntryDefaultExpenseCategoryId.asStateFlow()

    private val _quickEntryDefaultIncomeCategoryId = MutableStateFlow<String?>(null)
    val quickEntryDefaultIncomeCategoryId: StateFlow<String?> = _quickEntryDefaultIncomeCategoryId.asStateFlow()

    private val _quickEntryDefaultCurrency = MutableStateFlow("EUR")
    val quickEntryDefaultCurrency: StateFlow<String> = _quickEntryDefaultCurrency.asStateFlow()

    private val _quickEntryDefaultProfileId = MutableStateFlow<String?>(null)
    val quickEntryDefaultProfileId: StateFlow<String?> = _quickEntryDefaultProfileId.asStateFlow()

    init {
        loadExchangeRate()
        loadNotificationSettings()
        loadQuickEntrySettings()
    }

    private fun loadQuickEntrySettings() {
        _quickEntryEnabled.value = quickEntryPrefs.getBoolean(QuickEntryViewModel.KEY_ENABLED, false)
        _quickEntryDefaultExpenseSourceId.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_EXPENSE_SOURCE_ID, null)
        _quickEntryDefaultIncomeSourceId.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_INCOME_SOURCE_ID, null)
        _quickEntryDefaultExpenseCategoryId.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_EXPENSE_CATEGORY_ID, null)
        _quickEntryDefaultIncomeCategoryId.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_INCOME_CATEGORY_ID, null)
        _quickEntryDefaultCurrency.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_CURRENCY, "EUR") ?: "EUR"
        _quickEntryDefaultProfileId.value = quickEntryPrefs.getString(QuickEntryViewModel.KEY_DEFAULT_PROFILE_ID, null)
    }

    fun setQuickEntryEnabled(enabled: Boolean) {
        quickEntryPrefs.edit().putBoolean(QuickEntryViewModel.KEY_ENABLED, enabled).apply()
        _quickEntryEnabled.value = enabled
    }

    fun setQuickEntryDefaultExpenseSource(sourceId: String?) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_EXPENSE_SOURCE_ID, sourceId).apply()
        _quickEntryDefaultExpenseSourceId.value = sourceId
    }

    fun setQuickEntryDefaultIncomeSource(sourceId: String?) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_INCOME_SOURCE_ID, sourceId).apply()
        _quickEntryDefaultIncomeSourceId.value = sourceId
    }

    fun setQuickEntryDefaultExpenseCategory(categoryId: String?) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_EXPENSE_CATEGORY_ID, categoryId).apply()
        _quickEntryDefaultExpenseCategoryId.value = categoryId
    }

    fun setQuickEntryDefaultIncomeCategory(categoryId: String?) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_INCOME_CATEGORY_ID, categoryId).apply()
        _quickEntryDefaultIncomeCategoryId.value = categoryId
    }

    fun setQuickEntryDefaultCurrency(currency: String) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_CURRENCY, currency).apply()
        _quickEntryDefaultCurrency.value = currency
    }

    fun setQuickEntryDefaultProfile(profileId: String?) {
        quickEntryPrefs.edit().putString(QuickEntryViewModel.KEY_DEFAULT_PROFILE_ID, profileId).apply()
        _quickEntryDefaultProfileId.value = profileId
    }
}
