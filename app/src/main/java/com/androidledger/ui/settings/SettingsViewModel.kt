package com.androidledger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.ExchangeRate
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.SettingsRepository
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
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exchangeRate = MutableStateFlow<ExchangeRate?>(null)
    val exchangeRate: StateFlow<ExchangeRate?> = _exchangeRate.asStateFlow()

    init {
        loadExchangeRate()
    }

    private fun loadExchangeRate() {
        viewModelScope.launch {
            settingsRepository.getRate("EUR", "CNY").collect { rate ->
                _exchangeRate.value = rate
            }
        }
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
}
