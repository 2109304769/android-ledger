package com.androidledger.data.repository

import com.androidledger.data.dao.ProfileDao
import com.androidledger.data.dao.SourceDao
import com.androidledger.data.dao.WalletDao
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val walletDao: WalletDao,
    private val sourceDao: SourceDao
) {

    // ---- Profile ----

    fun getProfiles(): Flow<List<Profile>> = profileDao.getAll()

    fun getProfileById(id: String): Flow<Profile?> = profileDao.getById(id)

    fun getDefaultProfile(): Flow<Profile?> = profileDao.getDefault()

    suspend fun createProfile(name: String, type: String, emoji: String): String {
        val now = System.currentTimeMillis()
        val profileId = UUID.randomUUID().toString()

        val profile = Profile(
            id = profileId,
            name = name,
            type = type,
            emoji = emoji,
            createdAt = now
        )
        profileDao.insert(profile)

        // Create default EUR wallet
        walletDao.insert(
            Wallet(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "EUR",
                currency = "EUR",
                sortOrder = 0,
                createdAt = now
            )
        )

        // Create default CNY wallet
        walletDao.insert(
            Wallet(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "CNY",
                currency = "CNY",
                sortOrder = 1,
                createdAt = now
            )
        )

        return profileId
    }

    suspend fun updateProfile(profile: Profile) = profileDao.update(profile)

    suspend fun deleteProfile(profile: Profile) = profileDao.delete(profile)

    suspend fun setDefaultProfile(id: String) = profileDao.setDefault(id)

    // ---- Wallet ----

    fun getWalletsByProfile(profileId: String): Flow<List<Wallet>> =
        walletDao.getByProfileId(profileId)

    fun getAllWallets(): Flow<List<Wallet>> = walletDao.getAll()

    suspend fun createWallet(
        profileId: String,
        name: String,
        currency: String,
        sortOrder: Int = 0
    ): String {
        val walletId = UUID.randomUUID().toString()
        walletDao.insert(
            Wallet(
                id = walletId,
                profileId = profileId,
                name = name,
                currency = currency,
                sortOrder = sortOrder,
                createdAt = System.currentTimeMillis()
            )
        )
        return walletId
    }

    suspend fun updateWallet(wallet: Wallet) = walletDao.update(wallet)

    suspend fun deleteWallet(wallet: Wallet) = walletDao.delete(wallet)

    // ---- Source ----

    fun getSourcesByWallet(walletId: String): Flow<List<Source>> =
        sourceDao.getByWalletId(walletId)

    fun getActiveSourcesByWallet(walletId: String): Flow<List<Source>> =
        sourceDao.getActiveByWalletId(walletId)

    fun getAllSources(): Flow<List<Source>> = sourceDao.getAll()

    suspend fun createSource(
        walletId: String,
        name: String,
        type: String,
        icon: String? = null
    ): String {
        val sourceId = UUID.randomUUID().toString()
        sourceDao.insert(
            Source(
                id = sourceId,
                walletId = walletId,
                name = name,
                type = type,
                icon = icon
            )
        )
        return sourceId
    }

    suspend fun updateSource(source: Source) = sourceDao.update(source)

    suspend fun deleteSource(source: Source) = sourceDao.delete(source)

    suspend fun archiveSource(source: Source) =
        sourceDao.update(source.copy(isArchived = 1))
}
