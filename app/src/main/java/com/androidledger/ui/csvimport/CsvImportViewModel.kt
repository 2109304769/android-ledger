package com.androidledger.ui.csvimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Transaction
import com.androidledger.data.entity.Wallet
import com.androidledger.data.repository.ProfileRepository
import com.androidledger.data.repository.TransactionRepository
import com.androidledger.integration.csv.CsvFormat
import com.androidledger.integration.csv.CsvFormatDetector
import com.androidledger.integration.csv.CsvImportResult
import com.androidledger.integration.csv.PosteCsvImporter
import com.androidledger.integration.csv.RevolutCsvImporter
import com.androidledger.integration.csv.WiseCsvImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ImportState {
    IDLE,
    DETECTING,
    PREVIEW,
    IMPORTING,
    SUCCESS,
    ERROR
}

data class CsvImportUiState(
    val state: ImportState = ImportState.IDLE,
    val detectedFormat: CsvFormat? = null,
    val importResult: CsvImportResult? = null,
    val selectedSourceId: String? = null,
    val selectedProfileId: String? = null,
    val selectedWalletId: String? = null,
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val revolutCsvImporter: RevolutCsvImporter,
    private val wiseCsvImporter: WiseCsvImporter,
    private val posteCsvImporter: PosteCsvImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    val profiles: StateFlow<List<Profile>> = profileRepository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<Wallet>> = profileRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sources: StateFlow<List<Source>> = profileRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cached raw content for re-parsing or importing
    private var cachedCsvContent: String? = null

    fun processFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                state = ImportState.DETECTING,
                errorMessage = null
            )

            try {
                // Read file content
                val content = readFileContent(uri)
                if (content.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        state = ImportState.ERROR,
                        errorMessage = "文件内容为空"
                    )
                    return@launch
                }

                cachedCsvContent = content

                // Detect format
                val firstLine = content.lines().firstOrNull() ?: ""
                val format = CsvFormatDetector.detect(firstLine)

                if (format == CsvFormat.UNKNOWN) {
                    _uiState.value = _uiState.value.copy(
                        state = ImportState.ERROR,
                        detectedFormat = format,
                        errorMessage = "无法识别CSV格式。支持的格式：Revolut、Wise、Poste Italiane"
                    )
                    return@launch
                }

                // Parse according to detected format
                val result = when (format) {
                    CsvFormat.REVOLUT -> revolutCsvImporter.parse(content)
                    CsvFormat.WISE -> wiseCsvImporter.parse(content)
                    CsvFormat.POSTE -> posteCsvImporter.parse(content)
                    CsvFormat.UNKNOWN -> null
                }

                if (result == null || result.transactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        state = ImportState.ERROR,
                        detectedFormat = format,
                        importResult = result,
                        errorMessage = "未找到可导入的交易记录"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    state = ImportState.PREVIEW,
                    detectedFormat = format,
                    importResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    state = ImportState.ERROR,
                    errorMessage = "读取文件失败: ${e.message}"
                )
            }
        }
    }

    fun selectSource(sourceId: String) {
        _uiState.value = _uiState.value.copy(selectedSourceId = sourceId)

        // Auto-fill profile and wallet from source
        val source = sources.value.find { it.id == sourceId }
        if (source != null) {
            val wallet = wallets.value.find { it.id == source.walletId }
            _uiState.value = _uiState.value.copy(
                selectedWalletId = source.walletId,
                selectedProfileId = wallet?.profileId
            )
        }
    }

    fun selectProfile(profileId: String) {
        _uiState.value = _uiState.value.copy(selectedProfileId = profileId)
    }

    fun selectWallet(walletId: String) {
        _uiState.value = _uiState.value.copy(selectedWalletId = walletId)
    }

    fun confirmImport() {
        val currentState = _uiState.value
        val result = currentState.importResult ?: return
        val sourceId = currentState.selectedSourceId
        val profileId = currentState.selectedProfileId
        val walletId = currentState.selectedWalletId

        if (sourceId == null || profileId == null || walletId == null) {
            _uiState.value = currentState.copy(
                errorMessage = "请选择账户来源"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                state = ImportState.IMPORTING,
                errorMessage = null
            )

            try {
                val now = System.currentTimeMillis()
                val transactions = result.transactions.map { parsed ->
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        profileId = profileId,
                        walletId = walletId,
                        sourceId = sourceId,
                        occurredAt = parsed.occurredAt,
                        amountMinor = parsed.amountMinor,
                        currency = parsed.currency,
                        direction = parsed.direction,
                        merchant = parsed.merchant,
                        description = parsed.description,
                        source = parsed.entrySource,
                        externalId = parsed.externalId,
                        isConfirmed = 1,
                        createdAt = now
                    )
                }

                // Batch insert with ignore duplicates
                transactionRepository.addTransactionsOrIgnore(transactions)

                _uiState.value = _uiState.value.copy(
                    state = ImportState.SUCCESS,
                    importedCount = transactions.size,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    state = ImportState.ERROR,
                    errorMessage = "导入失败: ${e.message}"
                )
            }
        }
    }

    fun reset() {
        cachedCsvContent = null
        _uiState.value = CsvImportUiState()
    }

    private fun readFileContent(uri: Uri): String? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri) ?: return null

            // For Poste Italiane, we may need ISO-8859-1 encoding
            // We'll try to read as UTF-8 first, and if the format is Poste, re-read
            val bytes = inputStream.use { it.readBytes() }

            // First try UTF-8
            val utf8Content = String(bytes, Charsets.UTF_8)
            val firstLine = utf8Content.lines().firstOrNull() ?: ""
            val format = CsvFormatDetector.detect(firstLine)

            if (format == CsvFormat.POSTE) {
                // Re-decode as ISO-8859-1
                String(bytes, Charsets.ISO_8859_1)
            } else {
                utf8Content
            }
        } catch (e: Exception) {
            null
        }
    }
}
