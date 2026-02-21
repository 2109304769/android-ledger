package com.androidledger.ui.transactiondetail

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val profileName: String = "",
    val walletName: String = "",
    val sourceName: String = "",
    val categoryName: String = "",
    val categoryIcon: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val allCategories: List<Category> = emptyList(),
    val allSources: List<Source> = emptyList(),
    val allTags: List<Tag> = emptyList()
)

sealed class TransactionDetailEvent {
    data object NavigateBack : TransactionDetailEvent()
    data class ShowError(val message: String) : TransactionDetailEvent()
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TransactionDetailEvent>()
    val events: SharedFlow<TransactionDetailEvent> = _events.asSharedFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionById(transactionId),
                profileRepository.getProfiles(),
                profileRepository.getAllWallets(),
                profileRepository.getAllSources(),
                categoryRepository.getAll()
            ) { transaction, profiles, wallets, sources, categories ->
                TransactionLoadData(transaction, profiles, wallets, sources, categories)
            }.combine(settingsRepository.getAllTags()) { data, tags ->
                val transaction = data.transaction
                if (transaction == null) {
                    _uiState.value.copy(isLoading = false)
                } else {
                    val profile = data.profiles.find { it.id == transaction.profileId }
                    val wallet = data.wallets.find { it.id == transaction.walletId }
                    val source = data.sources.find { it.id == transaction.sourceId }
                    val category = transaction.categoryId?.let { catId ->
                        data.categories.find { it.id == catId }
                    }

                    TransactionDetailUiState(
                        transaction = transaction,
                        profileName = profile?.let { "${it.emoji} ${it.name}" } ?: "",
                        walletName = wallet?.name ?: "",
                        sourceName = source?.name ?: "",
                        categoryName = category?.name ?: "",
                        categoryIcon = category?.icon ?: "",
                        isEditing = _uiState.value.isEditing,
                        isLoading = false,
                        allCategories = data.categories,
                        allSources = data.sources,
                        allTags = tags
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private data class TransactionLoadData(
        val transaction: Transaction?,
        val profiles: List<Profile>,
        val wallets: List<Wallet>,
        val sources: List<Source>,
        val categories: List<Category>
    )

    fun toggleEdit() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    fun updateTransaction(
        amountMinor: Long? = null,
        direction: String? = null,
        categoryId: String? = null,
        clearCategory: Boolean = false,
        sourceId: String? = null,
        merchant: String? = null,
        description: String? = null,
        occurredAt: Long? = null,
        tagIds: String? = null,
        clearTags: Boolean = false
    ) {
        val current = _uiState.value.transaction ?: return
        val updated = current.copy(
            amountMinor = amountMinor ?: current.amountMinor,
            direction = direction ?: current.direction,
            categoryId = if (clearCategory) null else (categoryId ?: current.categoryId),
            sourceId = sourceId ?: current.sourceId,
            merchant = merchant ?: current.merchant,
            description = description ?: current.description,
            occurredAt = occurredAt ?: current.occurredAt,
            tagIds = if (clearTags) null else (tagIds ?: current.tagIds)
        )
        viewModelScope.launch {
            try {
                transactionRepository.updateTransaction(updated)
                _uiState.value = _uiState.value.copy(isEditing = false)
            } catch (e: Exception) {
                _events.emit(TransactionDetailEvent.ShowError("更新失败: ${e.message}"))
            }
        }
    }

    fun confirmTransaction() {
        val current = _uiState.value.transaction ?: return
        val confirmed = current.copy(isConfirmed = 1)
        viewModelScope.launch {
            try {
                transactionRepository.updateTransaction(confirmed)
            } catch (e: Exception) {
                _events.emit(TransactionDetailEvent.ShowError("确认失败: ${e.message}"))
            }
        }
    }

    fun deleteTransaction() {
        val current = _uiState.value.transaction ?: return
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(current)
                _events.emit(TransactionDetailEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(TransactionDetailEvent.ShowError("删除失败: ${e.message}"))
            }
        }
    }
}
