package com.androidledger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.dao.ProfileDao
import com.androidledger.data.dao.SourceDao
import com.androidledger.data.dao.WalletDao
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileDao: ProfileDao,
    private val walletDao: WalletDao,
    private val sourceDao: SourceDao
) : ViewModel() {

    val profiles = profileDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProfile(name: String, type: String, emoji: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            profileDao.insert(
                Profile(
                    id = id,
                    name = name,
                    type = type,
                    emoji = emoji,
                    isDefault = 0,
                    createdAt = System.currentTimeMillis()
                )
            )
            // also auto-create a default wallet for new profile
            val walletId = UUID.randomUUID().toString()
            walletDao.insert(
                Wallet(
                    id = walletId,
                    profileId = id,
                    name = "默认钱包",
                    currency = "EUR",
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis()
                )
            )
            // and a default source
            sourceDao.insert(
                Source(
                    id = UUID.randomUUID().toString(),
                    walletId = walletId,
                    name = "现金",
                    type = "CASH",
                    balanceSnapshot = 0L,
                    balanceUpdatedAt = System.currentTimeMillis(),
                    icon = "💵",
                    isArchived = 0
                )
            )
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileDao.delete(profile)
        }
    }
}
